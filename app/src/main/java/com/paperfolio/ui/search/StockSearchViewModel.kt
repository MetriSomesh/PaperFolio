package com.paperfolio.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperfolio.data.local.entity.StockMasterEntity
import com.paperfolio.data.repository.StockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StockSearchUiState(
    val query: String = "",
    val selectedExchange: String = "ALL", // "ALL", "NSE", "BSE", "ETF"
    val results: List<StockMasterEntity> = emptyList(),
    val isSearching: Boolean = false,
    val isShowingPopular: Boolean = true
)

class StockSearchViewModel(
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockSearchUiState())
    val uiState: StateFlow<StockSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialPopular()
    }

    private fun loadInitialPopular() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val popular = stockRepository.getPopularStocks()
            _uiState.value = _uiState.value.copy(
                results = popular,
                isSearching = false,
                isShowingPopular = true
            )
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            loadInitialPopular()
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, isShowingPopular = false)
            delay(150) // Small debounce for smooth typing

            val allMatches = stockRepository.searchStocks(newQuery)
            val filtered = filterByExchange(allMatches, _uiState.value.selectedExchange)

            _uiState.value = _uiState.value.copy(
                results = filtered,
                isSearching = false,
                isShowingPopular = false
            )
        }
    }

    fun onExchangeFilterSelected(exchange: String) {
        _uiState.value = _uiState.value.copy(selectedExchange = exchange)
        if (_uiState.value.query.isBlank()) {
            viewModelScope.launch {
                val popular = stockRepository.getPopularStocks()
                val filtered = filterByExchange(popular, exchange)
                _uiState.value = _uiState.value.copy(results = filtered, isShowingPopular = true)
            }
        } else {
            onQueryChanged(_uiState.value.query)
        }
    }

    private fun filterByExchange(stocks: List<StockMasterEntity>, exchange: String): List<StockMasterEntity> {
        return when (exchange) {
            "NSE" -> stocks.filter { it.exchange == "NSE" && !isEtf(it) }
            "BSE" -> stocks.filter { it.exchange == "BSE" }
            "ETF" -> stocks.filter { isEtf(it) }
            else -> stocks
        }
    }

    private fun isEtf(stock: StockMasterEntity): Boolean {
        return (stock.sector?.contains("ETF", ignoreCase = true) == true) ||
                stock.companyName.contains("ETF", ignoreCase = true) ||
                stock.tradingSymbol.endsWith("BEES", ignoreCase = true)
    }
}
