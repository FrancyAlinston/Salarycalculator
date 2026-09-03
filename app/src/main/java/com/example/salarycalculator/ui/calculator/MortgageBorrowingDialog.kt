package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.AffordabilityHealth
import com.example.salarycalculator.domain.MortgageBorrowingCalculator
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60

@Composable
fun MortgageBorrowingDialog(
    annualGross: Double,
    monthlyNet: Double,
    onDismiss: () -> Unit
) {
    var selectedMultiplier by remember { mutableDoubleStateOf(4.5) }
    var depositInput by remember { mutableStateOf("30000") }
    var monthlyDebtInput by remember { mutableStateOf("150") }
    var interestRate by remember { mutableFloatStateOf(4.5f) }
    var termYears by remember { mutableIntStateOf(30) }

    val deposit = depositInput.toDoubleOrNull() ?: 30000.0
    val monthlyDebt = monthlyDebtInput.toDoubleOrNull() ?: 0.0

    val result = remember(annualGross, monthlyNet, deposit, monthlyDebt, selectedMultiplier, interestRate, termYears) {
        MortgageBorrowingCalculator.calculate(
            annualGrossIncome = annualGross,
            monthlyNetTakeHome = monthlyNet,
            depositAmount = deposit,
            monthlyDebtCommitments = monthlyDebt,
            selectedMultiplier = selectedMultiplier,
            annualInterestRatePercent = interestRate.toDouble(),
            termYears = termYears
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Mortgage Capacity Estimator",
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
                    text = "Estimate UK mortgage borrowing power based on your salary income multiples, deposit, and stress-tested net affordability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current Income Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Annual Gross: £${"%,.0f".format(annualGross)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text("Net: £${"%,.2f".format(monthlyNet)}/mo", style = MaterialTheme.typography.labelMedium, color = Emerald60, fontWeight = FontWeight.Bold)
                    }
                }

                // Income Multiplier Selector
                Text("Lender Income Multiplier", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4.0 to "4.0x (Conservative)", 4.5 to "4.5x (Standard)", 5.0 to "5.0x (High Earner)").forEach { (m, label) ->
                        FilterChip(
                            selected = selectedMultiplier == m,
                            onClick = { selectedMultiplier = m },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Deposit Amount Input
                OutlinedTextField(
                    value = depositInput,
                    onValueChange = { depositInput = it },
                    label = { Text("Available Cash Deposit") },
                    prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Existing Monthly Debt Commitments Input
                OutlinedTextField(
                    value = monthlyDebtInput,
                    onValueChange = { monthlyDebtInput = it },
                    label = { Text("Existing Monthly Debts (Loans, Car Finance, Cards)") },
                    prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Interest Rate Slider
                Text(
                    text = "Mortgage Interest Rate: ${"%.2f".format(interestRate)}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = interestRate,
                    onValueChange = { interestRate = it },
                    valueRange = 2.0f..10.0f
                )

                // Loan Term Slider
                Text(
                    text = "Mortgage Term: $termYears years",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = termYears.toFloat(),
                    onValueChange = { termYears = it.toInt() },
                    valueRange = 15f..40f,
                    steps = 24
                )

                // Borrowing Capacity & Affordability Result Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Borrowing Power & Property Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Max Loan Capacity:", style = MaterialTheme.typography.bodyMedium)
                            Text("£${"%,.0f".format(result.maxBorrowingAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Emerald60)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Property Purchasing Price:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.0f".format(result.estimatedPropertyPrice)} (${"%.1f".format(result.loanToValuePercentage)}% LTV)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text("Monthly Stress-Tested Repayment", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated Monthly Repayment:", style = MaterialTheme.typography.bodyMedium)
                            Text("£${"%,.2f".format(result.estimatedMonthlyRepayment)} /mo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Remaining After Mortgage:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(result.monthlyDisposableAfterMortgage)} /mo", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (result.monthlyDisposableAfterMortgage > 0) Emerald60 else Rose60)
                        }

                        // Affordability Health Badge
                        val badgeColor = when (result.affordabilityStatus) {
                            AffordabilityHealth.EXCELLENT -> Emerald60
                            AffordabilityHealth.MODERATE -> Teal60
                            AffordabilityHealth.STRETCHED -> Amber60
                            AffordabilityHealth.HIGH_RISK -> Rose60
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${result.affordabilityStatus.displayName} (${"%.1f".format(result.repaymentToNetRatioPercent)}% of take-home)",
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
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
