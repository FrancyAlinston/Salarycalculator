package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.StatutoryLeaveCalculator
import com.example.salarycalculator.domain.StatutoryLeaveType
import com.example.salarycalculator.domain.TaxRegion
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60

@Composable
fun StatutoryLeaveDialog(
    initialWeeklyGross: Double = 600.0,
    taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(StatutoryLeaveType.SICK_PAY_SSP) }
    var weeklyEarningsInput by remember { mutableStateOf(initialWeeklyGross.toInt().toString()) }
    var weeksDuration by remember { mutableIntStateOf(4) }

    val weeklyEarnings = weeklyEarningsInput.toDoubleOrNull() ?: initialWeeklyGross

    // Adjust duration limits based on type
    val maxWeeks = when (selectedType) {
        StatutoryLeaveType.SICK_PAY_SSP -> 28
        StatutoryLeaveType.MATERNITY_SMP -> 39
        StatutoryLeaveType.PATERNITY_SPP -> 2
    }

    LaunchedEffect(selectedType) {
        weeksDuration = when (selectedType) {
            StatutoryLeaveType.SICK_PAY_SSP -> 4
            StatutoryLeaveType.MATERNITY_SMP -> 26
            StatutoryLeaveType.PATERNITY_SPP -> 2
        }
    }

    val result = remember(selectedType, weeklyEarnings, weeksDuration, taxRegion) {
        StatutoryLeaveCalculator.calculate(
            leaveType = selectedType,
            averageWeeklyEarnings = weeklyEarnings,
            durationWeeks = weeksDuration,
            taxRegion = taxRegion
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    when (selectedType) {
                        StatutoryLeaveType.SICK_PAY_SSP -> Icons.Default.Healing
                        StatutoryLeaveType.MATERNITY_SMP -> Icons.Default.ChildFriendly
                        StatutoryLeaveType.PATERNITY_SPP -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Statutory Leave Calculator",
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
                    text = "Calculate statutory minimum UK entitlements, tax deductions, and take-home pay during sick or parental leave.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Leave Type Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatutoryLeaveType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName.split(" (")[0], style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Average Weekly Earnings Input
                OutlinedTextField(
                    value = weeklyEarningsInput,
                    onValueChange = { weeklyEarningsInput = it },
                    label = { Text("Average Weekly Earnings (AWE)") },
                    prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Duration Slider
                Text(
                    text = "Leave Duration: $weeksDuration weeks (Max $maxWeeks wks)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = weeksDuration.toFloat().coerceIn(1f, maxWeeks.toFloat()),
                    onValueChange = { weeksDuration = it.toInt() },
                    valueRange = 1f..maxWeeks.toFloat(),
                    steps = if (maxWeeks > 1) maxWeeks - 2 else 0
                )

                // Summary Comparison Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${result.durationWeeks}-Week Leave Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Statutory Gross:", style = MaterialTheme.typography.bodyMedium)
                            Text("£${"%,.2f".format(result.totalStatutoryGross)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Emerald60)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Avg Weekly Statutory:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(result.averageWeeklyStatutoryPay)} /wk", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Est. Net Take-Home for Period:", style = MaterialTheme.typography.bodyMedium)
                            Text("£${"%,.2f".format(result.estimatedNetPayForPeriod)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Emerald60)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            text = "Compared to Regular Working Pay:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Regular Net for $weeksDuration weeks:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(result.regularEstimatedNetForPeriod)}", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gross Income Difference:", style = MaterialTheme.typography.bodySmall)
                            Text("-£${"%,.2f".format(result.grossIncomeLoss)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Rose60)
                        }
                    }
                }

                // Weekly Rates Breakdown List
                Text("Payment Schedule:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    result.weeklyBreakdown.take(8).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Week ${item.weekNumber}: ${item.rateDescription}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                Text("£${"%,.2f".format(item.statutoryAmount)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (result.weeklyBreakdown.size > 8) {
                        Text("+ ${result.weeklyBreakdown.size - 8} more weeks at standard statutory rate...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
