package com.example.salarycalculator.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftPersistenceAndSandboxTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testMultiYearScheduleSerializationAndLoading() {
        val schedule = mapOf(
            "2026-9" to mapOf("1" to 8.0, "2" to 10.0, "3" to 12.0),
            "2026-10" to mapOf("5" to 8.0, "6" to 8.0),
            "2027-1" to mapOf("1" to 8.0)
        )

        val encoded = json.encodeToString(schedule)
        val decoded = json.decodeFromString<Map<String, Map<String, Double>>>(encoded)

        assertEquals(3, decoded.size)
        assertEquals(8.0, decoded["2026-9"]?.get("1"))
        assertEquals(10.0, decoded["2026-9"]?.get("2"))
        assertEquals(12.0, decoded["2026-9"]?.get("3"))
        assertEquals(8.0, decoded["2027-1"]?.get("1"))
    }

    @Test
    fun testSequentialMonthNavigationRollOver() {
        var year = 2026
        var month = 12

        // Increment past December -> January of next year
        if (month < 12) {
            month += 1
        } else {
            month = 1
            year += 1
        }
        assertEquals(2027, year)
        assertEquals(1, month)

        // Decrement past January -> December of previous year
        if (month > 1) {
            month -= 1
        } else {
            month = 12
            year -= 1
        }
        assertEquals(2026, year)
        assertEquals(12, month)
    }

    @Test
    fun testZeroStateEarningsCalculation() {
        val daysWorked = 0
        val totalHours = 0.0
        val hourlyRate = 15.0

        val estimatedGross = if (daysWorked == 0 || totalHours == 0.0) 0.0 else totalHours * hourlyRate
        assertEquals(0.0, estimatedGross, 0.001)

        val report = TaxCalculator.calculateTax(
            grossPay = 0.0,
            taxCode = "1257L",
            isMonthly = true,
            region = TaxRegion.UK_STANDARD,
            taxYear = TaxYear.YEAR_2024_2025,
            pensionRatePercent = 5.0,
            studentLoanPlan = StudentLoanPlan.NONE,
            hasMarriageAllowance = false,
            hasBlindPersonsAllowance = false
        )

        assertEquals(0.0, report.grossPay, 0.001)
        assertEquals(0.0, report.netPay, 0.001)
        assertEquals(0.0, report.incomeTax, 0.001)
        assertEquals(0.0, report.nationalInsurance, 0.001)
    }

    @Test
    fun testSandboxScenarioTakeHomeCalculation() {
        // Baseline: 20 days @ 8h/day @ £15/h = £2,400 gross
        val baselineGross = 20.0 * 8.0 * 15.0
        val baseline = TaxCalculator.calculateTax(
            grossPay = baselineGross,
            taxCode = "1257L",
            isMonthly = true,
            region = TaxRegion.UK_STANDARD,
            taxYear = TaxYear.YEAR_2024_2025,
            pensionRatePercent = 5.0,
            studentLoanPlan = StudentLoanPlan.NONE,
            hasMarriageAllowance = false,
            hasBlindPersonsAllowance = false
        )

        // Sandbox Scenario: 20 days + 16h Overtime (1.5x) + £500 Bonus
        // Extra gross: (16 * 15 * 1.5) + 500 = 360 + 500 = £860 extra gross -> Total £3,260
        val scenarioGross = baselineGross + (16.0 * 15.0 * 1.5) + 500.0
        val scenario = TaxCalculator.calculateTax(
            grossPay = scenarioGross,
            taxCode = "1257L",
            isMonthly = true,
            region = TaxRegion.UK_STANDARD,
            taxYear = TaxYear.YEAR_2024_2025,
            pensionRatePercent = 5.0,
            studentLoanPlan = StudentLoanPlan.NONE,
            hasMarriageAllowance = false,
            hasBlindPersonsAllowance = false
        )

        assertEquals(2400.0, baseline.grossPay, 0.01)
        assertEquals(3260.0, scenario.grossPay, 0.01)
        assertTrue(scenario.netPay > baseline.netPay)
        assertTrue(scenario.incomeTax > baseline.incomeTax)
    }
}
