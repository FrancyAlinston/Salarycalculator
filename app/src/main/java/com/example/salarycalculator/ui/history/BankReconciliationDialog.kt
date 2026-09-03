package com.example.salarycalculator.ui.history

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.BankReconciliationEngine
import com.example.salarycalculator.domain.MonthlySalaryRecord
import com.example.salarycalculator.domain.ReconciliationStatus
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun BankReconciliationDialog(
    historyRecords: List<MonthlySalaryRecord>,
    onDismiss: () -> Unit
) {
    var csvInput by remember { mutableStateOf(BankReconciliationEngine.generateSampleCsv(historyRecords)) }
    var hasReconciled by remember { mutableStateOf(false) }

    val reconciliationResult = remember(csvInput, hasReconciled, historyRecords) {
        val transactions = BankReconciliationEngine.parseCsv(csvInput)
        BankReconciliationEngine.reconcile(transactions, historyRecords)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Bank Statement Reconciliation",
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
                    text = "Import or paste bank CSV transactions (Date, Description, Amount) to automatically verify and reconcile incoming salary deposits against recorded payslips.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // CSV Input Area
                OutlinedTextField(
                    value = csvInput,
                    onValueChange = { csvInput = it },
                    label = { Text("Bank Statement CSV (Date, Description, Amount)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            csvInput = BankReconciliationEngine.generateSampleCsv(historyRecords)
                            hasReconciled = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sample CSV", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { hasReconciled = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reconcile", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Summary Dashboard
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Reconciliation Overview", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Credits Detected:", style = MaterialTheme.typography.bodySmall)
                            Text("${reconciliationResult.totalCreditsFound}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Matched Against Payslips:", style = MaterialTheme.typography.bodySmall)
                            Text("${reconciliationResult.totalMatchedCount} exact matches", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Emerald60)
                        }

                        if (reconciliationResult.totalDiscrepanciesCount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Variances / Discrepancies:", style = MaterialTheme.typography.bodySmall)
                                Text("${reconciliationResult.totalDiscrepanciesCount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Amber60)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Reconciled Volume:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(reconciliationResult.totalReconciledAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Itemized Reconciliation Results
                Text("Reconciled Transactions:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reconciliationResult.items.forEach { item ->
                        val (statusColor, statusIcon) = when (item.status) {
                            ReconciliationStatus.EXACT_MATCH -> Pair(Emerald60, Icons.Default.CheckCircle)
                            ReconciliationStatus.VARIANCE_DETECTED -> Pair(Amber60, Icons.Default.Warning)
                            else -> Pair(Rose60, Icons.Default.Warning)
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(item.bankDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("£${"%,.2f".format(item.bankAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusColor
                                        ) {
                                            Text(
                                                text = item.status.displayName.split(" (")[0],
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = item.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
