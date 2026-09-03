package com.example.salarycalculator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.HmrcDynamicRates
import com.example.salarycalculator.domain.HmrcRateSyncManager
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import kotlinx.coroutines.launch

@Composable
fun HmrcRateSyncDialog(
    salaryRepository: SalaryRepository? = null,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentRates by remember { mutableStateOf(HmrcRateSyncManager.STATUTORY_DEFAULT_RATES) }
    var endpointUrl by remember { mutableStateOf("https://raw.githubusercontent.com/FrancyAlinston/Salarycalculator/main/rates/uk_statutory_rates.json") }
    var isLoading by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Live HMRC Rate Updates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Synchronize live UK HMRC statutory tax bands, National Insurance thresholds, and National Living Wage rates via remote JSON configuration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current Active Rates Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Rates Version", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Emerald60
                            ) {
                                Text(
                                    text = currentRates.version,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Last Effective Date: ${currentRates.lastUpdated}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Remote Endpoint Field
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("Remote Rate JSON Endpoint") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    singleLine = true
                )

                // Action Buttons (Fetch & Restore)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                syncStatusMessage = null
                                val result = HmrcRateSyncManager.fetchRemoteRates(endpointUrl)
                                isLoading = false
                                result.fold(
                                    onSuccess = { rates ->
                                        currentRates = rates
                                        isError = false
                                        syncStatusMessage = "Successfully updated to v${rates.version}!"
                                    },
                                    onFailure = { err ->
                                        isError = true
                                        syncStatusMessage = "Sync failed (${err.localizedMessage ?: "Offline"}). Retaining statutory defaults."
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("Check Updates", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = {
                            currentRates = HmrcRateSyncManager.STATUTORY_DEFAULT_RATES
                            syncStatusMessage = "Restored statutory baseline rates."
                            isError = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Status message alert if present
                if (syncStatusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isError) Rose60.copy(alpha = 0.15f) else Emerald60.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = syncStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) Rose60 else Emerald60,
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Active Statutory Rates Inspection
                Text("Active Thresholds & Rates:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RateItemRow("Personal Allowance", "£${"%,.0f".format(currentRates.personalAllowanceAnnual)} /yr")
                    RateItemRow("Basic Rate (20%)", "£0 to £${"%,.0f".format(currentRates.basicRateLimitAnnual)}")
                    RateItemRow("Higher Rate (40%)", "£${"%,.0f".format(currentRates.basicRateLimitAnnual)} to £${"%,.0f".format(currentRates.additionalRateThresholdAnnual)}")
                    RateItemRow("Additional Rate (45%)", "> £${"%,.0f".format(currentRates.additionalRateThresholdAnnual)}")
                    RateItemRow("Primary NI Threshold (0%)", "£${"%,.0f".format(currentRates.niPrimaryThresholdMonthly)} /mo")
                    RateItemRow("Main NI Rate", "${currentRates.niMainRatePercent}%")
                    RateItemRow("National Living Wage (21+)", "£${"%.2f".format(currentRates.nationalLivingWage21Plus)} /hr")
                    RateItemRow("Minimum Wage (18-20)", "£${"%.2f".format(currentRates.nationalMinimumWage18To20)} /hr")
                    RateItemRow("Apprentice Minimum", "£${"%.2f".format(currentRates.nationalMinimumWageApprentice)} /hr")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun RateItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
