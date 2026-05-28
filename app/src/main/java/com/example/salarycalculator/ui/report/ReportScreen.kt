package com.example.salarycalculator.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.domain.SalaryReport
import com.example.salarycalculator.domain.SalaryRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(salaryRepository: SalaryRepository, modifier: Modifier = Modifier) {
    val dateRangePickerState = rememberDateRangePickerState()
    
    var salaryReport by remember { mutableStateOf<SalaryReport?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis) {
        val startMillis = dateRangePickerState.selectedStartDateMillis
        val endMillis = dateRangePickerState.selectedEndDateMillis
        if (startMillis != null && endMillis != null) {
            val startStr = Instant.ofEpochMilli(startMillis).atZone(ZoneId.of("UTC")).toLocalDate().toString()
            val endStr = Instant.ofEpochMilli(endMillis).atZone(ZoneId.of("UTC")).toLocalDate().toString()
            salaryRepository.getSalaryReport(startStr, endStr).collect { report ->
                salaryReport = report
            }
        } else {
            salaryReport = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Salary Report", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Select Date Range:")
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.height(400.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        salaryReport?.let { report ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Hours: ${report.totalHours}", style = MaterialTheme.typography.bodyLarge)
                    Text("Gross Pay: £${"%.2f".format(report.grossPay)}", style = MaterialTheme.typography.bodyLarge)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Taxable Pay: £${"%.2f".format(report.taxablePay)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Estimated Income Tax: £${"%.2f".format(report.incomeTax)}", color = MaterialTheme.colorScheme.error)
                    Text("Estimated National Insurance: £${"%.2f".format(report.nationalInsurance)}", color = MaterialTheme.colorScheme.error)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Net Pay: £${"%.2f".format(report.netPay)}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        } ?: run {
            Text("Select a start and end date to generate a report.")
        }
    }
}
