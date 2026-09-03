package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryForecastEngineTest {

    @Test
    fun computeSalaryForecast_emptyHistory_generatesValidBaseline() {
        val analysis = SalaryForecastEngine.computeSalaryForecast(
            history = emptyList(),
            currentHourlyRate = 20.0,
            currentHoursPerWeek = 40.0,
            taxCode = "1257L"
        )

        assertNotNull(analysis)
        assertEquals(0, analysis.historicalMonthsCount)
        assertEquals(12, analysis.remainingMonthsCount)
        assertEquals(12, analysis.forecastTimeline.size)
        assertTrue(analysis.projectedAnnualGross > 0.0)
        assertTrue(analysis.projectedAnnualNet > 0.0)
        assertTrue(analysis.rSquaredConfidence > 0.0)
    }

    @Test
    fun computeSalaryForecast_steadyGrowth_calculatesPositiveSlope() {
        val sampleHistory = listOf(
            MonthlySalaryRecord(
                monthYear = "2024-04",
                daysWorked = 20.0,
                hoursPerDay = 8.0,
                overtimeHours = 0.0,
                overtimeMultiplier = 1.5,
                hourlyRate = 20.0,
                grossPay = 3000.0,
                salarySacrifice = 0.0,
                pensionRate = 5.0,
                pensionContribution = 150.0,
                employerPension = 90.0,
                taxablePay = 1802.5,
                incomeTax = 360.5,
                nationalInsurance = 156.16,
                studentLoanDeduction = 0.0,
                totalDeductions = 666.66,
                netPay = 2333.34
            ),
            MonthlySalaryRecord(
                monthYear = "2024-05",
                daysWorked = 20.0,
                hoursPerDay = 8.0,
                overtimeHours = 5.0,
                overtimeMultiplier = 1.5,
                hourlyRate = 20.0,
                grossPay = 3150.0,
                salarySacrifice = 0.0,
                pensionRate = 5.0,
                pensionContribution = 157.5,
                employerPension = 94.5,
                taxablePay = 1945.0,
                incomeTax = 389.0,
                nationalInsurance = 168.16,
                studentLoanDeduction = 0.0,
                totalDeductions = 714.66,
                netPay = 2435.34
            ),
            MonthlySalaryRecord(
                monthYear = "2024-06",
                daysWorked = 20.0,
                hoursPerDay = 8.0,
                overtimeHours = 10.0,
                overtimeMultiplier = 1.5,
                hourlyRate = 20.0,
                grossPay = 3300.0,
                salarySacrifice = 0.0,
                pensionRate = 5.0,
                pensionContribution = 165.0,
                employerPension = 99.0,
                taxablePay = 2087.5,
                incomeTax = 417.5,
                nationalInsurance = 180.16,
                studentLoanDeduction = 0.0,
                totalDeductions = 762.66,
                netPay = 2537.34
            )
        )

        val analysis = SalaryForecastEngine.computeSalaryForecast(
            history = sampleHistory,
            taxCode = "1257L"
        )

        assertEquals(3, analysis.historicalMonthsCount)
        assertEquals(9, analysis.remainingMonthsCount)
        assertTrue("Slope should be positive for increasing salary", analysis.monthlyTrendSlope > 0)
        assertEquals(1.0, analysis.rSquaredConfidence, 0.01)
        assertTrue(analysis.keyRecommendations.isNotEmpty())
    }

    @Test
    fun computeSalaryForecast_highEarner_generatesTaxTrapWarning() {
        val highSalaryHistory = (1..6).map { i ->
            MonthlySalaryRecord(
                monthYear = "2024-0$i",
                daysWorked = 20.0,
                hoursPerDay = 8.0,
                overtimeHours = 0.0,
                overtimeMultiplier = 1.5,
                hourlyRate = 60.0,
                grossPay = 9500.0, // £114k/yr projected
                salarySacrifice = 0.0,
                pensionRate = 5.0,
                pensionContribution = 475.0,
                employerPension = 285.0,
                taxablePay = 7977.5,
                incomeTax = 2200.0,
                nationalInsurance = 350.0,
                studentLoanDeduction = 0.0,
                totalDeductions = 3025.0,
                netPay = 6475.0
            )
        }

        val analysis = SalaryForecastEngine.computeSalaryForecast(
            history = highSalaryHistory,
            taxCode = "1257L"
        )

        assertTrue(analysis.projectedAnnualGross > 100000.0)
        assertTrue(analysis.keyRecommendations.any { it.contains("60% Marginal Tax Trap") })
    }
}
