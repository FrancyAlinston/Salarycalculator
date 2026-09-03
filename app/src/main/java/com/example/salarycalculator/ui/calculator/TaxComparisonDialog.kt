package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60
import kotlin.math.max

@Composable
fun TaxComparisonDialog(
    initialGrossAmount: Double,
    isMonthly: Boolean,
    taxRegion: TaxRegion,
    taxCode: String = "1257L",
    pensionRatePercent: Double = 5.0,
    onDismiss: () -> Unit
) {
    var grossInput by remember(initialGrossAmount) {
        mutableStateOf(if (initialGrossAmount > 0) initialGrossAmount else 2500.0)
    }
    var selectedRegion by remember(taxRegion) { mutableStateOf(taxRegion) }

    val comparison = remember(grossInput, isMonthly, selectedRegion, taxCode, pensionRatePercent) {
        TaxYearComparisonCalculator.compare(
            grossAmount = grossInput,
            taxCode = taxCode,
            isMonthly = isMonthly,
            region = selectedRegion,
            pensionRatePercent = pensionRatePercent
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Multi-Year Tax Comparison",
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
                    text = "Compare your net take-home and tax deductions across 2023/24, 2024/25, and 2025/26 tax regimes (reflecting the historical 12% to 8% NI cuts).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Gross Amount Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gross Pay (${if (isMonthly) "Monthly" else "Annual"}):", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "£${"%,.0f".format(grossInput)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val maxSlider = if (isMonthly) 10000f else 120000f
                    val minSlider = if (isMonthly) 800f else 10000f
                    Slider(
                        value = grossInput.toFloat().coerceIn(minSlider, maxSlider),
                        onValueChange = { grossInput = it.toDouble() },
                        valueRange = minSlider..maxSlider
                    )
                }

                // Visual Comparative Bar Chart
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Net Take-Home vs Tax Paid",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        ) {
                            val barWidth = size.width / (comparison.summaries.size * 2.5f)
                            val maxVal = comparison.summaries.maxOfOrNull { it.grossPay } ?: 1.0

                            comparison.summaries.forEachIndexed { index, item ->
                                val groupX = index * (size.width / comparison.summaries.size) + (barWidth * 0.5f)
                                val netBarHeight = ((item.netPay / maxVal) * (size.height - 20f)).toFloat()
                                val taxBarHeight = (((item.incomeTax + item.nationalInsurance) / maxVal) * (size.height - 20f)).toFloat()

                                // Net Pay Bar (Emerald)
                                drawRoundRect(
                                    color = Emerald60,
                                    topLeft = Offset(groupX, size.height - netBarHeight),
                                    size = Size(barWidth, netBarHeight),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )

                                // Tax + NI Bar (Rose)
                                drawRoundRect(
                                    color = Rose60,
                                    topLeft = Offset(groupX + barWidth + 4f, size.height - taxBarHeight),
                                    size = Size(barWidth, taxBarHeight),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (item in comparison.summaries) {
                                Text(
                                    text = item.yearLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Emerald60))
                                Text("Take-Home", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Rose60))
                                Text("Tax + NI", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Side-by-side Year Cards
                for (summary in comparison.summaries) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (summary.yearLabel == comparison.bestYearLabel)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = summary.yearLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (summary.yearLabel == comparison.bestYearLabel) {
                                        Badge(containerColor = Emerald60) {
                                            Text("Top Take-Home", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                                Text(
                                    text = "£${"%,.2f".format(summary.netPay)} ${if (isMonthly) "/mo" else "/yr"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Emerald60
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PAYE Tax: £${"%,.0f".format(summary.incomeTax)} · Class 1 NI: £${"%,.0f".format(summary.nationalInsurance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${"%.1f".format(summary.effectiveTaxRate)}% effective rate",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (summary.annualSavingsVs2023 > 0.0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Emerald60, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "+£${"%,.2f".format(summary.annualSavingsVs2023)}/yr extra take-home vs 2023/24",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Emerald60,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
