package com.paperfolio.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperfolio.data.local.dao.PortfolioSnapshotDao
import com.paperfolio.data.repository.PortfolioRepository
import com.paperfolio.data.repository.TradeRepository
import com.paperfolio.domain.model.ChartPoint
import com.paperfolio.domain.model.Portfolio
import com.paperfolio.domain.model.StockHolding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PortfolioDetailUiState(
    val isLoading: Boolean = true,
    val portfolio: Portfolio? = null,
    val holdings: List<StockHolding> = emptyList(),
    val selectedPeriod: String = "1D",
    val chartPoints: List<ChartPoint> = emptyList(),
    val errorMessage: String? = null
)

class PortfolioDetailViewModel(
    private val portfolioId: Long,
    private val portfolioRepository: PortfolioRepository,
    private val tradeRepository: TradeRepository,
    private val snapshotDao: PortfolioSnapshotDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioDetailUiState())
    val uiState: StateFlow<PortfolioDetailUiState> = _uiState.asStateFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val (enrichedPortfolio, liveHoldings) = portfolioRepository.getPortfolioDetailsWithLivePrices(portfolioId)
                val chartPoints = loadChartPointsForPeriod(_uiState.value.selectedPeriod, enrichedPortfolio)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    portfolio = enrichedPortfolio,
                    holdings = liveHoldings,
                    chartPoints = chartPoints
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load portfolio"
                )
            }
        }
    }

    fun onPeriodSelected(period: String) {
        viewModelScope.launch {
            val currentPortfolio = _uiState.value.portfolio ?: return@launch
            val chartPoints = loadChartPointsForPeriod(period, currentPortfolio)
            _uiState.value = _uiState.value.copy(
                selectedPeriod = period,
                chartPoints = chartPoints
            )
        }
    }

    fun executeSell(
        yahooSymbol: String,
        quantity: Int,
        price: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = tradeRepository.executeSell(
                portfolioId = portfolioId,
                yahooSymbol = yahooSymbol,
                quantity = quantity,
                price = price
            )
            if (res.isSuccess) {
                onSuccess()
                loadPortfolio()
            } else {
                onError(res.exceptionOrNull()?.message ?: "Failed to execute sell")
            }
        }
    }

    fun executeBuyMore(
        yahooSymbol: String,
        quantity: Int,
        price: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = tradeRepository.executeBuy(
                portfolioId = portfolioId,
                yahooSymbol = yahooSymbol,
                quantity = quantity,
                price = price
            )
            if (res.isSuccess) {
                onSuccess()
                loadPortfolio()
            } else {
                onError(res.exceptionOrNull()?.message ?: "Failed to execute buy")
            }
        }
    }

    private suspend fun loadChartPointsForPeriod(period: String, portfolio: Portfolio): List<ChartPoint> {
        val snapshots = snapshotDao.getSnapshotsForPortfolio(portfolioId)
        val points = mutableListOf<ChartPoint>()

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())

        val cal = Calendar.getInstance()
        val now = Date()

        val startDate: Date = when (period) {
            "1D" -> cal.apply { add(Calendar.DAY_OF_YEAR, -1) }.time
            "1W" -> cal.apply { add(Calendar.DAY_OF_YEAR, -7) }.time
            "1M" -> cal.apply { add(Calendar.MONTH, -1) }.time
            "6M" -> cal.apply { add(Calendar.MONTH, -6) }.time
            "1Y" -> cal.apply { add(Calendar.YEAR, -1) }.time
            "3Y" -> cal.apply { add(Calendar.YEAR, -3) }.time
            "5Y" -> cal.apply { add(Calendar.YEAR, -5) }.time
            else -> Date(0) // ALL
        }

        // Available History Scaling: If snapshots are younger than filter, show all available
        val filtered = snapshots.filter { s ->
            try {
                val d = formatter.parse(s.date) ?: now
                d.after(startDate) || d == startDate
            } catch (e: Exception) {
                true
            }
        }

        val effectiveSnapshots = if (filtered.isNotEmpty()) filtered else snapshots

        for (s in effectiveSnapshots) {
            val time = try { formatter.parse(s.date)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
            points.add(
                ChartPoint(
                    timestamp = time,
                    price = s.totalValue,
                    formattedTime = try { displayFormatter.format(Date(time)) } catch (e: Exception) { s.date }
                )
            )
        }

        // If today's current point isn't appended, add it
        if (points.isEmpty() || points.last().price != portfolio.totalNetWorth) {
            points.add(
                ChartPoint(
                    timestamp = System.currentTimeMillis(),
                    price = portfolio.totalNetWorth,
                    formattedTime = "Now"
                )
            )
        }

        return points
    }
}
