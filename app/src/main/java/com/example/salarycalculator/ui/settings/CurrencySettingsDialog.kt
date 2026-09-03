package com.example.salarycalculator.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.domain.ConvertedCurrencies
import com.example.salarycalculator.domain.SalaryRepository
import kotlinx.coroutines.launch

@Composable
fun CurrencySettingsDialog(
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val initialEurRate by salaryRepository.getCustomEurRate().collectAsState(initial = ConvertedCurrencies.DEFAULT_EUR_RATE)
    val initialUsdRate by salaryRepository.getCustomUsdRate().collectAsState(initial = ConvertedCurrencies.DEFAULT_USD_RATE)

    var eurInput by remember(initialEurRate) { mutableStateOf("%.4f".format(initialEurRate)) }
    var usdInput by remember(initialUsdRate) { mutableStateOf("%.4f".format(initialUsdRate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Foreign Exchange Rates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Customize the live exchange conversion rates used to estimate your take-home pay in Euros (€) and US Dollars ($).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = eurInput,
                    onValueChange = { eurInput = it },
                    label = { Text("1 GBP (£) to EUR (€)") },
                    prefix = { Text("€ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = usdInput,
                    onValueChange = { usdInput = it },
                    label = { Text("1 GBP (£) to USD ($)") },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                var isSyncingLive by remember { mutableStateOf(false) }
                var syncFeedback by remember { mutableStateOf<String?>(null) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                isSyncingLive = true
                                val result = com.example.salarycalculator.domain.LiveFxSyncEngine.fetchLiveRates()
                                isSyncingLive = false
                                eurInput = "%.4f".format(result.eurRate)
                                usdInput = "%.4f".format(result.usdRate)
                                syncFeedback = result.message
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSyncingLive
                    ) {
                        if (isSyncingLive) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Live FX", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            eurInput = "%.4f".format(ConvertedCurrencies.DEFAULT_EUR_RATE)
                            usdInput = "%.4f".format(ConvertedCurrencies.DEFAULT_USD_RATE)
                            syncFeedback = "Reset to baseline rates."
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset Default", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (syncFeedback != null) {
                    Text(
                        text = syncFeedback!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val eur = eurInput.toDoubleOrNull() ?: ConvertedCurrencies.DEFAULT_EUR_RATE
                    val usd = usdInput.toDoubleOrNull() ?: ConvertedCurrencies.DEFAULT_USD_RATE
                    coroutineScope.launch {
                        salaryRepository.setCustomEurRate(eur)
                        salaryRepository.setCustomUsdRate(usd)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Rates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
