package com.example.salarycalculator.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.domain.SalaryRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    val taxCode by salaryRepository.getTaxCode().collectAsState(initial = "")
    val hourlyRate by salaryRepository.getDefaultHourlyRate().collectAsState(initial = 0.0)

    var inputTaxCode by remember(taxCode) { mutableStateOf(taxCode) }
    var inputHourlyRate by remember(hourlyRate) { mutableStateOf(hourlyRate.toString()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = inputTaxCode,
            onValueChange = { inputTaxCode = it },
            label = { Text("UK Tax Code (e.g. 1257L)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputHourlyRate,
            onValueChange = { inputHourlyRate = it },
            label = { Text("Default Hourly Rate (£)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    salaryRepository.setTaxCode(inputTaxCode)
                    inputHourlyRate.toDoubleOrNull()?.let {
                        salaryRepository.setDefaultHourlyRate(it)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }
    }
}
