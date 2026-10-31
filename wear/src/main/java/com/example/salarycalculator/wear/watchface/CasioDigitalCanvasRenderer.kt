package com.example.salarycalculator.wear.watchface

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.salarycalculator.wear.domain.WearDutyState
import com.example.salarycalculator.wear.domain.WearTelemetryManager
import com.example.salarycalculator.wear.domain.WearTelemetrySnapshot
import com.example.salarycalculator.wear.domain.WearTodayDutySummary
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders the Casio Classic F-91W / G-Shock Stealth Black Edition Digital Watch Face
 * on any circular Android Canvas (used by System WatchFaceService, Quick Tile, and Watch UI).
 * Features:
 * - Pure Black (#000000) Super AMOLED background
 * - Authentic 7-segment digital LCD clock (HH:MM + :SS + ghost segments)
 * - Selectable Casio Retro LCD Color Themes (`[ACTION: FEAT-938]`)
 * - Casio Hourly Chime (`SIG`) & Night Shift Alert (`ALM`) indicators (`[ACTION: FEAT-939]`)
 * - Live Watch Battery Percentage (`BATT %` + 4-bar meter) (`[ACTION: PEND-930]`)
 * - Live Step Count box with segmented LCD progress bar
 * - Live Weather box (°C + condition + High/Low)
 * - Today's Duty Status ("NIGHT DUTY" vs "OFF DUTY") — no net pay exposed
 * - Bottom 7-day (M T W T F S S) Casio weekly shift matrix
 */
object CasioDigitalCanvasRenderer {

    // 7-segment bitmask: [0]=Top, [1]=TopRight, [2]=BottomRight, [3]=Bottom, [4]=BottomLeft, [5]=TopLeft, [6]=Middle
    private val DIGIT_SEGMENTS = mapOf(
        '0' to intArrayOf(1, 1, 1, 1, 1, 1, 0),
        '1' to intArrayOf(0, 1, 1, 0, 0, 0, 0),
        '2' to intArrayOf(1, 1, 0, 1, 1, 0, 1),
        '3' to intArrayOf(1, 1, 1, 1, 0, 0, 1),
        '4' to intArrayOf(0, 1, 1, 0, 0, 1, 1),
        '5' to intArrayOf(1, 0, 1, 1, 0, 1, 1),
        '6' to intArrayOf(1, 0, 1, 1, 1, 1, 1),
        '7' to intArrayOf(1, 1, 1, 0, 0, 0, 0),
        '8' to intArrayOf(1, 1, 1, 1, 1, 1, 1),
        '9' to intArrayOf(1, 1, 1, 1, 0, 1, 1),
        '-' to intArrayOf(0, 0, 0, 0, 0, 0, 1),
        ' ' to intArrayOf(0, 0, 0, 0, 0, 0, 0)
    )

    fun drawCasioWatchFace(
        canvas: Canvas,
        width: Int,
        height: Int,
        zonedDateTime: ZonedDateTime,
        dutySummary: WearTodayDutySummary,
        telemetry: WearTelemetrySnapshot,
        isAmbient: Boolean = false,
        showSeconds: Boolean = !isAmbient
    ) {
        val baseSize = 480f
        val scale = min(width, height) / baseSize

        canvas.save()
        canvas.scale(scale, scale)

        val cx = baseSize / 2f
        val cy = baseSize / 2f

        // 1. PURE BLACK (#000000) SUPER AMOLED BACKGROUND
        canvas.drawColor(Color.BLACK)

        // [ACTION: FEAT-938] Colors based on selected Casio Color Theme, duty state, and ambient mode
        val theme = telemetry.colorTheme
        val casioFrameColor = if (isAmbient) Color.parseColor("#334155") else Color.parseColor(theme.frameHex)
        val casioGold = if (isAmbient) Color.parseColor("#64748B") else Color.parseColor(theme.subHeaderHex)
        val casioRed = if (isAmbient) Color.parseColor("#475569") else Color.parseColor("#EF4444")
        val lcdLitColor = if (isAmbient) Color.parseColor("#CBD5E1") else Color.parseColor(theme.lcdLitHex)
        val lcdGhostColor = if (isAmbient) Color.TRANSPARENT else Color.parseColor("#111C24")
        val boxBorderColor = if (isAmbient) Color.parseColor("#1E293B") else Color.parseColor("#1E3A5F")

        val dutyAccent = when {
            isAmbient -> Color.parseColor("#94A3B8")
            dutySummary.dutyState == WearDutyState.NIGHT_DUTY -> Color.parseColor(theme.accentHex)
            dutySummary.dutyState == WearDutyState.OVERTIME -> Color.parseColor("#F59E0B")
            dutySummary.dutyState == WearDutyState.ANNUAL_LEAVE -> Color.parseColor("#3B82F6")
            dutySummary.dutyState == WearDutyState.SICK_LEAVE -> Color.parseColor("#A855F7")
            else -> Color.parseColor("#38BDF8")
        }

        // 2. Outer 60-Tick Chronograph Bezel Ring + [ACTION: FEAT-932] Shift Progress Arc
        if (!isAmbient) {
            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            for (i in 0 until 60) {
                val angleRad = Math.toRadians(i * 6.0 - 90.0)
                val isMajor = (i % 5 == 0)
                val outerR = 234f
                val innerR = if (isMajor) 224f else 229f
                tickPaint.strokeWidth = if (isMajor) 2.2f else 1.1f
                tickPaint.color = if (isMajor) Color.parseColor("#475569") else Color.parseColor("#22475569")
                canvas.drawLine(
                    cx + (outerR * cos(angleRad)).toFloat(),
                    cy + (outerR * sin(angleRad)).toFloat(),
                    cx + (innerR * cos(angleRad)).toFloat(),
                    cy + (innerR * sin(angleRad)).toFloat(),
                    tickPaint
                )
            }

            // Live Shift Progress Arc (220° .. 320°)
            val progressFraction = WearTelemetryManager.computeShiftProgressFraction(
                dutySummary.hasDuty,
                zonedDateTime.toLocalTime()
            )
            val arcRect = RectF(12f, 12f, baseSize - 12f, baseSize - 12f)
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                strokeCap = Paint.Cap.ROUND
                color = Color.parseColor("#1E293B")
            }
            canvas.drawArc(arcRect, 220f, 100f, false, trackPaint)

            if (progressFraction > 0f) {
                val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    strokeCap = Paint.Cap.ROUND
                    color = dutyAccent
                }
                canvas.drawArc(arcRect, 220f, 100f * progressFraction, false, activeArcPaint)
            }
        }

        // 3. Classic Casio Double-Line Inner Ring & Header Branding + [ACTION: PEND-930] Battery Meter
        val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = casioFrameColor
        }
        canvas.drawCircle(cx, cy, 216f, outerRingPaint)

        // Top Casio Header Text: "CASIO"
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 19f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.16f
        }
        canvas.drawText("CASIO", cx, 50f, brandPaint)

        // [ACTION: PEND-930] 4-Bar Digital Casio Battery Indicator + Theme Switcher Header
        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = casioGold
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.06f
        }
        val chargeGlyph = if (telemetry.isCharging) "⚡" else ""
        canvas.drawText(
            "◀ ${theme.label} • BATT ${telemetry.batteryPercent}%$chargeGlyph • MODE ▶",
            cx,
            68f,
            subBrandPaint
        )

        // Draw mini 4-bar Casio battery icon next to CASIO logo
        if (!isAmbient) {
            drawCasioBatteryIcon(canvas, cx + 52f, 36f, telemetry.batteryPercent)
        }

        // 4. TOP DIGITAL ROW: [ DAY & DATE ]  +  [ TODAY DUTY STATUS ]
        val topRowTop = 80f
        val topRowHeight = 52f

        // Left Box: Day of Week + Date (e.g., "SAT 31 OCT")
        val dateBoxRect = RectF(58f, topRowTop, 206f, topRowTop + topRowHeight)
        drawCasioFramedBox(canvas, dateBoxRect, Color.BLACK, boxBorderColor)

        val dowStr = zonedDateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
        val monthStr = zonedDateTime.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
        val dayNumStr = String.format(Locale.ENGLISH, "%02d", zonedDateTime.dayOfMonth)

        val microLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("DATE • 24H", dateBoxRect.left + 10f, dateBoxRect.top + 16f, microLabelPaint)

        val dateValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lcdLitColor
            textSize = 18.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("$dowStr $dayNumStr $monthStr", dateBoxRect.left + 10f, dateBoxRect.top + 41f, dateValuePaint)

        // Right Box: Today Duty Status ("NIGHT DUTY" vs "OFF DUTY" — NO NET PAY)
        val dutyBoxRect = RectF(214f, topRowTop, 422f, topRowTop + topRowHeight)
        val dutyBg = if (!isAmbient && dutySummary.hasDuty) Color.parseColor("#042F2E") else Color.BLACK
        drawCasioFramedBox(canvas, dutyBoxRect, dutyBg, dutyAccent)

        val dutyMicroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dutyAccent
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val dutyHeaderTag = when (dutySummary.dutyState) {
            WearDutyState.NIGHT_DUTY -> "● 20:00-08:00 (12H)"
            WearDutyState.OVERTIME -> "⚡ OVERTIME SHIFT"
            WearDutyState.ANNUAL_LEAVE -> "🌴 PAID LEAVE DAY"
            WearDutyState.SICK_LEAVE -> "💜 SICK LEAVE"
            WearDutyState.OFF_DUTY -> "○ NEXT: ${dutySummary.nextDutyFormatted.uppercase()}"
        }
        canvas.drawText(dutyHeaderTag, dutyBoxRect.left + 10f, dutyBoxRect.top + 16f, dutyMicroPaint)

        val dutyMainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dutySummary.hasDuty) dutyAccent else lcdLitColor
            textSize = 21f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.04f
        }
        canvas.drawText(dutySummary.title.uppercase(), dutyBoxRect.left + 10f, dutyBoxRect.top + 42f, dutyMainPaint)

        // 5. CENTERPIECE 7-SEGMENT DIGITAL LCD CLOCK PANEL (HH:MM + :SS + SIG/ALM)
        val clockBoxRect = RectF(42f, 140f, 438f, 254f)
        drawCasioFramedBox(canvas, clockBoxRect, Color.BLACK, casioFrameColor, strokeWidth = 2.2f)

        // Inner thin Casio trim line inside clock box
        val trimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = if (isAmbient) Color.parseColor("#1E293B") else casioFrameColor
            alpha = if (isAmbient) 255 else 90
        }
        canvas.drawRoundRect(
            RectF(clockBoxRect.left + 4f, clockBoxRect.top + 4f, clockBoxRect.right - 4f, clockBoxRect.bottom - 4f),
            8f, 8f, trimPaint
        )

        val hourStr = String.format(Locale.ENGLISH, "%02d", zonedDateTime.hour)
        val minStr = String.format(Locale.ENGLISH, "%02d", zonedDateTime.minute)
        val secStr = String.format(Locale.ENGLISH, "%02d", zonedDateTime.second)

        // Draw 4 Large 7-Segment Digits for HH:MM
        val digitW = 48f
        val digitH = 82f
        val digitThick = 9.2f
        val clockTop = 156f
        val h1X = 62f
        val h2X = 120f
        val colonX = 182f
        val m1X = 198f
        val m2X = 256f

        drawSevenSegmentChar(canvas, hourStr[0], h1X, clockTop, digitW, digitH, digitThick, lcdLitColor, lcdGhostColor)
        drawSevenSegmentChar(canvas, hourStr[1], h2X, clockTop, digitW, digitH, digitThick, lcdLitColor, lcdGhostColor)

        // Digital Colon (:)
        val colonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (!isAmbient && (zonedDateTime.second % 2 == 1) && showSeconds) {
                dutyAccent
            } else {
                lcdLitColor
            }
        }
        canvas.drawRect(colonX, clockTop + 22f, colonX + 8f, clockTop + 31f, colonPaint)
        canvas.drawRect(colonX, clockTop + 51f, colonX + 8f, clockTop + 60f, colonPaint)

        drawSevenSegmentChar(canvas, minStr[0], m1X, clockTop, digitW, digitH, digitThick, lcdLitColor, lcdGhostColor)
        drawSevenSegmentChar(canvas, minStr[1], m2X, clockTop, digitW, digitH, digitThick, lcdLitColor, lcdGhostColor)

        // [ACTION: FEAT-939] Casio SIG (Hourly Chime) & ALM (Shift Start Alert) Status Header above Seconds
        val secStatusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (telemetry.sigEnabled || telemetry.almEnabled) dutyAccent else Color.parseColor("#64748B")
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(telemetry.sigAlmLabel, 318f, clockTop + 16f, secStatusPaint)

        val secW = 31f
        val secH = 52f
        val secThick = 6.2f
        val secTop = clockTop + 28f
        val s1X = 322f
        val s2X = 360f
        if (showSeconds) {
            drawSevenSegmentChar(canvas, secStr[0], s1X, secTop, secW, secH, secThick, dutyAccent, lcdGhostColor)
            drawSevenSegmentChar(canvas, secStr[1], s2X, secTop, secW, secH, secThick, dutyAccent, lcdGhostColor)
        } else {
            drawSevenSegmentChar(canvas, '-', s1X, secTop, secW, secH, secThick, dutyAccent, lcdGhostColor)
            drawSevenSegmentChar(canvas, '-', s2X, secTop, secW, secH, secThick, dutyAccent, lcdGhostColor)
        }

        // 6. MIDDLE TELEMETRY ROW: [ 👟 STEP COUNT ]  +  [ 🌡️ WEATHER ]
        val teleTop = 262f
        val teleHeight = 62f

        // Left Telemetry Box: STEP COUNT
        val stepsBoxRect = RectF(50f, teleTop, 236f, teleTop + teleHeight)
        drawCasioFramedBox(canvas, stepsBoxRect, Color.BLACK, boxBorderColor)

        val teleHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = casioGold
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("STEPS", stepsBoxRect.left + 10f, stepsBoxRect.top + 17f, teleHeaderPaint)

        // Casio 8-segment LCD Step Progress Bar on top-right of Steps box
        val barSegments = 8
        val activeSegments = (telemetry.stepProgressFraction * barSegments).toInt().coerceIn(0, barSegments)
        val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (s in 0 until barSegments) {
            val sx = stepsBoxRect.left + 80f + s * 11.5f
            segPaint.color = if (s < activeSegments) dutyAccent else Color.parseColor("#1E293B")
            canvas.drawRect(sx, stepsBoxRect.top + 8f, sx + 8.5f, stepsBoxRect.top + 17f, segPaint)
        }

        val stepsValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lcdLitColor
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val formattedSteps = String.format(Locale.UK, "%,d", telemetry.stepsToday)
        canvas.drawText(formattedSteps, stepsBoxRect.left + 10f, stepsBoxRect.top + 49f, stepsValPaint)

        val stepsUnitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("/8K", stepsBoxRect.right - 10f, stepsBoxRect.top + 48f, stepsUnitPaint)

        // Right Telemetry Box: WEATHER
        val weatherBoxRect = RectF(244f, teleTop, 430f, teleTop + teleHeight)
        drawCasioFramedBox(canvas, weatherBoxRect, Color.BLACK, boxBorderColor)

        canvas.drawText("WEATHER", weatherBoxRect.left + 10f, weatherBoxRect.top + 17f, teleHeaderPaint)

        val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(telemetry.weather.conditionShort, weatherBoxRect.right - 10f, weatherBoxRect.top + 17f, condPaint)

        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lcdLitColor
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(telemetry.weather.formattedTemp, weatherBoxRect.left + 10f, weatherBoxRect.top + 49f, tempPaint)

        val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(telemetry.weather.formattedRange, weatherBoxRect.right - 10f, weatherBoxRect.top + 48f, hlPaint)

        // 7. BOTTOM 7-DAY CASIO WEEKLY SHIFT MATRIX (M T W T F S S)
        val weekBoxRect = RectF(74f, 332f, 406f, 404f)
        drawCasioFramedBox(canvas, weekBoxRect, Color.BLACK, boxBorderColor)

        val nodeSpacing = 44f
        val startNodeX = cx - (3 * nodeSpacing)
        val dayLetterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dayNumNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val todayBracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = Color.parseColor("#38BDF8")
        }

        dutySummary.weekDays.forEachIndexed { idx, node ->
            val nx = startNodeX + idx * nodeSpacing
            dayLetterPaint.color = if (node.isToday) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8")
            canvas.drawText(node.dayLetter, nx, weekBoxRect.top + 19f, dayLetterPaint)

            val cellRect = RectF(nx - 17f, weekBoxRect.top + 26f, nx + 17f, weekBoxRect.top + 62f)
            if (!isAmbient) {
                cellFillPaint.color = when (node.dutyState) {
                    WearDutyState.NIGHT_DUTY -> Color.parseColor("#0D9488")
                    WearDutyState.OVERTIME -> Color.parseColor("#D97706")
                    WearDutyState.ANNUAL_LEAVE -> Color.parseColor("#2563EB")
                    WearDutyState.SICK_LEAVE -> Color.parseColor("#7E22CE")
                    WearDutyState.OFF_DUTY -> Color.parseColor("#0F172A")
                }
                canvas.drawRoundRect(cellRect, 6f, 6f, cellFillPaint)
            }

            if (node.isToday) {
                canvas.drawRoundRect(
                    RectF(cellRect.left - 2f, cellRect.top - 2f, cellRect.right + 2f, cellRect.bottom + 2f),
                    7f, 7f, todayBracketPaint
                )
            }

            dayNumNodePaint.color = when {
                isAmbient -> lcdLitColor
                node.dutyState == WearDutyState.NIGHT_DUTY || node.dutyState == WearDutyState.OVERTIME -> Color.BLACK
                node.dutyState == WearDutyState.ANNUAL_LEAVE || node.dutyState == WearDutyState.SICK_LEAVE -> Color.WHITE
                node.isToday -> Color.parseColor("#38BDF8")
                else -> Color.parseColor("#CBD5E1")
            }
            canvas.drawText(node.dayOfMonth.toString(), nx, cellRect.centerY() + 5f, dayNumNodePaint)
        }

        // 8. Bottom Vintage Casio Water-Resist Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = casioRed
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText("WR 50M • NIGHT DUTY EDITION", cx, 428f, footerPaint)

        canvas.restore()
    }

    /**
     * [ACTION: PEND-930] Draws a 4-bar digital Casio battery icon.
     */
    private fun drawCasioBatteryIcon(canvas: Canvas, x: Float, y: Float, batteryPct: Int) {
        val bodyRect = RectF(x, y, x + 24f, y + 11f)
        val capRect = RectF(x + 24f, y + 3f, x + 26.5f, y + 8f)
        val borderColor = when {
            batteryPct <= 20 -> Color.parseColor("#EF4444")
            batteryPct <= 45 -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#10B981")
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            color = borderColor
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = borderColor
        }
        canvas.drawRoundRect(bodyRect, 2f, 2f, strokePaint)
        canvas.drawRect(capRect, fillPaint)

        val bars = ((batteryPct + 12) / 25).coerceIn(1, 4)
        for (b in 0 until bars) {
            val bx = x + 2.5f + b * 5f
            canvas.drawRect(bx, y + 2.5f, bx + 3.6f, y + 8.5f, fillPaint)
        }
    }

    private fun drawCasioFramedBox(
        canvas: Canvas,
        rect: RectF,
        bgColor: Int,
        borderColor: Int,
        strokeWidth: Float = 1.6f
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = bgColor
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = borderColor
        }
        canvas.drawRoundRect(rect, 10f, 10f, fillPaint)
        canvas.drawRoundRect(rect, 10f, 10f, strokePaint)
    }

    /**
     * Draws an authentic beveled 7-segment LCD numeral with optional dim ghost segments.
     */
    private fun drawSevenSegmentChar(
        canvas: Canvas,
        ch: Char,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        t: Float,
        litColor: Int,
        ghostColor: Int
    ) {
        val mask = DIGIT_SEGMENTS[ch] ?: DIGIT_SEGMENTS[' ']!!
        val halfH = h / 2f
        val gap = 1.8f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        drawHorizSeg(canvas, x + gap, y, w - 2 * gap, t, if (mask[0] == 1) litColor else ghostColor, paint)
        drawVertSeg(canvas, x + w - t, y + gap, t, halfH - 2 * gap, if (mask[1] == 1) litColor else ghostColor, paint)
        drawVertSeg(canvas, x + w - t, y + halfH + gap, t, halfH - 2 * gap, if (mask[2] == 1) litColor else ghostColor, paint)
        drawHorizSeg(canvas, x + gap, y + h - t, w - 2 * gap, t, if (mask[3] == 1) litColor else ghostColor, paint)
        drawVertSeg(canvas, x, y + halfH + gap, t, halfH - 2 * gap, if (mask[4] == 1) litColor else ghostColor, paint)
        drawVertSeg(canvas, x, y + gap, t, halfH - 2 * gap, if (mask[5] == 1) litColor else ghostColor, paint)
        drawHorizSeg(canvas, x + gap, y + halfH - t / 2f, w - 2 * gap, t, if (mask[6] == 1) litColor else ghostColor, paint)
    }

    private fun drawHorizSeg(canvas: Canvas, x: Float, y: Float, length: Float, thick: Float, color: Int, paint: Paint) {
        if (color == Color.TRANSPARENT) return
        paint.color = color
        val bev = thick * 0.45f
        val path = Path().apply {
            moveTo(x + bev, y)
            lineTo(x + length - bev, y)
            lineTo(x + length, y + thick / 2f)
            lineTo(x + length - bev, y + thick)
            lineTo(x + bev, y + thick)
            lineTo(x, y + thick / 2f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawVertSeg(canvas: Canvas, x: Float, y: Float, thick: Float, length: Float, color: Int, paint: Paint) {
        if (color == Color.TRANSPARENT) return
        paint.color = color
        val bev = thick * 0.45f
        val path = Path().apply {
            moveTo(x + thick / 2f, y)
            lineTo(x + thick, y + bev)
            lineTo(x + thick, y + length - bev)
            lineTo(x + thick / 2f, y + length)
            lineTo(x, y + length - bev)
            lineTo(x, y + bev)
            close()
        }
        canvas.drawPath(path, paint)
    }
}
