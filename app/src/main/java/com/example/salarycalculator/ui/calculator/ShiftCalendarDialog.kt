package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.example.salarycalculator.domain.IcsCalendarExporter
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.DateFormatSymbols
import java.util.Calendar

@Composable
fun ShiftCalendarDialog(
    initialDaysWorked: Double,
    initialHoursPerDay: Double,
    salaryRepository: SalaryRepository? = null,
    hourlyRate: Double = 15.0,
    onApply: (days: Double, hoursPerDay: Double, overtimeHours: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) } // 1..12

    val monthNames = remember { DateFormatSymbols().shortMonths.filter { it.isNotBlank() } }
    val dayOfWeekLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    // Map: Month (1..12) -> Map(Day -> Hours)
    val annualShifts = remember {
        mutableStateMapOf<Int, MutableMap<Int, Double>>().apply {
            for (m in 1..12) {
                put(m, mutableStateMapOf())
            }
            // Seed current month
            val currentMonthMap = get(selectedMonth)!!
            val initialCount = initialDaysWorked.toInt().coerceIn(0, 31)
            for (d in 1..initialCount) {
                currentMonthMap[d] = initialHoursPerDay
            }
        }
    }

    // Try loading persisted annual schedule if available
    LaunchedEffect(Unit) {
        if (salaryRepository != null) {
            salaryRepository.getAnnualShiftSchedule().collect { jsonStr ->
                if (jsonStr.isNotBlank()) {
                    try {
                        val parsed = Json.decodeFromString<Map<String, Map<String, Double>>>(jsonStr)
                        parsed.forEach { (mStr, dayMap) ->
                            val m = mStr.toIntOrNull()
                            if (m != null && m in 1..12) {
                                val targetMap = annualShifts.getOrPut(m) { mutableStateMapOf() }
                                dayMap.forEach { (dStr, hrs) ->
                                    val d = dStr.toIntOrNull()
                                    if (d != null && d in 1..31) {
                                        targetMap[d] = hrs
                                    }
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
                    val exportMap = annualShifts.mapKeys { it.key.toString() }.mapValues { (_, dayMap) ->
                        dayMap.mapKeys { it.key.toString() }
                    }
                    val jsonStr = Json.encodeToString(exportMap)
                    salaryRepository.setAnnualShiftSchedule(jsonStr)
                } catch (_: Exception) {}
            }
        }
    }

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

    val currentMonthMap = annualShifts.getOrPut(selectedMonth) { mutableStateMapOf() }

    val monthDaysWorked = currentMonthMap.values.count { it > 0 }
    val monthTotalHours = currentMonthMap.values.sum()
    val monthStandardHours = currentMonthMap.values.sumOf { minOf(8.0, it) }
    val monthOvertimeHours = currentMonthMap.values.sumOf { maxOf(0.0, it - 8.0) }
    val monthAvgHoursPerDay = if (monthDaysWorked > 0) monthTotalHours / monthDaysWorked else 8.0

    // Annual Aggregate Calculations
    val annualDaysWorked = annualShifts.values.sumOf { m -> m.values.count { it > 0 } }
    val annualTotalHours = annualShifts.values.sumOf { m -> m.values.sum() }
    val annualOvertimeHours = annualShifts.values.sumOf { m -> m.values.sumOf { maxOf(0.0, it - 8.0) } }
    val annualEstimatedGross = annualTotalHours * hourlyRate

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
                    Text("Annual Shift Heatmap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = {
                        val fullSchedule = annualShifts.mapValues { it.value.toMap() }
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
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Year Switcher Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedYear -= 1 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Year", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "Year $selectedYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { selectedYear += 1 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Year", modifier = Modifier.size(18.dp))
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
                        val mDaysCount = annualShifts[m]?.values?.count { it > 0 } ?: 0
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

                // Monthly Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear Summary",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$monthDaysWorked days · ${"%.0f".format(monthStandardHours)}h standard · ${"%.0f".format(monthOvertimeHours)}h OT",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${"%.0f".format(monthTotalHours)} hrs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Emerald60
                            )
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

                // Calendar Grid for Selected Month
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
                                    val (bgColor, textColor, label) = when {
                                        hours >= 12.0 -> Triple(Rose60, Color.White, "12h")
                                        hours >= 10.0 -> Triple(Amber60, Color.Black, "10h")
                                        hours >= 8.0 -> Triple(Emerald60, Color.White, "8h")
                                        hours > 0.0 -> Triple(Teal60, Color.White, "${hours.toInt()}h")
                                        else -> Triple(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), MaterialTheme.colorScheme.onSurface, "")
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bgColor)
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
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                fontSize = 11.sp
                                            )
                                            if (label.isNotEmpty()) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 8.sp,
                                                    color = textColor
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
                        },
                        label = { Text("4-On 4-Off", style = MaterialTheme.typography.labelSmall) }
                    )

                    AssistChip(
                        onClick = {
                            // Copy current month pattern to all 12 months
                            val template = currentMonthMap.toMap()
                            for (m in 1..12) {
                                if (m != selectedMonth) {
                                    val target = annualShifts.getOrPut(m) { mutableStateMapOf() }
                                    target.clear()
                                    template.forEach { (d, h) ->
                                        if (d <= 28) target[d] = h
                                    }
                                }
                            }
                        },
                        label = { Text("Copy to All 12 Months", style = MaterialTheme.typography.labelSmall) }
                    )

                    AssistChip(
                        onClick = { currentMonthMap.clear() },
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
                                text = "Est. Gross: £${"%,.0f".format(annualEstimatedGross)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Emerald60
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    saveSchedule()
                    onApply(monthDaysWorked.toDouble(), monthAvgHoursPerDay, monthOvertimeHours)
                    onDismiss()
                }
            ) {
                Text("Apply Month (${monthDaysWorked}d · ${"%.1f".format(monthAvgHoursPerDay)}h)")
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
