package com.example.salarycalculator.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.data.ShiftEvent
import com.example.salarycalculator.data.ShiftType
import com.example.salarycalculator.domain.SalaryRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val datePickerState = rememberDatePickerState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    
    val selectedDateStr = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
        } ?: LocalDate.now().toString()
    }

    val shiftsOnSelectedDate by salaryRepository.getShiftsByDate(selectedDateStr).collectAsState(initial = emptyList())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Shift")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Duty Rota", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth()
            )
            
            Divider()

            Text(
                "Events on $selectedDateStr:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(shiftsOnSelectedDate) { shift ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(shift.type.name.replace("_", " "), style = MaterialTheme.typography.titleMedium)
                                Text("${shift.hours} hours", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { scope.launch { salaryRepository.deleteShift(shift) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (shiftsOnSelectedDate.isEmpty()) {
                    item {
                        Text("No events for this date.")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddShiftDialog(
            dateStr = selectedDateStr,
            onDismiss = { showAddDialog = false },
            onSave = { type, hours, rate ->
                scope.launch {
                    salaryRepository.addShift(ShiftEvent(
                        date = selectedDateStr,
                        type = type,
                        hours = hours,
                        hourlyRate = rate
                    ))
                    showAddDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShiftDialog(
    dateStr: String,
    onDismiss: () -> Unit,
    onSave: (ShiftType, Double, Double?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(ShiftType.WORKING_DAY) }
    var hoursInput by remember { mutableStateOf("") }
    var customRateInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Event for $dateStr") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Type") },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ShiftType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ")) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text("Duration (Hours / Units)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customRateInput,
                    onValueChange = { customRateInput = it },
                    label = { Text("Custom Hourly Rate (£) (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val hours = hoursInput.toDoubleOrNull() ?: 0.0
                val rate = customRateInput.toDoubleOrNull()
                onSave(selectedType, hours, rate)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
