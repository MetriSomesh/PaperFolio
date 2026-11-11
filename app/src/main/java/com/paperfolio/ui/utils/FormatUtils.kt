package com.paperfolio.ui.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object FormatUtils {

    /**
     * Formats number into Indian currency style (Lakhs & Crores):
     * e.g. 1000 -> "₹1,000.00"
     * 100000 -> "₹1,00,000.00"
     * 12345678 -> "₹1,23,45,678.00"
     */
    fun formatInr(amount: Double, showSign: Boolean = false, includeDecimals: Boolean = true): String {
        val isNegative = amount < 0
        val absVal = abs(amount)

        val pattern = if (includeDecimals) "#,##,##0.00" else "#,##,##0"
        val symbols = DecimalFormatSymbols(Locale("en", "IN"))
        val formatter = DecimalFormat(pattern, symbols)
        val formatted = formatter.format(absVal)

        return when {
            isNegative -> "-₹$formatted"
            showSign && amount > 0 -> "+₹$formatted"
            else -> "₹$formatted"
        }
    }

    /**
     * Formats percentage with + or - sign
     */
    fun formatPercent(percent: Double, showSign: Boolean = true): String {
        val sign = if (showSign && percent > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.2f", percent)}%"
    }

    /**
     * Formats compact volume (e.g., 1.5M, 250K)
     */
    fun formatVolume(volume: Long): String {
        return when {
            volume >= 10_000_000 -> String.format(Locale.US, "%.2f Cr", volume / 10_000_000.0)
            volume >= 100_000 -> String.format(Locale.US, "%.2f L", volume / 100_000.0)
            volume >= 1_000 -> String.format(Locale.US, "%.1f K", volume / 1_000.0)
            else -> volume.toString()
        }
    }
}
