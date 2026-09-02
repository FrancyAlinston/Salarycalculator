package com.example.salarycalculator

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.ui.calculator.CalculatorScreen
import com.example.salarycalculator.ui.history.HistoryScreen
import com.example.salarycalculator.ui.settings.SettingsScreen

@Composable
fun MainNavigation(salaryRepository: SalaryRepository) {
  val backStack = rememberNavBackStack(Calculator)
  val currentTab = backStack.lastOrNull() ?: Calculator

  Scaffold(
      bottomBar = {
          NavigationBar(
              tonalElevation = 8.dp,
              containerColor = MaterialTheme.colorScheme.surface
          ) {
              val isCalculator = currentTab == Calculator
              val isHistory = currentTab == History
              val isSettings = currentTab == Settings

              NavigationBarItem(
                  selected = isCalculator,
                  onClick = { 
                      if (!isCalculator) {
                          backStack.clear()
                          backStack.add(Calculator)
                      }
                  },
                  icon = { 
                      Icon(
                          imageVector = if (isCalculator) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                          contentDescription = "Calculator"
                      ) 
                  },
                  label = { Text("Calculator", style = MaterialTheme.typography.labelMedium) },
                  colors = NavigationBarItemDefaults.colors(
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary
                  )
              )

              NavigationBarItem(
                  selected = isHistory,
                  onClick = { 
                      if (!isHistory) {
                          backStack.clear()
                          backStack.add(History)
                      }
                  },
                  icon = { 
                      Icon(
                          imageVector = if (isHistory) Icons.Filled.History else Icons.Outlined.History,
                          contentDescription = "History"
                      ) 
                  },
                  label = { Text("History", style = MaterialTheme.typography.labelMedium) },
                  colors = NavigationBarItemDefaults.colors(
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary
                  )
              )

              NavigationBarItem(
                  selected = isSettings,
                  onClick = { 
                      if (!isSettings) {
                          backStack.clear()
                          backStack.add(Settings)
                      }
                  },
                  icon = { 
                      Icon(
                          imageVector = if (isSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
                          contentDescription = "Settings"
                      ) 
                  },
                  label = { Text("Settings", style = MaterialTheme.typography.labelMedium) },
                  colors = NavigationBarItemDefaults.colors(
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary
                  )
              )
          }
      }
  ) { padding ->
      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Calculator> { CalculatorScreen(salaryRepository, Modifier.padding(padding)) }
            entry<History> { HistoryScreen(salaryRepository, Modifier.padding(padding)) }
            entry<Settings> { SettingsScreen(salaryRepository, Modifier.padding(padding)) }
        }
      )
  }
}
