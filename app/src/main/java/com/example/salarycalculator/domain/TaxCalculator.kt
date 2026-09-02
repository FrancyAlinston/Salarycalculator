package com.example.salarycalculator.domain

import kotlin.math.max

enum class TaxRegion(val displayName: String) {
    UK_STANDARD("England, Wales & NI"),
    SCOTLAND("Scotland (6-Tier System)")
}

enum class StudentLoanPlan(val displayName: String, val annualThreshold: Double, val rate: Double) {
    NONE("No Student Loan", 0.0, 0.0),
    PLAN_1("Plan 1 (£24,990 / 9%)", 24990.0, 0.09),
    PLAN_2("Plan 2 (£27,295 / 9%)", 27295.0, 0.09),
    PLAN_4("Plan 4 Scottish (£31,395 / 9%)", 31395.0, 0.09),
    POSTGRADUATE("Postgraduate (£21,000 / 6%)", 21000.0, 0.06)
}

enum class PayFrequency(val displayName: String) {
    MONTHLY("Monthly"),
    WEEKLY("Weekly"),
    ANNUAL("Annual"),
    HOURLY("Hourly")
}

data class SalaryReport(
    val grossPay: Double,
    val pensionContribution: Double,
    val employerPensionContribution: Double,
    val taxablePay: Double,
    val incomeTax: Double,
    val nationalInsurance: Double,
    val studentLoanDeduction: Double,
    val totalDeductions: Double,
    val netPay: Double,
    val effectiveTaxRate: Double,
    val takeHomePercentage: Double,
    val hourlyNet: Double,
    val weeklyNet: Double,
    val monthlyNet: Double,
    val annualNet: Double
)

object TaxCalculator {

    // CRITICAL: TAX_ENGINE
    /**
     * Parses tax-free allowance from standard UK Tax codes (e.g., "1257L" -> £12,570/year).
     * // EDGE_CASE: Handles special tax codes (BR, 0T, D0, D1, S1257L)
     */
    fun parseTaxFreeAllowance(taxCode: String, isMonthly: Boolean = true): Double {
        val upperCode = taxCode.uppercase().trim()
        
        // Special flat/zero allowance codes
        if (upperCode == "BR" || upperCode == "0T" || upperCode == "D0" || upperCode == "D1") {
            // RULE VIOLATION: NON_STANDARD_CODE
            return 0.0
        }

        val numericPart = upperCode.filter { it.isDigit() }.toIntOrNull() ?: 1257
        val yearlyAllowance = numericPart * 10.0
        return if (isMonthly) yearlyAllowance / 12.0 else yearlyAllowance
    }

    /**
     * Calculates UK & Scottish Income Tax, Class 1 Primary NI, Pension, and Student Loan.
     * Uses UK Standard and Scotland 2024/2025 statutory tax bands.
     * Sequence: Hours/Overtime -> Gross Pay -> Pension (Net Pay Relief) -> Allowance -> Taxable Income -> PAYE Tax -> NI -> Student Loan -> Net Pay
     */
    // CRITICAL: TAX_ENGINE
    fun calculateTax(
        grossPay: Double,
        taxCode: String,
        isMonthly: Boolean = true,
        region: TaxRegion = TaxRegion.UK_STANDARD,
        pensionRatePercent: Double = 0.0,
        studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
        standardHoursPerWeek: Double = 37.5
    ): SalaryReport {
        // Zero / Negative bounds protection
        val safeGross = max(0.0, grossPay)

        // 1. Workplace Pension (Net Pay Arrangement relief reduces taxable pay)
        val safePensionRate = max(0.0, pensionRatePercent)
        val employeePension = safeGross * (safePensionRate / 100.0)
        val employerPension = safeGross * 0.03 // Standard 3% statutory employer auto-enrolment

        // 2. Taxable Income Calculation after Pension Relief
        val grossAfterPension = max(0.0, safeGross - employeePension)
        val allowance = parseTaxFreeAllowance(taxCode, isMonthly)
        val taxablePay = max(0.0, grossAfterPension - allowance)

        // 3. PAYE Income Tax Calculation
        var incomeTax = 0.0

        if (taxablePay > 0) {
            when (region) {
                TaxRegion.UK_STANDARD -> {
                    // UK Standard 2024/2025: Basic (20% up to £37,700), Higher (40% up to £125,140), Additional (45% over £125,140)
                    val basicLimit = if (isMonthly) 37700.0 / 12.0 else 37700.0
                    val higherLimit = if (isMonthly) 125140.0 / 12.0 else 125140.0

                    val basicBand = minOf(taxablePay, basicLimit)
                    incomeTax += basicBand * 0.20

                    if (taxablePay > basicLimit) {
                        val higherBand = minOf(taxablePay - basicLimit, higherLimit - basicLimit)
                        incomeTax += higherBand * 0.40

                        if (taxablePay > higherLimit) {
                            val additionalBand = taxablePay - higherLimit
                            incomeTax += additionalBand * 0.45
                        }
                    }
                }
                TaxRegion.SCOTLAND -> {
                    // Scotland 2024/2025 6-Band System:
                    // Starter (19%): £0 to £2,306 (£192.17/mo)
                    // Basic (20%): £2,306 to £13,991 (£192.17 to £1,165.92/mo)
                    // Intermediate (21%): £13,991 to £31,092 (£1,165.92 to £2,591.00/mo)
                    // Higher (42%): £31,092 to £62,430 (£2,591.00 to £5,202.50/mo)
                    // Advanced (45%): £62,430 to £125,140 (£5,202.50 to £10,428.33/mo)
                    // Top (48%): Over £125,140 (> £10,428.33/mo)
                    val starterLimit = if (isMonthly) 2306.0 / 12.0 else 2306.0
                    val basicLimit = if (isMonthly) 13991.0 / 12.0 else 13991.0
                    val interLimit = if (isMonthly) 31092.0 / 12.0 else 31092.0
                    val higherLimit = if (isMonthly) 62430.0 / 12.0 else 62430.0
                    val advLimit = if (isMonthly) 125140.0 / 12.0 else 125140.0

                    val starterBand = minOf(taxablePay, starterLimit)
                    incomeTax += starterBand * 0.19

                    if (taxablePay > starterLimit) {
                        val basicBand = minOf(taxablePay - starterLimit, basicLimit - starterLimit)
                        incomeTax += basicBand * 0.20

                        if (taxablePay > basicLimit) {
                            val interBand = minOf(taxablePay - basicLimit, interLimit - basicLimit)
                            incomeTax += interBand * 0.21

                            if (taxablePay > interLimit) {
                                val higherBand = minOf(taxablePay - interLimit, higherLimit - interLimit)
                                incomeTax += higherBand * 0.42

                                if (taxablePay > higherLimit) {
                                    val advBand = minOf(taxablePay - higherLimit, advLimit - higherLimit)
                                    incomeTax += advBand * 0.45

                                    if (taxablePay > advLimit) {
                                        val topBand = taxablePay - advLimit
                                        incomeTax += topBand * 0.48
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Class 1 Primary National Insurance (2024/2025: PT £1,048/mo @ 8%, UEL £4,189/mo @ 2%)
        val pt = if (isMonthly) 1048.0 else 1048.0 * 12.0
        val uel = if (isMonthly) 4189.0 else 4189.0 * 12.0

        var nationalInsurance = 0.0
        if (safeGross > pt) {
            val niBasicBand = minOf(safeGross - pt, uel - pt)
            nationalInsurance += niBasicBand * 0.08

            if (safeGross > uel) {
                val niAdditionalBand = safeGross - uel
                nationalInsurance += niAdditionalBand * 0.02
            }
        }

        // 5. Student Loan Repayment
        var studentLoan = 0.0
        if (studentLoanPlan != StudentLoanPlan.NONE && studentLoanPlan.rate > 0.0) {
            val periodThreshold = if (isMonthly) studentLoanPlan.annualThreshold / 12.0 else studentLoanPlan.annualThreshold
            if (safeGross > periodThreshold) {
                studentLoan = (safeGross - periodThreshold) * studentLoanPlan.rate
            }
        }

        // 6. Net Pay & Deductions with zero/negative bounds
        val totalDeductions = employeePension + incomeTax + nationalInsurance + studentLoan
        val netPay = max(0.0, safeGross - totalDeductions)

        val effectiveTaxRate = if (safeGross > 0.0) (totalDeductions / safeGross) * 100.0 else 0.0
        val takeHomePercentage = if (safeGross > 0.0) (netPay / safeGross) * 100.0 else 100.0

        // 7. Multi-Period Conversions
        val annualNet = if (isMonthly) netPay * 12.0 else netPay
        val monthlyNet = annualNet / 12.0
        val weeklyNet = annualNet / 52.0
        val hoursPerWeek = if (standardHoursPerWeek > 0.0) standardHoursPerWeek else 37.5
        val hourlyNet = weeklyNet / hoursPerWeek

        return SalaryReport(
            grossPay = safeGross,
            pensionContribution = employeePension,
            employerPensionContribution = employerPension,
            taxablePay = taxablePay,
            incomeTax = incomeTax,
            nationalInsurance = nationalInsurance,
            studentLoanDeduction = studentLoan,
            totalDeductions = totalDeductions,
            netPay = netPay,
            effectiveTaxRate = effectiveTaxRate,
            takeHomePercentage = takeHomePercentage,
            hourlyNet = hourlyNet,
            weeklyNet = weeklyNet,
            monthlyNet = monthlyNet,
            annualNet = annualNet
        )
    }
}
