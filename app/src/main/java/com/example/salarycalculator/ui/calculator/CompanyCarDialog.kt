package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.CompanyCarCalculator
import com.example.salarycalculator.domain.VehicleFuelType
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun CompanyCarDialog(
    onDismiss: () -> Unit
) {
    var p11dInput by remember { mutableStateOf("32000") }
    var selectedFuelType by remember { mutableStateOf(VehicleFuelType.PURE_ELECTRIC) }
    var co2Grams by remember { mutableIntStateOf(0) }
    var electricRangeMiles by remember { mutableIntStateOf(50) }
    var providesFuel by remember { mutableStateOf(false) }

    val p11dValue = p11dInput.toDoubleOrNull() ?: 32000.0

    // Auto adjust CO2 when fuel type changes
    LaunchedEffect(selectedFuelType) {
        when (selectedFuelType) {
            VehicleFuelType.PURE_ELECTRIC -> co2Grams = 0
            VehicleFuelType.PLUG_IN_HYBRID -> co2Grams = 35
            VehicleFuelType.PETROL_RDE2_DIESEL -> co2Grams = 115
            VehicleFuelType.NON_RDE2_DIESEL -> co2Grams = 125
        }
    }

    val result = remember(p11dValue, selectedFuelType, co2Grams, electricRangeMiles, providesFuel) {
        CompanyCarCalculator.calculate(
            p11dValue = p11dValue,
            fuelType = selectedFuelType,
            co2GramsPerKm = co2Grams,
            electricRangeMiles = electricRangeMiles,
            providesFuel = providesFuel
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
                    if (selectedFuelType == VehicleFuelType.PURE_ELECTRIC) Icons.Default.ElectricCar else Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Company Car BiK Calculator",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Estimate UK HMRC Benefit-in-Kind (BiK) company vehicle taxation and net monthly take-home reduction.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Fuel Type Selector
                Text("Vehicle Fuel & Propulsion", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    VehicleFuelType.values().forEach { fuel ->
                        FilterChip(
                            selected = selectedFuelType == fuel,
                            onClick = { selectedFuelType = fuel },
                            label = { Text(fuel.displayName, style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // P11D Value Input & Slider
                OutlinedTextField(
                    value = p11dInput,
                    onValueChange = { p11dInput = it },
                    label = { Text("List Price (P11D Value)") },
                    prefix = { Text("£") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Slider(
                    value = p11dValue.toFloat().coerceIn(5000f, 100000f),
                    onValueChange = { p11dInput = it.toInt().toString() },
                    valueRange = 5000f..100000f,
                    steps = 18
                )

                // CO2 Emissions Slider
                if (selectedFuelType != VehicleFuelType.PURE_ELECTRIC) {
                    Text(
                        text = "CO2 Emissions: $co2Grams g/km",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = co2Grams.toFloat().coerceIn(1f, 200f),
                        onValueChange = { co2Grams = it.toInt() },
                        valueRange = 1f..200f
                    )
                }

                // Electric Range Slider for PHEV
                if (selectedFuelType == VehicleFuelType.PLUG_IN_HYBRID) {
                    Text(
                        text = "Zero-Emission Electric Range: $electricRangeMiles miles",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = electricRangeMiles.toFloat().coerceIn(10f, 150f),
                        onValueChange = { electricRangeMiles = it.toInt() },
                        valueRange = 10f..150f
                    )
                }

                // Employer Fuel Benefit Switch
                if (selectedFuelType != VehicleFuelType.PURE_ELECTRIC) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Employer Pays for Private Fuel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Applies HMRC statutory £27,800 fuel benefit charge.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = providesFuel, onCheckedChange = { providesFuel = it })
                        }
                    }
                }

                // BiK & Tax Impact Hero Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("BiK Banding Rate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (result.bikPercentage <= 5.0) Emerald60 else if (result.bikPercentage <= 20.0) Amber60 else Rose60
                            ) {
                                Text(
                                    text = "${"%.0f".format(result.bikPercentage)}% BiK",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Taxable Value:", style = MaterialTheme.typography.bodyMedium)
                            Text("£${"%,.2f".format(result.totalAnnualTaxableBenefit)} /yr", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }

                        // Tax Cost by Tax Band
                        Text("Net Take-Home Reduction by Tax Band:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Basic Rate (20%):", style = MaterialTheme.typography.bodySmall)
                            Text("-£${"%,.2f".format(result.basicRateMonthlyTaxCost)} /mo (-£${"%,.0f".format(result.basicRateAnnualTaxCost)}/yr)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Rose60)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Higher Rate (40%):", style = MaterialTheme.typography.bodySmall)
                            Text("-£${"%,.2f".format(result.higherRateMonthlyTaxCost)} /mo (-£${"%,.0f".format(result.higherRateAnnualTaxCost)}/yr)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Rose60)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Additional Rate (45%):", style = MaterialTheme.typography.bodySmall)
                            Text("-£${"%,.2f".format(result.additionalRateMonthlyTaxCost)} /mo (-£${"%,.0f".format(result.additionalRateAnnualTaxCost)}/yr)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Rose60)
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
