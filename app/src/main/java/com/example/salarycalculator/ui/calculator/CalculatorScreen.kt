package com.example.salarycalculator.ui.calculator

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.*
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val taxCode by salaryRepository.getTaxCode().collectAsState(initial = "1257L")
    val defaultHourlyRate by salaryRepository.getDefaultHourlyRate().collectAsState(initial = 12.71)
    val taxRegion by salaryRepository.getTaxRegion().collectAsState(initial = TaxRegion.UK_STANDARD)
    val pensionRate by salaryRepository.getPensionRate().collectAsState(initial = 5.0)
    val studentLoanPlan by salaryRepository.getStudentLoanPlan().collectAsState(initial = StudentLoanPlan.NONE)
    val defaultOvertimeMultiplier by salaryRepository.getOvertimeMultiplier().collectAsState(initial = 1.5)

    var selectedFrequency by remember { mutableStateOf(PayFrequency.MONTHLY) }
    var selectedOvertimeMultiplier by remember(defaultOvertimeMultiplier) { mutableStateOf(defaultOvertimeMultiplier) }
    var selectedPensionPercent by remember(pensionRate) { mutableStateOf(pensionRate) }
    var selectedStudentLoan by remember(studentLoanPlan) { mutableStateOf(studentLoanPlan) }

    var daysWorkedInput by remember { mutableStateOf("20") }
    var hoursPerDayInput by remember { mutableStateOf("8.0") }
    var overtimeHoursInput by remember { mutableStateOf("") }

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveMonthYear by remember { mutableStateOf("September 2026") }
    var saveNote by remember { mutableStateOf("") }

    // Memoize input numbers
    val daysWorked = remember(daysWorkedInput) { daysWorkedInput.toDoubleOrNull() ?: 0.0 }
    val hoursPerDay = remember(hoursPerDayInput) { hoursPerDayInput.toDoubleOrNull() ?: 8.0 }
    val overtimeHours = remember(overtimeHoursInput) { overtimeHoursInput.toDoubleOrNull() ?: 0.0 }

    // Memoize standard and overtime pay
    val standardPay = remember(daysWorked, hoursPerDay, defaultHourlyRate) {
        (daysWorked * hoursPerDay) * defaultHourlyRate
    }
    val overtimePay = remember(overtimeHours, defaultHourlyRate, selectedOvertimeMultiplier) {
        overtimeHours * (defaultHourlyRate * selectedOvertimeMultiplier)
    }
    val grossPay = remember(standardPay, overtimePay) { standardPay + overtimePay }

    // Full Salary Report
    val report: SalaryReport = remember(
        grossPay,
        taxCode,
        taxRegion,
        selectedPensionPercent,
        selectedStudentLoan
    ) {
        TaxCalculator.calculateTax(
            grossPay = grossPay,
            taxCode = taxCode,
            isMonthly = true,
            region = taxRegion,
            pensionRatePercent = selectedPensionPercent,
            studentLoanPlan = selectedStudentLoan
        )
    }

    val totalHours = remember(daysWorked, hoursPerDay, overtimeHours) {
        (daysWorked * hoursPerDay) + overtimeHours
    }

    // Active displayed net amount based on selected frequency
    val displayedNetAmount = remember(selectedFrequency, report) {
        when (selectedFrequency) {
            PayFrequency.MONTHLY -> report.monthlyNet
            PayFrequency.WEEKLY -> report.weeklyNet
            PayFrequency.ANNUAL -> report.annualNet
            PayFrequency.HOURLY -> report.hourlyNet
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Header & Region/Tax Code Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Salary Calculator",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text(if (taxRegion == TaxRegion.SCOTLAND) "Scotland" else "UK Standard", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(taxCode, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
                Text(
                    text = "Rate: £${"%.2f".format(defaultHourlyRate)}/hr · Base Pay: £${"%.2f".format(standardPay)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pay Frequency Selector (SingleChoiceSegmentedButtonRow)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PayFrequency.entries.forEachIndexed { index, freq ->
                    SegmentedButton(
                        selected = selectedFrequency == freq,
                        onClick = { selectedFrequency = freq },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PayFrequency.entries.size),
                        icon = {}
                    ) {
                        Text(freq.displayName, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            // Hero Take-Home Pay Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Net (${selectedFrequency.displayName})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = EmeraldContainerLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${"%.1f".format(report.takeHomePercentage)}% Take-Home",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Emerald40
                            )
                        }
                    }

                    // Animated Net Amount Display
                    AnimatedContent(
                        targetState = displayedNetAmount,
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                             slideInVertically { it / 2 }) togetherWith
                            (fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                             slideOutVertically { -it / 2 })
                        },
                        label = "NetAmountAnimation"
                    ) { amount ->
                        Text(
                            text = "£${"%.2f".format(amount)}",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald60
                        )
                    }

                    // Animated Proportional Breakdown Bar
                    if (grossPay > 0) {
                        val netRatio by animateFloatAsState(
                            targetValue = (report.netPay / grossPay).toFloat(),
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "netRatio"
                        )
                        val pensionRatio by animateFloatAsState(
                            targetValue = (report.pensionContribution / grossPay).toFloat(),
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "pensionRatio"
                        )
                        val taxRatio by animateFloatAsState(
                            targetValue = (report.incomeTax / grossPay).toFloat(),
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "taxRatio"
                        )
                        val niRatio by animateFloatAsState(
                            targetValue = (report.nationalInsurance / grossPay).toFloat(),
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "niRatio"
                        )
                        val studentLoanRatio by animateFloatAsState(
                            targetValue = (report.studentLoanDeduction / grossPay).toFloat(),
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "studentLoanRatio"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                if (netRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(netRatio.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(Emerald60)
                                    )
                                }
                                if (pensionRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(pensionRatio.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(Teal60)
                                    )
                                }
                                if (taxRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(taxRatio.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(Rose60)
                                    )
                                }
                                if (niRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(niRatio.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(Amber60)
                                    )
                                }
                                if (studentLoanRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(studentLoanRatio.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(Violet60)
                                    )
                                }
                            }

                            // Legend
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LegendItem(color = Emerald60, label = "Take Home")
                                if (report.pensionContribution > 0) LegendItem(color = Teal60, label = "Pension")
                                LegendItem(color = Rose60, label = "PAYE Tax")
                                LegendItem(color = Amber60, label = "NI")
                                if (report.studentLoanDeduction > 0) LegendItem(color = Violet60, label = "Student Loan")
                            }
                        }
                    }
                }
            }

            // Quick Input Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Schedule Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = daysWorkedInput == "20" && hoursPerDayInput == "8.0",
                        onClick = {
                            daysWorkedInput = "20"
                            hoursPerDayInput = "8.0"
                        },
                        label = { Text("Full Month (20d · 8h)") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = daysWorkedInput == "21.67" && hoursPerDayInput == "7.5",
                        onClick = {
                            daysWorkedInput = "21.67"
                            hoursPerDayInput = "7.5"
                        },
                        label = { Text("UK Avg (21.7d · 7.5h)") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = daysWorkedInput == "16" && hoursPerDayInput == "8.0",
                        onClick = {
                            daysWorkedInput = "16"
                            hoursPerDayInput = "8.0"
                        },
                        label = { Text("4-Day Week (16d)") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Working Hours & Overtime Multiplier Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Working Hours & Overtime Rate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = daysWorkedInput,
                        onValueChange = { daysWorkedInput = it },
                        label = { Text("Days Worked") },
                        placeholder = { Text("e.g. 20") },
                        leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                        trailingIcon = {
                            if (daysWorkedInput.isNotEmpty()) {
                                IconButton(onClick = { daysWorkedInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = hoursPerDayInput,
                        onValueChange = { hoursPerDayInput = it },
                        label = { Text("Hours per Day") },
                        placeholder = { Text("e.g. 8.0") },
                        leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                        trailingIcon = {
                            if (hoursPerDayInput.isNotEmpty()) {
                                IconButton(onClick = { hoursPerDayInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = overtimeHoursInput,
                        onValueChange = { overtimeHoursInput = it },
                        label = { Text("Overtime Hours (Optional)") },
                        placeholder = { Text("e.g. 5") },
                        leadingIcon = { Icon(Icons.Outlined.MoreTime, contentDescription = null) },
                        trailingIcon = {
                            if (overtimeHoursInput.isNotEmpty()) {
                                IconButton(onClick = { overtimeHoursInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Overtime Multiplier Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Overtime Multiplier",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedOvertimeMultiplier == 1.0,
                                onClick = { selectedOvertimeMultiplier = 1.0 },
                                label = { Text("1.0x (Standard)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            FilterChip(
                                selected = selectedOvertimeMultiplier == 1.5,
                                onClick = { selectedOvertimeMultiplier = 1.5 },
                                label = { Text("1.5x (Time & Half)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            FilterChip(
                                selected = selectedOvertimeMultiplier == 2.0,
                                onClick = { selectedOvertimeMultiplier = 2.0 },
                                label = { Text("2.0x (Double)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Deductions & Allowances Selector Card (Pension & Student Loan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Pension & Student Loan Adjustments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Pension Rate Options
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Employee Pension Contribution: ${"%.1f".format(selectedPensionPercent)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.0, 3.0, 5.0, 8.0, 10.0).forEach { rate ->
                                FilterChip(
                                    selected = selectedPensionPercent == rate,
                                    onClick = { selectedPensionPercent = rate },
                                    label = { Text("${rate.toInt()}%") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Student Loan Plan Options
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Student Loan Plan: ${selectedStudentLoan.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudentLoanPlan.entries.forEach { plan ->
                                FilterChip(
                                    selected = selectedStudentLoan == plan,
                                    onClick = { selectedStudentLoan = plan },
                                    label = { Text(if (plan == StudentLoanPlan.NONE) "None" else plan.name.replace("_", " ")) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Detailed Itemized Payslip Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Payslip Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total: ${"%.1f".format(totalHours)} hrs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PayslipRow(
                        label = "Basic Pay (${"%.1f".format(daysWorked * hoursPerDay)} hrs)",
                        value = "£${"%.2f".format(standardPay)}"
                    )

                    AnimatedVisibility(
                        visible = overtimeHours > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        PayslipRow(
                            label = "Overtime (${"%.1f".format(overtimeHours)} hrs @ ${selectedOvertimeMultiplier}x)",
                            value = "£${"%.2f".format(overtimePay)}"
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PayslipRow(
                        label = "Gross Total Pay",
                        value = "£${"%.2f".format(report.grossPay)}",
                        isBold = true
                    )

                    if (report.pensionContribution > 0) {
                        PayslipRow(
                            label = "Employee Pension (${"%.1f".format(selectedPensionPercent)}%)",
                            value = "-£${"%.2f".format(report.pensionContribution)}",
                            valueColor = Teal60
                        )
                        PayslipRow(
                            label = "Employer Pension (3% Company)",
                            value = "+£${"%.2f".format(report.employerPensionContribution)}",
                            isSecondary = true
                        )
                    }

                    PayslipRow(
                        label = "Taxable Pay (after relief)",
                        value = "£${"%.2f".format(report.taxablePay)}",
                        isSecondary = true
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PayslipRow(
                        label = if (taxRegion == TaxRegion.SCOTLAND) "Scottish Income Tax (2024/25)" else "PAYE Income Tax (2024/25)",
                        value = "-£${"%.2f".format(report.incomeTax)}",
                        valueColor = Rose60
                    )

                    PayslipRow(
                        label = "Class 1 National Insurance",
                        value = "-£${"%.2f".format(report.nationalInsurance)}",
                        valueColor = Amber60
                    )

                    if (report.studentLoanDeduction > 0) {
                        PayslipRow(
                            label = "Student Loan (${selectedStudentLoan.name.replace("_", " ")})",
                            value = "-£${"%.2f".format(report.studentLoanDeduction)}",
                            valueColor = Violet60
                        )
                    }

                    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

                    // Net Pay Final Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Net Take-Home",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "After tax, NI, pension & loan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "£${"%.2f".format(report.netPay)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald60
                        )
                    }
                }
            }

            // Multi-Period Comparison Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Multi-Period Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PeriodColumn(title = "Hourly", net = report.hourlyNet, gross = ((report.grossPay * 12.0) / 52.0) / 37.5)
                        PeriodColumn(title = "Weekly", net = report.weeklyNet, gross = (report.grossPay * 12.0) / 52.0)
                        PeriodColumn(title = "Monthly", net = report.monthlyNet, gross = report.grossPay)
                        PeriodColumn(title = "Annual", net = report.annualNet, gross = report.grossPay * 12.0)
                    }
                }
            }

            // Action Buttons: Save to History & Share Payslip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Record", style = MaterialTheme.typography.titleMedium, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = {
                        val shareText = """
                            💰 UK Salary Calculator Summary
                            ---------------------------------------
                            Gross Pay: £${"%.2f".format(report.grossPay)} / month
                            Tax Code: $taxCode (${if (taxRegion == TaxRegion.SCOTLAND) "Scotland" else "UK Standard"})
                            
                            Deductions:
                            • Pension (${selectedPensionPercent}%): £${"%.2f".format(report.pensionContribution)}
                            • PAYE Income Tax: £${"%.2f".format(report.incomeTax)}
                            • National Insurance: £${"%.2f".format(report.nationalInsurance)}
                            ${if (report.studentLoanDeduction > 0) "• Student Loan: £${"%.2f".format(report.studentLoanDeduction)}\n" else ""}
                            💵 Net Take-Home:
                            • Monthly: £${"%.2f".format(report.monthlyNet)} (${"%.1f".format(report.takeHomePercentage)}%)
                            • Weekly:  £${"%.2f".format(report.weeklyNet)}
                            • Annual:  £${"%.2f".format(report.annualNet)}
                            ---------------------------------------
                        """.trimIndent()

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Payslip Summary")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", style = MaterialTheme.typography.titleMedium, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        // Save to History Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = {
                    Text(
                        text = "Save Monthly Payslip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Net Take-Home: £${"%.2f".format(report.netPay)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Emerald60
                        )

                        OutlinedTextField(
                            value = saveMonthYear,
                            onValueChange = { saveMonthYear = it },
                            label = { Text("Month & Year") },
                            placeholder = { Text("e.g. September 2026") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick month selection suggestions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("September 2026", "August 2026", "July 2026", "June 2026").forEach { month ->
                                SuggestionChip(
                                    onClick = { saveMonthYear = month },
                                    label = { Text(month, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = saveNote,
                            onValueChange = { saveNote = it },
                            label = { Text("Optional Note") },
                            placeholder = { Text("e.g. Includes bonus & overtime") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = MonthlySalaryRecord(
                                monthYear = saveMonthYear.trim().ifBlank { "Monthly Payslip" },
                                daysWorked = daysWorked,
                                hoursPerDay = hoursPerDay,
                                overtimeHours = overtimeHours,
                                overtimeMultiplier = selectedOvertimeMultiplier,
                                hourlyRate = defaultHourlyRate,
                                grossPay = report.grossPay,
                                pensionRate = selectedPensionPercent,
                                pensionContribution = report.pensionContribution,
                                employerPension = report.employerPensionContribution,
                                taxablePay = report.taxablePay,
                                incomeTax = report.incomeTax,
                                nationalInsurance = report.nationalInsurance,
                                studentLoanPlan = selectedStudentLoan,
                                studentLoanDeduction = report.studentLoanDeduction,
                                totalDeductions = report.totalDeductions,
                                netPay = report.netPay,
                                taxCode = taxCode,
                                taxRegion = taxRegion,
                                note = saveNote.trim()
                            )
                            scope.launch {
                                salaryRepository.saveSalaryRecord(record)
                                showSaveDialog = false
                                snackbarHostState.showSnackbar("Saved ${record.monthYear} to Salary History!")
                            }
                        }
                    ) {
                        Text("Save Record", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun PeriodColumn(title: String, net: Double, gross: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "£${"%.0f".format(net)}", style = MaterialTheme.typography.titleMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emerald60)
        Text(text = "£${"%.0f".format(gross)} grs", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
    }
}

@Composable
private fun PayslipRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isSecondary: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isSecondary) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
