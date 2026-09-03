package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryBenchmarkDialog(
    currentAnnualGross: Double = 42000.0,
    onDismiss: () -> Unit
) {
    var selectedRoleId by remember { mutableStateOf("tech_swe") }
    var selectedRegion by remember { mutableStateOf(BenchmarkRegion.NATIONAL_AVERAGE) }
    var customGrossInput by remember { mutableStateOf(if (currentAnnualGross > 0) "%.0f".format(currentAnnualGross) else "42000") }

    var expandedRoleDropdown by remember { mutableStateOf(false) }
    var expandedRegionDropdown by remember { mutableStateOf(false) }

    val annualGross = customGrossInput.toDoubleOrNull() ?: currentAnnualGross

    val eval = remember(annualGross, selectedRoleId, selectedRegion) {
        SalaryBenchmarkEngine.evaluateBenchmark(
            annualGross = annualGross,
            roleId = selectedRoleId,
            region = selectedRegion
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Emerald60.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = Emerald60,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "UK Salary Benchmarking",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Market Percentiles & Regional Insights",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Role & Region Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Role Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = expandedRoleDropdown,
                                onExpandedChange = { expandedRoleDropdown = !expandedRoleDropdown }
                            ) {
                                OutlinedTextField(
                                    value = eval.roleTitle,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Job Role") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoleDropdown) },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedRoleDropdown,
                                    onDismissRequest = { expandedRoleDropdown = false }
                                ) {
                                    SalaryBenchmarkEngine.UK_ROLE_BENCHMARKS.forEach { role ->
                                        DropdownMenuItem(
                                            text = { Text(role.title, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                selectedRoleId = role.categoryId
                                                expandedRoleDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Region Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = expandedRegionDropdown,
                                onExpandedChange = { expandedRegionDropdown = !expandedRegionDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedRegion.displayName.substringBefore(" ("),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Region") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRegionDropdown) },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedRegionDropdown,
                                    onDismissRequest = { expandedRegionDropdown = false }
                                ) {
                                    BenchmarkRegion.values().forEach { region ->
                                        DropdownMenuItem(
                                            text = { Text(region.displayName, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                selectedRegion = region
                                                expandedRegionDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Annual Gross Input
                    OutlinedTextField(
                        value = customGrossInput,
                        onValueChange = { customGrossInput = it },
                        label = { Text("Your Annual Gross Salary (£)") },
                        prefix = { Text("£") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Hero Percentile Meter Card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = eval.quartileRating,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${eval.percentileRank}th Percentile in ${eval.sector}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Top ${100 - eval.percentileRank}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Percentile Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((eval.percentileRank / 100f).coerceIn(0.01f, 1f))
                                        .fillMaxHeight()
                                        .background(Emerald60)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("P10: £${"%,.0f".format(eval.adjustedP10)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Median: £${"%,.0f".format(eval.adjustedP50)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("P90: £${"%,.0f".format(eval.adjustedP90)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Distribution Matrix Grid
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Market Salary Distribution (${selectedRegion.displayName.substringBefore(" (")})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            BenchmarkTierRow("10th Percentile (Entry Level)", eval.adjustedP10, eval.userAnnualGross)
                            BenchmarkTierRow("25th Percentile (Junior / Developing)", eval.adjustedP25, eval.userAnnualGross)
                            BenchmarkTierRow("50th Percentile (Market Median)", eval.adjustedP50, eval.userAnnualGross, isHighlight = true)
                            BenchmarkTierRow("75th Percentile (Senior / Specialist)", eval.adjustedP75, eval.userAnnualGross)
                            BenchmarkTierRow("90th Percentile (Lead / Director)", eval.adjustedP90, eval.userAnnualGross)
                        }
                    }

                    // Strategic Negotiation Insight
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Amber60, modifier = Modifier.size(28.dp))
                            Column {
                                Text(
                                    text = "Compensation Insight",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = eval.summaryInsight,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun BenchmarkTierRow(
    label: String,
    benchmarkAmount: Double,
    userAmount: Double,
    isHighlight: Boolean = false
) {
    val isUserAbove = userAmount >= benchmarkAmount
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isUserAbove) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isUserAbove) Emerald60 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "£${"%,.0f".format(benchmarkAmount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
