package com.example.salarycalculator.domain

import com.example.salarycalculator.R
import com.example.salarycalculator.widget.ShiftCycleEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftCycleEngineTest {

    @Test
    fun testCompleteShiftCycleSequence() {
        val standardHours = 12.0

        // 1. Off (0.0) -> Standard Shift (12.0)
        val state1 = ShiftCycleEngine.getNextShiftState(0.0, standardHours)
        assertEquals(12.0, state1, 0.001)
        assertEquals("Today: 12h", ShiftCycleEngine.getShiftStateLabel(state1))
        assertEquals(R.drawable.widget_pill_emerald_bg, ShiftCycleEngine.getShiftPillBackgroundRes(state1))

        // 2. Standard Shift (12.0) -> Overtime Shift (-12.0)
        val state2 = ShiftCycleEngine.getNextShiftState(state1, standardHours)
        assertEquals(-12.0, state2, 0.001)
        assertEquals("Today: 12h OT", ShiftCycleEngine.getShiftStateLabel(state2))
        assertEquals(R.drawable.widget_pill_amber_bg, ShiftCycleEngine.getShiftPillBackgroundRes(state2))

        // 3. Overtime Shift (-12.0) -> Annual Leave (-999.0)
        val state3 = ShiftCycleEngine.getNextShiftState(state2, standardHours)
        assertEquals(SalaryRepository.ANNUAL_LEAVE_HOURS, state3, 0.001)
        assertEquals("Today: A/L", ShiftCycleEngine.getShiftStateLabel(state3))
        assertEquals(R.drawable.widget_pill_blue_bg, ShiftCycleEngine.getShiftPillBackgroundRes(state3))

        // 4. Annual Leave (-999.0) -> Statutory Sick Pay (-998.0)
        val state4 = ShiftCycleEngine.getNextShiftState(state3, standardHours)
        assertEquals(SalaryRepository.STATUTORY_SICK_LEAVE_HOURS, state4, 0.001)
        assertEquals("Today: SSP", ShiftCycleEngine.getShiftStateLabel(state4))
        assertEquals(R.drawable.widget_pill_purple_bg, ShiftCycleEngine.getShiftPillBackgroundRes(state4))

        // 5. Statutory Sick Pay (-998.0) -> Off (0.0)
        val state5 = ShiftCycleEngine.getNextShiftState(state4, standardHours)
        assertEquals(0.0, state5, 0.001)
        assertEquals("Today: Off", ShiftCycleEngine.getShiftStateLabel(state5))
        assertEquals(R.drawable.widget_pill_off_bg, ShiftCycleEngine.getShiftPillBackgroundRes(state5))
    }

    @Test
    fun testCustomHoursAndDecimals() {
        val standardHours = 7.5

        val std = ShiftCycleEngine.getNextShiftState(0.0, standardHours)
        assertEquals(7.5, std, 0.001)
        assertEquals("Today: 7.5h", ShiftCycleEngine.getShiftStateLabel(std))

        val ot = ShiftCycleEngine.getNextShiftState(std, standardHours)
        assertEquals(-7.5, ot, 0.001)
        assertEquals("Today: 7.5h OT", ShiftCycleEngine.getShiftStateLabel(ot))
    }

    @Test
    fun testConfirmationToastMessages() {
        assertEquals("Logged Today: 12h Duty Shift", ShiftCycleEngine.getShiftConfirmationToast(12.0))
        assertEquals("Logged Today: 12h Overtime Shift", ShiftCycleEngine.getShiftConfirmationToast(-12.0))
        assertEquals("Logged Today: Annual Leave (A/L)", ShiftCycleEngine.getShiftConfirmationToast(SalaryRepository.ANNUAL_LEAVE_HOURS))
        assertEquals("Logged Today: Statutory Sick Pay (SSP)", ShiftCycleEngine.getShiftConfirmationToast(SalaryRepository.STATUTORY_SICK_LEAVE_HOURS))
        assertEquals("Logged Today: Day Off", ShiftCycleEngine.getShiftConfirmationToast(0.0))
    }
}
