package com.example.salarycalculator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.widget.RemoteViews
import com.example.salarycalculator.MainActivity
import com.example.salarycalculator.R
import com.example.salarycalculator.domain.PayScheduleConfig
import com.example.salarycalculator.domain.PayScheduleEngine
import com.example.salarycalculator.domain.SalaryRepository
import java.text.DateFormatSymbols
import java.util.Calendar

class ShiftHeatmapWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        val prefs = getPrefs(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, ShiftHeatmapWidgetProvider::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        when (action) {
            ACTION_PREV_MONTH -> {
                val offset = prefs.getInt(PREF_MONTH_OFFSET, 0) - 1
                prefs.edit().putInt(PREF_MONTH_OFFSET, offset).apply()
                for (id in allWidgetIds) updateWidget(context, appWidgetManager, id)
            }
            ACTION_NEXT_MONTH -> {
                val offset = prefs.getInt(PREF_MONTH_OFFSET, 0) + 1
                prefs.edit().putInt(PREF_MONTH_OFFSET, offset).apply()
                for (id in allWidgetIds) updateWidget(context, appWidgetManager, id)
            }
            ACTION_RESET_MONTH -> {
                prefs.edit().putInt(PREF_MONTH_OFFSET, 0).apply()
                for (id in allWidgetIds) updateWidget(context, appWidgetManager, id)
            }
            ACTION_TOGGLE_TRANSPARENCY -> {
                val currentTransparent = prefs.getBoolean(PREF_TRANSPARENT_MODE, false)
                prefs.edit().putBoolean(PREF_TRANSPARENT_MODE, !currentTransparent).apply()
                for (id in allWidgetIds) updateWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        const val ACTION_PREV_MONTH = "com.example.salarycalculator.widget.PREV_MONTH"
        const val ACTION_NEXT_MONTH = "com.example.salarycalculator.widget.NEXT_MONTH"
        const val ACTION_RESET_MONTH = "com.example.salarycalculator.widget.RESET_MONTH"
        const val ACTION_TOGGLE_TRANSPARENCY = "com.example.salarycalculator.widget.TOGGLE_TRANSPARENCY"

        private const val PREFS_NAME = "salary_calc_widget_prefs"
        private const val PREF_MONTH_OFFSET = "month_offset"
        private const val PREF_TRANSPARENT_MODE = "transparent_mode"

        fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = getPrefs(context)
            val monthOffset = prefs.getInt(PREF_MONTH_OFFSET, 0)
            val isTransparent = prefs.getBoolean(PREF_TRANSPARENT_MODE, false)

            // Calculate target month and year
            val cal = Calendar.getInstance()
            if (monthOffset != 0) {
                cal.add(Calendar.MONTH, monthOffset)
            }
            val targetYear = cal.get(Calendar.YEAR)
            val targetMonth = cal.get(Calendar.MONTH) + 1 // 1..12

            // Load shifts from DataStore synchronous cache or direct parse
            val dataStoreFile = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val scheduleJson = dataStoreFile.getString("annual_shift_schedule_json", "") ?: ""
            val multiYearShifts = SalaryRepository.parseShiftScheduleJson(scheduleJson)
            val monthShifts = multiYearShifts["$targetYear-$targetMonth"] ?: emptyMap()

            val hourlyRate = dataStoreFile.getFloat("default_hourly_rate", 15.0f).toDouble()
            val standardHours = dataStoreFile.getFloat("default_hours_per_day", 12.0f).toDouble()

            // Calculate shift metrics
            val daysWorked = monthShifts.values.count { it != 0.0 }
            val annualLeaveDays = monthShifts.values.count { it == SalaryRepository.ANNUAL_LEAVE_HOURS }
            val sickDays = monthShifts.values.count { it == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS }
            val totalHours = monthShifts.values.sumOf {
                when (it) {
                    SalaryRepository.ANNUAL_LEAVE_HOURS -> 7.5
                    SalaryRepository.STATUTORY_SICK_LEAVE_HOURS -> 0.0
                    else -> kotlin.math.abs(it)
                }
            }
            val estGross = totalHours * hourlyRate

            val monthNames = DateFormatSymbols().months
            val monthTitle = "${monthNames.getOrElse(targetMonth - 1) { "Month $targetMonth" }} $targetYear"
            val summaryText = when {
                daysWorked > 0 -> "$daysWorked Shifts (${"%.0f".format(totalHours)}h) • ~£${"%,.2f".format(estGross)}"
                else -> "No shifts logged for $monthTitle"
            }

            val views = RemoteViews(context.packageName, R.layout.widget_shift_heatmap)
            views.setTextViewText(R.id.widget_month_title, monthTitle)
            views.setTextViewText(R.id.widget_summary_text, summaryText)

            // Transparent vs Glass Background
            val bgRes = if (isTransparent) R.drawable.widget_transparent_bg else R.drawable.widget_glass_bg
            views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

            // Generate Heatmap Canvas Bitmap
            val bitmap = renderHeatmapBitmap(
                year = targetYear,
                month = targetMonth,
                shifts = monthShifts,
                standardHours = standardHours
            )
            views.setImageViewBitmap(R.id.widget_heatmap_image, bitmap)

            // Button PendingIntents
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                createActionPendingIntent(context, ACTION_PREV_MONTH, 101)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                createActionPendingIntent(context, ACTION_NEXT_MONTH, 102)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_today,
                createActionPendingIntent(context, ACTION_RESET_MONTH, 103)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_transparency,
                createActionPendingIntent(context, ACTION_TOGGLE_TRANSPARENCY, 104)
            )

            // Open app on clicking header or body
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                200,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title_container, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_heatmap_image, launchPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, ShiftHeatmapWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * Renders a high-resolution, crisp 7-column calendar matrix bitmap.
         */
        fun renderHeatmapBitmap(
            year: Int,
            month: Int,
            shifts: Map<Int, Double>,
            standardHours: Double
        ): Bitmap {
            val width = 700
            val height = 480
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            cal.set(Calendar.DAY_OF_MONTH, 1)
            val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
            val startDayOffset = (dow + 5) % 7 // 0=Mon..6=Sun

            // Calculate PayDay and Cutoff Day for this month
            val payPeriod = PayScheduleEngine.calculatePayPeriod(year, month, PayScheduleConfig())

            val cellWidth = width / 7f
            val headerHeight = 36f
            val gridTop = headerHeight + 6f
            val numRows = (startDayOffset + daysInMonth + 6) / 7
            val cellHeight = (height - gridTop - 8f) / numRows

            // Day Headers Paint
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            for (i in 0 until 7) {
                val cx = i * cellWidth + cellWidth / 2f
                canvas.drawText(dayLabels[i], cx, headerHeight - 10f, headerPaint)
            }

            // Paints for Cells
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val dayNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 13.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            for (row in 0 until numRows) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startDayOffset + 1

                    val left = col * cellWidth + 3f
                    val top = gridTop + row * cellHeight + 3f
                    val right = (col + 1) * cellWidth - 3f
                    val bottom = gridTop + (row + 1) * cellHeight - 3f

                    if (dayNum in 1..daysInMonth) {
                        val hours = shifts[dayNum] ?: 0.0
                        val isAnnualLeave = (hours == SalaryRepository.ANNUAL_LEAVE_HOURS)
                        val isSickLeave = (hours == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS)
                        val isOt = !isAnnualLeave && !isSickLeave && (hours < 0.0 || hours > 8.0)

                        val isCutoff = (dayNum == payPeriod.cutoffDay)
                        val isPayday = (dayNum == payPeriod.payDay)

                        val (bgColor, textColor, badgeLabel) = when {
                            isAnnualLeave -> Triple(Color.parseColor("#1E88E5"), Color.WHITE, "A/L")
                            isSickLeave -> Triple(Color.parseColor("#A855F7"), Color.WHITE, "SSP")
                            isOt -> Triple(Color.parseColor("#F59E0B"), Color.BLACK, "${kotlin.math.abs(hours).toInt()}h OT")
                            hours >= standardHours -> Triple(Color.parseColor("#10B981"), Color.WHITE, "${hours.toInt()}h")
                            hours > 0.0 -> Triple(Color.parseColor("#0D9488"), Color.WHITE, "${hours.toInt()}h")
                            else -> Triple(Color.parseColor("#1E293B"), Color.parseColor("#CBD5E1"), "")
                        }

                        // Draw Rounded Box
                        boxPaint.color = bgColor
                        val rect = RectF(left, top, right, bottom)
                        canvas.drawRoundRect(rect, 10f, 10f, boxPaint)

                        // Special Borders for Payday / Cutoff
                        if (isPayday) {
                            borderPaint.color = Color.parseColor("#10B981") // Emerald
                            canvas.drawRoundRect(rect, 10f, 10f, borderPaint)
                        } else if (isCutoff) {
                            borderPaint.color = Color.parseColor("#F59E0B") // Amber
                            canvas.drawRoundRect(rect, 10f, 10f, borderPaint)
                        }

                        // Draw Text
                        val cx = rect.centerX()
                        dayNumPaint.color = textColor
                        labelPaint.color = textColor

                        if (badgeLabel.isNotBlank()) {
                            canvas.drawText(dayNum.toString(), cx, top + (cellHeight * 0.45f), dayNumPaint)
                            canvas.drawText(badgeLabel, cx, top + (cellHeight * 0.85f), labelPaint)
                        } else {
                            canvas.drawText(dayNum.toString(), cx, rect.centerY() + 6f, dayNumPaint)
                        }
                    } else {
                        // Empty off-grid cell
                        boxPaint.color = Color.parseColor("#0F172A")
                        val rect = RectF(left, top, right, bottom)
                        canvas.drawRoundRect(rect, 10f, 10f, boxPaint)
                    }
                }
            }

            return bitmap
        }
    }
}
