package com.example.salarycalculator.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.salarycalculator.wear.domain.TaxCalculator
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import com.example.salarycalculator.wear.domain.WearTelemetryManager
import com.example.salarycalculator.wear.watchface.CasioDigitalCanvasRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Interactive Casio Classic F-91W / G-Shock Black Digital Watch Screen inside the Wear OS app.
 * Features:
 * - Pure Black (#000000) Super AMOLED background
 * - Live 1-second ticking 7-segment digital clock (HH:MM:SS)
 * - Live Step Count (Hardware Step Counter) & Weather (°C + Condition + High/Low)
 * - Today's Duty Status ("NIGHT DUTY" vs "OFF DUTY") & 7-Day Weekly Rota Matrix
 * - Interactive Tap Zones:
 *   - Tap Top-Right Duty Box -> Toggle Today's Shift (Night Duty / Overtime / Leave / Off)
 *   - Tap Bottom 7-Day Matrix or Rotate Bezel -> Open Monthly Shift Heatmap
 */
@Composable
fun WearCasioWatchScreen(
    repository: WearSalaryRepository,
    onNavigateToHeatmap: () -> Unit,
    onNavigateToSalary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    val shifts by repository.shiftsFlow.collectAsState(initial = WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS)
    val standardHours by repository.standardShiftHoursFlow.collectAsState(initial = 12.0)

    var nowZoned by remember { mutableStateOf(ZonedDateTime.now()) }
    var telemetry by remember { mutableStateOf(WearTelemetryManager.getSnapshot(context)) }
    var rotaryAccum by remember { mutableFloatStateOf(0f) }

    // 1-second digital quartz tick + live step sensor read
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        WearTelemetryManager.ensureStepSensorListening(context)
        coroutineScope.launch {
            telemetry = WearTelemetryManager.refreshWeatherIfNeeded(context)
        }
        while (true) {
            nowZoned = ZonedDateTime.now()
            telemetry = WearTelemetryManager.getSnapshot(context)
            delay(1000L)
        }
    }

    val todaySummary = remember(nowZoned.toLocalDate(), shifts, standardHours) {
        TaxCalculator.resolveTodayDutySummary(nowZoned.toLocalDate(), shifts, standardHours)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                rotaryAccum += event.verticalScrollPixels
                if (rotaryAccum > 50f || rotaryAccum < -50f) {
                    rotaryAccum = 0f
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToHeatmap()
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(todaySummary.dateKey, standardHours) {
                detectTapGestures(
                    onTap = { offset ->
                        val normX = offset.x / size.width.toFloat()
                        val normY = offset.y / size.height.toFloat()

                        when {
                            // Top-Right Duty Box: 1-tap toggle today's shift status
                            normY in 0.14f..0.30f && normX >= 0.44f -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    repository.toggleShift(LocalDate.now().toString(), standardHours)
                                }
                            }
                            // Middle Telemetry Row: force weather & NAS refresh
                            normY in 0.52f..0.68f -> {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    telemetry = WearTelemetryManager.refreshWeatherIfNeeded(context, force = true)
                                    repository.syncFromNasServer()
                                }
                            }
                            // Bottom 7-Day Weekly Bar or Top Header: switch to Monthly Heatmap
                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToHeatmap()
                            }
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToSalary()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            CasioDigitalCanvasRenderer.drawCasioWatchFace(
                canvas = drawContext.canvas.nativeCanvas,
                width = size.width.toInt(),
                height = size.height.toInt(),
                zonedDateTime = nowZoned,
                dutySummary = todaySummary,
                telemetry = telemetry,
                isAmbient = false,
                showSeconds = true
            )
        }
    }
}
