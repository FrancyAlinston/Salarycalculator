package com.example.salarycalculator.ui.calculator

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SyncDialogTab {
    GOOGLE_CALENDAR,
    DUTY_ALARMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthCalendarSyncDialog(
    selectedYear: Int,
    selectedMonth: Int,
    monthShifts: Map<Int, Double>,
    standardShiftHours: Double = 12.0,
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(SyncDialogTab.GOOGLE_CALENDAR) }

    // Calendar sync permissions state
    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCalendarPermission = perms[Manifest.permission.READ_CALENDAR] == true && perms[Manifest.permission.WRITE_CALENDAR] == true
    }

    // Available device calendars
    var availableCalendars by remember { mutableStateOf<List<DeviceCalendarAccount>>(emptyList()) }
    var selectedCalendarId by remember { mutableLongStateOf(-1L) }

    // Load saved preferred calendar
    val savedCalendarIdState = salaryRepository.getSelectedGoogleCalendarId().collectAsState(initial = -1L)
    LaunchedEffect(savedCalendarIdState.value, hasCalendarPermission) {
        if (hasCalendarPermission) {
            val list = GoogleCalendarSyncEngine.getAvailableCalendars(context)
            availableCalendars = list
            if (savedCalendarIdState.value != -1L && list.any { it.id == savedCalendarIdState.value }) {
                selectedCalendarId = savedCalendarIdState.value
            } else if (list.isNotEmpty()) {
                selectedCalendarId = list.firstOrNull { it.isPrimary }?.id ?: list.first().id
            }
        }
    }

    // Calendar config options
    var shiftStartHour by remember { mutableIntStateOf(7) }
    var shiftStartMinute by remember { mutableIntStateOf(0) }
    var syncDutyShifts by remember { mutableStateOf(true) }
    var syncOvertimeShifts by remember { mutableStateOf(true) }
    var syncAnnualLeave by remember { mutableStateOf(true) }
    var syncSickLeave by remember { mutableStateOf(true) }
    var reminderMinutes by remember { mutableIntStateOf(30) }
    var clearPreviousSalaryCalcEvents by remember { mutableStateOf(true) }

    var isSyncingCalendar by remember { mutableStateOf(false) }
    var calendarSyncStatusMessage by remember { mutableStateOf<String?>(null) }

    // Duty Alarms configuration
    val savedAlarmJsonState = salaryRepository.getDutyAlarmTimesJson().collectAsState(initial = "")
    var dutyAlarmTimes by remember { mutableStateOf(DutyAlarmManager.DEFAULT_DUTY_ALARMS) }

    LaunchedEffect(savedAlarmJsonState.value) {
        if (savedAlarmJsonState.value.isNotBlank()) {
            val parsed = DutyAlarmManager.parseAlarmTimesJson(savedAlarmJsonState.value)
            if (parsed.isNotEmpty()) {
                dutyAlarmTimes = parsed
            }
        }
    }

    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var newAlarmHour by remember { mutableIntStateOf(6) }
    var newAlarmMinute by remember { mutableIntStateOf(0) }
    var newAlarmLabel by remember { mutableStateOf("Duty Shift - Commute") }

    var alarmStatusMessage by remember { mutableStateOf<String?>(null) }

    // Duty vs Excluded stats
    val dutyDays = remember(selectedYear, selectedMonth, monthShifts) {
        DutyAlarmManager.getDutyDaysForMonth(selectedYear, selectedMonth, monthShifts)
    }
    val annualLeaveDays = remember(selectedYear, selectedMonth, monthShifts) {
        DutyAlarmManager.getAnnualLeaveDaysForMonth(selectedYear, selectedMonth, monthShifts)
    }

    val monthNames = remember {
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
    val monthTitle = "${monthNames.getOrElse(selectedMonth - 1) { "Month $selectedMonth" }} $selectedYear"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Calendar Sync & Duty Alarms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Duty Shift Summary Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald60.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Emerald60.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("●", color = Emerald60, fontSize = 12.sp)
                            Column {
                                Text("${dutyDays.size} Duty Days", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Emerald60)
                                Text("Alarms Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Blue60.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Blue60.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("●", color = Blue60, fontSize = 12.sp)
                            Column {
                                Text("${annualLeaveDays.size} A/L Holidays", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Blue60)
                                Text("Alarms Excluded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                PrimaryTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = activeTab == SyncDialogTab.GOOGLE_CALENDAR,
                        onClick = { activeTab = SyncDialogTab.GOOGLE_CALENDAR },
                        text = { Text("Google Calendar", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeTab == SyncDialogTab.DUTY_ALARMS,
                        onClick = { activeTab = SyncDialogTab.DUTY_ALARMS },
                        text = { Text("Auto Duty Alarms", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        SyncDialogTab.GOOGLE_CALENDAR -> {
                            GoogleCalendarTabContent(
                                hasPermission = hasCalendarPermission,
                                onRequestPermission = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                },
                                availableCalendars = availableCalendars,
                                selectedCalendarId = selectedCalendarId,
                                onSelectCalendar = { id ->
                                    selectedCalendarId = id
                                    scope.launch { salaryRepository.setSelectedGoogleCalendarId(id) }
                                },
                                shiftStartHour = shiftStartHour,
                                onShiftStartHourChange = { shiftStartHour = it },
                                shiftStartMinute = shiftStartMinute,
                                onShiftStartMinuteChange = { shiftStartMinute = it },
                                syncDutyShifts = syncDutyShifts,
                                onToggleDutyShifts = { syncDutyShifts = it },
                                syncOvertimeShifts = syncOvertimeShifts,
                                onToggleOvertimeShifts = { syncOvertimeShifts = it },
                                syncAnnualLeave = syncAnnualLeave,
                                onToggleAnnualLeave = { syncAnnualLeave = it },
                                syncSickLeave = syncSickLeave,
                                onToggleSickLeave = { syncSickLeave = it },
                                reminderMinutes = reminderMinutes,
                                onReminderMinutesChange = { reminderMinutes = it },
                                clearPreviousSalaryCalcEvents = clearPreviousSalaryCalcEvents,
                                onToggleClearPrevious = { clearPreviousSalaryCalcEvents = it },
                                isSyncing = isSyncingCalendar,
                                statusMessage = calendarSyncStatusMessage,
                                onSyncNow = {
                                    if (selectedCalendarId == -1L) {
                                        Toast.makeText(context, "Please select a target Google Calendar category.", Toast.LENGTH_SHORT).show()
                                        return@GoogleCalendarTabContent
                                    }
                                    isSyncingCalendar = true
                                    calendarSyncStatusMessage = null
                                    scope.launch {
                                        val result = GoogleCalendarSyncEngine.syncMonthToCalendar(
                                            context = context,
                                            calendarId = selectedCalendarId,
                                            year = selectedYear,
                                            month = selectedMonth,
                                            shifts = monthShifts,
                                            config = CalendarSyncConfig(
                                                shiftStartHour = shiftStartHour,
                                                shiftStartMinute = shiftStartMinute,
                                                standardShiftHours = standardShiftHours,
                                                syncDutyShifts = syncDutyShifts,
                                                syncOvertimeShifts = syncOvertimeShifts,
                                                syncAnnualLeave = syncAnnualLeave,
                                                syncSickLeave = syncSickLeave,
                                                reminderMinutes = reminderMinutes,
                                                clearExistingSalaryCalcEvents = clearPreviousSalaryCalcEvents
                                            )
                                        )
                                        isSyncingCalendar = false
                                        calendarSyncStatusMessage = result.message
                                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                        SyncDialogTab.DUTY_ALARMS -> {
                            DutyAlarmsTabContent(
                                selectedYear = selectedYear,
                                selectedMonth = selectedMonth,
                                dutyDays = dutyDays,
                                annualLeaveDays = annualLeaveDays,
                                alarmTimes = dutyAlarmTimes,
                                onToggleAlarm = { id, enabled ->
                                    val updated = dutyAlarmTimes.map {
                                        if (it.id == id) it.copy(isEnabled = enabled) else it
                                    }
                                    dutyAlarmTimes = updated
                                    scope.launch {
                                        salaryRepository.setDutyAlarmTimesJson(DutyAlarmManager.encodeAlarmTimesJson(updated))
                                    }
                                },
                                onDeleteAlarm = { id ->
                                    val updated = dutyAlarmTimes.filter { it.id != id }
                                    dutyAlarmTimes = updated
                                    scope.launch {
                                        salaryRepository.setDutyAlarmTimesJson(DutyAlarmManager.encodeAlarmTimesJson(updated))
                                    }
                                },
                                onOpenAddAlarmDialog = { showAddAlarmDialog = true },
                                statusMessage = alarmStatusMessage,
                                onAutoSetAlarms = {
                                    val result = DutyAlarmManager.setAlarmsForMonthDutyDays(
                                        context = context,
                                        year = selectedYear,
                                        month = selectedMonth,
                                        shifts = monthShifts,
                                        alarmTimes = dutyAlarmTimes
                                    )
                                    alarmStatusMessage = result.message
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Alarm Sub-Dialog
    if (showAddAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAddAlarmDialog = false },
            title = { Text("Add Duty Shift Alarm Time", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configure an alarm time to set across all duty days in $monthTitle.", style = MaterialTheme.typography.bodySmall)

                    // Quick Time Chips
                    Text("Quick Presets:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            Triple(5, 0, "05:00 Early Wake"),
                            Triple(5, 30, "05:30 Wake Up"),
                            Triple(6, 0, "06:00 Depart"),
                            Triple(6, 30, "06:30 Handover"),
                            Triple(17, 30, "17:30 Night Prep"),
                            Triple(18, 0, "18:00 Night Shift")
                        )
                        items(presets) { (h, m, label) ->
                            SuggestionChip(
                                onClick = {
                                    newAlarmHour = h
                                    newAlarmMinute = m
                                    newAlarmLabel = label
                                },
                                label = { Text("%02d:%02d".format(h, m)) }
                            )
                        }
                    }

                    // Time inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = "%02d".format(newAlarmHour),
                            onValueChange = {
                                val v = it.toIntOrNull()
                                if (v != null && v in 0..23) newAlarmHour = v
                            },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = "%02d".format(newAlarmMinute),
                            onValueChange = {
                                val v = it.toIntOrNull()
                                if (v != null && v in 0..59) newAlarmMinute = v
                            },
                            label = { Text("Min (0-59)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = newAlarmLabel,
                        onValueChange = { newAlarmLabel = it },
                        label = { Text("Alarm Label / Message") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAlarm = DutyAlarmTime(
                            hour = newAlarmHour,
                            minute = newAlarmMinute,
                            label = newAlarmLabel.ifBlank { "Duty Shift" },
                            isEnabled = true
                        )
                        val updated = dutyAlarmTimes + newAlarm
                        dutyAlarmTimes = updated
                        scope.launch {
                            salaryRepository.setDutyAlarmTimesJson(DutyAlarmManager.encodeAlarmTimesJson(updated))
                        }
                        showAddAlarmDialog = false
                    }
                ) {
                    Text("Add Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAlarmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GoogleCalendarTabContent(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    availableCalendars: List<DeviceCalendarAccount>,
    selectedCalendarId: Long,
    onSelectCalendar: (Long) -> Unit,
    shiftStartHour: Int,
    onShiftStartHourChange: (Int) -> Unit,
    shiftStartMinute: Int,
    onShiftStartMinuteChange: (Int) -> Unit,
    syncDutyShifts: Boolean,
    onToggleDutyShifts: (Boolean) -> Unit,
    syncOvertimeShifts: Boolean,
    onToggleOvertimeShifts: (Boolean) -> Unit,
    syncAnnualLeave: Boolean,
    onToggleAnnualLeave: (Boolean) -> Unit,
    syncSickLeave: Boolean,
    onToggleSickLeave: (Boolean) -> Unit,
    reminderMinutes: Int,
    onReminderMinutesChange: (Int) -> Unit,
    clearPreviousSalaryCalcEvents: Boolean,
    onToggleClearPrevious: (Boolean) -> Unit,
    isSyncing: Boolean,
    statusMessage: String?,
    onSyncNow: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!hasPermission) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Amber60.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Amber60.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Amber60)
                            Text(
                                "Calendar Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Amber60
                            )
                        }
                        Text(
                            "Allow Salary Calculator to access device calendars so you can choose which Google Calendar category to add your shifts to.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Amber60),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant Calendar Permission", color = Color.White)
                        }
                    }
                }
            }
        }

        // Calendar Category Selector
        item {
            Text(
                "1. Select Target Google Calendar / Category:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (availableCalendars.isEmpty() && hasPermission) {
                Text(
                    "No local calendars detected. Please ensure a Google or device account has Calendar enabled in Android Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableCalendars.forEach { cal ->
                        val isSelected = cal.id == selectedCalendarId
                        val calColor = cal.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCalendar(cal.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(calColor)
                                    )
                                    Column {
                                        Text(
                                            text = cal.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (cal.accountName.isNotBlank()) {
                                            Text(
                                                text = cal.accountName + if (cal.isPrimary) " (Primary)" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectCalendar(cal.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shift Timing Presets
        item {
            Text(
                "2. Shift Start Time & Presets:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    Triple(7, 0, "07:00 Care Day"),
                    Triple(19, 0, "19:00 Care Night"),
                    Triple(8, 0, "08:00 Standard"),
                    Triple(9, 0, "09:00 Office")
                )
                items(presets) { (h, m, label) ->
                    val isSelected = (shiftStartHour == h && shiftStartMinute == m)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onShiftStartHourChange(h)
                            onShiftStartMinuteChange(m)
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }

        // Filter Inclusions
        item {
            Text(
                "3. Included Shift Types & Options:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Standard Duty Shifts", style = MaterialTheme.typography.bodySmall)
                    Checkbox(checked = syncDutyShifts, onCheckedChange = onToggleDutyShifts)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Overtime Shifts (OT)", style = MaterialTheme.typography.bodySmall)
                    Checkbox(checked = syncOvertimeShifts, onCheckedChange = onToggleOvertimeShifts)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Annual Leave Days (A/L)", style = MaterialTheme.typography.bodySmall)
                    Checkbox(checked = syncAnnualLeave, onCheckedChange = onToggleAnnualLeave)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Statutory Sick Pay Days (SSP)", style = MaterialTheme.typography.bodySmall)
                    Checkbox(checked = syncSickLeave, onCheckedChange = onToggleSickLeave)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Replace existing Salary Calculator events for this month", style = MaterialTheme.typography.bodySmall)
                    Checkbox(checked = clearPreviousSalaryCalcEvents, onCheckedChange = onToggleClearPrevious)
                }
            }
        }

        // Reminder Alert
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Calendar Reminder Alert:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val alerts = listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "1h")
                    alerts.forEach { (mins, label) ->
                        FilterChip(
                            selected = reminderMinutes == mins,
                            onClick = { onReminderMinutesChange(mins) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        if (statusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald60.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Emerald60.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Emerald60,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Action Button
        item {
            Button(
                onClick = onSyncNow,
                enabled = !isSyncing && hasPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing Shifts...")
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Month to Google Calendar", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DutyAlarmsTabContent(
    selectedYear: Int,
    selectedMonth: Int,
    dutyDays: List<Int>,
    annualLeaveDays: List<Int>,
    alarmTimes: List<DutyAlarmTime>,
    onToggleAlarm: (String, Boolean) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onOpenAddAlarmDialog: () -> Unit,
    statusMessage: String?,
    onAutoSetAlarms: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Explanation Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AlarmOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Auto Duty Days Alarm Setter",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Automatically configures multiple alarms for the whole month on every scheduled duty day. Annual Leave days are automatically excluded so you can sleep in on holidays!",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Duty Days List Overview
        item {
            Text(
                "Scheduled Duty Days (${dutyDays.size} Days):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (dutyDays.isEmpty()) {
                Text(
                    "No duty days logged in this month yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(dutyDays) { day ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald60.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Emerald60.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Day $day",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Emerald60,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Excluded Annual Leave Days
        if (annualLeaveDays.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.BeachAccess, contentDescription = null, tint = Blue60, modifier = Modifier.size(16.dp))
                    Text(
                        "Excluded Annual Leave (${annualLeaveDays.size} Days - No Alarms):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Blue60
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(annualLeaveDays) { day ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Blue60.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Blue60.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Day $day",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Blue60,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Alarm Times Configurations
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Configured Alarms per Duty Day:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenAddAlarmDialog) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Alarm")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alarmTimes.forEach { alarm ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = alarm.formattedTime,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = alarm.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { onToggleAlarm(alarm.id, it) }
                                )
                                if (alarmTimes.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteAlarm(alarm.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (statusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald60.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Emerald60.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Emerald60,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Action Button
        item {
            val enabledCount = alarmTimes.count { it.isEnabled }
            val totalAlarms = dutyDays.size * enabledCount

            Button(
                onClick = onAutoSetAlarms,
                enabled = dutyDays.isNotEmpty() && enabledCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald60)
            ) {
                Icon(Icons.Default.AlarmAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auto-Set Alarms for $totalAlarms Duty Alarms",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
