package com.paperfolio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperfolio.domain.model.StockHolding
import com.paperfolio.ui.theme.BorderDark
import com.paperfolio.ui.theme.BrandPrimary
import com.paperfolio.ui.theme.BseColor
import com.paperfolio.ui.theme.EtfColor
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.LossRedBg
import com.paperfolio.ui.theme.LossRedDark
import com.paperfolio.ui.theme.NseColor
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.ProfitGreenBg
import com.paperfolio.ui.theme.SurfaceElevatedDark
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.theme.TextTertiary
import com.paperfolio.ui.utils.FormatUtils

// ---------------------------------------------------------------------------
// Dhan-style shared components. Dhan's language, applied everywhere:
// white surfaces, 1dp hairline borders instead of shadows, flat list rows
// separated by dividers, text tabs with a green underline, small grey
// labels, Semibold symbols, ExtraBold money figures.
// ---------------------------------------------------------------------------

/** White card with a hairline border — Dhan's card. No shadow. */
@Composable
fun DhanCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

/** Hairline divider between flat list rows. */
@Composable
fun DhanDivider(modifier: Modifier = Modifier) {
    Divider(color = BorderDark, thickness = 1.dp, modifier = modifier)
}

/** Small grey section header, e.g. "MY PORTFOLIOS", "HOLDINGS". */
@Composable
fun DhanSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = modifier
    )
}

/** Dhan period tabs: plain text, green underline under the selected one. */
@Composable
fun TimePeriodSelector(
    periods: List<String>,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        periods.forEach { period ->
            val isSelected = period == selectedPeriod
            Column(
                modifier = Modifier
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = period,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) TextPrimary else TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(
                            if (isSelected) BrandPrimary else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

/**
 * Dhan P&L: plain coloured text with an arrow — no pill background.
 * e.g. ↑ +₹1,234.56 (+2.34%)
 */
@Composable
fun MetricBadge(
    value: Double,
    percent: Double,
    modifier: Modifier = Modifier
) {
    val isProfit = value >= 0
    val color = if (isProfit) ProfitGreen else LossRed

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isProfit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "${FormatUtils.formatInr(value, showSign = true)} (${FormatUtils.formatPercent(percent, showSign = true)})",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Dhan exchange tag: tiny grey uppercase text, not a coloured pill. */
@Composable
fun ExchangeBadge(
    exchange: String,
    modifier: Modifier = Modifier
) {
    val upper = exchange.uppercase()
    val badgeColor = when {
        upper.contains("ETF") -> EtfColor
        upper.contains("BSE") -> BseColor
        else -> NseColor
    }
    Text(
        text = upper,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = badgeColor,
        modifier = modifier
    )
}

/** Dhan watchlist row: flat, symbol + company on the left, LTP + P&L on
 *  the right, separated from the next row by a hairline divider. */
@Composable
fun StockHoldingCard(
    holding: StockHolding,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = holding.tradingSymbol,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ExchangeBadge(exchange = holding.exchange)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = holding.companyName,
                    fontSize = 12.sp,
                    color = TextTertiary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Qty: ${holding.quantity}  •  Avg: ${FormatUtils.formatInr(holding.averagePrice)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatInr(holding.currentPrice),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val isProfit = holding.pnl >= 0
                val pnlColor = if (isProfit) ProfitGreen else LossRed
                Text(
                    text = "${FormatUtils.formatInr(holding.pnl, showSign = true)} (${FormatUtils.formatPercent(holding.pnlPercent, showSign = true)})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pnlColor
                )
            }
        }
    }
}

/** Filter chip: selected = green tint + green text + green border. */
@Composable
fun DhanFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ProfitGreenBg else SurfaceElevatedDark)
            .border(
                width = 1.dp,
                color = if (selected) BrandPrimary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) BrandPrimary else TextSecondary
        )
    }
}

/** Small tinted P&L text used inside tinted backgrounds (AA on tint). */
@Composable
fun PnlText(
    value: Double,
    percent: Double? = null,
    fontSize: Int = 12,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    val isProfit = value >= 0
    Text(
        text = if (percent != null) {
            "${FormatUtils.formatInr(value, showSign = true)} (${FormatUtils.formatPercent(percent, showSign = true)})"
        } else {
            FormatUtils.formatInr(value, showSign = true)
        },
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        color = if (isProfit) ProfitGreen else LossRedDark
    )
}
