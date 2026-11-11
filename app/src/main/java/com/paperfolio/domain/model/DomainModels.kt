package com.paperfolio.domain.model

data class Portfolio(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val description: String,
    val colorTag: String,
    val initialBalance: Double,
    val cashBalance: Double,
    val investedAmount: Double = 0.0,
    val currentValue: Double = 0.0,
    val totalPnl: Double = 0.0,
    val totalPnlPercent: Double = 0.0,
    val holdingsCount: Int = 0
) {
    val totalNetWorth: Double
        get() = cashBalance + currentValue
}

data class StockHolding(
    val yahooSymbol: String,
    val tradingSymbol: String,
    val companyName: String,
    val exchange: String,
    val quantity: Int,
    val averagePrice: Double,
    val investedAmount: Double,
    val currentPrice: Double,
    val currentValue: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val dayChange: Double = 0.0,
    val dayChangePercent: Double = 0.0
)

data class ChartPoint(
    val timestamp: Long,
    val price: Double,
    val formattedTime: String = ""
)

data class StockDetail(
    val yahooSymbol: String,
    val tradingSymbol: String,
    val companyName: String,
    val exchange: String,
    val currentPrice: Double,
    val change: Double,
    val changePercent: Double,
    val prevClose: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val fiftyTwoWeekHigh: Double,
    val fiftyTwoWeekLow: Double,
    val volume: Long,
    val chartPoints: List<ChartPoint> = emptyList()
)

data class Trade(
    val id: Long,
    val portfolioId: Long,
    val yahooSymbol: String,
    val tradingSymbol: String,
    val companyName: String,
    val type: String, // "BUY" or "SELL"
    val quantity: Int,
    val price: Double,
    val timestamp: Long,
    val realizedPnl: Double? = null,
    val notes: String? = null
) {
    val totalAmount: Double
        get() = quantity * price
}
