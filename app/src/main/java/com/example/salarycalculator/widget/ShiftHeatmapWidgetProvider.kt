package com.example.salarycalculator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import com.example.salarycalculator.MainActivity
import com.example.salarycalculator.R
import com.example.salarycalculator.domain.PayScheduleConfig
import com.example.salarycalculator.domain.PayScheduleEngine
import com.example.salarycalculator.domain.SalaryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.DateFormatSymbols
import java.util.Calendar

class ShiftHeatmapWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        if (action == ACTION_CYCLE_TODAY_SHIFT) {
            cycleTodayShift(context)
            return
        }

        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val thisWidget = ComponentName(context, ShiftHeatmapWidgetProvider::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
        for (id in allWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    private fun cycleTodayShift(context: Context) {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthKey = "$year-$month"

        val repository = SalaryRepository(context)
        runBlocking {
            try {
                val currentScheduleJson = repository.getAnnualShiftSchedule().first()
                val standardHours = repository.getDefaultHoursPerDay().first()

                val multiYearShifts = SalaryRepository.parseShiftScheduleJson(currentScheduleJson).mapValues {
                    it.value.toMutableMap()
                }.toMutableMap()

                val monthMap = multiYearShifts.getOrPut(monthKey) { mutableMapOf() }
                val currentHours = monthMap[day] ?: 0.0

                val nextHours = ShiftCycleEngine.getNextShiftState(currentHours, standardHours)
                if (nextHours == 0.0) {
                    monthMap.remove(day)
                } else {
                    monthMap[day] = nextHours
                }

                val updatedJson = SalaryRepository.encodeShiftSchedule(multiYearShifts)
                repository.setAnnualShiftSchedule(updatedJson)

                // User visual confirmation feedback
                Toast.makeText(
                    context,
                    ShiftCycleEngine.getShiftConfirmationToast(nextHours),
                    Toast.LENGTH_SHORT
                ).show()

                // Refresh all active widgets immediately
                ShiftHeatmapWidgetManager.updateAllWidgets(context)
            } catch (_: Exception) {}
        }
    }

    private data class ShiftDataBundle(
        val scheduleJson: String,
        val hourlyRate: Double,
        val standardHours: Double,
        val payConfig: PayScheduleConfig,
        val isTransparent: Boolean
    )

    companion object {

        const val ACTION_CYCLE_TODAY_SHIFT = "com.example.salarycalculator.widget.CYCLE_TODAY_SHIFT"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            // Read shift schedule and preferences from DataStore via SalaryRepository
            val repository = SalaryRepository(context)
            val dataBundle = runBlocking {
                try {
                    val sched = repository.getAnnualShiftSchedule().first()
                    val rate = repository.getDefaultHourlyRate().first()
                    val stdH = repository.getDefaultHoursPerDay().first()
                    val pConfig = repository.getPayScheduleConfig().first()
                    val trans = repository.getWidgetTransparentMode().first()
                    ShiftDataBundle(sched, rate, stdH, pConfig, trans)
                } catch (_: Exception) {
                    ShiftDataBundle("", 12.82, 12.0, PayScheduleConfig(), false)
                }
            }

            // Always target the current calendar month
            val cal = Calendar.getInstance()
            val targetYear = cal.get(Calendar.YEAR)
            val targetMonth = cal.get(Calendar.MONTH) + 1 // 1..12
            val todayDay = cal.get(Calendar.DAY_OF_MONTH)

            // Parse shift records for the current month
            val multiYearShifts = SalaryRepository.parseShiftScheduleJson(dataBundle.scheduleJson)
            val monthShifts = multiYearShifts["$targetYear-$targetMonth"] ?: emptyMap()

            // Calculate shift metrics
            val daysWorked = monthShifts.values.count { it != 0.0 }
            val totalHours = monthShifts.values.sumOf {
                when (it) {
                    SalaryRepository.ANNUAL_LEAVE_HOURS -> 7.5
                    SalaryRepository.STATUTORY_SICK_LEAVE_HOURS -> 0.0
                    else -> kotlin.math.abs(it)
                }
            }
            val estGross = totalHours * dataBundle.hourlyRate

            val monthNames = DateFormatSymbols().months
            val monthTitle = "${monthNames.getOrElse(targetMonth - 1) { "Month $targetMonth" }} $targetYear"
            val summaryText = when {
                daysWorked > 0 -> "$daysWorked Shifts (${"%.0f".format(totalHours)}h) • ~£${"%,.2f".format(estGross)}"
                else -> "No shifts logged"
            }

            val views = RemoteViews(context.packageName, R.layout.widget_shift_heatmap)
            views.setTextViewText(R.id.widget_month_title, monthTitle)
            views.setTextViewText(R.id.widget_summary_text, summaryText)

            // FEAT-930: Interactive Quick Shift Pill for Today
            val todayShift = monthShifts[todayDay] ?: 0.0
            views.setTextViewText(
                R.id.widget_btn_quick_log_today,
                ShiftCycleEngine.getShiftStateLabel(todayShift)
            )
            views.setInt(
                R.id.widget_btn_quick_log_today,
                "setBackgroundResource",
                ShiftCycleEngine.getShiftPillBackgroundRes(todayShift)
            )

            val cycleIntent = Intent(context, ShiftHeatmapWidgetProvider::class.java).apply {
                action = ACTION_CYCLE_TODAY_SHIFT
            }
            val cyclePendingIntent = PendingIntent.getBroadcast(
                context,
                301,
                cycleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_quick_log_today, cyclePendingIntent)

            // Transparent vs Glass Background
            val bgRes = if (dataBundle.isTransparent) R.drawable.widget_transparent_bg else R.drawable.widget_glass_bg
            views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

            // Dynamic Widget Dimensions for pixel-perfect full-size rendering
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val density = context.resources.displayMetrics.density

            val widthDp = if (minWidthDp > 0) minWidthDp else 320
            val heightDp = if (minHeightDp > 0) minHeightDp else 240

            // Generate crisp bitmap matching the exact user-resized widget dimensions
            val bitmapWidth = kotlin.math.max(600, (widthDp * density).toInt())
            val bitmapHeight = kotlin.math.max(400, (heightDp * density).toInt())

            val bitmap = renderHeatmapBitmap(
                width = bitmapWidth,
                height = bitmapHeight,
                year = targetYear,
                month = targetMonth,
                shifts = monthShifts,
                standardHours = dataBundle.standardHours,
                payConfig = dataBundle.payConfig
            )
            views.setImageViewBitmap(R.id.widget_heatmap_image, bitmap)

            // Open app on clicking header title or canvas
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                200,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_month_title, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_summary_text, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_heatmap_image, launchPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Renders a crisp, responsive 7-column calendar matrix bitmap using the full widget canvas.
         */
        fun renderHeatmapBitmap(
            width: Int,
            height: Int,
            year: Int,
            month: Int,
            shifts: Map<Int, Double>,
            standardHours: Double,
            payConfig: PayScheduleConfig = PayScheduleConfig()
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1
            val todayDay = cal.get(Calendar.DAY_OF_MONTH)

            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
            val startDayOffset = (dow + 5) % 7 // 0=Mon..6=Sun

            // Calculate PayDay and Cutoff Day for this month
            val payPeriod = PayScheduleEngine.calculatePayPeriod(year, month, payConfig)

            val cellWidth = width / 7f
            val headerHeight = kotlin.math.min(height * 0.09f, 40f)
            val gridTop = headerHeight + 4f
            val numRows = (startDayOffset + daysInMonth + 6) / 7
            val availableGridHeight = height - gridTop - 4f
            val cellHeight = availableGridHeight / numRows

            // Day Headers Paint
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = kotlin.math.min(cellWidth * 0.28f, 22f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            for (i in 0 until 7) {
                val cx = i * cellWidth + cellWidth / 2f
                canvas.drawText(dayLabels[i], cx, headerHeight - 8f, headerPaint)
            }

            // Paints for Cells
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = kotlin.math.max(3f, cellWidth * 0.035f)
            }
            val todayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = kotlin.math.max(2.5f, cellWidth * 0.03f)
                color = Color.parseColor("#38BDF8") // Cyan highlight ring for Today
            }
            val dayNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = kotlin.math.min(cellWidth * 0.28f, cellHeight * 0.34f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = kotlin.math.min(cellWidth * 0.22f, cellHeight * 0.26f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val margin = kotlin.math.max(2.5f, cellWidth * 0.035f)
            val cornerRadius = kotlin.math.max(4f, cellWidth * 0.12f)

            for (row in 0 until numRows) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startDayOffset + 1

                    val left = col * cellWidth + margin
                    val top = gridTop + row * cellHeight + margin
                    val right = (col + 1) * cellWidth - margin
                    val bottom = gridTop + (row + 1) * cellHeight - margin
                    val rect = RectF(left, top, right, bottom)

                    if (dayNum in 1..daysInMonth) {
                        val hours = shifts[dayNum] ?: 0.0
                        val isAnnualLeave = (hours == SalaryRepository.ANNUAL_LEAVE_HOURS)
                        val isSickLeave = (hours == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS)
                        val isOt = !isAnnualLeave && !isSickLeave && (hours < 0.0 || hours > 8.0)

                        val isCutoff = (dayNum == payPeriod.cutoffDay)
                        val isPayday = (dayNum == payPeriod.payDay)
                        val isToday = (dayNum == todayDay && year == currentYear && month == currentMonth)

                        val (bgColor, textColor, badgeLabel) = when {
                            isAnnualLeave -> Triple(Color.parseColor("#1E88E5"), Color.WHITE, "A/L")
                            isSickLeave -> Triple(Color.parseColor("#A855F7"), Color.WHITE, "SSP")
                            isOt -> {
                                val otHours = kotlin.math.abs(hours)
                                val otStr = if (otHours % 1.0 == 0.0) "${otHours.toInt()}h OT" else "${otHours}h OT"
                                Triple(Color.parseColor("#F59E0B"), Color.parseColor("#0F172A"), otStr)
                            }
                            hours >= standardHours -> {
                                val hrsStr = if (hours % 1.0 == 0.0) "${hours.toInt()}h" else "${hours}h"
                                Triple(Color.parseColor("#10B981"), Color.WHITE, hrsStr)
                            }
                            hours > 0.0 -> {
                                val hrsStr = if (hours % 1.0 == 0.0) "${hours.toInt()}h" else "${hours}h"
                                Triple(Color.parseColor("#0D9488"), Color.WHITE, hrsStr)
                            }
                            else -> Triple(Color.parseColor("#1E293B"), Color.parseColor("#CBD5E1"), "")
                        }

                        // Draw Rounded Box
                        boxPaint.color = bgColor
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint)

                        // Special Borders for Payday / Cutoff
                        if (isPayday) {
                            borderPaint.color = Color.parseColor("#10B981") // Emerald
                            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
                        } else if (isCutoff) {
                            borderPaint.color = Color.parseColor("#F59E0B") // Amber
                            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
                        }

                        // Today highlight ring
                        if (isToday) {
                            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, todayRingPaint)
                        }

                        // Draw Text
                        val cx = rect.centerX()
                        dayNumPaint.color = textColor
                        labelPaint.color = textColor

                        if (badgeLabel.isNotBlank()) {
                            canvas.drawText(dayNum.toString(), cx, top + (cellHeight * 0.44f), dayNumPaint)
                            canvas.drawText(badgeLabel, cx, top + (cellHeight * 0.84f), labelPaint)
                        } else {
                            canvas.drawText(dayNum.toString(), cx, rect.centerY() + (dayNumPaint.textSize * 0.35f), dayNumPaint)
                        }
                    } else {
                        // Off-month grid filler cell
                        boxPaint.color = Color.parseColor("#0F172A")
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint)
                    }
                }
            }

            return bitmap
        }
    }
}
