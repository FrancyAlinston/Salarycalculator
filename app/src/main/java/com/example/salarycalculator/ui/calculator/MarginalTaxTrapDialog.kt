package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun MarginalTaxTrapDialog(
    initialAnnualIncome: Double = 110000.0,
    onDismiss: () -> Unit
) {
    var annualIncome by remember { mutableStateOf(maxOf(90000.0, minOf(140000.0, initialAnnualIncome))) }
    var pensionSacrifice by remember { mutableStateOf(0.0) }

    val adjustedNetIncome = maxOf(0.0, annualIncome - pensionSacrifice)

    // Personal Allowance Taper: £1 lost for every £2 earned above £100,000
    val standardAllowance = 12570.0
    val excessOver100k = maxOf(0.0, adjustedNetIncome - 100000.0)
    val allowanceLost = minOf(standardAllowance, excessOver100k / 2.0)
    val remainingAllowance = maxOf(0.0, standardAllowance - allowanceLost)

    // In the £100k - £125,140 band, marginal rate = 40% income tax + 20% effective tax from lost allowance + 2% NI = 62%
    val isInTrap = adjustedNetIncome in 100000.0..125140.0
    val effectiveMarginalRate = if (isInTrap) 60.0 else if (adjustedNetIncome > 125140.0) 45.0 else 40.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Rose60)
                Text("The 60% Marginal Tax Trap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    text = "In the UK, earnings between £100,000 and £125,140 lose £1 of Personal Allowance for every £2 earned, creating an effective 60% (62% with NI) marginal tax trap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Interactive Income Slider
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Annual Gross Income:", style = MaterialTheme.typography.labelMedium)
                            Text("£${"%,.0f".format(annualIncome)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = annualIncome.toFloat(),
                            onValueChange = { annualIncome = it.toDouble() },
                            valueRange = 90000f..140000f,
                            steps = 50
                        )
                    }
                }

                // Interactive Pension Sacrifice Remedy Slider
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Pension Sacrifice Remedy:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("£${"%,.0f".format(pensionSacrifice)}/yr", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = pensionSacrifice.toFloat(),
                            onValueChange = { pensionSacrifice = it.toDouble() },
                            valueRange = 0f..minOf(40000f, annualIncome.toFloat()),
                            steps = 40
                        )
                        Text(
                            text = "Sacrificing £${"%,.0f".format(pensionSacrifice)} reduces adjusted net income to £${"%,.0f".format(adjustedNetIncome)}/yr.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Breakdown Status Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isInTrap) Rose60.copy(alpha = 0.15f) else Emerald60.copy(alpha = 0.15f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Effective Marginal Rate:", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${effectiveMarginalRate.toInt()}%",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isInTrap) Rose60 else Emerald60
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Personal Allowance Retained:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.0f".format(remainingAllowance)} of £12,570", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax Allowance Clawed Back:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.0f".format(allowanceLost)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (allowanceLost > 0) Rose60 else Emerald60)
                        }
                    }
                }

                // Visual Trap Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Income Band Positioning", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val minVal = 90000.0
                            val maxVal = 140000.0
                            val range = maxVal - minVal

                            // Trap region [100000, 125140]
                            val trapStart = ((100000.0 - minVal) / range * size.width).toFloat()
                            val trapEnd = ((125140.0 - minVal) / range * size.width).toFloat()
                            drawRect(
                                color = Color(0xFFEF4444).copy(alpha = 0.35f),
                                topLeft = Offset(trapStart, 0f),
                                size = Size(trapEnd - trapStart, size.height)
                            )

                            // Current Adjusted Position Marker
                            val currentPos = (((adjustedNetIncome - minVal) / range).coerceIn(0.0, 1.0) * size.width).toFloat()
                            drawCircle(
                                color = Color(0xFF10B981),
                                radius = size.height / 2f,
                                center = Offset(currentPos, size.height / 2f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("£90k", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("£100k (Trap Starts)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Rose60)
                        Text("£125k (Trap Ends)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Rose60)
                        Text("£140k", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}
