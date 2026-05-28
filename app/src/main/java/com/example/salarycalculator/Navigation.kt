package com.example.salarycalculator

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavKey
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.ui.calendar.CalendarScreen
import com.example.salarycalculator.ui.report.ReportScreen
import com.example.salarycalculator.ui.settings.SettingsScreen
import com.example.salarycalculator.ui.templates.TemplateScreen

@Composable
fun MainNavigation(salaryRepository: SalaryRepository) {
  val backStack = rememberNavBackStack(Calendar)
  val currentTab = backStack.lastOrNull() ?: Calendar

  Scaffold(
      bottomBar = {
          NavigationBar {
              NavigationBarItem(
                  selected = currentTab == Calendar,
                  onClick = { 
                      backStack.clear()
                      backStack.add(Calendar)
                  },
                  icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                  label = { Text("Duty Rota") }
              )
              NavigationBarItem(
                  selected = currentTab == Templates,
                  onClick = { 
                      backStack.clear()
                      backStack.add(Templates)
                  },
                  icon = { Icon(Icons.Default.List, contentDescription = "Templates") },
                  label = { Text("Templates") }
              )
              NavigationBarItem(
                  selected = currentTab == Report,
                  onClick = { 
                      backStack.clear()
                      backStack.add(Report)
                  },
                  icon = { Icon(Icons.Default.Info, contentDescription = "Report") },
                  label = { Text("Report") }
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
            entry<Calendar> { CalendarScreen(salaryRepository, Modifier.padding(padding)) }
            entry<Templates> { TemplateScreen(salaryRepository, Modifier.padding(padding)) }
            entry<Report> { ReportScreen(salaryRepository, Modifier.padding(padding)) }
            entry<Settings> { SettingsScreen(salaryRepository, Modifier.padding(padding)) }
        }
      )
  }
}
