package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class OvertimeOptimizerTest {

    @Test
    fun testOvertimeOptimizer_BasicRate() {
        // Base £2,500/mo (£30,000/yr). Base rate £15/hr, 10h overtime at 1.5x (£22.50/hr gross = £225 extra gross)
        val result = OvertimeOptimizerEngine.calculateOvertimeReturn(
            baseGrossMonthly = 2500.0,
            baseHourlyRate = 15.0,
            extraOtHours = 10.0,
            overtimeMultiplier = 1.5,
            pensionRate = 0.0,
            studentLoanPlan = StudentLoanPlan.NONE
        )

        assertEquals(225.0, result.extraGross, 0.01)
        // In basic rate: extra gross is taxed at 20% Income Tax + 8% NI = 28% total deductions (72% retention)
        assertEquals(45.0, result.extraTax, 0.5)
        assertEquals(18.0, result.extraNi, 0.5)
        assertEquals(162.0, result.extraNetPay, 1.0)
        assertEquals(16.20, result.netPerHour, 0.1)
        assertEquals(72.0, result.retentionPercentage, 1.0)
        assertEquals(OvertimeEfficiencyRating.HIGH, result.efficiencyRating)
        assertNull(result.taxTrapWarning)
    }

    @Test
    fun testOvertimeOptimizer_HigherRate() {
        // Base £5,000/mo (£60,000/yr). Base rate £30/hr, 10h overtime at 1.5x (£45/hr gross = £450 extra gross)
        val result = OvertimeOptimizerEngine.calculateOvertimeReturn(
            baseGrossMonthly = 5000.0,
            baseHourlyRate = 30.0,
            extraOtHours = 10.0,
            overtimeMultiplier = 1.5,
            pensionRate = 0.0,
            studentLoanPlan = StudentLoanPlan.NONE
        )

        assertEquals(450.0, result.extraGross, 0.01)
        // In higher rate: extra gross is taxed at 40% Income Tax + 2% NI = 42% total deductions (58% retention)
        assertEquals(180.0, result.extraTax, 0.5)
        assertEquals(9.0, result.extraNi, 0.5)
        assertEquals(261.0, result.extraNetPay, 1.0)
        assertEquals(26.10, result.netPerHour, 0.1)
        assertEquals(58.0, result.retentionPercentage, 1.0)
        assertEquals(OvertimeEfficiencyRating.MODERATE, result.efficiencyRating)
    }

    @Test
    fun testOvertimeOptimizer_TaxTrapWarning() {
        // Base £8,333.33/mo (£100,000/yr). Overtime of 20h at £50/hr * 1.5x = £1,500 extra gross -> pushes into marginal trap
        val result = OvertimeOptimizerEngine.calculateOvertimeReturn(
            baseGrossMonthly = 8333.33,
            baseHourlyRate = 50.0,
            extraOtHours = 20.0,
            overtimeMultiplier = 1.5,
            pensionRate = 0.0,
            studentLoanPlan = StudentLoanPlan.NONE
        )

        assertNotNull(result.taxTrapWarning)
        assertTrue(result.taxTrapWarning!!.contains("Marginal Tax Trap") || result.taxTrapWarning!!.contains("personal allowance tapering"))
    }

    @Test
    fun testEmployerProfileColorHex() {
        val profile = EmployerProfile(
            name = "Secondary Job",
            employerName = "Agency Healthcare",
            hourlyRate = 18.50,
            colorHex = "#8B5CF6"
        )
        assertEquals("#8B5CF6", profile.colorHex)
    }
}
