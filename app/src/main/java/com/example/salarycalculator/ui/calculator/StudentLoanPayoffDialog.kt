package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.salarycalculator.domain.StudentLoanPayoffEngine
import com.example.salarycalculator.domain.StudentLoanPlan
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Violet60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentLoanPayoffDialog(
    initialPlan: StudentLoanPlan = StudentLoanPlan.PLAN_2,
    initialSalary: Double = 35000.0,
    onDismiss: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(if (initialPlan == StudentLoanPlan.NONE) StudentLoanPlan.PLAN_2 else initialPlan) }
    var balanceInput by remember { mutableStateOf("25000") }
    var salaryInput by remember { mutableStateOf("%.0f".format(initialSalary)) }
    var growthInput by remember { mutableStateOf("2.5") }
    var extraMonthlyInput by remember { mutableStateOf("0") }

    var expandedPlanDropdown by remember { mutableStateOf(false) }

    val balance = balanceInput.toDoubleOrNull() ?: 0.0
    val salary = salaryInput.toDoubleOrNull() ?: 0.0
    val growth = growthInput.toDoubleOrNull() ?: 0.0
    val extraMonthly = extraMonthlyInput.toDoubleOrNull() ?: 0.0

    val report = remember(selectedPlan, balance, salary, growth, extraMonthly) {
        StudentLoanPayoffEngine.calculatePayoffTimeline(
            plan = selectedPlan,
            currentBalance = balance,
            annualSalary = salary,
            annualSalaryGrowthPercent = growth,
            extraMonthlyOverpayment = extraMonthly
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
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Student Loan Repayment Horizon",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                // Strategic Summary Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (report.willBeWrittenOff) Amber60.copy(alpha = 0.12f)
                        else Emerald60.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (report.willBeWrittenOff) "⏳ 30-Year Statutory Write-Off"
                                else "🎉 Debt-Free in ${"%.1f".format(report.estimatedYearsToPayoff)} Years",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (report.willBeWrittenOff) Amber60 else Emerald60
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Violet60
                            ) {
                                Text(
                                    text = "${report.statutoryInterestRatePercent}% APR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Monthly Deduction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.2f".format(report.totalMonthlyRepayment)}/mo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Lifetime Repaid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.0f".format(report.totalRepaidLifetime)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Violet60)
                            }
                        }

                        if (report.totalSavingsFromOverpayment > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Savings from Overpayment:", style = MaterialTheme.typography.bodySmall, color = Emerald60)
                                Text("£${"%,.0f".format(report.totalSavingsFromOverpayment)}", style = MaterialTheme.typography.bodySmall, color = Emerald60, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Strategy Recommendation Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = report.strategicRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 11.sp
                    )
                }

                // Input Parameters
                Text("Loan & Salary Parameters:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                // Plan Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedPlanDropdown,
                    onExpandedChange = { expandedPlanDropdown = !expandedPlanDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedPlan.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repayment Plan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPlanDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPlanDropdown,
                        onDismissRequest = { expandedPlanDropdown = false }
                    ) {
                        listOf(
                            StudentLoanPlan.PLAN_1,
                            StudentLoanPlan.PLAN_2,
                            StudentLoanPlan.PLAN_4,
                            StudentLoanPlan.POSTGRADUATE
                        ).forEach { plan ->
                            DropdownMenuItem(
                                text = { Text(plan.displayName) },
                                onClick = {
                                    selectedPlan = plan
                                    expandedPlanDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = balanceInput,
                        onValueChange = { balanceInput = it },
                        label = { Text("Current Balance") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = salaryInput,
                        onValueChange = { salaryInput = it },
                        label = { Text("Annual Salary") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = growthInput,
                        onValueChange = { growthInput = it },
                        label = { Text("Salary Growth %/yr") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = extraMonthlyInput,
                        onValueChange = { extraMonthlyInput = it },
                        label = { Text("Extra Overpay/mo") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
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
