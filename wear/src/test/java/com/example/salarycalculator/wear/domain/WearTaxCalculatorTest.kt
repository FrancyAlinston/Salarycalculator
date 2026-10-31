package com.example.salarycalculator.wear.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.Calendar

class WearTaxCalculatorTest {

    @Test
    fun `resolveTodayDutySummary returns Night Duty when current day has standard night shift`() {
        val shifts = WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS
        // 2026-10-31 is a 12.0h Night Shift in the pre-seeded schedule
        val date = LocalDate.of(2026, 10, 31)
        val summary = TaxCalculator.resolveTodayDutySummary(date, shifts, 12.0)

        assertEquals("Night Duty", summary.title)
        assertEquals("Night Duty", summary.shortStatus)
        assertTrue(summary.hasDuty)
        assertEquals(WearDutyState.NIGHT_DUTY, summary.dutyState)
        assertEquals(7, summary.weekDays.size)
        assertTrue(summary.weekDays.any { it.isToday && it.dayOfMonth == 31 })
    }

    @Test
    fun `resolveTodayDutySummary returns Off Duty when current day has no shift`() {
        val shifts = WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS
        // 2026-10-27 is an Off Day in the pre-seeded schedule (next shift is 2026-10-29)
        val date = LocalDate.of(2026, 10, 27)
        val summary = TaxCalculator.resolveTodayDutySummary(date, shifts, 12.0)

        assertEquals("Off Duty", summary.title)
        assertEquals("Off Duty", summary.shortStatus)
        assertFalse(summary.hasDuty)
        assertEquals(WearDutyState.OFF_DUTY, summary.dutyState)
        assertEquals("Thu, 29 Oct", summary.nextDutyFormatted)
    }

    @Test
    fun `resolveTodayDutySummary returns Night Duty with Overtime state on overtime shift day`() {
        val shifts = WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS
        // 2026-10-21 is an Overtime shift (-12.0)
        val date = LocalDate.of(2026, 10, 21)
        val summary = TaxCalculator.resolveTodayDutySummary(date, shifts, 12.0)

        assertEquals("Night Duty", summary.title)
        assertEquals("Night Duty", summary.shortStatus)
        assertTrue(summary.hasDuty)
        assertEquals(WearDutyState.OVERTIME, summary.dutyState)
    }

    @Test
    fun `resolveTodayDutySummary returns Annual Leave on leave day`() {
        val shifts = WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS
        // 2026-10-25 is Annual Leave (-999.0)
        val date = LocalDate.of(2026, 10, 25)
        val summary = TaxCalculator.resolveTodayDutySummary(date, shifts, 12.0)

        assertEquals("Annual Leave", summary.title)
        assertFalse(summary.hasDuty)
        assertEquals(WearDutyState.ANNUAL_LEAVE, summary.dutyState)
    }

    @Test
    fun `November 2026 always has 30 days even when initialized on October 31st`() {
        val ym = YearMonth.of(2026, 11)
        assertEquals(30, ym.lengthOfMonth())

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 31) // Simulate running on Oct 31
        cal.set(2026, 11 - 1, 1) // Safe 3-arg setter for November
        assertEquals(30, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `computeShiftProgressFraction calculates elapsed progress during 20_00 to 08_00 night duty`() {
        assertEquals(0f, WearTelemetryManager.computeShiftProgressFraction(false, LocalTime.of(22, 0)), 0.001f)
        // At 02:00 AM (halfway through 20:00 -> 08:00 shift), progress is 0.5
        assertEquals(0.5f, WearTelemetryManager.computeShiftProgressFraction(true, LocalTime.of(2, 0)), 0.001f)
    }
}
