package com.example.salarycalculator.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object IcsCalendarExporter {

    /**
     * Converts a map of day -> hours worked for a given month/year into standard RFC 5545 iCalendar (.ics) format.
     */
    fun generateIcsContent(
        year: Int,
        month: Int, // 1-12
        dayShifts: Map<Int, Double>,
        jobTitle: String = "Work Shift"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//SalaryCalculator//ShiftCalendar 7.0//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")

        val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val localDateOnly = SimpleDateFormat("yyyyMMdd", Locale.US)

        val calendar = Calendar.getInstance()

        dayShifts.forEach { (day, hours) ->
            if (hours > 0) {
                calendar.set(year, month - 1, day, 9, 0, 0)
                val startTime = calendar.time
                val durationMs = (hours * 3600 * 1000).toLong()
                val endTime = Date(startTime.time + durationMs)

                val uid = "shift-${year}${month}${day}-${UUID.randomUUID()}@salarycalculator.app"
                val isOvertime = hours > 8.0
                val summary = if (isOvertime) "$jobTitle (Overtime: ${hours.toInt()}h)" else "$jobTitle (${hours.toInt()}h Shift)"
                val description = "Logged working shift in Salary Calculator. Total hours: ${"%.1f".format(hours)}h"

                sb.appendLine("BEGIN:VEVENT")
                sb.appendLine("UID:$uid")
                sb.appendLine("DTSTAMP:${utcFormat.format(Date())}")
                sb.appendLine("DTSTART:${utcFormat.format(startTime)}")
                sb.appendLine("DTEND:${utcFormat.format(endTime)}")
                sb.appendLine("SUMMARY:$summary")
                sb.appendLine("DESCRIPTION:$description")
                sb.appendLine("STATUS:CONFIRMED")
                sb.appendLine("END:VEVENT")
            }
        }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    /**
     * Converts an entire 12-month annual shift schedule (month 1..12 -> (day -> hours)) into RFC 5545 iCalendar.
     */
    fun generateAnnualIcsContent(
        year: Int,
        monthlyShifts: Map<Int, Map<Int, Double>>,
        jobTitle: String = "Work Shift"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//SalaryCalculator//AnnualShiftCalendar 9.0//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")

        val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val calendar = Calendar.getInstance()

        monthlyShifts.forEach { (month, dayMap) ->
            dayMap.forEach { (day, hours) ->
                if (hours > 0) {
                    calendar.set(year, month - 1, day, 9, 0, 0)
                    val startTime = calendar.time
                    val durationMs = (hours * 3600 * 1000).toLong()
                    val endTime = Date(startTime.time + durationMs)

                    val uid = "annual-shift-${year}-${month}-${day}-${UUID.randomUUID()}@salarycalculator.app"
                    val isOvertime = hours > 8.0
                    val summary = if (isOvertime) "$jobTitle (Overtime: ${hours.toInt()}h)" else "$jobTitle (${hours.toInt()}h Shift)"
                    val description = "Logged working shift in Salary Calculator. Month: $month, Day: $day. Total: ${"%.1f".format(hours)}h"

                    sb.appendLine("BEGIN:VEVENT")
                    sb.appendLine("UID:$uid")
                    sb.appendLine("DTSTAMP:${utcFormat.format(Date())}")
                    sb.appendLine("DTSTART:${utcFormat.format(startTime)}")
                    sb.appendLine("DTEND:${utcFormat.format(endTime)}")
                    sb.appendLine("SUMMARY:$summary")
                    sb.appendLine("DESCRIPTION:$description")
                    sb.appendLine("STATUS:CONFIRMED")
                    sb.appendLine("END:VEVENT")
                }
            }
        }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    /**
     * Saves iCalendar (.ics) string to cache directory and launches Android Sharesheet.
     */
    fun shareIcsFile(
        context: Context,
        icsContent: String,
        filename: String = "Shifts_Schedule.ics"
    ) {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            out.write(icsContent.toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Shift Schedule (.ics)")
            putExtra(Intent.EXTRA_TEXT, "Import this calendar file into Google Calendar, Outlook, or Apple Calendar.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Export Shift Calendar"))
    }
}
