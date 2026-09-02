package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.SalaryReport
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.domain.TaxCalculator
import com.example.salarycalculator.theme.*

@Composable
fun CalculatorScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val taxCode by salaryRepository.getTaxCode().collectAsState(initial = "1257L")
    val defaultHourlyRate by salaryRepository.getDefaultHourlyRate().collectAsState(initial = 12.71)

    var daysWorkedInput by remember { mutableStateOf("20") }
    var hoursPerDayInput by remember { mutableStateOf("8.0") }
    var overtimeHoursInput by remember { mutableStateOf("") }

    // Performance: Memoize parsed doubles
    val daysWorked = remember(daysWorkedInput) { daysWorkedInput.toDoubleOrNull() ?: 0.0 }
    val hoursPerDay = remember(hoursPerDayInput) { hoursPerDayInput.toDoubleOrNull() ?: 8.0 }
    val overtimeHours = remember(overtimeHoursInput) { overtimeHoursInput.toDoubleOrNull() ?: 0.0 }

    // Performance: Memoize Gross Pay and Tax Calculation
    val standardPay = remember(daysWorked, hoursPerDay, defaultHourlyRate) {
        (daysWorked * hoursPerDay) * defaultHourlyRate
    }
    val overtimePay = remember(overtimeHours, defaultHourlyRate) {
        overtimeHours * defaultHourlyRate
    }
    val grossPay = remember(standardPay, overtimePay) { standardPay + overtimePay }

    val report: SalaryReport = remember(grossPay, taxCode) {
        TaxCalculator.calculateTax(grossPay, taxCode, isMonthly = true)
    }

    val totalHours = remember(daysWorked, hoursPerDay, overtimeHours) {
        (daysWorked * hoursPerDay) + overtimeHours
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Header & Rate Subtitle
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Salary Calculator",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Tax Code: $taxCode", style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Text(
                text = "Rate: £${"%.2f".format(defaultHourlyRate)}/hr · Monthly View",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Hero Take-Home Pay Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated Net Pay",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = EmeraldContainerLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (grossPay > 0) "${"%.1f".format((report.netPay / grossPay) * 100)}% Take-Home" else "100%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Emerald40
                        )
                    }
                }

                // Animated Net Pay Display
                AnimatedContent(
                    targetState = report.netPay,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                         slideInVertically { it / 2 }) togetherWith
                        (fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                         slideOutVertically { -it / 2 })
                    },
                    label = "NetPayAnimation"
                ) { netAmount ->
                    Text(
                        text = "£${"%.2f".format(netAmount)}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald60
                    )
                }

                // Animated Distribution Progress Bar
                if (grossPay > 0) {
                    val netRatio by animateFloatAsState(
                        targetValue = (report.netPay / grossPay).toFloat(),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "netRatio"
                    )
                    val taxRatio by animateFloatAsState(
                        targetValue = (report.incomeTax / grossPay).toFloat(),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "taxRatio"
                    )
                    val niRatio by animateFloatAsState(
                        targetValue = (report.nationalInsurance / grossPay).toFloat(),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "niRatio"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (netRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(netRatio.coerceAtLeast(0.001f))
                                        .fillMaxHeight()
                                        .background(Emerald60)
                                )
                            }
                            if (taxRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(taxRatio.coerceAtLeast(0.001f))
                                        .fillMaxHeight()
                                        .background(Rose60)
                                )
                            }
                            if (niRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(niRatio.coerceAtLeast(0.001f))
                                        .fillMaxHeight()
                                        .background(Amber60)
                                )
                            }
                        }

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LegendItem(color = Emerald60, label = "Take Home")
                            LegendItem(color = Rose60, label = "PAYE Tax")
                            LegendItem(color = Amber60, label = "National Insurance")
                        }
                    }
                }
            }
        }

        // Quick Input Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = daysWorkedInput == "20" && hoursPerDayInput == "8.0",
                    onClick = {
                        daysWorkedInput = "20"
                        hoursPerDayInput = "8.0"
                    },
                    label = { Text("Full Month (20d · 8h)") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = daysWorkedInput == "21.67" && hoursPerDayInput == "7.5",
                    onClick = {
                        daysWorkedInput = "21.67"
                        hoursPerDayInput = "7.5"
                    },
                    label = { Text("UK Avg (21.7d · 7.5h)") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = daysWorkedInput == "16" && hoursPerDayInput == "8.0",
                    onClick = {
                        daysWorkedInput = "16"
                        hoursPerDayInput = "8.0"
                    },
                    label = { Text("4-Day Week (16d)") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Input Fields Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Working Hours & Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = daysWorkedInput,
                    onValueChange = { daysWorkedInput = it },
                    label = { Text("Days Worked") },
                    placeholder = { Text("e.g. 20") },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    trailingIcon = {
                        if (daysWorkedInput.isNotEmpty()) {
                            IconButton(onClick = { daysWorkedInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = hoursPerDayInput,
                    onValueChange = { hoursPerDayInput = it },
                    label = { Text("Hours per Day") },
                    placeholder = { Text("e.g. 8.0") },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    trailingIcon = {
                        if (hoursPerDayInput.isNotEmpty()) {
                            IconButton(onClick = { hoursPerDayInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = overtimeHoursInput,
                    onValueChange = { overtimeHoursInput = it },
                    label = { Text("Overtime Hours (Optional)") },
                    placeholder = { Text("e.g. 5") },
                    leadingIcon = { Icon(Icons.Outlined.MoreTime, contentDescription = null) },
                    trailingIcon = {
                        if (overtimeHoursInput.isNotEmpty()) {
                            IconButton(onClick = { overtimeHoursInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Detailed Itemized Payslip Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payslip Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total: ${"%.1f".format(totalHours)} hrs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Basic Pay
                PayslipRow(
                    label = "Basic Pay (${"%.1f".format(daysWorked * hoursPerDay)} hrs)",
                    value = "£${"%.2f".format(standardPay)}"
                )

                // Overtime Pay Animated
                AnimatedVisibility(
                    visible = overtimeHours > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    PayslipRow(
                        label = "Overtime (${"%.1f".format(overtimeHours)} hrs)",
                        value = "£${"%.2f".format(overtimePay)}"
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Gross Pay
                PayslipRow(
                    label = "Gross Total Pay",
                    value = "£${"%.2f".format(report.grossPay)}",
                    isBold = true
                )

                // Taxable Pay
                PayslipRow(
                    label = "Taxable Income",
                    value = "£${"%.2f".format(report.taxablePay)}",
                    isSecondary = true
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Deductions
                PayslipRow(
                    label = "PAYE Income Tax (2024/25)",
                    value = "-£${"%.2f".format(report.incomeTax)}",
                    valueColor = Rose60
                )

                PayslipRow(
                    label = "Class 1 National Insurance",
                    value = "-£${"%.2f".format(report.nationalInsurance)}",
                    valueColor = Amber60
                )

                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

                // Net Pay Final Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Net Take-Home",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "After tax & NI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "£${"%.2f".format(report.netPay)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald60
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
    }
}

@Composable
private fun PayslipRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isSecondary: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isSecondary) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
