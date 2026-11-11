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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperfolio.domain.model.Portfolio
import com.paperfolio.ui.theme.BorderDark
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.SurfaceCard
import com.paperfolio.ui.theme.SurfaceDark
import com.paperfolio.ui.theme.SurfaceElevatedDark
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.theme.TextTertiary
import com.paperfolio.ui.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyStockBottomSheet(
    tradingSymbol: String,
    companyName: String,
    currentPrice: Double,
    portfolios: List<Portfolio>,
    initialSelectedPortfolioId: Long?,
    onDismiss: () -> Unit,
    onConfirmBuy: (portfolioId: Long, quantity: Int, price: Double) -> Unit
) {
    var selectedPortfolioId by remember {
        mutableStateOf(initialSelectedPortfolioId ?: portfolios.firstOrNull()?.id ?: 0L)
    }
    var quantityText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", currentPrice)) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val qty = quantityText.toIntOrNull() ?: 1
    val customPrice = priceText.toDoubleOrNull() ?: currentPrice
    val totalCost = qty * customPrice

    val selectedPortfolio = portfolios.find { it.id == selectedPortfolioId }
    val availableCash = selectedPortfolio?.cashBalance ?: 0.0
    val hasEnoughCash = availableCash >= totalCost

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BUY $tradingSymbol",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                    Text(
                        text = companyName,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LTP",
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                    Text(
                        text = FormatUtils.formatInr(currentPrice),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Select Target Portfolio (Scrollable Row for responsiveness!)
            Text(
                text = "Park in Portfolio",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (portfolios.isEmpty()) {
                Text(
                    text = "No portfolio found! Please create a portfolio first.",
                    fontSize = 13.sp,
                    color = LossRed
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    portfolios.forEach { portfolio ->
                        val isSelected = portfolio.id == selectedPortfolioId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ProfitGreen.copy(alpha = 0.2f) else SurfaceCard)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ProfitGreen else BorderDark,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedPortfolioId = portfolio.id }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = portfolio.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ProfitGreen else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Available Cash: ${FormatUtils.formatInr(availableCash)}",
                    fontSize = 12.sp,
                    color = if (hasEnoughCash) TextSecondary else LossRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity with Stepper and Quick Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Quantity", fontSize = 12.sp, color = TextSecondary)
                // Quick add chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 25, 50).forEach { addQty ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevatedDark)
                                .clickable {
                                    val current = quantityText.toIntOrNull() ?: 0
                                    quantityText = (current + addQty).toString()
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("+$addQty", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val current = quantityText.toIntOrNull() ?: 1
                        if (current > 1) quantityText = (current - 1).toString()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevatedDark)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfitGreen,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        val current = quantityText.toIntOrNull() ?: 0
                        quantityText = (current + 1).toString()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevatedDark)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Buying Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Price per share (₹)", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = "Reset to LTP",
                    fontSize = 11.sp,
                    color = ProfitGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        priceText = String.format(java.util.Locale.US, "%.2f", currentPrice)
                    }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProfitGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Order Summary Card
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
                        Text("Total Order Amount", fontSize = 12.sp, color = TextSecondary)
                        Text("$qty shares × ₹$priceText", fontSize = 11.sp, color = TextTertiary)
                    }
                    Text(
                        FormatUtils.formatInr(totalCost),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Buy Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (selectedPortfolioId > 0 && qty > 0 && customPrice > 0.0) {
                        onConfirmBuy(selectedPortfolioId, qty, customPrice)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = hasEnoughCash && portfolios.isNotEmpty() && qty > 0 && customPrice > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProfitGreen,
                    contentColor = SurfaceDark
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (!hasEnoughCash) "INSUFFICIENT FUNDS" else "BUY DELIVERY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellStockBottomSheet(
    tradingSymbol: String,
    companyName: String,
    availableQuantity: Int,
    averageBuyPrice: Double,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirmSell: (quantity: Int, price: Double) -> Unit
) {
    var quantityText by remember { mutableStateOf(availableQuantity.toString()) }
    var priceText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", currentPrice)) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val qty = quantityText.toIntOrNull() ?: availableQuantity
    val customPrice = priceText.toDoubleOrNull() ?: currentPrice
    val totalRevenue = qty * customPrice
    val estimatedPnl = (customPrice - averageBuyPrice) * qty
    val isProfit = estimatedPnl >= 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELL $tradingSymbol",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LossRed
                    )
                    Text(
                        text = companyName,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("LTP", fontSize = 10.sp, color = TextTertiary)
                    Text(
                        text = FormatUtils.formatInr(currentPrice),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Holding: $availableQuantity shares  •  Avg: ${FormatUtils.formatInr(averageBuyPrice)}",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quantity to Sell with "SELL ALL" quick chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Quantity to Sell", fontSize = 12.sp, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevatedDark)
                        .clickable { quantityText = availableQuantity.toString() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Sell All ($availableQuantity)", fontSize = 11.sp, color = LossRed, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val current = quantityText.toIntOrNull() ?: 1
                        if (current > 1) quantityText = (current - 1).toString()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevatedDark)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LossRed,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        val current = quantityText.toIntOrNull() ?: 0
                        if (current < availableQuantity) quantityText = (current + 1).toString()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevatedDark)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selling Price per share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Selling Price per share (₹)", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = "Reset to LTP",
                    fontSize = 11.sp,
                    color = LossRed,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        priceText = String.format(java.util.Locale.US, "%.2f", currentPrice)
                    }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LossRed,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Proceeds", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            FormatUtils.formatInr(totalRevenue),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated P&L", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            FormatUtils.formatInr(estimatedPnl, showSign = true),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) ProfitGreen else LossRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    if (qty in 1..availableQuantity && customPrice > 0.0) {
                        onConfirmSell(qty, customPrice)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = qty in 1..availableQuantity && customPrice > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LossRed,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "CONFIRM SELL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
