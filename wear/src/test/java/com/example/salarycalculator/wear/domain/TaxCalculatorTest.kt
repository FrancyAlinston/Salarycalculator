package com.example.salarycalculator.wear.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCalculatorTest {

    @Test
    fun testParseTaxFreeAllowance_1257L() {
        val monthlyAllowance = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = true)
        assertEquals(1047.50, monthlyAllowance, 0.01)
    }

    @Test
    fun testCareWorkerSalary_16Shifts_192Hours() {
        // 16 shifts * 12h = 192h @ £16.25/hr = £3,120.00 Gross
        val gross = 192.0 * 16.25
        assertEquals(3120.00, gross, 0.01)

        val report = TaxCalculator.calculateTax(
            grossPay = gross,
            taxCode = "1257L",
            isMonthly = true,
            region = TaxRegion.UK_STANDARD,
            totalHours = 192.0,
            shiftCount = 16
        )

        // Allowance = £1,047.50
        // Taxable = £3,120.00 - £1,047.50 = £2,072.50
        // Income Tax @ 20% = £414.50
        assertEquals(2072.50, report.taxablePay, 0.01)
        assertEquals(414.50, report.incomeTax, 0.01)

        // NI: Threshold = £1,048.00. Taxable NI = £3,120 - £1,048 = £2,072.00 @ 8% = £165.76
        assertEquals(165.76, report.nationalInsurance, 0.01)

        // Net Pay = £3,120.00 - (£414.50 + £165.76) = £2,539.74
        assertEquals(2539.74, report.netPay, 0.01)
        assertEquals(192.0, report.totalHours, 0.01)
        assertEquals(16, report.shiftCount)
    }

    @Test
    fun testScottishTaxCalculation() {
        val report = TaxCalculator.calculateTax(
            grossPay = 3000.0,
            taxCode = "1257L",
            isMonthly = true,
            region = TaxRegion.SCOTLAND
        )
        assertTrue(report.incomeTax > 0.0)
        assertTrue(report.netPay > 0.0)
    }
}
