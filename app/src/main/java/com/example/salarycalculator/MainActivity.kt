package com.example.salarycalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.domain.ThemeMode
import com.example.salarycalculator.theme.SalaryCalculatorTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val salaryRepo = SalaryRepository(this)

    enableEdgeToEdge()
    setContent {
      val themeMode by salaryRepo.getThemeMode().collectAsState(initial = ThemeMode.SYSTEM)

      SalaryCalculatorTheme(themeMode = themeMode) { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
            MainNavigation(salaryRepo) 
        } 
      }
    }
  }
}
