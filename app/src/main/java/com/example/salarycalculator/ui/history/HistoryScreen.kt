package com.example.salarycalculator.ui.history

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.*
import com.example.salarycalculator.ui.calculator.Sa100Dialog
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historyList by salaryRepository.getSalaryHistory().collectAsState(initial = emptyList())

    var recordToDelete by remember { mutableStateOf<MonthlySalaryRecord?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var showP60Dialog by remember { mutableStateOf(false) }
    var showSa100Dialog by remember { mutableStateOf(false) }
    var showReconciliationDialog by remember { mutableStateOf(false) }

    // Cumulative stats
    val totalNet = remember(historyList) { historyList.sumOf { it.netPay } }
    val totalGross = remember(historyList) { historyList.sumOf { it.grossPay } }
    val totalTax = remember(historyList) { historyList.sumOf { it.incomeTax } }
    val totalNI = remember(historyList) { historyList.sumOf { it.nationalInsurance } }
    val avgNet = remember(historyList) { if (historyList.isNotEmpty()) totalNet / historyList.size else 0.0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (historyList.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.HistoryEdu,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Text(
                            text = "No Salary History Saved",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Calculate your monthly salary and tap 'Save Record' on the Calculator screen to track your earnings, tax, and take-home pay month by month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header & Action Bar
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Salary History",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${historyList.size} month${if (historyList.size > 1) "s" else ""} recorded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalIconButton(onClick = { showReconciliationDialog = true }) {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        contentDescription = "Reconcile Bank Statement",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                FilledTonalIconButton(onClick = { showSa100Dialog = true }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = "HMRC SA100 Self-Assessment",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                FilledTonalIconButton(onClick = { showP60Dialog = true }) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = "Generate Annual P60",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (historyList.size >= 2) {
                                    FilledTonalIconButton(onClick = { showCompareDialog = true }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.CompareArrows,
                                            contentDescription = "Compare Periods",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        val file = CsvSalaryExporter.exportHistoryCsv(context, historyList)
                                        CsvSalaryExporter.shareCsv(context, file)
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.FileDownload,
                                        contentDescription = "Export CSV",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(onClick = { showClearAllDialog = true }) {
                                    Icon(
                                        Icons.Outlined.DeleteSweep,
                                        contentDescription = "Clear All History",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Earnings Trend Chart
                    item {
                        SalaryTrendChart(history = historyList)
                    }

                    // Cumulative Statistics Hero Card
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Cumulative Earnings Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Take-Home",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "£${"%,.2f".format(totalNet)}",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Emerald60,
                                            maxLines = 1
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "Avg Monthly Net",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "£${"%,.2f".format(avgNet)}",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatItem(label = "Total Gross", value = "£${"%,.0f".format(totalGross)}")
                                    StatItem(label = "Total Tax Paid", value = "£${"%,.0f".format(totalTax)}", valueColor = Rose60)
                                    StatItem(label = "Total NI Paid", value = "£${"%,.0f".format(totalNI)}", valueColor = Amber60)
                                }
                            }
                        }
                    }

                    // Month History Item Cards
                    items(historyList, key = { it.id }) { record ->
                        HistoryRecordCard(
                            record = record,
                            onExportPdf = {
                                val pdfFile = PdfPayslipGenerator.generatePayslipPdf(context, record)
                                PdfPayslipGenerator.sharePdf(context, pdfFile)
                            },
                            onDelete = { recordToDelete = record },
                            onShare = {
                                val shareText = """
                                    💰 Salary Record: ${record.monthYear}
                                    ---------------------------------------
                                    Gross Pay: £${"%.2f".format(record.grossPay)}
                                    Days Worked: ${record.daysWorked}d @ ${record.hoursPerDay}h/d (£${"%.2f".format(record.hourlyRate)}/hr)
                                    ${if (record.overtimeHours > 0) "Overtime: ${record.overtimeHours}h @ ${record.overtimeMultiplier}x\n" else ""}
                                    Deductions:
                                    ${if (record.salarySacrifice > 0) "• Salary Sacrifice: £${"%.2f".format(record.salarySacrifice)}\n" else ""}
                                    • Pension (${record.pensionRate}%): £${"%.2f".format(record.pensionContribution)}
                                    • PAYE Tax: £${"%.2f".format(record.incomeTax)}
                                    • National Insurance: £${"%.2f".format(record.nationalInsurance)}
                                    ${if (record.studentLoanDeduction > 0) "• Student Loan: £${"%.2f".format(record.studentLoanDeduction)}\n" else ""}
                                    💵 Net Take-Home: £${"%.2f".format(record.netPay)}
                                    ${if (record.note.isNotBlank()) "\nNote: ${record.note}" else ""}
                                    ---------------------------------------
                                """.trimIndent()

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share ${record.monthYear} Payslip"))
                            }
                        )
                    }
                }
            }
        }

        // Compare Months Dialog
        if (showCompareDialog) {
            MonthDiffDialog(
                historyList = historyList,
                onDismiss = { showCompareDialog = false }
            )
        }

        // Annual P60 Certificate Dialog
        if (showP60Dialog) {
            P60Dialog(
                historyList = historyList,
                onDismiss = { showP60Dialog = false }
            )
        }

        // Delete Single Record Confirmation Dialog
        recordToDelete?.let { record ->
            AlertDialog(
                onDismissRequest = { recordToDelete = null },
                title = { Text("Delete Salary Record") },
                text = { Text("Are you sure you want to remove the record for ${record.monthYear}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch { salaryRepository.deleteSalaryRecord(record.id) }
                            recordToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Clear All Records Confirmation Dialog
        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = { Text("Clear All Salary History") },
                text = { Text("This will permanently remove all ${historyList.size} saved monthly salary records. This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch { salaryRepository.clearSalaryHistory() }
                            showClearAllDialog = false
                        }
                    ) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // HMRC SA100 Return Dialog
        if (showSa100Dialog) {
            Sa100Dialog(
                historyRecords = historyList,
                taxYearLabel = "2024/2025",
                onDismiss = { showSa100Dialog = false }
            )
        }

        // Bank Statement CSV Reconciliation Dialog
        if (showReconciliationDialog) {
            BankReconciliationDialog(
                historyRecords = historyList,
                onDismiss = { showReconciliationDialog = false }
            )
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: MonthlySalaryRecord,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Header & Net Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = record.monthYear,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Gross: £${"%.2f".format(record.grossPay)} · ${record.daysWorked}d",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "£${"%.2f".format(record.netPay)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald60,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Mini Visual Distribution Bar
            if (record.grossPay > 0) {
                val netRatio = (record.netPay / record.grossPay).toFloat()
                val pensionRatio = (record.pensionContribution / record.grossPay).toFloat()
                val taxRatio = (record.incomeTax / record.grossPay).toFloat()
                val niRatio = (record.nationalInsurance / record.grossPay).toFloat()
                val studentLoanRatio = (record.studentLoanDeduction / record.grossPay).toFloat()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (netRatio > 0) Box(modifier = Modifier.weight(netRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Emerald60))
                    if (pensionRatio > 0) Box(modifier = Modifier.weight(pensionRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Teal60))
                    if (taxRatio > 0) Box(modifier = Modifier.weight(taxRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Rose60))
                    if (niRatio > 0) Box(modifier = Modifier.weight(niRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Amber60))
                    if (studentLoanRatio > 0) Box(modifier = Modifier.weight(studentLoanRatio.coerceAtLeast(0.001f)).fillMaxHeight().background(Violet60))
                }
            }

            // Expandable Detailed Breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    HistoryDetailRow(label = "Standard Hourly Rate", value = "£${"%.2f".format(record.hourlyRate)}/hr")
                    HistoryDetailRow(
                        label = "Schedule",
                        value = "${record.daysWorked}d × ${record.hoursPerDay}h (${"%.1f".format(record.daysWorked * record.hoursPerDay)}h)"
                    )
                    if (record.overtimeHours > 0) {
                        HistoryDetailRow(
                            label = "Overtime",
                            value = "${record.overtimeHours}h @ ${record.overtimeMultiplier}x"
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    HistoryDetailRow(label = "Gross Total Pay", value = "£${"%.2f".format(record.grossPay)}", isBold = true)
                    if (record.salarySacrifice > 0) {
                        HistoryDetailRow(
                            label = "Salary Sacrifice Schemes",
                            value = "-£${"%.2f".format(record.salarySacrifice)}",
                            valueColor = Rose60
                        )
                    }
                    if (record.pensionContribution > 0) {
                        HistoryDetailRow(
                            label = "Employee Pension (${record.pensionRate}%)",
                            value = "-£${"%.2f".format(record.pensionContribution)}",
                            valueColor = Teal60
                        )
                    }
                    HistoryDetailRow(
                        label = "PAYE Income Tax (${if (record.taxRegion == TaxRegion.SCOTLAND) "Scotland" else "UK"})",
                        value = "-£${"%.2f".format(record.incomeTax)}",
                        valueColor = Rose60
                    )
                    HistoryDetailRow(
                        label = "Class 1 National Insurance",
                        value = "-£${"%.2f".format(record.nationalInsurance)}",
                        valueColor = Amber60
                    )
                    if (record.studentLoanDeduction > 0) {
                        HistoryDetailRow(
                            label = "Student Loan (${record.studentLoanPlan.name.replace("_", " ")})",
                            value = "-£${"%.2f".format(record.studentLoanDeduction)}",
                            valueColor = Violet60
                        )
                    }

                    if (record.note.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📝 Note: ${record.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Card Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onExportPdf,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryDetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
