package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.ActiveShiftState
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShiftStopwatchCard(
    salaryRepository: SalaryRepository,
    onApplyToCalculator: (days: Double, hours: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val shiftState by salaryRepository.getActiveShiftState().collectAsState(initial = ActiveShiftState())

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Live 1-second ticker when shift is active
    LaunchedEffect(shiftState.isPunchActive) {
        while (shiftState.isPunchActive) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsedSeconds = remember(currentTime, shiftState) {
        if (shiftState.isPunchActive && shiftState.startTime > 0L) {
            maxOf(0L, (currentTime - shiftState.startTime) / 1000L)
        } else {
            0L
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Shift Punch Clock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (shiftState.isPunchActive) {
                    Surface(
                        color = Emerald60.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Emerald60))
                            Text("Punched In", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Emerald60)
                        }
                    }
                }
            }

            // Timer display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (shiftState.isPunchActive) timeFormatted else "00:00:00",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (shiftState.isPunchActive) Emerald60 else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Accumulated: ${"%.1f".format(shiftState.accumulatedHours)} hrs (${shiftState.accumulatedDays.toInt()} shifts)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Punch In / Punch Out Action Button
                if (!shiftState.isPunchActive) {
                    Button(
                        onClick = {
                            scope.launch {
                                salaryRepository.saveActiveShiftState(
                                    shiftState.copy(
                                        isPunchActive = true,
                                        startTime = System.currentTimeMillis()
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Punch In", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            val durationHrs = elapsedSeconds / 3600.0
                            val newTotalHrs = shiftState.accumulatedHours + durationHrs
                            val newTotalDays = shiftState.accumulatedDays + 1.0
                            scope.launch {
                                salaryRepository.saveActiveShiftState(
                                    shiftState.copy(
                                        isPunchActive = false,
                                        startTime = 0L,
                                        accumulatedDays = newTotalDays,
                                        accumulatedHours = newTotalHrs
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose60),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Punch Out", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Apply to Calculator Action
            if (shiftState.accumulatedHours > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val avgHrsPerDay = if (shiftState.accumulatedDays > 0) shiftState.accumulatedHours / shiftState.accumulatedDays else 8.0
                            onApplyToCalculator(shiftState.accumulatedDays, avgHrsPerDay)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply to Calculator")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                salaryRepository.saveActiveShiftState(
                                    shiftState.copy(accumulatedDays = 0.0, accumulatedHours = 0.0)
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}
