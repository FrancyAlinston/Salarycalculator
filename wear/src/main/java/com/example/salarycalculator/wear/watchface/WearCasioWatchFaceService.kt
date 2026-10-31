package com.example.salarycalculator.wear.watchface

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.example.salarycalculator.wear.MainActivity
import com.example.salarycalculator.wear.domain.TaxCalculator
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import com.example.salarycalculator.wear.domain.WearTelemetryManager
import com.example.salarycalculator.wear.domain.WearTelemetrySnapshot
import com.example.salarycalculator.wear.domain.WearTodayDutySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Native Wear OS Digital Watch Face Service for Samsung Galaxy Watch 8 Classic.
 * Renders a Casio Classic F-91W / G-Shock Black Digital Watch Face with:
 * - Pure Black (#000000) Super AMOLED background
 * - 7-Segment Digital Clock (HH:MM:SS)
 * - Selectable Casio Retro Color Themes (`[ACTION: FEAT-938]`)
 * - Casio Hourly Chime (`SIG`) & Shift Start Alert (`ALM`) (`[ACTION: FEAT-939]`)
 * - Live Watch Battery Percentage (`BATT %`) (`[ACTION: PEND-930]`)
 * - Live Step Count (Hardware Step Counter Sensor)
 * - Live Weather (°C + Condition + High/Low)
 * - Today's Duty Status ("NIGHT DUTY" vs "OFF DUTY") & 7-Day Weekly Rota Matrix
 */
class WearCasioWatchFaceService : WatchFaceService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val repository = WearSalaryRepository(applicationContext)
        WearTelemetryManager.ensureStepSensorListening(applicationContext)

        val initialDuty = try {
            repository.getTodayDutySummary(LocalDate.now())
        } catch (_: Exception) {
            TaxCalculator.resolveTodayDutySummary(
                LocalDate.now(),
                WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS,
                12.0
            )
        }
        val initialTelemetry = WearTelemetryManager.getSnapshot(applicationContext)

        val renderer = CasioWatchCanvasRenderer(
            surfaceHolder = surfaceHolder,
            currentUserStyleRepository = currentUserStyleRepository,
            watchState = watchState,
            repository = repository,
            initialDuty = initialDuty,
            initialTelemetry = initialTelemetry
        )

        val watchFace = WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        )

        watchFace.setTapListener(object : WatchFace.TapListener {
            override fun onTapEvent(
                tapType: Int,
                tapEvent: TapEvent,
                complicationSlot: androidx.wear.watchface.ComplicationSlot?
            ) {
                if (tapType == TapType.UP) {
                    val w = surfaceHolder.surfaceFrame.width().coerceAtLeast(1)
                    val h = surfaceHolder.surfaceFrame.height().coerceAtLeast(1)
                    val normX = tapEvent.xPos.toFloat() / w.toFloat()
                    val normY = tapEvent.yPos.toFloat() / h.toFloat()

                    when {
                        // [ACTION: FEAT-938] Tap Top-Left "◀ LIGHT" header to cycle Casio Color Theme
                        normY < 0.16f && normX < 0.55f -> {
                            WearTelemetryManager.cycleColorTheme(applicationContext)
                            WearTelemetryManager.vibrateCasioDoubleBeep(applicationContext)
                            renderer.invalidate()
                        }
                        // [ACTION: FEAT-939] Tap SIG/ALM zone inside clock panel to toggle Hourly Chime & Shift Alert
                        normY in 0.29f..0.44f && normX >= 0.62f -> {
                            WearTelemetryManager.toggleSigAlm(applicationContext)
                            renderer.invalidate()
                        }
                        // Tap Bottom Weekly Matrix or Top Duty Box to open Watch Heatmap App
                        normY > 0.65f || normY in 0.16f..0.28f -> {
                            renderer.refreshStateAsync()
                            try {
                                val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                applicationContext.startActivity(launchIntent)
                            } catch (_: Exception) {
                            }
                        }
                        else -> {
                            renderer.refreshStateAsync()
                        }
                    }
                }
            }
        })

        return watchFace
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private inner class CasioSharedAssets : Renderer.SharedAssets {
        override fun onDestroy() {}
    }

    private inner class CasioWatchCanvasRenderer(
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        private val repository: WearSalaryRepository,
        initialDuty: WearTodayDutySummary,
        initialTelemetry: WearTelemetrySnapshot
    ) : Renderer.CanvasRenderer2<CasioSharedAssets>(
        surfaceHolder = surfaceHolder,
        currentUserStyleRepository = currentUserStyleRepository,
        watchState = watchState,
        canvasType = CanvasType.HARDWARE,
        interactiveDrawModeUpdateDelayMillis = 1000L,
        clearWithBackgroundTintBeforeRenderingHighlightLayer = false
    ) {
        @Volatile
        private var cachedDuty: WearTodayDutySummary = initialDuty

        @Volatile
        private var cachedTelemetry: WearTelemetrySnapshot = initialTelemetry

        private var lastRefreshMinute: Int = -1

        override suspend fun createSharedAssets(): CasioSharedAssets = CasioSharedAssets()

        fun refreshStateAsync() {
            serviceScope.launch {
                try {
                    cachedDuty = repository.getTodayDutySummary(LocalDate.now())
                    cachedTelemetry = WearTelemetryManager.refreshWeatherIfNeeded(applicationContext)
                    postInvalidate()
                } catch (_: Exception) {
                }
            }
        }

        override fun render(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: CasioSharedAssets
        ) {
            cachedTelemetry = WearTelemetryManager.getSnapshot(applicationContext)
            WearTelemetryManager.checkAndTriggerCasioSignals(
                applicationContext,
                zonedDateTime,
                cachedDuty.hasDuty
            )

            if (zonedDateTime.minute != lastRefreshMinute) {
                lastRefreshMinute = zonedDateTime.minute
                refreshStateAsync()
            }

            val isAmbient = (renderParameters.drawMode == DrawMode.AMBIENT)
            CasioDigitalCanvasRenderer.drawCasioWatchFace(
                canvas = canvas,
                width = bounds.width(),
                height = bounds.height(),
                zonedDateTime = zonedDateTime,
                dutySummary = cachedDuty,
                telemetry = cachedTelemetry,
                isAmbient = isAmbient,
                showSeconds = !isAmbient
            )
        }

        override fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: CasioSharedAssets
        ) {
            canvas.drawColor(renderParameters.highlightLayer?.backgroundTint ?: 0)
        }
    }
}
