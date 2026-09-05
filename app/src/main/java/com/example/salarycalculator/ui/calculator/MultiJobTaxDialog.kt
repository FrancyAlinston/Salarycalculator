package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
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
fun MultiJobTaxDialog(
    onDismiss: () -> Unit
) {
    var job1Title by remember { mutableStateOf("Primary Employment") }
    var job1SalaryInput by remember { mutableStateOf("30000") }
    var job1TaxCode by remember { mutableStateOf("1257L") }
    var job1PensionInput by remember { mutableStateOf("5.0") }

    var job2Title by remember { mutableStateOf("Secondary Employment") }
    var job2SalaryInput by remember { mutableStateOf("15000") }
    var job2TaxCode by remember { mutableStateOf("BR") }
    var job2PensionInput by remember { mutableStateOf("5.0") }

    var selectedRegion by remember { mutableStateOf(TaxRegion.UK_STANDARD) }
    var isMonthlyView by remember { mutableStateOf(false) }

    val job1Salary = remember(job1SalaryInput) { job1SalaryInput.toDoubleOrNull() ?: 0.0 }
    val job1Pension = remember(job1PensionInput) { job1PensionInput.toDoubleOrNull() ?: 0.0 }

    val job2Salary = remember(job2SalaryInput) { job2SalaryInput.toDoubleOrNull() ?: 0.0 }
    val job2Pension = remember(job2PensionInput) { job2PensionInput.toDoubleOrNull() ?: 0.0 }

    val summary = remember(job1Salary, job1TaxCode, job1Pension, job2Salary, job2TaxCode, job2Pension, selectedRegion) {
        val jobs = listOf(
            JobInput(
                title = job1Title,
                grossSalary = job1Salary,
                isAnnual = true,
                taxCode = job1TaxCode,
                pensionPercentage = job1Pension
            ),
            JobInput(
                title = job2Title,
                grossSalary = job2Salary,
                isAnnual = true,
                taxCode = job2TaxCode,
                pensionPercentage = job2Pension
            )
        )
        MultiJobAggregatorEngine.calculateMultiJob(jobs, region = selectedRegion)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Dual-Job & Multi-Employer Tax",
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
                    text = "Accurately models two concurrent jobs. Statutory Class 1 NI threshold (£12,576) applies independently to each employer, while PAYE Income Tax is assessed on combined cumulative earnings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Period & Region Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Region Chips
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = selectedRegion == TaxRegion.UK_STANDARD,
                            onClick = { selectedRegion = TaxRegion.UK_STANDARD },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("UK Standard", fontSize = 11.sp)
                        }
                        SegmentedButton(
                            selected = selectedRegion == TaxRegion.SCOTLAND,
                            onClick = { selectedRegion = TaxRegion.SCOTLAND },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Scotland", fontSize = 11.sp)
                        }
                    }

                    // Annual / Monthly toggle
                    FilterChip(
                        selected = isMonthlyView,
                        onClick = { isMonthlyView = !isMonthlyView },
                        label = { Text(if (isMonthlyView) "Monthly" else "Annual", fontSize = 11.sp) }
                    )
                }

                // 60% Marginal Trap Warning Banner if combined > £100k
                if (summary.isMarginalTrapReached) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Amber60.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Amber60)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Amber60)
                            Column {
                                Text(
                                    text = "60% Marginal Tax Trap Active!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Amber60
                                )
                                Text(
                                    text = "Combined earnings (£${String.format("%.0f", summary.totalAnnualGross)}) taper personal allowance by £${String.format("%.0f", summary.personalAllowanceTaperLoss)}. Consider increasing salary sacrifice pension.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Combined Summary Card
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
                            text = "Combined Take-Home Pay",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "£" + String.format("%,.2f", if (isMonthlyView) summary.totalMonthlyNet else summary.totalAnnualNet) + if (isMonthlyView) "/mo" else "/yr",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald60.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${String.format("%.1f", summary.overallTakeHomePercentage)}% Retained",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Emerald60,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Gross Earnings:", fontSize = 12.sp)
                            Text("£" + String.format("%,.2f", if (isMonthlyView) summary.totalMonthlyGross else summary.totalAnnualGross), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Combined Income Tax:", fontSize = 12.sp)
                            Text("£" + String.format("%,.2f", if (isMonthlyView) summary.totalMonthlyTax else summary.totalAnnualTax), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Rose60)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Combined Class 1 NI:", fontSize = 12.sp)
                            Text("£" + String.format("%,.2f", if (isMonthlyView) summary.totalMonthlyNi else summary.totalAnnualNi), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Effective Combined Tax Rate:", fontSize = 12.sp)
                            Text("${String.format("%.1f", summary.overallEffectiveRate)}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Job 1 (Primary) Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Job 1 (Primary)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedTextField(
                            value = job1SalaryInput,
                            onValueChange = { job1SalaryInput = it },
                            label = { Text("Annual Gross Salary (£)") },
                            prefix = { Text("£ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = job1TaxCode,
                                onValueChange = { job1TaxCode = it },
                                label = { Text("Tax Code") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = job1PensionInput,
                                onValueChange = { job1PensionInput = it },
                                label = { Text("Pension %") },
                                suffix = { Text("%") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        if (summary.jobs.isNotEmpty()) {
                            val j1 = summary.jobs[0]
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Job 1 Net:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("£" + String.format("%,.2f", if (isMonthlyView) j1.monthlyNet else j1.annualNet) + " (NI: £${String.format("%.0f", if (isMonthlyView) j1.monthlyNi else j1.annualNi)})", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Job 2 (Secondary) Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text("Job 2 (Secondary)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedTextField(
                            value = job2SalaryInput,
                            onValueChange = { job2SalaryInput = it },
                            label = { Text("Annual Gross Salary (£)") },
                            prefix = { Text("£ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick Tax Code Preset Chips for Job 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("BR", "0T", "D0", "1257L").forEach { code ->
                                FilterChip(
                                    selected = job2TaxCode.uppercase() == code,
                                    onClick = { job2TaxCode = code },
                                    label = { Text(code, fontSize = 10.sp) }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = job2TaxCode,
                                onValueChange = { job2TaxCode = it },
                                label = { Text("Tax Code") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = job2PensionInput,
                                onValueChange = { job2PensionInput = it },
                                label = { Text("Pension %") },
                                suffix = { Text("%") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        if (summary.jobs.size > 1) {
                            val j2 = summary.jobs[1]
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Job 2 Net:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("£" + String.format("%,.2f", if (isMonthlyView) j2.monthlyNet else j2.annualNet) + " (NI: £${String.format("%.0f", if (isMonthlyView) j2.monthlyNi else j2.annualNi)})", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                }
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
