package com.example.salarycalculator.wear.domain

import kotlin.math.max
import kotlin.math.min

enum class TaxYear(val displayName: String) {
    YEAR_2024_2025("2024 / 2025"),
    YEAR_2025_2026("2025 / 2026")
}

enum class TaxRegion(val displayName: String) {
    UK_STANDARD("England, Wales & NI"),
    SCOTLAND("Scotland (6-Tier)")
}

enum class StudentLoanPlan(val displayName: String, val annualThreshold: Double, val rate: Double) {
    NONE("No Student Loan", 0.0, 0.0),
    PLAN_1("Plan 1 (£24,990 / 9%)", 24990.0, 0.09),
    PLAN_2("Plan 2 (£27,295 / 9%)", 27295.0, 0.09),
    PLAN_4("Plan 4 Scottish (£31,395 / 9%)", 31395.0, 0.09),
    POSTGRADUATE("Postgraduate (£21,000 / 6%)", 21000.0, 0.06)
}

data class SalaryReport(
    val grossPay: Double,
    val bonusPay: Double = 0.0,
    val pensionContribution: Double = 0.0,
    val taxablePay: Double = 0.0,
    val incomeTax: Double = 0.0,
    val nationalInsurance: Double = 0.0,
    val studentLoanDeduction: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val netPay: Double = 0.0,
    val totalHours: Double = 0.0,
    val shiftCount: Int = 0
)

object TaxCalculator {

    // CRITICAL: TAX_ENGINE
    fun parseTaxFreeAllowance(
        taxCode: String,
        isMonthly: Boolean = true
    ): Double {
        val upperCode = taxCode.uppercase().trim()
        if (upperCode in listOf("BR", "0T", "D0", "D1")) {
            return 0.0
        }
        val numericPart = upperCode.filter { it.isDigit() }.toIntOrNull() ?: 1257
        val yearlyAllowance = numericPart * 10.0
        return if (isMonthly) yearlyAllowance / 12.0 else yearlyAllowance
    }

    // CRITICAL: TAX_ENGINE
    fun calculateTax(
        grossPay: Double,
        taxCode: String = "1257L",
        isMonthly: Boolean = true,
        region: TaxRegion = TaxRegion.UK_STANDARD,
        taxYear: TaxYear = TaxYear.YEAR_2024_2025,
        pensionRatePercent: Double = 0.0,
        studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
        totalHours: Double = 0.0,
        shiftCount: Int = 0
    ): SalaryReport {
        val safeGross = max(0.0, grossPay)
        val periodDivisor = if (isMonthly) 12.0 else 52.0

        // 1. Workplace Pension (Net Pay Arrangement relief reduces taxable pay)
        val pensionAmount = max(0.0, safeGross * (pensionRatePercent / 100.0))
        val postPensionGross = max(0.0, safeGross - pensionAmount)

        // 2. Personal Allowance
        val allowance = parseTaxFreeAllowance(taxCode, isMonthly)
        val taxablePay = max(0.0, postPensionGross - allowance)

        // 3. PAYE Income Tax
        val tax = if (region == TaxRegion.SCOTLAND) {
            calculateScottishTax(taxablePay, periodDivisor)
        } else {
            calculateUkStandardTax(taxablePay, periodDivisor)
        }

        // 4. National Insurance (Class 1 Primary)
        val ni = calculateClass1NI(safeGross, periodDivisor)

        // 5. Student Loan
        val studentLoanDeduction = if (studentLoanPlan != StudentLoanPlan.NONE && safeGross > (studentLoanPlan.annualThreshold / periodDivisor)) {
            val loanThreshold = studentLoanPlan.annualThreshold / periodDivisor
            (safeGross - loanThreshold) * studentLoanPlan.rate
        } else {
            0.0
        }

        val totalDeductions = pensionAmount + tax + ni + studentLoanDeduction
        val netPay = max(0.0, safeGross - totalDeductions)

        return SalaryReport(
            grossPay = safeGross,
            pensionContribution = pensionAmount,
            taxablePay = taxablePay,
            incomeTax = tax,
            nationalInsurance = ni,
            studentLoanDeduction = studentLoanDeduction,
            totalDeductions = totalDeductions,
            netPay = netPay,
            totalHours = totalHours,
            shiftCount = shiftCount
        )
    }

    private fun calculateUkStandardTax(taxable: Double, divisor: Double): Double {
        val basicBand = 37700.0 / divisor
        val higherBand = (125140.0 - 12570.0) / divisor

        var remaining = taxable
        var tax = 0.0

        if (remaining > 0.0) {
            val taxedAtBasic = min(remaining, basicBand)
            tax += taxedAtBasic * 0.20
            remaining -= taxedAtBasic
        }

        if (remaining > 0.0) {
            val taxedAtHigher = min(remaining, higherBand - basicBand)
            tax += taxedAtHigher * 0.40
            remaining -= taxedAtHigher
        }

        if (remaining > 0.0) {
            tax += remaining * 0.45
        }

        return max(0.0, tax)
    }

    private fun calculateScottishTax(taxable: Double, divisor: Double): Double {
        val bStarter = 2306.0 / divisor
        val bBasic = (13991.0 - 2306.0) / divisor
        val bInter = (31092.0 - 13991.0) / divisor
        val bHigher = (62430.0 - 31092.0) / divisor
        val bAdvanced = (125140.0 - 62430.0) / divisor

        var remaining = taxable
        var tax = 0.0

        val tStarter = min(remaining, bStarter); tax += tStarter * 0.19; remaining -= tStarter
        if (remaining > 0.0) { val tBasic = min(remaining, bBasic); tax += tBasic * 0.20; remaining -= tBasic }
        if (remaining > 0.0) { val tInter = min(remaining, bInter); tax += tInter * 0.21; remaining -= tInter }
        if (remaining > 0.0) { val tHigher = min(remaining, bHigher); tax += tHigher * 0.42; remaining -= tHigher }
        if (remaining > 0.0) { val tAdv = min(remaining, bAdvanced); tax += tAdv * 0.45; remaining -= tAdv }
        if (remaining > 0.0) { tax += remaining * 0.48 }

        return max(0.0, tax)
    }

    private fun calculateClass1NI(gross: Double, divisor: Double): Double {
        val pt = 1048.0 * (12.0 / divisor) // £1,048/month
        val uel = 4189.0 * (12.0 / divisor) // £4,189/month

        if (gross <= pt) return 0.0

        var ni = 0.0
        val mainEarnings = min(gross, uel) - pt
        if (mainEarnings > 0.0) {
            ni += mainEarnings * 0.08 // 8% main rate
        }

        if (gross > uel) {
            ni += (gross - uel) * 0.02 // 2% additional rate
        }

        return max(0.0, ni)
    }
}
