package com.example.salarycalculator.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.salarycalculator.wear.theme.WearEmerald
import com.example.salarycalculator.wear.theme.WearTeal
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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
    val listState = rememberScalingLazyListState()
    val hourlyRate by repository.hourlyRateFlow.collectAsState(initial = 16.25)
    val taxCode by repository.taxCodeFlow.collectAsState(initial = "1257L")
    val shifts by repository.shiftsFlow.collectAsState(initial = emptyMap())

    var salaryReport by remember { mutableStateOf<SalaryReport?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedYearMonth, shifts, hourlyRate, taxCode) {
        salaryReport = repository.calculateMonthSalary(selectedYearMonth)
    }

    val monthName = remember(selectedYearMonth) {
        selectedYearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH).uppercase()
    }
    val yearStr = remember(selectedYearMonth) { selectedYearMonth.year.toString() }

    // Calculate Last Friday Payday and Sunday Cutoff
    val (payDayStr, cutoffStr) = remember(selectedYearMonth) {
        val lastDay = selectedYearMonth.atEndOfMonth()
        val lastFriday = lastDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
        val cutoffSunday = lastFriday.minusDays(5) // preceding Sunday
        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
        Pair(lastFriday.format(formatter), cutoffSunday.format(formatter))
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
            contentPadding = PaddingValues(top = 28.dp, bottom = 32.dp, start = 10.dp, end = 10.dp)
        ) {
            // Month Switcher Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactButton(
                        onClick = { onMonthSelect(selectedYearMonth.minusMonths(1)) },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("<", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
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
                        onClick = { onMonthSelect(selectedYearMonth.plusMonths(1)) },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text(">", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Hero Net Take-Home Pay
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF064E3B),
                        endBackgroundColor = Color(0xFF042F2E)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "ESTIMATED TAKE-HOME",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA7F3D0)
                        )
                        Text(
                            text = salaryReport?.let { String.format(Locale.UK, "£%,.2f", it.netPay) } ?: "£0.00",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = WearEmerald
                        )
                    }
                }
            }

            // Gross Earnings & Hours Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF1E1E24),
                        endBackgroundColor = Color(0xFF18181B)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gross Pay", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = salaryReport?.let { String.format(Locale.UK, "£%,.2f", it.grossPay) } ?: "£0.00",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hours Worked", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = "${salaryReport?.totalHours?.toInt() ?: 0} hrs (${salaryReport?.shiftCount ?: 0} shifts)",
                                fontSize = 10.sp,
                                color = WearTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hourly Rate", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = String.format(Locale.UK, "£%.2f/hr", hourlyRate),
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Deductions Breakdown Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF1E1E24),
                        endBackgroundColor = Color(0xFF18181B)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Deductions Breakdown", fontSize = 10.sp, color = Color(0xFFA1A1AA), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PAYE Tax ($taxCode)", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = salaryReport?.let { String.format(Locale.UK, "-£%,.2f", it.incomeTax) } ?: "£0.00",
                                fontSize = 10.sp,
                                color = Color(0xFFF87171)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Class 1 NI (8%)", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = salaryReport?.let { String.format(Locale.UK, "-£%,.2f", it.nationalInsurance) } ?: "£0.00",
                                fontSize = 10.sp,
                                color = Color(0xFFF87171)
                            )
                        }
                        if ((salaryReport?.pensionContribution ?: 0.0) > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pension Relief", fontSize = 10.sp, color = Color.LightGray)
                                Text(
                                    text = String.format(Locale.UK, "-£%,.2f", salaryReport?.pensionContribution ?: 0.0),
                                    fontSize = 10.sp,
                                    color = Color(0xFFF87171)
                                )
                            }
                        }
                    }
                }
            }

            // Pay Schedule Card (Last Friday Payday & Cutoff)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF1E1E24),
                        endBackgroundColor = Color(0xFF18181B)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pay Day", fontSize = 10.sp, color = Color.LightGray)
                            Text(payDayStr, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WearTeal)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cutoff Date", fontSize = 10.sp, color = Color.LightGray)
                            Text(cutoffStr, fontSize = 10.sp, color = Color(0xFFFBBF24))
                        }
                    }
                }
            }

            // Quick Rate Adjuster (+ / - £0.50/hr)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.setHourlyRate(maxOf(5.0, hourlyRate - 0.50))
                            }
                        },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("-£0.50", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = String.format(Locale.UK, "£%.2f/h", hourlyRate),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    CompactButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.setHourlyRate(hourlyRate + 0.50)
                            }
                        },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("+£0.50", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Back to Heatmap Action Button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                CompactChip(
                    onClick = onBackToHeatmap,
                    label = { Text("Back to Heatmap", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}
