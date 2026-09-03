package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PensionAndStudentLoanTest {

    @Test
    fun pensionAllowance_standardSalary_receivesFull60kAllowance() {
        val report = PensionAllowanceOptimizer.calculatePensionAllowance(
            grossEarnings = 50000.0,
            employeePensionPercent = 5.0,
            employerPensionPercent = 3.0,
            hasTriggeredMpaa = false,
            unusedYearMinus1 = 0.0,
            unusedYearMinus2 = 0.0,
            unusedYearMinus3 = 0.0
        )

        assertEquals(60000.0, report.baseAllowance, 0.01)
        assertFalse(report.isTapered)
        assertFalse(report.isMpaaApplied)
        assertEquals(0.0, report.excessContribution, 0.01)
        assertEquals(0.0, report.estimatedTaxCharge, 0.01)
        assertEquals(56000.0, report.remainingAllowance, 0.01) // 60k - 4k
    }

    @Test
    fun pensionAllowance_highEarner_tapersAllowanceCorrectly() {
        // £280,000 earnings -> £20,000 over £260k taper start -> £10,000 reduction -> £50,000 allowance
        val report = PensionAllowanceOptimizer.calculatePensionAllowance(
            grossEarnings = 280000.0,
            employeePensionPercent = 0.0,
            employerPensionPercent = 0.0,
            hasTriggeredMpaa = false,
            unusedYearMinus1 = 0.0,
            unusedYearMinus2 = 0.0,
            unusedYearMinus3 = 0.0
        )

        assertTrue(report.isTapered)
        assertEquals(50000.0, report.baseAllowance, 0.01)
    }

    @Test
    fun pensionAllowance_mpaaTriggered_restrictsTo10kWithNoCarryForward() {
        val report = PensionAllowanceOptimizer.calculatePensionAllowance(
            grossEarnings = 80000.0,
            employeePensionPercent = 10.0,
            employerPensionPercent = 5.0, // total 15% of 80k = 12k
            hasTriggeredMpaa = true,
            unusedYearMinus1 = 20000.0,
            unusedYearMinus2 = 20000.0,
            unusedYearMinus3 = 20000.0
        )

        assertTrue(report.isMpaaApplied)
        assertEquals(10000.0, report.baseAllowance, 0.01)
        assertEquals(0.0, report.totalCarryForward, 0.01)
        assertEquals(2000.0, report.excessContribution, 0.01) // 12k - 10k
        assertTrue(report.estimatedTaxCharge > 0.0)
    }

    @Test
    fun studentLoan_lowEarner_calculatesStatutoryWriteOff() {
        val report = StudentLoanPayoffEngine.calculatePayoffTimeline(
            plan = StudentLoanPlan.PLAN_2,
            currentBalance = 45000.0,
            annualSalary = 30000.0,
            annualSalaryGrowthPercent = 1.0,
            extraMonthlyOverpayment = 0.0
        )

        assertTrue(report.willBeWrittenOff)
        assertTrue(report.writtenOffBalance > 0.0)
        assertTrue(report.strategicRecommendation.contains("Write-Off"))
    }

    @Test
    fun studentLoan_highEarner_calculatesEarlyPayoffSavings() {
        val reportWithOverpay = StudentLoanPayoffEngine.calculatePayoffTimeline(
            plan = StudentLoanPlan.PLAN_1,
            currentBalance = 15000.0,
            annualSalary = 65000.0,
            extraMonthlyOverpayment = 200.0
        )

        assertFalse(reportWithOverpay.willBeWrittenOff)
        assertTrue(reportWithOverpay.estimatedYearsToPayoff < 5.0)
        assertNotNull(reportWithOverpay.yearlyTrajectory)
    }
}
