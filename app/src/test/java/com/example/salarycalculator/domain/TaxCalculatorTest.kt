package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCalculatorTest {

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
        // £3,000 monthly with 5% employee pension (£150):
        // Adjusted Gross for Tax = £2,850
        // Allowance = £1,047.50 => Taxable = £1,802.50
        // Tax at 20% = £360.50
        val withoutPension = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, pensionRatePercent = 0.0)
        val withPension = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, pensionRatePercent = 5.0)

        assertEquals(150.0, withPension.pensionContribution, 0.01)
        assertEquals(90.0, withPension.employerPensionContribution, 0.01)
        assertEquals(1802.50, withPension.taxablePay, 0.01)
        assertEquals(360.50, withPension.incomeTax, 0.01)
        // With pension relief, income tax is lower (£360.50 vs £390.50)
        assertTrue(withPension.incomeTax < withoutPension.incomeTax)
    }

    @Test
    fun calculateTax_studentLoanPlan2_correctDeduction() {
        // Annual £35,000 (£2,916.67/mo) under Plan 2 (Threshold £2,274.58/mo):
        // Over threshold = £2,916.67 - £2,274.58 = £642.09
        // Student Loan = £642.09 * 0.09 = £57.79/mo
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
        // Scotland £3,000/mo (£36,000/yr):
        // Allowance = £1,047.50 => Taxable = £1,952.50/mo (£23,430/yr)
        // Yearly Scottish Tax on £23,430 taxable:
        // Starter (19% on £2,306) = £438.14
        // Basic (20% on £11,685) = £2,337.00
        // Intermediate (21% on £9,439) = £1,982.19
        // Total Yearly Tax = £4,757.33 => Monthly = £396.44
        val ukReport = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, region = TaxRegion.UK_STANDARD)
        val scotlandReport = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, region = TaxRegion.SCOTLAND)

        assertEquals(396.44, scotlandReport.incomeTax, 0.50)
        // Scottish tax on £36k is slightly higher than UK standard (£390.50)
        assertTrue(scotlandReport.incomeTax > ukReport.incomeTax)
    }

    @Test
    fun calculateTax_periodConversions_areConsistent() {
        val report = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true)

        assertEquals(report.netPay * 12.0, report.annualNet, 0.01)
        assertEquals(report.annualNet / 52.0, report.weeklyNet, 0.01)
        assertEquals(report.weeklyNet / 37.5, report.hourlyNet, 0.01)
    }
}
