package com.example.salarycalculator.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

object IcsCalendarExporter {

    /**
     * Formats the event summary line.
     * When jobTitle is "Night Duty" (or ends with "Duty" or "Shift"),
     * standard scheduled shifts are cleanly labeled (e.g. "Night Duty") without unwanted
     * false overtime markers or "(12h Shift)" suffixes.
     * Actual overtime shifts (negative hours or hours > standardShiftHours) are marked with "(Overtime)".
     */
    fun formatShiftSummary(
        jobTitle: String,
        hours: Double,
        standardShiftHours: Double = 12.0
    ): String {
        val absHours = kotlin.math.abs(hours)
        val isOvertime = hours < 0.0 || (standardShiftHours > 0.0 && absHours > standardShiftHours)
        val cleanTitle = jobTitle.trim().ifBlank { "Night Duty" }

        return when {
            isOvertime -> {
                if (cleanTitle.equals("Night Duty", ignoreCase = true) ||
                    cleanTitle.equals("Night Shift", ignoreCase = true) ||
                    cleanTitle.equals("Day Shift", ignoreCase = true) ||
                    cleanTitle.equals("Day Duty", ignoreCase = true) ||
                    cleanTitle.endsWith("Duty", ignoreCase = true)
                ) {
                    "$cleanTitle (Overtime)"
                } else {
                    "$cleanTitle (Overtime: ${absHours.toInt()}h)"
                }
            }
            cleanTitle.equals("Night Duty", ignoreCase = true) ||
            cleanTitle.equals("Night Shift", ignoreCase = true) ||
            cleanTitle.equals("Day Shift", ignoreCase = true) ||
            cleanTitle.equals("Day Duty", ignoreCase = true) ||
            cleanTitle.endsWith("Duty", ignoreCase = true) ||
            (!cleanTitle.contains("shift", ignoreCase = true) && !cleanTitle.contains("employment", ignoreCase = true)) -> {
                // Just say the duty name, e.g. "Night Duty"
                cleanTitle
            }
            else -> {
                "$cleanTitle (${absHours.toInt()}h Shift)"
            }
        }
    }

    /**
     * Resolves the starting hour (0..23) for a shift.
     * Night duties default to 20:00 (8:00 PM), while day duties default to 08:00.
     */
    fun getShiftStartHour(jobTitle: String, customStartHour: Int? = null): Int {
        if (customStartHour != null && customStartHour in 0..23) {
            return customStartHour
        }
        val lower = jobTitle.lowercase(Locale.ENGLISH)
        return when {
            lower.contains("night") -> 20 // 8:00 PM
            lower.contains("day") -> 8    // 8:00 AM
            else -> 8
        }
    }

    /**
     * Converts a map of day -> hours worked for a given month/year into standard RFC 5545 iCalendar (.ics) format.
     */
    fun generateIcsContent(
        year: Int,
        month: Int, // 1-12
        dayShifts: Map<Int, Double>,
        jobTitle: String = "Night Duty",
        standardShiftHours: Double = if (jobTitle.contains("Night", ignoreCase = true) || jobTitle.contains("Care", ignoreCase = true)) 12.0 else 8.0,
        startHour: Int? = null,
        startMinute: Int = 0
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

        val resolvedStartHour = getShiftStartHour(jobTitle, startHour)
        val calendar = Calendar.getInstance()

        dayShifts.forEach { (day, hours) ->
            val absHours = kotlin.math.abs(hours)
            if (absHours > 0.0) {
                calendar.set(year, month - 1, day, resolvedStartHour, startMinute, 0)
                val startTime = calendar.time
                val durationMs = (absHours * 3600 * 1000).toLong()
                val endTime = Date(startTime.time + durationMs)

                val uid = "shift-${year}${month}${day}-${UUID.randomUUID()}@salarycalculator.app"
                val summary = formatShiftSummary(jobTitle, hours, standardShiftHours)
                val isOt = hours < 0.0 || (standardShiftHours > 0.0 && absHours > standardShiftHours)
                val description = "Logged working shift in Salary Calculator. Total hours: ${"%.1f".format(absHours)}h${if (isOt) " (Overtime Shift)" else ""}"

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
        jobTitle: String = "Night Duty",
        standardShiftHours: Double = if (jobTitle.contains("Night", ignoreCase = true) || jobTitle.contains("Care", ignoreCase = true)) 12.0 else 8.0,
        startHour: Int? = null,
        startMinute: Int = 0
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

        val resolvedStartHour = getShiftStartHour(jobTitle, startHour)
        val calendar = Calendar.getInstance()

        monthlyShifts.forEach { (month, dayMap) ->
            dayMap.forEach { (day, hours) ->
                val absHours = kotlin.math.abs(hours)
                if (absHours > 0.0) {
                    calendar.set(year, month - 1, day, resolvedStartHour, startMinute, 0)
                    val startTime = calendar.time
                    val durationMs = (absHours * 3600 * 1000).toLong()
                    val endTime = Date(startTime.time + durationMs)

                    val uid = "annual-shift-${year}-${month}-${day}-${UUID.randomUUID()}@salarycalculator.app"
                    val summary = formatShiftSummary(jobTitle, hours, standardShiftHours)
                    val isOt = hours < 0.0 || (standardShiftHours > 0.0 && absHours > standardShiftHours)
                    val description = "Logged working shift in Salary Calculator. Month: $month, Day: $day. Total: ${"%.1f".format(absHours)}h${if (isOt) " (Overtime Shift)" else ""}"

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
     * Converts annual Payday and Cutoff deadlines into an RFC 5545 iCalendar (.ics) with alarms/reminders.
     */
    fun generatePayScheduleIcsContent(
        year: Int,
        config: PayScheduleConfig = PayScheduleConfig(),
        estimatedNetPay: Double = 0.0
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//SalaryCalculator//PayScheduleCalendar 17.0//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")

        val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val annualSchedule = PayScheduleEngine.calculateAnnualSchedule(year, config)
        val monthNames = DateFormatSymbols().months

        annualSchedule.forEach { period ->
            val monthTitle = monthNames.getOrElse(period.month - 1) { "Month ${period.month}" }

            // 1. Cutoff Event (23:59 on Cutoff Day with 1-day advance alarm)
            val cutoffCal = Calendar.getInstance().apply {
                set(period.cutoffYear, period.cutoffMonth - 1, period.cutoffDay, 20, 0, 0)
            }
            val cutoffStart = cutoffCal.time
            val cutoffEnd = Date(cutoffStart.time + 4 * 3600 * 1000) // 20:00 to 24:00

            val cutoffUid = "cutoff-${period.year}-${period.month}-${UUID.randomUUID()}@salarycalculator.app"
            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:$cutoffUid")
            sb.appendLine("DTSTAMP:${utcFormat.format(Date())}")
            sb.appendLine("DTSTART:${utcFormat.format(cutoffStart)}")
            sb.appendLine("DTEND:${utcFormat.format(cutoffEnd)}")
            sb.appendLine("SUMMARY:⏰ $monthTitle Timesheet & Overtime Cutoff")
            sb.appendLine("DESCRIPTION:Payroll cutoff for $monthTitle. Submit all hours and overtime shifts by midnight tonight to be included in $monthTitle payslip.")
            sb.appendLine("STATUS:CONFIRMED")
            sb.appendLine("BEGIN:VALARM")
            sb.appendLine("TRIGGER:-PT4H")
            sb.appendLine("ACTION:DISPLAY")
            sb.appendLine("DESCRIPTION:Reminder: Timesheet Cutoff Deadline tonight!")
            sb.appendLine("END:VALARM")
            sb.appendLine("END:VEVENT")

            // 2. Payday Event (09:00 on Payday with morning notification)
            val payCal = Calendar.getInstance().apply {
                set(period.payYear, period.payMonth - 1, period.payDay, 9, 0, 0)
            }
            val payStart = payCal.time
            val payEnd = Date(payStart.time + 3600 * 1000)

            val payUid = "payday-${period.year}-${period.month}-${UUID.randomUUID()}@salarycalculator.app"
            val netPayStr = if (estimatedNetPay > 0.0) " (£${"%,.2f".format(estimatedNetPay)})" else ""
            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:$payUid")
            sb.appendLine("DTSTAMP:${utcFormat.format(Date())}")
            sb.appendLine("DTSTART:${utcFormat.format(payStart)}")
            sb.appendLine("DTEND:${utcFormat.format(payEnd)}")
            sb.appendLine("SUMMARY:💰 $monthTitle Payday$netPayStr")
            sb.appendLine("DESCRIPTION:Salary deposit for $monthTitle. Schedule rule: ${config.type.displayName}.")
            sb.appendLine("STATUS:CONFIRMED")
            sb.appendLine("BEGIN:VALARM")
            sb.appendLine("TRIGGER:-PT15M")
            sb.appendLine("ACTION:DISPLAY")
            sb.appendLine("DESCRIPTION:Payday! Salary deposited today.")
            sb.appendLine("END:VALARM")
            sb.appendLine("END:VEVENT")
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
