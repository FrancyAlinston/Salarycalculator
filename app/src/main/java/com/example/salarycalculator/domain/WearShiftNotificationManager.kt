package com.example.salarycalculator.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.salarycalculator.MainActivity
import com.example.salarycalculator.R
import com.example.salarycalculator.receiver.WearShiftActionReceiver

object WearShiftNotificationManager {

    const val CHANNEL_DUTY_SHIFTS = "duty_shift_reminders"
    const val CHANNEL_DUTY_SHIFTS_NAME = "Duty Shift & Wear OS Alerts"
    const val CHANNEL_COMMUTE_PREALARM = "commute_prealarm_alerts"
    const val CHANNEL_COMMUTE_PREALARM_NAME = "Commute & Pre-Alarm Alerts"

    const val NOTIFICATION_ID_DUTY_SHIFT = 9001
    const val NOTIFICATION_ID_TEST = 9002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Duty Shift Channel
            val shiftChannel = NotificationChannel(
                CHANNEL_DUTY_SHIFTS,
                CHANNEL_DUTY_SHIFTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority shift reminders and Wear OS companion alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Commute & Pre-Alarm Channel
            val commuteChannel = NotificationChannel(
                CHANNEL_COMMUTE_PREALARM,
                CHANNEL_COMMUTE_PREALARM_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Commute departure buffers and traffic alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(shiftChannel)
            notificationManager.createNotificationChannel(commuteChannel)
        }
    }

    fun buildDutyShiftNotification(
        context: Context,
        title: String,
        body: String,
        dateKey: String,
        hours: Double,
        shiftType: String = "Duty Shift",
        actionsEnabled: Boolean = true
    ): NotificationCompat.Builder {
        createNotificationChannels(context)

        // Main Tap Content Intent (Opens Main App)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Overtime Action (+2h OT)
        val otIntent = Intent(context, WearShiftActionReceiver::class.java).apply {
            action = WearShiftActionReceiver.ACTION_LOG_EXTRA_OT
            putExtra(WearShiftActionReceiver.EXTRA_DATE_KEY, dateKey)
            putExtra(WearShiftActionReceiver.EXTRA_EXTRA_HOURS, 2.0)
        }
        val otPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            otIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Attended / Complete Action
        val attendedIntent = Intent(context, WearShiftActionReceiver::class.java).apply {
            action = WearShiftActionReceiver.ACTION_MARK_ATTENDED
            putExtra(WearShiftActionReceiver.EXTRA_DATE_KEY, dateKey)
        }
        val attendedPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            attendedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze (15 min) Action
        val snoozeIntent = Intent(context, WearShiftActionReceiver::class.java).apply {
            action = WearShiftActionReceiver.ACTION_SNOOZE_NOTIFICATION
            putExtra(WearShiftActionReceiver.EXTRA_DATE_KEY, dateKey)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Wearable Extender for Wear OS Smart Watches
        val wearableExtender = NotificationCompat.WearableExtender()
            .setHintShowBackgroundOnly(false)

        if (actionsEnabled) {
            val wearOtAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_input_add,
                "+2h OT",
                otPendingIntent
            ).build()

            val wearAttendedAction = NotificationCompat.Action.Builder(
                android.R.drawable.checkbox_on_background,
                "Attended",
                attendedPendingIntent
            ).build()

            val wearSnoozeAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze 15m",
                snoozePendingIntent
            ).build()

            wearableExtender.addAction(wearAttendedAction)
            wearableExtender.addAction(wearOtAction)
            wearableExtender.addAction(wearSnoozeAction)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DUTY_SHIFTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .extend(wearableExtender)

        if (actionsEnabled) {
            builder.addAction(android.R.drawable.checkbox_on_background, "Attended", attendedPendingIntent)
            builder.addAction(android.R.drawable.ic_input_add, "+2h Overtime", otPendingIntent)
            builder.addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePendingIntent)
        }

        return builder
    }

    fun dispatchDutyShiftNotification(
        context: Context,
        title: String,
        body: String,
        dateKey: String,
        hours: Double,
        shiftType: String = "Duty Shift",
        actionsEnabled: Boolean = true
    ): Boolean {
        return try {
            val builder = buildDutyShiftNotification(
                context = context,
                title = title,
                body = body,
                dateKey = dateKey,
                hours = hours,
                shiftType = shiftType,
                actionsEnabled = actionsEnabled
            )
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_DUTY_SHIFT, builder.build())
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun sendTestNotification(context: Context, actionsEnabled: Boolean = true): Boolean {
        val title = "⌚ Wear OS & Duty Shift Alert"
        val body = "Upcoming 12h Duty Shift starting soon. Tap actions to log overtime or mark attendance directly from your watch or phone."
        return try {
            val builder = buildDutyShiftNotification(
                context = context,
                title = title,
                body = body,
                dateKey = "today",
                hours = 12.0,
                shiftType = "Duty Shift",
                actionsEnabled = actionsEnabled
            )
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_TEST, builder.build())
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun cancelNotification(context: Context, id: Int = NOTIFICATION_ID_DUTY_SHIFT) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(id)
    }
}
