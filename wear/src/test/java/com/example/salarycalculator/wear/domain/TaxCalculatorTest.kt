package com.example.salarycalculator.wear.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class TaxCalculatorTest {

    @Test
    fun testParseTaxFreeAllowance_1257L() {
        val monthlyAllowance = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = true)
        assertEquals(1047.50, monthlyAllowance, 0.01)

        val augMonth5Allowance = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = true, taxMonth = 5)
        assertEquals(1048.42, augMonth5Allowance, 0.01)
    }

    @Test
    fun testAugust2026CareWorkerPayslipCalibration_16Shifts_1282Rate() {
        // 16 shifts * 12h = 192h @ £12.82/hr = £2,461.44 Gross
        val gross = 192.0 * 12.82
        assertEquals(2461.44, gross, 0.01)

        val report = TaxCalculator.calculateTax(
            grossPay = gross,
            taxCode = "1257L",
            isMonthly = true,
            region = TaxRegion.UK_STANDARD,
            totalHours = 192.0,
            shiftCount = 16,
            taxMonth = 5 // August = Month 5
        )

        // Allowance = £1,048.42 -> Taxable = £1,413.02 -> Tax @ 20% = £282.60
        assertEquals(1413.02, report.taxablePay, 0.01)
        assertEquals(282.60, report.incomeTax, 0.01)
        assertEquals(113.07, report.nationalInsurance, 0.01)
        assertEquals(2065.77, report.netPay, 0.01)
    }

    @Test
    fun testPayDayAndCutoffCalculation() {
        // October 2026: Last Friday is 30 Oct 2026, Cutoff Sunday is 25 Oct 2026
        val (octPayDay, octCutoffDay) = TaxCalculator.getPayDayAndCutoff(YearMonth.of(2026, 10))
        assertEquals(30, octPayDay)
        assertEquals(25, octCutoffDay)

        // August 2026: Last Friday is 28 Aug 2026, Cutoff Sunday is 23 Aug 2026
        val (augPayDay, augCutoffDay) = TaxCalculator.getPayDayAndCutoff(YearMonth.of(2026, 8))
        assertEquals(28, augPayDay)
        assertEquals(23, augCutoffDay)
    }

    @Test
    fun testDefault2026FlatShiftsSeeded() {
        val shifts = WearSalaryRepository.DEFAULT_2026_FLAT_SHIFTS
        assertTrue(shifts.isNotEmpty())
        assertEquals(12.0, shifts["2026-10-01"] ?: 0.0, 0.01)
        assertEquals(-12.0, shifts["2026-10-21"] ?: 0.0, 0.01)
        assertEquals(TaxCalculator.ANNUAL_LEAVE_SENTINEL, shifts["2026-10-25"] ?: 0.0, 0.01)
    }
}
