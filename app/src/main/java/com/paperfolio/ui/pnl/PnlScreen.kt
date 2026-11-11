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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.paperfolio.ui.components.TimePeriodSelector
import com.paperfolio.ui.theme.BackgroundDark
import com.paperfolio.ui.theme.BrandPrimary
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.SurfaceCard
import com.paperfolio.ui.theme.SurfaceDark
import com.paperfolio.ui.theme.SurfaceElevatedDark
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Time Period Selector (Scrollable)
                item {
                    TimePeriodSelector(
                        periods = periods,
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.loadPnlData(it) }
                    )
                }

                // Overall Net P&L Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Net P&L (${uiState.selectedPeriod})", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatInr(netTotalPnl, showSign = true),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetProfit) ProfitGreen else LossRed
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Realized P&L", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val rProfit = uiState.totalRealizedPnl >= 0
                                    Text(
                                        text = FormatUtils.formatInr(uiState.totalRealizedPnl, showSign = true),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rProfit) ProfitGreen else LossRed
                                    )
                                }

                                Column {
                                    Text("Unrealized P&L", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val uProfit = uiState.totalUnrealizedPnl >= 0
                                    Text(
                                        text = FormatUtils.formatInr(uiState.totalUnrealizedPnl, showSign = true),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uProfit) ProfitGreen else LossRed
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
                }

                // Portfolio Breakdown Header
                item {
                    Text(
                        text = "Breakdown by Portfolio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (uiState.portfolioBreakdown.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No portfolio data yet", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(uiState.portfolioBreakdown, key = { it.portfolioId }) { pItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPortfolio(pItem.portfolioId) },
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
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
                                        Text(pItem.portfolioName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(
                                            "Realized: ${FormatUtils.formatInr(pItem.realizedPnl, showSign = true)}  •  Open: ${FormatUtils.formatInr(pItem.unrealizedPnl, showSign = true)}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                val isP = pItem.totalPnl >= 0
                                Text(
                                    text = FormatUtils.formatInr(pItem.totalPnl, showSign = true),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isP) ProfitGreen else LossRed
                                )
                            }
                        }
                    }
                }

                // Closed Trades History Header
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Realized Trades History (${uiState.closedTrades.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (uiState.closedTrades.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No sell transactions recorded yet", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(uiState.closedTrades, key = { it.id }) { trade ->
                        ClosedTradeItemCard(trade = trade)
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
                    Text("Apply Range", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = SurfaceDark
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Select Date Range for P&L", color = TextPrimary, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) },
                colors = DatePickerDefaults.colors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    selectedDayContainerColor = BrandPrimary,
                    dayInSelectionRangeContainerColor = BrandPrimary.copy(alpha = 0.2f),
                    selectedDayContentColor = BackgroundDark,
                    todayDateBorderColor = BrandPrimary,
                    todayContentColor = BrandPrimary
                )
            )
        }
    }
}

@Composable
fun ClosedTradeItemCard(trade: TradeEntity) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(trade.timestamp))
    val pnl = trade.realizedPnl ?: 0.0
    val isProfit = pnl >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${trade.yahooSymbol.substringBefore(".")} (SOLD)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.Bold,
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
}
