package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.domain.TaxCalculator

@Composable
fun CalculatorScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val taxCode by salaryRepository.getTaxCode().collectAsState(initial = "1257L")
    val defaultHourlyRate by salaryRepository.getDefaultHourlyRate().collectAsState(initial = 12.71)

    var daysWorkedInput by remember { mutableStateOf("") }
    var hoursPerDayInput by remember { mutableStateOf("8.0") }
    var overtimeHoursInput by remember { mutableStateOf("") }

    val daysWorked = daysWorkedInput.toDoubleOrNull() ?: 0.0
    val hoursPerDay = hoursPerDayInput.toDoubleOrNull() ?: 8.0
    val overtimeHours = overtimeHoursInput.toDoubleOrNull() ?: 0.0

    // Calculate Gross Pay
    val standardPay = (daysWorked * hoursPerDay) * defaultHourlyRate
    val overtimePay = overtimeHours * defaultHourlyRate // Assuming standard rate for overtime unless specified otherwise
    val grossPay = standardPay + overtimePay

    val report = TaxCalculator.calculateTax(grossPay, taxCode, isMonthly = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Salary Calculator", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = daysWorkedInput,
            onValueChange = { daysWorkedInput = it },
            label = { Text("Days Worked") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = hoursPerDayInput,
            onValueChange = { hoursPerDayInput = it },
            label = { Text("Hours per Day") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = overtimeHoursInput,
            onValueChange = { overtimeHoursInput = it },
            label = { Text("Overtime Hours") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Payslip Summary", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Basic Pay (${daysWorked * hoursPerDay} hrs)")
                    Text("£${"%.2f".format(standardPay)}")
                }
                if (overtimeHours > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overtime (${overtimeHours} hrs)")
                        Text("£${"%.2f".format(overtimePay)}")
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text("Gross Pay: £${"%.2f".format(report.grossPay)}", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Taxable Pay: £${"%.2f".format(report.taxablePay)}", style = MaterialTheme.typography.bodyMedium)
                Text("PAYE Tax: £${"%.2f".format(report.incomeTax)}", color = MaterialTheme.colorScheme.error)
                Text("National Insurance: £${"%.2f".format(report.nationalInsurance)}", color = MaterialTheme.colorScheme.error)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Net Pay: £${"%.2f".format(report.netPay)}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
