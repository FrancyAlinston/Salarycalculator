package com.example.salarycalculator.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.*

@Composable
fun SalaryForecastDialog(
    history: List<MonthlySalaryRecord>,
    hourlyRate: Double,
    hoursPerWeek: Double,
    taxCode: String,
    taxRegion: TaxRegion,
    studentLoanPlan: StudentLoanPlan,
    pensionRate: Double,
    onDismiss: () -> Unit
) {
    val analysis = remember(history, hourlyRate, hoursPerWeek, taxCode, taxRegion, studentLoanPlan, pensionRate) {
        SalaryForecastEngine.computeSalaryForecast(
            history = history,
            currentHourlyRate = hourlyRate,
            currentHoursPerWeek = hoursPerWeek,
            taxCode = taxCode,
            taxRegion = taxRegion,
            studentLoanPlan = studentLoanPlan,
            pensionRate = pensionRate
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
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ML Salary & Tax Forecast",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Model Accuracy & Trend Header Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Regression Model",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Surface(
                                    color = if (analysis.rSquaredConfidence >= 0.8) Emerald60 else MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "R² Fit: ${"%.0f".format(analysis.rSquaredConfidence * 100)}%",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = analysis.optimizationSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Projected Full-Year Totals Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Projected Annual Totals (12 Months)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Projected Gross", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "£${"%,.2f".format(analysis.projectedAnnualGross)}",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Projected Take-Home", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "£${"%,.2f".format(analysis.projectedAnnualNet)}",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald60
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Projected PAYE Tax: £${"%,.2f".format(analysis.projectedAnnualTaxLiability)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "NI: £${"%,.2f".format(analysis.projectedAnnualNiLiability)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                "Effective Annual Tax Rate: ${"%.1f".format(analysis.effectiveTaxRatePercent)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Tax Rebate / Underpayment Forecast Card
                item {
                    val isRebate = analysis.estimatedPayeRebateOrDebt >= 0
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRebate) Emerald80.copy(alpha = 0.2f) else Rose80.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isRebate) Icons.Default.Savings else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isRebate) Emerald60 else Rose60
                            )
                            Column {
                                Text(
                                    text = if (isRebate) "Estimated HMRC Rebate" else "Estimated Tax Underpayment",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "£${"%,.2f".format(Math.abs(analysis.estimatedPayeRebateOrDebt))}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRebate) Emerald60 else Rose60
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Monthly 12-Month Trajectory
                item {
                    Text(
                        text = "12-Month Monthly Projection",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(analysis.forecastTimeline) { point ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (point.isHistorical) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (point.isHistorical) Slate600 else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = point.monthLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (point.isHistorical) "(Logged)" else "(Forecast)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Gross: £${"%,.0f".format(point.grossPay)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Net: £${"%,.0f".format(point.projectedNet)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Emerald60
                                )
                            }
                        }
                    }
                }

                // 5. Recommendations
                item {
                    Text(
                        text = "HMRC Tax Recommendations",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(analysis.keyRecommendations) { rec ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = rec,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
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
