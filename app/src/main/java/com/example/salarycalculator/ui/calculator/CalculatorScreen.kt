package com.example.salarycalculator.ui.calculator

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
fun CalculatorScreen(
    salaryRepository: SalaryRepository,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val taxCode by salaryRepository.getTaxCode().collectAsState(initial = "1257L")
    val defaultHourlyRate by salaryRepository.getDefaultHourlyRate().collectAsState(initial = 12.82)
    val taxRegion by salaryRepository.getTaxRegion().collectAsState(initial = TaxRegion.UK_STANDARD)
    val taxYear by salaryRepository.getTaxYear().collectAsState(initial = TaxYear.YEAR_2024_2025)
    val pensionRate by salaryRepository.getPensionRate().collectAsState(initial = 0.0)
    val studentLoanPlan by salaryRepository.getStudentLoanPlan().collectAsState(initial = StudentLoanPlan.NONE)
    val defaultOvertimeMultiplier by salaryRepository.getOvertimeMultiplier().collectAsState(initial = 1.5)
    val hasMarriageAllowance by salaryRepository.getHasMarriageAllowance().collectAsState(initial = false)
    val hasBlindPersonsAllowance by salaryRepository.getHasBlindPersonsAllowance().collectAsState(initial = false)
    val profiles by salaryRepository.getEmployerProfiles().collectAsState(initial = emptyList())
    val activeProfileId by salaryRepository.getActiveProfileId().collectAsState(initial = null)
    val customEurRate by salaryRepository.getCustomEurRate().collectAsState(initial = ConvertedCurrencies.DEFAULT_EUR_RATE)
    val customUsdRate by salaryRepository.getCustomUsdRate().collectAsState(initial = ConvertedCurrencies.DEFAULT_USD_RATE)
    val defaultHoursPerDay by salaryRepository.getDefaultHoursPerDay().collectAsState(initial = 12.0)
    val today = remember { java.util.Calendar.getInstance() }
    val curYear = today.get(java.util.Calendar.YEAR)
    val curMonth = today.get(java.util.Calendar.MONTH) + 1
    val prevMonth = if (curMonth == 1) 12 else curMonth - 1
    val prevYear = if (curMonth == 1) curYear - 1 else curYear
    val currentMonthShifts by salaryRepository.getMonthShiftSchedule(curYear, curMonth).collectAsState(initial = emptyMap())
    val prevMonthShifts by salaryRepository.getMonthShiftSchedule(prevYear, prevMonth).collectAsState(initial = emptyMap())
    val payScheduleConfig by salaryRepository.getPayScheduleConfig().collectAsState(initial = PayScheduleConfig())

    var selectedFrequency by remember { mutableStateOf(PayFrequency.MONTHLY) }
    var selectedOvertimeMultiplier by remember(defaultOvertimeMultiplier) { mutableStateOf(defaultOvertimeMultiplier) }
    var salarySacrificeInput by remember { mutableStateOf("") }

    var daysWorkedInput by remember { mutableStateOf("16") }
    var hoursPerDayOverride by remember { mutableStateOf<Double?>(null) }
    var overtimeHoursInput by remember { mutableStateOf("") }
    var bonusInput by remember { mutableStateOf("") }
    var commissionInput by remember { mutableStateOf("") }

    val activeProfile = remember(profiles, activeProfileId) {
        profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
    }
    val effectiveHourlyRate = activeProfile?.hourlyRate ?: defaultHourlyRate
    val effectiveHoursPerDay = activeProfile?.hoursPerDay ?: defaultHoursPerDay

    // Clear stale overrides whenever user updates standard shift length or changes active profile in Settings
    LaunchedEffect(defaultHoursPerDay, activeProfileId) {
        hoursPerDayOverride = null
    }

    // Live Sync with Shift Heatmap for current month (incorporating post-cutoff rollover from previous month)
    LaunchedEffect(currentMonthShifts, prevMonthShifts, effectiveHoursPerDay, payScheduleConfig) {
        if (currentMonthShifts.isNotEmpty()) {
            val split = PayScheduleEngine.calculateShiftPayrollSplit(
                year = curYear,
                month = curMonth,
                currentMonthShifts = currentMonthShifts,
                previousMonthShifts = prevMonthShifts,
                config = payScheduleConfig,
                standardHoursPerShift = effectiveHoursPerDay
            )
            if (split.totalPaidDays > 0) {
                daysWorkedInput = split.totalPaidDays.toString()
                hoursPerDayOverride = split.totalPaidHours / split.totalPaidDays
                overtimeHoursInput = if (split.totalPaidOtHours > 0) "%.1f".format(split.totalPaidOtHours) else ""
            } else {
                val dCount = currentMonthShifts.count { it.value > 0 }
                val totHrs = currentMonthShifts.values.sum()
                val otHrs = currentMonthShifts.values.sumOf { maxOf(0.0, it - effectiveHoursPerDay) }
                val avgHrs = if (dCount > 0) totHrs / dCount else effectiveHoursPerDay
                daysWorkedInput = dCount.toString()
                hoursPerDayOverride = avgHrs
                overtimeHoursInput = if (otHrs > 0) "%.1f".format(otHrs) else ""
            }
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showChildBenefitDialog by remember { mutableStateOf(false) }
    var showTaxExplainerDialog by remember { mutableStateOf(false) }
    var showShiftCalendarDialog by remember { mutableStateOf(false) }
    var showSandboxDialog by remember { mutableStateOf(false) }
    var showCurrencySettingsDialog by remember { mutableStateOf(false) }
    var showTaxTrapDialog by remember { mutableStateOf(false) }
    var showTaxComparisonDialog by remember { mutableStateOf(false) }
    var showSa100Dialog by remember { mutableStateOf(false) }
    var showTaxRefundDialog by remember { mutableStateOf(false) }
    var showCompanyCarDialog by remember { mutableStateOf(false) }
    var showStatutoryLeaveDialog by remember { mutableStateOf(false) }
    var showMortgageDialog by remember { mutableStateOf(false) }
    var showPayslipImportDialog by remember { mutableStateOf(false) }
    var showDirectorDividendDialog by remember { mutableStateOf(false) }
    var showYtdProjectionDialog by remember { mutableStateOf(false) }
    var showSalaryBenchmarkDialog by remember { mutableStateOf(false) }
    var showPensionAllowanceDialog by remember { mutableStateOf(false) }
    var showStudentLoanPayoffDialog by remember { mutableStateOf(false) }
    var showSelfEmployedDialog by remember { mutableStateOf(false) }
    var showGiftAidDialog by remember { mutableStateOf(false) }
    var showCapitalGainsDialog by remember { mutableStateOf(false) }
    var showOvertimeOptimizerDialog by remember { mutableStateOf(false) }
    var showMultiJobDialog by remember { mutableStateOf(false) }
    var showUmbrellaPayrollDialog by remember { mutableStateOf(false) }
    var showTaxFreeChildcareDialog by remember { mutableStateOf(false) }
    var showOvertimeBracketDialog by remember { mutableStateOf(false) }
    var showHicbcDialog by remember { mutableStateOf(false) }
    var showMultiCurrencyDialog by remember { mutableStateOf(false) }
    var showSspHolidayDialog by remember { mutableStateOf(false) }
    var showShiftRateDifferentialDialog by remember { mutableStateOf(false) }
    var saveMonthYear by remember { mutableStateOf("August 2026") }
    var saveNote by remember { mutableStateOf("") }
    var selectedTaxMonth by remember { mutableStateOf(5) } // Default to Month 5 (August) per care worker calibration
    var showTaxMonthDialog by remember { mutableStateOf(false) }

    // Memoize input numbers
    val daysWorked = remember(daysWorkedInput) { daysWorkedInput.toDoubleOrNull() ?: 0.0 }
    val hoursPerDay = hoursPerDayOverride ?: effectiveHoursPerDay
    val overtimeHours = remember(overtimeHoursInput) { overtimeHoursInput.toDoubleOrNull() ?: 0.0 }
    val bonusAmount = remember(bonusInput) { bonusInput.toDoubleOrNull() ?: 0.0 }
    val commissionAmount = remember(commissionInput) { commissionInput.toDoubleOrNull() ?: 0.0 }
    val salarySacrificeAmount = remember(salarySacrificeInput) { salarySacrificeInput.toDoubleOrNull() ?: 0.0 }

    // Selected UK tax year month (Month 1 = April, Month 5 = August, Month 6 = September, etc.)
    val taxMonth = selectedTaxMonth

    // Memoize standard and overtime pay
    val standardPay = remember(daysWorked, hoursPerDay, effectiveHourlyRate) {
        (daysWorked * hoursPerDay) * effectiveHourlyRate
    }
    val overtimePay = remember(overtimeHours, effectiveHourlyRate, selectedOvertimeMultiplier) {
        overtimeHours * (effectiveHourlyRate * selectedOvertimeMultiplier)
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
        pensionRate,
        studentLoanPlan,
        salarySacrificeAmount,
        hasMarriageAllowance,
        hasBlindPersonsAllowance,
        taxMonth
    ) {
        TaxCalculator.calculateTax(
            grossPay = standardPay + overtimePay,
            taxCode = taxCode,
            isMonthly = true,
            region = taxRegion,
            taxYear = taxYear,
            bonusPay = bonusAmount,
            commissionPay = commissionAmount,
            pensionRatePercent = pensionRate,
            studentLoanPlan = studentLoanPlan,
            salarySacrificeAmount = salarySacrificeAmount,
            hasMarriageAllowance = hasMarriageAllowance,
            hasBlindPersonsAllowance = hasBlindPersonsAllowance,
            taxMonth = taxMonth
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
                            defaultHourlyRate = effectiveHourlyRate,
                            defaultHoursPerDay = effectiveHoursPerDay,
                            pensionRate = pensionRate,
                            studentLoanPlan = studentLoanPlan,
                            standardPay = standardPay,
                            activeProfileName = activeProfile?.name,
                            onProfileClick = { showProfileDialog = true },
                            onTaxCodeClick = { showTaxExplainerDialog = true },
                            onNavigateToSettings = onNavigateToSettings,
                            selectedTaxMonth = selectedTaxMonth,
                            onTaxMonthClick = { showTaxMonthDialog = true }
                        )
                        ShiftStopwatchCard(
                            salaryRepository = salaryRepository,
                            onApplyToCalculator = { days, hours ->
                                daysWorkedInput = if (days > 0) "%.0f".format(days) else "1"
                                if (hours > 0) hoursPerDayOverride = hours
                            }
                        )
                        SchedulePresetsSection(
                            daysWorkedInput = daysWorkedInput,
                            onSelect = { days ->
                                daysWorkedInput = days
                            },
                            onChildBenefitClick = { showChildBenefitDialog = true },
                            onCalendarClick = { showShiftCalendarDialog = true },
                            onSandboxClick = { showSandboxDialog = true },
                            onTaxTrapClick = { showTaxTrapDialog = true },
                            onTaxComparisonClick = { showTaxComparisonDialog = true },
                            onSa100Click = { showSa100Dialog = true },
                            onTaxRefundClick = { showTaxRefundDialog = true },
                            onCompanyCarClick = { showCompanyCarDialog = true },
                            onStatutoryLeaveClick = { showStatutoryLeaveDialog = true },
                            onMortgageClick = { showMortgageDialog = true },
                            onPayslipOcrClick = { showPayslipImportDialog = true },
                            onDirectorTaxClick = { showDirectorDividendDialog = true },
                            onYtdProjectionClick = { showYtdProjectionDialog = true },
                            onSalaryBenchmarkClick = { showSalaryBenchmarkDialog = true },
                            onOvertimeOptimizerClick = { showOvertimeOptimizerDialog = true },
                            onMultiJobClick = { showMultiJobDialog = true },
                            onUmbrellaClick = { showUmbrellaPayrollDialog = true },
                            onTaxFreeChildcareClick = { showTaxFreeChildcareDialog = true },
                            onOvertimeBracketClick = { showOvertimeBracketDialog = true },
                            onHicbcClick = { showHicbcDialog = true },
                            onMultiCurrencyClick = { showMultiCurrencyDialog = true },
                            onSspHolidayClick = { showSspHolidayDialog = true },
                            onShiftRateDifferentialClick = { showShiftRateDifferentialDialog = true }
                        )
                        WorkingHoursCard(
                            daysWorkedInput = daysWorkedInput,
                            onDaysWorkedChange = { daysWorkedInput = it },
                            defaultHoursPerDay = effectiveHoursPerDay,
                            defaultHourlyRate = effectiveHourlyRate,
                            onQuickShiftChange = { hrs ->
                                scope.launch {
                                    salaryRepository.setDefaultHoursPerDay(hrs)
                                    hoursPerDayOverride = null
                                }
                            },
                            overtimeHoursInput = overtimeHoursInput,
                            onOvertimeHoursChange = { overtimeHoursInput = it },
                            selectedOvertimeMultiplier = selectedOvertimeMultiplier,
                            onOvertimeMultiplierChange = { selectedOvertimeMultiplier = it },
                            bonusInput = bonusInput,
                            onBonusChange = { bonusInput = it },
                            commissionInput = commissionInput,
                            onCommissionChange = { commissionInput = it },
                            onNavigateToSettings = onNavigateToSettings
                        )
                        DeductionsAdjustmentsCard(
                            pensionRate = pensionRate,
                            studentLoanPlan = studentLoanPlan,
                            salarySacrificeInput = salarySacrificeInput,
                            onSalarySacrificeChange = { salarySacrificeInput = it },
                            onNavigateToSettings = onNavigateToSettings
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
                            selectedPensionPercent = pensionRate,
                            selectedStudentLoan = studentLoanPlan,
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
                                    hourlyRate = effectiveHourlyRate,
                                    grossPay = report.grossPay,
                                    salarySacrifice = report.salarySacrifice,
                                    pensionRate = pensionRate,
                                    pensionContribution = report.pensionContribution,
                                    employerPension = report.employerPensionContribution,
                                    taxablePay = report.taxablePay,
                                    incomeTax = report.incomeTax,
                                    nationalInsurance = report.nationalInsurance,
                                    studentLoanPlan = studentLoanPlan,
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
                                launchShareIntent(context, report, taxCode, taxRegion, pensionRate, studentLoanPlan)
                            },
                            onEmailClick = {
                                val tempRecord = MonthlySalaryRecord(
                                    monthYear = saveMonthYear,
                                    daysWorked = daysWorked,
                                    hoursPerDay = hoursPerDay,
                                    overtimeHours = overtimeHours,
                                    overtimeMultiplier = selectedOvertimeMultiplier,
                                    hourlyRate = effectiveHourlyRate,
                                    grossPay = report.grossPay,
                                    salarySacrifice = report.salarySacrifice,
                                    pensionRate = pensionRate,
                                    pensionContribution = report.pensionContribution,
                                    employerPension = report.employerPensionContribution,
                                    taxablePay = report.taxablePay,
                                    incomeTax = report.incomeTax,
                                    nationalInsurance = report.nationalInsurance,
                                    studentLoanPlan = studentLoanPlan,
                                    studentLoanDeduction = report.studentLoanDeduction,
                                    totalDeductions = report.totalDeductions,
                                    netPay = report.netPay,
                                    taxCode = taxCode,
                                    taxRegion = taxRegion,
                                    note = "Generated from Live Calculator"
                                )
                                val pdfFile = PdfPayslipGenerator.generatePayslipPdf(context, tempRecord)
                                val bodyText = "Hello,\n\nPlease find attached the salary calculation breakdown for ${tempRecord.monthYear}.\n\nGross Pay: £${"%.2f".format(report.grossPay)}\nNet Take-Home: £${"%.2f".format(report.netPay)}\n\nKind regards."
                                EmailExporter.dispatchEmailWithAttachment(
                                    context = context,
                                    file = pdfFile,
                                    subject = "[Salary Calculator] Payslip - ${tempRecord.monthYear}",
                                    bodyText = bodyText
                                )
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
                            defaultHourlyRate = effectiveHourlyRate,
                            defaultHoursPerDay = effectiveHoursPerDay,
                            pensionRate = pensionRate,
                            studentLoanPlan = studentLoanPlan,
                            standardPay = standardPay,
                            activeProfileName = activeProfile?.name,
                            onProfileClick = { showProfileDialog = true },
                            onTaxCodeClick = { showTaxExplainerDialog = true },
                            onNavigateToSettings = onNavigateToSettings,
                            selectedTaxMonth = selectedTaxMonth,
                            onTaxMonthClick = { showTaxMonthDialog = true }
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
                                if (hours > 0) hoursPerDayOverride = hours
                            }
                        )
                        SchedulePresetsSection(
                            daysWorkedInput = daysWorkedInput,
                            onSelect = { days ->
                                daysWorkedInput = days
                            },
                            onChildBenefitClick = { showChildBenefitDialog = true },
                            onCalendarClick = { showShiftCalendarDialog = true },
                            onSandboxClick = { showSandboxDialog = true },
                            onTaxTrapClick = { showTaxTrapDialog = true },
                            onTaxComparisonClick = { showTaxComparisonDialog = true },
                            onSa100Click = { showSa100Dialog = true },
                            onTaxRefundClick = { showTaxRefundDialog = true },
                            onCompanyCarClick = { showCompanyCarDialog = true },
                            onStatutoryLeaveClick = { showStatutoryLeaveDialog = true },
                            onMortgageClick = { showMortgageDialog = true },
                            onPayslipOcrClick = { showPayslipImportDialog = true },
                            onDirectorTaxClick = { showDirectorDividendDialog = true },
                            onYtdProjectionClick = { showYtdProjectionDialog = true },
                            onSalaryBenchmarkClick = { showSalaryBenchmarkDialog = true },
                            onPensionAllowanceClick = { showPensionAllowanceDialog = true },
                            onStudentLoanPayoffClick = { showStudentLoanPayoffDialog = true },
                            onSelfEmployedClick = { showSelfEmployedDialog = true },
                            onGiftAidClick = { showGiftAidDialog = true },
                            onCapitalGainsClick = { showCapitalGainsDialog = true },
                            onOvertimeOptimizerClick = { showOvertimeOptimizerDialog = true },
                            onMultiJobClick = { showMultiJobDialog = true },
                            onUmbrellaClick = { showUmbrellaPayrollDialog = true },
                            onTaxFreeChildcareClick = { showTaxFreeChildcareDialog = true },
                            onOvertimeBracketClick = { showOvertimeBracketDialog = true },
                            onHicbcClick = { showHicbcDialog = true },
                            onMultiCurrencyClick = { showMultiCurrencyDialog = true },
                            onSspHolidayClick = { showSspHolidayDialog = true },
                            onShiftRateDifferentialClick = { showShiftRateDifferentialDialog = true }
                        )
                        WorkingHoursCard(
                            daysWorkedInput = daysWorkedInput,
                            onDaysWorkedChange = { daysWorkedInput = it },
                            defaultHoursPerDay = effectiveHoursPerDay,
                            defaultHourlyRate = effectiveHourlyRate,
                            onQuickShiftChange = { hrs ->
                                scope.launch {
                                    salaryRepository.setDefaultHoursPerDay(hrs)
                                    hoursPerDayOverride = null
                                }
                            },
                            overtimeHoursInput = overtimeHoursInput,
                            onOvertimeHoursChange = { overtimeHoursInput = it },
                            selectedOvertimeMultiplier = selectedOvertimeMultiplier,
                            onOvertimeMultiplierChange = { selectedOvertimeMultiplier = it },
                            bonusInput = bonusInput,
                            onBonusChange = { bonusInput = it },
                            commissionInput = commissionInput,
                            onCommissionChange = { commissionInput = it },
                            onNavigateToSettings = onNavigateToSettings
                        )
                        DeductionsAdjustmentsCard(
                            pensionRate = pensionRate,
                            studentLoanPlan = studentLoanPlan,
                            salarySacrificeInput = salarySacrificeInput,
                            onSalarySacrificeChange = { salarySacrificeInput = it },
                            onNavigateToSettings = onNavigateToSettings
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
                            selectedPensionPercent = pensionRate,
                            selectedStudentLoan = studentLoanPlan,
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
                                    hourlyRate = effectiveHourlyRate,
                                    grossPay = report.grossPay,
                                    salarySacrifice = report.salarySacrifice,
                                    pensionRate = pensionRate,
                                    pensionContribution = report.pensionContribution,
                                    employerPension = report.employerPensionContribution,
                                    taxablePay = report.taxablePay,
                                    incomeTax = report.incomeTax,
                                    nationalInsurance = report.nationalInsurance,
                                    studentLoanPlan = studentLoanPlan,
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
                                launchShareIntent(context, report, taxCode, taxRegion, pensionRate, studentLoanPlan)
                            },
                            onEmailClick = {
                                val tempRecord = MonthlySalaryRecord(
                                    monthYear = saveMonthYear,
                                    daysWorked = daysWorked,
                                    hoursPerDay = hoursPerDay,
                                    overtimeHours = overtimeHours,
                                    overtimeMultiplier = selectedOvertimeMultiplier,
                                    hourlyRate = effectiveHourlyRate,
                                    grossPay = report.grossPay,
                                    salarySacrifice = report.salarySacrifice,
                                    pensionRate = pensionRate,
                                    pensionContribution = report.pensionContribution,
                                    employerPension = report.employerPensionContribution,
                                    taxablePay = report.taxablePay,
                                    incomeTax = report.incomeTax,
                                    nationalInsurance = report.nationalInsurance,
                                    studentLoanPlan = studentLoanPlan,
                                    studentLoanDeduction = report.studentLoanDeduction,
                                    totalDeductions = report.totalDeductions,
                                    netPay = report.netPay,
                                    taxCode = taxCode,
                                    taxRegion = taxRegion,
                                    note = "Generated from Live Calculator"
                                )
                                val pdfFile = PdfPayslipGenerator.generatePayslipPdf(context, tempRecord)
                                val bodyText = "Hello,\n\nPlease find attached the salary calculation breakdown for ${tempRecord.monthYear}.\n\nGross Pay: £${"%.2f".format(report.grossPay)}\nNet Take-Home: £${"%.2f".format(report.netPay)}\n\nKind regards."
                                EmailExporter.dispatchEmailWithAttachment(
                                    context = context,
                                    file = pdfFile,
                                    subject = "[Salary Calculator] Payslip - ${tempRecord.monthYear}",
                                    bodyText = bodyText
                                )
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
                                pensionRate = pensionRate,
                                pensionContribution = report.pensionContribution,
                                employerPension = report.employerPensionContribution,
                                taxablePay = report.taxablePay,
                                incomeTax = report.incomeTax,
                                nationalInsurance = report.nationalInsurance,
                                studentLoanPlan = studentLoanPlan,
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
                salaryRepository = salaryRepository,
                hourlyRate = activeProfile?.hourlyRate ?: defaultHourlyRate,
                overtimeMultiplier = selectedOvertimeMultiplier,
                onApply = { days, hours, otHours ->
                    daysWorkedInput = if (days > 0) "%.0f".format(days) else "0"
                    if (hours > 0) hoursPerDayOverride = hours
                    if (otHours > 0) {
                        overtimeHoursInput = "%.1f".format(otHours)
                    }
                },
                onDismiss = { showShiftCalendarDialog = false }
            )
        }
        // What-If Scenario Sandbox Dialog
        if (showSandboxDialog) {
            SandboxCalculatorDialog(
                baselineDays = daysWorked,
                baselineHoursPerDay = hoursPerDay,
                baselineOvertimeHours = overtimeHours,
                hourlyRate = activeProfile?.hourlyRate ?: defaultHourlyRate,
                taxCode = taxCode,
                taxRegion = taxRegion,
                taxYear = taxYear,
                pensionRate = pensionRate,
                studentLoanPlan = studentLoanPlan,
                hasMarriageAllowance = hasMarriageAllowance,
                hasBlindPersonsAllowance = hasBlindPersonsAllowance,
                onApplyToCalculator = { sDays, sHrs, sOt, sBonus, sComm ->
                    daysWorkedInput = if (sDays > 0) "%.0f".format(sDays) else "0"
                    if (sHrs > 0) hoursPerDayOverride = sHrs
                    overtimeHoursInput = if (sOt > 0) "%.1f".format(sOt) else ""
                    if (sBonus > 0) bonusInput = "%.2f".format(sBonus)
                    if (sComm > 0) commissionInput = "%.2f".format(sComm)
                },
                onDismiss = { showSandboxDialog = false }
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
                pensionRatePercent = pensionRate,
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
        // Company Car BiK Dialog
        if (showCompanyCarDialog) {
            CompanyCarDialog(
                onDismiss = { showCompanyCarDialog = false }
            )
        }
        // Statutory Leave (SSP / SMP / SPP) Dialog
        if (showStatutoryLeaveDialog) {
            val weeklyGross = if (daysWorked > 0 && hoursPerDay > 0) {
                (daysWorked / 4.3333) * hoursPerDay * (activeProfile?.hourlyRate ?: defaultHourlyRate)
            } else {
                report.grossPay / 4.3333
            }
            StatutoryLeaveDialog(
                initialWeeklyGross = weeklyGross,
                taxRegion = taxRegion,
                onDismiss = { showStatutoryLeaveDialog = false }
            )
        }
        // Statutory Sick Pay & Holiday Pay Accrual Dialog (FEAT-242)
        if (showSspHolidayDialog) {
            SspHolidayDialog(
                currentHourlyRate = effectiveHourlyRate,
                currentStandardHours = hoursPerDay,
                totalLoggedHours = totalHours,
                onDismiss = { showSspHolidayDialog = false }
            )
        }
        // Shift Rate Differentials & Enhancements Dialog (FEAT-243)
        if (showShiftRateDifferentialDialog) {
            ShiftRateDifferentialDialog(
                currentHourlyRate = effectiveHourlyRate,
                currentStandardHours = hoursPerDay,
                totalLoggedHours = totalHours,
                onDismiss = { showShiftRateDifferentialDialog = false }
            )
        }
        // Mortgage Borrowing Capacity Dialog
        if (showMortgageDialog) {
            MortgageBorrowingDialog(
                annualGross = report.annualGross,
                monthlyNet = report.netPay,
                onDismiss = { showMortgageDialog = false }
            )
        }
        // Payslip ML OCR Scanner & Statutory Auditor Dialog
        if (showPayslipImportDialog) {
            PayslipImportDialog(
                salaryRepository = salaryRepository,
                onApplyToCalculator = { importedGross, importedTaxCode, importedPension ->
                    if (importedGross > 0) {
                        daysWorkedInput = "16"
                        hoursPerDayOverride = defaultHoursPerDay
                    }
                },
                onDismiss = { showPayslipImportDialog = false }
            )
        }
        // Company Director Dividend vs Salary Optimizer Dialog
        if (showDirectorDividendDialog) {
            DirectorDividendDialog(
                onDismiss = { showDirectorDividendDialog = false }
            )
        }
        // Year-to-Date (YTD) Tax Forecasting & Pension Top-Up Dialog
        if (showYtdProjectionDialog) {
            val historyList by salaryRepository.getSalaryHistory().collectAsState(initial = emptyList())
            YtdProjectionDialog(
                historyRecords = historyList,
                currentMonthlyGross = report.grossPay,
                taxCode = taxCode,
                taxRegion = taxRegion,
                onDismiss = { showYtdProjectionDialog = false }
            )
        }
        // Salary Benchmarking & Regional Market Wage Dialog
        if (showSalaryBenchmarkDialog) {
            SalaryBenchmarkDialog(
                currentAnnualGross = report.annualGross,
                onDismiss = { showSalaryBenchmarkDialog = false }
            )
        }
        // Pension Annual Allowance & Tapering Dialog
        if (showPensionAllowanceDialog) {
            PensionAllowanceDialog(
                initialAnnualGross = report.annualGross,
                initialPensionRate = pensionRate,
                taxRegion = taxRegion,
                onDismiss = { showPensionAllowanceDialog = false }
            )
        }
        // Student Loan Payoff Horizon Dialog
        if (showStudentLoanPayoffDialog) {
            StudentLoanPayoffDialog(
                initialPlan = studentLoanPlan,
                initialSalary = report.annualGross,
                onDismiss = { showStudentLoanPayoffDialog = false }
            )
        }
        // Self-Employed & Payments on Account Dialog
        if (showSelfEmployedDialog) {
            SelfEmployedTaxDialog(
                initialPayeGross = report.annualGross,
                initialPayeTax = report.incomeTax * 12.0,
                taxRegion = taxRegion,
                onDismiss = { showSelfEmployedDialog = false }
            )
        }
        // Gift Aid & Higher-Rate Tax Relief Dialog
        if (showGiftAidDialog) {
            GiftAidDialog(
                initialSalary = report.annualGross,
                taxRegion = taxRegion,
                onDismiss = { showGiftAidDialog = false }
            )
        }
        // Capital Gains Tax (CGT) Dialog
        if (showCapitalGainsDialog) {
            CapitalGainsDialog(
                initialSalary = report.annualGross,
                onDismiss = { showCapitalGainsDialog = false }
            )
        }
        // Overtime Tax-Efficiency & Marginal Return Optimizer Dialog
        if (showOvertimeOptimizerDialog) {
            val baseHourly = activeProfile?.hourlyRate ?: defaultHourlyRate
            val standardMonthlyBase = (daysWorked * hoursPerDay * baseHourly)
            OvertimeTaxOptimizerDialog(
                baseGrossMonthly = standardMonthlyBase,
                baseHourlyRate = baseHourly,
                taxCode = taxCode,
                taxRegion = taxRegion,
                taxYear = taxYear,
                pensionRate = pensionRate,
                studentLoanPlan = studentLoanPlan,
                hasMarriageAllowance = hasMarriageAllowance,
                hasBlindPersonsAllowance = hasBlindPersonsAllowance,
                defaultOvertimeMultiplier = selectedOvertimeMultiplier,
                onDismiss = { showOvertimeOptimizerDialog = false }
            )
        }
        // Dual-Job & Multi-Employer Aggregator Dialog
        if (showMultiJobDialog) {
            MultiJobTaxDialog(
                onDismiss = { showMultiJobDialog = false }
            )
        }
        // Umbrella Contractor & Employer On-Cost Dialog
        if (showUmbrellaPayrollDialog) {
            UmbrellaPayrollDialog(
                onDismiss = { showUmbrellaPayrollDialog = false }
            )
        }
        // HMRC Tax-Free Childcare & 30-Hours Subsidy Dialog
        if (showTaxFreeChildcareDialog) {
            TaxFreeChildcareDialog(
                initialSalary = report.annualGross,
                onDismiss = { showTaxFreeChildcareDialog = false }
            )
        }
        // Overtime Bracket Headroom & Threshold Monitor Dialog
        if (showOvertimeBracketDialog) {
            val baseHourly = activeProfile?.hourlyRate ?: defaultHourlyRate
            val standardAnnualBase = (daysWorked * hoursPerDay * baseHourly) * 12.0
            OvertimeBracketDialog(
                initialBaseSalary = standardAnnualBase,
                initialHourlyRate = baseHourly,
                onDismiss = { showOvertimeBracketDialog = false }
            )
        }
        // High Income Child Benefit Charge (HICBC) Dialog
        if (showHicbcDialog) {
            HicbcDialog(
                initialGrossAnnual = report.annualGross,
                onDismissRequest = { showHicbcDialog = false },
                onApplyPensionSacrifice = { sacrificeAnnual ->
                    val monthlySacrifice = sacrificeAnnual / 12.0
                    salarySacrificeInput = String.format("%.2f", monthlySacrifice)
                }
            )
        }
        // Company Car & EV Benefit-in-Kind (BiK) Dialog
        if (showCompanyCarDialog) {
            CompanyCarBikDialog(
                initialGrossAnnual = report.annualGross,
                onDismissRequest = { showCompanyCarDialog = false }
            )
        }
        // Multi-Currency Converter Dialog
        if (showMultiCurrencyDialog) {
            MultiCurrencyConverterDialog(
                annualNetTakeHomeGbp = report.annualNet,
                annualGrossGbp = report.annualGross,
                onDismissRequest = { showMultiCurrencyDialog = false }
            )
        }
        // Payroll Tax Month Selection Dialog
        if (showTaxMonthDialog) {
            TaxMonthSelectionDialog(
                selectedTaxMonth = selectedTaxMonth,
                onSelectMonth = { month, name ->
                    selectedTaxMonth = month
                    saveMonthYear = name
                },
                onDismiss = { showTaxMonthDialog = false }
            )
        }
    }
}

// Subcomponents

private enum class ToolCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ALL("All Tools", Icons.Default.GridView),
    TAX_RELIEF("Tax & Relief", Icons.Default.AccountBalance),
    WORK_SHIFTS("Work & Shifts", Icons.Default.Schedule),
    WEALTH_PLANNING("Wealth & Goals", Icons.AutoMirrored.Filled.TrendingUp)
}

@Composable
private fun AppHeaderSection(
    taxRegion: TaxRegion,
    taxCode: String,
    defaultHourlyRate: Double,
    defaultHoursPerDay: Double = 12.0,
    pensionRate: Double = 0.0,
    studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
    standardPay: Double,
    activeProfileName: String? = null,
    onProfileClick: () -> Unit = {},
    onTaxCodeClick: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    selectedTaxMonth: Int = 5,
    onTaxMonthClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Salary Calculator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "UK PAYE & Take-Home Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onNavigateToSettings,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "£${"%.2f".format(defaultHourlyRate)}/hr",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1
                        )
                        Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

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
                        label = { Text(activeProfileName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.WorkOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                AssistChip(
                    onClick = onNavigateToSettings,
                    label = { Text("${"%.1f".format(defaultHoursPerDay)}h Shift", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(10.dp)
                )
                AssistChip(
                    onClick = onNavigateToSettings,
                    label = { Text(if (pensionRate == 0.0) "Pension: Opted Out" else "Pension: ${"%.1f".format(pensionRate)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (pensionRate == 0.0) Emerald60 else MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(10.dp)
                )
                AssistChip(
                    onClick = onTaxMonthClick,
                    label = {
                        val monthLabel = when (selectedTaxMonth) {
                            1 -> "Apr (M1)"
                            2 -> "May (M2)"
                            3 -> "Jun (M3)"
                            4 -> "Jul (M4)"
                            5 -> "Aug (M5)"
                            6 -> "Sep (M6)"
                            7 -> "Oct (M7)"
                            8 -> "Nov (M8)"
                            9 -> "Dec (M9)"
                            10 -> "Jan (M10)"
                            11 -> "Feb (M11)"
                            12 -> "Mar (M12)"
                            else -> "M$selectedTaxMonth"
                        }
                        Text("Month: $monthLabel", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Select Tax Month", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(10.dp)
                )
                AssistChip(
                    onClick = onTaxCodeClick,
                    label = { Text("Code: $taxCode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Explain Tax Code", modifier = Modifier.size(14.dp), tint = Emerald60) },
                    shape = RoundedCornerShape(10.dp)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(if (taxRegion == TaxRegion.SCOTLAND) "🏴󠁧󠁢󠁳󠁣󠁴󠁿 Scotland" else "🇬🇧 UK", style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(10.dp)
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Base: £${"%.2f".format(standardPay)}", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber60) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
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
                Text(
                    text = freq.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (selectedFrequency == freq) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Emerald60)
                    )
                    Text(
                        text = "Net (${selectedFrequency.displayName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = EmeraldContainerLight,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${"%.1f".format(report.takeHomePercentage)}% Take-Home",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald40,
                        maxLines = 1
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
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald60
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCurrencyClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "≈ €${"%.2f".format(converted.eurAmount)} · $${"%.2f".format(converted.usdAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                Icons.Default.CurrencyExchange,
                                contentDescription = "Edit FX Rates",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                        LegendItem(color = Emerald60, label = "Take Home (${"%.0f".format(report.takeHomePercentage)}%)")
                        if (report.pensionContribution > 0) LegendItem(color = Teal60, label = "Pension (${"%.0f".format((report.pensionContribution / grossPay) * 100)}%)")
                        LegendItem(color = Rose60, label = "PAYE (${"%.0f".format((report.incomeTax / grossPay) * 100)}%)")
                        LegendItem(color = Amber60, label = "NI (${"%.0f".format((report.nationalInsurance / grossPay) * 100)}%)")
                        if (report.studentLoanDeduction > 0) LegendItem(color = Violet60, label = "Student Loan (${"%.0f".format((report.studentLoanDeduction / grossPay) * 100)}%)")
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulePresetsSection(
    daysWorkedInput: String,
    onSelect: (String) -> Unit,
    onChildBenefitClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onSandboxClick: () -> Unit = {},
    onTaxTrapClick: () -> Unit = {},
    onTaxComparisonClick: () -> Unit = {},
    onSa100Click: () -> Unit = {},
    onTaxRefundClick: () -> Unit = {},
    onCompanyCarClick: () -> Unit = {},
    onStatutoryLeaveClick: () -> Unit = {},
    onMortgageClick: () -> Unit = {},
    onPayslipOcrClick: () -> Unit = {},
    onDirectorTaxClick: () -> Unit = {},
    onYtdProjectionClick: () -> Unit = {},
    onSalaryBenchmarkClick: () -> Unit = {},
    onPensionAllowanceClick: () -> Unit = {},
    onStudentLoanPayoffClick: () -> Unit = {},
    onSelfEmployedClick: () -> Unit = {},
    onGiftAidClick: () -> Unit = {},
    onCapitalGainsClick: () -> Unit = {},
    onOvertimeOptimizerClick: () -> Unit = {},
    onMultiJobClick: () -> Unit = {},
    onUmbrellaClick: () -> Unit = {},
    onTaxFreeChildcareClick: () -> Unit = {},
    onOvertimeBracketClick: () -> Unit = {},
    onHicbcClick: () -> Unit = {},
    onMultiCurrencyClick: () -> Unit = {},
    onSspHolidayClick: () -> Unit = {},
    onShiftRateDifferentialClick: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(ToolCategory.ALL) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tools & Schedules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    text = "20+ Features",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ToolCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.title, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Filtered Tools Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tax & Relief Tools
                if (selectedCategory == ToolCategory.ALL || selectedCategory == ToolCategory.TAX_RELIEF) {
                    AssistChip(
                        onClick = onHicbcClick,
                        label = { Text("HICBC Child Benefit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.ChildCare, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onCompanyCarClick,
                        label = { Text("EV Company Car BiK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.ElectricCar, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onTaxTrapClick,
                        label = { Text("60% Tax Trap", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Rose60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onTaxFreeChildcareClick,
                        label = { Text("Tax-Free Childcare (£2k)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.ChildCare, contentDescription = null, modifier = Modifier.size(14.dp), tint = Teal60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onGiftAidClick,
                        label = { Text("Gift Aid Relief", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
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
                        label = { Text("Tax Refund Estimator", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onTaxComparisonClick,
                        label = { Text("Multi-Year Tax", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Work & Shifts Tools
                if (selectedCategory == ToolCategory.ALL || selectedCategory == ToolCategory.WORK_SHIFTS) {
                    AssistChip(
                        onClick = onCalendarClick,
                        label = { Text("Shift Heatmap", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onOvertimeOptimizerClick,
                        label = { Text("Overtime Optimizer", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onOvertimeBracketClick,
                        label = { Text("Bracket Headroom", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onMultiJobClick,
                        label = { Text("Dual-Job Tax", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onUmbrellaClick,
                        label = { Text("Umbrella / Employer Cost", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onSspHolidayClick,
                        label = { Text("Statutory Sick & Holiday Pay", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onShiftRateDifferentialClick,
                        label = { Text("Shift Differentials (Nights/Weekends)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onStatutoryLeaveClick,
                        label = { Text("Statutory Leave", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Wealth & Goals Tools
                if (selectedCategory == ToolCategory.ALL || selectedCategory == ToolCategory.WEALTH_PLANNING) {
                    AssistChip(
                        onClick = onSandboxClick,
                        label = { Text("What-If Sandbox", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onMultiCurrencyClick,
                        label = { Text("Currency Converter (10 FX)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onSalaryBenchmarkClick,
                        label = { Text("Salary Benchmarking", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onPensionAllowanceClick,
                        label = { Text("Pension Allowance (£60k)", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(14.dp), tint = Teal60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onStudentLoanPayoffClick,
                        label = { Text("Student Loan Payoff", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp), tint = Violet60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onDirectorTaxClick,
                        label = { Text("Director Dividends", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.BusinessCenter, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onCapitalGainsClick,
                        label = { Text("Capital Gains (CGT)", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onMortgageClick,
                        label = { Text("Mortgage Power", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onSelfEmployedClick,
                        label = { Text("Self-Employed Tax", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber60) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onYtdProjectionClick,
                        label = { Text("YTD Projections", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AssistChip(
                        onClick = onPayslipOcrClick,
                        label = { Text("Payslip OCR Scanner", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Shift Schedule Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = daysWorkedInput == "16",
                    onClick = { onSelect("16") },
                    label = { Text("16 Shifts (Care / 4-Day)") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = daysWorkedInput == "14",
                    onClick = { onSelect("14") },
                    label = { Text("14 Shifts (2-Wk Rota)") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = daysWorkedInput == "20",
                    onClick = { onSelect("20") },
                    label = { Text("20 Shifts (Full Month)") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = daysWorkedInput == "21.67",
                    onClick = { onSelect("21.67") },
                    label = { Text("21.7 Shifts (Standard)") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkingHoursCard(
    daysWorkedInput: String,
    onDaysWorkedChange: (String) -> Unit,
    defaultHoursPerDay: Double,
    defaultHourlyRate: Double,
    onQuickShiftChange: (Double) -> Unit = {},
    overtimeHoursInput: String,
    onOvertimeHoursChange: (String) -> Unit,
    selectedOvertimeMultiplier: Double,
    onOvertimeMultiplierChange: (Double) -> Unit,
    bonusInput: String = "",
    onBonusChange: (String) -> Unit = {},
    commissionInput: String = "",
    onCommissionChange: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shifts & Variable Pay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Static shift & hourly rate banner configured in Settings
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Standard Shift & Wage (Static)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "£${"%.2f".format(defaultHourlyRate)}/hr · ${"%.1f".format(defaultHoursPerDay)}h per shift",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        onClick = onNavigateToSettings,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settings", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Quick Shift Presets Chips (FEAT-241)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Quick Shift Length",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        12.0 to "12h Care",
                        8.0 to "8h Standard",
                        7.5 to "7.5h Office",
                        10.0 to "10h Extended"
                    ).forEach { (hrs, label) ->
                        FilterChip(
                            selected = defaultHoursPerDay == hrs,
                            onClick = { onQuickShiftChange(hrs) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Shifts / Days Worked with Quick Steppers
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = daysWorkedInput,
                    onValueChange = onDaysWorkedChange,
                    label = { Text("Shifts / Days Worked This Month") },
                    placeholder = { Text("e.g. 16") },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    trailingIcon = {
                        if (daysWorkedInput.isNotEmpty()) {
                            IconButton(onClick = { onDaysWorkedChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("14", "15", "16", "18", "20", "21.67").forEach { shifts ->
                        SuggestionChip(
                            onClick = { onDaysWorkedChange(shifts) },
                            label = { Text("${shifts} shifts", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Overtime Hours
            OutlinedTextField(
                value = overtimeHoursInput,
                onValueChange = onOvertimeHoursChange,
                label = { Text("Overtime Hours (Optional)") },
                placeholder = { Text("e.g. 5") },
                leadingIcon = { Icon(Icons.Outlined.MoreTime, contentDescription = null) },
                trailingIcon = {
                    if (overtimeHoursInput.isNotEmpty()) {
                        IconButton(onClick = { onOvertimeHoursChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Overtime Multiplier
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Overtime Multiplier",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        1.0 to "1.0x (Standard)",
                        1.25 to "1.25x",
                        1.5 to "1.5x (Weekday)",
                        1.75 to "1.75x",
                        2.0 to "2.0x (Weekend)",
                        2.25 to "2.25x",
                        2.5 to "2.5x (Bank Hol)",
                        3.0 to "3.0x (Triple)"
                    ).forEach { (rate, label) ->
                        FilterChip(
                            selected = selectedOvertimeMultiplier == rate,
                            onClick = { onOvertimeMultiplierChange(rate) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Bonus & Commission
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
        }
    }
}

@Composable
private fun DeductionsAdjustmentsCard(
    pensionRate: Double,
    studentLoanPlan: StudentLoanPlan,
    salarySacrificeInput: String,
    onSalarySacrificeChange: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {}
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deductions & Salary Sacrifice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Static Pension & Student Loan Status Banner configured in Settings
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Workplace Pension & Student Loan (Static)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        val pensionStatus = if (pensionRate <= 0.0) "Opted Out (0%)" else "Enrolled (${"%.1f".format(pensionRate)}%)"
                        val loanStatus = if (studentLoanPlan == StudentLoanPlan.NONE) "No Student Loan" else studentLoanPlan.displayName
                        Text(
                            text = "Pension: $pensionStatus · $loanStatus",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        onClick = onNavigateToSettings,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Salary Sacrifice Pre-Tax Deduction (Dynamic variable monthly input)
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
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${"%.1f".format(totalHours)} hrs total",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                label = if (taxRegion == TaxRegion.SCOTLAND) "Scottish Income Tax" else "PAYE Income Tax",
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
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Multi-Period Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodTile(modifier = Modifier.weight(1f), title = "Hourly", net = report.hourlyNet, gross = ((report.grossPay * 12.0) / 52.0) / 37.5)
                PeriodTile(modifier = Modifier.weight(1f), title = "Weekly", net = report.weeklyNet, gross = (report.grossPay * 12.0) / 52.0)
                PeriodTile(modifier = Modifier.weight(1f), title = "Monthly", net = report.monthlyNet, gross = report.grossPay)
                PeriodTile(modifier = Modifier.weight(1f), title = "Annual", net = report.annualNet, gross = report.grossPay * 12.0)
            }
        }
    }
}

@Composable
private fun PeriodTile(modifier: Modifier = Modifier, title: String, net: Double, gross: Double) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = "£${"%,.0f".format(net)}",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
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
}

@Composable
private fun ActionButtonsRow(
    onSaveClick: () -> Unit,
    onPdfClick: () -> Unit,
    onShareClick: () -> Unit,
    onEmailClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .weight(1.1f)
                .height(48.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Save", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        FilledTonalButton(
            onClick = onPdfClick,
            modifier = Modifier
                .weight(0.9f)
                .height(48.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("PDF", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        FilledTonalButton(
            onClick = onEmailClick,
            modifier = Modifier
                .weight(0.9f)
                .height(48.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Email", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        FilledTonalButton(
            onClick = onShareClick,
            modifier = Modifier
                .weight(0.9f)
                .height(48.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Share", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
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

@Composable
private fun TaxMonthSelectionDialog(
    selectedTaxMonth: Int,
    onSelectMonth: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val months = listOf(
        1 to "April", 2 to "May", 3 to "June", 4 to "July",
        5 to "August", 6 to "September", 7 to "October", 8 to "November",
        9 to "December", 10 to "January", 11 to "February", 12 to "March"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Select Payroll Tax Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HMRC cumulative allowances and thresholds adjust across the tax year (April = Month 1, August = Month 5).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                months.forEach { (m, name) ->
                    val isSelected = selectedTaxMonth == m
                    Surface(
                        onClick = {
                            onSelectMonth(m, "$name 2026")
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$name (Month $m)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                val allowance = if (m == 5) "£1,048.42 (Month 5 cumulative allowance)" else "£1,047.50 monthly allowance"
                                Text(
                                    text = allowance,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
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

