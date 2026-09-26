package com.example.salarycalculator.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.domain.WearShiftNotificationManager
import com.example.salarycalculator.widget.ShiftHeatmapWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class WearShiftActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_LOG_EXTRA_OT = "com.example.salarycalculator.ACTION_LOG_EXTRA_OT"
        const val ACTION_MARK_ATTENDED = "com.example.salarycalculator.ACTION_MARK_ATTENDED"
        const val ACTION_SNOOZE_NOTIFICATION = "com.example.salarycalculator.ACTION_SNOOZE_NOTIFICATION"
        const val ACTION_TEST_NOTIFICATION = "com.example.salarycalculator.ACTION_TEST_NOTIFICATION"

        const val EXTRA_DATE_KEY = "extra_date_key"
        const val EXTRA_EXTRA_HOURS = "extra_extra_hours"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val repository = SalaryRepository(context)

        when (action) {
            ACTION_LOG_EXTRA_OT -> {
                val extraHours = intent.getDoubleExtra(EXTRA_EXTRA_HOURS, 2.0)
                handleLogExtraOt(context, repository, extraHours)
            }
            ACTION_MARK_ATTENDED -> {
                handleMarkAttended(context, repository)
            }
            ACTION_SNOOZE_NOTIFICATION -> {
                handleSnooze(context)
            }
            ACTION_TEST_NOTIFICATION -> {
                WearShiftNotificationManager.sendTestNotification(context)
            }
        }
    }

    private fun handleLogExtraOt(context: Context, repository: SalaryRepository, extraHours: Double) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val monthKey = "$currentYear-$currentMonth"

                val fullScheduleJson = repository.getAnnualShiftSchedule().first()
                val parsedSchedule = SalaryRepository.parseShiftScheduleJson(fullScheduleJson).toMutableMap()
                val monthMap = (parsedSchedule[monthKey] ?: emptyMap()).toMutableMap()
                val currentHours = monthMap[currentDay] ?: 0.0

                // If currently standard (e.g. 12h or 0h), add extraHours (e.g. 12 + 2 = 14h OT)
                val newHours = if (currentHours > 0.0 && currentHours != SalaryRepository.ANNUAL_LEAVE_HOURS && currentHours != SalaryRepository.STATUTORY_SICK_LEAVE_HOURS) {
                    currentHours + extraHours
                } else {
                    12.0 + extraHours
                }

                monthMap[currentDay] = newHours
                parsedSchedule[monthKey] = monthMap
                val updatedJson = SalaryRepository.encodeShiftSchedule(parsedSchedule)
                repository.setAnnualShiftSchedule(updatedJson)

                ShiftHeatmapWidgetManager.updateAllWidgets(context)
                WearShiftNotificationManager.cancelNotification(context)

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "⌚ Logged +${extraHours.toInt()}h Overtime (${newHours}h Total Today)", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleMarkAttended(context: Context, repository: SalaryRepository) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val monthKey = "$currentYear-$currentMonth"

                val fullScheduleJson = repository.getAnnualShiftSchedule().first()
                val parsedSchedule = SalaryRepository.parseShiftScheduleJson(fullScheduleJson).toMutableMap()
                val monthMap = (parsedSchedule[monthKey] ?: emptyMap()).toMutableMap()
                val defaultStd = repository.getDefaultHoursPerDay().first()

                monthMap[currentDay] = if (defaultStd > 0.0) defaultStd else 12.0
                parsedSchedule[monthKey] = monthMap
                val updatedJson = SalaryRepository.encodeShiftSchedule(parsedSchedule)
                repository.setAnnualShiftSchedule(updatedJson)

                ShiftHeatmapWidgetManager.updateAllWidgets(context)
                WearShiftNotificationManager.cancelNotification(context)

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "⌚ Marked Shift Attended (${monthMap[currentDay]}h)", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnooze(context: Context) {
        WearShiftNotificationManager.cancelNotification(context)
        Toast.makeText(context, "⌚ Shift Reminder Snoozed for 15 minutes", Toast.LENGTH_SHORT).show()
    }
}
