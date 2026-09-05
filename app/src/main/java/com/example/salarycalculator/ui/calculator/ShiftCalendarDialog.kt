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
import androidx.compose.foundation.verticalScroll
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

@Composable
fun ShiftCalendarDialog(
    initialDaysWorked: Double = 20.0,
    initialHoursPerDay: Double = 8.0,
    salaryRepository: SalaryRepository? = null,
    hourlyRate: Double = 15.0,
    overtimeMultiplier: Double = 1.5,
    onApply: (days: Double, hoursPerDay: Double, overtimeHours: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) } // 1..12
    var showYearPicker by remember { mutableStateOf(false) }

    val monthNames = remember { DateFormatSymbols().shortMonths.filter { it.isNotBlank() } }
    val fullMonthNames = remember { DateFormatSymbols().months.filter { it.isNotBlank() } }
    val dayOfWeekLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    // Multi-Year Persistent Shift Store: Key = "$year-$month" -> Map(Day -> Hours)
    val multiYearShifts = remember { mutableStateMapOf<String, MutableMap<Int, Double>>() }

    // Load persisted schedule from DataStore
    LaunchedEffect(Unit) {
        if (salaryRepository != null) {
            salaryRepository.getAnnualShiftSchedule().collect { jsonStr ->
                if (jsonStr.isNotBlank()) {
                    try {
                        val parsed = Json.decodeFromString<Map<String, Map<String, Double>>>(jsonStr)
                        parsed.forEach { (keyStr, dayMap) ->
                            val targetKey = if (keyStr.contains("-")) keyStr else "${cal.get(Calendar.YEAR)}-$keyStr"
                            val targetMap = multiYearShifts.getOrPut(targetKey) { mutableStateMapOf() }
                            dayMap.forEach { (dStr, hrs) ->
                                val d = dStr.toIntOrNull()
                                if (d != null && d in 1..31 && hrs > 0.0) {
                                    targetMap[d] = hrs
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun saveSchedule() {
        if (salaryRepository != null) {
            scope.launch {
                try {
                    val exportMap = multiYearShifts.mapValues { (_, dayMap) ->
                        dayMap.filterValues { it > 0.0 }.mapKeys { it.key.toString() }
                    }.filterValues { it.isNotEmpty() }
                    val jsonStr = Json.encodeToString(exportMap)
                    salaryRepository.setAnnualShiftSchedule(jsonStr)
                } catch (_: Exception) {}
            }
        }
    }

    val currentMonthKey = "$selectedYear-$selectedMonth"
    val currentMonthMap = multiYearShifts.getOrPut(currentMonthKey) { mutableStateMapOf() }

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
        val dow = c.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...
        (dow + 5) % 7
    }

    val payScheduleConfigState = salaryRepository?.getPayScheduleConfig()?.collectAsState(initial = PayScheduleConfig())
    val payScheduleConfig = payScheduleConfigState?.value ?: PayScheduleConfig()

    val payPeriod = remember(selectedYear, selectedMonth, payScheduleConfig) {
        PayScheduleEngine.calculatePayPeriod(selectedYear, selectedMonth, payScheduleConfig)
    }

    val payrollSplit = remember(selectedYear, selectedMonth, currentMonthMap.toMap(), payScheduleConfig) {
        PayScheduleEngine.calculateShiftPayrollSplit(selectedYear, selectedMonth, currentMonthMap, payScheduleConfig)
    }

    val monthDaysWorked = currentMonthMap.values.count { it > 0 }
    val monthTotalHours = currentMonthMap.values.sum()
    val monthStandardHours = currentMonthMap.values.sumOf { minOf(8.0, it) }
    val monthOvertimeHours = currentMonthMap.values.sumOf { maxOf(0.0, it - 8.0) }
    val monthAvgHoursPerDay = if (monthDaysWorked > 0) monthTotalHours / monthDaysWorked else 8.0
    val inCycleAvgHours = if (payrollSplit.inCycleDays > 0) payrollSplit.inCycleHours / payrollSplit.inCycleDays else 8.0

    // Exact Monthly Estimated Gross Calculation (Strict £0.00 zero-state if no shifts)
    val monthEstimatedGross = if (monthDaysWorked == 0 || monthTotalHours == 0.0) {
        0.0
    } else {
        (monthStandardHours * hourlyRate) + (monthOvertimeHours * hourlyRate * overtimeMultiplier)
    }

    val inCycleEstimatedGross = if (payrollSplit.inCycleDays == 0 || payrollSplit.inCycleHours == 0.0) {
        0.0
    } else {
        (payrollSplit.inCycleStandardHours * hourlyRate) + (payrollSplit.inCycleOtHours * hourlyRate * overtimeMultiplier)
    }

    // Annual Aggregate Calculations for selectedYear
    val annualDaysWorked = (1..12).sumOf { m -> multiYearShifts["$selectedYear-$m"]?.values?.count { it > 0 } ?: 0 }
    val annualTotalHours = (1..12).sumOf { m -> multiYearShifts["$selectedYear-$m"]?.values?.sum() ?: 0.0 }
    val annualOvertimeHours = (1..12).sumOf { m -> multiYearShifts["$selectedYear-$m"]?.values?.sumOf { maxOf(0.0, it - 8.0) } ?: 0.0 }
    val annualEstimatedGross = if (annualTotalHours == 0.0) 0.0 else (annualTotalHours * hourlyRate)

    AlertDialog(
        onDismissRequest = {
            saveSchedule()
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Shift Heatmap & Cutoffs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val fullSchedule = (1..12).associateWith { m ->
                                (multiYearShifts["$selectedYear-$m"] ?: emptyMap()).toMap()
                            }
                            val pdf = com.example.salarycalculator.domain.AnnualShiftPdfGenerator.generateAnnualShiftPdf(
                                context = context,
                                year = selectedYear,
                                annualShifts = fullSchedule,
                                hourlyRate = hourlyRate
                            )
                            com.example.salarycalculator.domain.AnnualShiftPdfGenerator.sharePdf(context, pdf)
                        }
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
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export 12-Month .ICS Calendar", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            val ics = IcsCalendarExporter.generatePayScheduleIcsContent(
                                year = selectedYear,
                                config = payScheduleConfig,
                                estimatedNetPay = monthEstimatedGross * 0.8
                            )
                            IcsCalendarExporter.shareIcsFile(context, ics, "Pay_and_Cutoff_Schedule_${selectedYear}.ics")
                        }
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Export Pay & Cutoff Reminders (.ics)", tint = Amber60)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                // 2. Color Legend Row
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Standard 8h
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Emerald60))
                            Text("8h Standard", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        }
                        // Overtime 10h
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Amber60))
                            Text("10h OT", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        }
                        // Long Shift 12h
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Rose60))
                            Text("12h Long/Night", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        }
                        // Part time
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Teal60))
                            Text("<8h Part-Time", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        }
                        // Off
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                text = "Est. Gross: £${"%,.2f".format(monthEstimatedGross)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (monthEstimatedGross > 0) Emerald60 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "In This Payslip: ${payrollSplit.inCycleDays}d (${"%.0f".format(payrollSplit.inCycleHours)}h) · OT: ${"%.0f".format(payrollSplit.inCycleOtHours)}h",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (payrollSplit.inCycleDays > 0) Emerald60 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (payrollSplit.rolloverDays > 0) {
                                Text(
                                    text = "Rollover: +${payrollSplit.rolloverDays}d (${"%.0f".format(payrollSplit.rolloverHours)}h)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Amber60,
                                    fontWeight = FontWeight.Bold
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
                    dayOfWeekLabels.forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

                // 4. Calendar Grid for Selected Month
                val totalCells = startDayOffset + daysInCurrentMonth
                val totalRows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until totalRows) {
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

                                    val (bgColor, textColor, defaultLabel) = when {
                                        hours >= 12.0 -> Triple(Rose60, Color.White, "12h")
                                        hours >= 10.0 -> Triple(Amber60, Color.Black, "10h")
                                        hours >= 8.0 -> Triple(Emerald60, Color.White, "8h")
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
                                        isRollover && hours > 0.0 -> "+Roll"
                                        else -> defaultLabel
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bgColor)
                                            .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(8.dp)) else Modifier)
                                            .clickable {
                                                // Cycle: 0h -> 8h -> 10h -> 12h -> 0h
                                                val next = when (hours) {
                                                    0.0 -> 8.0
                                                    8.0 -> 10.0
                                                    10.0 -> 12.0
                                                    else -> 0.0
                                                }
                                                if (next > 0.0) {
                                                    currentMonthMap[dayNum] = next
                                                } else {
                                                    currentMonthMap.remove(dayNum)
                                                }
                                                saveSchedule()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
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
                        onClick = {
                            // Fill all Monday to Friday with 8h
                            val c = Calendar.getInstance()
                            for (d in 1..daysInCurrentMonth) {
                                c.set(selectedYear, selectedMonth - 1, d)
                                val dow = c.get(Calendar.DAY_OF_WEEK)
                                if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                                    currentMonthMap[d] = 8.0
                                } else {
                                    currentMonthMap.remove(d)
                                }
                            }
                            saveSchedule()
                        },
                        label = { Text("Mon–Fri (8h)", style = MaterialTheme.typography.labelSmall) }
                    )

                    AssistChip(
                        onClick = {
                            // 4 on 4 off pattern
                            for (d in 1..daysInCurrentMonth) {
                                val cycle = ((d - 1) % 8)
                                if (cycle < 4) {
                                    currentMonthMap[d] = 10.0
                                } else {
                                    currentMonthMap.remove(d)
                                }
                            }
                            saveSchedule()
                        },
                        label = { Text("4-On 4-Off", style = MaterialTheme.typography.labelSmall) }
                    )

                    AssistChip(
                        onClick = {
                            // Copy current month pattern to all 12 months for this selectedYear
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
                            saveSchedule()
                        },
                        label = { Text("Copy to All 12 Months", style = MaterialTheme.typography.labelSmall) }
                    )

                    AssistChip(
                        onClick = {
                            currentMonthMap.clear()
                            saveSchedule()
                        },
                        label = { Text("Clear Month", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Annual Full Year Outlook Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Full Year $selectedYear Outlook",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Working Days: $annualDaysWorked days",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Overtime: ${"%.0f".format(annualOvertimeHours)} hrs",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Amber60
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Annual Hours: ${"%.0f".format(annualTotalHours)} hrs",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Est. Gross: £${"%,.2f".format(annualEstimatedGross)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (annualEstimatedGross > 0) Emerald60 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (payrollSplit.rolloverDays > 0) {
                    FilledTonalButton(
                        onClick = {
                            saveSchedule()
                            onApply(monthDaysWorked.toDouble(), monthAvgHoursPerDay, monthOvertimeHours)
                            onDismiss()
                        }
                    ) {
                        Text("Apply All (${monthDaysWorked}d)")
                    }
                }
                Button(
                    onClick = {
                        saveSchedule()
                        if (payrollSplit.rolloverDays > 0) {
                            onApply(payrollSplit.inCycleDays.toDouble(), inCycleAvgHours, payrollSplit.inCycleOtHours)
                        } else {
                            onApply(monthDaysWorked.toDouble(), monthAvgHoursPerDay, monthOvertimeHours)
                        }
                        onDismiss()
                    }
                ) {
                    Text(
                        if (payrollSplit.rolloverDays > 0)
                            "Apply Cutoff (${payrollSplit.inCycleDays}d · ${"%.0f".format(payrollSplit.inCycleHours)}h)"
                        else
                            "Apply Month (${monthDaysWorked}d · ${"%.1f".format(monthAvgHoursPerDay)}h)"
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    saveSchedule()
                    onDismiss()
                }
            ) {
                Text("Close")
            }
        }
    )
}
