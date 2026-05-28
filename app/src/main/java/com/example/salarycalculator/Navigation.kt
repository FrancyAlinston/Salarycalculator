package com.example.salarycalculator

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.ui.calculator.CalculatorScreen
import com.example.salarycalculator.ui.settings.SettingsScreen

@Composable
fun MainNavigation(salaryRepository: SalaryRepository) {
  val backStack = rememberNavBackStack(Calculator)
  val currentTab = backStack.lastOrNull() ?: Calculator

  Scaffold(
      bottomBar = {
          NavigationBar {
              NavigationBarItem(
                  selected = currentTab == Calculator,
                  onClick = { 
                      backStack.clear()
                      backStack.add(Calculator)
                  },
                  icon = { Icon(Icons.Default.Info, contentDescription = "Calculator") },
                  label = { Text("Calculator") }
              )
              NavigationBarItem(
                  selected = currentTab == Settings,
                  onClick = { 
                      backStack.clear()
                      backStack.add(Settings)
                  },
                  icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                  label = { Text("Settings") }
              )
          }
      }
  ) { padding ->
      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Calculator> { CalculatorScreen(salaryRepository, Modifier.padding(padding)) }
            entry<Settings> { SettingsScreen(salaryRepository, Modifier.padding(padding)) }
        }
      )
  }
}
