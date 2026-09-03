package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.SelfEmployedTaxEngine
import com.example.salarycalculator.domain.TaxRegion
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Violet60

@Composable
fun SelfEmployedTaxDialog(
    initialPayeGross: Double = 0.0,
    initialPayeTax: Double = 0.0,
    taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    onDismiss: () -> Unit
) {
    var turnoverInput by remember { mutableStateOf("25000") }
    var expensesInput by remember { mutableStateOf("3000") }
    var payeGrossInput by remember { mutableStateOf(if (initialPayeGross > 0) "%.0f".format(initialPayeGross) else "0") }
    var payeTaxInput by remember { mutableStateOf(if (initialPayeTax > 0) "%.0f".format(initialPayeTax) else "0") }
    var useTradingAllowance by remember { mutableStateOf(true) }

    val turnover = turnoverInput.toDoubleOrNull() ?: 0.0
    val expenses = expensesInput.toDoubleOrNull() ?: 0.0
    val payeGross = payeGrossInput.toDoubleOrNull() ?: 0.0
    val payeTax = payeTaxInput.toDoubleOrNull() ?: 0.0

    val report = remember(turnover, expenses, payeGross, payeTax, useTradingAllowance, taxRegion) {
        SelfEmployedTaxEngine.calculateSelfEmployedTax(
            employmentGross = payeGross,
            employmentTaxPaid = payeTax,
            turnover = turnover,
            expenses = expenses,
            useTradingAllowanceIfBetter = useTradingAllowance,
            taxRegion = taxRegion
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Self-Employed & Payments on Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                // Summary Metric Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (report.paymentsOnAccountRequired) Amber60.copy(alpha = 0.12f)
                        else Emerald60.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (report.paymentsOnAccountRequired) "📅 Payments on Account Triggered"
                            else "✅ Standard Self Assessment Bill",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (report.paymentsOnAccountRequired) Amber60 else Emerald60
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total SA Tax Liability", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.2f".format(report.totalSelfAssessmentLiability)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Rose60)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Trading Profit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.2f".format(report.netSelfEmployedProfit)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Emerald60)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Class 4 NI (6% / 2%):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("£${"%,.2f".format(report.class4Ni)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Income Tax Due:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("£${"%,.2f".format(report.remainingIncomeTaxDue)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }

                        if (report.paymentsOnAccountRequired) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("1st POA (Due 31 Jan):", style = MaterialTheme.typography.bodySmall, color = Amber60, fontWeight = FontWeight.SemiBold)
                                Text("£${"%,.2f".format(report.firstPaymentOnAccountJan31)}", style = MaterialTheme.typography.bodySmall, color = Amber60, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("2nd POA (Due 31 Jul):", style = MaterialTheme.typography.bodySmall, color = Amber60, fontWeight = FontWeight.SemiBold)
                                Text("£${"%,.2f".format(report.secondPaymentOnAccountJul31)}", style = MaterialTheme.typography.bodySmall, color = Amber60, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Jan 31 Cash Outlay:", style = MaterialTheme.typography.bodySmall, color = Rose60, fontWeight = FontWeight.Bold)
                                Text("£${"%,.2f".format(report.totalFirstYearCashOutlayJan31)}", style = MaterialTheme.typography.bodySmall, color = Rose60, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                // Inputs
                Text("Self-Employment Finances:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = turnoverInput,
                        onValueChange = { turnoverInput = it },
                        label = { Text("Gross Turnover") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = expensesInput,
                        onValueChange = { expensesInput = it },
                        label = { Text("Allowable Expenses") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text("Side-Hustle / PAYE Employment (Optional):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = payeGrossInput,
                        onValueChange = { payeGrossInput = it },
                        label = { Text("PAYE Salary") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = payeTaxInput,
                        onValueChange = { payeTaxInput = it },
                        label = { Text("PAYE Tax Paid") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Advisory Notes
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    report.notes.forEach { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
