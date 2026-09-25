package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class DutyAlarmManagerTest {

    @Test
    fun testDutyDaysExtraction_ExcludesAnnualLeaveAndSickLeaveAndOffDays() {
        val shifts = mapOf(
            1 to 12.0,                                    // Day 1: Standard Duty -> YES
            2 to 12.0,                                    // Day 2: Standard Duty -> YES
            3 to SalaryRepository.ANNUAL_LEAVE_HOURS,     // Day 3: Annual Leave -> EXCLUDED
            4 to SalaryRepository.ANNUAL_LEAVE_HOURS,     // Day 4: Annual Leave -> EXCLUDED
            5 to 0.0,                                     // Day 5: Off Day -> EXCLUDED
            6 to -12.0,                                   // Day 6: Overtime Duty -> YES
            7 to SalaryRepository.STATUTORY_SICK_LEAVE_HOURS // Day 7: Sick Leave -> EXCLUDED
        )

        val dutyDays = DutyAlarmManager.getDutyDaysForMonth(2026, 9, shifts)
        val annualLeaveDays = DutyAlarmManager.getAnnualLeaveDaysForMonth(2026, 9, shifts)

        assertEquals(listOf(1, 2, 6), dutyDays)
        assertEquals(listOf(3, 4), annualLeaveDays)
    }

    @Test
    fun testAlarmTimesJsonEncodingAndDecoding() {
        val customAlarms = listOf(
            DutyAlarmTime(id = "a1", hour = 5, minute = 15, label = "Early Wake", isEnabled = true),
            DutyAlarmTime(id = "a2", hour = 6, minute = 0, label = "Commute", isEnabled = false),
            DutyAlarmTime(id = "a3", hour = 18, minute = 30, label = "Night Shift Alarm", isEnabled = true)
        )

        val json = DutyAlarmManager.encodeAlarmTimesJson(customAlarms)
        assertNotNull(json)
        assertTrue(json.contains("Early Wake"))
        assertTrue(json.contains("Night Shift Alarm"))

        val parsed = DutyAlarmManager.parseAlarmTimesJson(json)
        assertEquals(3, parsed.size)
        assertEquals("a1", parsed[0].id)
        assertEquals(5, parsed[0].hour)
        assertEquals(15, parsed[0].minute)
        assertEquals("05:15", parsed[0].formattedTime)
        assertTrue(parsed[0].isEnabled)

        assertEquals("a2", parsed[1].id)
        assertFalse(parsed[1].isEnabled)

        assertEquals("a3", parsed[2].id)
        assertEquals(18, parsed[2].hour)
        assertEquals(30, parsed[2].minute)
        assertEquals("18:30", parsed[2].formattedTime)
    }

    @Test
    fun testParseAlarmTimesJson_HandlesEmptyAndCorruptStringsGracefully() {
        val emptyParsed = DutyAlarmManager.parseAlarmTimesJson("")
        assertEquals(DutyAlarmManager.DEFAULT_DUTY_ALARMS.size, emptyParsed.size)

        val invalidParsed = DutyAlarmManager.parseAlarmTimesJson("{\"corrupted\": true}")
        assertEquals(DutyAlarmManager.DEFAULT_DUTY_ALARMS.size, invalidParsed.size)
    }

    @Test
    fun testDefaultDutyAlarms() {
        val defaults = DutyAlarmManager.DEFAULT_DUTY_ALARMS
        assertEquals(3, defaults.size)
        assertEquals(5, defaults[0].hour)
        assertEquals(30, defaults[0].minute)
        assertEquals("05:30", defaults[0].formattedTime)

        assertEquals(6, defaults[1].hour)
        assertEquals(0, defaults[1].minute)
        assertEquals("06:00", defaults[1].formattedTime)

        assertEquals(6, defaults[2].hour)
        assertEquals(30, defaults[2].minute)
        assertEquals("06:30", defaults[2].formattedTime)
    }
}
