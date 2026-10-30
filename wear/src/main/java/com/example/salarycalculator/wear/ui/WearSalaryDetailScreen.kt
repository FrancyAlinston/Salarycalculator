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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.salarycalculator.wear.domain.SalaryReport
import com.example.salarycalculator.wear.domain.TaxCalculator
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import com.example.salarycalculator.wear.theme.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WearSalaryDetailScreen(
    repository: WearSalaryRepository,
    selectedYearMonth: YearMonth,
    onMonthSelect: (YearMonth) -> Unit,
    onBackToHeatmap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    val hourlyRate by repository.hourlyRateFlow.collectAsState(initial = 12.82)
    val taxCode by repository.taxCodeFlow.collectAsState(initial = "1257L")
    val shifts by repository.shiftsFlow.collectAsState(initial = WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS)
    val useCutoffCycle by repository.useCutoffCycleFlow.collectAsState(initial = true)

    var salaryReport by remember { mutableStateOf<SalaryReport?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(selectedYearMonth, shifts, hourlyRate, taxCode, useCutoffCycle) {
        salaryReport = repository.calculateMonthSalary(selectedYearMonth, useCutoffCycle)
    }

    val shortMonthName = remember(selectedYearMonth) {
        selectedYearMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
    }
    val fullMonthName = remember(selectedYearMonth) {
        selectedYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    }
    val yearStr = remember(selectedYearMonth) { selectedYearMonth.year.toString() }

    val (payDayStr, cutoffStr) = remember(selectedYearMonth) {
        val (payDay, cutoffDay) = TaxCalculator.getPayDayAndCutoff(selectedYearMonth)
        val payDate = selectedYearMonth.atDay(payDay)
        val cutoffDate = selectedYearMonth.atDay(cutoffDay)
        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
        Pair(payDate.format(formatter), cutoffDate.format(formatter))
    }

    if (showMonthPicker) {
        WearMonthPickerDialog(
            currentYearMonth = selectedYearMonth,
            onSelectMonthForHeatmap = { ym ->
                onMonthSelect(ym)
                showMonthPicker = false
                onBackToHeatmap()
            },
            onSelectMonthForSalary = { ym ->
                onMonthSelect(ym)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
        return
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .onRotaryScrollEvent { event ->
                    coroutineScope.launch {
                        listState.scrollBy(event.verticalScrollPixels)
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 26.dp, bottom = 36.dp, start = 10.dp, end = 10.dp)
        ) {
            // 1. Circular-Safe Month Switcher & 12-Month Picker Trigger
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
                                onMonthSelect(selectedYearMonth.minusMonths(1))
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
                            text = "$fullMonthName ▾",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WearTeal
                        )
                        Text(
                            text = "$yearStr • Tap to Pick Month",
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
                                onMonthSelect(selectedYearMonth.plusMonths(1))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 2. Cutoff Cycle vs Full Calendar Month Mode Pill
            item {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier
                        .widthIn(max = 168.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WearSurface)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                repository.setUseCutoffCycle(!useCutoffCycle)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (useCutoffCycle) "Mode: Payslip Cutoff ⟳" else "Mode: Full Month (1–End) ⟳",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (useCutoffCycle) WearCyan else WearAmber
                    )
                }
            }

            // 3. Hero Net Take-Home Pay Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = { showMonthPicker = true },
                    modifier = Modifier.widthIn(max = 176.dp).fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF064E3B),
                        endBackgroundColor = Color(0xFF042F2E)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "$shortMonthName $yearStr TAKE-HOME",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA7F3D0)
                        )
                        Text(
                            text = salaryReport?.let { String.format(Locale.UK, "£%,.2f", it.netPay) } ?: "£0.00",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = WearEmerald
                        )
                        Text(
                            text = "${salaryReport?.shiftCount ?: 0} Paid Shifts • ${salaryReport?.totalHours?.toInt() ?: 0} hrs",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // 4. Gross Earnings & Shift Composition Card
            item {
                val split = salaryReport?.payrollSplit
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = {},
                    modifier = Modifier.widthIn(max = 176.dp).fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = WearSurface,
                        endBackgroundColor = WearSurfaceDark
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gross Pay", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                text = salaryReport?.let { String.format(Locale.UK, "£%,.2f", it.grossPay) } ?: "£0.00",
                                fontSize = 10.sp,
                                color = WearTeal,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        if (useCutoffCycle && split != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("In-Cycle (≤${split.cutoffDay})", fontSize = 9.sp, color = WearTextSecondary)
                                Text(
                                    text = "${split.inCycleStandardShifts + split.inCycleOtShifts + split.inCycleAnnualLeaveDays} shifts",
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                            if (split.rolloverInShifts > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Prev Cutoff Rollover", fontSize = 9.sp, color = WearTextSecondary)
                                    Text(
                                        text = "+${split.rolloverInShifts} shifts (${(split.rolloverInStandardHours + split.rolloverInOtHours).toInt()}h)",
                                        fontSize = 9.sp,
                                        color = WearCyan
                                    )
                                }
                            }
                            if (split.totalPaidAnnualLeaveHours > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Annual Leave (A/L)", fontSize = 9.sp, color = WearTextSecondary)
                                    Text(
                                        text = String.format(Locale.UK, "£%,.2f", salaryReport?.annualLeavePay ?: 0.0),
                                        fontSize = 9.sp,
                                        color = WearBlue
                                    )
                                }
                            }
                            if (split.postCutoffRolloverOutShifts > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Next Month Rollover", fontSize = 9.sp, color = WearTextSecondary)
                                    Text(
                                        text = "${split.postCutoffRolloverOutShifts} shifts →",
                                        fontSize = 9.sp,
                                        color = WearAmber
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Hours", fontSize = 9.sp, color = WearTextSecondary)
                                Text(
                                    text = "${salaryReport?.totalHours?.toInt() ?: 0}h @ £${String.format(Locale.UK, "%.2f", hourlyRate)}/h",
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 5. Deductions Breakdown Card (PAYE Tax & Class 1 NI)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = {},
                    modifier = Modifier.widthIn(max = 176.dp).fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = WearSurface,
                        endBackgroundColor = WearSurfaceDark
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "UK Tax & NI ($taxCode)",
                            fontSize = 9.sp,
                            color = WearTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PAYE Income Tax", fontSize = 9.sp, color = Color.LightGray)
                            Text(
                                text = salaryReport?.let { String.format(Locale.UK, "-£%,.2f", it.incomeTax) } ?: "£0.00",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF87171)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Class 1 NI (8%)", fontSize = 9.sp, color = Color.LightGray)
                            Text(
                                text = salaryReport?.let { String.format(Locale.UK, "-£%,.2f", it.nationalInsurance) } ?: "£0.00",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF87171)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Deductions", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                text = salaryReport?.let { String.format(Locale.UK, "-£%,.2f", it.totalDeductions) } ?: "£0.00",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = WearRose
                            )
                        }
                    }
                }
            }

            // 6. Pay Day & Timesheet Cutoff Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = {},
                    modifier = Modifier.widthIn(max = 176.dp).fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = WearSurface,
                        endBackgroundColor = WearSurfaceDark
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pay Day", fontSize = 9.sp, color = Color.LightGray)
                            Text(payDayStr, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WearEmerald)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cutoff Sun", fontSize = 9.sp, color = Color.LightGray)
                            Text(cutoffStr, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WearAmber)
                        }
                    }
                }
            }

            // 7. Quick On-Wrist Rate Adjuster (-£0.10 / +£0.10)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.widthIn(max = 166.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch {
                                repository.setHourlyRate(maxOf(5.0, hourlyRate - 0.10))
                            }
                        },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("-10p", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = String.format(Locale.UK, "£%.2f/hr", hourlyRate),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    CompactButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch {
                                repository.setHourlyRate(hourlyRate + 0.10)
                            }
                        },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("+10p", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 8. Bottom Navigation & NAS Sync Actions
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.widthIn(max = 168.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactChip(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBackToHeatmap()
                        },
                        label = {
                            Text("◀ Heatmap", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        },
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = WearTealDark,
                            contentColor = Color.White
                        )
                    )

                    CompactChip(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                syncStatusText = "Syncing..."
                                val ok = repository.syncFromNasServer()
                                syncStatusText = if (ok) "NAS Synced ✓" else "Local Rota ✓"
                            }
                        },
                        label = {
                            Text(syncStatusText ?: "🔄 Sync NAS", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
            }
        }
    }
}
