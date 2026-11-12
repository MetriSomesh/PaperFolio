package com.paperfolio.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paperfolio.domain.model.Portfolio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.paperfolio.ui.components.HeroStat
import com.paperfolio.ui.components.MetricBadge
import com.paperfolio.ui.components.VeltoCard
import com.paperfolio.ui.components.VeltoFilterChip
import com.paperfolio.ui.components.VeltoHeroCard
import com.paperfolio.ui.components.VeltoHeroDivider
import com.paperfolio.ui.components.VeltoPrimaryButton
import com.paperfolio.ui.components.VeltoSectionHeader
import com.paperfolio.ui.theme.AppBackground
import com.paperfolio.ui.theme.Border
import com.paperfolio.ui.theme.VeltoGreen
import com.paperfolio.ui.theme.HeroText
import com.paperfolio.ui.theme.HeroTextSecondary
import com.paperfolio.ui.theme.Lime
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.ProfitGreenBg
import com.paperfolio.ui.theme.Surface
import com.paperfolio.ui.theme.SurfaceMuted
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.theme.TextTertiary
import com.paperfolio.ui.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToPortfolio: (Long) -> Unit
) {
    val portfolios by viewModel.portfolios.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // VELTO's home opens with a date line under the wordmark.
    val todayLabel = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }
    // Real totals across portfolios — the hero sums what exists, it
    // invents nothing: value, P&L and invested cash are all sums of the
    // portfolios the user already has.
    val totalValue = portfolios.sumOf { it.totalNetWorth }
    val totalPnl = portfolios.sumOf { it.totalPnl }
    val totalInvested = portfolios.sumOf { it.investedAmount }
    val totalCash = portfolios.sumOf { it.cashBalance }
    val totalPnlPercent = if (totalInvested > 0) totalPnl / totalInvested * 100 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PaperFolio",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = todayLabel,
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search Stocks", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // VELTO hero: the dark balance card. Total value, P&L in the
            // light green/red, then invested / cash / portfolio count.
            if (portfolios.isNotEmpty()) {
                item {
                    VeltoHeroCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Total portfolio value", fontSize = 12.sp, color = HeroTextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = FormatUtils.formatInr(totalValue),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HeroText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MetricBadge(
                                value = totalPnl,
                                percent = totalPnlPercent,
                                onDark = true
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            VeltoHeroDivider()
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HeroStat("Invested", FormatUtils.formatInr(totalInvested))
                                HeroStat("Cash", FormatUtils.formatInr(totalCash))
                                HeroStat(
                                    "Portfolios",
                                    "${portfolios.size}",
                                    alignEnd = true
                                )
                            }
                        }
                    }
                }
            }

            // Search bar: muted pill, magnifier, placeholder.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceMuted)
                        .clickable { onNavigateToSearch() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Search NSE / BSE stocks & ETFs...", color = TextSecondary, fontSize = 14.sp)
                }
            }

            // Primary action: VELTO's lime pill CTA.
            item {
                VeltoPrimaryButton(
                    text = "Create portfolio",
                    onClick = { showCreateDialog = true }
                )
            }

            // Portfolio Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VeltoSectionHeader(text = "My portfolios (${portfolios.size})")
                    TextButton(onClick = { showCreateDialog = true }) {
                        Text("+ Create new", color = VeltoGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (portfolios.isEmpty()) {
                item {
                    VeltoCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(ProfitGreenBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = VeltoGreen, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No portfolios yet",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create a portfolio with custom capital (₹1K to Unlimited) to start buying and parking your delivery stocks.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            VeltoPrimaryButton(
                                text = "Create first portfolio",
                                onClick = { showCreateDialog = true }
                            )
                        }
                    }
                }
            } else {
                items(portfolios, key = { it.id }) { portfolio ->
                    PortfolioItemCard(
                        portfolio = portfolio,
                        onClick = { onNavigateToPortfolio(portfolio.id) },
                        onDelete = { viewModel.deletePortfolio(portfolio.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePortfolioDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, budget, color ->
                viewModel.createPortfolio(name, budget, colorTag = color) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun PortfolioItemCard(
    portfolio: Portfolio,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    VeltoCard(onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = try {
                        Color(android.graphics.Color.parseColor(portfolio.colorTag))
                    } catch (e: Exception) {
                        VeltoGreen
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = portfolio.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceMuted)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Available Cash", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatInr(portfolio.cashBalance),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Budget", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (portfolio.initialBalance >= 100_000_000) "Unlimited" else FormatUtils.formatInr(portfolio.initialBalance, includeDecimals = false),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Portfolio?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text("Are you sure you want to delete '${portfolio.name}' and all its trade history? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = LossRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = Surface
        )
    }
}

@Composable
fun CreatePortfolioDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, budget: Double, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("100000") }
    var selectedColor by remember { mutableStateOf("#122D28") }
    var budgetError by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val presetBudgets = listOf(
        "₹25K" to 25000.0,
        "₹50K" to 50000.0,
        "₹1L" to 100000.0,
        "₹5L" to 500000.0,
        "₹10L" to 1000000.0,
        "Unlimited" to 100000000.0
    )

    val colorOptions = listOf(
        "#122D28", "#007A5E", "#2563EB", "#B45309", "#7C3AED"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create new portfolio", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
        text = {
            Column {
                Text("Portfolio Name", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Long Term, Dividend, Growth", color = TextTertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VeltoGreen,
                        unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Starting Capital / Budget (₹)", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it.filter { ch -> ch.isDigit() }
                        budgetError = null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    isError = budgetError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VeltoGreen,
                        unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        errorBorderColor = LossRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                budgetError?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, fontSize = 12.sp, color = LossRed)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetBudgets.forEach { (label, amt) ->
                        val isSelected = budgetText == amt.toLong().toString()
                        VeltoFilterChip(
                            text = label,
                            selected = isSelected,
                            onClick = {
                                budgetText = amt.toLong().toString()
                                budgetError = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Color Tag", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    colorOptions.forEach { hex ->
                        val isSelected = hex == selectedColor
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedColor = hex }
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    val budget = budgetText.toDoubleOrNull() ?: 100000.0
                    // Minimum capital is ₹1,000 (README promise). Zero or
                    // tiny budgets used to create portfolios where every
                    // buy failed with "insufficient funds".
                    if (budget < 1000.0) {
                        budgetError = "Minimum capital is ₹1,000"
                        return@Button
                    }
                    if (name.isNotBlank()) {
                        onConfirm(name, budget, selectedColor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = VeltoGreen),
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Create portfolio", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextPrimary)
            }
        },
        containerColor = Surface
    )
}
