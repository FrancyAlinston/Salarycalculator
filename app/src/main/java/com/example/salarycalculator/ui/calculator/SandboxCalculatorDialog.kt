package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60

@Composable
fun SandboxCalculatorDialog(
    baselineDays: Double,
    baselineHoursPerDay: Double,
    baselineOvertimeHours: Double,
    hourlyRate: Double = 15.0,
    taxCode: String = "1257L",
    taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    taxYear: TaxYear = TaxYear.YEAR_2024_2025,
    pensionRate: Double = 5.0,
    studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
    hasMarriageAllowance: Boolean = false,
    hasBlindPersonsAllowance: Boolean = false,
    onApplyToCalculator: ((days: Double, hoursPerDay: Double, overtimeHours: Double, bonus: Double, commission: Double) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var scenarioDaysInput by remember { mutableStateOf(baselineDays.toInt().toString()) }
    var scenarioHoursPerDayInput by remember { mutableStateOf(baselineHoursPerDay.toString()) }
    var scenarioOtHoursInput by remember { mutableStateOf(if (baselineOvertimeHours > 0) baselineOvertimeHours.toString() else "0") }
    var selectedOtMultiplier by remember { mutableDoubleStateOf(1.5) }

    var scenarioBankHolidayHoursInput by remember { mutableStateOf("0") }
    var selectedBankHolidayMultiplier by remember { mutableDoubleStateOf(2.0) }

    var scenarioBonusInput by remember { mutableStateOf("") }
    var scenarioCommissionInput by remember { mutableStateOf("") }

    val days = scenarioDaysInput.toDoubleOrNull() ?: 0.0
    val hoursPerDay = scenarioHoursPerDayInput.toDoubleOrNull() ?: 8.0
    val otHours = scenarioOtHoursInput.toDoubleOrNull() ?: 0.0
    val bhHours = scenarioBankHolidayHoursInput.toDoubleOrNull() ?: 0.0
    val bonus = scenarioBonusInput.toDoubleOrNull() ?: 0.0
    val commission = scenarioCommissionInput.toDoubleOrNull() ?: 0.0

    // Baseline calculation
    val baselineGross = (baselineDays * baselineHoursPerDay * hourlyRate) + (baselineOvertimeHours * hourlyRate * 1.5)
    val baselineReport = remember(baselineGross, taxCode, taxRegion, taxYear, pensionRate, studentLoanPlan, hasMarriageAllowance, hasBlindPersonsAllowance) {
        TaxCalculator.calculateTax(
            grossPay = baselineGross,
            taxCode = taxCode,
            isMonthly = true,
            region = taxRegion,
            taxYear = taxYear,
            pensionRatePercent = pensionRate,
            studentLoanPlan = studentLoanPlan,
            hasMarriageAllowance = hasMarriageAllowance,
            hasBlindPersonsAllowance = hasBlindPersonsAllowance
        )
    }

    // Scenario calculation
    val scenarioGrossStandard = days * hoursPerDay * hourlyRate
    val scenarioGrossOvertime = otHours * hourlyRate * selectedOtMultiplier
    val scenarioGrossBankHoliday = bhHours * hourlyRate * selectedBankHolidayMultiplier
    val scenarioVariableGross = bonus + commission
    val scenarioTotalGross = scenarioGrossStandard + scenarioGrossOvertime + scenarioGrossBankHoliday + scenarioVariableGross

    val scenarioReport = remember(scenarioTotalGross, taxCode, taxRegion, taxYear, pensionRate, studentLoanPlan, hasMarriageAllowance, hasBlindPersonsAllowance) {
        TaxCalculator.calculateTax(
            grossPay = scenarioTotalGross,
            taxCode = taxCode,
            isMonthly = true,
            region = taxRegion,
            taxYear = taxYear,
            pensionRatePercent = pensionRate,
            studentLoanPlan = studentLoanPlan,
            hasMarriageAllowance = hasMarriageAllowance,
            hasBlindPersonsAllowance = hasBlindPersonsAllowance
        )
    }

    val netDelta = scenarioReport.netPay - baselineReport.netPay
    val pctDelta = if (baselineReport.netPay > 0) (netDelta / baselineReport.netPay) * 100 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("What-If Scenario Sandbox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Model extra shifts, overtime & bonuses isolated from live schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // 1. Take-Home Comparison Hero Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scenario Estimated Net", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (netDelta >= 0) Emerald60.copy(alpha = 0.2f) else Rose60.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (netDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (netDelta >= 0) Emerald60 else Rose60
                                    )
                                    Text(
                                        text = "${if (netDelta >= 0) "+" else ""}£${"%,.2f".format(netDelta)} (${if (pctDelta >= 0) "+" else ""}${"%.1f".format(pctDelta)}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netDelta >= 0) Emerald60 else Rose60
                                    )
                                }
                            }
                        }

                        Text(
                            text = "£${"%,.2f".format(scenarioReport.netPay)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Baseline: £${"%,.2f".format(baselineReport.netPay)} Net (£${"%,.2f".format(baselineReport.grossPay)} Gross)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gross: £${"%,.2f".format(scenarioReport.grossPay)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 2. Scenario Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            scenarioOtHoursInput = ((scenarioOtHoursInput.toDoubleOrNull() ?: 0.0) + 16.0).toString()
                            selectedOtMultiplier = 1.5
                        },
                        label = { Text("+2 Weekend OT (16h @ 1.5x)", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = {
                            scenarioBankHolidayHoursInput = ((scenarioBankHolidayHoursInput.toDoubleOrNull() ?: 0.0) + 8.0).toString()
                            selectedBankHolidayMultiplier = 2.0
                        },
                        label = { Text("+1 Bank Holiday (8h @ 2.0x)", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = {
                            scenarioBonusInput = "500"
                        },
                        label = { Text("+£500 Bonus", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = {
                            scenarioDaysInput = baselineDays.toInt().toString()
                            scenarioHoursPerDayInput = baselineHoursPerDay.toString()
                            scenarioOtHoursInput = if (baselineOvertimeHours > 0) baselineOvertimeHours.toString() else "0"
                            scenarioBankHolidayHoursInput = "0"
                            scenarioBonusInput = ""
                            scenarioCommissionInput = ""
                        },
                        label = { Text("Reset to Baseline", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // 3. Standard Working Days & Hours
                Text("Standard Shift Hours", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = scenarioDaysInput,
                        onValueChange = { scenarioDaysInput = it },
                        label = { Text("Days Worked") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = scenarioHoursPerDayInput,
                        onValueChange = { scenarioHoursPerDayInput = it },
                        label = { Text("Hours / Day") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                // 4. Overtime Hours & Multiplier Pills
                Text("Overtime Shifts & Multiplier", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Amber60)
                OutlinedTextField(
                    value = scenarioOtHoursInput,
                    onValueChange = { scenarioOtHoursInput = it },
                    label = { Text("Overtime Hours") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1.0 to "1.0x (1T)", 1.25 to "1.25x", 1.5 to "1.5x (1.5T)", 1.75 to "1.75x", 2.0 to "2.0x (2T)", 2.5 to "2.5x").forEach { (mult, label) ->
                        FilterChip(
                            selected = selectedOtMultiplier == mult,
                            onClick = { selectedOtMultiplier = mult },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // 5. Bank Holiday Shifts & Multiplier
                Text("Bank Holiday Shifts", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Rose60)
                OutlinedTextField(
                    value = scenarioBankHolidayHoursInput,
                    onValueChange = { scenarioBankHolidayHoursInput = it },
                    label = { Text("Bank Holiday Hours") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1.5 to "1.5x (Time & Half)", 2.0 to "2.0x (Double Time)", 2.5 to "2.5x", 3.0 to "3.0x (Triple Time)").forEach { (mult, label) ->
                        FilterChip(
                            selected = selectedBankHolidayMultiplier == mult,
                            onClick = { selectedBankHolidayMultiplier = mult },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // 6. Bonus & Commission Fields
                Text("Bonus & Commission", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Teal60)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = scenarioBonusInput,
                        onValueChange = { scenarioBonusInput = it },
                        label = { Text("Bonus (£)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = scenarioCommissionInput,
                        onValueChange = { scenarioCommissionInput = it },
                        label = { Text("Commission (£)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                // 7. Deductions Breakdown Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Tax & Deduction Impact", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PAYE Income Tax:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(scenarioReport.incomeTax)} (Baseline: £${"%,.2f".format(baselineReport.incomeTax)})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Class 1 National Insurance:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(scenarioReport.nationalInsurance)} (Baseline: £${"%,.2f".format(baselineReport.nationalInsurance)})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        if (scenarioReport.pensionContribution > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pension (${pensionRate}%):", style = MaterialTheme.typography.bodySmall)
                                Text("£${"%,.2f".format(scenarioReport.pensionContribution)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onApplyToCalculator != null) {
                    Button(
                        onClick = {
                            onApplyToCalculator(days, hoursPerDay, otHours + bhHours, bonus, commission)
                            onDismiss()
                        }
                    ) {
                        Text("Apply Scenario")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
