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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperfolio.domain.model.StockHolding
import com.paperfolio.ui.theme.Border
import com.paperfolio.ui.theme.BseColor
import com.paperfolio.ui.theme.EtfColor
import com.paperfolio.ui.theme.HeroLoss
import com.paperfolio.ui.theme.HeroProfit
import com.paperfolio.ui.theme.HeroText
import com.paperfolio.ui.theme.HeroTextSecondary
import com.paperfolio.ui.theme.Lime
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.LossRedBg
import com.paperfolio.ui.theme.NseColor
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.ProfitGreenBg
import com.paperfolio.ui.theme.Surface
import com.paperfolio.ui.theme.SurfaceMuted
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.theme.TextTertiary
import com.paperfolio.ui.theme.VeltoGreen
import com.paperfolio.ui.utils.FormatUtils

// ---------------------------------------------------------------------------
// VELTO-style shared components. VELTO's language, applied everywhere:
// off-white app, white cards with soft borders and 16dp corners, one dark
// green hero card per screen, lime pill CTAs with dark text, sentence-case
// Semibold section titles, pill tabs and chips, a floating pill nav.
// ---------------------------------------------------------------------------

/** White card with a soft border and 16dp corners — VELTO's card. */
@Composable
fun VeltoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

/**
 * The dark-green hero card — VELTO's balance card. Off-white text,
 * muted green-grey labels, light green/red P&L. 20dp corners, no border.
 */
@Composable
fun VeltoHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VeltoGreen),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

/** Hairline divider between flat list rows. */
@Composable
fun VeltoDivider(modifier: Modifier = Modifier) {
    Divider(color = Border, thickness = 1.dp, modifier = modifier)
}

/** Hairline divider on the dark hero card (white at low alpha). */
@Composable
fun VeltoHeroDivider(modifier: Modifier = Modifier) {
    Divider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp, modifier = modifier)
}

/** Sentence-case Semibold section title, e.g. "Holdings", "Performance". */
@Composable
fun VeltoSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = modifier
    )
}

/** VELTO period tabs: pills — selected is a dark-green pill, the rest
 *  are light grey pills. Replaces the old underline tabs. */
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { period ->
            val isSelected = period == selectedPeriod
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) VeltoGreen else SurfaceMuted)
                    .clickable { onPeriodSelected(period) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) HeroText else TextSecondary
                )
            }
        }
    }
}

/**
 * P&L: plain coloured text with an arrow — no pill background.
 * e.g. ↑ +₹1,234.56 (+2.34%). Set onDark when it sits on the hero card.
 */
@Composable
fun MetricBadge(
    value: Double,
    percent: Double,
    modifier: Modifier = Modifier,
    onDark: Boolean = false
) {
    val isProfit = value >= 0
    val color = if (onDark) {
        if (isProfit) HeroProfit else HeroLoss
    } else {
        if (isProfit) ProfitGreen else LossRed
    }

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

/** Exchange tag: tiny grey uppercase text, not a coloured pill. */
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

/**
 * VELTO activity-row icon tile: a rounded square with the first
 * letter of the name, dark green on light grey — the tile VELTO puts
 * at the left of every activity row (Fika Coffee, Studio payroll…).
 */
@Composable
fun VeltoIconTile(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceMuted),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.firstOrNull()?.uppercase() ?: "?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VeltoGreen
        )
    }
}

/** Holding row: icon tile + symbol + company on the left, LTP + P&L
 *  on the right, separated from the next row by a hairline divider. */
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
            VeltoIconTile(label = holding.tradingSymbol)
            Spacer(modifier = Modifier.width(12.dp))
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

/** Filter chip as a pill: selected = dark green pill, white text. */
@Composable
fun VeltoFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) VeltoGreen else SurfaceMuted)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) HeroText else TextSecondary
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
        color = if (isProfit) ProfitGreen else LossRed
    )
}

/**
 * VELTO primary CTA: a lime pill with dark text. Lime never takes
 * white text (1.2:1) — dark on lime is 12.5:1.
 */
@Composable
fun VeltoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Lime,
            contentColor = VeltoGreen,
            disabledContainerColor = SurfaceMuted,
            disabledContentColor = TextTertiary
        ),
        shape = RoundedCornerShape(26.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** Dark pill CTA: dark-green fill, off-white text. */
@Composable
fun VeltoDarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = VeltoGreen,
            contentColor = HeroText,
            disabledContainerColor = SurfaceMuted,
            disabledContentColor = TextTertiary
        ),
        shape = RoundedCornerShape(26.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** One bottom-nav tab for the floating pill nav. */
data class VeltoNavTab(
    val route: String,
    val title: String,
    val icon: ImageVector
)

/**
 * VELTO's floating pill nav: a white rounded bar that floats above the
 * content, the active tab a dark-green pill with white icon + label.
 */
@Composable
fun VeltoPillNav(
    tabs: List<VeltoNavTab>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(8.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (selected) VeltoGreen else Color.Transparent)
                        .clickable { onNavigate(tab.route) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (selected) HeroText else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HeroText
                        )
                    }
                }
            }
        }
    }
}

/** Muted label + value pair for the dark hero card. */
@Composable
fun HeroStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(label, fontSize = 11.sp, color = HeroTextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HeroText
        )
    }
}
