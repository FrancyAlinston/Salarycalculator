package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.salarycalculator.domain.BikTaxYear
import com.example.salarycalculator.domain.CompanyCarBikEngine
import com.example.salarycalculator.domain.PowertrainType

@Composable
fun CompanyCarBikDialog(
    initialGrossAnnual: Double,
    onDismissRequest: () -> Unit
) {
    var p11dText by remember { mutableStateOf("45000") }
    var monthlyGrossLeaseText by remember { mutableStateOf("550") }
    var selectedPowertrain by remember { mutableStateOf(PowertrainType.PURE_EV) }
    var selectedTaxYear by remember { mutableStateOf(BikTaxYear.YEAR_2024_2025) }

    val p11dValue = p11dText.toDoubleOrNull() ?: 45000.0
    val monthlyGrossLease = monthlyGrossLeaseText.toDoubleOrNull() ?: 550.0

    val bikResult = remember(p11dValue, monthlyGrossLease, selectedPowertrain, selectedTaxYear, initialGrossAnnual) {
        CompanyCarBikEngine.calculateCompanyCar(
            p11dValue = p11dValue,
            powertrain = selectedPowertrain,
            taxYear = selectedTaxYear,
            grossMonthlySalarySacrifice = monthlyGrossLease,
            annualGrossIncome = initialGrossAnnual
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ElectricCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Company Car & EV BiK Calculator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Salary Sacrifice & Benefit-in-Kind Tax",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // Powertrain Chips
                Text(
                    text = "Powertrain / Fuel Type:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PowertrainType.values().forEach { pt ->
                        FilterChip(
                            selected = selectedPowertrain == pt,
                            onClick = { selectedPowertrain = pt },
                            label = { Text(pt.displayName) }
                        )
                    }
                }

                // Tax Year Chips
                Text(
                    text = "Tax Year:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BikTaxYear.values().forEach { ty ->
                        FilterChip(
                            selected = selectedTaxYear == ty,
                            onClick = { selectedTaxYear = ty },
                            label = { Text(ty.displayName) }
                        )
                    }
                }

                // Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = p11dText,
                        onValueChange = { p11dText = it },
                        label = { Text("P11D Value (£)") },
                        leadingIcon = { Text("£", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = monthlyGrossLeaseText,
                        onValueChange = { monthlyGrossLeaseText = it },
                        label = { Text("Gross Lease (£/mo)") },
                        leadingIcon = { Text("£", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Main Result Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Statutory BiK Rate:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${String.format("%.1f", bikResult.bikPercentage)}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Monthly Gross Sacrifice:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "£${String.format("%,.2f", bikResult.grossMonthlySalarySacrifice)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax & NI Relief Saved:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "-£${String.format("%,.2f", bikResult.totalMonthlyRelief)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Monthly BiK Tax Charge:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            Text(
                                text = "+£${String.format("%,.2f", bikResult.monthlyBikTaxPayable)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Cost to Take-Home:",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "£${String.format("%,.2f", bikResult.netMonthlyCost)}/mo",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Retail Savings Comparison
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "Salary Sacrifice Savings: £${String.format("%,.0f", bikResult.monthlySavingsVsPrivate * 12)}/yr",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "You save approx ${String.format("%.0f", bikResult.totalSavingsPercentage)}% compared to leasing privately with post-tax income.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Close Button
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
