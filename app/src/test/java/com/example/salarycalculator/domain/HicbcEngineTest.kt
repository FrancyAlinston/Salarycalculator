package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HicbcEngineTest {

    @Test
    fun testZeroChildrenEntitlement() {
        val result = HicbcEngine.calculateHicbc(
            annualGrossIncome = 70000.0,
            numberOfChildren = 0
        )
        assertEquals(0.0, result.annualChildBenefitReceived, 0.01)
        assertEquals(0.0, result.annualTaxCharge, 0.01)
        assertFalse(result.isTaperApplicable)
    }

    @Test
    fun testOneChildRatesBelowThreshold() {
        // 1 Child = £25.60 * 52 = £1,331.20/year
        val result = HicbcEngine.calculateHicbc(
            annualGrossIncome = 50000.0,
            numberOfChildren = 1
        )
        assertEquals(1331.20, result.annualChildBenefitReceived, 0.01)
        assertEquals(0.0, result.annualTaxCharge, 0.01)
        assertEquals(0.0, result.clawbackPercentage, 0.01)
        assertEquals(1331.20, result.netChildBenefitRetained, 0.01)
        assertFalse(result.isTaperApplicable)
    }

    @Test
    fun testTwoChildrenMidTaperAt70k() {
        // 2 Children = £1,331.20 + (£16.95 * 52 = £881.40) = £2,212.60/year
        // Income £70,000 is £10,000 above £60,000 threshold
        // Clawback = £10,000 / £200 = 50%
        val result = HicbcEngine.calculateHicbc(
            annualGrossIncome = 70000.0,
            numberOfChildren = 2
        )
        assertEquals(2212.60, result.annualChildBenefitReceived, 0.01)
        assertTrue(result.isTaperApplicable)
        assertEquals(50.0, result.clawbackPercentage, 0.01)
        assertEquals(1106.30, result.annualTaxCharge, 0.01)
        assertEquals(1106.30, result.netChildBenefitRetained, 0.01)
        assertFalse(result.isFullyClawedBack)
        assertEquals(10000.0, result.recommendedPensionSacrifice, 0.01)
        assertEquals(1106.30, result.restoredChildBenefitAnnual, 0.01)
    }

    @Test
    fun testFullClawbackAt80kPlus() {
        // Income £85,000 >= £80,000 -> 100% clawback
        val result = HicbcEngine.calculateHicbc(
            annualGrossIncome = 85000.0,
            numberOfChildren = 3
        )
        assertTrue(result.isTaperApplicable)
        assertTrue(result.isFullyClawedBack)
        assertEquals(100.0, result.clawbackPercentage, 0.01)
        assertEquals(0.0, result.netChildBenefitRetained, 0.01)
        assertEquals(result.annualChildBenefitReceived, result.annualTaxCharge, 0.01)
        assertEquals(25000.0, result.recommendedPensionSacrifice, 0.01)
    }
}
