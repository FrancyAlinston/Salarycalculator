package com.example.salarycalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.salarycalculator.data.AppDatabase
import com.example.salarycalculator.data.SettingsRepository
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.theme.SalaryCalculatorTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val database = AppDatabase.getDatabase(this)
    val settingsRepo = SettingsRepository(this)
    val salaryRepo = SalaryRepository(database.shiftDao(), database.templateDao(), settingsRepo)

    enableEdgeToEdge()
    setContent {
      SalaryCalculatorTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
            MainNavigation(salaryRepo) 
        } 
      }
    }
  }
}
