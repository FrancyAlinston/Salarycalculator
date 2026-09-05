package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
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
import com.example.salarycalculator.domain.TaxFreeChildcareEngine
import com.example.salarycalculator.domain.TaxFreeChildcareInput
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun TaxFreeChildcareDialog(
    initialSalary: Double,
    onDismiss: () -> Unit
) {
    var salaryInput by remember { mutableStateOf(String.format("%.0f", if (initialSalary > 0) initialSalary else 45000.0)) }
    var childrenCount by remember { mutableIntStateOf(1) }
    var spendInput by remember { mutableStateOf("7000") }
    var has30Hours by remember { mutableStateOf(true) }

    val salary = remember(salaryInput) { salaryInput.toDoubleOrNull() ?: 0.0 }
    val spend = remember(spendInput) { spendInput.toDoubleOrNull() ?: 0.0 }

    val result = remember(salary, childrenCount, spend, has30Hours) {
        TaxFreeChildcareEngine.calculate(
            TaxFreeChildcareInput(
                annualGrossIncome = salary,
                eligibleChildrenCount = childrenCount,
                annualChildcareSpendPerChild = spend,
                has30HoursFreeChildcare = has30Hours
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
                Icon(Icons.Default.ChildCare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Tax-Free Childcare & 30-Hours",
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
                    text = "HMRC 20% government top-up (up to £2,000/yr per child) and 30-hours free childcare subsidy. WARNING: Benefits are 100% lost if Adjusted Net Income exceeds £100,000.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Salary & Spend Inputs
                OutlinedTextField(
                    value = salaryInput,
                    onValueChange = { salaryInput = it },
                    label = { Text("Annual Gross Salary (£)") },
                    prefix = { Text("£ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = spendInput,
                    onValueChange = { spendInput = it },
                    label = { Text("Annual Childcare Cost per Child (£)") },
                    prefix = { Text("£ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Children Counter & 30 Hours Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Eligible Children (<11yo):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { if (childrenCount > 1) childrenCount-- },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("-") }
                        Text("$childrenCount", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        FilledTonalButton(
                            onClick = { if (childrenCount < 6) childrenCount++ },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("+") }
                    }
                }

                FilterChip(
                    selected = has30Hours,
                    onClick = { has30Hours = !has30Hours },
                    label = { Text("Include 30 Hours Free Childcare (Aged 3-4)", fontSize = 11.sp) }
                )

                // £100k Cliff Edge Alert
                if (result.isCliffEdgeHit) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Rose60.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Rose60)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Rose60)
                                Text("£100k Childcare Cliff-Edge Reached!", fontWeight = FontWeight.Bold, color = Rose60, fontSize = 13.sp)
                            }
                            Text(
                                text = "Your Adjusted Net Income (£${String.format("%,.0f", result.adjustedNetIncome)}) is £${String.format("%,.0f", result.excessOver100k)} over the £100,000 threshold. You have lost ALL Tax-Free Childcare & 30-Hours benefits.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(color = Rose60.copy(alpha = 0.3f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Savings, contentDescription = null, tint = Emerald60)
                                Column {
                                    Text("Pension Sacrifice Remedy:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Emerald60)
                                    Text(
                                        text = "Sacrifice £${String.format("%,.0f", result.requiredPensionSacrificeForEligibility)} into your pension (Net cost: £${String.format("%,.0f", result.netTakeHomeCostOfSacrifice)}) to restore £${String.format("%,.0f", result.totalAnnualGainBySacrificing + result.netTakeHomeCostOfSacrifice)} in government subsidies!",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary Benefit Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Annual Childcare Government Subsidy",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "£" + String.format("%,.2f", result.totalChildcareBenefitValue) + "/year",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (result.isEligible) Emerald60 else Rose60
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("20% Tax-Free Top-Up:", fontSize = 12.sp)
                            Text("£" + String.format("%,.2f", result.governmentTopUpAnnual), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        if (has30Hours) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("30-Hours Free Childcare Value:", fontSize = 12.sp)
                                Text("£" + String.format("%,.2f", result.freeChildcareHoursAnnualValue), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Your Net Out-of-Pocket Cost:", fontSize = 12.sp)
                            Text("£" + String.format("%,.2f", result.outOfPocketSpendAnnual), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
