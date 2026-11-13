package com.paperfolio.data.repository

import com.paperfolio.data.local.entity.TradeEntity

/**
 * FIFO cost-basis accounting for PaperFolio.
 *
 * The bug this replaces (reported Nov 2026): [TradeRepository.executeSell]
 * computed the average buy price over *all* buys ever made in a stock,
 * ignoring sells that had already closed earlier lots, and
 * [TradeRepository.getHoldings] did the same for the open position.
 * So buy 10 @ ₹100, sell 10 @ ₹120, buy 10 @ ₹200, sell 10 @ ₹210
 * showed +₹600 on the second sell (avg ₹150) instead of the true +₹100
 * (the only lot still open was the 10 @ ₹200), and the re-bought holding
 * showed an average of ₹150 / invested ₹1,500 instead of ₹200 / ₹2,000.
 *
 * FIFO walks trades oldest-first (timestamp, then id as the tie-break
 * for trades in the same millisecond) and matches each sell against the
 * oldest still-open buy lots. Everything here is pure computation over
 * [TradeEntity] rows, so it is unit-testable on the JVM with no database.
 */
object FifoPnl {

    /** One open buy lot: [quantity] shares still held at [price]. */
    data class Lot(var quantity: Int, val price: Double)

    /** An open position derived from the remaining FIFO lots. */
    data class Holding(
        val quantity: Int,
        val averagePrice: Double,
        val investedAmount: Double
    )

    private fun ordered(trades: List<TradeEntity>): List<TradeEntity> =
        trades.sortedWith(compareBy({ it.timestamp }, { it.id }))

    /**
     * Remaining open lots per stock symbol, after replaying [trades]
     * (one portfolio's trades) oldest-first. A sell consumes the oldest
     * lots first; a sell larger than the open lots consumes what exists
     * and stops (callers that must reject oversells check quantity first).
     */
    fun remainingLots(trades: List<TradeEntity>): Map<String, List<Lot>> {
        val lotsBySymbol = mutableMapOf<String, ArrayDeque<Lot>>()
        for (t in ordered(trades)) {
            val queue = lotsBySymbol.getOrPut(t.yahooSymbol) { ArrayDeque() }
            when (t.type) {
                "BUY" -> queue.addLast(Lot(t.quantity, t.price))
                "SELL" -> {
                    var remaining = t.quantity
                    while (remaining > 0 && queue.isNotEmpty()) {
                        val lot = queue.first()
                        val take = minOf(remaining, lot.quantity)
                        lot.quantity -= take
                        remaining -= take
                        if (lot.quantity == 0) queue.removeFirst()
                    }
                }
            }
        }
        return lotsBySymbol.mapValues { (_, queue) -> queue.toList() }
    }

    /** Open position per symbol: quantity, FIFO average price, invested cost. */
    fun holdings(trades: List<TradeEntity>): Map<String, Holding> {
        val out = mutableMapOf<String, Holding>()
        for ((symbol, lots) in remainingLots(trades)) {
            val qty = lots.sumOf { it.quantity }
            if (qty > 0) {
                val cost = lots.sumOf { it.quantity * it.price }
                out[symbol] = Holding(
                    quantity = qty,
                    averagePrice = cost / qty,
                    investedAmount = cost
                )
            }
        }
        return out
    }

    /** Shares currently held in [trades] for one stock (open lots only). */
    fun availableQuantity(trades: List<TradeEntity>): Int =
        remainingLots(trades).values.flatten().sumOf { it.quantity }

    /**
     * Realized P&L of selling [quantity] shares at [price], matched FIFO
     * against the open lots in [priorTrades] (trades before this sell).
     */
    fun realizedForSell(priorTrades: List<TradeEntity>, quantity: Int, price: Double): Double {
        var remaining = quantity
        var realized = 0.0
        for (lot in remainingLots(priorTrades).values.flatten()) {
            if (remaining <= 0) break
            val take = minOf(remaining, lot.quantity)
            realized += (price - lot.price) * take
            remaining -= take
        }
        return realized
    }

    /**
     * Realized P&L per SELL trade id, replaying the full history FIFO.
     * Keyed by (portfolio, symbol) so one stock's lots never absorb
     * another's, and so a sell is always matched against the lots that
     * were actually open when it happened. Used by the P&L screen to
     * correct the `realizedPnl` stored on old SELL rows, which was
     * computed with the broken all-buys average.
     */
    fun realizedPnlByTradeId(trades: List<TradeEntity>): Map<Long, Double> {
        val lotsByKey = mutableMapOf<Pair<Long, String>, ArrayDeque<Lot>>()
        val out = mutableMapOf<Long, Double>()
        for (t in ordered(trades)) {
            val queue = lotsByKey.getOrPut(t.portfolioId to t.yahooSymbol) { ArrayDeque() }
            when (t.type) {
                "BUY" -> queue.addLast(Lot(t.quantity, t.price))
                "SELL" -> {
                    var remaining = t.quantity
                    var realized = 0.0
                    while (remaining > 0 && queue.isNotEmpty()) {
                        val lot = queue.first()
                        val take = minOf(remaining, lot.quantity)
                        realized += (t.price - lot.price) * take
                        lot.quantity -= take
                        remaining -= take
                        if (lot.quantity == 0) queue.removeFirst()
                    }
                    out[t.id] = realized
                }
            }
        }
        return out
    }
}
