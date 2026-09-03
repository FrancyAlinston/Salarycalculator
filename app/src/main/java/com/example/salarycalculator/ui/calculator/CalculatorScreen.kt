package com.example.salarycalculator.ui.calculator

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.*
import kotlinx.coroutines.launch

import com.example.salarycalculator.ui.settings.ProfileManagerDialog
import com.example.salarycalculator.ui.settings.CurrencySettingsDialog

@Composable
fun CalculatorScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val taxCode by salaryRepository.getTaxCode().collectAsState(initial = "1257L")
    val defaultHourlyRate by salaryRepository.getDefaultHourlyRate().collectAsState(initial = 12.71)
    val taxRegion by salaryRepository.getTaxRegion().collectAsState(initial = TaxRegion.UK_STANDARD)
    val taxYear by salaryRepository.getTaxYear().collectAsState(initial = TaxYear.YEAR_2024_2025)
    val pensionRate by salaryRepository.getPensionRate().collectAsState(initial = 5.0)
    val studentLoanPlan by salaryRepository.getStudentLoanPlan().collectAsState(initial = StudentLoanPlan.NONE)
    val defaultOvertimeMultiplier by salaryRepository.getOvertimeMultiplier().collectAsState(initial = 1.5)
    val hasMarriageAllowance by salaryRepository.getHasMarriageAllowance().collectAsState(initial = false)
    val hasBlindPersonsAllowance by salaryRepository.getHasBlindPersonsAllowance().collectAsState(initial = false)
    val profiles by salaryRepository.getEmployerProfiles().collectAsState(initial = emptyList())
    val activeProfileId by salaryRepository.getActiveProfileId().collectAsState(initial = null)
    val customEurRate by salaryRepository.getCustomEurRate().collectAsState(initial = ConvertedCurrencies.DEFAULT_EUR_RATE)
    val customUsdRate by salaryRepository.getCustomUsdRate().collectAsState(initial = ConvertedCurrencies.DEFAULT_USD_RATE)

    var selectedFrequency by remember { mutableStateOf(PayFrequency.MONTHLY) }
    var selectedOvertimeMultiplier by remember(defaultOvertimeMultiplier) { mutableStateOf(defaultOvertimeMultiplier) }
    var selectedPensionPercent by remember(pensionRate) { mutableStateOf(pensionRate) }
    var selectedStudentLoan by remember(studentLoanPlan) { mutableStateOf(studentLoanPlan) }
    var salarySacrificeInput by remember { mutableStateOf("") }

    var daysWorkedInput by remember { mutableStateOf("20") }
    var hoursPerDayInput by remember { mutableStateOf("8.0") }
    var overtimeHoursInput by remember { mutableStateOf("") }
    var bonusInput by remember { mutableStateOf("") }
    var commissionInput by remember { mutableStateOf("") }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showChildBenefitDialog by remember { mutableStateOf(false) }
    var showTaxExplainerDialog by remember { mutableStateOf(false) }
    var showShiftCalendarDialog by remember { mutableStateOf(false) }
    var showCurrencySettingsDialog by remember { mutableStateOf(false) }
    var showTaxTrapDialog by remember { mutableStateOf(false) }
    var showTaxComparisonDialog by remember { mutableStateOf(false) }
    var showSa100Dialog by remember { mutableStateOf(false) }
    var showTaxRefundDialog by remember { mutableStateOf(false) }
    var saveMonthYear by remember { mutableStateOf("September 2026") }
    var saveNote by remember { mutableStateOf("") }

    val activeProfile = remember(profiles, activeProfileId) {
        profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
    }

    // Memoize input numbers
    val daysWorked = remember(daysWorkedInput) { daysWorkedInput.toDoubleOrNull() ?: 0.0 }
    val hoursPerDay = remember(hoursPerDayInput) { hoursPerDayInput.toDoubleOrNull() ?: 8.0 }
    val overtimeHours = remember(overtimeHoursInput) { overtimeHoursInput.toDoubleOrNull() ?: 0.0 }
    val bonusAmount = remember(bonusInput) { bonusInput.toDoubleOrNull() ?: 0.0 }
    val commissionAmount = remember(commissionInput) { commissionInput.toDoubleOrNull() ?: 0.0 }
    val salarySacrificeAmount = remember(salarySacrificeInput) { salarySacrificeInput.toDoubleOrNull() ?: 0.0 }

    // Memoize standard and overtime pay
    val standardPay = remember(daysWorked, hoursPerDay, defaultHourlyRate) {
        (daysWorked * hoursPerDay) * defaultHourlyRate
    }
    val overtimePay = remember(overtimeHours, defaultHourlyRate, selectedOvertimeMultiplier) {
        overtimeHours * (defaultHourlyRate * selectedOvertimeMultiplier)
    }
    val grossPay = remember(standardPay, overtimePay, bonusAmount, commissionAmount) {
        standardPay + overtimePay + bonusAmount + commissionAmount
    }

    // Full Salary Report
    val report: SalaryReport = remember(
        grossPay,
        taxCode,
        taxRegion,
        taxYear,
        bonusAmount,
        commissionAmount,
        selectedPensionPercent,
        selectedStudentLoan,
        salarySacrificeAmount,
        hasMarriageAllowance,
        hasBlindPersonsAllowance
    ) {
        TaxCalculator.calculateTax(
            grossPay = standardPay + overtimePay,
            taxCode = taxCode,
            isMonthly = true,
            region = taxRegion,
            taxYear = taxYear,
            bonusPay = bonusAmount,
            commissionPay = commissionAmount,
            pensionRatePercent = selectedPensionPercent,
            studentLoanPlan = selectedStudentLoan,
            salarySacrificeAmount = salarySacrificeAmount,
            hasMarriageAllowance = hasMarriageAllowance,
            hasBlindPersonsAllowance = hasBlindPersonsAllowance
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 900.dp

            if (isWideScreen) {
                // Adaptive 2-Column Dual-Pane Layout for Massive Screens
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Pane: Inputs & Adjustments
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        AppHeaderSection(
                            taxRegion = taxRegion,
                            taxCode = taxCode,
                            defaultHourlyRate = defaultHourlyRate,
                            standardPay = standardPay,
                            activeProfileName = activeProfile?.name,
                            onProfileClick = { showProfileDialog = true },
                            onTaxCodeClick = { showTaxExplainerDialog = true }
                        )
                        ShiftStopwatchCard(
                            salaryRepository = salaryRepository,
                            onApplyToCalculator = { days, hours ->
                                daysWorkedInput = if (days > 0) "%.0f".format(days) else "1"
                                hoursPerDayInput = "%.1f".format(hours)
                            }
                        )
                        SchedulePresetsSection(
                            daysWorkedInput = daysWorkedInput,
                            hoursPerDayInput = hoursPerDayInput,
                            onSelect = { days, hours ->
                                daysWorkedInput = days
                                hoursPerDayInput = hours
                            },
                            onChildBenefitClick = { showChildBenefitDialog = true },
                            onCalendarClick = { showShiftCalendarDialog = true },
                            onTaxTrapClick = { showTaxTrapDialog = true },
                            onTaxComparisonClick = { showTaxComparisonDialog = true },
                            onSa100Click = { showSa100Dialog = true },
                            onTaxRefundClick = { showTaxRefundDialog = true }
                        )
                        WorkingHoursCard(
                            daysWorkedInput = daysWorkedInput,
                            onDaysWorkedChange = { daysWorkedInput = it },
                            hoursPerDayInput = hoursPerDayInput,
                            onHoursPerDayChange = { hoursPerDayInput = it },
                            overtimeHoursInput = overtimeHoursInput,
                            onOvertimeHoursChange = { overtimeHoursInput = it },
                            selectedOvertimeMultiplier = selectedOvertimeMultiplier,
                            onOvertimeMultiplierChange = { selectedOvertimeMultiplier = it },
                            bonusInput = bonusInput,
                            onBonusChange = { bonusInput = it },
                            commissionInput = commissionInput,
                            onCommissionChange = { commissionInput = it }
                        )
                        DeductionsAdjustmentsCard(
                            selectedPensionPercent = selectedPensionPercent,
                            onPensionChange = { selectedPensionPercent = it },
                            selectedStudentLoan = selectedStudentLoan,
                            onStudentLoanChange = { selectedStudentLoan = it },
                            salarySacrificeInput = salarySacrificeInput,
                            onSalarySacrificeChange = { salarySacrificeInput = it }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Right Pane: Hero Summary, Breakdown & Actions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        FrequencySelectorRow(selectedFrequency = selectedFrequency, onSelect = { selectedFrequency = it })
                        HeroNetPayCard(
                            selectedFrequency = selectedFrequency,
                            report = report,
                            displayedNetAmount = displayedNetAmount,
                            grossPay = grossPay,
                            eurRate = customEurRate,
                            usdRate = customUsdRate,
                            onCurrencyClick = { showCurrencySettingsDialog = true }
                        )
                        DetailedPayslipCard(
                            daysWorked = daysWorked,
                            hoursPerDay = hoursPerDay,
                            standardPay = standardPay,
                            overtimeHours = overtimeHours,
                            overtimePay = overtimePay,
                            selectedOvertimeMultiplier = selectedOvertimeMultiplier,
                            report = report,
                            totalHours = totalHours,
                            selectedPensionPercent = selectedPensionPercent,
                            selectedStudentLoan = selectedStudentLoan,
                            taxRegion = taxRegion
                        )
                        MultiPeriodCard(report = report)
                        ActionButtonsRow(
                            onSaveClick = { showSaveDialog = true },
                            onPdfClick = {
                                val tempRecord = MonthlySalaryRecord(
                                    monthYear = saveMonthYear,
                                    daysWorked = daysWorked,
                                    hoursPerDay = hoursPerDay,
                                    overtimeHours = overtimeHours,
                                    overtimeMultiplier = selectedOvertimeMultiplier,
                                    hourlyRate = defaultHourlyRate,
                                    grossPay = report.grossPay,
                                    salarySacrifice = report.salarySacrifice,
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
                                    note = "Generated from Live Calculator"
                                )
                                val pdfFile = PdfPayslipGenerator.generatePayslipPdf(context, tempRecord)
                                PdfPayslipGenerator.sharePdf(context, pdfFile)
                            },
                            onShareClick = {
                                launchShareIntent(context, report, taxCode, taxRegion, selectedPensionPercent, selectedStudentLoan)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // Single Column Layout with Centering for Mobile / Dual-Pane Panels
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        AppHeaderSection(
                            taxRegion = taxRegion,
                            taxCode = taxCode,
                            defaultHourlyRate = defaultHourlyRate,
                            standardPay = standardPay,
                            activeProfileName = activeProfile?.name,
                            onProfileClick = { showProfileDialog = true },
                            onTaxCodeClick = { showTaxExplainerDialog = true }
                        )
                        FrequencySelectorRow(selectedFrequency = selectedFrequency, onSelect = { selectedFrequency = it })
                        HeroNetPayCard(
                            selectedFrequency = selectedFrequency,
                            report = report,
                            displayedNetAmount = displayedNetAmount,
                            grossPay = grossPay,
                            eurRate = customEurRate,
                            usdRate = customUsdRate,
                            onCurrencyClick = { showCurrencySettingsDialog = true }
                        )
                        ShiftStopwatchCard(
                            salaryRepository = salaryRepository,
                            onApplyToCalculator = { days, hours ->
                                daysWorkedInput = if (days > 0) "%.0f".format(days) else "1"
                                hoursPerDayInput = "%.1f".format(hours)
                            }
                        )
                        SchedulePresetsSection(
                            daysWorkedInput = daysWorkedInput,
                            hoursPerDayInput = hoursPerDayInput,
                            onSelect = { days, hours ->
                                daysWorkedInput = days
                                hoursPerDayInput = hours
                            },
                            onChildBenefitClick = { showChildBenefitDialog = true },
                            onCalendarClick = { showShiftCalendarDialog = true },
                            onTaxTrapClick = { showTaxTrapDialog = true },
                            onTaxComparisonClick = { showTaxComparisonDialog = true },
                            onSa100Click = { showSa100Dialog = true },
                            onTaxRefundClick = { showTaxRefundDialog = true }
                        )
                        WorkingHoursCard(
                            daysWorkedInput = daysWorkedInput,
                            onDaysWorkedChange = { daysWorkedInput = it },
                            hoursPerDayInput = hoursPerDayInput,
                            onHoursPerDayChange = { hoursPerDayInput = it },
                            overtimeHoursInput = overtimeHoursInput,
                            onOvertimeHoursChange = { overtimeHoursInput = it },
                            selectedOvertimeMultiplier = selectedOvertimeMultiplier,
                            onOvertimeMultiplierChange = { selectedOvertimeMultiplier = it },
                            bonusInput = bonusInput,
                            onBonusChange = { bonusInput = it },
                            commissionInput = commissionInput,
                            onCommissionChange = { commissionInput = it }
                        )
                        DeductionsAdjustmentsCard(
                            selectedPensionPercent = selectedPensionPercent,
                            onPensionChange = { selectedPensionPercent = it },
                            selectedStudentLoan = selectedStudentLoan,
                            onStudentLoanChange = { selectedStudentLoan = it },
                            salarySacrificeInput = salarySacrificeInput,
                            onSalarySacrificeChange = { salarySacrificeInput = it }
                        )
                        DetailedPayslipCard(
                            daysWorked = daysWorked,
                            hoursPerDay = hoursPerDay,
                            standardPay = standardPay,
                            overtimeHours = overtimeHours,
                            overtimePay = overtimePay,
                            selectedOvertimeMultiplier = selectedOvertimeMultiplier,
                            report = report,
                            totalHours = totalHours,
                            selectedPensionPercent = selectedPensionPercent,
                            selectedStudentLoan = selectedStudentLoan,
                            taxRegion = taxRegion
                        )
                        MultiPeriodCard(report = report)
                        ActionButtonsRow(
                            onSaveClick = { showSaveDialog = true },
                            onPdfClick = {
                                val tempRecord = MonthlySalaryRecord(
                                    monthYear = saveMonthYear,
                                    daysWorked = daysWorked,
                                    hoursPerDay = hoursPerDay,
                                    overtimeHours = overtimeHours,
                                    overtimeMultiplier = selectedOvertimeMultiplier,
                                    hourlyRate = defaultHourlyRate,
                                    grossPay = report.grossPay,
                                    salarySacrifice = report.salarySacrifice,
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
                                    note = "Generated from Live Calculator"
                                )
                                val pdfFile = PdfPayslipGenerator.generatePayslipPdf(context, tempRecord)
                                PdfPayslipGenerator.sharePdf(context, pdfFile)
                            },
                            onShareClick = {
                                launchShareIntent(context, report, taxCode, taxRegion, selectedPensionPercent, selectedStudentLoan)
                            }
                        )
                    }
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
                                salarySacrifice = report.salarySacrifice,
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
        // Profile Manager Dialog
        if (showProfileDialog) {
            ProfileManagerDialog(
                salaryRepository = salaryRepository,
                onDismiss = { showProfileDialog = false }
            )
        }
        // Child Benefit Dialog
        if (showChildBenefitDialog) {
            ChildBenefitDialog(
                initialAnnualIncome = report.annualGross,
                onDismiss = { showChildBenefitDialog = false }
            )
        }
        // Tax Code Explainer Dialog
        if (showTaxExplainerDialog) {
            TaxCodeExplainerDialog(
                currentTaxCode = taxCode,
                onDismiss = { showTaxExplainerDialog = false }
            )
        }
        // Shift Calendar Heatmap Dialog
        if (showShiftCalendarDialog) {
            ShiftCalendarDialog(
                initialDaysWorked = daysWorked,
                initialHoursPerDay = hoursPerDay,
                onApply = { days, hours, otHours ->
                    daysWorkedInput = if (days > 0) "%.0f".format(days) else "0"
                    hoursPerDayInput = "%.1f".format(hours)
                    if (otHours > 0) {
                        overtimeHoursInput = "%.1f".format(otHours)
                    }
                },
                onDismiss = { showShiftCalendarDialog = false }
            )
        }
        // Currency Settings Dialog
        if (showCurrencySettingsDialog) {
            CurrencySettingsDialog(
                salaryRepository = salaryRepository,
                onDismiss = { showCurrencySettingsDialog = false }
            )
        }
        // Marginal 60% Tax Trap Dialog
        if (showTaxTrapDialog) {
            MarginalTaxTrapDialog(
                initialAnnualIncome = report.annualGross,
                onDismiss = { showTaxTrapDialog = false }
            )
        }
        // Multi-Year Tax Comparison Dialog
        if (showTaxComparisonDialog) {
            TaxComparisonDialog(
                initialGrossAmount = report.grossPay,
                isMonthly = true,
                taxRegion = taxRegion,
                taxCode = taxCode,
                pensionRatePercent = selectedPensionPercent,
                onDismiss = { showTaxComparisonDialog = false }
            )
        }
        // HMRC SA100 Self-Assessment Return Dialog
        if (showSa100Dialog) {
            Sa100Dialog(
                taxReport = report,
                taxCode = taxCode,
                employerName = activeProfile?.name ?: "Primary Employment",
                taxYearLabel = taxYear.name.replace("YEAR_", "").replace("_", "/"),
                onDismiss = { showSa100Dialog = false }
            )
        }
        // Mid-Year Tax Code Refund & Rebate Estimator Dialog
        if (showTaxRefundDialog) {
            TaxRefundEstimatorDialog(
                initialMonthlyGross = report.grossPay,
                taxRegion = taxRegion,
                onDismiss = { showTaxRefundDialog = false }
            )
        }
    }
}

// Subcomponents

@Composable
private fun AppHeaderSection(
    taxRegion: TaxRegion,
    taxCode: String,
    defaultHourlyRate: Double,
    standardPay: Double,
    activeProfileName: String? = null,
    onProfileClick: () -> Unit = {},
    onTaxCodeClick: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Salary Calculator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeProfileName != null) {
                AssistChip(
                    onClick = onProfileClick,
                    label = { Text(activeProfileName, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.WorkOutline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(if (taxRegion == TaxRegion.SCOTLAND) "Scotland" else "UK Standard", style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(10.dp)
            )
            AssistChip(
                onClick = onTaxCodeClick,
                label = { Text(taxCode, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Explain Tax Code", modifier = Modifier.size(14.dp)) },
                shape = RoundedCornerShape(10.dp)
            )
        }

        Text(
            text = "Rate: £${"%.2f".format(defaultHourlyRate)}/hr · Base Pay: £${"%.2f".format(standardPay)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FrequencySelectorRow(
    selectedFrequency: PayFrequency,
    onSelect: (PayFrequency) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        PayFrequency.entries.forEachIndexed { index, freq ->
            SegmentedButton(
                selected = selectedFrequency == freq,
                onClick = { onSelect(freq) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = PayFrequency.entries.size),
                icon = {}
            ) {
                Text(freq.displayName, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HeroNetPayCard(
    selectedFrequency: PayFrequency,
    report: SalaryReport,
    displayedNetAmount: Double,
    grossPay: Double,
    eurRate: Double = ConvertedCurrencies.DEFAULT_EUR_RATE,
    usdRate: Double = ConvertedCurrencies.DEFAULT_USD_RATE,
    onCurrencyClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                val converted = CurrencyConverter.convert(amount, eurRate, usdRate)
                Column {
                    Text(
                        text = "£${"%.2f".format(amount)}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald60
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onCurrencyClick)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "≈ €${"%.2f".format(converted.eurAmount)} · $${"%.2f".format(converted.usdAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.CurrencyExchange,
                            contentDescription = "Edit FX Rates",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

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
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (netRatio > 0) Box(modifier = Modifier.weight(netRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Emerald60))
                        if (pensionRatio > 0) Box(modifier = Modifier.weight(pensionRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Teal60))
                        if (taxRatio > 0) Box(modifier = Modifier.weight(taxRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Rose60))
                        if (niRatio > 0) Box(modifier = Modifier.weight(niRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Amber60))
                        if (studentLoanRatio > 0) Box(modifier = Modifier.weight(studentLoanRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Violet60))
                    }

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
}

@Composable
private fun SchedulePresetsSection(
    daysWorkedInput: String,
    hoursPerDayInput: String,
    onSelect: (String, String) -> Unit,
    onChildBenefitClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onTaxTrapClick: () -> Unit = {},
    onTaxComparisonClick: () -> Unit = {},
    onSa100Click: () -> Unit = {},
    onTaxRefundClick: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Schedule Presets & Tools",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(
                onClick = onTaxComparisonClick,
                label = { Text("Multi-Year Tax", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(10.dp)
            )
            AssistChip(
                onClick = onSa100Click,
                label = { Text("SA100 Return", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
                shape = RoundedCornerShape(10.dp)
            )
            AssistChip(
                onClick = onTaxRefundClick,
                label = { Text("Tax Refund", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber60) },
                shape = RoundedCornerShape(10.dp)
            )
            AssistChip(
                onClick = onCalendarClick,
                label = { Text("Shift Heatmap", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)) },
                shape = RoundedCornerShape(10.dp)
            )
            AssistChip(
                onClick = onChildBenefitClick,
                label = { Text("Child Benefit", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.ChildCare, contentDescription = null, modifier = Modifier.size(14.dp)) },
                shape = RoundedCornerShape(10.dp)
            )
            AssistChip(
                onClick = onTaxTrapClick,
                label = { Text("60% Tax Trap", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Rose60) },
                shape = RoundedCornerShape(10.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = daysWorkedInput == "20" && hoursPerDayInput == "8.0",
                onClick = { onSelect("20", "8.0") },
                label = { Text("Full Month (20d · 8h)") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = daysWorkedInput == "21.67" && hoursPerDayInput == "7.5",
                onClick = { onSelect("21.67", "7.5") },
                label = { Text("UK Avg (21.7d · 7.5h)") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = daysWorkedInput == "16" && hoursPerDayInput == "8.0",
                onClick = { onSelect("16", "8.0") },
                label = { Text("4-Day Week (16d)") },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun WorkingHoursCard(
    daysWorkedInput: String,
    onDaysWorkedChange: (String) -> Unit,
    hoursPerDayInput: String,
    onHoursPerDayChange: (String) -> Unit,
    overtimeHoursInput: String,
    onOvertimeHoursChange: (String) -> Unit,
    selectedOvertimeMultiplier: Double,
    onOvertimeMultiplierChange: (Double) -> Unit,
    bonusInput: String = "",
    onBonusChange: (String) -> Unit = {},
    commissionInput: String = "",
    onCommissionChange: (String) -> Unit = {}
) {
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
                text = "Working Hours & Variable Earnings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = daysWorkedInput,
                onValueChange = onDaysWorkedChange,
                label = { Text("Days Worked") },
                placeholder = { Text("e.g. 20") },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                trailingIcon = {
                    if (daysWorkedInput.isNotEmpty()) {
                        IconButton(onClick = { onDaysWorkedChange("") }) {
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
                onValueChange = onHoursPerDayChange,
                label = { Text("Hours per Day") },
                placeholder = { Text("e.g. 8.0") },
                leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                trailingIcon = {
                    if (hoursPerDayInput.isNotEmpty()) {
                        IconButton(onClick = { onHoursPerDayChange("") }) {
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
                onValueChange = onOvertimeHoursChange,
                label = { Text("Overtime Hours (Optional)") },
                placeholder = { Text("e.g. 5") },
                leadingIcon = { Icon(Icons.Outlined.MoreTime, contentDescription = null) },
                trailingIcon = {
                    if (overtimeHoursInput.isNotEmpty()) {
                        IconButton(onClick = { onOvertimeHoursChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = bonusInput,
                    onValueChange = onBonusChange,
                    label = { Text("Bonus Pay (£)") },
                    placeholder = { Text("e.g. 250") },
                    prefix = { Text("£ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = commissionInput,
                    onValueChange = onCommissionChange,
                    label = { Text("Commission (£)") },
                    placeholder = { Text("e.g. 150") },
                    prefix = { Text("£ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

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
                        onClick = { onOvertimeMultiplierChange(1.0) },
                        label = { Text("1.0x (Standard)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedOvertimeMultiplier == 1.5,
                        onClick = { onOvertimeMultiplierChange(1.5) },
                        label = { Text("1.5x (Weekday)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedOvertimeMultiplier == 2.0,
                        onClick = { onOvertimeMultiplierChange(2.0) },
                        label = { Text("2.0x (Weekend)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedOvertimeMultiplier == 2.5,
                        onClick = { onOvertimeMultiplierChange(2.5) },
                        label = { Text("2.5x (Bank Hol)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeductionsAdjustmentsCard(
    selectedPensionPercent: Double,
    onPensionChange: (Double) -> Unit,
    selectedStudentLoan: StudentLoanPlan,
    onStudentLoanChange: (StudentLoanPlan) -> Unit,
    salarySacrificeInput: String,
    onSalarySacrificeChange: (String) -> Unit
) {
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
                text = "Pension, Student Loan & Sacrifice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Employee Pension
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Employee Pension: ${"%.1f".format(selectedPensionPercent)}%",
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
                            onClick = { onPensionChange(rate) },
                            label = { Text("${rate.toInt()}%") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Student Loan
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Student Loan: ${selectedStudentLoan.displayName}",
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
                            onClick = { onStudentLoanChange(plan) },
                            label = { Text(if (plan == StudentLoanPlan.NONE) "None" else plan.name.replace("_", " ")) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Salary Sacrifice Pre-Tax Deduction
            OutlinedTextField(
                value = salarySacrificeInput,
                onValueChange = onSalarySacrificeChange,
                label = { Text("Salary Sacrifice (Cycle to Work / EV)") },
                placeholder = { Text("e.g. 150.00") },
                prefix = { Text("£ ") },
                supportingText = { Text("Pre-tax deduction reducing Income Tax & NI") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun DetailedPayslipCard(
    daysWorked: Double,
    hoursPerDay: Double,
    standardPay: Double,
    overtimeHours: Double,
    overtimePay: Double,
    selectedOvertimeMultiplier: Double,
    report: SalaryReport,
    totalHours: Double,
    selectedPensionPercent: Double,
    selectedStudentLoan: StudentLoanPlan,
    taxRegion: TaxRegion
) {
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
                    text = "${"%.1f".format(totalHours)} hrs",
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
                    label = "Overtime (${"%.1f".format(overtimeHours)}h @ ${selectedOvertimeMultiplier}x)",
                    value = "£${"%.2f".format(overtimePay)}"
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PayslipRow(
                label = "Gross Total Pay",
                value = "£${"%.2f".format(report.grossPay)}",
                isBold = true
            )

            if (report.salarySacrifice > 0) {
                PayslipRow(
                    label = "Salary Sacrifice Schemes",
                    value = "-£${"%.2f".format(report.salarySacrifice)}",
                    valueColor = Rose60
                )
            }

            if (report.pensionContribution > 0) {
                PayslipRow(
                    label = "Employee Pension (${"%.1f".format(selectedPensionPercent)}%)",
                    value = "-£${"%.2f".format(report.pensionContribution)}",
                    valueColor = Teal60
                )
                PayslipRow(
                    label = "Employer Pension (3%)",
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
                        text = "After all deductions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "£${"%.2f".format(report.netPay)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = Emerald60,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MultiPeriodCard(report: SalaryReport) {
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
}

@Composable
private fun ActionButtonsRow(
    onSaveClick: () -> Unit,
    onPdfClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .weight(1.2f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        FilledTonalButton(
            onClick = onPdfClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("PDF", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        FilledTonalButton(
            onClick = onShareClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Share", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun launchShareIntent(
    context: android.content.Context,
    report: SalaryReport,
    taxCode: String,
    taxRegion: TaxRegion,
    selectedPensionPercent: Double,
    selectedStudentLoan: StudentLoanPlan
) {
    val shareText = """
        💰 UK Salary Calculator Summary
        ---------------------------------------
        Gross Pay: £${"%.2f".format(report.grossPay)} / month
        Tax Code: $taxCode (${if (taxRegion == TaxRegion.SCOTLAND) "Scotland" else "UK Standard"})
        
        Deductions:
        ${if (report.salarySacrifice > 0) "• Salary Sacrifice: £${"%.2f".format(report.salarySacrifice)}\n" else ""}
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
    context.startActivity(Intent.createChooser(sendIntent, "Share Payslip Summary"))
}

@Composable
private fun PeriodColumn(title: String, net: Double, gross: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = "£${"%,.0f".format(net)}",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Emerald60,
            maxLines = 1
        )
        Text(
            text = "£${"%,.0f".format(gross)}",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
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
            color = if (isSecondary) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
