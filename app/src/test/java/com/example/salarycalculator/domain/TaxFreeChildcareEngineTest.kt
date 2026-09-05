package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class TaxFreeChildcareEngineTest {

    @Test
    fun testEligibleStandardTopUpAndFreeHours() {
        val input = TaxFreeChildcareInput(
            annualGrossIncome = 50000.0,
            eligibleChildrenCount = 1,
            annualChildcareSpendPerChild = 10000.0,
            has30HoursFreeChildcare = true,
            averageChildcareHourlyCost = 6.0
        )

        val result = TaxFreeChildcareEngine.calculate(input)

        assertTrue(result.isEligible)
        assertFalse(result.isCliffEdgeHit)
        // 20% of £10k capped at £2,000
        assertEquals(2000.0, result.governmentTopUpAnnual, 0.01)
        // 1,140 hours * £6 = £6,840
        assertEquals(6840.0, result.freeChildcareHoursAnnualValue, 0.01)
        assertEquals(8840.0, result.totalChildcareBenefitValue, 0.01)
        assertEquals(0.0, result.requiredPensionSacrificeForEligibility, 0.01)
    }

    @Test
    fun testCliffEdgeOver100kAndSacrificeRemedy() {
        val input = TaxFreeChildcareInput(
            annualGrossIncome = 105000.0,
            eligibleChildrenCount = 2,
            annualChildcareSpendPerChild = 10000.0,
            has30HoursFreeChildcare = true
        )

        val result = TaxFreeChildcareEngine.calculate(input)

        assertFalse(result.isEligible)
        assertTrue(result.isCliffEdgeHit)
        assertEquals(5000.0, result.excessOver100k, 0.01)
        assertEquals(0.0, result.totalChildcareBenefitValue, 0.01) // 100% lost

        // Remedy required: £5,001 sacrifice
        assertEquals(5001.0, result.requiredPensionSacrificeForEligibility, 0.01)
        assertTrue(result.totalAnnualGainBySacrificing > 5000.0) // Significant positive return
    }

    @Test
    fun testDisabledChildCapAt4000() {
        val input = TaxFreeChildcareInput(
            annualGrossIncome = 40000.0,
            eligibleChildrenCount = 0,
            disabledChildrenCount = 1,
            annualChildcareSpendPerChild = 25000.0,
            has30HoursFreeChildcare = false
        )

        val result = TaxFreeChildcareEngine.calculate(input)

        assertTrue(result.isEligible)
        // 20% of £25,000 capped at £4,000 for disabled child
        assertEquals(4000.0, result.governmentTopUpAnnual, 0.01)
    }
}
