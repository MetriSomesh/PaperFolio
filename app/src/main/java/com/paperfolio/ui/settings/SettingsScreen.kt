package com.paperfolio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paperfolio.BuildConfig
import com.paperfolio.ui.components.VeltoCard
import com.paperfolio.ui.components.VeltoFilterChip
import com.paperfolio.ui.theme.AppBackground
import com.paperfolio.ui.theme.Border
import com.paperfolio.ui.theme.VeltoGreen
import com.paperfolio.ui.theme.Lime
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.LossRedBg
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.ProfitGreenBg
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var newStockSymbol by remember { mutableStateOf("") }
    var selectedExchange by remember { mutableStateOf("NSE") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Tools",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add Newly Listed Stock Section
            VeltoCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, tint = VeltoGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add newly listed stock", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "If a new IPO or stock just got listed, enter its symbol below. The app will validate it via live Yahoo Finance market feeds and register it into your local offline database.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Stock Symbol", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newStockSymbol,
                        onValueChange = {
                            newStockSymbol = it.uppercase()
                            viewModel.clearMessages()
                        },
                        placeholder = { Text("e.g. SWIGGY, ZOMATO, NYKAA", color = TextTertiary, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VeltoGreen,
                            unfocusedBorderColor = Border,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Exchange", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("NSE", "BSE").forEach { exch ->
                            VeltoFilterChip(
                                text = exch,
                                selected = exch == selectedExchange,
                                onClick = { selectedExchange = exch }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.addNewStock(newStockSymbol, selectedExchange)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = newStockSymbol.isNotBlank() && !uiState.isAddingStock,
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = VeltoGreen),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        if (uiState.isAddingStock) {
                            CircularProgressIndicator(color = VeltoGreen, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Validate & add to app", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    // Success Feedback
                    uiState.addStockSuccessMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ProfitGreenBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = ProfitGreen, fontSize = 12.sp)
                        }
                    }

                    // Error Feedback
                    uiState.addStockErrorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LossRedBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LossRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(err, color = LossRed, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Database Info Card
            VeltoCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Stock master database", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Stocks in App", fontSize = 13.sp, color = TextSecondary)
                        Text("${uiState.totalStockCount} instruments", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VeltoGreen)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Market Segments", fontSize = 13.sp, color = TextSecondary)
                        Text("NSE Equities, BSE Equities & ETFs", fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }

            // About App Card
            VeltoCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About PaperFolio", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "100% Free Indian Stock Market Paper Trading App. Self-contained on your device with no external server or login required.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Was hardcoded "Version 1.0.1" — now reads the real
                    // build version so it can never go stale again.
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (Pure Delivery & Cash)",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}
