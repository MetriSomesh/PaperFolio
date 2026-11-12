package com.paperfolio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperfolio.data.repository.PortfolioRepository
import com.paperfolio.domain.model.Portfolio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    val portfolios: StateFlow<List<Portfolio>> = portfolioRepository.portfoliosFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Was: create/delete launched bare coroutines, so a database error
    // crashed the app (uncaught exception in viewModelScope) and a
    // failed create silently left the dialog open. Failures now surface
    // as a message the screen can show, and the dialog stays open.
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun createPortfolio(
        name: String,
        initialBalance: Double,
        description: String = "",
        colorTag: String = "#00916E",
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = portfolioRepository.createPortfolio(name, initialBalance, description, colorTag)
                onSuccess(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not create portfolio"
            }
        }
    }

    fun deletePortfolio(portfolioId: Long) {
        viewModelScope.launch {
            try {
                portfolioRepository.deletePortfolio(portfolioId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not delete portfolio"
            }
        }
    }
}
