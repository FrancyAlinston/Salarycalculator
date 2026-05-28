package com.example.salarycalculator.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.salarycalculator.data.ShiftTemplate
import com.example.salarycalculator.domain.SalaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TemplateViewModel(private val repository: SalaryRepository) : ViewModel() {
    val templates = repository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTemplate(template: ShiftTemplate) {
        viewModelScope.launch {
            repository.addTemplate(template)
        }
    }

    fun deleteTemplate(template: ShiftTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }
}

@Composable
fun TemplateScreen(
    salaryRepository: SalaryRepository,
    modifier: Modifier = Modifier
) {
    val viewModel: TemplateViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TemplateViewModel(salaryRepository) as T
            }
        }
    )

    val templates by viewModel.templates.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Template")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates) { template ->
                TemplateCard(template, onDelete = { viewModel.deleteTemplate(template) })
            }
        }

        if (showDialog) {
            AddTemplateDialog(
                onDismiss = { showDialog = false },
                onSave = { 
                    viewModel.saveTemplate(it)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun TemplateCard(template: ShiftTemplate, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(template.color))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(template.name, style = MaterialTheme.typography.titleMedium)
                    Text("${template.defaultHours} hrs | £${template.defaultRate ?: "Default"}/hr", style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddTemplateDialog(onDismiss: () -> Unit, onSave: (ShiftTemplate) -> Unit) {
    var name by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("8.0") }
    var rate by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Basic, Annual Leave)") }
                )
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Default Hours") }
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Custom Rate (£/hr) - Optional") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hours.toDoubleOrNull() ?: 8.0
                val r = rate.toDoubleOrNull()
                // Random color for now
                val color = android.graphics.Color.HSVToColor(floatArrayOf((0..360).random().toFloat(), 0.5f, 0.9f)).toLong()
                onSave(
                    ShiftTemplate(
                        id = UUID.randomUUID().toString(),
                        name = name.ifEmpty { "New Template" },
                        color = color,
                        defaultHours = h,
                        defaultRate = r
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
