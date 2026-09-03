package com.example.salarycalculator.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCalculatorTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun parseTaxFreeAllowance_standardCode_returnsCorrectValues() {
        val monthly = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = true)
        val yearly = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = false)

        assertEquals(1047.50, monthly, 0.01)
        assertEquals(12570.0, yearly, 0.01)
    }

    @Test
    fun parseTaxFreeAllowance_customAndSpecialCodes_returnsCorrectValues() {
        // Custom 1100L
        val customMonthly = TaxCalculator.parseTaxFreeAllowance("1100L", isMonthly = true)
        assertEquals(11000.0 / 12, customMonthly, 0.01)

        // Flat / Zero tax codes
        assertEquals(0.0, TaxCalculator.parseTaxFreeAllowance("BR", isMonthly = true), 0.01)
        assertEquals(0.0, TaxCalculator.parseTaxFreeAllowance("0T", isMonthly = true), 0.01)
        assertEquals(0.0, TaxCalculator.parseTaxFreeAllowance("D0", isMonthly = true), 0.01)
    }

    @Test
    fun calculateTax_zeroOrNegativeGross_returnsZeroValues() {
        val zeroReport = TaxCalculator.calculateTax(0.0, "1257L", isMonthly = true)
        assertEquals(0.0, zeroReport.grossPay, 0.01)
        assertEquals(0.0, zeroReport.taxablePay, 0.01)
        assertEquals(0.0, zeroReport.incomeTax, 0.01)
        assertEquals(0.0, zeroReport.nationalInsurance, 0.01)
        assertEquals(0.0, zeroReport.netPay, 0.01)

        val negativeReport = TaxCalculator.calculateTax(-500.0, "1257L", isMonthly = true)
        assertEquals(0.0, negativeReport.grossPay, 0.01)
        assertEquals(0.0, negativeReport.taxablePay, 0.01)
        assertEquals(0.0, negativeReport.incomeTax, 0.01)
        assertEquals(0.0, negativeReport.nationalInsurance, 0.01)
        assertEquals(0.0, negativeReport.netPay, 0.01)
    }

    @Test
    fun calculateTax_underAllowance_paysNoIncomeTaxOrNI() {
        val report = TaxCalculator.calculateTax(1000.0, "1257L", isMonthly = true)

        assertEquals(1000.0, report.grossPay, 0.01)
        assertEquals(0.0, report.taxablePay, 0.01)
        assertEquals(0.0, report.incomeTax, 0.01)
        assertEquals(0.0, report.nationalInsurance, 0.01)
        assertEquals(1000.0, report.netPay, 0.01)
    }

    @Test
    fun calculateTax_basicRateBand_correctTaxAndNI() {
        val report = TaxCalculator.calculateTax(2500.0, "1257L", isMonthly = true)

        assertEquals(2500.0, report.grossPay, 0.01)
        assertEquals(1452.50, report.taxablePay, 0.01)
        assertEquals(290.50, report.incomeTax, 0.01)
        assertEquals(116.16, report.nationalInsurance, 0.01)
        assertEquals(2093.34, report.netPay, 0.01)
    }

    @Test
    fun calculateTax_higherRateBand_correctTaxAndNI() {
        val report = TaxCalculator.calculateTax(5000.0, "1257L", isMonthly = true)

        assertEquals(5000.0, report.grossPay, 0.01)
        assertEquals(3952.50, report.taxablePay, 0.01)
        assertEquals(952.67, report.incomeTax, 0.05)
        assertEquals(267.50, report.nationalInsurance, 0.01)
        assertTrue(report.netPay > 0.0)
    }

    @Test
    fun calculateTax_pensionContribution_providesTaxRelief() {
        val withoutPension = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, pensionRatePercent = 0.0)
        val withPension = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, pensionRatePercent = 5.0)

        assertEquals(150.0, withPension.pensionContribution, 0.01)
        assertEquals(90.0, withPension.employerPensionContribution, 0.01)
        assertEquals(1802.50, withPension.taxablePay, 0.01)
        assertEquals(360.50, withPension.incomeTax, 0.01)
        assertTrue(withPension.incomeTax < withoutPension.incomeTax)
    }

    @Test
    fun calculateTax_salarySacrifice_reducesTaxAndNI() {
        val withoutSacrifice = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, salarySacrificeAmount = 0.0)
        val withSacrifice = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, salarySacrificeAmount = 200.0)

        assertEquals(200.0, withSacrifice.salarySacrifice, 0.01)
        assertEquals(2800.0, withSacrifice.adjustedGrossPay, 0.01)
        assertTrue(withSacrifice.incomeTax < withoutSacrifice.incomeTax)
        assertTrue(withSacrifice.nationalInsurance < withoutSacrifice.nationalInsurance)
    }

    @Test
    fun calculateTax_studentLoanPlan2_correctDeduction() {
        val report = TaxCalculator.calculateTax(
            2916.67,
            "1257L",
            isMonthly = true,
            studentLoanPlan = StudentLoanPlan.PLAN_2
        )

        assertEquals(57.79, report.studentLoanDeduction, 0.10)
        assertEquals(2916.67 - report.incomeTax - report.nationalInsurance - report.studentLoanDeduction, report.netPay, 0.01)
    }

    @Test
    fun calculateTax_scotlandRegion_appliesScottishBands() {
        val ukReport = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, region = TaxRegion.UK_STANDARD)
        val scotlandReport = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, region = TaxRegion.SCOTLAND)

        assertEquals(396.44, scotlandReport.incomeTax, 0.50)
        assertTrue(scotlandReport.incomeTax > ukReport.incomeTax)
    }

    @Test
    fun calculateTax_periodConversions_areConsistent() {
        val report = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true)

        assertEquals(report.netPay * 12.0, report.annualNet, 0.01)
        assertEquals(report.annualNet / 52.0, report.weeklyNet, 0.01)
        assertEquals(report.weeklyNet / 37.5, report.hourlyNet, 0.01)
    }

    @Test
    fun monthlySalaryRecord_serialization_isLossless() {
        val record = MonthlySalaryRecord(
            monthYear = "September 2026",
            daysWorked = 20.0,
            hoursPerDay = 8.0,
            overtimeHours = 5.0,
            overtimeMultiplier = 1.5,
            hourlyRate = 15.0,
            grossPay = 2512.50,
            salarySacrifice = 100.0,
            pensionRate = 5.0,
            pensionContribution = 120.63,
            employerPension = 72.38,
            taxablePay = 1245.37,
            incomeTax = 249.07,
            nationalInsurance = 109.16,
            studentLoanPlan = StudentLoanPlan.PLAN_2,
            studentLoanDeduction = 21.41,
            totalDeductions = 600.27,
            netPay = 1912.23,
            taxCode = "1257L",
            taxRegion = TaxRegion.UK_STANDARD,
            note = "September paycheck"
        )

        val encoded = json.encodeToString(listOf(record))
        val decoded = json.decodeFromString<List<MonthlySalaryRecord>>(encoded)

        assertEquals(1, decoded.size)
        val first = decoded[0]
        assertEquals("September 2026", first.monthYear)
        assertEquals(1912.23, first.netPay, 0.01)
        assertEquals(2512.50, first.grossPay, 0.01)
        assertEquals(100.0, first.salarySacrifice, 0.01)
        assertEquals(StudentLoanPlan.PLAN_2, first.studentLoanPlan)
        assertEquals("September paycheck", first.note)
    }
}
