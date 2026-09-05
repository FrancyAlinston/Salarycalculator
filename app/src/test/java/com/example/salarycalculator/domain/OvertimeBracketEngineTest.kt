package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class OvertimeBracketEngineTest {

    @Test
    fun testHeadroomUnderHigherRateBand() {
        val input = OvertimeBracketInput(
            baseAnnualGross = 40000.0,
            hourlyRate = 20.0,
            overtimeMultiplier = 1.5,
            monthlyOvertimeHours = 10.0,
            pensionSacrificeRate = 0.0
        )

        val result = OvertimeBracketEngine.calculate(input)

        // £20 * 1.5 = £30/hr. 10 hrs/mo * 12 = £3,600 OT.
        // Total Gross = £43,600.
        // Headroom to £50,270 = £50,270 - £43,600 = £6,670.
        assertEquals(43600.0, result.totalAnnualGross, 0.01)
        assertFalse(result.isHigherRateCrossed)
        assertEquals(6670.0, result.higherRateHeadroomPounds, 0.01)
        assertTrue(result.maxMonthlyOvertimeHoursBeforeHigherRate > 20.0)
    }

    @Test
    fun testCrossingHigherRateThresholdAndSacrificeRemedy() {
        val input = OvertimeBracketInput(
            baseAnnualGross = 48000.0,
            hourlyRate = 20.0,
            overtimeMultiplier = 1.5,
            monthlyOvertimeHours = 20.0, // £600/mo * 12 = £7,200 OT -> £55,200
            pensionSacrificeRate = 0.0
        )

        val result = OvertimeBracketEngine.calculate(input)

        assertEquals(55200.0, result.totalAnnualGross, 0.01)
        assertTrue(result.isHigherRateCrossed)
        // £55,200 - £50,270 = £4,930 excess / 12 = £410.83/mo sacrifice
        assertEquals(410.83, result.recommendedPensionSacrificeToStayBelowHigherRate, 0.5)
    }

    @Test
    fun testCrossing100kTaxTrapThreshold() {
        val input = OvertimeBracketInput(
            baseAnnualGross = 95000.0,
            hourlyRate = 50.0,
            overtimeMultiplier = 1.5,
            monthlyOvertimeHours = 15.0, // £75/hr * 15 * 12 = £13,500 OT -> £108,500
            pensionSacrificeRate = 0.0
        )

        val result = OvertimeBracketEngine.calculate(input)

        assertEquals(108500.0, result.totalAnnualGross, 0.01)
        assertTrue(result.isTaxTrapCrossed)
        // £108,500 - £100,000 = £8,500 excess / 12 = £708.33/mo
        assertEquals(708.33, result.recommendedPensionSacrificeToStayBelowTaxTrap, 0.5)
    }
}
