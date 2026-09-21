package com.example.salarycalculator.ui.calculator

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/**
 * Interactive Payslip Import & Timesheet Discrepancy Auditor Dialog.
 * Automatically extracts figures from payslip photos/PDFs via OCR, cross-references
 * them against logged shifts in the Heatmap, flags underpayments, and generates
 * 1-tap formal dispute emails to employer payroll departments.
 */
@Composable
fun PayslipAuditDialog(
    initialYear: Int = 2026,
    initialMonth: Int = 9,
    allMultiYearShifts: Map<String, Map<Int, Double>> = emptyMap(),
    configuredHourlyRate: Double = 12.82,
    standardShiftHours: Double = 12.0,
    payScheduleConfig: PayScheduleConfig = PayScheduleConfig(),
    salaryRepository: SalaryRepository? = null,
    onDismiss: () -> Unit,
    onUpdateMonthShifts: (year: Int, month: Int, shifts: Map<Int, Double>) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showEditFields by remember { mutableStateOf(false) }
    var showShiftPickerGrid by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf(DisputeEmailTemplate.FORMAL) }

    // Persistent Payroll Contact Email Memory
    val savedPayrollEmailState = salaryRepository?.getPayrollContactEmail()?.collectAsState(initial = "")
    var payrollContactEmail by remember { mutableStateOf("") }
    LaunchedEffect(savedPayrollEmailState?.value) {
        savedPayrollEmailState?.value?.let { saved ->
            if (saved.isNotBlank() && payrollContactEmail.isBlank()) {
                payrollContactEmail = saved
            }
        }
    }

    val monthNames = remember { DateFormatSymbols().months.filter { it.isNotBlank() } }
    val dayOfWeekLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    // Live mutable shift map for the currently selected month
    val liveMonthShifts = remember { mutableStateMapOf<Int, Double>() }

    // Synchronize liveMonthShifts whenever selectedYear or selectedMonth changes
    LaunchedEffect(selectedYear, selectedMonth, allMultiYearShifts) {
        val key1 = "$selectedYear-$selectedMonth"
        val key2 = "$selectedYear-${if (selectedMonth < 10) "0$selectedMonth" else "$selectedMonth"}"
        val existing = allMultiYearShifts[key1] ?: allMultiYearShifts[key2] ?: emptyMap()
        liveMonthShifts.clear()
        existing.forEach { (d, h) ->
            if (h != 0.0) liveMonthShifts[d] = h
        }
    }

    // Previous month shifts for cutoff window crossover
    val prevMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
    val prevYear = if (selectedMonth == 1) selectedYear - 1 else selectedYear
    val prevKey1 = "$prevYear-$prevMonth"
    val prevKey2 = "$prevYear-${if (prevMonth < 10) "0$prevMonth" else "$prevMonth"}"
    val prevMonthShifts = remember(selectedYear, selectedMonth, allMultiYearShifts) {
        allMultiYearShifts[prevKey1] ?: allMultiYearShifts[prevKey2] ?: emptyMap()
    }

    // Preloaded with default parsed data for September 2026 Waterloo Manor Ltd reference
    var parsedPayslip by remember {
        mutableStateOf(
            ParsedPayslipData(
                employeeName = "Francy Alinston D'Silva",
                employeeRef = "910",
                niNumber = "RZ021006C",
                employerName = "Waterloo Manor Ltd",
                payPeriod = "September 2026",
                processDate = "30-09-2026",
                taxPeriod = 6,
                taxCode = "1257L",
                basicHours = 179.90,
                basicRate = 12.82,
                basicAmount = 2306.32,
                bankHolidayHours = 7.25,
                bankHolidayRate = 6.41,
                bankHolidayAmount = 46.47,
                grossPay = 2352.79,
                incomeTax = 260.80,
                nationalInsurance = 104.38,
                netPay = 1987.61,
                rawExtractedText = "September 2026 Payslip OCR Parsed",
                confidenceRating = "High Confidence"
            )
        )
    }

    // Editable text field states
    var editEmployeeName by remember(parsedPayslip) { mutableStateOf(parsedPayslip.employeeName ?: "") }
    var editEmployeeRef by remember(parsedPayslip) { mutableStateOf(parsedPayslip.employeeRef ?: "") }
    var editEmployerName by remember(parsedPayslip) { mutableStateOf(parsedPayslip.employerName ?: "") }
    var editBasicHours by remember(parsedPayslip) { mutableStateOf(parsedPayslip.basicHours.toString()) }
    var editBasicRate by remember(parsedPayslip) { mutableStateOf(parsedPayslip.basicRate.toString()) }
    var editGrossPay by remember(parsedPayslip) { mutableStateOf(parsedPayslip.grossPay.toString()) }
    var editNetPay by remember(parsedPayslip) { mutableStateOf(parsedPayslip.netPay.toString()) }
    var editTaxCode by remember(parsedPayslip) { mutableStateOf(parsedPayslip.taxCode) }
    var editProcessDate by remember(parsedPayslip) { mutableStateOf(parsedPayslip.processDate ?: "30-09-2026") }

    // Compute live audit report
    val auditReport = remember(parsedPayslip, liveMonthShifts.toMap(), prevMonthShifts, selectedYear, selectedMonth, configuredHourlyRate, standardShiftHours, payScheduleConfig) {
        PayslipAuditEngine.auditPayslipAgainstTimesheet(
            payslip = parsedPayslip,
            monthShifts = liveMonthShifts.toMap(),
            previousMonthShifts = prevMonthShifts,
            year = selectedYear,
            month = selectedMonth,
            configuredHourlyRate = configuredHourlyRate,
            standardShiftDuration = standardShiftHours,
            payScheduleConfig = payScheduleConfig,
            useCutoffWindow = true
        )
    }

    // Days in current selected month/year
    val daysInMonth = remember(selectedYear, selectedMonth) {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, selectedYear)
        c.set(Calendar.MONTH, selectedMonth - 1)
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Day of week for 1st day (0 = Monday, 6 = Sunday)
    val startDayOffset = remember(selectedYear, selectedMonth) {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, selectedYear)
        c.set(Calendar.MONTH, selectedMonth - 1)
        c.set(Calendar.DAY_OF_MONTH, 1)
        val dow = c.get(Calendar.DAY_OF_WEEK)
        (dow + 5) % 7
    }

    fun toggleShiftDay(day: Int) {
        val current = liveMonthShifts[day] ?: 0.0
        val effectiveStd = if (standardShiftHours > 0.0) standardShiftHours else 12.0
        if (current == 0.0) {
            liveMonthShifts[day] = effectiveStd
        } else {
            liveMonthShifts.remove(day)
        }
        onUpdateMonthShifts(selectedYear, selectedMonth, liveMonthShifts.toMap())
    }

    // Document / PDF Picker Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isAnalyzing = true
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.toString().contains(".pdf", ignoreCase = true)
                try {
                    val result = if (isPdf) {
                        PayslipOcrAnalyzer.analyzePdf(context, uri)
                    } else {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }
                        PayslipOcrAnalyzer.analyzeImage(bitmap)
                    }
                    if (result.grossPay > 0 || result.basicHours > 0 || result.netPay > 0) {
                        parsedPayslip = result
                        // Auto-align selected month & year from parsed process date / period
                        result.processDate?.let { pDate ->
                            val parts = pDate.split('-', '/', '.')
                            if (parts.size == 3) {
                                val m = parts[1].toIntOrNull()
                                val y = parts[2].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
                                if (m != null && m in 1..12) selectedMonth = m
                                if (y != null && y in 2020..2030) selectedYear = y
                            }
                        }
                    } else {
                        Toast.makeText(context, "OCR could not read payslip text clearly. You can verify and edit numbers manually.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to analyze payslip: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                isAnalyzing = true
                try {
                    val result = PayslipOcrAnalyzer.analyzeImage(bitmap)
                    if (result.grossPay > 0 || result.basicHours > 0 || result.netPay > 0) {
                        parsedPayslip = result
                        result.processDate?.let { pDate ->
                            val parts = pDate.split('-', '/', '.')
                            if (parts.size == 3) {
                                val m = parts[1].toIntOrNull()
                                val y = parts[2].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
                                if (m != null && m in 1..12) selectedMonth = m
                                if (y != null && y in 2020..2030) selectedYear = y
                            }
                        }
                    } else {
                        Toast.makeText(context, "OCR could not read payslip photo clearly. You can verify and edit numbers manually.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Camera OCR failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Scaffold(
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                                }
                                Column {
                                    Text(
                                        text = "Payslip Timesheet Auditor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "OCR Scan & Discrepancy Emailer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { showEditFields = !showEditFields },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    if (showEditFields) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showEditFields) "Done" else "Edit Values", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Primary Action: Draft Dispute Email (with attached Timesheet PDF)
                            Button(
                                onClick = {
                                    val pdfFile = try {
                                        TimesheetPdfGenerator.generateTimesheetPdf(context, auditReport)
                                    } catch (_: Exception) {
                                        null
                                    }
                                    PayslipAuditEngine.dispatchDisputeEmail(
                                        context = context,
                                        report = auditReport,
                                        recipientEmail = payrollContactEmail,
                                        template = selectedTemplate,
                                        attachmentFile = pdfFile
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (auditReport.status.isProblem) Rose60 else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (auditReport.status == AuditMismatchStatus.UNDERPAID) "Draft Dispute Email & PDF Attachment" else "Email Payslip Audit Report",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        TimesheetPdfGenerator.shareTimesheetPdf(context, auditReport)
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share PDF", maxLines = 1, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val ok = PayslipAuditEngine.copyDiscrepancyTextToClipboard(context, auditReport, selectedTemplate)
                                        if (ok) {
                                            Toast.makeText(context, "📋 Dispute email (${selectedTemplate.displayName.split(" ")[0]}) copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Text", maxLines = 1, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        documentPickerLauncher.launch("*/*")
                                    },
                                    modifier = Modifier.weight(0.9f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan", maxLines = 1, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Month & Year Selector Navigation Bar
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 1) {
                                            selectedMonth = 12
                                            selectedYear -= 1
                                        } else {
                                            selectedMonth -= 1
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${monthNames[selectedMonth - 1]} $selectedYear",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Heatmap: ${liveMonthShifts.size} Shifts Worked (${ "%.1f".format(liveMonthShifts.values.sumOf { kotlin.math.abs(it) }) }h)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Emerald60,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 12) {
                                            selectedMonth = 1
                                            selectedYear += 1
                                        } else {
                                            selectedMonth += 1
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                                }
                            }

                            // Quick Toggle to open inline Shift Picker Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showShiftPickerGrid = !showShiftPickerGrid },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(if (showShiftPickerGrid) Icons.Default.ExpandLess else Icons.Default.CalendarViewMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showShiftPickerGrid) "Hide Date Grid" else "Edit Heatmap Dates (${liveMonthShifts.size} Shifts)", fontSize = 12.sp)
                                }

                                if (liveMonthShifts.isEmpty()) {
                                    FilledTonalButton(
                                        onClick = {
                                            // Apply 16-shift default reference rota (e.g. 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 30)
                                            val defaultShifts = listOf(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 30)
                                            defaultShifts.forEach { d ->
                                                if (d <= daysInMonth) liveMonthShifts[d] = 12.0
                                            }
                                            onUpdateMonthShifts(selectedYear, selectedMonth, liveMonthShifts.toMap())
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("+ Populate 16 Shifts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Inline Interactive Shift Date Grid
                            if (showShiftPickerGrid) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Day Headers
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            dayOfWeekLabels.forEach { lbl ->
                                                Text(
                                                    text = lbl,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        // Calendar Grid Rows
                                        val totalCells = startDayOffset + daysInMonth
                                        val rows = (totalCells + 6) / 7

                                        for (row in 0 until rows) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                for (col in 0..6) {
                                                    val cellIndex = row * 7 + col
                                                    val dayNum = cellIndex - startDayOffset + 1

                                                    if (dayNum in 1..daysInMonth) {
                                                        val hrs = liveMonthShifts[dayNum] ?: 0.0
                                                        val isWorked = hrs != 0.0

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .aspectRatio(1.1f)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(
                                                                    if (isWorked) Emerald60.copy(alpha = 0.85f)
                                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                                )
                                                                .clickable { toggleShiftDay(dayNum) },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    text = "$dayNum",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isWorked) Color.White else MaterialTheme.colorScheme.onSurface
                                                                )
                                                                if (isWorked) {
                                                                    Text(
                                                                        text = "12h",
                                                                        fontSize = 8.sp,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = Color.White.copy(alpha = 0.9f)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            text = "💡 Tap any date to toggle a 12h worked shift in the heatmap.",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Import Options Bar
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Import Payslip Document / Photo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Camera", fontSize = 13.sp)
                                }
                                FilledTonalButton(
                                    onClick = { documentPickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gallery", fontSize = 13.sp)
                                }
                                FilledTonalButton(
                                    onClick = { documentPickerLauncher.launch("application/pdf") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (isAnalyzing) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Text("Analyzing payslip with ML Kit OCR...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Status Banner
                    val bannerColor = when (auditReport.status) {
                        AuditMismatchStatus.UNDERPAID -> Rose60
                        AuditMismatchStatus.OVERPAID -> Amber60
                        AuditMismatchStatus.RATE_DISCREPANCY -> Amber60
                        AuditMismatchStatus.IN_SYNC -> Emerald60
                    }

                    val bannerIcon = when (auditReport.status) {
                        AuditMismatchStatus.UNDERPAID -> Icons.Default.Warning
                        AuditMismatchStatus.OVERPAID -> Icons.Default.Info
                        AuditMismatchStatus.RATE_DISCREPANCY -> Icons.Default.Paid
                        AuditMismatchStatus.IN_SYNC -> Icons.Default.CheckCircle
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.12f)),
                        border = BorderStroke(1.5.dp, bannerColor.copy(alpha = 0.8f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(bannerIcon, contentDescription = null, tint = bannerColor, modifier = Modifier.size(24.dp))
                                Text(
                                    text = auditReport.status.label.uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = bannerColor
                                )
                            }
                            Text(
                                text = auditReport.discrepancySummary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pay Cycle: ${auditReport.payCycleStartDate} → ${auditReport.payCycleCutoffDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Pay Day: ${auditReport.payDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (auditReport.status == AuditMismatchStatus.UNDERPAID) {
                                HorizontalDivider(color = bannerColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Gross Shortfall", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("+£${ "%.2f".format(auditReport.grossShortfall) }", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = bannerColor)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Net Deficit (Take-Home)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("+£${ "%.2f".format(auditReport.netShortfall) }", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = bannerColor)
                                    }
                                }
                            }
                        }
                    }

                    // Side-by-Side Comparison Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left: Heatmap Timesheet Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald60, modifier = Modifier.size(16.dp))
                                    Text("Heatmap Rota", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("${auditReport.timesheetShiftsCount} In-Cycle Shifts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Emerald60)
                                Text("Total: ${ "%.1f".format(auditReport.timesheetTotalHours) } hrs", style = MaterialTheme.typography.bodySmall)
                                Text("Std: ${ "%.1f".format(auditReport.timesheetStandardHours) }h · OT: ${ "%.1f".format(auditReport.timesheetOvertimeHours) }h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                Text("Exp. Gross: £${ "%.2f".format(auditReport.timesheetExpectedGross) }", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text("Exp. Net: £${ "%.2f".format(auditReport.timesheetExpectedNet) }", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Right: Imported Payslip Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text("Paid Payslip", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Paid: ${ "%.2f".format(auditReport.payslipPaidBasicHours) } hrs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Rate: £${ "%.2f".format(auditReport.payslipPaidRate) }/hr", style = MaterialTheme.typography.bodySmall)
                                if (auditReport.payslipPaidBankHolidayHours > 0.0) {
                                    Text("BH Uplift: ${ "%.2f".format(auditReport.payslipPaidBankHolidayHours) }h @ £${ "%.2f".format(auditReport.payslipPaidBankHolidayRate) }", style = MaterialTheme.typography.labelSmall, color = Teal60)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                Text("Paid Gross: £${ "%.2f".format(auditReport.payslipPaidGross) }", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text("Paid Net: £${ "%.2f".format(auditReport.payslipPaidNet) }", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Editable Fields Section (Collapsible)
                    if (showEditFields) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Edit Parsed Payslip Values", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                OutlinedTextField(
                                    value = editEmployeeName,
                                    onValueChange = {
                                        editEmployeeName = it
                                        parsedPayslip = parsedPayslip.copy(employeeName = it)
                                    },
                                    label = { Text("Employee Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = editEmployeeRef,
                                        onValueChange = {
                                            editEmployeeRef = it
                                            parsedPayslip = parsedPayslip.copy(employeeRef = it)
                                        },
                                        label = { Text("Ref / Emp No") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = editEmployerName,
                                        onValueChange = {
                                            editEmployerName = it
                                            parsedPayslip = parsedPayslip.copy(employerName = it)
                                        },
                                        label = { Text("Employer Name") },
                                        modifier = Modifier.weight(1.5f),
                                        singleLine = true
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = editBasicHours,
                                        onValueChange = {
                                            editBasicHours = it
                                            val hrs = it.toDoubleOrNull() ?: 0.0
                                            parsedPayslip = parsedPayslip.copy(basicHours = hrs)
                                        },
                                        label = { Text("Basic Paid Hours") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = editBasicRate,
                                        onValueChange = {
                                            editBasicRate = it
                                            val r = it.toDoubleOrNull() ?: 0.0
                                            parsedPayslip = parsedPayslip.copy(basicRate = r)
                                        },
                                        label = { Text("Hourly Rate (£)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = editGrossPay,
                                        onValueChange = {
                                            editGrossPay = it
                                            val g = it.toDoubleOrNull() ?: 0.0
                                            parsedPayslip = parsedPayslip.copy(grossPay = g)
                                        },
                                        label = { Text("Gross Pay (£)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = editNetPay,
                                        onValueChange = {
                                            editNetPay = it
                                            val n = it.toDoubleOrNull() ?: 0.0
                                            parsedPayslip = parsedPayslip.copy(netPay = n)
                                        },
                                        label = { Text("Net Pay (£)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // Itemized Dates Worked in Heatmap
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "In-Cycle Worked Dates (${auditReport.workedDays.size} Shifts)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pay Cycle: ${auditReport.payCycleStartDate} to ${auditReport.payCycleCutoffDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${ "%.1f".format(auditReport.timesheetTotalHours) } hrs total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Emerald60,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (auditReport.workedDays.isEmpty()) {
                                Text(
                                    text = "No qualifying in-cycle shifts logged in heatmap for ${auditReport.payPeriod}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                auditReport.workedDays.forEachIndexed { idx, day ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (day.isOvertime) Amber60.copy(alpha = 0.2f) else Emerald60.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (day.isOvertime) Amber60 else Emerald60
                                                )
                                            }
                                            Text(
                                                text = day.dateFormatted,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (day.isOvertime) Amber60.copy(alpha = 0.15f) else Emerald60.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${day.hours}h",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (day.isOvertime) Amber60 else Emerald60,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Post-Cutoff Rollover Shifts Section
                            if (auditReport.postCutoffRolloverDays.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Teal60, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Post-Cutoff Shifts (${auditReport.postCutoffRolloverDays.size} Shifts)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Teal60
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Teal60.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Rolls to next month",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Teal60,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Worked after cutoff date (${auditReport.payCycleCutoffDate}) — correctly paid in subsequent pay cycle.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                auditReport.postCutoffRolloverDays.forEach { day ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${day.dateFormatted}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "${day.hours}h (Rollover)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Teal60,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Email Preview & Customizer Card
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text("Dispute Email Customizer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "+ PDF Attached",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Template Selection Chips
                            Text("Email Tone & Format Template:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DisputeEmailTemplate.values().forEach { tpl ->
                                    val isSel = selectedTemplate == tpl
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedTemplate = tpl },
                                        label = { Text(tpl.displayName.split(" ")[0], fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Employer Payroll Contact Email (Remembered across sessions)
                            OutlinedTextField(
                                value = payrollContactEmail,
                                onValueChange = { newEmail ->
                                    payrollContactEmail = newEmail
                                    scope.launch {
                                        salaryRepository?.setPayrollContactEmail(newEmail)
                                    }
                                },
                                label = { Text("Payroll Contact Email (Auto-Saved)") },
                                placeholder = { Text("e.g. payroll@employer.co.uk") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            Text(
                                text = "Subject: ${PayslipAuditEngine.generateDisputeEmailSubject(auditReport, selectedTemplate)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = PayslipAuditEngine.generateDiscrepancyEmailText(auditReport, selectedTemplate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 8
                            )
                        }
                    }
                }
            }
        }
    }
}
