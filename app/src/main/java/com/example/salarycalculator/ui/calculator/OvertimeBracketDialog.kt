package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.OvertimeBracketEngine
import com.example.salarycalculator.domain.OvertimeBracketInput
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun OvertimeBracketDialog(
    initialBaseSalary: Double,
    initialHourlyRate: Double,
    onDismiss: () -> Unit
) {
    var salaryInput by remember { mutableStateOf(String.format("%.0f", if (initialBaseSalary > 0) initialBaseSalary else 35000.0)) }
    var hourlyRateInput by remember { mutableStateOf(String.format("%.2f", if (initialHourlyRate > 0) initialHourlyRate else 18.0)) }
    var selectedMultiplier by remember { mutableDoubleStateOf(1.5) }
    var monthlyOtHoursInput by remember { mutableStateOf("12") }
    var pensionSacrificeInput by remember { mutableStateOf("5.0") }

    val baseSalary = remember(salaryInput) { salaryInput.toDoubleOrNull() ?: 0.0 }
    val hourlyRate = remember(hourlyRateInput) { hourlyRateInput.toDoubleOrNull() ?: 0.0 }
    val monthlyOtHours = remember(monthlyOtHoursInput) { monthlyOtHoursInput.toDoubleOrNull() ?: 0.0 }
    val pensionRate = remember(pensionSacrificeInput) { pensionSacrificeInput.toDoubleOrNull() ?: 0.0 }

    val result = remember(baseSalary, hourlyRate, selectedMultiplier, monthlyOtHours, pensionRate) {
        OvertimeBracketEngine.calculate(
            OvertimeBracketInput(
                baseAnnualGross = baseSalary,
                hourlyRate = hourlyRate,
                overtimeMultiplier = selectedMultiplier,
                monthlyOvertimeHours = monthlyOtHours,
                pensionSacrificeRate = pensionRate
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Overtime Bracket Headroom",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Monitors how many overtime hours you can work before triggering the 40% Higher Rate band (£50,270) or 60% Marginal Tax Trap (£100,000).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Base Salary & Hourly Rate Inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = salaryInput,
                        onValueChange = { salaryInput = it },
                        label = { Text("Base Annual Gross (£)") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hourlyRateInput,
                        onValueChange = { hourlyRateInput = it },
                        label = { Text("Base Rate (£/hr)") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }

                // Overtime Multiplier Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Overtime Rate Multiplier:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1.0 to "1.0x", 1.25 to "1.25x", 1.5 to "1.5x (Time & Half)", 2.0 to "2.0x (Double)").forEach { (mult, label) ->
                            FilterChip(
                                selected = selectedMultiplier == mult,
                                onClick = { selectedMultiplier = mult },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = monthlyOtHoursInput,
                        onValueChange = { monthlyOtHoursInput = it },
                        label = { Text("Monthly OT Hours") },
                        suffix = { Text("hrs/mo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pensionSacrificeInput,
                        onValueChange = { pensionSacrificeInput = it },
                        label = { Text("Pension Sacrifice %") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // 40% Higher Rate Headroom Card (£50,270)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (result.isHigherRateCrossed) Rose60.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (result.isHigherRateCrossed) Rose60 else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (result.isHigherRateCrossed) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (result.isHigherRateCrossed) Rose60 else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("40% Higher Rate Band (£50,270)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (result.isHigherRateCrossed) {
                            Text(
                                text = "Crossed! You are £${String.format("%,.0f", result.taxableGrossAfterPension - 50270.0)} into the 40% tax band. Overtime is now taxed at 40% PAYE + 2% NI.",
                                fontSize = 11.sp,
                                color = Rose60,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• Salary Sacrifice Remedy: Sacrifice £${String.format("%,.0f", result.recommendedPensionSacrificeToStayBelowHigherRate)}/mo into pension to pull back below £50,270.",
                                fontSize = 11.sp
                            )
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining Headroom:", fontSize = 12.sp)
                                Text("£" + String.format("%,.2f", result.higherRateHeadroomPounds), fontWeight = FontWeight.Bold, color = Emerald60, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Safe OT Hours Capacity:", fontSize = 12.sp)
                                Text("${String.format("%.1f", result.maxMonthlyOvertimeHoursBeforeHigherRate)} hrs/month max", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 60% Marginal Tax Trap Headroom Card (£100,000)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (result.isTaxTrapCrossed) Amber60.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (result.isTaxTrapCrossed) Amber60 else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (result.isTaxTrapCrossed) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (result.isTaxTrapCrossed) Amber60 else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("60% Marginal Tax Trap (£100k)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (result.isTaxTrapCrossed) {
                            Text(
                                text = "Crossed! Earnings above £100,000 lose personal allowance (£1 per £2 earned), creating an effective 60% marginal tax rate.",
                                fontSize = 11.sp,
                                color = Amber60,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• Salary Sacrifice Remedy: Sacrifice £${String.format("%,.0f", result.recommendedPensionSacrificeToStayBelowTaxTrap)}/mo into pension to restore 100% personal allowance.",
                                fontSize = 11.sp
                            )
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining Headroom:", fontSize = 12.sp)
                                Text("£" + String.format("%,.2f", result.taxTrapHeadroomPounds), fontWeight = FontWeight.Bold, color = Emerald60, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Safe OT Hours Capacity:", fontSize = 12.sp)
                                Text("${String.format("%.1f", result.maxMonthlyOvertimeHoursBeforeTaxTrap)} hrs/month max", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Close")
            }
        }
    )
}
