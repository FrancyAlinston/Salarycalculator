package com.example.salarycalculator.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.salarycalculator.theme.SalaryCalculatorTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel() },
) {
  val grossSalary by viewModel.grossSalary.collectAsStateWithLifecycle()
  val taxRate by viewModel.taxRate.collectAsStateWithLifecycle()
  val netSalary by viewModel.netSalary.collectAsStateWithLifecycle()

  Column(
    modifier = modifier
        .fillMaxSize()
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = "Salary Calculator", style = MaterialTheme.typography.headlineMedium)
    
    Spacer(modifier = Modifier.height(24.dp))
    
    OutlinedTextField(
      value = grossSalary,
      onValueChange = { viewModel.updateGrossSalary(it) },
      label = { Text("Gross Salary") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    OutlinedTextField(
      value = taxRate,
      onValueChange = { viewModel.updateTaxRate(it) },
      label = { Text("Tax Rate (%)") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
      onClick = { viewModel.calculate() },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Calculate Net Salary")
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text(
      text = "Net Salary: $${"%.2f".format(netSalary)}",
      style = MaterialTheme.typography.headlineSmall
    )
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  SalaryCalculatorTheme { MainScreen(onItemClick = {}) }
}
