package com.paperfolio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YahooChartResponse(
    @SerialName("chart")
    val chart: ChartWrapper? = null
)

@Serializable
data class ChartWrapper(
    @SerialName("result")
    val result: List<ChartResult>? = null,
    @SerialName("error")
    val error: YahooError? = null
)

@Serializable
data class YahooError(
    @SerialName("code")
    val code: String? = null,
    @SerialName("description")
    val description: String? = null
)

@Serializable
data class ChartResult(
    @SerialName("meta")
    val meta: ChartMeta? = null,
    @SerialName("timestamp")
    val timestamp: List<Long>? = null,
    @SerialName("indicators")
    val indicators: ChartIndicators? = null
)

@Serializable
data class ChartMeta(
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("symbol")
    val symbol: String? = null,
    @SerialName("regularMarketPrice")
    val regularMarketPrice: Double? = null,
    @SerialName("chartPreviousClose")
    val chartPreviousClose: Double? = null,
    @SerialName("previousClose")
    val previousClose: Double? = null,
    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: Double? = null,
    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: Double? = null,
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: Double? = null,
    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: Double? = null,
    @SerialName("regularMarketVolume")
    val regularMarketVolume: Long? = null,
    @SerialName("longName")
    val longName: String? = null,
    @SerialName("shortName")
    val shortName: String? = null,
    @SerialName("regularMarketChangePercent")
    val regularMarketChangePercent: Double? = null,
    @SerialName("fulldayChange")
    val fulldayChange: Double? = null
)

@Serializable
data class ChartIndicators(
    @SerialName("quote")
    val quote: List<QuoteIndicators>? = null
)

@Serializable
data class QuoteIndicators(
    @SerialName("open")
    val open: List<Double?>? = null,
    @SerialName("high")
    val high: List<Double?>? = null,
    @SerialName("low")
    val low: List<Double?>? = null,
    @SerialName("close")
    val close: List<Double?>? = null,
    @SerialName("volume")
    val volume: List<Long?>? = null
)
