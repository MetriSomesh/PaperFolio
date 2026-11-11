package com.paperfolio.ui.stockdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperfolio.data.repository.PortfolioRepository
import com.paperfolio.data.repository.StockRepository
import com.paperfolio.data.repository.TradeRepository
import com.paperfolio.domain.model.Portfolio
import com.paperfolio.domain.model.StockDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StockDetailUiState(
    val isLoading: Boolean = true,
    val stockDetail: StockDetail? = null,
    val selectedRange: String = "1d",
    val errorMessage: String? = null
)

class StockDetailViewModel(
    private val yahooSymbol: String,
    private val stockRepository: StockRepository,
    private val portfolioRepository: PortfolioRepository,
    private val tradeRepository: TradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    val portfolios: StateFlow<List<Portfolio>> = portfolioRepository.portfoliosFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadStockData("1d")
    }

    fun loadStockData(range: String) {
        val interval = when (range) {
            "1d" -> "5m"
            "5d" -> "15m"
            "1mo" -> "1d"
            "6mo" -> "1d"
            "1y" -> "1d"
            "5y" -> "1wk"
            else -> "1mo"
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedRange = range, errorMessage = null)
            val result = stockRepository.getStockDetail(yahooSymbol, range = range, interval = interval)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stockDetail = result.getOrNull()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load stock data"
                )
            }
        }
    }

    fun executeBuy(
        portfolioId: Long,
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
            } else {
                onError(res.exceptionOrNull()?.message ?: "Failed to execute buy")
            }
        }
    }
}
