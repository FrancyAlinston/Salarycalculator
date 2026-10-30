package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class IcsCalendarExporterTest {

    @Test
    fun testNightDutyStandardShifts_strictlyLabeledNightDutyWithoutOvertimeOrHours() {
        // 15 standard 12.0h shifts in a month (typical care worker schedule)
        val dayShifts = mapOf(
            1 to 12.0,
            2 to 12.0,
            3 to 12.0,
            6 to 12.0,
            7 to 12.0,
            8 to 12.0
        )

        val ics = IcsCalendarExporter.generateIcsContent(
            year = 2026,
            month = 10,
            dayShifts = dayShifts,
            jobTitle = "Night Duty",
            standardShiftHours = 12.0
        )

        assertTrue(ics.startsWith("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("BEGIN:VEVENT"))
        // Exactly "Night Duty" as requested by user ("instead of just saying night duty")
        assertTrue("Event summary must be 'Night Duty'", ics.contains("SUMMARY:Night Duty\n") || ics.contains("SUMMARY:Night Duty\r\n"))
        // Must NOT falsely tag 12.0h standard shifts as overtime
        assertFalse("Standard 12h shifts must not be labeled as Overtime", ics.contains("Overtime"))
        assertFalse("Standard Night Duty shifts must not have unwanted (12h Shift) suffix", ics.contains("12h Shift"))
    }

    @Test
    fun testNightDutyOvertimeShift_labeledNightDutyOvertime() {
        // Mix of standard 12.0h shift and -12.0h overtime shift from Heatmap
        val dayShifts = mapOf(
            1 to 12.0,  // Standard
            5 to -12.0  // Heatmap Overtime Shift
        )

        val ics = IcsCalendarExporter.generateIcsContent(
            year = 2026,
            month = 10,
            dayShifts = dayShifts,
            jobTitle = "Night Duty",
            standardShiftHours = 12.0
        )

        assertTrue(ics.contains("SUMMARY:Night Duty\n") || ics.contains("SUMMARY:Night Duty\r\n"))
        assertTrue(ics.contains("SUMMARY:Night Duty (Overtime)\n") || ics.contains("SUMMARY:Night Duty (Overtime)\r\n"))
    }

    @Test
    fun testNightDutyTiming_defaultsTo2000Evening() {
        val startHour = IcsCalendarExporter.getShiftStartHour("Night Duty", null)
        assertEquals(20, startHour) // 20:00 (8:00 PM)

        val dayHour = IcsCalendarExporter.getShiftStartHour("Day Shift", null)
        assertEquals(8, dayHour) // 08:00 (8:00 AM)

        val customHour = IcsCalendarExporter.getShiftStartHour("Night Duty", 19)
        assertEquals(19, customHour)
    }

    @Test
    fun testAnnualIcsContent_exportsWithoutOvertimeDriftFor12hShifts() {
        val annualShifts = mapOf(
            10 to mapOf(1 to 12.0, 2 to 12.0, 3 to 12.0),
            11 to mapOf(4 to 12.0, 5 to 12.0)
        )

        val ics = IcsCalendarExporter.generateAnnualIcsContent(
            year = 2026,
            monthlyShifts = annualShifts,
            jobTitle = "Night Duty",
            standardShiftHours = 12.0
        )

        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("SUMMARY:Night Duty\n") || ics.contains("SUMMARY:Night Duty\r\n"))
        assertFalse(ics.contains("Overtime"))
    }

    @Test
    fun testFormatShiftSummary_genericEmployment_maintainsCompatibility() {
        // 8h shift with 8h standard
        val summary8 = IcsCalendarExporter.formatShiftSummary("Primary Employment", 8.0, 8.0)
        assertEquals("Primary Employment (8h Shift)", summary8)

        // 10h shift with 8h standard -> Overtime
        val summary10 = IcsCalendarExporter.formatShiftSummary("Primary Employment", 10.0, 8.0)
        assertEquals("Primary Employment (Overtime: 10h)", summary10)

        // Night Duty 12h with 12h standard -> Just "Night Duty"
        val summaryNight = IcsCalendarExporter.formatShiftSummary("Night Duty", 12.0, 12.0)
        assertEquals("Night Duty", summaryNight)

        // Night Duty OT -> "Night Duty (Overtime)"
        val summaryNightOt = IcsCalendarExporter.formatShiftSummary("Night Duty", -12.0, 12.0)
        assertEquals("Night Duty (Overtime)", summaryNightOt)
    }
}
