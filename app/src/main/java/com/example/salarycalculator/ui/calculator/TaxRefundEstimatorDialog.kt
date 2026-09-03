package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun TaxRefundEstimatorDialog(
    initialMonthlyGross: Double,
    taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    onDismiss: () -> Unit
) {
    var monthlyGrossInput by remember(initialMonthlyGross) {
        mutableStateOf(if (initialMonthlyGross > 0) initialMonthlyGross else 2500.0)
    }
    var monthsOnOldCode by remember { mutableStateOf(4) }
    var oldTaxCode by remember { mutableStateOf("BR") }
    var newTaxCode by remember { mutableStateOf("1257L") }

    val refundResult = remember(monthlyGrossInput, monthsOnOldCode, oldTaxCode, newTaxCode, taxRegion) {
        TaxRefundEstimator.estimate(
            monthlyGross = monthlyGrossInput,
            monthsOnOldCode = monthsOnOldCode,
            oldTaxCode = oldTaxCode,
            newTaxCode = newTaxCode,
            region = taxRegion
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Mid-Year Tax Refund Estimator",
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
                    text = "Estimate your one-off PAYE refund when your employer or HMRC updates your emergency or incorrect tax code midway through the tax year.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Monthly Salary Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Monthly Gross Salary:", style = MaterialTheme.typography.labelMedium)
                        Text("£${"%,.0f".format(monthlyGrossInput)} / mo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = monthlyGrossInput.toFloat().coerceIn(800f, 10000f),
                        onValueChange = { monthlyGrossInput = it.toDouble() },
                        valueRange = 800f..10000f
                    )
                }

                // Months on Previous Code Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Months on Old Code:", style = MaterialTheme.typography.labelMedium)
                        Text("$monthsOnOldCode months", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = monthsOnOldCode.toFloat(),
                        onValueChange = { monthsOnOldCode = it.toInt() },
                        valueRange = 1f..11f,
                        steps = 9
                    )
                }

                // Previous Tax Code Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Previous / Emergency Tax Code", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("BR", "0T", "1257L", "D0").forEach { code ->
                            FilterChip(
                                selected = oldTaxCode == code,
                                onClick = { oldTaxCode = code },
                                label = { Text(code) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // New Correct Tax Code Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("New Correct Tax Code", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1257L", "1383M", "1185L", "BR").forEach { code ->
                            FilterChip(
                                selected = newTaxCode == code,
                                onClick = { newTaxCode = code },
                                label = { Text(code) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Immediate Refund Hero Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (refundResult.immediatePaycheckRefund > 0)
                            Emerald60.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimated One-Off Refund",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Emerald60)
                        }

                        Text(
                            text = "£${"%,.2f".format(refundResult.immediatePaycheckRefund)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (refundResult.immediatePaycheckRefund > 0) Emerald60 else MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = refundResult.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Ongoing Monthly Impact
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                            Text("New Regular Monthly Net Pay:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(refundResult.newMonthlyTakeHome)}", fontWeight = FontWeight.Bold, color = Emerald60)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Previous Monthly Net Pay ($oldTaxCode):", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(refundResult.oldMonthlyTakeHome)}", fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Monthly Pay Increase:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("+£${"%,.2f".format(refundResult.ongoingMonthlyIncrease)} / mo", fontWeight = FontWeight.ExtraBold, color = Emerald60)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Done")
            }
        }
    )
}
