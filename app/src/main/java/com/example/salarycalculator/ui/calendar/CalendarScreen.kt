package com.example.salarycalculator.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.data.ShiftEvent
import com.example.salarycalculator.data.ShiftTemplate
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

    val templates by salaryRepository.getAllTemplates().collectAsState(initial = emptyList())
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val selectedDateStr = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
        } ?: LocalDate.now().toString()
    }

    val shiftsOnSelectedDate by salaryRepository.getShiftsByDate(selectedDateStr).collectAsState(initial = emptyList())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(shiftsOnSelectedDate) { shift ->
                    val template = templates.find { it.id == shift.templateId }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(template?.color?.let { Color(it) } ?: Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(template?.name ?: "Unknown Template", style = MaterialTheme.typography.titleMedium)
                                    Text("${shift.hours} hours", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            IconButton(onClick = { scope.launch { salaryRepository.deleteShift(shift) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (shiftsOnSelectedDate.isEmpty()) {
                    item {
                        Text("No events for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Shift",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                )
                Divider()
                LazyColumn {
                    items(templates) { template ->
                        ListItem(
                            headlineContent = { Text(template.name) },
                            supportingContent = { Text("All Day") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(template.color))
                                )
                            },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    salaryRepository.addShift(ShiftEvent(
                                        date = selectedDateStr,
                                        templateId = template.id,
                                        hours = template.defaultHours,
                                        hourlyRate = template.defaultRate
                                    ))
                                    showBottomSheet = false
                                }
                            }
                        )
                        Divider()
                    }
                    item {
                        ListItem(
                            headlineContent = { Text("New Shift") },
                            leadingContent = {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            },
                            modifier = Modifier.clickable {
                                showBottomSheet = false
                                // Since we are in bottom tabs, creating a new shift would normally navigate
                                // to the Templates tab, but for now we just dismiss.
                            }
                        )
                    }
                }
            }
        }
    }
}
