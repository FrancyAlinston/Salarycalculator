package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60

/**
 * Interactive Shift Swap & Coverage Forecaster Dialog.
 * Simulates the statutory PAYE and NI tax impact when swapping shifts with colleagues,
 * picking up overtime coverage, or giving away shifts.
 */
@Composable
fun ShiftSwapForecasterDialog(
    hourlyRate: Double = 15.0,
    standardShiftHours: Double = 12.0,
    currentMonthlyGross: Double = 2400.0,
    taxCode: String = "1257L",
    availableColleagues: List<RotaStaffMember> = DutyRotaOcrEngine.OCTOBER_2026_REFERENCE_STAFF,
    initialDayOfMonth: Int = 1,
    onDismiss: () -> Unit,
    onApplySwapToSchedule: ((day: Int, newHours: Double, note: String) -> Unit)? = null
) {
    var selectedSwapType by remember { mutableStateOf(ShiftSwapType.PICK_UP_COVER) }
    var selectedColleague by remember { mutableStateOf(availableColleagues.firstOrNull { it.id != "francy_dsilva" } ?: availableColleagues.first()) }
    var selectedDay by remember { mutableIntStateOf(initialDayOfMonth) }
    var currentShiftHoursInput by remember { mutableStateOf(standardShiftHours.toString()) }
    var newShiftHoursInput by remember { mutableStateOf(standardShiftHours.toString()) }
    var selectedOtMultiplier by remember { mutableDoubleStateOf(1.5) }

    val currentHours = currentShiftHoursInput.toDoubleOrNull() ?: standardShiftHours
    val newHours = newShiftHoursInput.toDoubleOrNull() ?: standardShiftHours

    val simulationResult = remember(
        selectedSwapType,
        selectedColleague,
        selectedDay,
        currentHours,
        newHours,
        hourlyRate,
        currentMonthlyGross,
        taxCode,
        selectedOtMultiplier
    ) {
        ShiftSwapEngine.calculateShiftSwapImpact(
            ShiftSwapSimulationInput(
                colleagueName = selectedColleague.name,
                dayOfMonth = selectedDay,
                swapType = selectedSwapType,
                currentShiftHours = currentHours,
                newShiftHours = newHours,
                baseHourlyRate = hourlyRate,
                currentMonthlyGross = currentMonthlyGross,
                taxCode = taxCode,
                overtimeMultiplier = selectedOtMultiplier,
                isOvertimeCover = true
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Shift Swap & Coverage Forecaster",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Statutory PAYE & NI Impact Calculator",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Swap Type Selector Tabs
                    Text(
                        text = "1. Select Action Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ShiftSwapType.PICK_UP_COVER to "Pick Up (+OT)",
                            ShiftSwapType.DIRECT_SWAP to "Direct Swap",
                            ShiftSwapType.GIVE_AWAY to "Give Away"
                        ).forEach { (type, label) ->
                            val isSelected = selectedSwapType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSwapType = type },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    // 2. Colleague Selector
                    Text(
                        text = "2. Select Colleague from Roster",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Colleague: ${selectedColleague.name} (${selectedColleague.role})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableColleagues.take(4).forEach { colleague ->
                                    val isSelected = selectedColleague.id == colleague.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedColleague = colleague },
                                        label = { Text(colleague.name.split(" ").firstOrNull() ?: colleague.name, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Shift Configuration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedDay.toString(),
                            onValueChange = {
                                val d = it.toIntOrNull()
                                if (d != null && d in 1..31) selectedDay = d
                            },
                            label = { Text("Day of Month") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (selectedSwapType == ShiftSwapType.PICK_UP_COVER) {
                            OutlinedTextField(
                                value = newShiftHoursInput,
                                onValueChange = { newShiftHoursInput = it },
                                label = { Text("Cover Hours") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        } else if (selectedSwapType == ShiftSwapType.DIRECT_SWAP) {
                            OutlinedTextField(
                                value = currentShiftHoursInput,
                                onValueChange = { currentShiftHoursInput = it },
                                label = { Text("Your Hours") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }

                    // Overtime Rate Multiplier (for Cover Shifts)
                    if (selectedSwapType == ShiftSwapType.PICK_UP_COVER) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Overtime Multiplier:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1.0, 1.25, 1.5, 2.0).forEach { mul ->
                                    FilterChip(
                                        selected = selectedOtMultiplier == mul,
                                        onClick = { selectedOtMultiplier = mul },
                                        label = { Text("${mul}x", fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Financial Impact Analysis Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (simulationResult.netDelta >= 0) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (simulationResult.netDelta >= 0) Emerald60.copy(alpha = 0.5f) else Rose60.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Estimated Net Take-Home Delta",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = (if (simulationResult.netDelta >= 0) "+" else "") + "£${"%.2f".format(simulationResult.netDelta)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (simulationResult.netDelta >= 0) Emerald60 else Rose60
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (simulationResult.netDelta >= 0) Emerald60 else Amber60
                                ) {
                                    Text(
                                        text = "${"%.1f".format(simulationResult.marginalRetentionRate)}% Cash Retention",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricColumn(
                                    label = "Gross Change",
                                    value = (if (simulationResult.grossDelta >= 0) "+" else "") + "£${"%.2f".format(simulationResult.grossDelta)}"
                                )
                                MetricColumn(
                                    label = "PAYE Tax (20%)",
                                    value = (if (simulationResult.taxDelta >= 0) "+" else "") + "£${"%.2f".format(simulationResult.taxDelta)}"
                                )
                                MetricColumn(
                                    label = "Class 1 NI (8%)",
                                    value = (if (simulationResult.niDelta >= 0) "+" else "") + "£${"%.2f".format(simulationResult.niDelta)}"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricColumn(
                                    label = "New Monthly Gross",
                                    value = "£${"%,.2f".format(simulationResult.postSwapGrossMonthly)}"
                                )
                                MetricColumn(
                                    label = "New Monthly Net",
                                    value = "£${"%,.2f".format(simulationResult.postSwapNetMonthly)}"
                                )
                                MetricColumn(
                                    label = "Net Rate / Hour",
                                    value = "£${"%.2f".format(simulationResult.netHourlyYield)}/h"
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = simulationResult.summaryAdvice,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Apply Button
                Button(
                    onClick = {
                        val appliedHours = when (selectedSwapType) {
                            ShiftSwapType.PICK_UP_COVER -> -newHours // Recorded as overtime shift
                            ShiftSwapType.GIVE_AWAY -> 0.0
                            ShiftSwapType.DIRECT_SWAP -> newHours
                        }
                        val note = "${selectedSwapType.label} with ${selectedColleague.name}"
                        onApplySwapToSchedule?.invoke(selectedDay, appliedHours, note)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald60)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Apply Shift Change to Calendar (Day $selectedDay)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
