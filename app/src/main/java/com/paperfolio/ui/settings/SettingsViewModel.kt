package com.paperfolio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperfolio.data.local.dao.StockMasterDao
import com.paperfolio.data.local.entity.StockMasterEntity
import com.paperfolio.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val totalStockCount: Int = 0,
    val isAddingStock: Boolean = false,
    val addStockSuccessMessage: String? = null,
    val addStockErrorMessage: String? = null
)

class SettingsViewModel(
    private val stockMasterDao: StockMasterDao,
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStockCount()
    }

    private fun loadStockCount() {
        viewModelScope.launch {
            val count = stockMasterDao.getStockCount()
            _uiState.value = _uiState.value.copy(totalStockCount = count)
        }
    }

    fun addNewStock(symbol: String, exchange: String) {
        if (symbol.isBlank()) {
            _uiState.value = _uiState.value.copy(addStockErrorMessage = "Please enter a stock symbol")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAddingStock = true,
                addStockSuccessMessage = null,
                addStockErrorMessage = null
            )

            val result = stockRepository.validateAndAddStock(symbol, exchange)
            if (result.isSuccess) {
                val added = result.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isAddingStock = false,
                    addStockSuccessMessage = "Successfully added ${added?.companyName} (${added?.yahooSymbol})! It is now searchable and tradeable.",
                    addStockErrorMessage = null
                )
                loadStockCount()
            } else {
                _uiState.value = _uiState.value.copy(
                    isAddingStock = false,
                    addStockErrorMessage = "Could not validate stock with Yahoo Finance: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            addStockSuccessMessage = null,
            addStockErrorMessage = null
        )
    }
}
