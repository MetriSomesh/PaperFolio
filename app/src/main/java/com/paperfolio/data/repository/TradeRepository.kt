package com.paperfolio.data.repository

import androidx.room.withTransaction
import com.paperfolio.data.local.AppDatabase
import com.paperfolio.data.local.entity.TradeEntity
import com.paperfolio.domain.model.StockHolding
import com.paperfolio.domain.model.Trade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TradeRepository(
    private val database: AppDatabase
) {
    private val tradeDao get() = database.tradeDao()
    private val portfolioDao get() = database.portfolioDao()
    private val stockMasterDao get() = database.stockMasterDao()

    fun getTradesForPortfolioFlow(portfolioId: Long): Flow<List<Trade>> {
        return tradeDao.getTradesForPortfolio(portfolioId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun executeBuy(
        portfolioId: Long,
        yahooSymbol: String,
        quantity: Int,
        price: Double,
        notes: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (quantity <= 0) return@withContext Result.failure(Exception("Quantity must be greater than 0"))
        if (price <= 0.0) return@withContext Result.failure(Exception("Price must be greater than 0"))

        try {
            val totalCost = quantity * price
            // Balance check + cash deduction + trade insert run in ONE
            // transaction: the check sees the same snapshot the write
            // uses, and if the app is killed mid-write both roll back
            // together — cash and holdings can never disagree.
            val tradeId = database.withTransaction {
                val portfolio = portfolioDao.getPortfolioByIdOnce(portfolioId)
                    ?: throw IllegalStateException("Portfolio not found")
                if (portfolio.cashBalance < totalCost) {
                    throw IllegalStateException(
                        "Insufficient cash balance! Required: ₹${String.format("%.2f", totalCost)}, " +
                            "Available: ₹${String.format("%.2f", portfolio.cashBalance)}"
                    )
                }
                portfolioDao.updateCashBalance(portfolioId, portfolio.cashBalance - totalCost)
                tradeDao.insertTrade(
                    TradeEntity(
                        portfolioId = portfolioId,
                        yahooSymbol = yahooSymbol,
                        type = "BUY",
                        quantity = quantity,
                        price = price,
                        timestamp = System.currentTimeMillis(),
                        notes = notes
                    )
                )
            }
            Result.success(tradeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeSell(
        portfolioId: Long,
        yahooSymbol: String,
        quantity: Int,
        price: Double,
        notes: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (quantity <= 0) return@withContext Result.failure(Exception("Quantity must be greater than 0"))
        if (price <= 0.0) return@withContext Result.failure(Exception("Price must be greater than 0"))

        try {
            val totalRevenue = quantity * price
            // Holdings check + cash refund + trade insert in ONE
            // transaction (same atomicity guarantee as executeBuy).
            val tradeId = database.withTransaction {
                val portfolio = portfolioDao.getPortfolioByIdOnce(portfolioId)
                    ?: throw IllegalStateException("Portfolio not found")

                val trades = tradeDao.getTradesForStock(portfolioId, yahooSymbol)
                var totalBoughtQty = 0
                var totalBoughtCost = 0.0
                var totalSoldQty = 0
                for (t in trades) {
                    if (t.type == "BUY") {
                        totalBoughtQty += t.quantity
                        totalBoughtCost += (t.quantity * t.price)
                    } else if (t.type == "SELL") {
                        totalSoldQty += t.quantity
                    }
                }
                val availableQty = totalBoughtQty - totalSoldQty
                if (quantity > availableQty) {
                    throw IllegalStateException(
                        "Cannot sell $quantity shares! You only hold $availableQty shares."
                    )
                }
                val avgBuyPrice = if (totalBoughtQty > 0) totalBoughtCost / totalBoughtQty else price
                val realizedPnl = (price - avgBuyPrice) * quantity

                portfolioDao.updateCashBalance(portfolioId, portfolio.cashBalance + totalRevenue)
                tradeDao.insertTrade(
                    TradeEntity(
                        portfolioId = portfolioId,
                        yahooSymbol = yahooSymbol,
                        type = "SELL",
                        quantity = quantity,
                        price = price,
                        timestamp = System.currentTimeMillis(),
                        realizedPnl = realizedPnl,
                        notes = notes
                    )
                )
            }
            Result.success(tradeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compute raw holdings without live prices
     */
    suspend fun getHoldings(portfolioId: Long): List<StockHolding> = withContext(Dispatchers.IO) {
        val trades = tradeDao.getTradesForPortfolioList(portfolioId)
        val grouped = trades.groupBy { it.yahooSymbol }
        val holdings = mutableListOf<StockHolding>()

        for ((symbol, tradeList) in grouped) {
            var buyQty = 0
            var buyCost = 0.0
            var sellQty = 0

            for (t in tradeList) {
                if (t.type == "BUY") {
                    buyQty += t.quantity
                    buyCost += (t.quantity * t.price)
                } else if (t.type == "SELL") {
                    sellQty += t.quantity
                }
            }

            val currentQty = buyQty - sellQty
            if (currentQty > 0) {
                val avgPrice = if (buyQty > 0) buyCost / buyQty else 0.0
                val invested = currentQty * avgPrice
                val info = stockMasterDao.getStockByYahooSymbol(symbol)

                val tradingSym = info?.tradingSymbol ?: symbol.substringBefore(".")
                val compName = info?.companyName ?: tradingSym
                val exch = info?.exchange ?: if (symbol.endsWith(".NS")) "NSE" else "BSE"

                holdings.add(
                    StockHolding(
                        yahooSymbol = symbol,
                        tradingSymbol = tradingSym,
                        companyName = compName,
                        exchange = exch,
                        quantity = currentQty,
                        averagePrice = avgPrice,
                        investedAmount = invested,
                        currentPrice = avgPrice, // Default until live price is fetched
                        currentValue = invested,
                        pnl = 0.0,
                        pnlPercent = 0.0
                    )
                )
            }
        }
        holdings
    }

    private fun TradeEntity.toDomain(): Trade {
        val sym = this.yahooSymbol.substringBefore(".")
        return Trade(
            id = this.id,
            portfolioId = this.portfolioId,
            yahooSymbol = this.yahooSymbol,
            tradingSymbol = sym,
            companyName = sym,
            type = this.type,
            quantity = this.quantity,
            price = this.price,
            timestamp = this.timestamp,
            realizedPnl = this.realizedPnl,
            notes = this.notes
        )
    }
}
