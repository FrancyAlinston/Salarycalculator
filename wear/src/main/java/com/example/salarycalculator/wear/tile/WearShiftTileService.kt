package com.example.salarycalculator.wear.tile

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.example.salarycalculator.wear.MainActivity
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import com.example.salarycalculator.wear.domain.WearTelemetryManager
import com.example.salarycalculator.wear.watchface.CasioDigitalCanvasRenderer
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * [ACTION: FEAT-927 / FEAT-931 / FEAT-932]
 * Samsung Galaxy Watch 8 Classic Super AMOLED Quick Tile — Casio Classic Black Digital Edition.
 * Renders the Casio F-91W / G-Shock Black Digital Watch layout with:
 * - Pure Black (#000000) background
 * - 7-Segment Digital Clock (HH:MM)
 * - Live Step Count & Weather
 * - Today's Duty Status ("NIGHT DUTY" vs "OFF DUTY") & 7-Day Weekly Rota Matrix
 * - Interactive tap cycling (`toggle_today_duty`) & 1-tap Heatmap launch
 */
class WearShiftTileService : TileService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private const val IMAGE_DUTY_CARD_ID = "casio_duty_card_canvas"
        private const val TILE_SIZE_PX = 480
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            serviceScope.launch {
                try {
                    val repo = WearSalaryRepository(applicationContext)
                    val todayStr = LocalDate.now().toString()

                    // [ACTION: FEAT-931] Interactive tile tap cycling when user taps the tile reload target
                    val lastClickId = requestParams.currentState.lastClickableId
                    if (lastClickId == "toggle_today_duty") {
                        repo.toggleShift(todayStr, 12.0)
                    }

                    val summary = repo.getTodayDutySummary(LocalDate.now())
                    val telemetry = WearTelemetryManager.refreshWeatherIfNeeded(applicationContext)
                    val nowMin = ZonedDateTime.now().let { "${it.hour}_${it.minute}" }
                    val versionKey = "${summary.dateKey}_${summary.dutyState.name}_${telemetry.stepsToday}_${telemetry.weather.tempCelsius}_${telemetry.colorTheme.id}_${telemetry.batteryPercent}_$nowMin"

                    val launchAction = ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setPackageName(packageName)
                                .setClassName(MainActivity::class.java.name)
                                .build()
                        )
                        .build()

                    val clickable = ModifiersBuilders.Clickable.Builder()
                        .setId("open_heatmap")
                        .setOnClick(launchAction)
                        .build()

                    val screenWidthDp = requestParams.deviceConfiguration.screenWidthDp
                        .takeIf { it > 0 }?.toFloat() ?: 210f
                    val screenHeightDp = requestParams.deviceConfiguration.screenHeightDp
                        .takeIf { it > 0 }?.toFloat() ?: 210f

                    val imageElement = LayoutElementBuilders.Image.Builder()
                        .setResourceId(IMAGE_DUTY_CARD_ID)
                        .setWidth(dp(screenWidthDp))
                        .setHeight(dp(screenHeightDp))
                        .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_FIT)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setClickable(clickable)
                                .build()
                        )
                        .build()

                    val rootBox = LayoutElementBuilders.Box.Builder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .addContent(imageElement)
                        .build()

                    val layout = LayoutElementBuilders.Layout.Builder()
                        .setRoot(rootBox)
                        .build()

                    val timeline = TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(
                            TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(layout)
                                .build()
                        )
                        .build()

                    val tile = TileBuilders.Tile.Builder()
                        .setResourcesVersion(versionKey)
                        .setFreshnessIntervalMillis(60 * 1000L) // Refresh every minute for digital clock & steps
                        .setTileTimeline(timeline)
                        .build()

                    completer.set(tile)
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
            "WearShiftTileService.onTileRequest"
        }
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        return CallbackToFutureAdapter.getFuture { completer ->
            serviceScope.launch {
                try {
                    val repo = WearSalaryRepository(applicationContext)
                    val summary = repo.getTodayDutySummary(LocalDate.now())
                    val telemetry = WearTelemetryManager.getSnapshot(applicationContext)

                    val bitmap = Bitmap.createBitmap(TILE_SIZE_PX, TILE_SIZE_PX, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    CasioDigitalCanvasRenderer.drawCasioWatchFace(
                        canvas = canvas,
                        width = TILE_SIZE_PX,
                        height = TILE_SIZE_PX,
                        zonedDateTime = ZonedDateTime.now(),
                        dutySummary = summary,
                        telemetry = telemetry,
                        isAmbient = false,
                        showSeconds = false
                    )

                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    val pngBytes = out.toByteArray()

                    val inlineImage = ResourceBuilders.InlineImageResource.Builder()
                        .setData(pngBytes)
                        .setWidthPx(TILE_SIZE_PX)
                        .setHeightPx(TILE_SIZE_PX)
                        .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
                        .build()

                    val imageResource = ResourceBuilders.ImageResource.Builder()
                        .setInlineResource(inlineImage)
                        .build()

                    val resources = ResourceBuilders.Resources.Builder()
                        .setVersion(requestParams.version)
                        .addIdToImageMapping(IMAGE_DUTY_CARD_ID, imageResource)
                        .build()

                    completer.set(resources)
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
            "WearShiftTileService.onTileResourcesRequest"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
