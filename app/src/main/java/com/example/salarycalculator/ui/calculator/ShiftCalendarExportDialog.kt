package com.example.salarycalculator.ui.calculator

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.salarycalculator.domain.IcsCalendarExporter
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Teal60
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols

/**
 * Interactive Shift Calendar Export Dialog for Google Calendar, Apple Calendar, and Outlook (.ics).
 * Enables seamless export of shifts with clean, custom titles (e.g. "Night Duty")
 * and realistic shift timing without false overtime tags.
 */
@Composable
fun ShiftCalendarExportDialog(
    selectedYear: Int,
    selectedMonth: Int,
    multiYearShifts: Map<String, Map<Int, Double>>,
    effectiveStandardHours: Double = 12.0,
    salaryRepository: SalaryRepository? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monthNames = remember { DateFormatSymbols().months }
    val currentMonthTitle = monthNames.getOrElse(selectedMonth - 1) { "Month $selectedMonth" }

    // Persistent Title & Timing state
    val savedTitleFlow = salaryRepository?.getShiftCalendarTitle()?.collectAsState(initial = "Night Duty")
    val savedHourFlow = salaryRepository?.getShiftCalendarStartHour()?.collectAsState(initial = 20)

    var shiftTitle by remember { mutableStateOf("Night Duty") }
    var startHour by remember { mutableIntStateOf(20) } // 20:00 (8:00 PM) for Night Duty
    var exportScopeAllYear by remember { mutableStateOf(false) } // Default to Active Month for quick export

    LaunchedEffect(savedTitleFlow?.value) {
        savedTitleFlow?.value?.let { saved ->
            if (saved.isNotBlank()) {
                shiftTitle = saved
            }
        }
    }

    LaunchedEffect(savedHourFlow?.value) {
        savedHourFlow?.value?.let { savedH ->
            if (savedH in 0..23) {
                startHour = savedH
            }
        }
    }

    // Preset shift titles
    val presetTitles = listOf("Night Duty", "Day Shift", "Care Shift", "Work Shift")

    // Shift timings
    data class TimingPreset(val label: String, val hour: Int, val description: String)
    val timingPresets = listOf(
        TimingPreset("Night Shift", 20, "20:00 – 08:00"),
        TimingPreset("Day Shift", 8, "08:00 – 20:00"),
        TimingPreset("Standard", 9, "09:00 – 21:00")
    )

    // Calculate shift counts
    val currentMonthMap = multiYearShifts["$selectedYear-$selectedMonth"] ?: emptyMap()
    val monthShiftsCount = currentMonthMap.values.count { kotlin.math.abs(it) > 0.0 }
    val annualShiftsCount = multiYearShifts.filterKeys { it.startsWith("$selectedYear-") }
        .values.sumOf { map -> map.values.count { kotlin.math.abs(it) > 0.0 } }

    val totalShiftsToExport = if (exportScopeAllYear) annualShiftsCount else monthShiftsCount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Export to Google Calendar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export RFC 5545 iCalendar (.ics)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // 1. Shift Title Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Calendar Event Title",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = shiftTitle,
                        onValueChange = { shiftTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g. Night Duty") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Selection Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetTitles.forEach { preset ->
                            val isSelected = shiftTitle.equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    shiftTitle = preset
                                    if (preset.contains("Night", ignoreCase = true)) {
                                        startHour = 20
                                    } else if (preset.contains("Day", ignoreCase = true)) {
                                        startHour = 8
                                    }
                                },
                                label = { Text(preset, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // 2. Shift Hours & Timing Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Shift Time of Day",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timingPresets.forEach { timing ->
                            val isSelected = startHour == timing.hour
                            FilterChip(
                                selected = isSelected,
                                onClick = { startHour = timing.hour },
                                label = {
                                    Column {
                                        Text(timing.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text(timing.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    val icon = if (timing.hour >= 18) Icons.Default.NightsStay else Icons.Default.WbSunny
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                // 3. Export Scope Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Export Scope",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = !exportScopeAllYear,
                            onClick = { exportScopeAllYear = false },
                            label = { Text("$currentMonthTitle $selectedYear ($monthShiftsCount shifts)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = exportScopeAllYear,
                            onClick = { exportScopeAllYear = true },
                            label = { Text("Full Year $selectedYear ($annualShiftsCount shifts)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Live Calendar Event Preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PREVIEW IN GOOGLE CALENDAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Emerald60)
                            )
                            Column {
                                Text(
                                    text = shiftTitle.ifBlank { "Night Duty" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val endHourFormatted = ((startHour + effectiveStandardHours.toInt()) % 24)
                                Text(
                                    text = "${String.format("%02d:00", startHour)} – ${String.format("%02d:00", endHourFormatted)} (${effectiveStandardHours.toInt()}h Shift)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "✓ Standard shifts show as \"$shiftTitle\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald60,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "✓ Overtime shifts show as \"$shiftTitle (Overtime)\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber60
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val targetTitle = shiftTitle.trim().ifBlank { "Night Duty" }

                            // Persist preferences
                            salaryRepository?.let { repo ->
                                scope.launch {
                                    repo.setShiftCalendarTitle(targetTitle)
                                    repo.setShiftCalendarStartHour(startHour)
                                }
                            }

                            val icsContent = if (exportScopeAllYear) {
                                val fullSchedule = (1..12).associateWith { m ->
                                    (multiYearShifts["$selectedYear-$m"] ?: emptyMap()).toMap()
                                }
                                IcsCalendarExporter.generateAnnualIcsContent(
                                    year = selectedYear,
                                    monthlyShifts = fullSchedule,
                                    jobTitle = targetTitle,
                                    standardShiftHours = effectiveStandardHours,
                                    startHour = startHour
                                )
                            } else {
                                IcsCalendarExporter.generateIcsContent(
                                    year = selectedYear,
                                    month = selectedMonth,
                                    dayShifts = currentMonthMap,
                                    jobTitle = targetTitle,
                                    standardShiftHours = effectiveStandardHours,
                                    startHour = startHour
                                )
                            }

                            val filename = if (exportScopeAllYear) {
                                "${targetTitle.replace(" ", "_")}_${selectedYear}.ics"
                            } else {
                                "${targetTitle.replace(" ", "_")}_${currentMonthTitle}_${selectedYear}.ics"
                            }

                            Toast.makeText(context, "Exporting $totalShiftsToExport shifts to Google Calendar...", Toast.LENGTH_SHORT).show()
                            IcsCalendarExporter.shareIcsFile(context, icsContent, filename)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export ($totalShiftsToExport)")
                    }
                }
            }
        }
    }
}
