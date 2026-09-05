package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.*

@Composable
fun OvertimeTaxOptimizerDialog(
    baseGrossMonthly: Double,
    baseHourlyRate: Double,
    taxCode: String,
    taxRegion: TaxRegion,
    taxYear: TaxYear,
    pensionRate: Double,
    studentLoanPlan: StudentLoanPlan,
    hasMarriageAllowance: Boolean,
    hasBlindPersonsAllowance: Boolean,
    defaultOvertimeMultiplier: Double = 1.5,
    onDismiss: () -> Unit
) {
    var extraHours by remember { mutableStateOf(10.0) }
    var selectedMultiplier by remember { mutableStateOf(defaultOvertimeMultiplier) }

    val multipliers = listOf(1.0, 1.25, 1.5, 1.75, 2.0, 2.5)

    val result = remember(
        baseGrossMonthly,
        baseHourlyRate,
        extraHours,
        selectedMultiplier,
        taxCode,
        taxRegion,
        taxYear,
        pensionRate,
        studentLoanPlan,
        hasMarriageAllowance,
        hasBlindPersonsAllowance
    ) {
        OvertimeOptimizerEngine.calculateOvertimeReturn(
            baseGrossMonthly = baseGrossMonthly,
            baseHourlyRate = baseHourlyRate,
            extraOtHours = extraHours,
            overtimeMultiplier = selectedMultiplier,
            taxCode = taxCode,
            taxRegion = taxRegion,
            taxYear = taxYear,
            pensionRate = pensionRate,
            studentLoanPlan = studentLoanPlan,
            hasMarriageAllowance = hasMarriageAllowance,
            hasBlindPersonsAllowance = hasBlindPersonsAllowance
        )
    }

    val efficiencyColor = when (result.efficiencyRating) {
        OvertimeEfficiencyRating.HIGH -> Emerald60
        OvertimeEfficiencyRating.MODERATE -> Amber60
        OvertimeEfficiencyRating.LOW -> Rose60
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Overtime Return Optimizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Marginal Take-Home & Net Return / Hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Card: Extra Net Pay & Net/Hour
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Extra Monthly Take-Home",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+£${"%,.2f".format(result.extraNetPay)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald60
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Net Return / Hour",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "£${"%.2f".format(result.netPerHour)} / hr",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(Gross: £${"%.2f".format(baseHourlyRate * selectedMultiplier)}/hr)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Retained",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${"%.1f".format(result.retentionPercentage)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = efficiencyColor
                                )
                                Text(
                                    text = "Tax Drag: ${"%.1f".format(result.marginalDeductionPercentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Efficiency Rating Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = efficiencyColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(efficiencyColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = result.efficiencyRating.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = efficiencyColor
                                )
                            }
                        }
                    }
                }

                // Tax Trap Warning if applicable
                if (result.taxTrapWarning != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Rose60.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Rose60,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = result.taxTrapWarning!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Extra Overtime Hours Slider & Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extra Overtime Hours",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${extraHours.toInt()} hrs",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = extraHours.toFloat(),
                        onValueChange = { extraHours = it.toDouble() },
                        valueRange = 0f..40f,
                        steps = 39,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Hour Preset Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(4.0, 8.0, 12.0, 16.0, 24.0).forEach { h ->
                            val isSelected = extraHours == h
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { extraHours = h }
                            ) {
                                Text(
                                    text = "+${h.toInt()}h",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Overtime Multiplier Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Overtime Rate Multiplier",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        multipliers.forEach { mult ->
                            val isSelected = selectedMultiplier == mult
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMultiplier = mult }
                            ) {
                                Text(
                                    text = "${mult}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Marginal Deductions Breakdown Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Marginal Payroll Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        BreakdownRow(
                            label = "Extra Gross Earned",
                            amount = "+£${"%,.2f".format(result.extraGross)}",
                            color = MaterialTheme.colorScheme.onSurface,
                            isBold = true
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        BreakdownRow(
                            label = "Extra PAYE Income Tax",
                            amount = "-£${"%,.2f".format(result.extraTax)}",
                            color = Rose60
                        )

                        BreakdownRow(
                            label = "Extra Class 1 NI",
                            amount = "-£${"%,.2f".format(result.extraNi)}",
                            color = Amber60
                        )

                        if (result.extraStudentLoan > 0.0) {
                            BreakdownRow(
                                label = "Extra Student Loan",
                                amount = "-£${"%,.2f".format(result.extraStudentLoan)}",
                                color = Teal60
                            )
                        }

                        if (result.extraPension > 0.0) {
                            BreakdownRow(
                                label = "Extra Pension Contribution",
                                amount = "-£${"%,.2f".format(result.extraPension)}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        BreakdownRow(
                            label = "Extra Total Deductions",
                            amount = "-£${"%,.2f".format(result.extraTotalDeductions)}",
                            color = Rose60,
                            isBold = true
                        )

                        BreakdownRow(
                            label = "Net Cash Received",
                            amount = "+£${"%,.2f".format(result.extraNetPay)}",
                            color = Emerald60,
                            isBold = true
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

@Composable
private fun BreakdownRow(
    label: String,
    amount: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
