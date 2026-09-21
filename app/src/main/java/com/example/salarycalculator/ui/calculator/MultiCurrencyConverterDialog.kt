package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.salarycalculator.domain.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MultiCurrencyConverterDialog(
    annualNetTakeHomeGbp: Double,
    annualGrossGbp: Double,
    salaryRepository: SalaryRepository? = null,
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf(PayPeriod.MONTHLY) }
    var selectedCategory by remember { mutableStateOf(CurrencyCategory.ALL) }
    var useNetPay by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val cachedRatesState = salaryRepository?.getCachedCurrencyRates()?.collectAsState(initial = emptyMap())
    val lastFetchState = salaryRepository?.getCurrencyRatesLastFetch()?.collectAsState(initial = "")
    var liveRatesMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    LaunchedEffect(cachedRatesState?.value) {
        cachedRatesState?.value?.let { cached ->
            if (cached.isNotEmpty() && liveRatesMap.isEmpty()) {
                liveRatesMap = cached
            }
        }
    }

    fun refreshLiveRates() {
        scope.launch {
            isRefreshing = true
            val fetched = MultiCurrencyConverterEngine.fetchLiveRatesOnline()
            if (fetched != null && fetched.isNotEmpty()) {
                liveRatesMap = fetched
                val timeStamp = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date())
                salaryRepository?.setCachedCurrencyRates(fetched, timeStamp)
            }
            isRefreshing = false
        }
    }

    val effectiveRates = remember(liveRatesMap) {
        MultiCurrencyConverterEngine.mergeRatesWithDefaults(liveRatesMap)
    }

    val baseAmount = if (useNetPay) annualNetTakeHomeGbp else annualGrossGbp
    val convertedItems = remember(baseAmount, selectedPeriod, effectiveRates, selectedCategory) {
        MultiCurrencyConverterEngine.convertAmount(
            annualGbpAmount = baseAmount,
            period = selectedPeriod,
            customRates = effectiveRates,
            categoryFilter = selectedCategory
        )
    }

    // Infinite rotation for refresh indicator
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Live Refresh Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Multi-Currency & Crypto",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val lastFetchStr = lastFetchState?.value ?: ""
                            Text(
                                text = if (liveRatesMap.isNotEmpty()) "Live FX · Updated $lastFetchStr" else "Standard Reference FX",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (liveRatesMap.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { refreshLiveRates() },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Live Rates",
                            modifier = if (isRefreshing) Modifier.rotate(spinAngle) else Modifier
                        )
                    }
                }

                HorizontalDivider()

                // Category Filter Chips (All, Fiat, Crypto)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyCategory.values().forEach { cat ->
                        val isSel = selectedCategory == cat
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName.split(" ")[0], fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Net / Gross Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = useNetPay,
                        onClick = { useNetPay = true },
                        label = { Text("Net Take-Home Pay") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !useNetPay,
                        onClick = { useNetPay = false },
                        label = { Text("Gross Income") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Period Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PayPeriod.values().forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.displayName) }
                        )
                    }
                }

                // Baseline GBP Header Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Base UK Pay (${selectedPeriod.displayName}):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "£${String.format(Locale.ENGLISH, "%,.2f", baseAmount / selectedPeriod.annualDivisor)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Currency List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(convertedItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(item.currency.flag, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = "${item.currency.code} - ${item.currency.name}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "Rate: 1 GBP = ${item.currency.gbpRate} ${item.currency.code}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = item.formattedString,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }

                // Close Button
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
