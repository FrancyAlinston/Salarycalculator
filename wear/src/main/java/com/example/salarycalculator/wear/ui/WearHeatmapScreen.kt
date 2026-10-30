package com.example.salarycalculator.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.salarycalculator.wear.domain.SalaryReport
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import com.example.salarycalculator.wear.theme.WearAmber
import com.example.salarycalculator.wear.theme.WearEmerald
import com.example.salarycalculator.wear.theme.WearTeal
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun WearHeatmapScreen(
    repository: WearSalaryRepository,
    currentYearMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onNavigateToSalary: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    val shifts by repository.shiftsFlow.collectAsState(initial = emptyMap())
    val standardHours by repository.standardShiftHoursFlow.collectAsState(initial = 12.0)
    val hourlyRate by repository.hourlyRateFlow.collectAsState(initial = 16.25)

    var salaryReport by remember { mutableStateOf<SalaryReport?>(null) }
    var selectedDayKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentYearMonth, shifts, hourlyRate) {
        salaryReport = repository.calculateMonthSalary(currentYearMonth)
    }

    val today = remember { LocalDate.now() }
    val todayKey = remember { today.format(DateTimeFormatter.ISO_LOCAL_DATE) }

    val monthName = remember(currentYearMonth) {
        currentYearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH).uppercase()
    }
    val yearStr = remember(currentYearMonth) { currentYearMonth.year.toString() }

    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfMonth = currentYearMonth.atDay(1)
    val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7 // Monday = 0 .. Sunday = 6

    val monthPrefix = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val (nightCount, otCount, totalHours) = remember(shifts, currentYearMonth) {
        var nights = 0
        var ots = 0
        var hours = 0.0
        shifts.forEach { (date, h) ->
            if (date.startsWith(monthPrefix) && h != 0.0) {
                if (h < 0.0) ots++ else nights++
                hours += abs(h)
            }
        }
        Triple(nights, ots, hours)
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 28.dp, bottom = 32.dp, start = 8.dp, end = 8.dp)
        ) {
            // Month Header with Prev/Next buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactButton(
                        onClick = { onMonthChange(currentYearMonth.minusMonths(1)) },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("<", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = monthName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WearTeal
                        )
                        Text(
                            text = yearStr,
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }

                    CompactButton(
                        onClick = { onMonthChange(currentYearMonth.plusMonths(1)) },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text(">", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Stats Badge
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E24))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(WearTeal))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$nightCount Nights",
                        fontSize = 10.sp,
                        color = Color.White
                    )
                    if (otCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(WearAmber))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$otCount OT",
                            fontSize = 10.sp,
                            color = WearAmber
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${totalHours.toInt()}h",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            // Day of Week Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA1A1AA),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(22.dp)
                        )
                    }
                }
            }

            // Heatmap Calendar Grid Weeks
            val totalCells = startDayOfWeek + daysInMonth
            val totalWeeks = (totalCells + 6) / 7

            for (week in 0 until totalWeeks) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (dayOfWeek in 0..6) {
                            val cellIndex = week * 7 + dayOfWeek
                            val dayNumber = cellIndex - startDayOfWeek + 1

                            if (dayNumber in 1..daysInMonth) {
                                val dateKey = String.format(Locale.ROOT, "%s-%02d", monthPrefix, dayNumber)
                                val shiftHours = shifts[dateKey] ?: 0.0
                                val isToday = (dateKey == todayKey)
                                val isNight = shiftHours > 0.0
                                val isOvertime = shiftHours < 0.0

                                val bgColor = when {
                                    isNight -> WearTeal
                                    isOvertime -> WearAmber
                                    else -> Color(0xFF222226)
                                }
                                val textColor = when {
                                    isNight || isOvertime -> Color.Black
                                    else -> Color.White
                                }

                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .then(
                                            if (isToday) Modifier.border(1.5.dp, Color.White, CircleShape)
                                            else Modifier
                                        )
                                        .clickable {
                                            selectedDayKey = dateKey
                                            coroutineScope.launch {
                                                repository.toggleShift(dateKey, standardHours)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = if (isNight || isOvertime || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            // Selected Day Feedback Tip
            if (selectedDayKey != null) {
                item {
                    val hours = shifts[selectedDayKey] ?: 0.0
                    val statusText = when {
                        hours > 0.0 -> "Night Duty (12h)"
                        hours < 0.0 -> "Overtime (12h)"
                        else -> "Off / Rest Day"
                    }
                    Text(
                        text = "$selectedDayKey: $statusText",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Primary Action: View Month Salary Chip
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val netPayText = salaryReport?.let { String.format(Locale.UK, "£%,.2f Net", it.netPay) } ?: "Calculating..."
                Chip(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    onClick = { onNavigateToSalary(currentYearMonth) },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = Color(0xFF0F766E),
                        contentColor = Color.White
                    ),
                    label = {
                        Text(
                            text = "View $monthName Salary",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = netPayText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WearEmerald
                        )
                    }
                )
            }
        }
    }
}
