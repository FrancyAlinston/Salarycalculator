package com.example.salarycalculator.wear.tile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
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
import com.example.salarycalculator.wear.domain.TaxCalculator
import com.example.salarycalculator.wear.domain.WearDutyState
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import com.example.salarycalculator.wear.domain.WearTodayDutySummary
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

/**
 * [ACTION: FEAT-927]
 * Samsung Galaxy Watch 8 Classic Super AMOLED Quick Tile.
 * Displays whether the current day has duty or not ("Night Duty" vs "Off Duty")
 * with a glowing circular chronograph aesthetic and 7-day weekly rota strip.
 * Does NOT show net pay on the tile.
 */
class WearShiftTileService : TileService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private const val IMAGE_DUTY_CARD_ID = "duty_card_canvas"
        private const val TILE_SIZE_PX = 480
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            serviceScope.launch {
                try {
                    val repo = WearSalaryRepository(applicationContext)
                    val summary = repo.getTodayDutySummary(LocalDate.now())
                    val versionKey = "${summary.dateKey}_${summary.dutyState.name}_${summary.shiftsThisWeek}"

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
                        .takeIf { it > 0 }?.toFloat() ?: 220f
                    val screenHeightDp = requestParams.deviceConfiguration.screenHeightDp
                        .takeIf { it > 0 }?.toFloat() ?: 220f

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
                        .setFreshnessIntervalMillis(15 * 60 * 1000L) // Refresh every 15 minutes
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
                    val pngBytes = renderDutyTilePng(summary)

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

    /**
     * Renders a luxury 480x480 circular Super AMOLED watch tile graphic for Samsung Galaxy Watch 8 Classic.
     */
    private fun renderDutyTilePng(summary: WearTodayDutySummary): ByteArray {
        val size = TILE_SIZE_PX
        val cx = size / 2f
        val cy = size / 2f
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Primary Palette based on Today's Duty Status
        val primaryAccent: Int
        val secondaryAccent: Int
        val auraCenterColor: Int
        val pillBgColor: Int

        when (summary.dutyState) {
            WearDutyState.NIGHT_DUTY -> {
                primaryAccent = Color.parseColor("#14B8A6")   // Vibrant Teal
                secondaryAccent = Color.parseColor("#10B981") // Emerald
                auraCenterColor = Color.parseColor("#380D9488")
                pillBgColor = Color.parseColor("#FF042F2E")
            }
            WearDutyState.OVERTIME -> {
                primaryAccent = Color.parseColor("#F59E0B")   // Warm Gold/Amber
                secondaryAccent = Color.parseColor("#FBBF24")
                auraCenterColor = Color.parseColor("#38D97706")
                pillBgColor = Color.parseColor("#FF451A03")
            }
            WearDutyState.ANNUAL_LEAVE -> {
                primaryAccent = Color.parseColor("#3B82F6")   // Royal Blue
                secondaryAccent = Color.parseColor("#60A5FA")
                auraCenterColor = Color.parseColor("#382563EB")
                pillBgColor = Color.parseColor("#FF172554")
            }
            WearDutyState.SICK_LEAVE -> {
                primaryAccent = Color.parseColor("#A855F7")   // Purple
                secondaryAccent = Color.parseColor("#C084FC")
                auraCenterColor = Color.parseColor("#389333EA")
                pillBgColor = Color.parseColor("#FF3B0764")
            }
            WearDutyState.OFF_DUTY -> {
                primaryAccent = Color.parseColor("#38BDF8")   // Calm Sky Cyan
                secondaryAccent = Color.parseColor("#94A3B8") // Slate
                auraCenterColor = Color.parseColor("#240284C7")
                pillBgColor = Color.parseColor("#FF0F172A")
            }
        }

        // 2. Pure AMOLED Black Base + Radial Ambient Glow
        canvas.drawColor(Color.BLACK)
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy - 34f, 210f,
                intArrayOf(auraCenterColor, Color.parseColor("#12090D16"), Color.BLACK),
                floatArrayOf(0f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, 232f, auraPaint)

        // 3. Outer 60-Tick Samsung Watch 8 Classic Chronograph Bezel Ring
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 60) {
            val angleDeg = i * 6.0 - 90.0
            val angleRad = Math.toRadians(angleDeg)
            val isMajor = (i % 5 == 0)
            val outerR = 232f
            val innerR = if (isMajor) 221f else 226f
            tickPaint.strokeWidth = if (isMajor) 2.5f else 1.3f
            tickPaint.color = if (isMajor) Color.parseColor("#5594A3B8") else Color.parseColor("#2864748B")
            val x1 = cx + (outerR * cos(angleRad)).toFloat()
            val y1 = cy + (outerR * sin(angleRad)).toFloat()
            val x2 = cx + (innerR * cos(angleRad)).toFloat()
            val y2 = cy + (innerR * sin(angleRad)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        // 4. Glowing Top Status Arc (215° to 325°)
        val arcRect = RectF(14f, 14f, size - 14f, size - 14f)
        val arcGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            color = primaryAccent
            setShadowLayer(12f, 0f, 0f, primaryAccent)
        }
        canvas.drawArc(arcRect, 220f, 100f, false, arcGlowPaint)

        // 5. Top Formatted Date Header ("SAT, 31 OCT")
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 21f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
        }
        canvas.drawText(summary.formattedDate, cx, 64f, datePaint)

        // 6. Glowing Centerpiece Emblem Circle & Custom Moon / Rest Icon (y = 128f)
        val emblemCy = 126f
        val emblemR = 38f
        val emblemBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = pillBgColor
        }
        val emblemRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = primaryAccent
        }
        canvas.drawCircle(cx, emblemCy, emblemR, emblemBgPaint)
        canvas.drawCircle(cx, emblemCy, emblemR, emblemRingPaint)

        if (summary.hasDuty) {
            // Draw Glowing Crescent Moon & Stars for "Night Duty"
            drawCrescentMoonAndStars(canvas, cx, emblemCy, primaryAccent, secondaryAccent)
        } else {
            // Draw Serene Sun / Star Emblem for "Off Duty" or "Annual Leave"
            drawOffDutySparkleEmblem(canvas, cx, emblemCy, primaryAccent)
        }

        // 7. HERO TITLE: "Night Duty" or "Off Duty" (NO NET PAY)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(14f, 0f, 2f, primaryAccent)
        }
        canvas.drawText(summary.title, cx, 220f, titlePaint)

        // 8. Glowing Status Badge Capsule ("ON DUTY TONIGHT" / "REST DAY TODAY")
        val badgeWidth = 248f
        val badgeHeight = 36f
        val badgeTop = 238f
        val badgeRect = RectF(cx - badgeWidth / 2f, badgeTop, cx + badgeWidth / 2f, badgeTop + badgeHeight)
        val badgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                badgeRect.left, badgeRect.top, badgeRect.right, badgeRect.bottom,
                pillBgColor, Color.parseColor("#FF090D16"),
                Shader.TileMode.CLAMP
            )
        }
        val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = primaryAccent
        }
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgeFillPaint)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgeStrokePaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryAccent
            textSize = 17f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        val dotPrefix = if (summary.hasDuty) "● " else "○ "
        canvas.drawText(dotPrefix + summary.badgeLabel, cx, badgeTop + 24f, badgeTextPaint)

        // 9. Subtitle ("20:00 – 08:00 • 12h Shift" or "Next: Mon, 02 Nov")
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(summary.subtitle, cx, 306f, subPaint)

        // 10. 7-Day Mini Weekly Rota Strip Card (Mon .. Sun)
        val stripWidth = 316f
        val stripHeight = 82f
        val stripTop = 324f
        val stripRect = RectF(cx - stripWidth / 2f, stripTop, cx + stripWidth / 2f, stripTop + stripHeight)
        val stripBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FF111827")
        }
        val stripBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#FF1E293B")
        }
        canvas.drawRoundRect(stripRect, 24f, 24f, stripBgPaint)
        canvas.drawRoundRect(stripRect, 24f, 24f, stripBorderPaint)

        val nodeSpacing = 41f
        val startNodeX = cx - (3 * nodeSpacing)
        val dayLetterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val nodeDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val todayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#38BDF8")
        }

        summary.weekDays.forEachIndexed { idx, node ->
            val nx = startNodeX + idx * nodeSpacing
            dayLetterPaint.color = if (node.isToday) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8")
            canvas.drawText(node.dayLetter, nx, stripTop + 22f, dayLetterPaint)

            val orbCy = stripTop + 53f
            val orbR = 16.5f
            orbPaint.color = when (node.dutyState) {
                WearDutyState.NIGHT_DUTY -> Color.parseColor("#0D9488")
                WearDutyState.OVERTIME -> Color.parseColor("#F59E0B")
                WearDutyState.ANNUAL_LEAVE -> Color.parseColor("#3B82F6")
                WearDutyState.SICK_LEAVE -> Color.parseColor("#A855F7")
                WearDutyState.OFF_DUTY -> Color.parseColor("#1E293B")
            }
            canvas.drawCircle(nx, orbCy, orbR, orbPaint)

            if (node.isToday) {
                canvas.drawCircle(nx, orbCy, orbR + 2.5f, todayRingPaint)
            }

            nodeDatePaint.color = when (node.dutyState) {
                WearDutyState.NIGHT_DUTY, WearDutyState.OVERTIME -> Color.BLACK
                WearDutyState.ANNUAL_LEAVE, WearDutyState.SICK_LEAVE -> Color.WHITE
                WearDutyState.OFF_DUTY -> if (node.isToday) Color.parseColor("#38BDF8") else Color.parseColor("#CBD5E1")
            }
            canvas.drawText(node.dayOfMonth.toString(), nx, orbCy + 5.5f, nodeDatePaint)
        }

        // 11. Bottom Footer Caption
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText("TAP FOR SHIFT HEATMAP", cx, 434f, footerPaint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    private fun drawCrescentMoonAndStars(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        primaryColor: Int,
        starColor: Int
    ) {
        val moonBitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val moonCanvas = Canvas(moonBitmap)
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = primaryColor
        }
        // Outer moon circle
        moonCanvas.drawCircle(38f, 42f, 20f, moonPaint)
        // Cutout circle using PorterDuff CLEAR to form a crisp crescent moon
        val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }
        moonCanvas.drawCircle(47f, 34f, 17f, clearPaint)
        canvas.drawBitmap(moonBitmap, cx - 40f, cy - 40f, null)

        // Draw 2 sparkling 4-point stars next to the crescent moon
        drawFourPointStar(canvas, cx + 14f, cy - 11f, 7.5f, starColor)
        drawFourPointStar(canvas, cx + 5f, cy + 4f, 4.5f, Color.WHITE)
    }

    private fun drawOffDutySparkleEmblem(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        accentColor: Int
    ) {
        drawFourPointStar(canvas, cx - 4f, cy - 2f, 15f, accentColor)
        drawFourPointStar(canvas, cx + 12f, cy - 12f, 6.5f, Color.WHITE)
        drawFourPointStar(canvas, cx + 10f, cy + 10f, 5f, accentColor)
    }

    private fun drawFourPointStar(
        canvas: Canvas,
        x: Float,
        y: Float,
        r: Float,
        color: Int
    ) {
        val inner = r * 0.32f
        val path = Path().apply {
            moveTo(x, y - r)
            lineTo(x + inner, y - inner)
            lineTo(x + r, y)
            lineTo(x + inner, y + inner)
            lineTo(x, y + r)
            lineTo(x - inner, y + inner)
            lineTo(x - r, y)
            lineTo(x - inner, y - inner)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        canvas.drawPath(path, paint)
    }
}
