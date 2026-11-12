package com.paperfolio.data.repository

import androidx.room.withTransaction
import com.paperfolio.data.local.AppDatabase
import com.paperfolio.data.local.entity.PortfolioEntity
import com.paperfolio.data.local.entity.PortfolioSnapshotEntity
import com.paperfolio.domain.model.Portfolio
import com.paperfolio.domain.model.StockHolding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PortfolioRepository(
    private val database: AppDatabase,
    private val tradeRepository: TradeRepository,
    private val stockRepository: StockRepository
) {
    private val portfolioDao get() = database.portfolioDao()
    private val snapshotDao get() = database.portfolioSnapshotDao()

    val portfoliosFlow: Flow<List<Portfolio>> = portfolioDao.getAllPortfolios().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllPortfolios(): List<Portfolio> = withContext(Dispatchers.IO) {
        portfolioDao.getAllPortfoliosList().map { it.toDomain() }
    }

    suspend fun getPortfolioById(id: Long): Portfolio? = withContext(Dispatchers.IO) {
        portfolioDao.getPortfolioByIdOnce(id)?.toDomain()
    }

    suspend fun createPortfolio(
        name: String,
        initialBalance: Double,
        description: String = "",
        colorTag: String = "#00916E"
    ): Long = withContext(Dispatchers.IO) {
        // Portfolio row + initial snapshot in ONE transaction: a kill
        // between the two used to leave a portfolio with no snapshot
        // (and a snapshot insert failure used to surface as a crash
        // after the portfolio was already visible).
        database.withTransaction {
            val entity = PortfolioEntity(
                name = name.trim(),
                initialBalance = initialBalance,
                cashBalance = initialBalance,
                description = description.trim(),
                colorTag = colorTag
            )
            val id = portfolioDao.insertPortfolio(entity)

            // Record initial snapshot
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            snapshotDao.insertSnapshot(
                PortfolioSnapshotEntity(
                    portfolioId = id,
                    date = today,
                    totalValue = initialBalance,
                    investedAmount = 0.0,
                    cashBalance = initialBalance
                )
            )
            id
        }
    }

    suspend fun updatePortfolio(portfolio: Portfolio) = withContext(Dispatchers.IO) {
        val existing = portfolioDao.getPortfolioByIdOnce(portfolio.id) ?: return@withContext
        portfolioDao.updatePortfolio(
            existing.copy(
                name = portfolio.name,
                description = portfolio.description,
                colorTag = portfolio.colorTag
            )
        )
    }

    suspend fun deletePortfolio(id: Long) = withContext(Dispatchers.IO) {
        val existing = portfolioDao.getPortfolioByIdOnce(id) ?: return@withContext
        portfolioDao.deletePortfolio(existing)
    }

    /**
     * Called ONLY when portfolio detail screen is opened.
     * Fetches current prices for all held stocks in this portfolio.
     */
    suspend fun getPortfolioDetailsWithLivePrices(portfolioId: Long): Pair<Portfolio, List<StockHolding>> = withContext(Dispatchers.IO) {
        val portfolioEntity = portfolioDao.getPortfolioByIdOnce(portfolioId)
            ?: throw Exception("Portfolio not found")

        val rawHoldings = tradeRepository.getHoldings(portfolioId)
        if (rawHoldings.isEmpty()) {
            val emptyPortfolio = portfolioEntity.toDomain(invested = 0.0, current = 0.0, count = 0)
            return@withContext Pair(emptyPortfolio, emptyList())
        }

        // Fetch live quotes for held stocks
        val symbols = rawHoldings.map { it.yahooSymbol }
        val liveQuotes = stockRepository.getBatchQuotes(symbols)

        var totalInvested = 0.0
        var totalCurrentVal = 0.0

        val liveHoldings = rawHoldings.map { holding ->
            val quote = liveQuotes[holding.yahooSymbol]
            val currentPrice = quote?.price ?: holding.averagePrice
            val currentVal = holding.quantity * currentPrice
            val pnl = currentVal - holding.investedAmount
            val pnlPct = if (holding.investedAmount > 0) (pnl / holding.investedAmount) * 100 else 0.0

            totalInvested += holding.investedAmount
            totalCurrentVal += currentVal

            holding.copy(
                currentPrice = currentPrice,
                currentValue = currentVal,
                pnl = pnl,
                pnlPercent = pnlPct,
                dayChange = quote?.dayChange ?: 0.0,
                dayChangePercent = quote?.dayChangePercent ?: 0.0
            )
        }

        val enrichedPortfolio = portfolioEntity.toDomain(
            invested = totalInvested,
            current = totalCurrentVal,
            count = liveHoldings.size
        )

        Pair(enrichedPortfolio, liveHoldings)
    }

    private fun PortfolioEntity.toDomain(
        invested: Double = 0.0,
        current: Double = 0.0,
        count: Int = 0
    ): Portfolio {
        val pnl = current - invested
        val pnlPct = if (invested > 0) (pnl / invested) * 100 else 0.0
        return Portfolio(
            id = this.id,
            name = this.name,
            createdAt = this.createdAt,
            description = this.description,
            colorTag = this.colorTag,
            initialBalance = this.initialBalance,
            cashBalance = this.cashBalance,
            investedAmount = invested,
            currentValue = current,
            totalPnl = pnl,
            totalPnlPercent = pnlPct,
            holdingsCount = count
        )
    }
}
