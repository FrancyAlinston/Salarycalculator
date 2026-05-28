package com.example.salarycalculator.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    var selectedTemplate by remember { mutableStateOf<ShiftTemplate?>(null) }
    
    val selectedDateStr = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
        } ?: LocalDate.now().toString()
    }

    val shiftsOnSelectedDate by salaryRepository.getShiftsByDate(selectedDateStr).collectAsState(initial = emptyList())

    // The Paint Mechanic: When a date is selected, if a template is active, instantly apply it.
    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis
        val template = selectedTemplate
        if (millis != null && template != null) {
            val dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toString()
            salaryRepository.addShift(ShiftEvent(
                date = dateStr,
                templateId = template.id,
                hours = template.defaultHours,
                hourlyRate = template.defaultRate
            ))
            // We do NOT clear selectedTemplate here so they can keep "painting" other days!
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Select a Template to Paint:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTemplate == null,
                            onClick = { selectedTemplate = null },
                            label = { Text("None (View Only)") }
                        )
                    }
                    items(templates) { template ->
                        FilterChip(
                            selected = selectedTemplate?.id == template.id,
                            onClick = { selectedTemplate = template },
                            label = { Text(template.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(template.color).copy(alpha = 0.2f),
                                selectedLabelColor = Color(template.color)
                            )
                        )
                    }
                }
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
}
