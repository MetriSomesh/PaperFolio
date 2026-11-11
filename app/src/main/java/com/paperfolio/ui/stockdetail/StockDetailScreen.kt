package com.paperfolio.ui.stockdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paperfolio.domain.model.ChartPoint
import com.paperfolio.ui.components.BuyStockBottomSheet
import com.paperfolio.ui.components.ExchangeBadge
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
import com.paperfolio.ui.theme.TextTertiary
import com.paperfolio.ui.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: StockDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val portfolios by viewModel.portfolios.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showBuySheet by remember { mutableStateOf(false) }
    var highlightedPoint by remember { mutableStateOf<ChartPoint?>(null) }

    val periodMap = listOf(
        "1D" to "1d",
        "5D" to "5d",
        "1M" to "1mo",
        "6M" to "6mo",
        "1Y" to "1y",
        "5Y" to "5y",
        "MAX" to "max"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.stockDetail?.tradingSymbol ?: "Stock Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            uiState.stockDetail?.let {
                                Spacer(modifier = Modifier.width(6.dp))
                                ExchangeBadge(exchange = it.exchange)
                            }
                        }
                        uiState.stockDetail?.let {
                            Text(it.companyName, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStockData(uiState.selectedRange) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        bottomBar = {
            uiState.stockDetail?.let { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { showBuySheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("BUY DELIVERY", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        if (uiState.isLoading && uiState.stockDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (uiState.errorMessage != null && uiState.stockDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(uiState.errorMessage ?: "Error loading stock", color = LossRed, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.loadStockData(uiState.selectedRange) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val stock = uiState.stockDetail!!
            val displayPrice = highlightedPoint?.price ?: stock.currentPrice
            val isPositive = stock.change >= 0

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Price Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = FormatUtils.formatInr(displayPrice),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${FormatUtils.formatInr(stock.change, showSign = true)} (${FormatUtils.formatPercent(stock.changePercent, showSign = true)})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPositive) ProfitGreen else LossRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = highlightedPoint?.formattedTime ?: "Today",
                                fontSize = 11.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time Period Tabs (Scrollable & Responsive)
                TimePeriodSelector(
                    periods = periodMap.map { it.first },
                    selectedPeriod = periodMap.find { it.second == uiState.selectedRange }?.first ?: "1D",
                    onPeriodSelected = { label ->
                        val code = periodMap.find { it.first == label }?.second ?: "1d"
                        viewModel.loadStockData(code)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Chart
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    StockLineChart(
                        points = stock.chartPoints,
                        isPositive = isPositive,
                        previousClose = stock.prevClose,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        onPointSelected = { point ->
                            highlightedPoint = point
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dhan-style Performance Range Bars (Day Range & 52-Week Range)
                Text(
                    text = "Performance",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RangeSliderView(
                            title = "Today's Low / High",
                            low = stock.dayLow,
                            high = stock.dayHigh,
                            current = stock.currentPrice
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        RangeSliderView(
                            title = "52-Week Low / High",
                            low = stock.fiftyTwoWeekLow,
                            high = stock.fiftyTwoWeekHigh,
                            current = stock.currentPrice
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Key Statistics Header
                Text(
                    text = "Key Statistics",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Statistics Grid Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem(label = "Day High", value = FormatUtils.formatInr(stock.dayHigh))
                            StatItem(label = "Day Low", value = FormatUtils.formatInr(stock.dayLow))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem(label = "52W High", value = FormatUtils.formatInr(stock.fiftyTwoWeekHigh))
                            StatItem(label = "52W Low", value = FormatUtils.formatInr(stock.fiftyTwoWeekLow))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem(label = "Prev. Close", value = FormatUtils.formatInr(stock.prevClose))
                            StatItem(label = "Volume", value = FormatUtils.formatVolume(stock.volume))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Bottom clearance for buy bar
            }
        }
    }

    // Buy Bottom Sheet
    if (showBuySheet && uiState.stockDetail != null) {
        val stock = uiState.stockDetail!!
        BuyStockBottomSheet(
            tradingSymbol = stock.tradingSymbol,
            companyName = stock.companyName,
            currentPrice = stock.currentPrice,
            portfolios = portfolios,
            initialSelectedPortfolioId = portfolios.firstOrNull()?.id,
            onDismiss = { showBuySheet = false },
            onConfirmBuy = { portfolioId, qty, price ->
                viewModel.executeBuy(
                    portfolioId = portfolioId,
                    quantity = qty,
                    price = price,
                    onSuccess = {
                        showBuySheet = false
                        Toast.makeText(context, "Bought $qty shares of ${stock.tradingSymbol}!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun RangeSliderView(
    title: String,
    low: Double,
    high: Double,
    current: Double
) {
    val totalRange = (high - low).coerceAtLeast(0.01)
    val fraction = ((current - low) / totalRange).toFloat().coerceIn(0f, 1f)

    Column {
        Text(title, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar with pointer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceElevatedDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(BrandPrimary)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(FormatUtils.formatInr(low), fontSize = 11.sp, color = TextSecondary)
            Text(FormatUtils.formatInr(high), fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
