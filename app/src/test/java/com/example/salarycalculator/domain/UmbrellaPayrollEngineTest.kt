package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class UmbrellaPayrollEngineTest {

    @Test
    fun testUmbrella20252026Calculation() {
        val input = UmbrellaPayrollInput(
            rateType = UmbrellaRateType.DAILY,
            rateAmount = 450.0,
            unitsWorkedPerWeek = 5.0,
            weeksWorkedPerYear = 48.0,
            umbrellaMarginWeekly = 25.0,
            taxYear = TaxYear.YEAR_2025_2026,
            taxRegion = TaxRegion.UK_STANDARD
        )

        val result = UmbrellaPayrollEngine.calculate(input)

        // £450 * 5 * 48 = £108,000 invoiced
        assertEquals(108000.0, result.annualInvoicedRevenue, 0.01)
        assertEquals(1200.0, result.annualUmbrellaMargin, 0.01) // £25 * 48
        assertEquals(0.15, result.employerNiRateUsed, 0.001)
        assertEquals(5000.0, result.employerNiThresholdUsed, 0.01)

        // Ensure gross pay is solved correctly and lower than available fund
        assertTrue(result.annualGrossPay > 0.0)
        assertTrue(result.annualGrossPay < result.annualInvoicedRevenue)

        // Company costs
        assertTrue(result.annualEmployerNi > 0.0)
        assertTrue(result.annualApprenticeshipLevy > 0.0)
        assertTrue(result.annualEmployerPension > 0.0)

        // Net pay and retention
        assertTrue(result.annualNetPay > 0.0)
        assertTrue(result.netRetentionPercentage in 50.0..70.0)
    }

    @Test
    fun testUmbrella20242025Comparison() {
        val input = UmbrellaPayrollInput(
            rateType = UmbrellaRateType.DAILY,
            rateAmount = 500.0,
            unitsWorkedPerWeek = 5.0,
            weeksWorkedPerYear = 48.0,
            umbrellaMarginWeekly = 30.0,
            taxYear = TaxYear.YEAR_2024_2025,
            taxRegion = TaxRegion.UK_STANDARD
        )

        val result = UmbrellaPayrollEngine.calculate(input)

        assertEquals(120000.0, result.annualInvoicedRevenue, 0.01)
        assertEquals(0.138, result.employerNiRateUsed, 0.001)
        assertEquals(9100.0, result.employerNiThresholdUsed, 0.01)
        assertTrue(result.annualNetPay > 0.0)
    }

    @Test
    fun testHourlyRateUmbrellaCalculation() {
        val input = UmbrellaPayrollInput(
            rateType = UmbrellaRateType.HOURLY,
            rateAmount = 60.0,
            unitsWorkedPerWeek = 37.5,
            weeksWorkedPerYear = 48.0,
            umbrellaMarginWeekly = 20.0
        )

        val result = UmbrellaPayrollEngine.calculate(input)

        // 60 * 37.5 * 48 = £108,000
        assertEquals(108000.0, result.annualInvoicedRevenue, 0.01)
        assertTrue(result.weeklyNetPay > 0.0)
        assertTrue(result.dailyNetPay > 0.0)
    }
}
