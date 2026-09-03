package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.DirectorDividendOptimizer
import com.example.salarycalculator.domain.DirectorScenarioResult
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun DirectorDividendDialog(
    onDismiss: () -> Unit
) {
    var profitInput by remember { mutableStateOf("60000") }
    val profit = remember(profitInput) { profitInput.toDoubleOrNull() ?: 0.0 }
    val report = remember(profit) { DirectorDividendOptimizer.generateOptimizationReport(profit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Director Salary vs Dividends",
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
                    text = "Compare UK Corporation Tax (19%–25%), Dividend Tax (8.75%–39.35%), and PAYE salary extraction to maximize net cash retained in your pocket.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Company Profit Input
                OutlinedTextField(
                    value = profitInput,
                    onValueChange = { profitInput = it },
                    label = { Text("Annual Company Gross Pre-Tax Profit (£)") },
                    prefix = { Text("£ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Savings Highlight Banner
                if (profit > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald60.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = Emerald60)
                            Column {
                                Text(
                                    text = "Optimal Annual Tax Savings: +£${"%,.2f".format(report.annualTaxSavingsVsPureSalary)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald60
                                )
                                Text(
                                    text = "Taking the optimal mix (£12,570 salary + dividends) saves significant National Insurance compared to taking 100% PAYE salary.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Scenario Cards
                Text("Remuneration Scenarios:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                report.scenarios.forEach { scenario ->
                    val isOptimal = scenario.name.contains("Optimal")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOptimal) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (isOptimal) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isOptimal) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald60, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = scenario.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOptimal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isOptimal) Emerald60 else MaterialTheme.colorScheme.outlineVariant
                                ) {
                                    Text(
                                        text = "${"%.1f".format(scenario.effectiveTaxRate)}% Effective Tax",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOptimal) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = scenario.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Director Salary:", style = MaterialTheme.typography.bodySmall)
                                Text("£${"%,.2f".format(scenario.directorSalary)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Dividends:", style = MaterialTheme.typography.bodySmall)
                                Text("£${"%,.2f".format(scenario.grossDividends)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Corporation Tax (19%-25%):", style = MaterialTheme.typography.bodySmall)
                                Text("£${"%,.2f".format(scenario.corporationTax)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Rose60)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Dividend Tax:", style = MaterialTheme.typography.bodySmall)
                                Text("£${"%,.2f".format(scenario.dividendTax)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Amber60)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Combined Tax Burden:", style = MaterialTheme.typography.bodySmall)
                                Text("£${"%,.2f".format(scenario.totalTaxBurden)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Rose60)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Cash Retained:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("£${"%,.2f".format(scenario.netCashInPocket)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Emerald60)
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
