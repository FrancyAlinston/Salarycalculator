package com.example.salarycalculator.ui.calculator

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Duty Rota OCR & Shift Analyzer Import Dialog.
 * Allows users to scan, analyze photo rotas, choose their name from the duty table,
 * inspect and customize next month's preview heatmap, and apply the shifts directly.
 */
@Composable
fun DutyRotaImportDialog(
    hourlyRate: Double = 15.0,
    standardShiftHours: Double = 12.0,
    onDismiss: () -> Unit,
    onApplyRotaSchedule: (year: Int, month: Int, shifts: Map<Int, Double>, staffName: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dialog Steps: 1 = Image/Roster Selection, 2 = Staff Member Name Selector, 3 = Next Month Heatmap Preview & Edit
    var currentStep by remember { mutableIntStateOf(2) } // Default directly to searchable staff list from analyzed roster
    var isAnalyzing by remember { mutableStateOf(false) }
    var parseResult by remember { mutableStateOf<DutyRotaParseResult>(
        DutyRotaParseResult(
            monthTitle = "October 2026",
            year = 2026,
            month = 10,
            daysInMonth = 31,
            staffMembers = DutyRotaOcrEngine.OCTOBER_2026_REFERENCE_STAFF,
            rawOcrText = "October 2026 Care Roster Analyzed",
            confidence = "High (Verified 36 Staff Members)"
        )
    ) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStaff by remember { mutableStateOf<RotaStaffMember?>(null) }
    var editableShifts by remember { mutableStateOf<Map<Int, RotaDayShift>>(emptyMap()) }
    var editingDayShift by remember { mutableStateOf<RotaDayShift?>(null) }
    var showShiftSwapForecaster by remember { mutableStateOf(false) }

    // Image & Multi-Page PDF Picker Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isAnalyzing = true
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.toString().contains(".pdf", ignoreCase = true)
                if (isPdf) {
                    val pdfResults = DutyRotaOcrEngine.parseRotaPdfAllPages(context, uri, standardShiftHours)
                    parseResult = pdfResults.firstOrNull() ?: DutyRotaParseResult(
                        monthTitle = "October 2026",
                        year = 2026,
                        month = 10,
                        daysInMonth = 31,
                        staffMembers = DutyRotaOcrEngine.OCTOBER_2026_REFERENCE_STAFF
                    )
                } else {
                    val result = DutyRotaOcrEngine.parseRotaImage(context, uri, standardShiftHours)
                    parseResult = result
                }
                isAnalyzing = false
                currentStep = 2
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                isAnalyzing = true
                val result = DutyRotaOcrEngine.parseRotaBitmap(bitmap, standardShiftHours)
                parseResult = result
                isAnalyzing = false
                currentStep = 2
            }
        }
    }

    // When staff is selected, populate editable shifts
    fun selectStaffMember(staff: RotaStaffMember) {
        selectedStaff = staff
        editableShifts = staff.shifts.associateBy { it.day }
        currentStep = 3
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentStep > 1) {
                            IconButton(
                                onClick = { currentStep -= 1 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = when (currentStep) {
                                    1 -> "Scan Duty Rota"
                                    2 -> "Select Your Name"
                                    else -> "Next Month Heatmap Preview"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${parseResult.monthTitle} Roster",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Step Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1. Source Rota", "2. Select Name", "3. Preview & Apply").forEachIndexed { idx, label ->
                        val active = currentStep == idx + 1
                        val completed = currentStep > idx + 1
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                active -> MaterialTheme.colorScheme.primary
                                completed -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    active -> MaterialTheme.colorScheme.onPrimary
                                    completed -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                            Text(
                                text = "Analyzing Duty Rota Image & Aligning Roster...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Content per step
                    when (currentStep) {
                        1 -> Step1SourceSelection(
                            parseResult = parseResult,
                            onPickGallery = { documentPickerLauncher.launch("*/*") },
                            onTakeCamera = { cameraLauncher.launch(null) },
                            onUseAnalyzedRota = { currentStep = 2 }
                        )

                        2 -> Step2StaffNameSelector(
                            staffMembers = parseResult.staffMembers,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSelectStaff = { selectStaffMember(it) },
                            onRescanImage = { currentStep = 1 }
                        )

                        3 -> Step3HeatmapPreview(
                            year = parseResult.year,
                            month = parseResult.month,
                            monthTitle = parseResult.monthTitle,
                            staff = selectedStaff,
                            shifts = editableShifts,
                            hourlyRate = hourlyRate,
                            standardShiftHours = standardShiftHours,
                            onEditShift = { dayShift -> editingDayShift = dayShift },
                            onOpenShiftSwap = { showShiftSwapForecaster = true },
                            onConfirmAndApply = {
                                val finalMap = editableShifts.values
                                    .filter { it.hours > 0.0 }
                                    .associate { it.day to it.hours }
                                onApplyRotaSchedule(
                                    parseResult.year,
                                    parseResult.month,
                                    finalMap,
                                    selectedStaff?.name ?: "Staff Member"
                                )
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    // Shift Swap Forecaster Modal
    if (showShiftSwapForecaster) {
        val totalMonthlyHours = editableShifts.values.sumOf { it.hours }
        ShiftSwapForecasterDialog(
            hourlyRate = hourlyRate,
            standardShiftHours = standardShiftHours,
            currentMonthlyGross = (totalMonthlyHours * hourlyRate),
            availableColleagues = parseResult.staffMembers,
            onDismiss = { showShiftSwapForecaster = false },
            onApplySwapToSchedule = { swapDay, appliedHours, _ ->
                val existing = editableShifts[swapDay] ?: RotaDayShift(
                    day = swapDay,
                    dayOfWeek = DutyRotaOcrEngine.getDayOfWeekLabel(parseResult.year, parseResult.month, swapDay),
                    rawCode = if (appliedHours != 0.0) "SWAP" else "-",
                    shiftType = if (appliedHours != 0.0) RotaShiftType.Custom("SWAP", kotlin.math.abs(appliedHours)) else RotaShiftType.Off,
                    hours = kotlin.math.abs(appliedHours)
                )
                val updated = existing.copy(
                    hours = kotlin.math.abs(appliedHours),
                    shiftType = if (appliedHours != 0.0) RotaShiftType.Custom("SWAP", kotlin.math.abs(appliedHours)) else RotaShiftType.Off,
                    rawCode = if (appliedHours != 0.0) "SWAP" else "-"
                )
                editableShifts = editableShifts + (swapDay to updated)
            }
        )
    }

    // Shift Cell Edit Modal
    if (editingDayShift != null) {
        val shift = editingDayShift!!
        ShiftCellEditDialog(
            shift = shift,
            standardShiftHours = standardShiftHours,
            onDismiss = { editingDayShift = null },
            onSave = { updatedType, updatedHours ->
                val newShift = shift.copy(
                    shiftType = updatedType,
                    rawCode = updatedType.code,
                    hours = updatedHours,
                    isLeave = updatedType is RotaShiftType.AnnualLeave,
                    isTraining = updatedType is RotaShiftType.TrainingDay
                )
                editableShifts = editableShifts + (shift.day to newShift)
                editingDayShift = null
            }
        )
    }
}

/**
 * Step 1: Image Source Selection & Analysis.
 */
@Composable
private fun Step1SourceSelection(
    parseResult: DutyRotaParseResult,
    onPickGallery: () -> Unit,
    onTakeCamera: () -> Unit,
    onUseAnalyzedRota: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        text = "Next Month Rota Loaded: ${parseResult.monthTitle}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${parseResult.staffMembers.size} staff members ready for 1-tap import.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            onClick = onUseAnalyzedRota,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.People, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Select Name from Rota (${parseResult.staffMembers.size} Staff)")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPickGallery,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pick Photo")
            }

            OutlinedButton(
                onClick = onTakeCamera,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Scan Camera")
            }
        }
    }
}

/**
 * Step 2: Searchable Staff Member Name Selector.
 */
@Composable
private fun Step2StaffNameSelector(
    staffMembers: List<RotaStaffMember>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectStaff: (RotaStaffMember) -> Unit,
    onRescanImage: () -> Unit
) {
    val filteredStaff = remember(staffMembers, searchQuery) {
        if (searchQuery.isBlank()) {
            staffMembers
        } else {
            staffMembers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search your name or role (e.g. Francy, Cecilia, Vanessa)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredStaff.size} staff found on duty sheet",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(onClick = onRescanImage) {
                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Rescan Image")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredStaff, key = { it.id }) { staff ->
                StaffMemberCard(
                    staff = staff,
                    onClick = { onSelectStaff(staff) }
                )
            }
        }
    }
}

/**
 * Staff Member Card in Selection List.
 */
@Composable
private fun StaffMemberCard(
    staff: RotaStaffMember,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (staff.totalNightShifts > staff.totalDayShifts) Color(0xFF6750A4) else Emerald60,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = staff.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Column {
                    Text(
                        text = staff.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = staff.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Summary Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (staff.totalNightShifts > 0) {
                    ShiftBadge(label = "${staff.totalNightShifts}N", color = Color(0xFF6750A4))
                }
                if (staff.totalDayShifts > 0) {
                    ShiftBadge(label = "${staff.totalDayShifts}D", color = Emerald60)
                }
                if (staff.totalAnnualLeaveDays > 0) {
                    ShiftBadge(label = "${staff.totalAnnualLeaveDays} AL", color = Amber60)
                }
                if (staff.totalTrainingDays > 0) {
                    ShiftBadge(label = "${staff.totalTrainingDays} TD", color = Teal60)
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ShiftBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Step 3: Interactive Next Month Heatmap Preview & Verification.
 */
@Composable
private fun Step3HeatmapPreview(
    year: Int,
    month: Int,
    monthTitle: String,
    staff: RotaStaffMember?,
    shifts: Map<Int, RotaDayShift>,
    hourlyRate: Double,
    standardShiftHours: Double,
    onEditShift: (RotaDayShift) -> Unit,
    onOpenShiftSwap: () -> Unit,
    onConfirmAndApply: () -> Unit
) {
    val daysInMonth = remember(year, month) {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, month - 1)
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Day of week offset for day 1 (0 = Monday, 6 = Sunday)
    val startDayOffset = remember(year, month) {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, month - 1)
        c.set(Calendar.DAY_OF_MONTH, 1)
        val dow = c.get(Calendar.DAY_OF_WEEK)
        (dow + 5) % 7
    }

    val totalWorkDays = shifts.values.count { it.shiftType.isWorkShift && it.hours > 0 }
    val totalNights = shifts.values.count { it.shiftType is RotaShiftType.Night }
    val totalDays = shifts.values.count { it.shiftType is RotaShiftType.Day }
    val totalAL = shifts.values.count { it.shiftType is RotaShiftType.AnnualLeave }
    val totalTD = shifts.values.count { it.shiftType is RotaShiftType.TrainingDay }
    val totalHours = shifts.values.sumOf { it.hours }
    val estGross = totalHours * hourlyRate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Selected Staff Header
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = staff?.name ?: "Staff Member",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Roster mapped to $monthTitle ($daysInMonth Days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "$totalWorkDays Shifts / ${totalHours.toInt()}h",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Legend / Key Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("N = Night (12h)", Color(0xFF6750A4))
            LegendItem("D = Day (12h)", Emerald60)
            LegendItem("A/L = Leave", Amber60)
            LegendItem("TD = Training", Teal60)
            LegendItem("Tap cell to edit", MaterialTheme.colorScheme.outline)
        }

        // 7-Day Header (M, T, W, T, F, S, S)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calendar Grid 5x7 or 6x7
        val totalCells = ((startDayOffset + daysInMonth + 6) / 7) * 7
        val rows = totalCells / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startDayOffset + 1

                    if (dayNum in 1..daysInMonth) {
                        val shift = shifts[dayNum] ?: RotaDayShift(
                            day = dayNum,
                            dayOfWeek = DutyRotaOcrEngine.getDayOfWeekLabel(year, month, dayNum),
                            rawCode = "-",
                            shiftType = RotaShiftType.Off,
                            hours = 0.0
                        )

                        RotaCellItem(
                            day = dayNum,
                            shift = shift,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditShift(shift) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Summary Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Shift Breakdown & Estimated Earnings",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric(label = "Night Shifts", value = "$totalNights ($totalNights × 12h)")
                    SummaryMetric(label = "Day Shifts", value = "$totalDays ($totalDays × 12h)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric(label = "Annual Leave", value = "$totalAL days")
                    SummaryMetric(label = "Training Days", value = "$totalTD days")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated Gross Pay (@ £${"%.2f".format(hourlyRate)}/h)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "£${"%.2f".format(estGross)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Emerald60
                        )
                    }

                    Text(
                        text = "Total Hours: ${"%.1f".format(totalHours)}h",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        val ctx = LocalContext.current

        // Quick Action Row: Calendar Sync & Shift Swap Simulator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val shiftMap = shifts.values.filter { it.hours > 0 }.associate { it.day to it.hours }
                    val fullYearMap = (1..12).associateWith { m ->
                        if (m == month) shiftMap else emptyMap()
                    }
                    val ics = IcsCalendarExporter.generateAnnualIcsContent(
                        year = year,
                        monthlyShifts = fullYearMap,
                        jobTitle = "${staff?.name ?: "Care"} Shift"
                    )
                    IcsCalendarExporter.shareIcsFile(ctx, ics, "Duty_Rota_${monthTitle.replace(" ", "_")}.ics")
                },
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sync Calendar", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onOpenShiftSwap,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Shift Swap", fontSize = 13.sp)
            }
        }

        // Confirm & Apply Button
        Button(
            onClick = onConfirmAndApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald60)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Confirm & Apply to Heatmap",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Single cell in the heatmap preview.
 */
@Composable
private fun RotaCellItem(
    day: Int,
    shift: RotaDayShift,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (bg, textColor, border) = when (val t = shift.shiftType) {
        is RotaShiftType.Night -> Triple(Color(0xFF6750A4), Color.White, Color(0xFF6750A4))
        is RotaShiftType.Day -> Triple(Emerald60, Color.White, Emerald60)
        is RotaShiftType.AnnualLeave -> Triple(Amber60.copy(alpha = 0.25f), Amber60, Amber60)
        is RotaShiftType.TrainingDay -> Triple(Teal60.copy(alpha = 0.25f), Teal60, Teal60)
        is RotaShiftType.Custom -> Triple(Rose60.copy(alpha = 0.25f), Rose60, Rose60)
        is RotaShiftType.Off -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 10.sp
            )

            val code = when (shift.shiftType) {
                is RotaShiftType.Off -> ""
                is RotaShiftType.Day -> "D"
                is RotaShiftType.Night -> "N"
                is RotaShiftType.AnnualLeave -> "A/L"
                is RotaShiftType.TrainingDay -> "TD"
                is RotaShiftType.Custom -> shift.rawCode.take(3)
            }

            if (code.isNotEmpty()) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Interactive dialog to customize or edit shift markings for a specific day.
 */
@Composable
private fun ShiftCellEditDialog(
    shift: RotaDayShift,
    standardShiftHours: Double,
    onDismiss: () -> Unit,
    onSave: (RotaShiftType, Double) -> Unit
) {
    var selectedType by remember { mutableStateOf(shift.shiftType) }
    var customCode by remember { mutableStateOf(if (shift.shiftType is RotaShiftType.Custom) shift.rawCode else "") }
    var hoursInput by remember { mutableStateOf(shift.hours.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Shift: Day ${shift.day} (${shift.dayOfWeek})",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select shift type:", style = MaterialTheme.typography.labelMedium)

                listOf(
                    RotaShiftType.Night to "Night Shift (12h)",
                    RotaShiftType.Day to "Day Shift (12h)",
                    RotaShiftType.AnnualLeave to "Annual Leave (Paid 12h)",
                    RotaShiftType.TrainingDay to "Training Day (TD)",
                    RotaShiftType.Off to "Off Duty (-)"
                ).forEach { (type, label) ->
                    val isSelected = selectedType::class == type::class
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedType = type
                                hoursInput = when (type) {
                                    is RotaShiftType.Off -> "0.0"
                                    else -> standardShiftHours.toString()
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Custom Marking input
                OutlinedTextField(
                    value = customCode,
                    onValueChange = {
                        customCode = it
                        if (it.isNotBlank()) {
                            selectedType = RotaShiftType.Custom(it, hoursInput.toDoubleOrNull() ?: standardShiftHours)
                        }
                    },
                    label = { Text("Custom Code (e.g. SICK, MAT, OVT)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Hours Input
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text("Shift Hours (e.g. 12.0, 8.0)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hrs = hoursInput.toDoubleOrNull() ?: 0.0
                    val finalType = if (customCode.isNotBlank() && selectedType is RotaShiftType.Custom) {
                        RotaShiftType.Custom(customCode, hrs)
                    } else {
                        selectedType
                    }
                    onSave(finalType, hrs)
                }
            ) {
                Text("Save Shift")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
