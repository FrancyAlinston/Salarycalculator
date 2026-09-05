package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class PayScheduleEngineTest {

    @Test
    fun testLastFridayAndSundayCutoff_2025FullYear() {
        val config = PayScheduleConfig(type = PayScheduleType.LAST_FRIDAY_OF_MONTH)

        // Jan 2025: Last Friday = Jan 31, Cutoff = Jan 26
        val jan = PayScheduleEngine.calculatePayPeriod(2025, 1, config)
        assertEquals(31, jan.payDay)
        assertEquals(26, jan.cutoffDay)
        assertEquals(1, jan.cutoffMonth)

        // Feb 2025: Last Friday = Feb 28, Cutoff = Feb 23
        val feb = PayScheduleEngine.calculatePayPeriod(2025, 2, config)
        assertEquals(28, feb.payDay)
        assertEquals(23, feb.cutoffDay)
        assertEquals(2, feb.cutoffMonth)

        // Mar 2025: Last Friday = Mar 28, Cutoff = Mar 23
        val mar = PayScheduleEngine.calculatePayPeriod(2025, 3, config)
        assertEquals(28, mar.payDay)
        assertEquals(23, mar.cutoffDay)
        assertEquals(3, mar.cutoffMonth)

        // Apr 2025: Last Friday = Apr 25, Cutoff = Apr 20
        val apr = PayScheduleEngine.calculatePayPeriod(2025, 4, config)
        assertEquals(25, apr.payDay)
        assertEquals(20, apr.cutoffDay)

        // May 2025: Last Friday = May 30, Cutoff = May 25
        val may = PayScheduleEngine.calculatePayPeriod(2025, 5, config)
        assertEquals(30, may.payDay)
        assertEquals(25, may.cutoffDay)

        // Jun 2025: Last Friday = Jun 27, Cutoff = Jun 22
        val jun = PayScheduleEngine.calculatePayPeriod(2025, 6, config)
        assertEquals(27, jun.payDay)
        assertEquals(22, jun.cutoffDay)

        // Jul 2025: Last Friday = Jul 25, Cutoff = Jul 20
        val jul = PayScheduleEngine.calculatePayPeriod(2025, 7, config)
        assertEquals(25, jul.payDay)
        assertEquals(20, jul.cutoffDay)

        // Aug 2025: Last Friday = Aug 29, Cutoff = Aug 24
        val aug = PayScheduleEngine.calculatePayPeriod(2025, 8, config)
        assertEquals(29, aug.payDay)
        assertEquals(24, aug.cutoffDay)

        // Sep 2025: Last Friday = Sep 26, Cutoff = Sep 21
        val sep = PayScheduleEngine.calculatePayPeriod(2025, 9, config)
        assertEquals(26, sep.payDay)
        assertEquals(21, sep.cutoffDay)

        // Oct 2025: Last Friday = Oct 31, Cutoff = Oct 26
        val oct = PayScheduleEngine.calculatePayPeriod(2025, 10, config)
        assertEquals(31, oct.payDay)
        assertEquals(26, oct.cutoffDay)

        // Nov 2025: Last Friday = Nov 28, Cutoff = Nov 23
        val nov = PayScheduleEngine.calculatePayPeriod(2025, 11, config)
        assertEquals(28, nov.payDay)
        assertEquals(23, nov.cutoffDay)

        // Dec 2025: Last Friday = Dec 26 (Boxing Day), with adjustForBankHolidays=false -> 26, with true -> 24
        val decRaw = PayScheduleEngine.calculatePayPeriod(2025, 12, config.copy(adjustForBankHolidays = false))
        assertEquals(26, decRaw.payDay)
        assertEquals(21, decRaw.cutoffDay)
        assertFalse(decRaw.isBankHolidayAdjusted)

        val decAdjusted = PayScheduleEngine.calculatePayPeriod(2025, 12, config.copy(adjustForBankHolidays = true))
        assertEquals(24, decAdjusted.payDay)
        assertEquals(21, decAdjusted.cutoffDay)
        assertTrue(decAdjusted.isBankHolidayAdjusted)
    }

    @Test
    fun testShiftPayrollSplit_CalculatesCutoffAndRolloverCorrectly() {
        val config = PayScheduleConfig(type = PayScheduleType.LAST_FRIDAY_OF_MONTH)
        // Oct 2025 cutoff is 26th
        val shifts = mapOf(
            1 to 8.0,
            2 to 8.0,
            25 to 10.0, // in cycle (8h std + 2h ot)
            26 to 8.0,  // on cutoff Sunday (in cycle)
            27 to 8.0,  // post cutoff (rollover to Nov)
            28 to 12.0  // post cutoff (rollover to Nov, 8h std + 4h ot)
        )

        val split = PayScheduleEngine.calculateShiftPayrollSplit(
            year = 2025,
            month = 10,
            dayShifts = shifts,
            config = config
        )

        assertEquals(26, split.cutoffDay)
        assertEquals(31, split.payDay)

        // In-cycle: days 1, 2, 25, 26 -> 4 days, 34 hours (32h std + 2h ot)
        assertEquals(4, split.inCycleDays)
        assertEquals(34.0, split.inCycleHours, 0.01)
        assertEquals(2.0, split.inCycleOtHours, 0.01)
        assertEquals(32.0, split.inCycleStandardHours, 0.01)

        // Rollover: days 27, 28 -> 2 days, 20 hours (4h ot)
        assertEquals(2, split.rolloverDays)
        assertEquals(20.0, split.rolloverHours, 0.01)
        assertEquals(4.0, split.rolloverOtHours, 0.01)

        // Total
        assertEquals(6, split.totalMonthDays)
        assertEquals(54.0, split.totalMonthHours, 0.01)
    }

    @Test
    fun testLastWorkingDayAndFixedDaySchedules() {
        // Last working day of May 2025 (May 31 is Sat -> Last working day = May 30 Fri)
        val lwd = PayScheduleEngine.calculatePayPeriod(
            year = 2025,
            month = 5,
            config = PayScheduleConfig(type = PayScheduleType.LAST_WORKING_DAY, cutoffLeadDays = 5)
        )
        assertEquals(30, lwd.payDay)
        assertEquals(25, lwd.cutoffDay)

        // Fixed 25th of month with 3 lead days
        val fixed = PayScheduleEngine.calculatePayPeriod(
            year = 2025,
            month = 7,
            config = PayScheduleConfig(type = PayScheduleType.FIXED_DAY_OF_MONTH, fixedDay = 25, cutoffLeadDays = 3)
        )
        assertEquals(25, fixed.payDay)
        assertEquals(22, fixed.cutoffDay)
    }

    @Test
    fun testIcsPayScheduleExporter_GeneratesValidVCalendar() {
        val config = PayScheduleConfig(type = PayScheduleType.LAST_FRIDAY_OF_MONTH)
        val ics = IcsCalendarExporter.generatePayScheduleIcsContent(
            year = 2025,
            config = config,
            estimatedNetPay = 2850.50
        )

        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("END:VCALENDAR"))
        assertTrue(ics.contains("Timesheet & Overtime Cutoff"))
        assertTrue(ics.contains("Payday (£2,850.50)"))
        assertTrue(ics.contains("BEGIN:VALARM"))
        assertTrue(ics.contains("TRIGGER:-PT4H"))
    }
}
