package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanyCarBikEngineTest {

    @Test
    fun testPureEvBikRatesAcrossYears() {
        // £40,000 EV, 2024/25 (2%) vs 2025/26 (3%)
        val res2024 = CompanyCarBikEngine.calculateCompanyCar(
            p11dValue = 40000.0,
            powertrain = PowertrainType.PURE_EV,
            taxYear = BikTaxYear.YEAR_2024_2025,
            grossMonthlySalarySacrifice = 500.0,
            annualGrossIncome = 60000.0
        )
        assertEquals(2.0, res2024.bikPercentage, 0.01)
        assertEquals(800.0, res2024.annualBikTaxableValue, 0.01)
        // 40% higher rate tax on £800 = £320/yr = £26.67/mo
        assertEquals(26.67, res2024.monthlyBikTaxPayable, 0.01)

        val res2025 = CompanyCarBikEngine.calculateCompanyCar(
            p11dValue = 40000.0,
            powertrain = PowertrainType.PURE_EV,
            taxYear = BikTaxYear.YEAR_2025_2026,
            grossMonthlySalarySacrifice = 500.0,
            annualGrossIncome = 60000.0
        )
        assertEquals(3.0, res2025.bikPercentage, 0.01)
        assertEquals(1200.0, res2025.annualBikTaxableValue, 0.01)
        // 40% tax on £1200 = £480/yr = £40.00/mo
        assertEquals(40.00, res2025.monthlyBikTaxPayable, 0.01)
    }

    @Test
    fun testNetCostReliefSavings() {
        // £500 gross monthly sacrifice at 40% tax + 2% NI (42% relief = £210 saved)
        val res = CompanyCarBikEngine.calculateCompanyCar(
            p11dValue = 45000.0,
            powertrain = PowertrainType.PURE_EV,
            taxYear = BikTaxYear.YEAR_2024_2025,
            grossMonthlySalarySacrifice = 500.0,
            annualGrossIncome = 65000.0
        )
        assertEquals(200.0, res.monthlyTaxSaved, 0.01)
        assertEquals(10.0, res.monthlyNiSaved, 0.01)
        assertEquals(210.0, res.totalMonthlyRelief, 0.01)
        // 2% BiK on £45,000 = £900/yr = £75/mo BiK value -> 40% tax = £30/mo
        assertEquals(30.00, res.monthlyBikTaxPayable, 0.01)
        // Net cost = £500 - £210 + £30 = £320.00/mo
        assertEquals(320.00, res.netMonthlyCost, 0.01)
        assertTrue(res.monthlySavingsVsPrivate > 0.0)
    }
}
