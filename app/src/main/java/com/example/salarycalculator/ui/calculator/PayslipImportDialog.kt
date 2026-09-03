package com.example.salarycalculator.ui.calculator

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import kotlinx.coroutines.launch

@Composable
fun PayslipImportDialog(
    salaryRepository: SalaryRepository? = null,
    onApplyToCalculator: (gross: Double, taxCode: String, pension: Double) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var parsedData by remember { mutableStateOf<ParsedPayslipData?>(null) }
    var batchResults by remember { mutableStateOf<List<ParsedPayslipData>>(emptyList()) }
    var selectedBatchIndex by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Editable field states
    var periodInput by remember { mutableStateOf("January 2025") }
    var employerInput by remember { mutableStateOf("Primary Employment") }
    var taxCodeInput by remember { mutableStateOf("1257L") }
    var grossInput by remember { mutableStateOf("") }
    var netInput by remember { mutableStateOf("") }
    var taxInput by remember { mutableStateOf("") }
    var niInput by remember { mutableStateOf("") }
    var pensionInput by remember { mutableStateOf("") }
    var studentLoanInput by remember { mutableStateOf("") }

    fun populateFields(data: ParsedPayslipData) {
        parsedData = data
        periodInput = data.payPeriod
        employerInput = data.employerName ?: "Primary Employment"
        taxCodeInput = data.taxCode
        grossInput = "%.2f".format(data.grossPay)
        netInput = "%.2f".format(data.netPay)
        taxInput = "%.2f".format(data.incomeTax)
        niInput = "%.2f".format(data.nationalInsurance)
        pensionInput = "%.2f".format(data.employeePension)
        studentLoanInput = "%.2f".format(data.studentLoan)
    }

    // Single Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                errorMessage = null
                saveSuccessMessage = null
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val result = PayslipOcrAnalyzer.analyzeImage(bitmap)
                        batchResults = listOf(result)
                        selectedBatchIndex = 0
                        populateFields(result)
                    } else {
                        errorMessage = "Could not decode selected image."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error reading image: ${e.localizedMessage}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Multiple Images Picker
    val multiImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessing = true
                errorMessage = null
                saveSuccessMessage = null
                try {
                    val bitmaps = uris.mapNotNull { u ->
                        context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
                    }
                    val results = PayslipOcrAnalyzer.analyzeMultipleImages(bitmaps)
                    if (results.isNotEmpty()) {
                        batchResults = results
                        selectedBatchIndex = 0
                        populateFields(results.first())
                    } else {
                        errorMessage = "No valid payroll fields detected in selected images."
                    }
                } catch (e: Exception) {
                    errorMessage = "Batch image error: ${e.localizedMessage}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // PDF Multi-Page Picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                errorMessage = null
                saveSuccessMessage = null
                try {
                    val allPages = PayslipOcrAnalyzer.analyzePdfAllPages(context, uri)
                    if (allPages.isNotEmpty()) {
                        batchResults = allPages
                        selectedBatchIndex = 0
                        populateFields(allPages.first())
                    } else {
                        val singleResult = PayslipOcrAnalyzer.analyzePdf(context, uri)
                        batchResults = listOf(singleResult)
                        selectedBatchIndex = 0
                        populateFields(singleResult)
                    }
                } catch (e: Exception) {
                    errorMessage = "Error reading PDF: ${e.localizedMessage}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Recalculate dynamic analysis when gross or tax code is edited
    val currentAnalysis = remember(grossInput, taxCodeInput, taxInput, niInput, netInput) {
        val g = grossInput.toDoubleOrNull() ?: 0.0
        val t = taxInput.toDoubleOrNull() ?: 0.0
        val ni = niInput.toDoubleOrNull() ?: 0.0
        val n = netInput.toDoubleOrNull() ?: 0.0
        PayslipParserEngine.analyzeStatutoryAlignment(g, taxCodeInput, t, ni, n)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Import & Analyze Payslip",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
                    text = "Import a payslip image (JPEG/PNG) or PDF document. Machine Learning OCR will extract your earnings, PAYE tax, NI, and tax code, running an instant statutory audit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Pickers Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("1 Photo", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = { multiImagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Batch Photos", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("PDF", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Batch Selection Chips (if multiple months parsed)
                if (batchResults.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Extracted ${batchResults.size} Payslips - Select to Inspect:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            batchResults.forEachIndexed { idx, item ->
                                FilterChip(
                                    selected = selectedBatchIndex == idx,
                                    onClick = {
                                        selectedBatchIndex = idx
                                        populateFields(item)
                                    },
                                    label = { Text("${idx + 1}. ${item.payPeriod}") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Processing Indicator
                if (isProcessing) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Scanning document with ML OCR...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Rose60.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Rose60,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Save Success Alert
                if (saveSuccessMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Emerald60.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = saveSuccessMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald60,
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Extracted Data Form
                if (parsedData != null || grossInput.isNotEmpty()) {
                    // Statutory Diagnosis Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentAnalysis.isEmergencyTax) Rose60.copy(alpha = 0.15f)
                        else if (currentAnalysis.isStatutoryMatch) Emerald60.copy(alpha = 0.15f)
                        else Amber60.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("HMRC Statutory Audit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentAnalysis.isStatutoryMatch) Emerald60 else if (currentAnalysis.isEmergencyTax) Rose60 else Amber60
                                ) {
                                    Text(
                                        text = if (currentAnalysis.isStatutoryMatch) "Verified Match" else if (currentAnalysis.isEmergencyTax) "Emergency Tax" else "Variance Alert",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = currentAnalysis.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Editable Fields
                    Text("Extracted Payslip Fields:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = periodInput,
                            onValueChange = { periodInput = it },
                            label = { Text("Period / Month") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = taxCodeInput,
                            onValueChange = { taxCodeInput = it },
                            label = { Text("Tax Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = grossInput,
                            onValueChange = { grossInput = it },
                            label = { Text("Gross Pay (£)") },
                            prefix = { Text("£ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = netInput,
                            onValueChange = { netInput = it },
                            label = { Text("Net Pay (£)") },
                            prefix = { Text("£ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taxInput,
                            onValueChange = { taxInput = it },
                            label = { Text("PAYE Tax (£)") },
                            prefix = { Text("£ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = niInput,
                            onValueChange = { niInput = it },
                            label = { Text("Class 1 NI (£)") },
                            prefix = { Text("£ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pensionInput,
                            onValueChange = { pensionInput = it },
                            label = { Text("Pension (£)") },
                            prefix = { Text("£ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = studentLoanInput,
                            onValueChange = { studentLoanInput = it },
                            label = { Text("Student Loan (£)") },
                            prefix = { Text("£ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (grossInput.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val g = grossInput.toDoubleOrNull() ?: 0.0
                            val p = pensionInput.toDoubleOrNull() ?: 0.0
                            onApplyToCalculator(g, taxCodeInput, p)
                            onDismiss()
                        }
                    ) {
                        Text("Apply to Calc")
                    }

                    if (batchResults.size > 1) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    batchResults.forEach { item ->
                                        val g = item.grossPay
                                        val n = item.netPay
                                        val t = item.incomeTax
                                        val ni = item.nationalInsurance
                                        val p = item.employeePension
                                        val sl = item.studentLoan

                                        val record = MonthlySalaryRecord(
                                            monthYear = item.payPeriod,
                                            daysWorked = 20.0,
                                            hoursPerDay = 8.0,
                                            overtimeHours = 0.0,
                                            overtimeMultiplier = 1.5,
                                            hourlyRate = if (g > 0) g / 160.0 else 15.0,
                                            grossPay = g,
                                            salarySacrifice = 0.0,
                                            pensionRate = if (g > 0) (p / g) * 100.0 else 5.0,
                                            pensionContribution = p,
                                            employerPension = g * 0.03,
                                            taxablePay = maxOf(0.0, g - (12570.0 / 12.0) - p),
                                            incomeTax = t,
                                            nationalInsurance = ni,
                                            studentLoanPlan = if (sl > 0) StudentLoanPlan.PLAN_2 else StudentLoanPlan.NONE,
                                            studentLoanDeduction = sl,
                                            totalDeductions = t + ni + p + sl,
                                            netPay = n,
                                            taxCode = item.taxCode,
                                            taxRegion = if (item.taxCode.startsWith("S", ignoreCase = true)) TaxRegion.SCOTLAND else TaxRegion.UK_STANDARD,
                                            note = "Batch imported via Payslip OCR Scanner"
                                        )
                                        salaryRepository?.saveSalaryRecord(record)
                                    }
                                    saveSuccessMessage = "Successfully imported all ${batchResults.size} payslips to History Ledger!"
                                }
                            }
                        ) {
                            Text("Save All (${batchResults.size})")
                        }
                    }

                    Button(
                        onClick = {
                            val g = grossInput.toDoubleOrNull() ?: 0.0
                            val n = netInput.toDoubleOrNull() ?: 0.0
                            val t = taxInput.toDoubleOrNull() ?: 0.0
                            val ni = niInput.toDoubleOrNull() ?: 0.0
                            val p = pensionInput.toDoubleOrNull() ?: 0.0
                            val sl = studentLoanInput.toDoubleOrNull() ?: 0.0

                            val record = MonthlySalaryRecord(
                                monthYear = periodInput,
                                daysWorked = 20.0,
                                hoursPerDay = 8.0,
                                overtimeHours = 0.0,
                                overtimeMultiplier = 1.5,
                                hourlyRate = if (g > 0) g / 160.0 else 15.0,
                                grossPay = g,
                                salarySacrifice = 0.0,
                                pensionRate = if (g > 0) (p / g) * 100.0 else 5.0,
                                pensionContribution = p,
                                employerPension = g * 0.03,
                                taxablePay = maxOf(0.0, g - (12570.0 / 12.0) - p),
                                incomeTax = t,
                                nationalInsurance = ni,
                                studentLoanPlan = if (sl > 0) StudentLoanPlan.PLAN_2 else StudentLoanPlan.NONE,
                                studentLoanDeduction = sl,
                                totalDeductions = t + ni + p + sl,
                                netPay = n,
                                taxCode = taxCodeInput,
                                taxRegion = if (taxCodeInput.startsWith("S", ignoreCase = true)) TaxRegion.SCOTLAND else TaxRegion.UK_STANDARD,
                                note = "Imported via Payslip OCR Scanner"
                            )

                            scope.launch {
                                salaryRepository?.saveSalaryRecord(record)
                                saveSuccessMessage = "Payslip successfully saved to History Ledger!"
                            }
                        }
                    ) {
                        Text(if (batchResults.size > 1) "Save Current" else "Save to History")
                    }
                } else {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
