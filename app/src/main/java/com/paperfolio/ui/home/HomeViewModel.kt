package com.paperfolio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperfolio.data.repository.PortfolioRepository
import com.paperfolio.domain.model.Portfolio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun createPortfolio(
        name: String,
        initialBalance: Double,
        description: String = "",
        colorTag: String = "#00D09C",
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = portfolioRepository.createPortfolio(name, initialBalance, description, colorTag)
            onSuccess(id)
        }
    }

    fun deletePortfolio(portfolioId: Long) {
        viewModelScope.launch {
            portfolioRepository.deletePortfolio(portfolioId)
        }
    }
}
