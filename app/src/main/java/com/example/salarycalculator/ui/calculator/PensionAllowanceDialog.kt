package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.PensionAllowanceOptimizer
import com.example.salarycalculator.domain.TaxRegion
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60

@Composable
fun PensionAllowanceDialog(
    initialAnnualGross: Double = 60000.0,
    initialPensionRate: Double = 5.0,
    taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    onDismiss: () -> Unit
) {
    var grossInput by remember { mutableStateOf("%.0f".format(initialAnnualGross)) }
    var otherIncomeInput by remember { mutableStateOf("0") }
    var employeeRateInput by remember { mutableStateOf("%.1f".format(initialPensionRate)) }
    var employerRateInput by remember { mutableStateOf("3.0") }
    var isMpaaEnabled by remember { mutableStateOf(false) }

    var yMinus1Input by remember { mutableStateOf("20000") }
    var yMinus2Input by remember { mutableStateOf("15000") }
    var yMinus3Input by remember { mutableStateOf("10000") }

    val gross = grossInput.toDoubleOrNull() ?: 0.0
    val otherIncome = otherIncomeInput.toDoubleOrNull() ?: 0.0
    val employeeRate = employeeRateInput.toDoubleOrNull() ?: 0.0
    val employerRate = employerRateInput.toDoubleOrNull() ?: 0.0
    val y1 = yMinus1Input.toDoubleOrNull() ?: 0.0
    val y2 = yMinus2Input.toDoubleOrNull() ?: 0.0
    val y3 = yMinus3Input.toDoubleOrNull() ?: 0.0

    val report = remember(gross, otherIncome, employeeRate, employerRate, isMpaaEnabled, y1, y2, y3, taxRegion) {
        PensionAllowanceOptimizer.calculatePensionAllowance(
            grossEarnings = gross,
            employeePensionPercent = employeeRate,
            employerPensionPercent = employerRate,
            otherTaxableIncome = otherIncome,
            hasTriggeredMpaa = isMpaaEnabled,
            unusedYearMinus1 = y1,
            unusedYearMinus2 = y2,
            unusedYearMinus3 = y3,
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
                    Icons.Default.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pension Annual Allowance & Tapering",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                // Summary Metric Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (report.excessContribution > 0) Rose60.copy(alpha = 0.12f)
                        else Emerald60.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (report.excessContribution > 0) "⚠️ Allowance Limit Exceeded"
                            else "✅ Within Annual Statutory Allowance",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (report.excessContribution > 0) Rose60 else Emerald60
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Available Allowance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.0f".format(report.totalAvailableAllowance)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Annual Pension Contrib", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.0f".format(report.totalPensionContributions)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Teal60)
                            }
                        }

                        // Utilization Progress Bar
                        val progress = if (report.totalAvailableAllowance > 0) {
                            (report.totalPensionContributions / report.totalAvailableAllowance).toFloat().coerceIn(0f, 1f)
                        } else 1f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (report.excessContribution > 0) Rose60 else Teal60,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        if (report.excessContribution > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimated Tax Charge (${(report.marginalTaxRate * 100).toInt()}%):", style = MaterialTheme.typography.bodySmall, color = Rose60, fontWeight = FontWeight.SemiBold)
                                Text("£${"%,.2f".format(report.estimatedTaxCharge)}", style = MaterialTheme.typography.bodySmall, color = Rose60, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining Headroom:", style = MaterialTheme.typography.bodySmall, color = Emerald60)
                                Text("£${"%,.0f".format(report.remainingAllowance)}", style = MaterialTheme.typography.bodySmall, color = Emerald60, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Input Controls
                Text("Income & Pension Parameters:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grossInput,
                        onValueChange = { grossInput = it },
                        label = { Text("Annual Salary") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = otherIncomeInput,
                        onValueChange = { otherIncomeInput = it },
                        label = { Text("Other Income") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = employeeRateInput,
                        onValueChange = { employeeRateInput = it },
                        label = { Text("Employee %") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = employerRateInput,
                        onValueChange = { employerRateInput = it },
                        label = { Text("Employer %") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // MPAA Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Money Purchase (MPAA) £10k", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Accessed defined contribution pension flexibly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isMpaaEnabled,
                            onCheckedChange = { isMpaaEnabled = it }
                        )
                    }
                }

                if (!isMpaaEnabled) {
                    Text("3-Year Unused Carry Forward (£):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = yMinus1Input,
                            onValueChange = { yMinus1Input = it },
                            label = { Text("2023/24") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = yMinus2Input,
                            onValueChange = { yMinus2Input = it },
                            label = { Text("2022/23") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = yMinus3Input,
                            onValueChange = { yMinus3Input = it },
                            label = { Text("2021/22") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Advisory Notes
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    report.advisoryNotes.forEach { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
