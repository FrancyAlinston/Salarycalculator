package com.example.salarycalculator.domain

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DutyShiftReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = SalaryRepository(applicationContext)
        val notificationsEnabled = repository.getWearOsNotificationsEnabled().first()
        val actionsEnabled = repository.getWearOsActionsEnabled().first()

        if (!notificationsEnabled) {
            return Result.success()
        }

        return try {
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)

            val fullScheduleJson = repository.getAnnualShiftSchedule().first()
            val fullSchedule = SalaryRepository.parseShiftScheduleJson(fullScheduleJson)
            val monthKey = "$currentYear-$currentMonth"
            val monthMap = fullSchedule[monthKey] ?: emptyMap()
            val todayHours = monthMap[currentDay] ?: 0.0

            if (todayHours > 0.0 && todayHours != SalaryRepository.ANNUAL_LEAVE_HOURS && todayHours != SalaryRepository.STATUTORY_SICK_LEAVE_HOURS) {
                val shiftStartTime = repository.getDutyShiftStartTime().first()
                val isOt = (todayHours > 12.0)
                val shiftType = if (isOt) "Overtime Shift (${todayHours}h)" else "Duty Shift (${todayHours}h)"
                val title = "⌚ Upcoming $shiftType"
                val body = "Shift starts at $shiftStartTime. Tap to mark attended or log overtime on your watch/phone."

                WearShiftNotificationManager.dispatchDutyShiftNotification(
                    context = applicationContext,
                    title = title,
                    body = body,
                    dateKey = "$monthKey-$currentDay",
                    hours = todayHours,
                    shiftType = shiftType,
                    actionsEnabled = actionsEnabled
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME = "duty_shift_wear_reminder_worker"

        fun schedulePeriodicCheck(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val request = PeriodicWorkRequestBuilder<DutyShiftReminderWorker>(4, TimeUnit.HOURS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
