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
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val taxCode by salaryRepository.getTaxCode().collectAsState(initial = "1257L")
    val hourlyRate by salaryRepository.getDefaultHourlyRate().collectAsState(initial = 12.71)
    val currentThemeMode by salaryRepository.getThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
    val taxRegion by salaryRepository.getTaxRegion().collectAsState(initial = TaxRegion.UK_STANDARD)
    val taxYear by salaryRepository.getTaxYear().collectAsState(initial = TaxYear.YEAR_2024_2025)
    val pensionRate by salaryRepository.getPensionRate().collectAsState(initial = 5.0)
    val studentLoanPlan by salaryRepository.getStudentLoanPlan().collectAsState(initial = StudentLoanPlan.NONE)
    val overtimeMultiplier by salaryRepository.getOvertimeMultiplier().collectAsState(initial = 1.5)
    val hasMarriageAllowance by salaryRepository.getHasMarriageAllowance().collectAsState(initial = false)
    val hasBlindPersonsAllowance by salaryRepository.getHasBlindPersonsAllowance().collectAsState(initial = false)
    val profiles by salaryRepository.getEmployerProfiles().collectAsState(initial = emptyList())
    val activeProfileId by salaryRepository.getActiveProfileId().collectAsState(initial = "primary_default")

    var inputTaxCode by remember(taxCode) { mutableStateOf(taxCode) }
    var inputHourlyRate by remember(hourlyRate) { mutableStateOf(hourlyRate.toString()) }
    var inputPensionRate by remember(pensionRate) { mutableStateOf(pensionRate.toString()) }
    var selectedThemeMode by remember(currentThemeMode) { mutableStateOf(currentThemeMode) }
    var selectedTaxRegion by remember(taxRegion) { mutableStateOf(taxRegion) }
    var selectedTaxYear by remember(taxYear) { mutableStateOf(taxYear) }
    var selectedStudentLoan by remember(studentLoanPlan) { mutableStateOf(studentLoanPlan) }
    var selectedOvertimeMultiplier by remember(overtimeMultiplier) { mutableStateOf(overtimeMultiplier) }
    var switchMarriageAllowance by remember(hasMarriageAllowance) { mutableStateOf(hasMarriageAllowance) }
    var switchBlindAllowance by remember(hasBlindPersonsAllowance) { mutableStateOf(hasBlindPersonsAllowance) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    val activeProfile = remember(profiles, activeProfileId) {
        profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
    }

    val calculatedAllowance = remember(inputTaxCode, switchMarriageAllowance, switchBlindAllowance) {
        TaxCalculator.parseTaxFreeAllowance(
            taxCode = inputTaxCode,
            isMonthly = false,
            hasMarriageAllowance = switchMarriageAllowance,
            hasBlindPersonsAllowance = switchBlindAllowance
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
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
                        text = "Configure your regional tax rules, employer profiles, statutory reliefs, and app theme.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 0. Employer & Job Profiles Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.WorkOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Employer Profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            FilledTonalButton(
                                onClick = { showProfileDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Manage (${profiles.size})", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Active Employment:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(activeProfile?.name ?: "Primary Employment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("£${"%.2f".format(activeProfile?.hourlyRate ?: 12.71)}/hr · Code: ${activeProfile?.taxCode ?: "1257L"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                AssistChip(
                                    onClick = { showProfileDialog = true },
                                    label = { Text("Switch") },
                                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
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
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = selectedThemeMode == ThemeMode.SYSTEM,
                                onClick = {
                                    selectedThemeMode = ThemeMode.SYSTEM
                                    scope.launch { salaryRepository.setThemeMode(ThemeMode.SYSTEM) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                icon = {}
                            ) {
                                Text("System", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }

                            SegmentedButton(
                                selected = selectedThemeMode == ThemeMode.LIGHT,
                                onClick = {
                                    selectedThemeMode = ThemeMode.LIGHT
                                    scope.launch { salaryRepository.setThemeMode(ThemeMode.LIGHT) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                icon = {}
                            ) {
                                Text("Light", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }

                            SegmentedButton(
                                selected = selectedThemeMode == ThemeMode.DARK,
                                onClick = {
                                    selectedThemeMode = ThemeMode.DARK
                                    scope.launch { salaryRepository.setThemeMode(ThemeMode.DARK) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                icon = {}
                            ) {
                                Text("Dark", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }

                // 2. Tax Year & Tax Region Card
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
                            Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Tax Year & Region", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                        // Tax Year
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tax Year Framework", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TaxYear.entries.forEach { year ->
                                    FilterChip(
                                        selected = selectedTaxYear == year,
                                        onClick = { selectedTaxYear = year },
                                        label = { Text(year.displayName) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        // Region Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tax Region", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTaxRegion == TaxRegion.UK_STANDARD,
                                    onClick = { selectedTaxRegion = TaxRegion.UK_STANDARD },
                                    label = { Text("UK Standard") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = selectedTaxRegion == TaxRegion.SCOTLAND,
                                    onClick = { selectedTaxRegion = TaxRegion.SCOTLAND },
                                    label = { Text("Scotland (6 Rates)") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
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

                // 3. Statutory Allowances & Reliefs (Marriage & Blind Person's Allowance)
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
                            Icon(Icons.Outlined.Diversity1, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Statutory Reliefs & Allowances", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                        // Marriage Allowance Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Marriage Allowance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Transfers £1,260 personal allowance from spouse (saves up to £252/yr in tax)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = switchMarriageAllowance,
                                onCheckedChange = { switchMarriageAllowance = it }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Blind Person's Allowance Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Blind Person's Allowance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Statutory extra £3,070 tax-free allowance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = switchBlindAllowance,
                                onCheckedChange = { switchBlindAllowance = it }
                            )
                        }
                    }
                }

                // 4. Pension & Student Loan Configuration Card
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
                            Icon(Icons.Outlined.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Pension & Student Loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedTextField(
                            value = inputPensionRate,
                            onValueChange = { inputPensionRate = it },
                            label = { Text("Employee Pension Contribution (%)") },
                            placeholder = { Text("e.g. 5.0") },
                            suffix = { Text("%") },
                            supportingText = { Text("Auto-enrolment standard is 5% employee / 3% employer") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Student Loan Plan Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Student Loan Repayment Plan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudentLoanPlan.entries.forEach { plan ->
                                    FilterChip(
                                        selected = selectedStudentLoan == plan,
                                        onClick = { selectedStudentLoan = plan },
                                        label = { Text(if (plan == StudentLoanPlan.NONE) "No Loan" else plan.name.replace("_", " ")) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Default Wage & Overtime Multiplier Card
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
                            Icon(Icons.Outlined.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Default Wage & Overtime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedTextField(
                            value = inputHourlyRate,
                            onValueChange = { inputHourlyRate = it },
                            label = { Text("Default Hourly Rate (£)") },
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
                                label = { Text("£12.21 (Living Wage)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            SuggestionChip(
                                onClick = { inputHourlyRate = "12.60" },
                                label = { Text("£12.60 (Real Living)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            SuggestionChip(
                                onClick = { inputHourlyRate = "13.85" },
                                label = { Text("£13.85 (London)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Default Overtime Multiplier
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Default Overtime Multiplier", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedOvertimeMultiplier == 1.0,
                                    onClick = { selectedOvertimeMultiplier = 1.0 },
                                    label = { Text("1.0x (Standard)") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = selectedOvertimeMultiplier == 1.5,
                                    onClick = { selectedOvertimeMultiplier = 1.5 },
                                    label = { Text("1.5x (Time & Half)") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = selectedOvertimeMultiplier == 2.0,
                                    onClick = { selectedOvertimeMultiplier = 2.0 },
                                    label = { Text("2.0x (Double)") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // Save Settings Button
                Button(
                    onClick = {
                        scope.launch {
                            salaryRepository.setTaxCode(inputTaxCode)
                            salaryRepository.setTaxRegion(selectedTaxRegion)
                            salaryRepository.setTaxYear(selectedTaxYear)
                            salaryRepository.setStudentLoanPlan(selectedStudentLoan)
                            salaryRepository.setOvertimeMultiplier(selectedOvertimeMultiplier)
                            salaryRepository.setHasMarriageAllowance(switchMarriageAllowance)
                            salaryRepository.setHasBlindPersonsAllowance(switchBlindAllowance)
                            inputHourlyRate.toDoubleOrNull()?.let {
                                salaryRepository.setDefaultHourlyRate(it)
                            }
                            inputPensionRate.toDoubleOrNull()?.let {
                                salaryRepository.setPensionRate(it)
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
                    Text("Save All Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // In-App Changelog & Version Info Button
                OutlinedButton(
                    onClick = { showChangelogDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Release Notes", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Employer Profiles Modal
        if (showProfileDialog) {
            ProfileManagerDialog(
                salaryRepository = salaryRepository,
                onDismiss = { showProfileDialog = false }
            )
        }

        // Release Notes Dialog
        if (showChangelogDialog) {
            AlertDialog(
                onDismissRequest = { showChangelogDialog = false },
                title = {
                    Text(
                        text = "Salary Calculator v4.0 Release Notes 🚀",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("• Annual P60 End-of-Year Certificate Generator: Official HMRC-styled A4 vector PDF certificate.", style = MaterialTheme.typography.bodyMedium)
                        Text("• Multiple Job & Employer Profiles: Manage and switch between primary, secondary, and freelance jobs.", style = MaterialTheme.typography.bodyMedium)
                        Text("• Statutory Reliefs: Full support for Marriage Allowance (£1,260 transfer) and Blind Person's Allowance (£3,070).", style = MaterialTheme.typography.bodyMedium)
                        Text("• Vector PDF Payslip Generator & CSV Ledger Exporter.", style = MaterialTheme.typography.bodyMedium)
                        Text("• Side-by-Side Historical Month Diff & Variance Tool.", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChangelogDialog = false }) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
