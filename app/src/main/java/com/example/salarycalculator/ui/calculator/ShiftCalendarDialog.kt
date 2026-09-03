package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Teal60

@Composable
fun ShiftCalendarDialog(
    initialDaysWorked: Double,
    initialHoursPerDay: Double,
    onApply: (days: Double, hoursPerDay: Double, overtimeHours: Double) -> Unit,
    onDismiss: () -> Unit
) {
    // 30-day month model for shift logging
    val dayShifts = remember {
        mutableStateMapOf<Int, Double>().apply {
            val count = initialDaysWorked.toInt().coerceIn(0, 30)
            for (i in 1..count) {
                put(i, initialHoursPerDay)
            }
        }
    }

    val totalDaysWorked = dayShifts.values.count { it > 0 }
    val totalHoursLogged = dayShifts.values.sum()
    val standardHours = dayShifts.values.sumOf { minOf(it, 8.0) }
    val overtimeHours = dayShifts.values.sumOf { maxOf(0.0, it - 8.0) }
    val avgHoursPerDay = if (totalDaysWorked > 0) standardHours / totalDaysWorked else 8.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Monthly Shift Heatmap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tap any day to toggle between: Off (0h) ➔ Regular (8h) ➔ Overtime (10h).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Day headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(day, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // 30 Days Grid (Chunked Rows for optimal Dialog performance)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..30).toList().chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { day ->
                                val hours = dayShifts[day] ?: 0.0
                                val bgColor = when {
                                    hours >= 9.0 -> Amber60
                                    hours > 0.0 -> Emerald60
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val textColor = if (hours > 0.0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .clickable {
                                            dayShifts[day] = when (hours) {
                                                0.0 -> 8.0
                                                8.0 -> 10.0
                                                else -> 0.0
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$day",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        if (hours > 0) {
                                            Text(
                                                text = "${hours.toInt()}h",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill remaining slots in last week
                            for (pad in week.size until 7) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Heatmap Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Emerald60))
                        Text("8h Shift", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Amber60))
                        Text("10h Overtime", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                        Text("Day Off", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider()

                // Cumulative Shift Summary
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Days Worked", style = MaterialTheme.typography.labelSmall)
                            Text("$totalDaysWorked days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Regular Hours", style = MaterialTheme.typography.labelSmall)
                            Text("${"%.1f".format(standardHours)}h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Emerald60)
                        }
                        Column {
                            Text("Overtime", style = MaterialTheme.typography.labelSmall)
                            Text("${"%.1f".format(overtimeHours)}h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Amber60)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(totalDaysWorked.toDouble(), avgHoursPerDay, overtimeHours)
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply to Calculator")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
