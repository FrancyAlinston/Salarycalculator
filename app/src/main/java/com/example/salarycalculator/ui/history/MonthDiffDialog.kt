package com.example.salarycalculator.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.MonthlySalaryRecord
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDiffDialog(
    historyList: List<MonthlySalaryRecord>,
    onDismiss: () -> Unit
) {
    if (historyList.size < 2) return

    var selectedIndexA by remember { mutableIntStateOf(0) }
    var selectedIndexB by remember { mutableIntStateOf(minOf(1, historyList.size - 1)) }

    val recordA = historyList.getOrNull(selectedIndexA) ?: historyList[0]
    val recordB = historyList.getOrNull(selectedIndexB) ?: historyList[1]

    val netDelta = recordB.netPay - recordA.netPay
    val netPctDelta = if (recordA.netPay > 0) (netDelta / recordA.netPay) * 100.0 else 0.0
    val grossDelta = recordB.grossPay - recordA.grossPay
    val taxDelta = recordB.incomeTax - recordA.incomeTax
    val niDelta = recordB.nationalInsurance - recordA.nationalInsurance
    val pensionDelta = recordB.pensionContribution - recordA.pensionContribution

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Compare Pay Periods",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Period Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Month A
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Base Period", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        var expandedA by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedA, onExpandedChange = { expandedA = !expandedA }) {
                            OutlinedTextField(
                                value = recordA.monthYear,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedA) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            ExposedDropdownMenu(expanded = expandedA, onDismissRequest = { expandedA = false }) {
                                historyList.forEachIndexed { index, item ->
                                    DropdownMenuItem(
                                        text = { Text(item.monthYear, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            selectedIndexA = index
                                            expandedA = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Month B
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Comparison Period", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        var expandedB by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedB, onExpandedChange = { expandedB = !expandedB }) {
                            OutlinedTextField(
                                value = recordB.monthYear,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedB) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            ExposedDropdownMenu(expanded = expandedB, onDismissRequest = { expandedB = false }) {
                                historyList.forEachIndexed { index, item ->
                                    DropdownMenuItem(
                                        text = { Text(item.monthYear, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            selectedIndexB = index
                                            expandedB = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary Hero Card of Difference
                Surface(
                    color = if (netDelta >= 0) Emerald60.copy(alpha = 0.15f) else Rose60.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Take-Home Difference", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${if (netDelta >= 0) "+" else ""}£${"%,.2f".format(netDelta)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netDelta >= 0) Emerald60 else Rose60
                            )
                        }

                        Surface(
                            color = if (netDelta >= 0) Emerald60 else Rose60,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (netDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${if (netPctDelta >= 0) "+" else ""}${"%.1f".format(netPctDelta)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Detailed Itemized Comparison Table
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiffRow(
                        label = "Gross Total Pay",
                        valA = "£${"%,.2f".format(recordA.grossPay)}",
                        valB = "£${"%,.2f".format(recordB.grossPay)}",
                        delta = grossDelta
                    )
                    DiffRow(
                        label = "Pension Relief",
                        valA = "£${"%,.2f".format(recordA.pensionContribution)}",
                        valB = "£${"%,.2f".format(recordB.pensionContribution)}",
                        delta = pensionDelta,
                        invertColors = true
                    )
                    DiffRow(
                        label = "PAYE Income Tax",
                        valA = "£${"%,.2f".format(recordA.incomeTax)}",
                        valB = "£${"%,.2f".format(recordB.incomeTax)}",
                        delta = taxDelta,
                        invertColors = true
                    )
                    DiffRow(
                        label = "Class 1 NI",
                        valA = "£${"%,.2f".format(recordA.nationalInsurance)}",
                        valB = "£${"%,.2f".format(recordB.nationalInsurance)}",
                        delta = niDelta,
                        invertColors = true
                    )
                    DiffRow(
                        label = "Net Take-Home",
                        valA = "£${"%,.2f".format(recordA.netPay)}",
                        valB = "£${"%,.2f".format(recordB.netPay)}",
                        delta = netDelta,
                        isBold = true
                    )
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

@Composable
private fun DiffRow(
    label: String,
    valA: String,
    valB: String,
    delta: Double,
    isBold: Boolean = false,
    invertColors: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = label,
                style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$valA → $valB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val isPositiveGood = !invertColors
        val deltaColor = if (delta == 0.0) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else if ((delta > 0 && isPositiveGood) || (delta < 0 && !isPositiveGood)) {
            Emerald60
        } else {
            Rose60
        }

        Text(
            text = "${if (delta > 0) "+" else ""}£${"%,.2f".format(delta)}",
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = deltaColor,
            maxLines = 1
        )
    }
}
