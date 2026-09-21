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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    monthShifts: Map<Int, Double>,
    configuredHourlyRate: Double = 12.82,
    standardShiftHours: Double = 12.0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showEditFields by remember { mutableStateOf(false) }

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
    val auditReport = remember(parsedPayslip, monthShifts, selectedYear, selectedMonth, configuredHourlyRate, standardShiftHours) {
        PayslipAuditEngine.auditPayslipAgainstTimesheet(
            payslip = parsedPayslip,
            monthShifts = monthShifts,
            year = selectedYear,
            month = selectedMonth,
            configuredHourlyRate = configuredHourlyRate,
            standardShiftDuration = standardShiftHours
        )
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
                            // Primary Action: Draft Dispute Email
                            Button(
                                onClick = {
                                    PayslipAuditEngine.dispatchDisputeEmail(context, auditReport)
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
                                    text = if (auditReport.status == AuditMismatchStatus.UNDERPAID) "Draft Dispute Email to Employer" else "Email Payslip Audit Report",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val ok = PayslipAuditEngine.copyDiscrepancyTextToClipboard(context, auditReport)
                                        if (ok) {
                                            Toast.makeText(context, "📋 Dispute email copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Email", maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        documentPickerLauncher.launch("*/*")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan Other", maxLines = 1)
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
                                Text("${auditReport.timesheetShiftsCount} Shifts Logged", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Emerald60)
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
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                                Text(
                                    text = "Dates Worked (${auditReport.workedDays.size} Shifts)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ "%.1f".format(auditReport.timesheetTotalHours) } hrs total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Emerald60,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (auditReport.workedDays.isEmpty()) {
                                Text(
                                    text = "No shifts logged in heatmap for ${auditReport.payPeriod}.",
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
                        }
                    }

                    // Email Preview Snippet Card
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Dispute Email Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Subject: ${PayslipAuditEngine.generateDisputeEmailSubject(auditReport)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            Text(
                                text = PayslipAuditEngine.generateDiscrepancyEmailText(auditReport),
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
