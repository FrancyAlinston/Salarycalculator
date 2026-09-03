package com.example.salarycalculator.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.MonthlySalaryRecord
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60

data class MonthlyVarianceItem(
    val currentMonth: MonthlySalaryRecord,
    val previousMonth: MonthlySalaryRecord?,
    val grossDelta: Double,
    val grossPctDelta: Double,
    val netDelta: Double,
    val netPctDelta: Double,
    val taxDelta: Double,
    val niDelta: Double,
    val pensionDelta: Double,
    val overtimeHoursDelta: Double
)

@Composable
fun PayslipVarianceDialog(
    history: List<MonthlySalaryRecord>,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, NET, GROSS, TAX

    // Sort chronologically ascending
    val sortedHistory = remember(history) {
        history.sortedBy { it.monthYear }
    }

    val varianceList = remember(sortedHistory) {
        sortedHistory.mapIndexed { index, current ->
            val prev = if (index > 0) sortedHistory[index - 1] else null
            val gDelta = if (prev != null) current.grossPay - prev.grossPay else 0.0
            val gPct = if (prev != null && prev.grossPay > 0) (gDelta / prev.grossPay) * 100.0 else 0.0
            val nDelta = if (prev != null) current.netPay - prev.netPay else 0.0
            val nPct = if (prev != null && prev.netPay > 0) (nDelta / prev.netPay) * 100.0 else 0.0
            val tDelta = if (prev != null) current.incomeTax - prev.incomeTax else 0.0
            val niDelta = if (prev != null) current.nationalInsurance - prev.nationalInsurance else 0.0
            val pDelta = if (prev != null) current.pensionContribution - prev.pensionContribution else 0.0
            val otDelta = if (prev != null) current.overtimeHours - prev.overtimeHours else 0.0

            MonthlyVarianceItem(
                currentMonth = current,
                previousMonth = prev,
                grossDelta = gDelta,
                grossPctDelta = gPct,
                netDelta = nDelta,
                netPctDelta = nPct,
                taxDelta = tDelta,
                niDelta = niDelta,
                pensionDelta = pDelta,
                overtimeHoursDelta = otDelta
            )
        }.reversed() // Show newest on top
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.QueryStats,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Month-over-Month Variance Heatmap",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All Variations",
                        "NET" to "Net Take-Home",
                        "GROSS" to "Gross Pay",
                        "TAX" to "PAYE & NI Tax"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (varianceList.isEmpty()) {
                    Text(
                        text = "No recorded payslips available to compute variance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(varianceList) { item ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.currentMonth.monthYear,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (item.previousMonth != null) {
                                            Surface(
                                                color = if (item.netDelta >= 0) Emerald60.copy(alpha = 0.15f) else Rose60.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.netDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                        contentDescription = null,
                                                        tint = if (item.netDelta >= 0) Emerald60 else Rose60,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "${if (item.netDelta >= 0) "+" else ""}${"%.1f".format(item.netPctDelta)}% Net",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (item.netDelta >= 0) Emerald60 else Rose60,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        } else {
                                            Text("Baseline Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Metrics Grid
                                    if (selectedFilter == "ALL" || selectedFilter == "NET") {
                                        VarianceRow(
                                            label = "Take-Home Pay",
                                            currentVal = "£${"%,.2f".format(item.currentMonth.netPay)}",
                                            delta = item.netDelta,
                                            hasPrev = item.previousMonth != null,
                                            positiveIsGood = true
                                        )
                                    }

                                    if (selectedFilter == "ALL" || selectedFilter == "GROSS") {
                                        VarianceRow(
                                            label = "Gross Earnings",
                                            currentVal = "£${"%,.2f".format(item.currentMonth.grossPay)}",
                                            delta = item.grossDelta,
                                            hasPrev = item.previousMonth != null,
                                            positiveIsGood = true
                                        )
                                    }

                                    if (selectedFilter == "ALL" || selectedFilter == "TAX") {
                                        VarianceRow(
                                            label = "PAYE Tax & NI",
                                            currentVal = "£${"%,.2f".format(item.currentMonth.incomeTax + item.currentMonth.nationalInsurance)}",
                                            delta = item.taxDelta + item.niDelta,
                                            hasPrev = item.previousMonth != null,
                                            positiveIsGood = false
                                        )
                                    }

                                    if (item.currentMonth.overtimeHours > 0 || item.overtimeHoursDelta != 0.0) {
                                        VarianceRow(
                                            label = "Overtime Volume",
                                            currentVal = "${item.currentMonth.overtimeHours} hrs",
                                            delta = item.overtimeHoursDelta,
                                            hasPrev = item.previousMonth != null,
                                            unit = "h",
                                            positiveIsGood = true
                                        )
                                    }
                                }
                            }
                        }
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

@Composable
private fun VarianceRow(
    label: String,
    currentVal: String,
    delta: Double,
    hasPrev: Boolean,
    unit: String = "",
    positiveIsGood: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(currentVal, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            if (hasPrev && delta != 0.0) {
                val isPositive = delta > 0
                val color = if ((isPositive && positiveIsGood) || (!isPositive && !positiveIsGood)) Emerald60 else Rose60
                val sign = if (isPositive) "+" else ""
                val deltaText = if (unit == "h") "$sign${"%.1f".format(delta)}h" else "$sign£${"%,.2f".format(delta)}"

                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
