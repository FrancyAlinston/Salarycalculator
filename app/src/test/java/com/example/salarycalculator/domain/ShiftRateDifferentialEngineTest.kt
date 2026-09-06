package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftRateDifferentialEngineTest {

    @Test
    fun testStandardDayShiftOnly() {
        val input = ShiftRateDifferentialEngine.ShiftDifferentialInput(
            baseHourlyRate = 12.82,
            standardDayHours = 120.0,
            nightHours = 0.0,
            weekendHours = 0.0,
            bankHolidayHours = 0.0,
            sleepInDutiesCount = 0
        )
        val result = ShiftRateDifferentialEngine.calculateDifferentials(input)
        assertEquals(120.0, result.totalWorkingHours, 0.001)
        assertEquals(1538.40, result.standardBasePay, 0.01)
        assertEquals(1538.40, result.totalGrossPay, 0.01)
        assertEquals(0.0, result.totalDifferentialUplift, 0.01)
        assertEquals(12.82, result.blendedHourlyRate, 0.01)
    }

    @Test
    fun testNightHourlyAdditionAndWeekendMultiplier() {
        // Base: £12.82/hr
        // 96h Day: 96 * 12.82 = £1230.72
        // 36h Night with +£2.00/hr = £14.82/hr => 36 * 14.82 = £533.52 (uplift = 36 * 2 = £72.00)
        // 24h Weekend with 1.25x = £16.025/hr => 24 * 16.025 = £384.60 (uplift = £76.92)
        // Total Hours = 156.0h
        // Total Gross = 1230.72 + 533.52 + 384.60 = £2148.84
        // Blended Rate = 2148.84 / 156.0 = £13.77/hr
        val input = ShiftRateDifferentialEngine.ShiftDifferentialInput(
            baseHourlyRate = 12.82,
            standardDayHours = 96.0,
            nightHours = 36.0,
            nightUpliftType = ShiftRateDifferentialEngine.UpliftType.HOURLY_ADDITION,
            nightUpliftValue = 2.00,
            weekendHours = 24.0,
            weekendMultiplier = 1.25,
            bankHolidayHours = 0.0,
            sleepInDutiesCount = 0
        )
        val result = ShiftRateDifferentialEngine.calculateDifferentials(input)
        assertEquals(156.0, result.totalWorkingHours, 0.001)
        assertEquals(1230.72, result.standardBasePay, 0.01)
        assertEquals(14.82, result.nightEffectiveRate, 0.01)
        assertEquals(533.52, result.nightTotalPay, 0.01)
        assertEquals(72.00, result.nightUpliftGross, 0.01)
        assertEquals(16.02, result.weekendEffectiveRate, 0.02)
        assertEquals(384.60, result.weekendTotalPay, 0.02)
        assertEquals(2148.84, result.totalGrossPay, 0.05)
        assertEquals(13.77, result.blendedHourlyRate, 0.02)
    }

    @Test
    fun testBankHolidayDoubleTimeAndSleepInAllowance() {
        // Base £12.82/hr
        // 12h Bank Holiday @ 2.0x = £25.64/hr => 12 * 25.64 = £307.68
        // 2 Sleep-in duties @ £50.00 = £100.00
        val input = ShiftRateDifferentialEngine.ShiftDifferentialInput(
            baseHourlyRate = 12.82,
            standardDayHours = 0.0,
            nightHours = 0.0,
            weekendHours = 0.0,
            bankHolidayHours = 12.0,
            bankHolidayMultiplier = 2.0,
            sleepInDutiesCount = 2,
            sleepInAllowancePerDuty = 50.0
        )
        val result = ShiftRateDifferentialEngine.calculateDifferentials(input)
        assertEquals(12.0, result.totalWorkingHours, 0.001)
        assertEquals(25.64, result.bankHolidayEffectiveRate, 0.01)
        assertEquals(307.68, result.bankHolidayTotalPay, 0.01)
        assertEquals(100.00, result.sleepInTotalPay, 0.01)
        assertEquals(407.68, result.totalGrossPay, 0.01)
        assertEquals(253.84, result.totalDifferentialUplift, 0.01)
    }
}
