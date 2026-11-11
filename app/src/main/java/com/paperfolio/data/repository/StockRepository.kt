package com.paperfolio.data.repository

import com.paperfolio.data.local.dao.StockMasterDao
import com.paperfolio.data.local.entity.StockMasterEntity
import com.paperfolio.data.remote.YahooChartResponse
import com.paperfolio.data.remote.YahooFinanceApi
import com.paperfolio.domain.model.ChartPoint
import com.paperfolio.domain.model.StockDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockRepository(
    private val stockMasterDao: StockMasterDao,
    private val yahooApi: YahooFinanceApi
) {

    suspend fun searchStocks(query: String): List<StockMasterEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        stockMasterDao.searchStocks(query.trim())
    }

    suspend fun getPopularStocks(): List<StockMasterEntity> = withContext(Dispatchers.IO) {
        val popularSymbols = listOf(
            "RELIANCE.NS", "TCS.NS", "HDFCBANK.NS", "INFY.NS", "ICICIBANK.NS",
            "TATAMOTORS.NS", "ITC.NS", "SBIN.NS", "BHARTIARTL.NS", "NIFTYBEES.NS",
            "GOLDBEES.NS", "BANKBEES.NS"
        )
        val stocks = stockMasterDao.getStocksBySymbols(popularSymbols)
        if (stocks.isNotEmpty()) stocks else stockMasterDao.searchStocks("A", limit = 10)
    }

    suspend fun getStockByYahooSymbol(yahooSymbol: String): StockMasterEntity? = withContext(Dispatchers.IO) {
        stockMasterDao.getStockByYahooSymbol(yahooSymbol)
    }

    suspend fun getStockDetail(
        yahooSymbol: String,
        range: String = "1d",
        interval: String = "5m"
    ): Result<StockDetail> = withContext(Dispatchers.IO) {
        try {
            val response = yahooApi.getChartData(symbol = yahooSymbol, range = range, interval = interval)
            val chartResult = response.chart?.result?.firstOrNull()
                ?: return@withContext Result.failure(Exception("Stock data not found for $yahooSymbol"))

            val meta = chartResult.meta ?: return@withContext Result.failure(Exception("No metadata for $yahooSymbol"))
            val localInfo = stockMasterDao.getStockByYahooSymbol(yahooSymbol)

            val currentPrice = meta.regularMarketPrice ?: 0.0
            val prevClose = meta.chartPreviousClose ?: meta.previousClose ?: currentPrice
            val change = meta.fulldayChange ?: (currentPrice - prevClose)
            val changePercent = meta.regularMarketChangePercent ?: if (prevClose > 0) ((change / prevClose) * 100) else 0.0

            val timestamps = chartResult.timestamp ?: emptyList()
            val closes = chartResult.indicators?.quote?.firstOrNull()?.close ?: emptyList()

            val timeFormatter = when (range) {
                "1d" -> SimpleDateFormat("HH:mm", Locale.getDefault())
                "5d", "1mo" -> SimpleDateFormat("dd MMM", Locale.getDefault())
                else -> SimpleDateFormat("MMM yyyy", Locale.getDefault())
            }

            val chartPoints = mutableListOf<ChartPoint>()
            for (i in timestamps.indices) {
                val time = timestamps[i]
                val closeVal = closes.getOrNull(i)
                if (closeVal != null) {
                    chartPoints.add(
                        ChartPoint(
                            timestamp = time * 1000,
                            price = closeVal,
                            formattedTime = timeFormatter.format(Date(time * 1000))
                        )
                    )
                }
            }

            val tradingSym = localInfo?.tradingSymbol ?: yahooSymbol.substringBefore(".")
            val compName = localInfo?.companyName ?: meta.longName ?: meta.shortName ?: tradingSym
            val exch = localInfo?.exchange ?: if (yahooSymbol.endsWith(".NS")) "NSE" else "BSE"

            Result.success(
                StockDetail(
                    yahooSymbol = yahooSymbol,
                    tradingSymbol = tradingSym,
                    companyName = compName,
                    exchange = exch,
                    currentPrice = currentPrice,
                    change = change,
                    changePercent = changePercent,
                    prevClose = prevClose,
                    dayHigh = meta.regularMarketDayHigh ?: currentPrice,
                    dayLow = meta.regularMarketDayLow ?: currentPrice,
                    fiftyTwoWeekHigh = meta.fiftyTwoWeekHigh ?: currentPrice,
                    fiftyTwoWeekLow = meta.fiftyTwoWeekLow ?: currentPrice,
                    volume = meta.regularMarketVolume ?: 0L,
                    chartPoints = chartPoints
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch fetch live prices for a list of symbols in parallel
     */
    suspend fun getBatchQuotes(symbols: List<String>): Map<String, StockQuote> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyMap()

        coroutineScope {
            symbols.distinct().map { sym ->
                async {
                    try {
                        val response = yahooApi.getChartData(symbol = sym, range = "1d", interval = "1d")
                        val meta = response.chart?.result?.firstOrNull()?.meta
                        if (meta?.regularMarketPrice != null) {
                            val price = meta.regularMarketPrice
                            val prev = meta.chartPreviousClose ?: meta.previousClose ?: price
                            val chg = price - prev
                            val pct = if (prev > 0) (chg / prev) * 100 else 0.0
                            sym to StockQuote(
                                price = price,
                                dayChange = chg,
                                dayChangePercent = pct
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }

    /**
     * User can add newly listed stocks by symbol
     */
    suspend fun validateAndAddStock(symbolInput: String, exchange: String): Result<StockMasterEntity> = withContext(Dispatchers.IO) {
        val cleanSym = symbolInput.trim().uppercase()
        val yahooSymbol = if (cleanSym.endsWith(".NS") || cleanSym.endsWith(".BO")) {
            cleanSym
        } else {
            if (exchange.equals("BSE", ignoreCase = true)) "$cleanSym.BO" else "$cleanSym.NS"
        }
        val exch = if (yahooSymbol.endsWith(".BO")) "BSE" else "NSE"
        val tradingSymbol = yahooSymbol.substringBefore(".")

        try {
            val response = yahooApi.getChartData(symbol = yahooSymbol, range = "1d", interval = "1d")
            val meta = response.chart?.result?.firstOrNull()?.meta
                ?: return@withContext Result.failure(Exception("Stock not found on Yahoo Finance for $yahooSymbol"))

            val companyName = meta.longName ?: meta.shortName ?: tradingSymbol
            val entity = StockMasterEntity(
                yahooSymbol = yahooSymbol,
                tradingSymbol = tradingSymbol,
                companyName = companyName,
                exchange = exch,
                sector = null,
                isCustom = true
            )
            stockMasterDao.insertStock(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class StockQuote(
    val price: Double,
    val dayChange: Double,
    val dayChangePercent: Double
)
