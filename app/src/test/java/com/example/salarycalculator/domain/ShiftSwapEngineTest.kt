package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class ShiftSwapEngineTest {

    @Test
    fun testPickUpOvertimeCoverShift_calculatesPositiveNetAndGross() {
        val input = ShiftSwapSimulationInput(
            colleagueName = "Cecilia",
            dayOfMonth = 15,
            swapType = ShiftSwapType.PICK_UP_COVER,
            currentShiftHours = 0.0,
            newShiftHours = 12.0,
            baseHourlyRate = 15.0,
            currentMonthlyGross = 2400.0,
            taxCode = "1257L",
            overtimeMultiplier = 1.5,
            isOvertimeCover = true
        )

        val result = ShiftSwapEngine.calculateShiftSwapImpact(input)

        // Gross delta: 12h * £15 * 1.5 = £270.00
        assertEquals(270.0, result.grossDelta, 0.01)
        assertEquals(2400.0, result.baselineGrossMonthly, 0.01)
        assertEquals(2670.0, result.postSwapGrossMonthly, 0.01)

        // Tax (20%) = £54.00, NI (8%) = £21.60 -> Net delta = £194.40
        assertEquals(54.00, result.taxDelta, 0.05)
        assertEquals(21.60, result.niDelta, 0.05)
        assertEquals(194.40, result.netDelta, 0.05)

        // 72% retention rate
        assertEquals(72.0, result.marginalRetentionRate, 0.5)

        // Net rate: £194.40 / 12h = £16.20/hr
        assertEquals(16.20, result.netHourlyYield, 0.05)
        assertTrue(result.summaryAdvice.contains("Cecilia"))
    }

    @Test
    fun testGiveAwayShift_calculatesAccurateNetReduction() {
        val input = ShiftSwapSimulationInput(
            colleagueName = "Vanessa",
            dayOfMonth = 20,
            swapType = ShiftSwapType.GIVE_AWAY,
            currentShiftHours = 12.0,
            newShiftHours = 0.0,
            baseHourlyRate = 15.0,
            currentMonthlyGross = 2400.0,
            taxCode = "1257L"
        )

        val result = ShiftSwapEngine.calculateShiftSwapImpact(input)

        // Gross reduction: -12h * £15 = -£180.00
        assertEquals(-180.0, result.grossDelta, 0.01)
        assertEquals(2400.0, result.baselineGrossMonthly, 0.01)
        assertEquals(2220.0, result.postSwapGrossMonthly, 0.01)

        // Net reduction: £180 - £36 (tax) - £14.40 (NI) = -£129.60
        assertEquals(-129.60, result.netDelta, 0.05)
        assertTrue(result.summaryAdvice.contains("Vanessa"))
    }

    @Test
    fun testEqualDirectSwap_zeroFinancialDelta() {
        val input = ShiftSwapSimulationInput(
            colleagueName = "Francy D'Silva",
            dayOfMonth = 10,
            swapType = ShiftSwapType.DIRECT_SWAP,
            currentShiftHours = 12.0,
            newShiftHours = 12.0,
            baseHourlyRate = 15.0,
            currentMonthlyGross = 2400.0,
            taxCode = "1257L"
        )

        val result = ShiftSwapEngine.calculateShiftSwapImpact(input)
        assertEquals(0.0, result.grossDelta, 0.001)
        assertEquals(0.0, result.netDelta, 0.001)
        assertEquals(100.0, result.marginalRetentionRate, 0.001)
        assertTrue(result.summaryAdvice.contains("Equal hours swap"))
    }
}
