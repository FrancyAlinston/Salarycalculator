package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.example.salarycalculator.domain.GiftAidOptimizer
import com.example.salarycalculator.domain.TaxRegion
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.example.salarycalculator.theme.Teal60

@Composable
fun GiftAidDialog(
    initialSalary: Double = 60000.0,
    taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    onDismiss: () -> Unit
) {
    var donationInput by remember { mutableStateOf("1000") }
    var salaryInput by remember { mutableStateOf(if (initialSalary > 0) "%.0f".format(initialSalary) else "60000") }

    val donation = donationInput.toDoubleOrNull() ?: 0.0
    val salary = salaryInput.toDoubleOrNull() ?: 0.0

    val report = remember(donation, salary, taxRegion) {
        GiftAidOptimizer.calculateGiftAid(
            netDonation = donation,
            annualSalary = salary,
            taxRegion = taxRegion
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
                    Icons.Default.VolunteerActivism,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Gift Aid & Higher-Rate Relief",
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
                        containerColor = Emerald60.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🎁 Total Value to Charity",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Emerald60
                        )

                        Text(
                            text = "£${"%,.2f".format(report.grossDonationToCharity)}",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald60
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Your Direct Gift:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("£${"%,.2f".format(report.netDonation)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("HMRC 25% Basic Top-Up:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+£${"%,.2f".format(report.hmrcBasicRateTopUp)}", style = MaterialTheme.typography.bodySmall, color = Emerald60, fontWeight = FontWeight.SemiBold)
                        }

                        if (report.higherRateTaxReliefClaimable > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Higher Rate Relief Claimable:", style = MaterialTheme.typography.bodySmall, color = Teal60, fontWeight = FontWeight.Bold)
                                Text("£${"%,.2f".format(report.higherRateTaxReliefClaimable)}", style = MaterialTheme.typography.bodySmall, color = Teal60, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Effective Real Cost to You:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text("£${"%,.2f".format(report.effectiveNetCostToDonor)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                // Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("50", "100", "250", "500", "1000", "2500").forEach { amount ->
                        FilterChip(
                            selected = donationInput == amount,
                            onClick = { donationInput = amount },
                            label = { Text("£$amount") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = donationInput,
                        onValueChange = { donationInput = it },
                        label = { Text("Donation Amount") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = salaryInput,
                        onValueChange = { salaryInput = it },
                        label = { Text("Annual Salary") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

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
