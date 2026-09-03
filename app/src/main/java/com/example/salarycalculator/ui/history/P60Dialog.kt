package com.example.salarycalculator.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.MonthlySalaryRecord
import com.example.salarycalculator.domain.P60Generator
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun P60Dialog(
    historyList: List<MonthlySalaryRecord>,
    onDismiss: () -> Unit
) {
    if (historyList.isEmpty()) return

    val context = LocalContext.current
    var taxYearLabel by remember { mutableStateOf("2024/2025") }
    var employeeName by remember { mutableStateOf("Valued Employee") }
    var employerName by remember { mutableStateOf("Primary Employment Ltd") }
    var payeReference by remember { mutableStateOf("120/AB54321") }

    val totalGross = remember(historyList) { historyList.sumOf { it.grossPay } }
    val totalTax = remember(historyList) { historyList.sumOf { it.incomeTax } }
    val totalNI = remember(historyList) { historyList.sumOf { it.nationalInsurance } }
    val totalNet = remember(historyList) { historyList.sumOf { it.netPay } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Generate Annual P60",
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
                    text = "Generates an official HMRC-styled P60 End-of-Year Certificate summarizing your ${historyList.size} recorded pay periods.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Annual Totals Hero Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Pay in Year", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(totalGross)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Income Tax", style = MaterialTheme.typography.bodySmall)
                            Text("-£${"%,.2f".format(totalTax)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Rose60)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Class 1 NI", style = MaterialTheme.typography.bodySmall)
                            Text("-£${"%,.2f".format(totalNI)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Annual Net Pay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("£${"%,.2f".format(totalNet)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Emerald60)
                        }
                    }
                }

                OutlinedTextField(
                    value = taxYearLabel,
                    onValueChange = { taxYearLabel = it },
                    label = { Text("Tax Year") },
                    placeholder = { Text("e.g. 2024/2025") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = employeeName,
                    onValueChange = { employeeName = it },
                    label = { Text("Employee Full Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = employerName,
                    onValueChange = { employerName = it },
                    label = { Text("Employer Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = payeReference,
                    onValueChange = { payeReference = it },
                    label = { Text("Employer PAYE Reference") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p60File = P60Generator.generateP60Pdf(
                        context = context,
                        taxYearLabel = taxYearLabel.trim().ifBlank { "2024/2025" },
                        records = historyList,
                        employeeName = employeeName.trim().ifBlank { "Valued Employee" },
                        employerName = employerName.trim().ifBlank { "Primary Employment Ltd" },
                        payeRef = payeReference.trim().ifBlank { "120/AB54321" }
                    )
                    P60Generator.shareP60(context, p60File)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export P60 PDF", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
