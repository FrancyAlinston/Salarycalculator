package com.example.salarycalculator.domain

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import java.util.Calendar
import java.util.TimeZone

data class DeviceCalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val ownerAccount: String,
    val color: Int?,
    val isPrimary: Boolean = false
) {
    val displaySummary: String
        get() = when {
            accountType.contains("google", ignoreCase = true) -> "$displayName ($accountName - Google)"
            accountName.isNotBlank() -> "$displayName ($accountName)"
            else -> displayName
        }
}

data class CalendarSyncConfig(
    val shiftStartHour: Int = 7,
    val shiftStartMinute: Int = 0,
    val standardShiftHours: Double = 12.0,
    val syncDutyShifts: Boolean = true,
    val syncOvertimeShifts: Boolean = true,
    val syncAnnualLeave: Boolean = true,
    val syncSickLeave: Boolean = true,
    val reminderMinutes: Int = 30,
    val clearExistingSalaryCalcEvents: Boolean = true
)

data class CalendarSyncResult(
    val success: Boolean,
    val eventsInserted: Int,
    val eventsCleared: Int = 0,
    val message: String
)

object GoogleCalendarSyncEngine {

    const val SALARY_CALC_TAG = "[SalaryCalculator]"

    /**
     * Queries all available device calendar categories and Google accounts.
     */
    fun getAvailableCalendars(context: Context): List<DeviceCalendarAccount> {
        val list = mutableListOf<DeviceCalendarAccount>()
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.ACCOUNT_NAME} ASC"
            )

            cursor?.let {
                val idIdx = it.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameIdx = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val ownerIdx = it.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
                val colorIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val isPrimIdx = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else -1L
                    val name = if (nameIdx != -1) it.getString(nameIdx) ?: "Calendar" else "Calendar"
                    val accName = if (accNameIdx != -1) it.getString(accNameIdx) ?: "" else ""
                    val accType = if (accTypeIdx != -1) it.getString(accTypeIdx) ?: "" else ""
                    val owner = if (ownerIdx != -1) it.getString(ownerIdx) ?: "" else ""
                    val color = if (colorIdx != -1 && !it.isNull(colorIdx)) it.getInt(colorIdx) else null
                    val isPrim = if (isPrimIdx != -1) it.getInt(isPrimIdx) == 1 else false

                    if (id != -1L) {
                        list.add(
                            DeviceCalendarAccount(
                                id = id,
                                displayName = name,
                                accountName = accName,
                                accountType = accType,
                                ownerAccount = owner,
                                color = color,
                                isPrimary = isPrim
                            )
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            // Calendar read permissions not granted
        } catch (_: Exception) {
            // General query fallback
        } finally {
            cursor?.close()
        }

        return list
    }

    /**
     * Clears previously synchronized Salary Calculator events for a specific calendar and month.
     */
    fun clearMonthSalaryCalcEvents(context: Context, calendarId: Long, year: Int, month: Int): Int {
        var cleared = 0
        try {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMillis = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val endMillis = cal.timeInMillis

            val where = "(${CalendarContract.Events.CALENDAR_ID} = ?) AND (${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?) AND (${CalendarContract.Events.DESCRIPTION} LIKE ?)"
            val args = arrayOf(calendarId.toString(), startMillis.toString(), endMillis.toString(), "%$SALARY_CALC_TAG%")

            cleared = context.contentResolver.delete(CalendarContract.Events.CONTENT_URI, where, args)
        } catch (_: Exception) {
            cleared = 0
        }
        return cleared
    }

    /**
     * Synchronizes shifts of the selected month directly into the specified Google Calendar category.
     */
    fun syncMonthToCalendar(
        context: Context,
        calendarId: Long,
        year: Int,
        month: Int,
        shifts: Map<Int, Double>,
        config: CalendarSyncConfig,
        jobTitle: String = "Duty Shift"
    ): CalendarSyncResult {
        var clearedCount = 0
        if (config.clearExistingSalaryCalcEvents) {
            clearedCount = clearMonthSalaryCalcEvents(context, calendarId, year, month)
        }

        var insertedCount = 0
        val cal = Calendar.getInstance()
        val daysInMonth = cal.apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        val timeZone = TimeZone.getDefault().id

        for (day in 1..daysInMonth) {
            val hours = shifts[day] ?: 0.0
            if (hours == 0.0) continue

            val isAnnualLeave = (hours == SalaryRepository.ANNUAL_LEAVE_HOURS)
            val isStatutorySickLeave = (hours == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS)
            val isOvertime = (!isAnnualLeave && !isStatutorySickLeave && (hours < 0.0 || hours > 8.0))
            val isStandardDuty = (!isAnnualLeave && !isStatutorySickLeave && !isOvertime && hours > 0.0)

            if (isAnnualLeave && !config.syncAnnualLeave) continue
            if (isStatutorySickLeave && !config.syncSickLeave) continue
            if (isOvertime && !config.syncOvertimeShifts) continue
            if (isStandardDuty && !config.syncDutyShifts) continue

            val effectiveHours = when {
                isAnnualLeave -> 7.5
                isStatutorySickLeave -> 8.0
                else -> kotlin.math.abs(hours)
            }

            cal.set(year, month - 1, day, config.shiftStartHour, config.shiftStartMinute, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMillis = cal.timeInMillis
            val durationMs = (effectiveHours * 3600 * 1000).toLong()
            val endMillis = startMillis + durationMs

            val title = when {
                isAnnualLeave -> "Annual Leave (A/L)"
                isStatutorySickLeave -> "Statutory Sick Pay (SSP)"
                isOvertime -> "$jobTitle (Overtime: ${"%.1f".format(effectiveHours)}h)"
                else -> "$jobTitle (${"%.1f".format(effectiveHours)}h Shift)"
            }

            val desc = when {
                isAnnualLeave -> "Annual holiday leave logged from Salary Calculator. $SALARY_CALC_TAG"
                isStatutorySickLeave -> "Statutory Sick Pay (SSP) day logged from Salary Calculator. $SALARY_CALC_TAG"
                isOvertime -> "Overtime duty shift (${"%.1f".format(effectiveHours)}h) logged from Salary Calculator. $SALARY_CALC_TAG"
                else -> "Standard duty shift (${"%.1f".format(effectiveHours)}h) logged from Salary Calculator. $SALARY_CALC_TAG"
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, desc)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                if (config.reminderMinutes > 0) {
                    put(CalendarContract.Events.HAS_ALARM, 1)
                }
            }

            try {
                val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (eventUri != null) {
                    insertedCount++
                    val eventId = ContentUris.parseId(eventUri)

                    if (config.reminderMinutes > 0) {
                        val remValues = ContentValues().apply {
                            put(CalendarContract.Reminders.EVENT_ID, eventId)
                            put(CalendarContract.Reminders.MINUTES, config.reminderMinutes)
                            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                        }
                        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, remValues)
                    }
                }
            } catch (_: Exception) {
                // Ignore individual insert failure and continue
            }
        }

        return CalendarSyncResult(
            success = insertedCount > 0,
            eventsInserted = insertedCount,
            eventsCleared = clearedCount,
            message = if (insertedCount > 0) {
                "Successfully added $insertedCount shift events to Google Calendar!"
            } else {
                "No shift events were inserted. Please check calendar permissions and selected shift filters."
            }
        )
    }
}
