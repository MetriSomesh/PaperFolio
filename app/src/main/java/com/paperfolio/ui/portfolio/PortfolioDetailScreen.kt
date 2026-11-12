package com.paperfolio.ui.portfolio

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paperfolio.domain.model.ChartPoint
import com.paperfolio.domain.model.StockHolding
import com.paperfolio.ui.components.BuyStockBottomSheet
import com.paperfolio.ui.components.DhanCard
import com.paperfolio.ui.components.DhanDivider
import com.paperfolio.ui.components.DhanSectionHeader
import com.paperfolio.ui.components.MetricBadge
import com.paperfolio.ui.components.SellStockBottomSheet
import com.paperfolio.ui.components.StockHoldingCard
import com.paperfolio.ui.components.StockLineChart
import com.paperfolio.ui.components.TimePeriodSelector
import com.paperfolio.ui.theme.BackgroundDark
import com.paperfolio.ui.theme.BrandPrimary
import com.paperfolio.ui.theme.BuyGreen
import com.paperfolio.ui.theme.LossRedBg
import com.paperfolio.ui.theme.LossRedDark
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.ProfitGreenBg
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioDetailScreen(
    viewModel: PortfolioDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStockDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedHoldingToSell by remember { mutableStateOf<StockHolding?>(null) }
    var selectedHoldingToBuyMore by remember { mutableStateOf<StockHolding?>(null) }
    var highlightedChartPoint by remember { mutableStateOf<ChartPoint?>(null) }

    val periods = listOf("1D", "1W", "1M", "6M", "1Y", "3Y", "5Y", "ALL")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.portfolio?.name ?: "Portfolio",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPortfolio() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        if (uiState.isLoading && uiState.portfolio == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (uiState.errorMessage != null && uiState.portfolio == null) {
            // Was: a load failure rendered an empty list with no message
            // at all — the screen just looked blank.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        uiState.errorMessage ?: "Could not load portfolio",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.loadPortfolio() },
                        colors = ButtonDefaults.buttonColors(containerColor = BuyGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Retry", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            val portfolio = uiState.portfolio
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview: big value, P&L as plain coloured text (Dhan).
                if (portfolio != null) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Portfolio Value", fontSize = 12.sp, color = TextSecondary)
                                highlightedChartPoint?.let { pt ->
                                    Text(pt.formattedTime, fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatInr(highlightedChartPoint?.price ?: portfolio.totalNetWorth),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MetricBadge(
                                value = portfolio.totalPnl,
                                percent = portfolio.totalPnlPercent
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            DhanDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Invested", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        FormatUtils.formatInr(portfolio.investedAmount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Column {
                                    Text("Current Value", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        FormatUtils.formatInr(portfolio.currentValue),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Available Cash", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        FormatUtils.formatInr(portfolio.cashBalance),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Time Period Tabs
                    item {
                        TimePeriodSelector(
                            periods = periods,
                            selectedPeriod = uiState.selectedPeriod,
                            onPeriodSelected = { viewModel.onPeriodSelected(it) }
                        )
                    }

                    // Interactive Line Chart
                    if (uiState.chartPoints.isNotEmpty()) {
                        item {
                            DhanCard {
                                StockLineChart(
                                    points = uiState.chartPoints,
                                    isPositive = portfolio.totalPnl >= 0,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(12.dp),
                                    onPointSelected = { pt ->
                                        highlightedChartPoint = pt
                                    }
                                )
                            }
                        }
                    }

                    // Holdings Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DhanSectionHeader(text = "HOLDINGS (${uiState.holdings.size})")
                            TextButton(onClick = onNavigateToSearch) {
                                Text("+ Add Stock", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandPrimary)
                            }
                        }
                    }

                    if (uiState.holdings.isEmpty()) {
                        item {
                            DhanCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No Stocks Parked in This Portfolio", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Search Indian stocks or ETFs and buy them with delivery to track them here.",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onNavigateToSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = BuyGreen, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Explore & Buy Stocks", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            DhanCard {
                                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    uiState.holdings.forEachIndexed { index, holding ->
                                        Column {
                                            StockHoldingCard(
                                                holding = holding,
                                                onClick = { onNavigateToStockDetail(holding.yahooSymbol) }
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Button(
                                                    onClick = { selectedHoldingToBuyMore = holding },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = ProfitGreenBg,
                                                        contentColor = ProfitGreen
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Buy More", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { selectedHoldingToSell = holding },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = LossRedBg,
                                                        contentColor = LossRedDark
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Sell", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                        if (index < uiState.holdings.size - 1) {
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
    }

    // Sell Bottom Sheet
    selectedHoldingToSell?.let { holding ->
        SellStockBottomSheet(
            tradingSymbol = holding.tradingSymbol,
            companyName = holding.companyName,
            availableQuantity = holding.quantity,
            averageBuyPrice = holding.averagePrice,
            currentPrice = holding.currentPrice,
            onDismiss = { selectedHoldingToSell = null },
            onConfirmSell = { qty, price ->
                viewModel.executeSell(
                    yahooSymbol = holding.yahooSymbol,
                    quantity = qty,
                    price = price,
                    onSuccess = {
                        selectedHoldingToSell = null
                        Toast.makeText(context, "Successfully sold $qty shares of ${holding.tradingSymbol}", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // Buy More Bottom Sheet
    selectedHoldingToBuyMore?.let { holding ->
        uiState.portfolio?.let { currentPortfolio ->
            BuyStockBottomSheet(
                tradingSymbol = holding.tradingSymbol,
                companyName = holding.companyName,
                currentPrice = holding.currentPrice,
                portfolios = listOf(currentPortfolio),
                initialSelectedPortfolioId = currentPortfolio.id,
                onDismiss = { selectedHoldingToBuyMore = null },
                onConfirmBuy = { _, qty, price ->
                    viewModel.executeBuyMore(
                        yahooSymbol = holding.yahooSymbol,
                        quantity = qty,
                        price = price,
                        onSuccess = {
                            selectedHoldingToBuyMore = null
                            Toast.makeText(context, "Successfully bought $qty shares of ${holding.tradingSymbol}", Toast.LENGTH_SHORT).show()
                        },
                        onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }
}
