package com.paperfolio.ui.pnl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperfolio.data.local.dao.TradeDao
import com.paperfolio.data.local.entity.TradeEntity
import com.paperfolio.data.repository.FifoPnl
import com.paperfolio.data.repository.PortfolioRepository
import com.paperfolio.data.repository.TradeRepository
import com.paperfolio.domain.model.Portfolio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PnlUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: String = "ALL", // "1D", "1W", "1M", "6M", "1Y", "3Y", "5Y", "ALL", "CUSTOM"
    val startDateTimestamp: Long? = null,
    val endDateTimestamp: Long? = null,
    val totalRealizedPnl: Double = 0.0,
    val totalUnrealizedPnl: Double = 0.0,
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
    val closedTrades: List<TradeEntity> = emptyList(),
    val portfolioBreakdown: List<PortfolioPnlItem> = emptyList()
)

data class PortfolioPnlItem(
    val portfolioId: Long,
    val portfolioName: String,
    val colorTag: String,
    val realizedPnl: Double,
    val unrealizedPnl: Double,
    val totalPnl: Double
)

class PnlViewModel(
    private val tradeDao: TradeDao,
    private val portfolioRepository: PortfolioRepository,
    private val tradeRepository: TradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PnlUiState())
    val uiState: StateFlow<PnlUiState> = _uiState.asStateFlow()

    init {
        loadPnlData("ALL")
    }

    fun loadPnlData(
        period: String,
        customStart: Long? = null,
        customEnd: Long? = null
    ) {
        viewModelScope.launch {
            // Was: any database/network failure inside this launch left
            // the screen stuck on the spinner forever (isLoading never
            // reset) or crashed the coroutine. Failures now end loading
            // and keep the last good data on screen.
            try {
                loadPnlDataUnsafe(period, customStart, customEnd)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadPnlDataUnsafe(
        period: String,
        customStart: Long? = null,
        customEnd: Long? = null
    ) {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedPeriod = period)

            val cal = Calendar.getInstance()
            val now = System.currentTimeMillis()

            val (startMs, endMs) = when (period) {
                "1D" -> Pair(cal.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis, now)
                "1W" -> Pair(cal.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis, now)
                "1M" -> Pair(cal.apply { add(Calendar.MONTH, -1) }.timeInMillis, now)
                "6M" -> Pair(cal.apply { add(Calendar.MONTH, -6) }.timeInMillis, now)
                "1Y" -> Pair(cal.apply { add(Calendar.YEAR, -1) }.timeInMillis, now)
                "3Y" -> Pair(cal.apply { add(Calendar.YEAR, -3) }.timeInMillis, now)
                "5Y" -> Pair(cal.apply { add(Calendar.YEAR, -5) }.timeInMillis, now)
                // The date picker returns the START of the picked days
                // (00:00). Without this, every trade on the end day
                // itself was silently excluded from the range.
                "CUSTOM" -> Pair(
                    customStart ?: 0L,
                    (customEnd ?: now) + 86_399_999L // end of the picked day
                )
                else -> Pair(0L, now) // ALL
            }

            // Realized P&L, recomputed FIFO from the FULL history.
            // The `realizedPnl` stored on old SELL rows was computed with
            // the broken all-buys average (buy 10@100, sell 10@120, buy
            // 10@200, sell 10@210 stored +600 instead of +100), so the
            // screen cannot trust it. FIFO needs the lots open before
            // each sell, so the replay always starts from the full
            // history — even for a filtered period — and the period
            // filter is applied to the sells afterwards.
            val allTrades = tradeDao.getAllTradesList()
            val correctedRealized = FifoPnl.realizedPnlByTradeId(allTrades)
            val trades = if (period == "ALL") {
                allTrades
            } else {
                allTrades.filter { it.timestamp in startMs..endMs }
            }

            // Realized PnL from SELL trades (FIFO-corrected values)
            val sellTrades = trades.filter { it.type == "SELL" }
                .map { it.copy(realizedPnl = correctedRealized[it.id] ?: it.realizedPnl) }
            val realizedPnl = sellTrades.sumOf { it.realizedPnl ?: 0.0 }

            // Unrealized PnL across active portfolios
            val portfolios = portfolioRepository.getAllPortfolios()
            var overallInvested = 0.0
            var overallCurrentVal = 0.0
            val breakdown = mutableListOf<PortfolioPnlItem>()

            for (p in portfolios) {
                try {
                    val (enriched, holdings) = portfolioRepository.getPortfolioDetailsWithLivePrices(p.id)
                    overallInvested += enriched.investedAmount
                    overallCurrentVal += enriched.currentValue

                    val portfolioSellTrades = sellTrades.filter { it.portfolioId == p.id }
                    val portRealized = portfolioSellTrades.sumOf { it.realizedPnl ?: 0.0 }
                    val portUnrealized = enriched.totalPnl

                    breakdown.add(
                        PortfolioPnlItem(
                            portfolioId = p.id,
                            portfolioName = p.name,
                            colorTag = p.colorTag,
                            realizedPnl = portRealized,
                            unrealizedPnl = portUnrealized,
                            totalPnl = portRealized + portUnrealized
                        )
                    )
                } catch (e: Exception) {
                    // Ignore transient errors
                }
            }

            val unrealizedPnl = overallCurrentVal - overallInvested

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                startDateTimestamp = startMs,
                endDateTimestamp = endMs,
                totalRealizedPnl = realizedPnl,
                totalUnrealizedPnl = unrealizedPnl,
                totalInvested = overallInvested,
                totalCurrentValue = overallCurrentVal,
                closedTrades = sellTrades,
                portfolioBreakdown = breakdown
            )
    }
}
