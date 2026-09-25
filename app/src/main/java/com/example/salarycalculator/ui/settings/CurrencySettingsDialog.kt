package com.example.salarycalculator.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.ConvertedCurrencies
import com.example.salarycalculator.domain.LiveFxWorker
import com.example.salarycalculator.domain.MultiCurrencyConverterEngine
import com.example.salarycalculator.domain.SalaryRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrencySettingsDialog(
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val preferredCurrency by salaryRepository.getPreferredCurrency().collectAsState(initial = "EUR")
    val secondaryCurrency by salaryRepository.getSecondaryCurrency().collectAsState(initial = "USD")
    val autoUpdateRates by salaryRepository.getAutoUpdateRates().collectAsState(initial = true)
    val cachedRates by salaryRepository.getCachedCurrencyRates().collectAsState(initial = emptyMap())
    val lastFetchStr by salaryRepository.getCurrencyRatesLastFetch().collectAsState(initial = "")

    var primaryCodeInput by remember(preferredCurrency) { mutableStateOf(preferredCurrency) }
    var secondaryCodeInput by remember(secondaryCurrency) { mutableStateOf(secondaryCurrency) }
    var autoUpdateEnabled by remember(autoUpdateRates) { mutableStateOf(autoUpdateRates) }

    var primaryRateInput by remember(primaryCodeInput, cachedRates) {
        val rate = cachedRates[primaryCodeInput.uppercase()]
            ?: MultiCurrencyConverterEngine.getCurrencyRate(primaryCodeInput).gbpRate
        mutableStateOf(String.format(Locale.ENGLISH, "%.4f", rate))
    }

    var secondaryRateInput by remember(secondaryCodeInput, cachedRates) {
        val rate = cachedRates[secondaryCodeInput.uppercase()]
            ?: MultiCurrencyConverterEngine.getCurrencyRate(secondaryCodeInput).gbpRate
        mutableStateOf(String.format(Locale.ENGLISH, "%.4f", rate))
    }

    var isSyncingLive by remember { mutableStateOf(false) }
    var syncFeedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Universal Currency Conversion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    text = "Configure live currency conversion for your take-home pay to any world currency or crypto asset. Live rates update automatically in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Live Sync Status Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Live FX Auto-Update", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (lastFetchStr.isNotBlank()) "Last synced: $lastFetchStr" else "160+ currencies supported",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoUpdateEnabled,
                                onCheckedChange = { autoUpdateEnabled = it }
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSyncingLive = true
                                    val fetched = MultiCurrencyConverterEngine.fetchLiveRatesOnline()
                                    isSyncingLive = false
                                    if (fetched != null && fetched.isNotEmpty()) {
                                        val timestamp = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date())
                                        salaryRepository.setCachedCurrencyRates(fetched, timestamp)
                                        salaryRepository.setCachedCurrencyRatesJson(Json.encodeToString(fetched))
                                        val pRate = fetched[primaryCodeInput.uppercase()]
                                        val sRate = fetched[secondaryCodeInput.uppercase()]
                                        if (pRate != null) primaryRateInput = String.format(Locale.ENGLISH, "%.4f", pRate)
                                        if (sRate != null) secondaryRateInput = String.format(Locale.ENGLISH, "%.4f", sRate)
                                        syncFeedback = "✓ Updated ${fetched.size} live exchange rates"
                                    } else {
                                        syncFeedback = "⚠ Network error: Using cached/default rates"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSyncingLive
                        ) {
                            if (isSyncingLive) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetching Live Rates...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetch Live Rates Now", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        syncFeedback?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Primary Currency Selection
                Text("Primary Display Currency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                // Quick selector chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickChoices = listOf("EUR", "USD", "INR", "PHP", "CAD", "AUD", "BTC", "ETH", "AED", "PLN", "NGN")
                    items(quickChoices) { code ->
                        val rateObj = MultiCurrencyConverterEngine.getCurrencyRate(code)
                        FilterChip(
                            selected = primaryCodeInput.equals(code, ignoreCase = true),
                            onClick = {
                                primaryCodeInput = code
                                val r = cachedRates[code] ?: rateObj.gbpRate
                                primaryRateInput = String.format(Locale.ENGLISH, "%.4f", r)
                            },
                            label = { Text("${rateObj.flag} $code") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = primaryCodeInput,
                        onValueChange = {
                            primaryCodeInput = it.take(5).uppercase()
                            val r = cachedRates[primaryCodeInput]
                                ?: MultiCurrencyConverterEngine.getCurrencyRate(primaryCodeInput).gbpRate
                            primaryRateInput = String.format(Locale.ENGLISH, "%.4f", r)
                        },
                        label = { Text("Code (e.g. EUR)") },
                        modifier = Modifier.weight(0.45f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = primaryRateInput,
                        onValueChange = { primaryRateInput = it },
                        label = { Text("1 GBP (£) = ") },
                        modifier = Modifier.weight(0.55f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                HorizontalDivider()

                // Secondary Currency Selection
                Text("Secondary Display Currency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickChoices = listOf("USD", "EUR", "INR", "JPY", "SGD", "NZD", "ZAR", "SOL", "CHF", "BRL", "KES")
                    items(quickChoices) { code ->
                        val rateObj = MultiCurrencyConverterEngine.getCurrencyRate(code)
                        FilterChip(
                            selected = secondaryCodeInput.equals(code, ignoreCase = true),
                            onClick = {
                                secondaryCodeInput = code
                                val r = cachedRates[code] ?: rateObj.gbpRate
                                secondaryRateInput = String.format(Locale.ENGLISH, "%.4f", r)
                            },
                            label = { Text("${rateObj.flag} $code") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = secondaryCodeInput,
                        onValueChange = {
                            secondaryCodeInput = it.take(5).uppercase()
                            val r = cachedRates[secondaryCodeInput]
                                ?: MultiCurrencyConverterEngine.getCurrencyRate(secondaryCodeInput).gbpRate
                            secondaryRateInput = String.format(Locale.ENGLISH, "%.4f", r)
                        },
                        label = { Text("Code (e.g. USD)") },
                        modifier = Modifier.weight(0.45f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = secondaryRateInput,
                        onValueChange = { secondaryRateInput = it },
                        label = { Text("1 GBP (£) = ") },
                        modifier = Modifier.weight(0.55f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val pCode = primaryCodeInput.trim().uppercase().ifBlank { "EUR" }
                        val sCode = secondaryCodeInput.trim().uppercase().ifBlank { "USD" }
                        val pRate = primaryRateInput.toDoubleOrNull()
                            ?: MultiCurrencyConverterEngine.getCurrencyRate(pCode).gbpRate
                        val sRate = secondaryRateInput.toDoubleOrNull()
                            ?: MultiCurrencyConverterEngine.getCurrencyRate(sCode).gbpRate

                        salaryRepository.setPreferredCurrency(pCode)
                        salaryRepository.setSecondaryCurrency(sCode)
                        salaryRepository.setAutoUpdateRates(autoUpdateEnabled)

                        // Update cached map with manual overrides if entered
                        val updatedMap = cachedRates.toMutableMap()
                        updatedMap[pCode] = pRate
                        updatedMap[sCode] = sRate
                        val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date())
                        salaryRepository.setCachedCurrencyRates(updatedMap, timeStr)
                        salaryRepository.setCachedCurrencyRatesJson(Json.encodeToString(updatedMap))

                        if (pCode == "EUR") salaryRepository.setCustomEurRate(pRate)
                        if (sCode == "USD") salaryRepository.setCustomUsdRate(sRate)

                        onDismiss()
                    }
                }
            ) {
                Text("Save & Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
