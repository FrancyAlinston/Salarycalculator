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
import com.example.salarycalculator.domain.AssetType
import com.example.salarycalculator.domain.CapitalGainsTaxEngine
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Violet60

@Composable
fun CapitalGainsDialog(
    initialSalary: Double = 35000.0,
    onDismiss: () -> Unit
) {
    var proceedsInput by remember { mutableStateOf("15000") }
    var costsInput by remember { mutableStateOf("5000") }
    var salaryInput by remember { mutableStateOf(if (initialSalary > 0) "%.0f".format(initialSalary) else "35000") }
    var selectedAssetType by remember { mutableStateOf(AssetType.SHARES_AND_OTHER) }

    val proceeds = proceedsInput.toDoubleOrNull() ?: 0.0
    val costs = costsInput.toDoubleOrNull() ?: 0.0
    val salary = salaryInput.toDoubleOrNull() ?: 0.0
    val taxableSalary = (salary - 12570.0).coerceAtLeast(0.0)

    val report = remember(proceeds, costs, taxableSalary, selectedAssetType) {
        CapitalGainsTaxEngine.calculateCgt(
            disposalProceeds = proceeds,
            acquisitionAndAllowableCosts = costs,
            annualTaxableIncome = taxableSalary,
            assetType = selectedAssetType
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
                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Capital Gains Tax (CGT) Planner",
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
                        containerColor = if (report.totalCgtDue > 0) Rose60.copy(alpha = 0.12f)
                        else Emerald60.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (report.totalCgtDue > 0) "📈 Estimated CGT Payable" else "✅ Covered by £3k Annual Exemption",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (report.totalCgtDue > 0) Rose60 else Emerald60
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Gain", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.2f".format(report.totalGain)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total CGT Tax", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.2f".format(report.totalCgtDue)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (report.totalCgtDue > 0) Rose60 else Emerald60)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("£3,000 Annual Exemption Used:", style = MaterialTheme.typography.bodySmall, color = Emerald60)
                            Text("-£${"%,.2f".format(report.annualExemptionUsed)}", style = MaterialTheme.typography.bodySmall, color = Emerald60, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxable Capital Gain:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("£${"%,.2f".format(report.taxableGain)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }

                        if (report.gainTaxedAtBasicRate > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Basic Rate (${(selectedAssetType.basicRate * 100).toInt()}%):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("£${"%,.2f".format(report.basicRateTax)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (report.gainTaxedAtHigherRate > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Higher Rate (${(selectedAssetType.higherRate * 100).toInt()}%):", style = MaterialTheme.typography.bodySmall, color = Rose60)
                                Text("£${"%,.2f".format(report.higherRateTax)}", style = MaterialTheme.typography.bodySmall, color = Rose60, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Asset Type Selector
                Text("Asset Category:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedAssetType == AssetType.SHARES_AND_OTHER,
                        onClick = { selectedAssetType = AssetType.SHARES_AND_OTHER },
                        label = { Text("Shares & Crypto (10%/20%)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedAssetType == AssetType.RESIDENTIAL_PROPERTY,
                        onClick = { selectedAssetType = AssetType.RESIDENTIAL_PROPERTY },
                        label = { Text("Property (18%/24%)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proceedsInput,
                        onValueChange = { proceedsInput = it },
                        label = { Text("Sale Proceeds") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = costsInput,
                        onValueChange = { costsInput = it },
                        label = { Text("Purchase / Costs") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = salaryInput,
                    onValueChange = { salaryInput = it },
                    label = { Text("Annual Salary (for income tax band)") },
                    prefix = { Text("£ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

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
