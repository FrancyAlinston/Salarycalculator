package com.example.salarycalculator.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import com.example.salarycalculator.wear.theme.WearSalaryTheme
import com.example.salarycalculator.wear.ui.WearHeatmapScreen
import com.example.salarycalculator.wear.ui.WearSalaryDetailScreen
import java.time.YearMonth

enum class WearScreen {
    HEATMAP,
    SALARY_DETAIL
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: WearSalaryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WearSalaryRepository(applicationContext)

        setContent {
            WearSalaryTheme {
                var currentScreen by remember { mutableStateOf(WearScreen.HEATMAP) }
                var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }

                BackHandler(enabled = currentScreen == WearScreen.SALARY_DETAIL) {
                    currentScreen = WearScreen.HEATMAP
                }

                when (currentScreen) {
                    WearScreen.HEATMAP -> {
                        WearHeatmapScreen(
                            repository = repository,
                            currentYearMonth = selectedYearMonth,
                            onMonthChange = { newMonth -> selectedYearMonth = newMonth },
                            onNavigateToSalary = { month ->
                                selectedYearMonth = month
                                currentScreen = WearScreen.SALARY_DETAIL
                            }
                        )
                    }
                    WearScreen.SALARY_DETAIL -> {
                        WearSalaryDetailScreen(
                            repository = repository,
                            selectedYearMonth = selectedYearMonth,
                            onMonthSelect = { newMonth -> selectedYearMonth = newMonth },
                            onBackToHeatmap = { currentScreen = WearScreen.HEATMAP }
                        )
                    }
                }
            }
        }
    }
}
