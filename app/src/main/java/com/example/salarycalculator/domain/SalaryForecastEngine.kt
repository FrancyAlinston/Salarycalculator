package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Data structures for ML-based salary forecasting and year-end tax liability projections.
 */
data class MonthlyForecastPoint(
    val monthIndex: Int, // 1 to 12
    val monthLabel: String,
    val isHistorical: Boolean,
    val grossPay: Double,
    val projectedTax: Double,
    val projectedNi: Double,
    val projectedNet: Double
)

data class ForecastTaxAnalysis(
    val historicalMonthsCount: Int,
    val remainingMonthsCount: Int,
    val rSquaredConfidence: Double, // 0.0 to 1.0 (model goodness of fit)
    val monthlyTrendSlope: Double, // +£/month or -£/month
    val trendDirection: String, // "Growing", "Stable", "Volatile", "Decreasing"
    val ytdGrossLogged: Double,
    val ytdTaxLogged: Double,
    val ytdNetLogged: Double,
    val projectedAnnualGross: Double,
    val projectedAnnualTaxLiability: Double,
    val projectedAnnualNiLiability: Double,
    val projectedAnnualNet: Double,
    val estimatedPayeRebateOrDebt: Double, // >0 is refund due, <0 is underpayment
    val effectiveTaxRatePercent: Double,
    val forecastTimeline: List<MonthlyForecastPoint>,
    val optimizationSummary: String,
    val keyRecommendations: List<String>
)

object SalaryForecastEngine {

    private val MONTH_NAMES = listOf(
        "Apr", "May", "Jun", "Jul", "Aug", "Sep",
        "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"
    )

    // CRITICAL: TAX_ENGINE
    /**
     * Executes ordinary least squares (OLS) regression and time-series projection across payroll history.
     */
    fun computeSalaryForecast(
        history: List<MonthlySalaryRecord>,
        currentHourlyRate: Double = 0.0,
        currentHoursPerWeek: Double = 37.5,
        taxCode: String = "1257L",
        taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
        studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
        pensionRate: Double = 5.0
    ): ForecastTaxAnalysis {
        val count = history.size
        val historySorted = history.sortedBy { it.monthYear }

        // Baseline fallback monthly gross from active calculator if no history
        val fallbackMonthlyGross = if (currentHourlyRate > 0) {
            (currentHourlyRate * currentHoursPerWeek * 52.0) / 12.0
        } else {
            2500.0 // Default baseline £30k/yr
        }

        val n = count.toDouble()
        val trendSlope: Double
        val intercept: Double
        val rSquared: Double

        if (count >= 2) {
            // Ordinary Least Squares Linear Regression
            var sumX = 0.0
            var sumY = 0.0
            var sumXY = 0.0
            var sumX2 = 0.0
            var sumY2 = 0.0

            for (i in 0 until count) {
                val x = (i + 1).toDouble()
                val y = historySorted[i].grossPay
                sumX += x
                sumY += y
                sumXY += x * y
                sumX2 += x * x
                sumY2 += y * y
            }

            val denominator = (n * sumX2 - sumX * sumX)
            trendSlope = if (denominator != 0.0) (n * sumXY - sumX * sumY) / denominator else 0.0
            intercept = (sumY - trendSlope * sumX) / n

            // R^2 calculation
            val meanY = sumY / n
            var ssTot = 0.0
            var ssRes = 0.0
            for (i in 0 until count) {
                val x = (i + 1).toDouble()
                val y = historySorted[i].grossPay
                val yPred = intercept + trendSlope * x
                ssTot += (y - meanY).pow(2)
                ssRes += (y - yPred).pow(2)
            }
            rSquared = if (ssTot > 0.0) max(0.0, min(1.0, 1.0 - (ssRes / ssTot))) else 1.0
        } else if (count == 1) {
            trendSlope = 0.0
            intercept = historySorted[0].grossPay
            rSquared = 0.85
        } else {
            trendSlope = 0.0
            intercept = fallbackMonthlyGross
            rSquared = 0.70
        }

        val trendDirection = when {
            count < 2 -> "Baseline Projection"
            trendSlope > 50.0 -> "Strong Growth (+£${"%,.0f".format(trendSlope)}/mo)"
            trendSlope > 10.0 -> "Moderate Upward Trend (+£${"%,.0f".format(trendSlope)}/mo)"
            trendSlope < -50.0 -> "Decreasing Earnings (-£${"%,.0f".format(-trendSlope)}/mo)"
            trendSlope < -10.0 -> "Slight Downward Drift (-£${"%,.0f".format(-trendSlope)}/mo)"
            else -> "Stable Earnings (±£${"%,.0f".format(trendSlope)}/mo)"
        }

        // Build 12-month fiscal timeline (April through March)
        val forecastTimeline = mutableListOf<MonthlyForecastPoint>()
        var ytdGrossLogged = 0.0
        var ytdTaxLogged = 0.0
        var ytdNetLogged = 0.0

        val historicalMonthsCount = min(12, count)
        val remainingMonthsCount = max(0, 12 - historicalMonthsCount)

        for (m in 1..12) {
            val monthLabel = MONTH_NAMES[m - 1]
            if (m <= historicalMonthsCount) {
                val record = historySorted[m - 1]
                ytdGrossLogged += record.grossPay
                ytdTaxLogged += record.incomeTax
                ytdNetLogged += record.netPay
                forecastTimeline.add(
                    MonthlyForecastPoint(
                        monthIndex = m,
                        monthLabel = monthLabel,
                        isHistorical = true,
                        grossPay = record.grossPay,
                        projectedTax = record.incomeTax,
                        projectedNi = record.nationalInsurance,
                        projectedNet = record.netPay
                    )
                )
            } else {
                // Predict gross for future month using regression slope + seasonal holiday bonus boost
                val rawPred = intercept + trendSlope * m.toDouble()
                val seasonalBoost = if (monthLabel == "Dec") 1.10 else 1.0 // 10% holiday seasonality
                val projectedGross = max(500.0, rawPred * seasonalBoost)

                val monthlyCalc = TaxCalculator.calculateTax(
                    grossPay = projectedGross,
                    taxCode = taxCode,
                    isMonthly = true,
                    region = taxRegion,
                    pensionRatePercent = pensionRate,
                    studentLoanPlan = studentLoanPlan
                )

                forecastTimeline.add(
                    MonthlyForecastPoint(
                        monthIndex = m,
                        monthLabel = monthLabel,
                        isHistorical = false,
                        grossPay = projectedGross,
                        projectedTax = monthlyCalc.incomeTax,
                        projectedNi = monthlyCalc.nationalInsurance,
                        projectedNet = monthlyCalc.netPay
                    )
                )
            }
        }

        val projectedAnnualGross = forecastTimeline.sumOf { it.grossPay }

        // Run full-year statutory reconciliation
        val annualStatutoryReport = TaxCalculator.calculateTax(
            grossPay = projectedAnnualGross,
            taxCode = taxCode,
            isMonthly = false,
            region = taxRegion,
            pensionRatePercent = pensionRate,
            studentLoanPlan = studentLoanPlan
        )

        val totalExpectedTaxDeductions = forecastTimeline.sumOf { it.projectedTax }
        val rebateOrUnderpayment = totalExpectedTaxDeductions - annualStatutoryReport.incomeTax
        val effectiveTaxRate = if (projectedAnnualGross > 0) {
            (annualStatutoryReport.incomeTax + annualStatutoryReport.nationalInsurance) / projectedAnnualGross * 100.0
        } else 0.0

        val recommendations = mutableListOf<String>()

        if (projectedAnnualGross > 100000.0 && projectedAnnualGross <= 125140.0) {
            val excess = projectedAnnualGross - 100000.0
            recommendations.add(
                "🚨 60% Marginal Tax Trap: Contributing £${"%,.0f".format(excess)} into a pension recovers £${"%,.2f".format(excess * 0.60)} in lost personal allowance and tax relief."
            )
        } else if (projectedAnnualGross > 50270.0 && projectedAnnualGross <= 60000.0) {
            recommendations.add(
                "💡 Higher Rate Entry: £${"%,.0f".format(projectedAnnualGross - 50270.0)} is taxed at 40%. Consider salary sacrifice to remain in the 20% basic rate band."
            )
        }

        if (rebateOrUnderpayment > 15.0) {
            recommendations.add(
                "💰 Tax Rebate Expected: Overpaid PAYE during variable months indicates an estimated £${"%,.2f".format(rebateOrUnderpayment)} refund from HMRC after year-end reconciliation."
            )
        } else if (rebateOrUnderpayment < -15.0) {
            recommendations.add(
                "⚠️ Underpayment Alert: Rapid wage/overtime increases suggest a potential £${"%,.2f".format(-rebateOrUnderpayment)} tax shortfall by April 5."
            )
        } else {
            recommendations.add(
                "✅ PAYE Alignment: Monthly tax deductions are tracking within ±£15 of annual statutory liability."
            )
        }

        val summaryText = if (historicalMonthsCount > 0) {
            "Trained on $historicalMonthsCount logged payslips (Model Confidence: ${"%.0f".format(rSquared * 100)}%, Trend: $trendDirection). Projected full-year earnings: £${"%,.2f".format(projectedAnnualGross)}."
        } else {
            "Simulated projection based on active hourly rate and standard 52-week scheduling. Model Confidence: ${"%.0f".format(rSquared * 100)}%."
        }

        return ForecastTaxAnalysis(
            historicalMonthsCount = historicalMonthsCount,
            remainingMonthsCount = remainingMonthsCount,
            rSquaredConfidence = rSquared,
            monthlyTrendSlope = trendSlope,
            trendDirection = trendDirection,
            ytdGrossLogged = ytdGrossLogged,
            ytdTaxLogged = ytdTaxLogged,
            ytdNetLogged = ytdNetLogged,
            projectedAnnualGross = projectedAnnualGross,
            projectedAnnualTaxLiability = annualStatutoryReport.incomeTax,
            projectedAnnualNiLiability = annualStatutoryReport.nationalInsurance,
            projectedAnnualNet = annualStatutoryReport.netPay,
            estimatedPayeRebateOrDebt = rebateOrUnderpayment,
            effectiveTaxRatePercent = effectiveTaxRate,
            forecastTimeline = forecastTimeline,
            optimizationSummary = summaryText,
            keyRecommendations = recommendations
        )
    }
}
