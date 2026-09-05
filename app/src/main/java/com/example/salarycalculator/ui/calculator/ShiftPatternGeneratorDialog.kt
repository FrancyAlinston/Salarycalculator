package com.example.salarycalculator.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.salarycalculator.domain.ShiftPatternGenerator
import com.example.salarycalculator.domain.ShiftPatternType
import com.example.salarycalculator.theme.Amber60
import com.example.salarycalculator.theme.Emerald60
import java.util.Calendar

enum class PatternGenerationRange(val displayName: String) {
    CURRENT_MONTH("Current Month"),
    NEXT_THREE_MONTHS("Next 3 Months"),
    FULL_YEAR("Full Calendar Year")
}

@Composable
fun ShiftPatternGeneratorDialog(
    currentYear: Int,
    currentMonth: Int,
    hourlyRate: Double = 15.0,
    onApplyPattern: (generatedShifts: Map<String, Map<Int, Double>>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPattern by remember { mutableStateOf(ShiftPatternType.FOUR_ON_FOUR_OFF) }
    var selectedRange by remember { mutableStateOf(PatternGenerationRange.FULL_YEAR) }

    var anchorDayInput by remember { mutableStateOf("1") }
    var anchorMonthInput by remember { mutableStateOf(currentMonth.toString()) }
    var anchorYearInput by remember { mutableStateOf(currentYear.toString()) }

    var dayHoursInput by remember { mutableStateOf(if (selectedPattern == ShiftPatternType.STANDARD_MON_FRI) "8.0" else "12.0") }
    var nightHoursInput by remember { mutableStateOf("12.0") }

    val anchorDay = anchorDayInput.toIntOrNull()?.coerceIn(1, 31) ?: 1
    val anchorMonth = anchorMonthInput.toIntOrNull()?.coerceIn(1, 12) ?: currentMonth
    val anchorYear = anchorYearInput.toIntOrNull() ?: currentYear
    val dayHours = dayHoursInput.toDoubleOrNull() ?: 12.0
    val nightHours = nightHoursInput.toDoubleOrNull() ?: 12.0

    val (startMonth, endMonth) = when (selectedRange) {
        PatternGenerationRange.CURRENT_MONTH -> currentMonth to currentMonth
        PatternGenerationRange.NEXT_THREE_MONTHS -> currentMonth to minOf(12, currentMonth + 2)
        PatternGenerationRange.FULL_YEAR -> 1 to 12
    }

    val generatedSchedule = remember(currentYear, startMonth, endMonth, anchorYear, anchorMonth, anchorDay, selectedPattern, dayHours, nightHours) {
        ShiftPatternGenerator.generatePatternShifts(
            year = currentYear,
            startMonth = startMonth,
            endMonth = endMonth,
            anchorYear = anchorYear,
            anchorMonth = anchorMonth,
            anchorDay = anchorDay,
            pattern = selectedPattern,
            dayHours = dayHours,
            nightHours = nightHours
        )
    }

    val totalShifts = generatedSchedule.values.sumOf { it.values.count { hrs -> hrs > 0 } }
    val totalHours = generatedSchedule.values.sumOf { it.values.sum() }
    val estimatedGross = totalHours * hourlyRate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Rotational Pattern Wizard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Auto-populate annual shifts with rotational cycles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Pattern Selector
                Text("Select Shift Pattern", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                ShiftPatternType.entries.forEach { pattern ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedPattern == pattern) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPattern = pattern
                                if (pattern == ShiftPatternType.STANDARD_MON_FRI) {
                                    dayHoursInput = "8.0"
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pattern.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedPattern == pattern) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedPattern == pattern) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pattern.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedPattern == pattern) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 2. Reference Anchor Date (Day 1 of Cycle)
                Text("Cycle Start Date (Anchor Reference)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = anchorDayInput,
                        onValueChange = { anchorDayInput = it },
                        label = { Text("Day") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = anchorMonthInput,
                        onValueChange = { anchorMonthInput = it },
                        label = { Text("Month") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = anchorYearInput,
                        onValueChange = { anchorYearInput = it },
                        label = { Text("Year") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // 3. Shift Duration Inputs
                Text("Shift Hours Allocation", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dayHoursInput,
                        onValueChange = { dayHoursInput = it },
                        label = { Text("Day Shift (hrs)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    if (selectedPattern == ShiftPatternType.CONTINENTAL_2_2_3 || selectedPattern == ShiftPatternType.THREE_SHIFT_ROTATING) {
                        OutlinedTextField(
                            value = nightHoursInput,
                            onValueChange = { nightHoursInput = it },
                            label = { Text("Night Shift (hrs)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }

                // 4. Target Generation Range
                Text("Target Calendar Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PatternGenerationRange.entries.forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.displayName) }
                        )
                    }
                }

                // 5. Generation Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Generation Preview (${selectedRange.displayName})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Shifts: $totalShifts shifts", style = MaterialTheme.typography.bodySmall)
                            Text("Total Hours: ${"%.0f".format(totalHours)} hrs", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated Gross:", style = MaterialTheme.typography.bodySmall)
                            Text("£${"%,.2f".format(estimatedGross)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Emerald60)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyPattern(generatedSchedule)
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Pattern ($totalShifts shifts)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
