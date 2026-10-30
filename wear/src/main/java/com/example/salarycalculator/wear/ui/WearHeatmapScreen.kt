package com.example.salarycalculator.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.salarycalculator.wear.domain.SalaryReport
import com.example.salarycalculator.wear.domain.TaxCalculator
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import com.example.salarycalculator.wear.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
    val haptic = LocalHapticFeedback.current
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    val focusRequester = remember { FocusRequester() }

    val shifts by repository.shiftsFlow.collectAsState(initial = WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS)
    val standardHours by repository.standardShiftHoursFlow.collectAsState(initial = 12.0)
    val hourlyRate by repository.hourlyRateFlow.collectAsState(initial = 12.82)
    val useCutoffCycle by repository.useCutoffCycleFlow.collectAsState(initial = true)

    var salaryReport by remember { mutableStateOf<SalaryReport?>(null) }
    var selectedDayKey by remember { mutableStateOf<String?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var bezelMonthDialMode by remember { mutableStateOf(false) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Background sync with UGOS NAS on launch
        repository.syncFromNasServer()
    }

    LaunchedEffect(currentYearMonth, shifts, hourlyRate, useCutoffCycle) {
        salaryReport = repository.calculateMonthSalary(currentYearMonth, useCutoffCycle)
    }

    val today = remember { LocalDate.now() }
    val todayKey = remember { today.format(DateTimeFormatter.ISO_LOCAL_DATE) }

    val shortMonthName = remember(currentYearMonth) {
        currentYearMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
    }
    val fullMonthName = remember(currentYearMonth) {
        currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    }
    val yearStr = remember(currentYearMonth) { currentYearMonth.year.toString() }

    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfMonth = currentYearMonth.atDay(1)
    val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7 // Monday = 0 .. Sunday = 6
    val (payDay, cutoffDay) = remember(currentYearMonth) {
        TaxCalculator.getPayDayAndCutoff(currentYearMonth)
    }

    val monthPrefix = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    if (showMonthPicker) {
        WearMonthPickerDialog(
            currentYearMonth = currentYearMonth,
            onSelectMonthForHeatmap = { ym ->
                onMonthChange(ym)
                showMonthPicker = false
            },
            onSelectMonthForSalary = { ym ->
                onMonthChange(ym)
                showMonthPicker = false
                onNavigateToSalary(ym)
            },
            onDismiss = { showMonthPicker = false }
        )
        return
    }

    Scaffold(
        timeText = {
            TimeText(
                timeSource = TimeTextDefaults.timeSource(TimeTextDefaults.timeFormat())
            )
        },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .onRotaryScrollEvent { event ->
                    if (bezelMonthDialMode) {
                        rotaryAccumulator += event.verticalScrollPixels
                        if (rotaryAccumulator >= 48f) {
                            rotaryAccumulator = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMonthChange(currentYearMonth.plusMonths(1))
                        } else if (rotaryAccumulator <= -48f) {
                            rotaryAccumulator = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMonthChange(currentYearMonth.minusMonths(1))
                        }
                    } else {
                        coroutineScope.launch {
                            listState.scrollBy(event.verticalScrollPixels)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState,
            anchorType = ScalingLazyListAnchorType.ItemCenter,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 26.dp, bottom = 36.dp, start = 10.dp, end = 10.dp)
        ) {
            // 1. Samsung Watch 8 Classic Circular-Safe Month Header & Picker Trigger
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = 166.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(WearSurface)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onMonthChange(currentYearMonth.minusMonths(1))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◀", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMonthPicker = true
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$shortMonthName $yearStr ▾",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WearTeal
                        )
                        Text(
                            text = "Tap for Month List",
                            fontSize = 8.sp,
                            color = WearTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(WearSurface)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onMonthChange(currentYearMonth.plusMonths(1))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 2. Quick Live Salary & Shifts Pill (1-Tap to Select Month's Salary)
            item {
                val netStr = salaryReport?.let { String.format(Locale.UK, "£%,.2f", it.netPay) } ?: "£0.00"
                val shiftsCount = salaryReport?.shiftCount ?: 0
                val hoursCount = salaryReport?.totalHours?.toInt() ?: 0

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF064E3B))
                        .border(1.dp, WearEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToSalary(currentYearMonth)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = netStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WearEmerald
                    )
                    Text(
                        text = " • $shiftsCount Shifts (${hoursCount}h) ▸",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // 3. Cohesive Circular-Safe 7-Column Heatmap Grid Card (Zero Bezel Clipping)
            item {
                val totalCells = startDayOfWeek + daysInMonth
                val totalWeeks = (totalCells + 6) / 7

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(172.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121217))
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    // Day-of-week header (Mon - Sun)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { idx, day ->
                            Text(
                                text = day,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (idx >= 5) WearAmber.copy(alpha = 0.85f) else WearTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(21.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    for (week in 0 until totalWeeks) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.5.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayOfWeek in 0..6) {
                                val cellIndex = week * 7 + dayOfWeek
                                val dayNumber = cellIndex - startDayOfWeek + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val dateKey = String.format(Locale.ROOT, "%s-%02d", monthPrefix, dayNumber)
                                    val shiftHours = shifts[dateKey] ?: 0.0
                                    val isToday = (dateKey == todayKey)
                                    val isCutoff = (dayNumber == cutoffDay)
                                    val isPayDay = (dayNumber == payDay)

                                    val isAnnualLeave = (shiftHours == TaxCalculator.ANNUAL_LEAVE_SENTINEL)
                                    val isSsp = (shiftHours == TaxCalculator.SSP_SENTINEL)
                                    val isOvertime = (!isAnnualLeave && !isSsp && (shiftHours < 0.0 || shiftHours > standardHours))
                                    val isNightDuty = (shiftHours > 0.0 && shiftHours <= standardHours)

                                    val bgColor = when {
                                        isNightDuty -> WearTeal
                                        isOvertime -> WearAmber
                                        isAnnualLeave -> WearBlue
                                        isSsp -> WearPurple
                                        else -> Color(0xFF232329)
                                    }
                                    val textColor = when {
                                        isNightDuty || isOvertime -> Color.Black
                                        isAnnualLeave || isSsp -> Color.White
                                        isToday -> WearCyan
                                        else -> Color(0xFFD4D4D8)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(21.dp)
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .then(
                                                when {
                                                    isToday -> Modifier.border(1.5.dp, WearCyan, CircleShape)
                                                    isCutoff -> Modifier.border(1.dp, WearRose, CircleShape)
                                                    isPayDay -> Modifier.border(1.dp, WearEmerald, CircleShape)
                                                    else -> Modifier
                                                }
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedDayKey = dateKey
                                                coroutineScope.launch {
                                                    repository.toggleShift(dateKey, standardHours)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = if (shiftHours != 0.0 || isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = textColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(21.dp))
                                }
                            }
                        }
                    }

                    // Mini Legend Row inside Heatmap Card
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(color = WearTeal, label = "Night")
                        LegendDot(color = WearAmber, label = "OT")
                        LegendDot(color = WearBlue, label = "A/L")
                        LegendDot(color = WearRose, label = "Cut:$cutoffDay")
                    }
                }
            }

            // 4. Tapped Day Feedback Banner
            if (selectedDayKey != null) {
                item {
                    val hours = shifts[selectedDayKey] ?: 0.0
                    val statusText = when {
                        hours == TaxCalculator.ANNUAL_LEAVE_SENTINEL -> "Annual Leave (A/L)"
                        hours == TaxCalculator.SSP_SENTINEL -> "Sick Leave (SSP)"
                        hours < 0.0 -> "Overtime (${abs(hours).toInt()}h)"
                        hours > 0.0 -> "Night Duty (${hours.toInt()}h)"
                        else -> "Off / Rest Day"
                    }
                    Text(
                        text = "$selectedDayKey → $statusText",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WearCyan,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            // 5. Primary CTA: View Selected Month's Salary Breakdown
            item {
                Spacer(modifier = Modifier.height(6.dp))
                val netPayText = salaryReport?.let { String.format(Locale.UK, "£%,.2f Net", it.netPay) } ?: "£0.00 Net"
                val modeTag = if (useCutoffCycle) "Payslip Cutoff" else "Full Month"
                Chip(
                    modifier = Modifier
                        .widthIn(max = 172.dp)
                        .fillMaxWidth(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToSalary(currentYearMonth)
                    },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = WearTealDark,
                        contentColor = Color.White
                    ),
                    label = {
                        Text(
                            text = "$shortMonthName Salary Breakdown",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "$netPayText ($modeTag)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WearEmerald
                        )
                    }
                )
            }

            // 6. Select Any Month Salary + Watch 8 Classic Physical Bezel Mode Toggle
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.widthIn(max = 168.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactChip(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMonthPicker = true
                        },
                        label = {
                            Text(
                                text = "📅 Months",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors()
                    )

                    CompactChip(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            bezelMonthDialMode = !bezelMonthDialMode
                        },
                        label = {
                            Text(
                                text = if (bezelMonthDialMode) "🔄 Bezel:Mo" else "🔄 Bezel:Scr",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bezelMonthDialMode) WearCyan else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = label, fontSize = 8.sp, color = WearTextSecondary)
    }
}

/**
 * Circular-Safe 12-Month Quick Picker for Samsung Galaxy Watch 8 Classic.
 * Allows selecting any month (Jan - Dec) and jumping either to its Heatmap or its Salary Breakdown.
 */
@Composable
fun WearMonthPickerDialog(
    currentYearMonth: YearMonth,
    onSelectMonthForHeatmap: (YearMonth) -> Unit,
    onSelectMonthForSalary: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberScalingLazyListState()
    var selectedYear by remember { mutableIntStateOf(currentYearMonth.year) }
    var pickedMonth by remember { mutableIntStateOf(currentYearMonth.monthValue) }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 26.dp, bottom = 32.dp, start = 12.dp, end = 12.dp)
        ) {
            // Year Switcher
            item {
                Row(
                    modifier = Modifier.widthIn(max = 150.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(WearSurface)
                            .clickable { selectedYear-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◀", fontSize = 10.sp, color = Color.White)
                    }
                    Text(
                        text = "SELECT MONTH $selectedYear",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WearTeal
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(WearSurface)
                            .clickable { selectedYear++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            // 3x4 Grid of Months (Jan - Dec)
            val months = Month.entries
            for (row in 0 until 4) {
                item {
                    Row(
                        modifier = Modifier
                            .width(168.dp)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until 3) {
                            val mIndex = row * 3 + col + 1
                            val mName = months[mIndex - 1].getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
                            val isSelected = (mIndex == pickedMonth && selectedYear == currentYearMonth.year) || (mIndex == pickedMonth)

                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) WearTeal else WearSurface)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        pickedMonth = mIndex
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: View Month Salary OR View Month Heatmap
            item {
                val targetYm = YearMonth.of(selectedYear, pickedMonth)
                val targetLabel = targetYm.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    modifier = Modifier.width(168.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectMonthForSalary(targetYm)
                    },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = Color(0xFF065F46),
                        contentColor = Color.White
                    ),
                    label = {
                        Text("💰 View $targetLabel Salary", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                )
            }

            item {
                val targetYm = YearMonth.of(selectedYear, pickedMonth)
                val targetLabel = targetYm.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
                CompactChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectMonthForHeatmap(targetYm)
                    },
                    label = {
                        Text("📅 Open $targetLabel Heatmap", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            item {
                CompactChip(
                    onClick = onDismiss,
                    label = { Text("Cancel", fontSize = 9.sp) },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}
