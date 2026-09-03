package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.ChildBenefitCalculator
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun ChildBenefitDialog(
    initialAnnualIncome: Double,
    onDismiss: () -> Unit
) {
    var numChildrenInput by remember { mutableStateOf("1") }
    var incomeInput by remember(initialAnnualIncome) {
        mutableStateOf(if (initialAnnualIncome > 0) "%.0f".format(initialAnnualIncome) else "65000")
    }

    val numChildren = remember(numChildrenInput) { numChildrenInput.toIntOrNull() ?: 1 }
    val income = remember(incomeInput) { incomeInput.toDoubleOrNull() ?: 60000.0 }

    val result = remember(numChildren, income) {
        ChildBenefitCalculator.calculate(income, numChildren)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ChildCare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "High Income Child Benefit",
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
                    text = "UK 2024/2025 rules: Tax charge applies if you earn over £60,000, tapering 1% per £200 up to £80,000 (full clawback).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Inputs
                OutlinedTextField(
                    value = numChildrenInput,
                    onValueChange = { numChildrenInput = it },
                    label = { Text("Number of Children") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = incomeInput,
                    onValueChange = { incomeInput = it },
                    label = { Text("Adjusted Net Annual Income") },
                    prefix = { Text("£ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Summary Calculation Hero Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gross Benefit Entitlement", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(result.annualBenefitEntitlement)} / yr", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("HICBC Clawback Rate", style = MaterialTheme.typography.bodySmall)
                            Text("${"%.0f".format(result.clawbackPercentage)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (result.clawbackPercentage > 0) Rose60 else Emerald60)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Annual Tax Charge (HMRC)", style = MaterialTheme.typography.bodySmall)
                            Text("-£${"%,.2f".format(result.annualTaxCharge)} / yr", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Rose60)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Net Benefit Retained", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "£${"%,.2f".format(result.netAnnualBenefit)} / yr",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald60
                            )
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
