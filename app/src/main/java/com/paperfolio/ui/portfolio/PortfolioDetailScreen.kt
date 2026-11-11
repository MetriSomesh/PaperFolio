package com.paperfolio.ui.portfolio

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paperfolio.domain.model.ChartPoint
import com.paperfolio.domain.model.StockHolding
import com.paperfolio.ui.components.BuyStockBottomSheet
import com.paperfolio.ui.components.MetricBadge
import com.paperfolio.ui.components.SellStockBottomSheet
import com.paperfolio.ui.components.StockHoldingCard
import com.paperfolio.ui.components.StockLineChart
import com.paperfolio.ui.components.TimePeriodSelector
import com.paperfolio.ui.theme.BackgroundDark
import com.paperfolio.ui.theme.BrandPrimary
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.SurfaceCard
import com.paperfolio.ui.theme.SurfaceElevatedDark
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
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
        } else {
            val portfolio = uiState.portfolio
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Overview Summary Card
                if (portfolio != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val displayNetWorth = highlightedChartPoint?.price ?: portfolio.totalNetWorth

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
                                    text = FormatUtils.formatInr(displayNetWorth),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(14.dp))

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

                                Spacer(modifier = Modifier.height(12.dp))

                                MetricBadge(
                                    value = portfolio.totalPnl,
                                    percent = portfolio.totalPnlPercent
                                )
                            }
                        }
                    }

                    // Time Period Selector (Scrollable)
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
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                StockLineChart(
                                    points = uiState.chartPoints,
                                    isPositive = portfolio.totalPnl >= 0,
                                    modifier = Modifier
                                        .fillMaxSize()
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
                            Text(
                                text = "Holdings (${uiState.holdings.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Button(
                                onClick = onNavigateToSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevatedDark, contentColor = BrandPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Add Stock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (uiState.holdings.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No Stocks Parked in This Portfolio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BackgroundDark),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Explore & Buy Stocks", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(uiState.holdings, key = { it.yahooSymbol }) { holding ->
                            Column {
                                StockHoldingCard(
                                    holding = holding,
                                    onClick = { onNavigateToStockDetail(holding.yahooSymbol) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { selectedHoldingToBuyMore = holding },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ProfitGreen.copy(alpha = 0.15f),
                                            contentColor = ProfitGreen
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Buy More", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                    Button(
                                        onClick = { selectedHoldingToSell = holding },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LossRed.copy(alpha = 0.15f),
                                            contentColor = LossRed
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Sell", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
