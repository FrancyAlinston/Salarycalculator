package com.example.salarycalculator.domain

import kotlin.math.max

/**
 * Data structures and calculation engine for Year-to-Date (YTD) cumulative tax auditing,
 * full-year tax liability forecasting, and March year-end pension top-up optimization.
 */

data class YtdSummary(
    val taxYearLabel: String,
    val monthsLogged: Int,
    val monthsRemaining: Int,
    val ytdGross: Double,
    val ytdTaxPaid: Double,
    val ytdNiPaid: Double,
    val ytdPensionPaid: Double,
    val ytdStudentLoanPaid: Double,
    val ytdNetPay: Double,
    val avgMonthlyGross: Double,
    val projectedAnnualGross: Double,
    val projectedAnnualTaxLiability: Double,
    val projectedAnnualNiLiability: Double,
    val projectedAnnualNet: Double,
    val projectedTaxUnderOverPayment: Double, // Positive = overpayment refund expected, Negative = underpayment risk
    val statusMessage: String,
    val pensionOptimization: PensionTopUpRecommendation
)

data class PensionTopUpRecommendation(
    val recommendedTopUpAmount: Double,
    val targetThresholdName: String,
    val taxReliefRatePercent: Double,
    val immediateTaxSavings: Double,
    val netCostToWorker: Double,
    val explanation: String
)

object YtdTaxProjector {

    // CRITICAL: TAX_ENGINE
    /**
     * Projects cumulative YTD progress and computes year-end tax forecasts.
     */
    fun computeYtdProjection(
        history: List<MonthlySalaryRecord>,
        currentMonthlyGross: Double = 0.0,
        taxCode: String = "1257L",
        taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
        taxYearLabel: String = "2024/2025"
    ): YtdSummary {
        val count = history.size
        val monthsLogged = if (count > 0) minOf(12, count) else if (currentMonthlyGross > 0) 1 else 0
        val monthsRemaining = max(0, 12 - monthsLogged)

        val ytdGross = if (count > 0) history.take(12).sumOf { it.grossPay } else currentMonthlyGross
        val ytdTax = if (count > 0) history.take(12).sumOf { it.incomeTax } else 0.0
        val ytdNi = if (count > 0) history.take(12).sumOf { it.nationalInsurance } else 0.0
        val ytdPension = if (count > 0) history.take(12).sumOf { it.pensionContribution } else 0.0
        val ytdStudentLoan = if (count > 0) history.take(12).sumOf { it.studentLoanDeduction } else 0.0
        val ytdNet = if (count > 0) history.take(12).sumOf { it.netPay } else 0.0

        val avgMonthlyGross = if (monthsLogged > 0) ytdGross / monthsLogged else currentMonthlyGross
        val baselineMonthlyForRemaining = if (currentMonthlyGross > 0) currentMonthlyGross else avgMonthlyGross

        val projectedAnnualGross = ytdGross + (monthsRemaining * baselineMonthlyForRemaining)

        // Run full-year statutory calculation for annual projection
        val annualReport = TaxCalculator.calculateTax(
            grossPay = projectedAnnualGross,
            taxCode = taxCode,
            isMonthly = false,
            region = taxRegion
        )

        // Estimated remaining monthly tax under standard PAYE
        val remainingMonthlyTax = if (monthsRemaining > 0) {
            TaxCalculator.calculateTax(
                grossPay = baselineMonthlyForRemaining,
                taxCode = taxCode,
                isMonthly = true,
                region = taxRegion
            ).incomeTax
        } else 0.0

        val totalExpectedTaxPaid = ytdTax + (monthsRemaining * remainingMonthlyTax)
        val projectedVariance = totalExpectedTaxPaid - annualReport.incomeTax

        val statusMsg = when {
            monthsLogged == 0 -> "No payroll records logged yet. Enter your salary details to generate annual tax forecasts."
            Math.abs(projectedVariance) <= 5.0 -> "Your cumulative PAYE deductions are on track with HMRC annual liability."
            projectedVariance > 5.0 -> "Estimated tax refund of £${"%,.2f".format(projectedVariance)} expected due to overpaid PAYE tax earlier in the tax year."
            else -> "Potential underpayment risk of £${"%,.2f".format(Math.abs(projectedVariance))} by April 5 (check overtime or mid-year salary increments)."
        }

        // Compute March Year-End Pension Top-Up Recommendation
        val pensionRec = evaluatePensionTopUp(projectedAnnualGross, taxRegion)

        return YtdSummary(
            taxYearLabel = taxYearLabel,
            monthsLogged = monthsLogged,
            monthsRemaining = monthsRemaining,
            ytdGross = ytdGross,
            ytdTaxPaid = ytdTax,
            ytdNiPaid = ytdNi,
            ytdPensionPaid = ytdPension,
            ytdStudentLoanPaid = ytdStudentLoan,
            ytdNetPay = ytdNet,
            avgMonthlyGross = avgMonthlyGross,
            projectedAnnualGross = projectedAnnualGross,
            projectedAnnualTaxLiability = annualReport.incomeTax,
            projectedAnnualNiLiability = annualReport.nationalInsurance,
            projectedAnnualNet = annualReport.netPay,
            projectedTaxUnderOverPayment = projectedVariance,
            statusMessage = statusMsg,
            pensionOptimization = pensionRec
        )
    }

    /**
     * Evaluates SIPP / workplace pension top-up opportunities before April 5 tax year end.
     */
    private fun evaluatePensionTopUp(annualGross: Double, taxRegion: TaxRegion): PensionTopUpRecommendation {
        return when {
            // Case 1: 60% Marginal Tax Trap (£100k - £125,140)
            annualGross > 100000.0 && annualGross <= 125140.0 -> {
                val excess = annualGross - 100000.0
                val taxSavings = excess * 0.60 // 40% income tax + 20% personal allowance clawback
                val netCost = excess * 0.40
                PensionTopUpRecommendation(
                    recommendedTopUpAmount = excess,
                    targetThresholdName = "£100k 60% Tax Trap Barrier",
                    taxReliefRatePercent = 60.0,
                    immediateTaxSavings = taxSavings,
                    netCostToWorker = netCost,
                    explanation = "Contributing £${"%,.0f".format(excess)} into a pension before April 5 eliminates the 60% marginal tax trap, fully restoring your £12,570 tax-free personal allowance and saving £${"%,.2f".format(taxSavings)} in tax for only £${"%,.2f".format(netCost)} out of pocket."
                )
            }
            // Case 2: Above Additional Rate Threshold (£125,140+)
            annualGross > 125140.0 -> {
                val excess = minOf(40000.0, annualGross - 100000.0)
                val rate = if (taxRegion == TaxRegion.SCOTLAND) 48.0 else 45.0
                val taxSavings = excess * (rate / 100.0)
                val netCost = excess - taxSavings
                PensionTopUpRecommendation(
                    recommendedTopUpAmount = excess,
                    targetThresholdName = "Top-Rate Tax Mitigation",
                    taxReliefRatePercent = rate,
                    immediateTaxSavings = taxSavings,
                    netCostToWorker = netCost,
                    explanation = "A pension contribution of £${"%,.0f".format(excess)} attracts ${"%.0f".format(rate)}% tax relief, saving £${"%,.2f".format(taxSavings)} in PAYE income tax."
                )
            }
            // Case 3: Higher Rate Threshold (£50,270 - £100,000)
            annualGross > 50270.0 -> {
                val excess = minOf(15000.0, annualGross - 50270.0)
                val rate = if (taxRegion == TaxRegion.SCOTLAND) 42.0 else 40.0
                val taxSavings = excess * (rate / 100.0)
                val netCost = excess - taxSavings
                PensionTopUpRecommendation(
                    recommendedTopUpAmount = excess,
                    targetThresholdName = "40% Higher Rate Threshold (£50,270)",
                    taxReliefRatePercent = rate,
                    immediateTaxSavings = taxSavings,
                    netCostToWorker = netCost,
                    explanation = "Contributing £${"%,.0f".format(excess)} to your pension reduces income taxed at ${"%.0f".format(rate)}%, delivering an immediate tax saving of £${"%,.2f".format(taxSavings)}."
                )
            }
            // Case 4: Basic Rate Earner
            else -> {
                val suggested = 2000.0
                val taxSavings = suggested * 0.20
                val netCost = suggested * 0.80
                PensionTopUpRecommendation(
                    recommendedTopUpAmount = suggested,
                    targetThresholdName = "20% Basic Rate Relief & Compound Growth",
                    taxReliefRatePercent = 20.0,
                    immediateTaxSavings = taxSavings,
                    netCostToWorker = netCost,
                    explanation = "Contributing £${"%,.0f".format(suggested)} into a pension generates £${"%,.2f".format(taxSavings)} in statutory government tax top-ups directly into your pension pot."
                )
            }
        }
    }
}
