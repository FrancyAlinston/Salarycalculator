package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SspHolidayEngineTest {

    @Test
    fun testSspWaitingDaysStandard5DayWorker() {
        // 5 qualifying days per week, 5 sick days logged
        // 3 waiting days unpaid, 2 days paid
        // Daily rate: £116.75 / 5 = £23.35
        // Expected total: 2 * 23.35 = £46.70
        val result = SspHolidayEngine.calculateSsp(
            qualifyingDaysPerWeek = 5,
            sickDaysLogged = 5,
            isLinkedPeriod = false
        )
        assertEquals(3, result.waitingDaysCount)
        assertEquals(2, result.paidSspDaysCount)
        assertEquals(23.35, result.dailySspRate, 0.01)
        assertEquals(46.70, result.totalGrossSsp, 0.02)
    }

    @Test
    fun testSspLinkedPeriodBypassesWaitingDays() {
        // Linked period (within 8 weeks of previous illness)
        // All 4 sick days should be paid
        val result = SspHolidayEngine.calculateSsp(
            qualifyingDaysPerWeek = 5,
            sickDaysLogged = 4,
            isLinkedPeriod = true
        )
        assertEquals(0, result.waitingDaysCount)
        assertEquals(4, result.paidSspDaysCount)
        assertEquals(23.35, result.dailySspRate, 0.01)
        assertEquals(93.40, result.totalGrossSsp, 0.05)
    }

    @Test
    fun testSspCareWorker3DayWeek() {
        // Care worker working three 12-hour shifts a week
        // 3 qualifying days per week, 4 sick days logged
        // 3 waiting days unpaid, 1 day paid
        // Daily rate: £116.75 / 3 = £38.92
        val result = SspHolidayEngine.calculateSsp(
            qualifyingDaysPerWeek = 3,
            sickDaysLogged = 4,
            isLinkedPeriod = false
        )
        assertEquals(3, result.waitingDaysCount)
        assertEquals(1, result.paidSspDaysCount)
        assertEquals(38.92, result.dailySspRate, 0.01)
        assertEquals(38.92, result.totalGrossSsp, 0.02)
    }

    @Test
    fun testHolidayAccrual12Point07Percent() {
        // 160 hours worked @ £12.82/hr
        // 160 * 0.1207 = 19.31 hours accrued
        // 19.31 * 12.82 = £247.55
        val result = SspHolidayEngine.calculateHolidayAccrual(
            hoursWorked = 160.0,
            hourlyRate = 12.82,
            standardShiftHours = 12.0
        )
        assertEquals(19.31, result.accruedHolidayHours, 0.02)
        assertEquals(247.55, result.accruedHolidayPay, 0.10)
        assertEquals(14.37, result.rolledUpHourlyRate, 0.02)
        assertEquals(1.6, result.daysEquivalent, 0.1)
    }

    @Test
    fun testCombinedImpact() {
        val result = SspHolidayEngine.calculateCombinedImpact(
            qualifyingDaysPerWeek = 5,
            sickDaysLogged = 5,
            isLinkedPeriod = false,
            hoursWorked = 160.0,
            hourlyRate = 12.82,
            standardShiftHours = 12.0
        )
        // SSP: £46.70 + Holiday: £247.55 = £294.25
        assertEquals(294.25, result.totalStatutoryAddition, 0.20)
    }
}
