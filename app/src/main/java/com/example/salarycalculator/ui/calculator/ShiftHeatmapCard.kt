package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.DateFormatSymbols
import java.util.Calendar

/**
 * Embedded Shift Heatmap Card for Home Screen (Default View)
 * Displays interactive monthly shift heatmap, 12h OT calibration, payroll cutoff,
 * and quick rota presets directly on the primary calculator interface.
 */
@Composable
fun ShiftHeatmapCard(
    salaryRepository: SalaryRepository,
    hourlyRate: Double = 15.0,
    standardShiftHours: Double = 12.0,
    overtimeMultiplier: Double = 1.5,
    onApplyToCalculator: ((days: Double, hoursPerDay: Double, overtimeHours: Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) } // 1..12
    var showYearPicker by remember { mutableStateOf(false) }
    var showPatternWizardDialog by remember { mutableStateOf(false) }
    var showDutyRotaImportDialog by remember { mutableStateOf(false) }
    var showShiftSwapDialog by remember { mutableStateOf(false) }
    var showPayslipAuditDialog by remember { mutableStateOf(false) }

    val monthNames = remember { DateFormatSymbols().shortMonths.filter { it.isNotBlank() } }
    val fullMonthNames = remember { DateFormatSymbols().months.filter { it.isNotBlank() } }
    val dayOfWeekLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    val employerProfiles by salaryRepository.getEmployerProfiles().collectAsState(initial = emptyList())
    val shiftAssignments by salaryRepository.getShiftEmployerAssignments().collectAsState(initial = emptyMap())
    val defaultHoursPerDay by salaryRepository.getDefaultHoursPerDay().collectAsState(initial = 12.0)
    val effectiveStandardHours = if (standardShiftHours > 0.0) standardShiftHours else defaultHoursPerDay
    var selectedEmployerFilterId by remember { mutableStateOf<String?>(null) }

    // Multi-Year Persistent Shift Store: Key = "$year-$month" -> Map(Day -> Hours)
    val multiYearShifts = remember { mutableStateMapOf<String, MutableMap<Int, Double>>() }

    // Load persisted schedule from DataStore
    LaunchedEffect(Unit) {
        salaryRepository.getAnnualShiftSchedule().collect { jsonStr ->
            if (jsonStr.isNotBlank()) {
                try {
                    val parsed = Json.decodeFromString<Map<String, Map<String, Double>>>(jsonStr)
                    parsed.forEach { (keyStr, dayMap) ->
                        val targetKey = if (keyStr.contains("-")) keyStr else "${cal.get(Calendar.YEAR)}-$keyStr"
                        val targetMap = multiYearShifts.getOrPut(targetKey) { mutableStateMapOf() }
                        dayMap.forEach { (dStr, hrs) ->
                            val d = dStr.toIntOrNull()
                            if (d != null && d in 1..31 && hrs != 0.0) {
                                targetMap[d] = hrs
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val currentMonthKey = "$selectedYear-$selectedMonth"
    val currentMonthMap = multiYearShifts.getOrPut(currentMonthKey) { mutableStateMapOf() }

    // Previous month reference for post-cutoff rollover calculation
    val prevMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
    val prevYear = if (selectedMonth == 1) selectedYear - 1 else selectedYear
    val prevMonthKey = "$prevYear-$prevMonth"
    val prevMonthMap = multiYearShifts[prevMonthKey] ?: emptyMap()

    // Days in current selected month/year
    val daysInCurrentMonth = remember(selectedYear, selectedMonth) {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, selectedYear)
        c.set(Calendar.MONTH, selectedMonth - 1)
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Day of week for 1st day (0 = Monday, 6 = Sunday)
    val startDayOffset = remember(selectedYear, selectedMonth) {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, selectedYear)
        c.set(Calendar.MONTH, selectedMonth - 1)
        c.set(Calendar.DAY_OF_MONTH, 1)
        val dow = c.get(Calendar.DAY_OF_WEEK)
        (dow + 5) % 7
    }

    val payScheduleConfigState = salaryRepository.getPayScheduleConfig().collectAsState(initial = PayScheduleConfig())
    val payScheduleConfig = payScheduleConfigState.value

    val payPeriod = remember(selectedYear, selectedMonth, payScheduleConfig) {
        PayScheduleEngine.calculatePayPeriod(selectedYear, selectedMonth, payScheduleConfig)
    }

    // Payroll split including rollover
    val payrollSplit = remember(selectedYear, selectedMonth, currentMonthMap.toMap(), prevMonthMap.toMap(), payScheduleConfig, effectiveStandardHours) {
        PayScheduleEngine.calculateShiftPayrollSplit(
            year = selectedYear,
            month = selectedMonth,
            currentMonthShifts = currentMonthMap,
            previousMonthShifts = prevMonthMap,
            config = payScheduleConfig,
            standardHoursPerShift = effectiveStandardHours
        )
    }

    val monthDaysWorked = currentMonthMap.values.count { it != 0.0 }
    val monthTotalHours = currentMonthMap.values.sumOf { kotlin.math.abs(it) }
    val monthStandardHours = currentMonthMap.values.sumOf { if (it < 0.0) 0.0 else minOf(effectiveStandardHours, it) }
    val monthOvertimeHours = currentMonthMap.values.sumOf { if (it < 0.0) kotlin.math.abs(it) else maxOf(0.0, it - effectiveStandardHours) }
    val monthAvgHoursPerDay = if (monthDaysWorked > 0) monthTotalHours / monthDaysWorked else effectiveStandardHours

    val totalPaidStdDays = if (effectiveStandardHours > 0) payrollSplit.totalPaidStandardHours / effectiveStandardHours else payrollSplit.totalPaidDays.toDouble()
    val totalPaidAvgHours = effectiveStandardHours

    val payslipEstimatedGross = if (payrollSplit.totalPaidDays == 0 || payrollSplit.totalPaidHours == 0.0) {
        0.0
    } else {
        (payrollSplit.totalPaidStandardHours * hourlyRate) + (payrollSplit.totalPaidOtHours * hourlyRate * overtimeMultiplier)
    }

    // Care Worker Overtime UK Tax & Take-Home Calculation for this schedule
    val otTaxBreakdown = remember(payslipEstimatedGross, payrollSplit.totalPaidStandardHours, payrollSplit.totalPaidOtHours, hourlyRate, overtimeMultiplier) {
        if (payrollSplit.totalPaidOtHours > 0.0) {
            val stdGross = payrollSplit.totalPaidStandardHours * hourlyRate
            OvertimeOptimizerEngine.calculateCareWorkerOvertimeTaxBreakdown(
                standardGrossMonthly = stdGross,
                baseHourlyRate = hourlyRate,
                overtimeHours = payrollSplit.totalPaidOtHours,
                overtimeMultiplier = overtimeMultiplier
            )
        } else null
    }

    fun saveScheduleAndNotify() {
        scope.launch {
            try {
                val exportMap = multiYearShifts.mapValues { (_, dayMap) ->
                    dayMap.filterValues { it != 0.0 }.mapKeys { it.key.toString() }
                }.filterValues { it.isNotEmpty() }
                val jsonStr = Json.encodeToString(exportMap)
                salaryRepository.setAnnualShiftSchedule(jsonStr)

                // Calculate split fresh with latest state
                val freshSplit = PayScheduleEngine.calculateShiftPayrollSplit(
                    year = selectedYear,
                    month = selectedMonth,
                    currentMonthShifts = currentMonthMap.toMap(),
                    previousMonthShifts = prevMonthMap.toMap(),
                    config = payScheduleConfig,
                    standardHoursPerShift = effectiveStandardHours
                )
                val freshStdDays = if (effectiveStandardHours > 0) freshSplit.totalPaidStandardHours / effectiveStandardHours else freshSplit.totalPaidDays.toDouble()

                // Notify parent calculator of updated payroll parameters:
                onApplyToCalculator?.invoke(
                    freshStdDays,
                    effectiveStandardHours,
                    freshSplit.totalPaidOtHours
                )
            } catch (_: Exception) {}
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title & Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Shift Heatmap & Cutoffs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Default Live Rota View",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { showPayslipAuditDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = "Audit Payslip vs Timesheet (OCR)", tint = Rose60)
                    }

                    IconButton(
                        onClick = { showDutyRotaImportDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan & Import Duty Rota (OCR)", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { showShiftSwapDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Shift Swap Forecaster", tint = Teal60)
                    }

                    IconButton(
                        onClick = { showPatternWizardDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Rotational Pattern Wizard", tint = Emerald60)
                    }

                    IconButton(
                        onClick = {
                            val fullSchedule = (1..12).associateWith { m ->
                                (multiYearShifts["$selectedYear-$m"] ?: emptyMap()).toMap()
                            }
                            val pdf = AnnualShiftPdfGenerator.generateAnnualShiftPdf(
                                context = context,
                                year = selectedYear,
                                annualShifts = fullSchedule,
                                hourlyRate = hourlyRate
                            )
                            AnnualShiftPdfGenerator.sharePdf(context, pdf)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export 12-Month Printable PDF", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            val fullSchedule = (1..12).associateWith { m ->
                                (multiYearShifts["$selectedYear-$m"] ?: emptyMap()).toMap()
                            }
                            val ics = IcsCalendarExporter.generateAnnualIcsContent(
                                year = selectedYear,
                                monthlyShifts = fullSchedule,
                                jobTitle = "Work Shift"
                            )
                            IcsCalendarExporter.shareIcsFile(context, ics, "Annual_Shift_Schedule_${selectedYear}.ics")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export 12-Month .ICS Calendar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 1. Month-by-Month Primary Sequential Navigation Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedMonth > 1) {
                                selectedMonth -= 1
                            } else {
                                selectedMonth = 12
                                selectedYear -= 1
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    // Month & Year Clickable Heading
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { showYearPicker = !showYearPicker }
                    ) {
                        Text(
                            text = "${fullMonthNames.getOrElse(selectedMonth - 1) { "Month $selectedMonth" }} $selectedYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch Year", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            if (selectedMonth < 12) {
                                selectedMonth += 1
                            } else {
                                selectedMonth = 1
                                selectedYear += 1
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }

            // Expandable Quick Year Picker
            AnimatedVisibility(visible = showYearPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (2024..2028).forEach { y ->
                        FilterChip(
                            selected = selectedYear == y,
                            onClick = {
                                selectedYear = y
                                showYearPicker = false
                            },
                            label = { Text("$y", fontWeight = if (selectedYear == y) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // 12-Month Scrollable Chip Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (m in 1..12) {
                    val isSelected = selectedMonth == m
                    val mDaysCount = multiYearShifts["$selectedYear-$m"]?.values?.count { it > 0 } ?: 0
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMonth = m },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = monthNames.getOrElse(m - 1) { "M$m" },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (mDaysCount > 0) {
                                    Text(
                                        text = "${mDaysCount}d",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Emerald60
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Multi-Employer Filter Bar (if multiple profiles exist)
            if (employerProfiles.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedEmployerFilterId == null,
                        onClick = { selectedEmployerFilterId = null },
                        label = { Text("All Employers (${employerProfiles.size})", style = MaterialTheme.typography.labelSmall) }
                    )
                    employerProfiles.forEach { profile ->
                        val isSelected = selectedEmployerFilterId == profile.id
                        val pColor = try {
                            Color(android.graphics.Color.parseColor(profile.colorHex))
                        } catch (_: Exception) { Teal60 }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedEmployerFilterId = if (isSelected) null else profile.id },
                            label = { Text(profile.name.ifBlank { "Employer" }, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(pColor))
                            }
                        )
                    }
                }
            }

            // 2. Color Legend Row (Calibrated dynamically to configured standard shift duration)
            val stdHoursLabel = if (effectiveStandardHours % 1.0 == 0.0) "${effectiveStandardHours.toInt()}" else "%.1f".format(effectiveStandardHours)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Standard Shift
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Emerald60))
                        Text("${stdHoursLabel}h Standard", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                    // Overtime Shift
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Amber60))
                        Text("${stdHoursLabel}h OT", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    // Part-time < Standard
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Teal60))
                        Text("<${stdHoursLabel}h Part-Time", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                    // Day Off
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                        Text("0h Off", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                }
            }

            // 3. Payroll Cutoff & Pay Cycle Banner Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Amber60.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "⏰ Cutoff: Sun ${payPeriod.cutoffDay} ${monthNames.getOrElse(selectedMonth - 1) { "" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber60,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Emerald60.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "💰 Pay: Fri ${payPeriod.payDay} ${monthNames.getOrElse(selectedMonth - 1) { "" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald60,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Payslip Gross: £${"%,.2f".format(payslipEstimatedGross)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (payslipEstimatedGross > 0) Emerald60 else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Care Worker Overtime UK Tax & Take-Home Preview Banner
                    if (otTaxBreakdown != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Amber60.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ Overtime (${"%.0f".format(otTaxBreakdown.overtimeHours)}h): +£${"%.2f".format(otTaxBreakdown.grossOvertimePay)} gross",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Take-Home: +£${"%.2f".format(otTaxBreakdown.netOvertimePay)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald60
                                    )
                                }
                                Text(
                                    text = "↳ UK Marginal Deductions: -£${"%.2f".format(otTaxBreakdown.payeTaxOnOt)} Tax (20%) · -£${"%.2f".format(otTaxBreakdown.niOnOt)} NI (8%) · Retain ${"%.0f".format(otTaxBreakdown.retentionPercentage)}% in hand (£${"%.2f".format(otTaxBreakdown.netPerHour)}/hr net · £${"%.2f".format(otTaxBreakdown.netPer12hShift)}/12h shift)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Detailed In-Payslip Partitioning
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Paid in This Payslip: ${payrollSplit.totalPaidDays}d (${"%.0f".format(payrollSplit.totalPaidHours)}h) · OT: ${"%.0f".format(payrollSplit.totalPaidOtHours)}h",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (payrollSplit.totalPaidDays > 0) Emerald60 else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (payrollSplit.previousRolloverDays > 0) {
                            Text(
                                text = "↳ Includes +${payrollSplit.previousRolloverDays}d (${"%.0f".format(payrollSplit.previousRolloverHours)}h) rolled over from ${monthNames.getOrElse(prevMonth - 1) { "" }} post-cutoff",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald60,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (payrollSplit.rolloverDays > 0) {
                            Text(
                                text = "↳ +${payrollSplit.rolloverDays}d (${"%.0f".format(payrollSplit.rolloverHours)}h) logged after Sun ${payPeriod.cutoffDay} will roll over to ${monthNames.getOrElse((selectedMonth % 12)) { "" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber60,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Day-of-week headers (M, T, W, T, F, S, S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayOfWeekLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 7-Column Interactive Shift Calendar Grid
            val numRows = (startDayOffset + daysInCurrentMonth + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until numRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startDayOffset + 1

                            if (dayNum in 1..daysInCurrentMonth) {
                                val hours = currentMonthMap[dayNum] ?: 0.0
                                val isCutoff = (dayNum == payPeriod.cutoffDay && selectedMonth == payPeriod.cutoffMonth && selectedYear == payPeriod.cutoffYear)
                                val isPayday = (dayNum == payPeriod.payDay && selectedMonth == payPeriod.payMonth && selectedYear == payPeriod.payYear)
                                val isRollover = (dayNum > payPeriod.cutoffDay && selectedMonth == payPeriod.cutoffMonth && selectedYear == payPeriod.cutoffYear)

                                val isOtShift = hours < 0.0
                                val absHours = kotlin.math.abs(hours)

                                val (bgColor, textColor, defaultLabel) = when {
                                    isOtShift -> Triple(Amber60, Color.Black, "${absHours.toInt()}h OT")
                                    hours > effectiveStandardHours -> Triple(Amber60, Color.Black, "${hours.toInt()}h OT")
                                    hours >= effectiveStandardHours -> Triple(Emerald60, Color.White, "${hours.toInt()}h")
                                    hours > 0.0 -> Triple(Teal60, Color.White, "${hours.toInt()}h")
                                    else -> Triple(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), MaterialTheme.colorScheme.onSurface, "")
                                }

                                val cellBorder = when {
                                    isPayday -> BorderStroke(2.dp, Emerald60)
                                    isCutoff -> BorderStroke(2.dp, Amber60)
                                    else -> null
                                }

                                val cellLabel = when {
                                    isCutoff && hours == 0.0 -> "Cutoff"
                                    isPayday && hours == 0.0 -> "Payday"
                                    isRollover && hours != 0.0 -> "+Roll"
                                    else -> defaultLabel
                                }

                                val assignedEmployerId = shiftAssignments["$selectedYear-$selectedMonth-$dayNum"]
                                val assignedProfile = employerProfiles.find { it.id == assignedEmployerId }
                                val profileColor = if (assignedProfile != null) {
                                    try { Color(android.graphics.Color.parseColor(assignedProfile.colorHex)) } catch (_: Exception) { null }
                                } else null

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(8.dp)) else Modifier)
                                        .clickable {
                                            // Dynamic Calibrated Cycle:
                                            // 0h -> Standard Shift (effectiveStandardHours) -> Overtime Shift (-effectiveStandardHours) -> 0h
                                            val next = when {
                                                hours == 0.0 -> effectiveStandardHours
                                                hours == effectiveStandardHours -> -effectiveStandardHours
                                                else -> 0.0
                                            }
                                            if (next != 0.0) {
                                                currentMonthMap[dayNum] = next
                                                if (selectedEmployerFilterId != null) {
                                                    scope.launch {
                                                        salaryRepository.setShiftEmployerAssignment("$selectedYear-$selectedMonth-$dayNum", selectedEmployerFilterId!!)
                                                    }
                                                }
                                            } else {
                                                currentMonthMap.remove(dayNum)
                                            }
                                            saveScheduleAndNotify()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileColor != null && hours != 0.0) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .align(Alignment.TopEnd)
                                                .padding(top = 2.dp, end = 2.dp)
                                                .clip(CircleShape)
                                                .background(profileColor)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cellLabel == "Cutoff") Amber60 else if (cellLabel == "Payday") Emerald60 else textColor,
                                            fontSize = 11.sp
                                        )
                                        if (cellLabel.isNotEmpty()) {
                                            Text(
                                                text = cellLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (cellLabel == "Cutoff") Amber60 else if (cellLabel == "Payday") Emerald60 else if (cellLabel == "+Roll") Amber60 else textColor
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.size(38.dp))
                            }
                        }
                    }
                }
            }

            // Month Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { showPatternWizardDialog = true },
                    label = { Text("Pattern Wizard", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) }
                )

                AssistChip(
                    onClick = {
                        // Fill Mon-Fri with configured standard shift hours (e.g. 12h)
                        val c = Calendar.getInstance()
                        for (d in 1..daysInCurrentMonth) {
                            c.set(selectedYear, selectedMonth - 1, d)
                            val dow = c.get(Calendar.DAY_OF_WEEK)
                            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                                currentMonthMap[d] = effectiveStandardHours
                            } else {
                                currentMonthMap.remove(d)
                            }
                        }
                        saveScheduleAndNotify()
                    },
                    label = { Text("Mon–Fri (${stdHoursLabel}h)", style = MaterialTheme.typography.labelSmall) }
                )

                AssistChip(
                    onClick = {
                        // 4 on 4 off pattern calibrated to configured shift duration (e.g. 12.0h Care shifts)
                        for (d in 1..daysInCurrentMonth) {
                            val cycle = ((d - 1) % 8)
                            if (cycle < 4) {
                                currentMonthMap[d] = effectiveStandardHours
                            } else {
                                currentMonthMap.remove(d)
                            }
                        }
                        saveScheduleAndNotify()
                    },
                    label = { Text("4-On 4-Off (${stdHoursLabel}h)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )

                // If existing month has 8h shifts but settings has 12h, provide 1-tap conversion chip
                val hasLegacy8hShifts = currentMonthMap.values.any { it == 8.0 } && effectiveStandardHours != 8.0
                if (hasLegacy8hShifts) {
                    AssistChip(
                        onClick = {
                            currentMonthMap.keys.toList().forEach { d ->
                                if (currentMonthMap[d] == 8.0) {
                                    currentMonthMap[d] = effectiveStandardHours
                                }
                            }
                            saveScheduleAndNotify()
                        },
                        label = { Text("⚡ Convert 8h to ${stdHoursLabel}h", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald60) }
                    )
                }

                AssistChip(
                    onClick = {
                        val template = currentMonthMap.toMap()
                        for (m in 1..12) {
                            if (m != selectedMonth) {
                                val target = multiYearShifts.getOrPut("$selectedYear-$m") { mutableStateMapOf() }
                                target.clear()
                                template.forEach { (d, h) ->
                                    if (d <= 28) target[d] = h
                                }
                            }
                        }
                        saveScheduleAndNotify()
                    },
                    label = { Text("Copy to All 12 Months", style = MaterialTheme.typography.labelSmall) }
                )

                AssistChip(
                    onClick = {
                        currentMonthMap.clear()
                        saveScheduleAndNotify()
                    },
                    label = { Text("Clear Month", style = MaterialTheme.typography.labelSmall) }
                )
            }

            Button(
                onClick = { saveScheduleAndNotify() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Rota to Calculator (${payrollSplit.totalPaidDays} shifts • £${"%,.2f".format(payslipEstimatedGross)})")
            }
        }
    }

    // Rotational Pattern Generator Wizard Dialog
    if (showPatternWizardDialog) {
        ShiftPatternGeneratorDialog(
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            hourlyRate = hourlyRate,
            onApplyPattern = { generatedShifts ->
                generatedShifts.forEach { (ymKey, dayMap) ->
                    val target = multiYearShifts.getOrPut(ymKey) { mutableStateMapOf() }
                    target.clear()
                    dayMap.forEach { (d, h) ->
                        if (h != 0.0) target[d] = h
                    }
                }
                saveScheduleAndNotify()
            },
            onDismiss = { showPatternWizardDialog = false }
        )
    }

    // Duty Rota AI & Staff Roster Importer Dialog
    if (showDutyRotaImportDialog) {
        DutyRotaImportDialog(
            hourlyRate = hourlyRate,
            standardShiftHours = effectiveStandardHours,
            onDismiss = { showDutyRotaImportDialog = false },
            onApplyRotaSchedule = { rotaYear, rotaMonth, rotaShifts, _ ->
                selectedYear = rotaYear
                selectedMonth = rotaMonth
                val targetKey = "$rotaYear-$rotaMonth"
                val targetMap = multiYearShifts.getOrPut(targetKey) { mutableStateMapOf() }
                targetMap.clear()
                rotaShifts.forEach { (d, h) ->
                    targetMap[d] = h
                }
                saveScheduleAndNotify()
            }
        )
    }

    // Shift Swap & Coverage Forecaster Dialog
    if (showShiftSwapDialog) {
        val totalHours = currentMonthMap.values.sumOf { kotlin.math.abs(it) }
        ShiftSwapForecasterDialog(
            hourlyRate = hourlyRate,
            standardShiftHours = effectiveStandardHours,
            currentMonthlyGross = (totalHours * hourlyRate),
            onDismiss = { showShiftSwapDialog = false },
            onApplySwapToSchedule = { swapDay, appliedHours, _ ->
                if (appliedHours != 0.0) {
                    currentMonthMap[swapDay] = appliedHours
                } else {
                    currentMonthMap.remove(swapDay)
                }
                saveScheduleAndNotify()
            }
        )
    }

    // Payslip Timesheet Auditor & Discrepancy Emailer Dialog
    if (showPayslipAuditDialog) {
        PayslipAuditDialog(
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            allMultiYearShifts = multiYearShifts.mapValues { it.value.toMap() },
            configuredHourlyRate = hourlyRate,
            standardShiftHours = effectiveStandardHours,
            payScheduleConfig = payScheduleConfig,
            onDismiss = { showPayslipAuditDialog = false },
            onUpdateMonthShifts = { yr, mo, updatedMap ->
                val targetKey = "$yr-$mo"
                val targetMap = multiYearShifts.getOrPut(targetKey) { mutableStateMapOf() }
                targetMap.clear()
                updatedMap.forEach { (d, h) ->
                    targetMap[d] = h
                }
                saveScheduleAndNotify()
            }
        )
    }
}
