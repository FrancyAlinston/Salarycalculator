package com.example.salarycalculator.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.salarycalculator.wear.domain.WearTelemetryManager
import com.example.salarycalculator.wear.theme.WearSalaryTheme
import com.example.salarycalculator.wear.ui.WearCasioWatchScreen
import com.example.salarycalculator.wear.ui.WearHeatmapScreen
import com.example.salarycalculator.wear.ui.WearSalaryDetailScreen
import java.time.YearMonth

enum class WearScreen {
    HEATMAP,
    CASIO_WATCH,
    SALARY_DETAIL
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: WearSalaryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WearSalaryRepository(applicationContext)

        // Eagerly pull latest shifts from NAS server if Wi-Fi is connected
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                repository.syncFromNasServer()
            } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    101
                )
            }
        }
        WearTelemetryManager.ensureStepSensorListening(applicationContext)

        setContent {
            WearSalaryTheme {
                val startOnCasio = remember {
                    WearTelemetryManager.isCasioDefaultHomeScreen(applicationContext)
                }
                var currentScreen by remember {
                    mutableStateOf(if (startOnCasio) WearScreen.CASIO_WATCH else WearScreen.HEATMAP)
                }
                var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }

                // Keep screen awake when viewing the live ticking Casio Digital Watch screen
                DisposableEffect(currentScreen) {
                    if (currentScreen == WearScreen.CASIO_WATCH) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                BackHandler(enabled = currentScreen != WearScreen.HEATMAP) {
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
                            },
                            onNavigateToCasioWatch = {
                                currentScreen = WearScreen.CASIO_WATCH
                            }
                        )
                    }
                    WearScreen.CASIO_WATCH -> {
                        WearCasioWatchScreen(
                            repository = repository,
                            onNavigateToHeatmap = { currentScreen = WearScreen.HEATMAP },
                            onNavigateToSalary = { currentScreen = WearScreen.SALARY_DETAIL }
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
