package com.example.salarycalculator.domain

import com.example.salarycalculator.receiver.WearShiftActionReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearShiftNotificationManagerTest {

    @Test
    fun testNotificationChannelsAndConstants() {
        assertEquals("duty_shift_reminders", WearShiftNotificationManager.CHANNEL_DUTY_SHIFTS)
        assertEquals("commute_prealarm_alerts", WearShiftNotificationManager.CHANNEL_COMMUTE_PREALARM)
        assertEquals(9001, WearShiftNotificationManager.NOTIFICATION_ID_DUTY_SHIFT)
        assertEquals(9002, WearShiftNotificationManager.NOTIFICATION_ID_TEST)
    }

    @Test
    fun testWearShiftActionReceiverActions() {
        assertEquals("com.example.salarycalculator.ACTION_LOG_EXTRA_OT", WearShiftActionReceiver.ACTION_LOG_EXTRA_OT)
        assertEquals("com.example.salarycalculator.ACTION_MARK_ATTENDED", WearShiftActionReceiver.ACTION_MARK_ATTENDED)
        assertEquals("com.example.salarycalculator.ACTION_SNOOZE_NOTIFICATION", WearShiftActionReceiver.ACTION_SNOOZE_NOTIFICATION)
        assertEquals("com.example.salarycalculator.ACTION_TEST_NOTIFICATION", WearShiftActionReceiver.ACTION_TEST_NOTIFICATION)
    }

    @Test
    fun testDefaultWearOsPreferences() {
        assertNotNull(WearShiftNotificationManager.CHANNEL_DUTY_SHIFTS_NAME)
        assertTrue(WearShiftNotificationManager.CHANNEL_DUTY_SHIFTS_NAME.contains("Wear OS"))
    }
}
