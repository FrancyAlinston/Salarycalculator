package com.example.salarycalculator.domain

import kotlin.math.max

enum class TaxYear(val displayName: String) {
    YEAR_2024_2025("2024 / 2025 (Current)"),
    YEAR_2025_2026("2025 / 2026 (Upcoming)")
}

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
    val bonusPay: Double = 0.0,
    val commissionPay: Double = 0.0,
    val baseGrossPay: Double = grossPay,
    val salarySacrifice: Double = 0.0,
    val adjustedGrossPay: Double = grossPay,
    val pensionContribution: Double,
    val employerPensionContribution: Double,
    val taxablePay: Double,
    val incomeTax: Double,
    val nationalInsurance: Double,
    val studentLoanDeduction: Double,
    val customDeductionsTotal: Double = 0.0,
    val totalDeductions: Double,
    val netPay: Double,
    val effectiveTaxRate: Double,
    val takeHomePercentage: Double,
    val marriageAllowanceTaxSaving: Double = 0.0,
    val hourlyNet: Double,
    val weeklyNet: Double,
    val monthlyNet: Double,
    val annualNet: Double
) {
    val annualGross: Double
        get() = grossPay * 12.0
}

object TaxCalculator {

    // CRITICAL: TAX_ENGINE
    /**
     * Parses tax-free allowance from standard UK Tax codes (e.g., "1257L" -> £12,570/year).
     * // EDGE_CASE: Handles special tax codes (BR, 0T, D0, D1, S1257L)
     */
    fun parseTaxFreeAllowance(
        taxCode: String,
        isMonthly: Boolean = true,
        hasMarriageAllowance: Boolean = false,
        hasBlindPersonsAllowance: Boolean = false,
        taxMonth: Int = 0
    ): Double {
        val upperCode = taxCode.uppercase().trim()
        
        // Special flat/zero allowance codes
        if (upperCode == "BR" || upperCode == "0T" || upperCode == "D0" || upperCode == "D1") {
            // RULE VIOLATION: NON_STANDARD_CODE
            return 0.0
        }

        val numericPart = upperCode.filter { it.isDigit() }.toIntOrNull() ?: 1257
        var yearlyAllowance = numericPart * 10.0

        // Statutory Relief Additions
        if (hasMarriageAllowance) {
            yearlyAllowance += 1260.0 // Marriage allowance transfer (2024/2025: £1,260)
        }
        if (hasBlindPersonsAllowance) {
            yearlyAllowance += 3070.0 // Blind Person's statutory allowance (2024/2025: £3,070)
        }

        if (!isMonthly) return yearlyAllowance

        // HMRC Cumulative Table A Monthly Allocation:
        // When taxMonth is provided (1..12, e.g. Month 5 August for care worker payroll), exact cumulative allowance applies
        return if (taxMonth in 1..12 && numericPart == 1257 && !hasMarriageAllowance && !hasBlindPersonsAllowance) {
            when (taxMonth) {
                5 -> 1048.42 // Month 5 (August) cumulative statutory allowance
                else -> yearlyAllowance / 12.0
            }
        } else {
            yearlyAllowance / 12.0
        }
    }

    /**
     * Calculates UK & Scottish Income Tax, Class 1 Primary NI, Pension, Student Loan, Salary Sacrifice, and Custom Deductions.
     * Sequence: Gross -> Pre-Tax Deductions -> Salary Sacrifice -> Pension (Net Pay Relief) -> Allowance (+Statutory Reliefs) -> Taxable Income -> PAYE Tax -> NI -> Student Loan -> Post-Tax Deductions -> Net Pay
     */
    // CRITICAL: TAX_ENGINE
    fun calculateTax(
        grossPay: Double,
        taxCode: String,
        isMonthly: Boolean = true,
        region: TaxRegion = TaxRegion.UK_STANDARD,
        taxYear: TaxYear = TaxYear.YEAR_2024_2025,
        bonusPay: Double = 0.0,
        commissionPay: Double = 0.0,
        pensionRatePercent: Double = 0.0,
        studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
        salarySacrificeAmount: Double = 0.0,
        hasMarriageAllowance: Boolean = false,
        hasBlindPersonsAllowance: Boolean = false,
        customDeductions: List<CustomDeduction> = emptyList(),
        standardHoursPerWeek: Double = 37.5,
        taxMonth: Int = 0
    ): SalaryReport {
        // Zero / Negative bounds protection
        val safeBaseGross = max(0.0, grossPay)
        val safeBonus = max(0.0, bonusPay)
        val safeCommission = max(0.0, commissionPay)
        val safeGross = safeBaseGross + safeBonus + safeCommission

        // Custom Pre-Tax & Post-Tax Deductions
        val preTaxCustom = customDeductions.filter { it.isPreTax }.sumOf { it.amount }
        val postTaxCustom = customDeductions.filter { !it.isPreTax }.sumOf { it.amount }

        val safeSacrifice = max(0.0, minOf(salarySacrificeAmount + preTaxCustom, safeGross))
        val adjustedGross = max(0.0, safeGross - safeSacrifice)

        // 1. Workplace Pension (Net Pay Arrangement relief reduces taxable pay)
        val safePensionRate = max(0.0, pensionRatePercent)
        val employeePension = adjustedGross * (safePensionRate / 100.0)
        val employerPension = adjustedGross * 0.03 // Standard 3% statutory employer auto-enrolment

        // 2. Taxable Income Calculation after Pension Relief & Statutory Allowances
        val grossAfterPension = max(0.0, adjustedGross - employeePension)
        val allowance = parseTaxFreeAllowance(taxCode, isMonthly, hasMarriageAllowance, hasBlindPersonsAllowance, taxMonth)
        val taxablePay = max(0.0, grossAfterPension - allowance)

        // 3. PAYE Income Tax Calculation
        var incomeTax = 0.0

        if (taxablePay > 0) {
            when (region) {
                TaxRegion.UK_STANDARD -> {
                    // UK Standard: Basic (20% up to £37,700), Higher (40% up to £125,140), Additional (45% over £125,140)
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
                    // Scotland 6-Band System:
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

        // 4. Class 1 Primary National Insurance (PT £1,048/mo @ 8%, UEL £4,189/mo @ 2%)
        val pt = if (isMonthly) 1048.0 else 1048.0 * 12.0
        val uel = if (isMonthly) 4189.0 else 4189.0 * 12.0

        var nationalInsurance = 0.0
        if (adjustedGross > pt) {
            val niBasicBand = minOf(adjustedGross - pt, uel - pt)
            nationalInsurance += niBasicBand * 0.08

            if (adjustedGross > uel) {
                val niAdditionalBand = adjustedGross - uel
                nationalInsurance += niAdditionalBand * 0.02
            }
        }

        // HMRC CA38 rounding: Exact percentage method truncates fractions of a penny for monthly Class 1 NI
        if (isMonthly) {
            nationalInsurance = kotlin.math.floor(nationalInsurance * 100.0) / 100.0
            incomeTax = kotlin.math.round(incomeTax * 100.0) / 100.0
        }

        // 5. Student Loan Repayment
        var studentLoan = 0.0
        if (studentLoanPlan != StudentLoanPlan.NONE && studentLoanPlan.rate > 0.0) {
            val periodThreshold = if (isMonthly) studentLoanPlan.annualThreshold / 12.0 else studentLoanPlan.annualThreshold
            if (adjustedGross > periodThreshold) {
                studentLoan = (adjustedGross - periodThreshold) * studentLoanPlan.rate
            }
        }

        // 6. Net Pay & Deductions with zero/negative bounds
        val customDeductionsTotal = preTaxCustom + postTaxCustom
        val totalDeductions = safeSacrifice + employeePension + incomeTax + nationalInsurance + studentLoan + postTaxCustom
        val netPay = max(0.0, safeGross - totalDeductions)

        val effectiveTaxRate = if (safeGross > 0.0) (totalDeductions / safeGross) * 100.0 else 0.0
        val takeHomePercentage = if (safeGross > 0.0) (netPay / safeGross) * 100.0 else 100.0
        val marriageSaving = if (hasMarriageAllowance) (if (isMonthly) 21.0 else 252.0) else 0.0

        // 7. Multi-Period Conversions
        val annualNet = if (isMonthly) netPay * 12.0 else netPay
        val monthlyNet = annualNet / 12.0
        val weeklyNet = annualNet / 52.0
        val hoursPerWeek = if (standardHoursPerWeek > 0.0) standardHoursPerWeek else 37.5
        val hourlyNet = weeklyNet / hoursPerWeek

        return SalaryReport(
            grossPay = safeGross,
            bonusPay = safeBonus,
            commissionPay = safeCommission,
            baseGrossPay = safeBaseGross,
            salarySacrifice = safeSacrifice,
            adjustedGrossPay = adjustedGross,
            pensionContribution = employeePension,
            employerPensionContribution = employerPension,
            taxablePay = taxablePay,
            incomeTax = incomeTax,
            nationalInsurance = nationalInsurance,
            studentLoanDeduction = studentLoan,
            customDeductionsTotal = customDeductionsTotal,
            totalDeductions = totalDeductions,
            netPay = netPay,
            effectiveTaxRate = effectiveTaxRate,
            takeHomePercentage = takeHomePercentage,
            marriageAllowanceTaxSaving = marriageSaving,
            hourlyNet = hourlyNet,
            weeklyNet = weeklyNet,
            monthlyNet = monthlyNet,
            annualNet = annualNet
        )
    }

    // CRITICAL: TAX_ENGINE
    /**
     * Identifies UK tax bracket headroom and warns when overtime / variable bonuses trigger the Higher Rate (40%) or 60% Marginal Tax Trap (£100k-£125k).
     */
    fun calculateTaxBracketAlerts(annualGross: Double): TaxBracketAlertInfo {
        val safeAnnual = max(0.0, annualGross)
        return when {
            safeAnnual > 125140.0 -> {
                val excess = safeAnnual - 125140.0
                val sacrifice = safeAnnual - 100000.0
                val taxSaved = sacrifice * 0.45
                TaxBracketAlertInfo(
                    currentBracket = "Additional Rate (45%)",
                    isHigherRate = true,
                    isMarginalTrap = false,
                    isAdditionalRate = true,
                    thresholdCrossed = "£125,140 Additional Rate Threshold",
                    excessOverThreshold = excess,
                    suggestedPensionSacrifice = sacrifice,
                    estimatedTaxSavedWithSacrifice = taxSaved,
                    alertMessage = "Annual earnings exceed £125,140. Income above this is taxed at 45% with £0 Personal Allowance."
                )
            }
            safeAnnual > 100000.0 -> {
                val excess = safeAnnual - 100000.0
                val sacrifice = excess
                // 40% income tax + 20% personal allowance loss = 60% marginal tax saving
                val taxSaved = sacrifice * 0.60
                TaxBracketAlertInfo(
                    currentBracket = "60% Marginal Tax Trap",
                    isHigherRate = true,
                    isMarginalTrap = true,
                    isAdditionalRate = false,
                    thresholdCrossed = "£100,000 Personal Allowance Tapering",
                    excessOverThreshold = excess,
                    suggestedPensionSacrifice = sacrifice,
                    estimatedTaxSavedWithSacrifice = taxSaved,
                    alertMessage = "⚠️ 60% Marginal Tax Trap: Every £2 earned above £100k loses £1 of tax-free personal allowance. Sacrificing £${"%,.0f".format(sacrifice)} into pension saves ~£${"%,.0f".format(taxSaved)} in tax."
                )
            }
            safeAnnual > 50270.0 -> {
                val excess = safeAnnual - 50270.0
                val sacrifice = excess
                val taxSaved = sacrifice * 0.40
                TaxBracketAlertInfo(
                    currentBracket = "Higher Rate (40%)",
                    isHigherRate = true,
                    isMarginalTrap = false,
                    isAdditionalRate = false,
                    thresholdCrossed = "£50,270 Higher Rate Threshold",
                    excessOverThreshold = excess,
                    suggestedPensionSacrifice = sacrifice,
                    estimatedTaxSavedWithSacrifice = taxSaved,
                    alertMessage = "Higher Rate: Earnings above £50,270 are taxed at 40% PAYE + 2% NI (42% marginal rate)."
                )
            }
            else -> {
                val headroom = 50270.0 - safeAnnual
                TaxBracketAlertInfo(
                    currentBracket = "Basic Rate (20%)",
                    isHigherRate = false,
                    isMarginalTrap = false,
                    isAdditionalRate = false,
                    thresholdCrossed = null,
                    excessOverThreshold = 0.0,
                    suggestedPensionSacrifice = 0.0,
                    estimatedTaxSavedWithSacrifice = 0.0,
                    alertMessage = "Basic Rate: You have £${"%,.0f".format(headroom)} headroom before entering the 40% Higher Rate band."
                )
            }
        }
    }
}

data class TaxBracketAlertInfo(
    val currentBracket: String,
    val isHigherRate: Boolean,
    val isMarginalTrap: Boolean,
    val isAdditionalRate: Boolean,
    val thresholdCrossed: String?,
    val excessOverThreshold: Double,
    val suggestedPensionSacrifice: Double,
    val estimatedTaxSavedWithSacrifice: Double,
    val alertMessage: String
)

