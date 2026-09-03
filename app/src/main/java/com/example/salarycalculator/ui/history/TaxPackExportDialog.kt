package com.example.salarycalculator.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.MonthlySalaryRecord
import com.example.salarycalculator.domain.TaxPackZipExporter
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Amber60
import java.util.Calendar

@Composable
fun TaxPackExportDialog(
    historyRecords: List<MonthlySalaryRecord>,
    annualShifts: Map<Int, Map<Int, Double>> = emptyMap(),
    taxYearLabel: String = "2024/2025",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalGross = remember(historyRecords) { historyRecords.sumOf { it.grossPay } }
    val totalTax = remember(historyRecords) { historyRecords.sumOf { it.incomeTax } }
    val totalNI = remember(historyRecords) { historyRecords.sumOf { it.nationalInsurance } }
    val totalNet = remember(historyRecords) { historyRecords.sumOf { it.netPay } }

    var isGenerating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Annual Tax Pack Exporter",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Package your full tax year records into a single complete ZIP bundle containing all statutory PDF certificates, tax returns, shift calendars, and CSV spreadsheets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Tax Year Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Tax Year $taxYearLabel Totals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Gross Income:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(totalGross)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total PAYE Tax Deducted:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(totalTax)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Rose60)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total National Insurance:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(totalNI)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Amber60)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Take-Home Volume:", style = MaterialTheme.typography.bodyMedium)
                            Text("£${"%,.2f".format(totalNet)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Emerald60)
                        }
                    }
                }

                // Included Files Checklist
                Text("Bundled Files Included in Archive:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    IncludedFileItem(Icons.Default.Description, "1_Annual_P60_Certificate.pdf", "Official HMRC Year-End Certificate")
                    IncludedFileItem(Icons.AutoMirrored.Filled.Assignment, "2_HMRC_SA100_SelfAssessment.pdf", "SA100/SA102 Box 1-7 Tax Return")
                    IncludedFileItem(Icons.Default.CalendarMonth, "3_Full_Year_Shift_Calendar.ics", "RFC 5545 iCalendar (Google/Apple Cal)")
                    IncludedFileItem(Icons.Outlined.FileDownload, "4_Payroll_History_Ledger.csv", "Raw spreadsheet data for Excel/Sheets")
                    IncludedFileItem(Icons.Default.PictureAsPdf, "5_Annual_Shift_Poster.pdf", "Printable 12-Month Shift Calendar Poster")
                    IncludedFileItem(Icons.Default.Info, "README_Tax_Pack.txt", "Plain-text payroll audit summary")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isGenerating = true
                    val zip = TaxPackZipExporter.createTaxPackZip(
                        context = context,
                        historyRecords = historyRecords,
                        annualShifts = annualShifts,
                        taxYearLabel = taxYearLabel,
                        year = Calendar.getInstance().get(Calendar.YEAR)
                    )
                    isGenerating = false
                    TaxPackZipExporter.shareZip(context, zip)
                    onDismiss()
                },
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("Export & Share ZIP Pack")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun IncludedFileItem(icon: ImageVector, fileName: String, description: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(fileName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
