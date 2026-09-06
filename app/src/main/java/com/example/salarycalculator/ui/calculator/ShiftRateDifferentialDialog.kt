package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.domain.ShiftRateDifferentialEngine
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftRateDifferentialDialog(
    currentHourlyRate: Double,
    currentStandardHours: Double,
    totalLoggedHours: Double,
    onDismiss: () -> Unit
) {
    var baseRate by remember { mutableDoubleStateOf(if (currentHourlyRate > 0) currentHourlyRate else 12.82) }
    var dayHours by remember { mutableDoubleStateOf(if (totalLoggedHours > 36.0) totalLoggedHours - 36.0 else 120.0) }
    
    // Night Shift
    var nightHours by remember { mutableDoubleStateOf(36.0) }
    var nightUpliftType by remember { mutableStateOf(ShiftRateDifferentialEngine.UpliftType.HOURLY_ADDITION) }
    var nightUpliftValue by remember { mutableDoubleStateOf(2.00) }

    // Weekend
    var weekendHours by remember { mutableDoubleStateOf(24.0) }
    var weekendMultiplier by remember { mutableDoubleStateOf(1.25) }

    // Bank Holiday
    var bankHolidayHours by remember { mutableDoubleStateOf(0.0) }
    var bankHolidayMultiplier by remember { mutableDoubleStateOf(2.0) }

    // Sleep-in Duties
    var sleepInDutiesCount by remember { mutableIntStateOf(0) }
    var sleepInAllowance by remember { mutableDoubleStateOf(50.0) }

    val input = remember(
        baseRate,
        dayHours,
        nightHours,
        nightUpliftType,
        nightUpliftValue,
        weekendHours,
        weekendMultiplier,
        bankHolidayHours,
        bankHolidayMultiplier,
        sleepInDutiesCount,
        sleepInAllowance
    ) {
        ShiftRateDifferentialEngine.ShiftDifferentialInput(
            baseHourlyRate = baseRate,
            standardDayHours = dayHours,
            nightHours = nightHours,
            nightUpliftType = nightUpliftType,
            nightUpliftValue = nightUpliftValue,
            weekendHours = weekendHours,
            weekendMultiplier = weekendMultiplier,
            bankHolidayHours = bankHolidayHours,
            bankHolidayMultiplier = bankHolidayMultiplier,
            sleepInDutiesCount = sleepInDutiesCount,
            sleepInAllowancePerDuty = sleepInAllowance
        )
    }

    val result = remember(input) {
        ShiftRateDifferentialEngine.calculateDifferentials(input)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Shift Rate Differentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Nights, Weekends & Sleep-Ins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Blended Rate & Gross Pay Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Total Enhanced Gross Pay",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "£${String.format(Locale.UK, "%.2f", result.totalGrossPay)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "+£${String.format(Locale.UK, "%.2f", result.totalDifferentialUplift)} uplift",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Blended Rate: £${String.format(Locale.UK, "%.2f", result.blendedHourlyRate)}/hr",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Total Hours: ${String.format(Locale.UK, "%.1f", result.totalWorkingHours)}h",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // 1. Base Rate & Standard Hours
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Base Rate & Standard Day Hours", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (baseRate > 0) baseRate.toString() else "",
                                onValueChange = { baseRate = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Base Rate (£/hr)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = if (dayHours > 0) dayHours.toString() else "",
                                onValueChange = { dayHours = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Day Hours (h)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                // 2. Night Shift Enhancements
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🌙 Night Shift Enhancement", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Rate: £${String.format(Locale.UK, "%.2f", result.nightEffectiveRate)}/hr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (nightHours > 0) nightHours.toString() else "",
                                onValueChange = { nightHours = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Night Hours") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = if (nightUpliftValue > 0) nightUpliftValue.toString() else "",
                                onValueChange = { nightUpliftValue = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text(if (nightUpliftType == ShiftRateDifferentialEngine.UpliftType.HOURLY_ADDITION) "+£/hr" else "Multiplier") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1.50 to "+£1.50", 2.00 to "+£2.00", 2.50 to "+£2.50", 3.00 to "+£3.00").forEach { (valAmt, label) ->
                                FilterChip(
                                    selected = nightUpliftType == ShiftRateDifferentialEngine.UpliftType.HOURLY_ADDITION && nightUpliftValue == valAmt,
                                    onClick = {
                                        nightUpliftType = ShiftRateDifferentialEngine.UpliftType.HOURLY_ADDITION
                                        nightUpliftValue = valAmt
                                    },
                                    label = { Text(label, maxLines = 1) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Weekend Enhancements
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🗓️ Weekend Premium", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Rate: £${String.format(Locale.UK, "%.2f", result.weekendEffectiveRate)}/hr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (weekendHours > 0) weekendHours.toString() else "",
                                onValueChange = { weekendHours = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Weekend Hours") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = weekendMultiplier.toString(),
                                onValueChange = { weekendMultiplier = it.toDoubleOrNull() ?: 1.0 },
                                label = { Text("Multiplier (x)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1.15 to "1.15x", 1.25 to "1.25x", 1.33 to "1.33x", 1.50 to "1.50x").forEach { (mult, label) ->
                                FilterChip(
                                    selected = weekendMultiplier == mult,
                                    onClick = { weekendMultiplier = mult },
                                    label = { Text(label, maxLines = 1) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Bank Holiday & Sleep-Ins
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⭐ Bank Holidays & Sleep-In Duties", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (bankHolidayHours > 0) bankHolidayHours.toString() else "",
                                onValueChange = { bankHolidayHours = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Bank Hol Hours") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = if (sleepInDutiesCount > 0) sleepInDutiesCount.toString() else "",
                                onValueChange = { sleepInDutiesCount = it.toIntOrNull() ?: 0 },
                                label = { Text("Sleep-Ins (#)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        if (sleepInDutiesCount > 0) {
                            OutlinedTextField(
                                value = sleepInAllowance.toString(),
                                onValueChange = { sleepInAllowance = it.toDoubleOrNull() ?: 50.0 },
                                label = { Text("Sleep-In Allowance per Shift (£)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
