package com.example.salarycalculator.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.domain.TaxCalculator
import com.example.salarycalculator.domain.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val taxCode by salaryRepository.getTaxCode().collectAsState(initial = "1257L")
    val hourlyRate by salaryRepository.getDefaultHourlyRate().collectAsState(initial = 12.71)
    val currentThemeMode by salaryRepository.getThemeMode().collectAsState(initial = ThemeMode.SYSTEM)

    var inputTaxCode by remember(taxCode) { mutableStateOf(taxCode) }
    var inputHourlyRate by remember(hourlyRate) { mutableStateOf(hourlyRate.toString()) }
    var selectedThemeMode by remember(currentThemeMode) { mutableStateOf(currentThemeMode) }

    val calculatedAllowance = remember(inputTaxCode) {
        TaxCalculator.parseTaxFreeAllowance(inputTaxCode, isMonthly = false)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Preferences & Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Customize your theme, UK tax allowances, and default wage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Appearance / Theme Mode Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "App Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedThemeMode == ThemeMode.SYSTEM,
                            onClick = {
                                selectedThemeMode = ThemeMode.SYSTEM
                                scope.launch { salaryRepository.setThemeMode(ThemeMode.SYSTEM) }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            icon = {
                                Icon(
                                    Icons.Default.BrightnessAuto,
                                    contentDescription = "System Theme",
                                    Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text("System", style = MaterialTheme.typography.labelMedium)
                        }

                        SegmentedButton(
                            selected = selectedThemeMode == ThemeMode.LIGHT,
                            onClick = {
                                selectedThemeMode = ThemeMode.LIGHT
                                scope.launch { salaryRepository.setThemeMode(ThemeMode.LIGHT) }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            icon = {
                                Icon(
                                    Icons.Default.LightMode,
                                    contentDescription = "Light Theme",
                                    Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text("Light", style = MaterialTheme.typography.labelMedium)
                        }

                        SegmentedButton(
                            selected = selectedThemeMode == ThemeMode.DARK,
                            onClick = {
                                selectedThemeMode = ThemeMode.DARK
                                scope.launch { salaryRepository.setThemeMode(ThemeMode.DARK) }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            icon = {
                                Icon(
                                    Icons.Default.DarkMode,
                                    contentDescription = "Dark Theme",
                                    Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text("Dark", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 2. Tax Code Configuration Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tax Code Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = inputTaxCode,
                        onValueChange = { inputTaxCode = it.uppercase() },
                        label = { Text("UK Tax Code") },
                        placeholder = { Text("e.g. 1257L") },
                        supportingText = {
                            Text("Allowance: £${"%,.2f".format(calculatedAllowance)}/year (£${"%,.2f".format(calculatedAllowance / 12)}/mo)")
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Quick Tax Code Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { inputTaxCode = "1257L" },
                            label = { Text("1257L (Standard)") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        SuggestionChip(
                            onClick = { inputTaxCode = "BR" },
                            label = { Text("BR (Flat 20%)") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        SuggestionChip(
                            onClick = { inputTaxCode = "0T" },
                            label = { Text("0T (No Allowance)") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 3. Default Hourly Wage Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Default Hourly Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = inputHourlyRate,
                        onValueChange = { inputHourlyRate = it },
                        label = { Text("Hourly Rate (£)") },
                        placeholder = { Text("e.g. 12.71") },
                        prefix = { Text("£ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Quick Wage Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { inputHourlyRate = "12.21" },
                            label = { Text("£12.21 (UK Living Wage)") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        SuggestionChip(
                            onClick = { inputHourlyRate = "12.60" },
                            label = { Text("£12.60 (Real Living Wage)") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        SuggestionChip(
                            onClick = { inputHourlyRate = "13.85" },
                            label = { Text("£13.85 (London)") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Save Settings Button
            Button(
                onClick = {
                    scope.launch {
                        salaryRepository.setTaxCode(inputTaxCode)
                        inputHourlyRate.toDoubleOrNull()?.let {
                            salaryRepository.setDefaultHourlyRate(it)
                        }
                        snackbarHostState.showSnackbar("Settings saved successfully!")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
