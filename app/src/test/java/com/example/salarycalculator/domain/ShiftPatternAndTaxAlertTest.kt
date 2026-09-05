package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class ShiftPatternAndTaxAlertTest {

    @Test
    fun testTaxBracketAlert_StandardBasicRate() {
        val alert = TaxCalculator.calculateTaxBracketAlerts(35000.0)
        assertFalse(alert.isHigherRate)
        assertFalse(alert.isMarginalTrap)
        assertNull(alert.thresholdCrossed)
        assertEquals("Basic Rate (20%)", alert.currentBracket)
    }

    @Test
    fun testTaxBracketAlert_HigherRate() {
        val alert = TaxCalculator.calculateTaxBracketAlerts(65000.0)
        assertTrue(alert.isHigherRate)
        assertFalse(alert.isMarginalTrap)
        assertEquals("Higher Rate (40%)", alert.currentBracket)
        assertNotNull(alert.thresholdCrossed)
        assertTrue(alert.thresholdCrossed!!.contains("£50,270"))
        assertEquals(14730.0, alert.excessOverThreshold, 0.01)
        assertEquals(14730.0, alert.suggestedPensionSacrifice, 0.01)
        assertEquals(14730.0 * 0.40, alert.estimatedTaxSavedWithSacrifice, 0.01)
    }

    @Test
    fun testTaxBracketAlert_MarginalTaxTrap() {
        val alert = TaxCalculator.calculateTaxBracketAlerts(115000.0)
        assertTrue(alert.isHigherRate)
        assertTrue(alert.isMarginalTrap)
        assertEquals("60% Marginal Tax Trap", alert.currentBracket)
        assertNotNull(alert.thresholdCrossed)
        assertTrue(alert.thresholdCrossed!!.contains("£100,000"))
        assertEquals(15000.0, alert.excessOverThreshold, 0.01)
        assertEquals(15000.0, alert.suggestedPensionSacrifice, 0.01)
        assertEquals(15000.0 * 0.60, alert.estimatedTaxSavedWithSacrifice, 0.01)
    }

    @Test
    fun testShiftPatternGenerator_FourOnFourOff() {
        val schedule = ShiftPatternGenerator.generatePatternShifts(
            year = 2025,
            startMonth = 10,
            endMonth = 10,
            anchorYear = 2025,
            anchorMonth = 10,
            anchorDay = 1,
            pattern = ShiftPatternType.FOUR_ON_FOUR_OFF,
            dayHours = 12.0,
            nightHours = 12.0
        )

        // Month key "2025-10"
        val octShifts = schedule["2025-10"]
        assertNotNull(octShifts)
        assertTrue(octShifts!!.isNotEmpty())

        // Day 1..4 should be working (12.0h), Day 5..8 should be off (0.0h), Day 9..12 working (12.0h)
        assertEquals(12.0, octShifts[1] ?: 0.0, 0.01)
        assertEquals(12.0, octShifts[2] ?: 0.0, 0.01)
        assertEquals(12.0, octShifts[3] ?: 0.0, 0.01)
        assertEquals(12.0, octShifts[4] ?: 0.0, 0.01)
        assertEquals(0.0, octShifts[5] ?: 0.0, 0.01)
        assertEquals(0.0, octShifts[6] ?: 0.0, 0.01)
        assertEquals(0.0, octShifts[7] ?: 0.0, 0.01)
        assertEquals(0.0, octShifts[8] ?: 0.0, 0.01)
        assertEquals(12.0, octShifts[9] ?: 0.0, 0.01)
    }

    @Test
    fun testShiftPatternGenerator_MonToFri() {
        val schedule = ShiftPatternGenerator.generatePatternShifts(
            year = 2025,
            startMonth = 10,
            endMonth = 10,
            anchorYear = 2025,
            anchorMonth = 10,
            anchorDay = 1,
            pattern = ShiftPatternType.STANDARD_MON_FRI,
            dayHours = 8.0,
            nightHours = 8.0
        )

        val octShifts = schedule["2025-10"]!!
        // Oct 1, 2025 was a Wednesday -> 8h, Oct 2 (Thu) -> 8h, Oct 3 (Fri) -> 8h, Oct 4 (Sat) -> 0h, Oct 5 (Sun) -> 0h
        assertEquals(8.0, octShifts[1] ?: 0.0, 0.01)
        assertEquals(8.0, octShifts[2] ?: 0.0, 0.01)
        assertEquals(8.0, octShifts[3] ?: 0.0, 0.01)
        assertEquals(0.0, octShifts[4] ?: 0.0, 0.01)
        assertEquals(0.0, octShifts[5] ?: 0.0, 0.01)
        assertEquals(8.0, octShifts[6] ?: 0.0, 0.01) // Oct 6 Mon
    }
}
