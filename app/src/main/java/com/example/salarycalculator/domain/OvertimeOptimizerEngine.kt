package com.example.salarycalculator.domain

import kotlin.math.max

enum class OvertimeEfficiencyRating(val label: String, val description: String, val badgeColorHex: String) {
    HIGH("High Efficiency (65%+ Retained)", "You keep the vast majority of your overtime earnings with minimal tax drag.", "#10B981"),
    MODERATE("Standard Efficiency (50%–65% Retained)", "Typical basic-to-higher rate retention. Modest tax & NI impact.", "#F59E0B"),
    LOW("High Tax Drag (< 50% Retained)", "Over 50% of your extra overtime earnings is absorbed by PAYE, NI, or student loan deductions.", "#EF4444")
}

data class OvertimeScenarioResult(
    val extraHours: Double,
    val multiplier: Double,
    val extraGross: Double,
    val extraTax: Double,
    val extraNi: Double,
    val extraStudentLoan: Double,
    val extraPension: Double,
    val extraTotalDeductions: Double,
    val extraNetPay: Double,
    val netPerHour: Double,
    val retentionPercentage: Double,
    val marginalDeductionPercentage: Double,
    val efficiencyRating: OvertimeEfficiencyRating,
    val baselineReport: SalaryReport,
    val overtimeReport: SalaryReport,
    val taxTrapWarning: String? = null
)

// CRITICAL: TAX_ENGINE
object OvertimeOptimizerEngine {

    /**
     * Calculates the exact marginal penny return for additional overtime hours worked.
     */
    fun calculateOvertimeReturn(
        baseGrossMonthly: Double,
        baseHourlyRate: Double,
        extraOtHours: Double,
        overtimeMultiplier: Double = 1.5,
        taxCode: String = "1257L",
        taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
        taxYear: TaxYear = TaxYear.YEAR_2024_2025,
        pensionRate: Double = 5.0,
        studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
        hasMarriageAllowance: Boolean = false,
        hasBlindPersonsAllowance: Boolean = false
    ): OvertimeScenarioResult {
        val safeBaseGross = max(0.0, baseGrossMonthly)
        val safeExtraHours = max(0.0, extraOtHours)
        val safeMultiplier = max(1.0, overtimeMultiplier)

        val baselineReport = TaxCalculator.calculateTax(
            grossPay = safeBaseGross,
            taxCode = taxCode,
            isMonthly = true,
            region = taxRegion,
            taxYear = taxYear,
            pensionRatePercent = pensionRate,
            studentLoanPlan = studentLoanPlan,
            hasMarriageAllowance = hasMarriageAllowance,
            hasBlindPersonsAllowance = hasBlindPersonsAllowance
        )

        val extraGross = safeExtraHours * baseHourlyRate * safeMultiplier
        val scenarioGross = safeBaseGross + extraGross

        val overtimeReport = TaxCalculator.calculateTax(
            grossPay = scenarioGross,
            taxCode = taxCode,
            isMonthly = true,
            region = taxRegion,
            taxYear = taxYear,
            pensionRatePercent = pensionRate,
            studentLoanPlan = studentLoanPlan,
            hasMarriageAllowance = hasMarriageAllowance,
            hasBlindPersonsAllowance = hasBlindPersonsAllowance
        )

        val extraTax = max(0.0, overtimeReport.incomeTax - baselineReport.incomeTax)
        val extraNi = max(0.0, overtimeReport.nationalInsurance - baselineReport.nationalInsurance)
        val extraStudentLoan = max(0.0, overtimeReport.studentLoanDeduction - baselineReport.studentLoanDeduction)
        val extraPension = max(0.0, overtimeReport.pensionContribution - baselineReport.pensionContribution)
        val extraTotalDeductions = max(0.0, overtimeReport.totalDeductions - baselineReport.totalDeductions)
        val extraNetPay = max(0.0, overtimeReport.netPay - baselineReport.netPay)

        val retentionPercentage = if (extraGross > 0.0) (extraNetPay / extraGross) * 100.0 else 100.0
        val marginalDeductionPercentage = if (extraGross > 0.0) (extraTotalDeductions / extraGross) * 100.0 else 0.0
        val netPerHour = if (safeExtraHours > 0.0) extraNetPay / safeExtraHours else 0.0

        val efficiencyRating = when {
            retentionPercentage >= 65.0 -> OvertimeEfficiencyRating.HIGH
            retentionPercentage >= 50.0 -> OvertimeEfficiencyRating.MODERATE
            else -> OvertimeEfficiencyRating.LOW
        }

        // Check if OT pushed into 60% Marginal Tax Trap
        val baselineAnnual = safeBaseGross * 12.0
        val scenarioAnnual = scenarioGross * 12.0
        val taxTrapWarning = if (scenarioAnnual > 100000.0 && baselineAnnual <= 100000.0) {
            "⚠️ Overtime pushed annual income into the £100k–£125k 60% Marginal Tax Trap zone (Personal Allowance Tapering)."
        } else if (scenarioAnnual in 100000.0..125140.0) {
            "⚠️ Overtime is taxed at an effective 60%+ marginal rate due to personal allowance tapering (£1 lost per £2 earned)."
        } else null

        return OvertimeScenarioResult(
            extraHours = safeExtraHours,
            multiplier = safeMultiplier,
            extraGross = extraGross,
            extraTax = extraTax,
            extraNi = extraNi,
            extraStudentLoan = extraStudentLoan,
            extraPension = extraPension,
            extraTotalDeductions = extraTotalDeductions,
            extraNetPay = extraNetPay,
            netPerHour = netPerHour,
            retentionPercentage = retentionPercentage,
            marginalDeductionPercentage = marginalDeductionPercentage,
            efficiencyRating = efficiencyRating,
            baselineReport = baselineReport,
            overtimeReport = overtimeReport,
            taxTrapWarning = taxTrapWarning
        )
    }
}
