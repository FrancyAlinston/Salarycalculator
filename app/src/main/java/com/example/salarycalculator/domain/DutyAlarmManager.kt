package com.example.salarycalculator.domain

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID

@Serializable
data class DutyAlarmTime(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean = true
) {
    val formattedTime: String
        get() = "%02d:%02d".format(hour, minute)

    val displaySummary: String
        get() = "$formattedTime - $label"
}

data class DutyAlarmResult(
    val success: Boolean,
    val dutyDaysCount: Int,
    val activeAlarmTimesCount: Int,
    val totalAlarmsScheduled: Int,
    val dutyDaysList: List<Int>,
    val excludedAnnualLeaveDays: List<Int>,
    val message: String
)

object DutyAlarmManager {

    val DEFAULT_DUTY_ALARMS = listOf(
        DutyAlarmTime(
            id = "alarm-1",
            hour = 5,
            minute = 30,
            label = "Duty Shift - Wake Up & Prepare",
            isEnabled = true
        ),
        DutyAlarmTime(
            id = "alarm-2",
            hour = 6,
            minute = 0,
            label = "Duty Shift - Depart / Commute",
            isEnabled = true
        ),
        DutyAlarmTime(
            id = "alarm-3",
            hour = 6,
            minute = 30,
            label = "Duty Shift - Arrive & Handover",
            isEnabled = true
        )
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun parseAlarmTimesJson(jsonStr: String): List<DutyAlarmTime> {
        if (jsonStr.isBlank()) return DEFAULT_DUTY_ALARMS
        return try {
            json.decodeFromString<List<DutyAlarmTime>>(jsonStr)
        } catch (_: Exception) {
            DEFAULT_DUTY_ALARMS
        }
    }

    fun encodeAlarmTimesJson(alarmTimes: List<DutyAlarmTime>): String {
        return try {
            json.encodeToString(alarmTimes)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Identifies all working duty days in the month (standard and overtime shifts).
     * Strictly EXCLUDES Annual Leave days (-999.0) and Off days (0.0).
     */
    fun getDutyDaysForMonth(year: Int, month: Int, shifts: Map<Int, Double>): List<Int> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dutyDays = mutableListOf<Int>()
        for (day in 1..daysInMonth) {
            val hours = shifts[day] ?: 0.0
            val isAnnualLeave = (hours == SalaryRepository.ANNUAL_LEAVE_HOURS)
            val isSickLeave = (hours == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS)
            val isOff = (hours == 0.0)

            if (!isAnnualLeave && !isSickLeave && !isOff) {
                dutyDays.add(day)
            }
        }
        return dutyDays.sorted()
    }

    /**
     * Identifies all Annual Leave days in the month that are excluded from duty alarms.
     */
    fun getAnnualLeaveDaysForMonth(year: Int, month: Int, shifts: Map<Int, Double>): List<Int> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val alDays = mutableListOf<Int>()
        for (day in 1..daysInMonth) {
            val hours = shifts[day] ?: 0.0
            if (hours == SalaryRepository.ANNUAL_LEAVE_HOURS) {
                alDays.add(day)
            }
        }
        return alDays.sorted()
    }

    /**
     * Auto-sets phone alarms for the duty days in the selected month across all enabled alarm times.
     */
    fun setAlarmsForMonthDutyDays(
        context: Context,
        year: Int,
        month: Int,
        shifts: Map<Int, Double>,
        alarmTimes: List<DutyAlarmTime>
    ): DutyAlarmResult {
        val dutyDays = getDutyDaysForMonth(year, month, shifts)
        val annualLeaveDays = getAnnualLeaveDaysForMonth(year, month, shifts)
        val enabledAlarms = alarmTimes.filter { it.isEnabled }

        if (dutyDays.isEmpty()) {
            return DutyAlarmResult(
                success = false,
                dutyDaysCount = 0,
                activeAlarmTimesCount = enabledAlarms.size,
                totalAlarmsScheduled = 0,
                dutyDaysList = emptyList(),
                excludedAnnualLeaveDays = annualLeaveDays,
                message = "No duty shift days found in this month to set alarms for."
            )
        }

        if (enabledAlarms.isEmpty()) {
            return DutyAlarmResult(
                success = false,
                dutyDaysCount = dutyDays.size,
                activeAlarmTimesCount = 0,
                totalAlarmsScheduled = 0,
                dutyDaysList = dutyDays,
                excludedAnnualLeaveDays = annualLeaveDays,
                message = "No alarm times are enabled. Please add or enable at least one alarm time."
            )
        }

        // Calculate the days of week for all duty days in the month
        val cal = Calendar.getInstance()
        val daysOfWeekSet = mutableSetOf<Int>()
        for (day in dutyDays) {
            cal.set(year, month - 1, day)
            val dow = cal.get(Calendar.DAY_OF_WEEK) // Calendar.SUNDAY = 1 .. Calendar.SATURDAY = 7
            daysOfWeekSet.add(dow)
        }

        var alarmsDispatched = 0
        val daysOfWeekList = ArrayList(daysOfWeekSet)

        for (alarm in enabledAlarms) {
            try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, alarm.hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, alarm.minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, alarm.label)
                    if (daysOfWeekList.isNotEmpty()) {
                        putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS, daysOfWeekList)
                    }
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                alarmsDispatched++
            } catch (_: Exception) {
                // If skip_ui fails, try standard interactive alarm intent
                try {
                    val fallbackIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, alarm.hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, alarm.minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, alarm.label)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    alarmsDispatched++
                } catch (_: Exception) {}
            }
        }

        val totalAlarmsCalc = dutyDays.size * enabledAlarms.size

        return DutyAlarmResult(
            success = alarmsDispatched > 0,
            dutyDaysCount = dutyDays.size,
            activeAlarmTimesCount = enabledAlarms.size,
            totalAlarmsScheduled = totalAlarmsCalc,
            dutyDaysList = dutyDays,
            excludedAnnualLeaveDays = annualLeaveDays,
            message = if (alarmsDispatched > 0) {
                "Successfully configured $alarmsDispatched alarm sequence(s) for ${dutyDays.size} duty days (${totalAlarmsCalc} total duty shift alarms)!"
            } else {
                "Could not access phone Clock app. Please ensure a system Clock app is available."
            }
        )
    }
}
