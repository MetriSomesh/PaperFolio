package com.paperfolio.ui.pnl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paperfolio.data.local.entity.TradeEntity
import com.paperfolio.ui.components.DhanCard
import com.paperfolio.ui.components.DhanDivider
import com.paperfolio.ui.components.DhanSectionHeader
import com.paperfolio.ui.components.TimePeriodSelector
import com.paperfolio.ui.theme.BackgroundDark
import com.paperfolio.ui.theme.BrandPrimary
import com.paperfolio.ui.theme.BuyGreen
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.SurfaceCard
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.theme.TextTertiary
import com.paperfolio.ui.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PnlScreen(
    viewModel: PnlViewModel,
    onNavigateToPortfolio: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomDateDialog by remember { mutableStateOf(false) }

    val periods = listOf("1D", "1W", "1M", "6M", "1Y", "3Y", "5Y", "ALL")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "P&L Statement",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { showCustomDateDialog = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Custom Dates", tint = TextPrimary)
                    }
                    IconButton(onClick = { viewModel.loadPnlData(uiState.selectedPeriod) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else {
            val netTotalPnl = uiState.totalRealizedPnl + uiState.totalUnrealizedPnl
            val isNetProfit = netTotalPnl >= 0

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Period Selector
                item {
                    TimePeriodSelector(
                        periods = periods,
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.loadPnlData(it) }
                    )
                }

                // Overall Net P&L — Dhan style: big number, plain stats row.
                // NOTE: the old header said "Total Net P&L (1M)" which was
                // misleading — unrealized P&L is always current holdings,
                // only realized P&L follows the selected period. The label
                // now says so explicitly.
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Total Net P&L", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatInr(netTotalPnl, showSign = true),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isNetProfit) ProfitGreen else LossRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Realized P&L is for the selected period. Unrealized P&L is your open holdings right now.",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DhanDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Realized P&L", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatInr(uiState.totalRealizedPnl, showSign = true),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.totalRealizedPnl >= 0) ProfitGreen else LossRed
                                )
                            }
                            Column {
                                Text("Unrealized P&L", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatInr(uiState.totalUnrealizedPnl, showSign = true),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.totalUnrealizedPnl >= 0) ProfitGreen else LossRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Current Holdings", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatInr(uiState.totalCurrentValue),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Portfolio Breakdown
                item {
                    DhanSectionHeader(text = "BREAKDOWN BY PORTFOLIO")
                }

                if (uiState.portfolioBreakdown.isEmpty()) {
                    item {
                        DhanCard {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No portfolio data yet", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    item {
                        DhanCard {
                            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                                uiState.portfolioBreakdown.forEachIndexed { index, pItem ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToPortfolio(pItem.portfolioId) }
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val tagColor = try {
                                                Color(android.graphics.Color.parseColor(pItem.colorTag))
                                            } catch (e: Exception) {
                                                BrandPrimary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(tagColor)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(pItem.portfolioName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                Text(
                                                    "Realized: ${FormatUtils.formatInr(pItem.realizedPnl, showSign = true)}  •  Open: ${FormatUtils.formatInr(pItem.unrealizedPnl, showSign = true)}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = FormatUtils.formatInr(pItem.totalPnl, showSign = true),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (pItem.totalPnl >= 0) ProfitGreen else LossRed
                                        )
                                    }
                                    if (index < uiState.portfolioBreakdown.size - 1) {
                                        DhanDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                // Closed Trades History
                item {
                    DhanSectionHeader(text = "REALIZED TRADES HISTORY (${uiState.closedTrades.size})")
                }

                if (uiState.closedTrades.isEmpty()) {
                    item {
                        DhanCard {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No sell transactions recorded yet", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    item {
                        DhanCard {
                            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                                uiState.closedTrades.forEachIndexed { index, trade ->
                                    ClosedTradeItemRow(trade = trade)
                                    if (index < uiState.closedTrades.size - 1) {
                                        DhanDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDateDialog) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showCustomDateDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis ?: start
                        if (start != null && end != null) {
                            viewModel.loadPnlData("CUSTOM", customStart = start, customEnd = end)
                        }
                        showCustomDateDialog = false
                    }
                ) {
                    Text("Apply Range", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = SurfaceCard
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Select Date Range for P&L", color = TextPrimary, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold) },
                colors = DatePickerDefaults.colors(
                    containerColor = SurfaceCard,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    selectedDayContainerColor = BrandPrimary,
                    dayInSelectionRangeContainerColor = BrandPrimary.copy(alpha = 0.15f),
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = BrandPrimary,
                    todayContentColor = BrandPrimary
                )
            )
        }
    }
}

@Composable
fun ClosedTradeItemRow(trade: TradeEntity) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(trade.timestamp))
    val pnl = trade.realizedPnl ?: 0.0
    val isProfit = pnl >= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${trade.yahooSymbol.substringBefore(".")} (SOLD)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Sold ${trade.quantity} shares @ ${FormatUtils.formatInr(trade.price)}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = dateStr,
                fontSize = 10.sp,
                color = TextTertiary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = FormatUtils.formatInr(pnl, showSign = true),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isProfit) ProfitGreen else LossRed
            )
            Text(
                text = "Realized P&L",
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
    }
}
