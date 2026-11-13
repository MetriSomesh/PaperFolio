package com.paperfolio.ui.stockdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paperfolio.domain.model.ChartPoint
import com.paperfolio.ui.components.BuyStockBottomSheet
import com.paperfolio.ui.components.VeltoCard
import com.paperfolio.ui.components.VeltoDivider
import com.paperfolio.ui.components.VeltoPrimaryButton
import com.paperfolio.ui.components.VeltoSectionHeader
import com.paperfolio.ui.components.ExchangeBadge
import com.paperfolio.ui.components.StockLineChart
import com.paperfolio.ui.components.TimePeriodSelector
import com.paperfolio.ui.theme.AppBackground
import com.paperfolio.ui.theme.VeltoGreen
import com.paperfolio.ui.theme.Lime
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.SurfaceMuted
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.theme.TextTertiary
import com.paperfolio.ui.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: StockDetailViewModel,
    onNavigateBack: () -> Unit,
    // Portfolio the buy started from (-1 = none, e.g. opened from the
    // Home search tab). The buy sheet pre-selects this portfolio; see
    // resolveInitialPortfolioId. Without it, buys from portfolio A's
    // page landed in B, because the sheet defaulted to the newest
    // portfolio (the list is newest-first) and nothing said "from A".
    sourcePortfolioId: Long = -1L
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
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        bottomBar = {
            uiState.stockDetail?.let { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppBackground)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    VeltoPrimaryButton(
                        text = "Buy delivery",
                        onClick = { showBuySheet = true }
                    )
                }
            }
        },
        containerColor = AppBackground
    ) { paddingValues ->
        if (uiState.isLoading && uiState.stockDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VeltoGreen)
            }
        } else if (uiState.errorMessage != null && uiState.stockDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(uiState.errorMessage ?: "Error loading stock", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    VeltoPrimaryButton(
                        text = "Retry",
                        onClick = { viewModel.loadStockData(uiState.selectedRange) }
                    )
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
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
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

                // Time Period Tabs
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
                VeltoCard {
                    StockLineChart(
                        points = stock.chartPoints,
                        isPositive = isPositive,
                        previousClose = stock.prevClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(12.dp),
                        onPointSelected = { point ->
                            highlightedPoint = point
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Performance range bars
                VeltoSectionHeader(text = "Performance")
                Spacer(modifier = Modifier.height(8.dp))

                VeltoCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RangeSliderView(
                            title = "Today's Low / High",
                            low = stock.dayLow,
                            high = stock.dayHigh,
                            current = stock.currentPrice
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        RangeSliderView(
                            title = "52-Week Low / High",
                            low = stock.fiftyTwoWeekLow,
                            high = stock.fiftyTwoWeekHigh,
                            current = stock.currentPrice
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Key statistics as rows with dividers
                VeltoSectionHeader(text = "Key statistics")
                Spacer(modifier = Modifier.height(8.dp))

                VeltoCard {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        StatRow(label = "Day High", value = FormatUtils.formatInr(stock.dayHigh))
                        VeltoDivider()
                        StatRow(label = "Day Low", value = FormatUtils.formatInr(stock.dayLow))
                        VeltoDivider()
                        StatRow(label = "52W High", value = FormatUtils.formatInr(stock.fiftyTwoWeekHigh))
                        VeltoDivider()
                        StatRow(label = "52W Low", value = FormatUtils.formatInr(stock.fiftyTwoWeekLow))
                        VeltoDivider()
                        StatRow(label = "Prev. Close", value = FormatUtils.formatInr(stock.prevClose))
                        VeltoDivider()
                        StatRow(label = "Volume", value = FormatUtils.formatVolume(stock.volume))
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
            initialSelectedPortfolioId = if (sourcePortfolioId > 0) sourcePortfolioId
            else portfolios.firstOrNull()?.id,
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
        Spacer(modifier = Modifier.height(8.dp))

        // Range track: thin grey track, dark-green fill to the
        // marker, dot at the current price.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceMuted)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VeltoGreen)
            )
            val dotOffset = with(LocalDensity.current) {
                (maxWidth * fraction).toPx()
            }
            Box(
                modifier = Modifier
                    .offset(x = with(LocalDensity.current) { (dotOffset - 7.dp.toPx()).toDp() })
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(VeltoGreen)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
