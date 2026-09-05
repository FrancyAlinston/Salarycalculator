package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmbrellaPayrollDialog(
    onDismiss: () -> Unit
) {
    var rateType by remember { mutableStateOf(UmbrellaRateType.DAILY) }
    var rateAmountInput by remember { mutableStateOf("450") }
    var unitsPerWeekInput by remember { mutableStateOf("5") }
    var weeksPerYearInput by remember { mutableStateOf("48") }
    var marginInput by remember { mutableStateOf("25") }
    var selectedTaxYear by remember { mutableStateOf(TaxYear.YEAR_2025_2026) }
    var isMonthlyView by remember { mutableStateOf(true) }

    val rateAmount = remember(rateAmountInput) { rateAmountInput.toDoubleOrNull() ?: 0.0 }
    val unitsPerWeek = remember(unitsPerWeekInput) { unitsPerWeekInput.toDoubleOrNull() ?: 5.0 }
    val weeksPerYear = remember(weeksPerYearInput) { weeksPerYearInput.toDoubleOrNull() ?: 48.0 }
    val margin = remember(marginInput) { marginInput.toDoubleOrNull() ?: 25.0 }

    val result = remember(rateType, rateAmount, unitsPerWeek, weeksPerYear, margin, selectedTaxYear) {
        val input = UmbrellaPayrollInput(
            rateType = rateType,
            rateAmount = rateAmount,
            unitsWorkedPerWeek = unitsPerWeek,
            weeksWorkedPerYear = weeksPerYear,
            umbrellaMarginWeekly = margin,
            taxYear = selectedTaxYear
        )
        UmbrellaPayrollEngine.calculate(input)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Umbrella & Employer On-Cost",
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
                    text = "Calculates Inside-IR35 umbrella contractor pay and employer on-costs. Bridges invoice rates to take-home pay after Employer NI, Apprenticeship Levy, and PAYE.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Tax Year & Rate Type Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = rateType == UmbrellaRateType.DAILY,
                            onClick = { 
                                rateType = UmbrellaRateType.DAILY 
                                if (unitsPerWeekInput == "37.5") unitsPerWeekInput = "5"
                                if (rateAmountInput == "50") rateAmountInput = "450"
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Daily", fontSize = 11.sp)
                        }
                        SegmentedButton(
                            selected = rateType == UmbrellaRateType.HOURLY,
                            onClick = { 
                                rateType = UmbrellaRateType.HOURLY
                                if (unitsPerWeekInput == "5") unitsPerWeekInput = "37.5"
                                if (rateAmountInput == "450") rateAmountInput = "50"
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Hourly", fontSize = 11.sp)
                        }
                    }

                    FilterChip(
                        selected = selectedTaxYear == TaxYear.YEAR_2025_2026,
                        onClick = {
                            selectedTaxYear = if (selectedTaxYear == TaxYear.YEAR_2025_2026) TaxYear.YEAR_2024_2025 else TaxYear.YEAR_2025_2026
                        },
                        label = { Text(if (selectedTaxYear == TaxYear.YEAR_2025_2026) "2025/26 (15% NI)" else "2024/25 (13.8% NI)", fontSize = 10.sp) }
                    )
                }

                // Rate & Work Volume Inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateAmountInput,
                        onValueChange = { rateAmountInput = it },
                        label = { Text(if (rateType == UmbrellaRateType.DAILY) "Day Rate (£)" else "Hourly Rate (£)") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitsPerWeekInput,
                        onValueChange = { unitsPerWeekInput = it },
                        label = { Text(if (rateType == UmbrellaRateType.DAILY) "Days/Wk" else "Hours/Wk") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weeksPerYearInput,
                        onValueChange = { weeksPerYearInput = it },
                        label = { Text("Weeks Worked/Yr") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = marginInput,
                        onValueChange = { marginInput = it },
                        label = { Text("Umbrella Fee/Wk") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Net Take-Home Hero Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Estimated Net Take-Home Pay",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "£" + String.format("%,.2f", if (isMonthlyView) result.monthlyNetPay else result.annualNetPay) + if (isMonthlyView) "/mo" else "/yr",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald60.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${String.format("%.1f", result.netRetentionPercentage)}% Retained",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Emerald60,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Weekly: £${String.format("%,.0f", result.weeklyNetPay)}/wk", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Daily Net: £${String.format("%,.0f", result.dailyNetPay)}/day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Step-Down Waterfall Cards
                // 1. Invoiced Gross
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Invoiced Revenue:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("£" + String.format("%,.2f", if (isMonthlyView) result.monthlyInvoicedRevenue else result.annualInvoicedRevenue), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // 2. Company Deductions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Company / Employer On-Costs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Rose60)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Employer Class 1 NI (${String.format("%.1f", result.employerNiRateUsed * 100)}%):", fontSize = 11.sp)
                            Text("-£" + String.format("%,.2f", if (isMonthlyView) result.monthlyEmployerNi else result.annualEmployerNi), fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Apprenticeship Levy (0.5%):", fontSize = 11.sp)
                            Text("-£" + String.format("%,.2f", if (isMonthlyView) result.monthlyApprenticeshipLevy else result.annualApprenticeshipLevy), fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Employer Pension (3%):", fontSize = 11.sp)
                            Text("-£" + String.format("%,.2f", if (isMonthlyView) result.monthlyEmployerPension else result.annualEmployerPension), fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Umbrella Margin / Fee:", fontSize = 11.sp)
                            Text("-£" + String.format("%,.2f", if (isMonthlyView) result.monthlyUmbrellaMargin else result.annualUmbrellaMargin), fontSize = 11.sp)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Resulting Employee Gross Pay:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("£" + String.format("%,.2f", if (isMonthlyView) result.monthlyGrossPay else result.annualGrossPay), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // 3. Employee PAYE Deductions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Employee PAYE Deductions", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Rose60)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• PAYE Income Tax:", fontSize = 11.sp)
                            Text("-£" + String.format("%,.2f", if (isMonthlyView) result.monthlyEmployeeTax else result.annualEmployeeTax), fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Employee Class 1 NI (8% / 2%):", fontSize = 11.sp)
                            Text("-£" + String.format("%,.2f", if (isMonthlyView) result.monthlyEmployeeNi else result.annualEmployeeNi), fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Employee Pension (5%):", fontSize = 11.sp)
                            Text("-£" + String.format("%,.2f", if (isMonthlyView) result.monthlyEmployeePension else result.annualEmployeePension), fontSize = 11.sp)
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
