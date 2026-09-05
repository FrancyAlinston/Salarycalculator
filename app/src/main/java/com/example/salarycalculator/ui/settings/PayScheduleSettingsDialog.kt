package com.example.salarycalculator.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

@Composable
fun PayScheduleSettingsDialog(
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val initialConfig by salaryRepository.getPayScheduleConfig().collectAsState(initial = PayScheduleConfig())

    var selectedType by remember(initialConfig) { mutableStateOf(initialConfig.type) }
    var fixedDayInput by remember(initialConfig) { mutableIntStateOf(initialConfig.fixedDay) }
    var cutoffLeadDaysInput by remember(initialConfig) { mutableIntStateOf(initialConfig.cutoffLeadDays) }

    val currentConfig = remember(selectedType, fixedDayInput, cutoffLeadDaysInput) {
        PayScheduleConfig(
            type = selectedType,
            fixedDay = fixedDayInput,
            cutoffLeadDays = cutoffLeadDaysInput
        )
    }

    val currentCal = remember { Calendar.getInstance() }
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentMonth = currentCal.get(Calendar.MONTH) + 1
    val monthNames = remember { DateFormatSymbols().shortMonths }

    // Next 3 preview periods
    val upcomingPeriods = remember(currentConfig, currentYear, currentMonth) {
        (0..2).map { offset ->
            var m = currentMonth + offset
            var y = currentYear
            if (m > 12) {
                m -= 12
                y += 1
            }
            PayScheduleEngine.calculatePayPeriod(y, m, currentConfig)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Payroll & Cutoff Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure your company's salary calculation rule and timesheet cutoff deadlines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Schedule Type Options
                Text(
                    text = "Pay Schedule Rule",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                PayScheduleType.values().forEach { scheduleType ->
                    val isSelected = selectedType == scheduleType
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedType = scheduleType }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedType = scheduleType }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scheduleType.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = scheduleType.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Custom parameters if FIXED_DAY_OF_MONTH
                AnimatedVisibility(visible = selectedType == PayScheduleType.FIXED_DAY_OF_MONTH) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Fixed Payday & Cutoff Lead Time",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = fixedDayInput.toString(),
                                onValueChange = { fixedDayInput = it.toIntOrNull()?.coerceIn(1, 31) ?: 28 },
                                label = { Text("Payday (e.g. 28th)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cutoffLeadDaysInput.toString(),
                                onValueChange = { cutoffLeadDaysInput = it.toIntOrNull()?.coerceIn(0, 14) ?: 5 },
                                label = { Text("Lead Days (e.g. 5d)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Upcoming Pay & Cutoff Dates Preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        upcomingPeriods.forEach { period ->
                            val mName = monthNames.getOrElse(period.month - 1) { "M${period.month}" }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$mName ${period.year}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Amber60.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Cutoff: ${period.cutoffDay} $mName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Amber60,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Emerald60.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Pay: ${period.payDay} $mName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Emerald60,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // iCalendar Export Action
                OutlinedButton(
                    onClick = {
                        val ics = IcsCalendarExporter.generatePayScheduleIcsContent(
                            year = currentYear,
                            config = currentConfig
                        )
                        IcsCalendarExporter.shareIcsFile(
                            context = context,
                            icsContent = ics,
                            filename = "Pay_and_Cutoff_Schedule_${currentYear}.ics"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Payday & Cutoff Reminders (.ics)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        salaryRepository.setPayScheduleConfig(currentConfig)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
