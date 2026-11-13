package com.paperfolio

import com.paperfolio.data.local.entity.TradeEntity
import com.paperfolio.data.repository.FifoPnl
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression test for the FIFO P&L bug reported Nov 2026:
 *
 * buy 10 @ ₹100, sell 10 @ ₹120, buy 10 @ ₹200, sell 10 @ ₹210
 * showed +₹600 on the second sell (average of ALL buys ever: ₹150)
 * instead of the true +₹100 (the only lot still open was 10 @ ₹200),
 * and the re-bought holding showed avg ₹150 / invested ₹1,500 instead
 * of ₹200 / ₹2,000.
 *
 * Pure JVM test — FifoPnl is pure computation over TradeEntity rows,
 * no database or Robolectric needed.
 */
class FifoPnlTest {

    private fun trade(
        id: Long,
        type: String,
        quantity: Int,
        price: Double,
        symbol: String = "RELIANCE.NS",
        portfolioId: Long = 1L,
        timestamp: Long = id * 1000L
    ) = TradeEntity(
        id = id,
        portfolioId = portfolioId,
        yahooSymbol = symbol,
        type = type,
        quantity = quantity,
        price = price,
        timestamp = timestamp
    )

    @Test
    fun reportedSequence_secondSellIs100Not600() {
        val trades = listOf(
            trade(1, "BUY", 10, 100.0),
            trade(2, "SELL", 10, 120.0),
            trade(3, "BUY", 10, 200.0),
            trade(4, "SELL", 10, 210.0)
        )
        val realized = FifoPnl.realizedPnlByTradeId(trades)
        assertEquals(200.0, realized[2L]!!, 0.001)
        assertEquals(100.0, realized[4L]!!, 0.001)
        assertEquals(300.0, realized.values.sum(), 0.001)
    }

    @Test
    fun reportedSequence_reboughtHoldingAverages200Not150() {
        val trades = listOf(
            trade(1, "BUY", 10, 100.0),
            trade(2, "SELL", 10, 120.0),
            trade(3, "BUY", 10, 200.0)
        )
        val holding = FifoPnl.holdings(trades)["RELIANCE.NS"]!!
        assertEquals(10, holding.quantity)
        assertEquals(200.0, holding.averagePrice, 0.001)
        assertEquals(2000.0, holding.investedAmount, 0.001)
    }

    @Test
    fun partialSellConsumesOldestLotFirst() {
        val trades = listOf(
            trade(1, "BUY", 10, 100.0),
            trade(2, "BUY", 10, 200.0),
            trade(3, "SELL", 15, 210.0)
        )
        // 10 @ (210-100) + 5 @ (210-200) = 1100 + 50
        val realized = FifoPnl.realizedPnlByTradeId(trades)
        assertEquals(1150.0, realized[3L]!!, 0.001)
        val holding = FifoPnl.holdings(trades)["RELIANCE.NS"]!!
        assertEquals(5, holding.quantity)
        assertEquals(200.0, holding.averagePrice, 0.001)
    }

    @Test
    fun realizedForSell_matchesReportedSequence() {
        val prior = listOf(
            trade(1, "BUY", 10, 100.0),
            trade(2, "SELL", 10, 120.0),
            trade(3, "BUY", 10, 200.0)
        )
        assertEquals(100.0, FifoPnl.realizedForSell(prior, 10, 210.0), 0.001)
        assertEquals(10, FifoPnl.availableQuantity(prior))
    }

    @Test
    fun lotsNeverLeakAcrossSymbolsOrPortfolios() {
        val trades = listOf(
            trade(1, "BUY", 10, 100.0, symbol = "RELIANCE.NS"),
            trade(2, "BUY", 10, 50.0, symbol = "TCS.NS"),
            trade(3, "SELL", 10, 120.0, symbol = "TCS.NS"),
            trade(4, "BUY", 10, 100.0, symbol = "RELIANCE.NS", portfolioId = 2L),
            trade(5, "SELL", 10, 90.0, symbol = "RELIANCE.NS", portfolioId = 2L)
        )
        val realized = FifoPnl.realizedPnlByTradeId(trades)
        assertEquals(700.0, realized[3L]!!, 0.001) // TCS: 10 @ (120-50)
        assertEquals(-100.0, realized[5L]!!, 0.001) // portfolio 2 RELIANCE: 10 @ (90-100)
        // Portfolio 1 RELIANCE still holds its 10 @ 100 untouched.
        val p1 = FifoPnl.holdings(trades.filter { it.portfolioId == 1L })
        assertEquals(100.0, p1["RELIANCE.NS"]!!.averagePrice, 0.001)
    }

    @Test
    fun fullyExitedHoldingDisappears() {
        val trades = listOf(
            trade(1, "BUY", 10, 100.0),
            trade(2, "SELL", 10, 120.0)
        )
        assertEquals(0, FifoPnl.holdings(trades).size)
        assertEquals(0, FifoPnl.availableQuantity(trades))
    }
}
