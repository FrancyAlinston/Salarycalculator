package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.PictureAsPdf
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
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun Sa100Dialog(
    taxReport: SalaryReport? = null,
    taxCode: String = "1257L",
    historyRecords: List<MonthlySalaryRecord> = emptyList(),
    employerName: String = "Primary Employment",
    taxYearLabel: String = "2024/2025",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val summary = remember(taxReport, taxCode, historyRecords, employerName, taxYearLabel) {
        if (historyRecords.isNotEmpty()) {
            Sa100Generator.generateFromRecords(
                records = historyRecords,
                taxYearLabel = taxYearLabel,
                employerName = employerName
            )
        } else if (taxReport != null) {
            Sa100Generator.generateFromLive(
                report = taxReport,
                taxCode = taxCode,
                employerName = employerName,
                taxYearLabel = taxYearLabel
            )
        } else {
            Sa100Generator.generateFromRecords(
                records = emptyList(),
                taxYearLabel = taxYearLabel,
                employerName = employerName
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "HMRC Self-Assessment (SA100)",
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
                    text = "Summary of earnings mapped to official HMRC Employment Page (SA102) box numbers for your annual self-assessment tax return.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Meta card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Employer: ${summary.employerName}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("Tax Code: ${summary.employeeTaxCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tax Year ${summary.taxYearLabel}", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "Take-Home: £${"%,.2f".format(summary.netTakeHome)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald60
                            )
                        }
                    }
                }

                // Itemized Boxes
                for (box in summary.boxes) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(end = 2.dp)
                                ) {
                                    Text(
                                        text = box.boxNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = box.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "£${"%,.2f".format(box.amount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (box.isDeduction && box.amount > 0) Rose60 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        val pdfFile = Sa100Generator.generatePdf(context, summary)
                        Sa100Generator.sharePdf(context, pdfFile)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF", style = MaterialTheme.typography.labelMedium)
                }
                Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                    Text("Done")
                }
            }
        }
    )
}
