package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min

// CRITICAL: TAX_ENGINE
data class TaxRefundResult(
    val monthlyGross: Double,
    val monthsOnOldCode: Int,
    val oldTaxCode: String,
    val newTaxCode: String,
    val cumulativeGross: Double,
    val taxPaidUnderOldCode: Double,
    val cumulativeTaxDueUnderNewCode: Double,
    val immediatePaycheckRefund: Double,
    val oldMonthlyTakeHome: Double,
    val newMonthlyTakeHome: Double,
    val ongoingMonthlyIncrease: Double,
    val explanation: String
)

object TaxRefundEstimator {

    /**
     * Estimates PAYE tax refund and take-home adjustments when switching
     * from an emergency/previous tax code (e.g. BR, 0T) to a standard/higher code (e.g. 1257L, 1383M).
     */
    fun estimate(
        monthlyGross: Double,
        monthsOnOldCode: Int,
        oldTaxCode: String = "BR",
        newTaxCode: String = "1257L",
        pensionRatePercent: Double = 5.0,
        region: TaxRegion = TaxRegion.UK_STANDARD
    ): TaxRefundResult {
        val months = monthsOnOldCode.coerceIn(1, 12)
        val cumulativeGross = monthlyGross * months
        val monthlyPension = monthlyGross * (pensionRatePercent / 100.0)
        val cumulativePension = monthlyPension * months

        // Old tax code calculation
        val oldMonthlyReport = TaxCalculator.calculateTax(
            grossPay = monthlyGross,
            taxCode = oldTaxCode,
            isMonthly = true,
            region = region,
            pensionRatePercent = pensionRatePercent
        )
        val taxPaidSoFar = oldMonthlyReport.incomeTax * months

        // Cumulative tax calculation under new code for 'months' periods
        val newAnnualAllowance = TaxCalculator.parseTaxFreeAllowance(
            taxCode = newTaxCode,
            isMonthly = false
        )
        val cumulativeAllowance = (newAnnualAllowance / 12.0) * months
        val cumulativeTaxableIncome = max(0.0, cumulativeGross - cumulativePension - cumulativeAllowance)

        val cumulativeTaxDue = computeCumulativeTax(cumulativeTaxableIncome, months, region)

        // The tax due in month M under cumulative operation
        val taxDueThisMonth = cumulativeTaxDue - (oldMonthlyReport.incomeTax * (months - 1))
        val normalNewMonthTax = TaxCalculator.calculateTax(
            grossPay = monthlyGross,
            taxCode = newTaxCode,
            isMonthly = true,
            region = region,
            pensionRatePercent = pensionRatePercent
        ).incomeTax

        // Immediate refund = tax overpaid in previous months (months - 1)
        val overpaidInPreviousMonths = max(0.0, (oldMonthlyReport.incomeTax * (months - 1)) - (cumulativeTaxDue - normalNewMonthTax))
        val immediateRefund = if (taxPaidSoFar > cumulativeTaxDue) taxPaidSoFar - cumulativeTaxDue else 0.0

        val newMonthlyReport = TaxCalculator.calculateTax(
            grossPay = monthlyGross,
            taxCode = newTaxCode,
            isMonthly = true,
            region = region,
            pensionRatePercent = pensionRatePercent
        )

        val monthlyDiff = max(0.0, newMonthlyReport.netPay - oldMonthlyReport.netPay)

        val expl = if (immediateRefund > 0.0) {
            "Switching from $oldTaxCode to $newTaxCode in Month $months recovers cumulative tax-free personal allowance for the past $months months, giving you an immediate estimated refund of £${"%,.2f".format(immediateRefund)} on your next payslip."
        } else {
            "Your tax code $newTaxCode is now correctly aligned. Your ongoing monthly take-home increases by £${"%,.2f".format(monthlyDiff)}."
        }

        return TaxRefundResult(
            monthlyGross = monthlyGross,
            monthsOnOldCode = months,
            oldTaxCode = oldTaxCode,
            newTaxCode = newTaxCode,
            cumulativeGross = cumulativeGross,
            taxPaidUnderOldCode = taxPaidSoFar,
            cumulativeTaxDueUnderNewCode = cumulativeTaxDue,
            immediatePaycheckRefund = immediateRefund,
            oldMonthlyTakeHome = oldMonthlyReport.netPay,
            newMonthlyTakeHome = newMonthlyReport.netPay,
            ongoingMonthlyIncrease = monthlyDiff,
            explanation = expl
        )
    }

    private fun computeCumulativeTax(cumulativeTaxable: Double, months: Int, region: TaxRegion): Double {
        if (cumulativeTaxable <= 0.0) return 0.0
        val proRatedBasicBand = (37700.0 / 12.0) * months
        val proRatedHigherBand = (125140.0 / 12.0) * months

        val basicTax = min(cumulativeTaxable, proRatedBasicBand) * 0.20
        val higherTax = if (cumulativeTaxable > proRatedBasicBand) {
            min(cumulativeTaxable - proRatedBasicBand, proRatedHigherBand - proRatedBasicBand) * 0.40
        } else 0.0
        val additionalTax = if (cumulativeTaxable > proRatedHigherBand) {
            (cumulativeTaxable - proRatedHigherBand) * 0.45
        } else 0.0

        return basicTax + higherTax + additionalTax
    }
}
