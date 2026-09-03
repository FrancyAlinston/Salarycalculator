package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min

// CRITICAL: TAX_ENGINE
data class TaxYearSummary(
    val yearLabel: String,
    val grossPay: Double,
    val taxablePay: Double,
    val incomeTax: Double,
    val nationalInsurance: Double,
    val pensionContribution: Double,
    val netPay: Double,
    val effectiveTaxRate: Double,
    val takeHomePercentage: Double,
    val annualSavingsVs2023: Double
)

data class MultiYearComparison(
    val grossPay: Double,
    val isMonthly: Boolean,
    val summaries: List<TaxYearSummary>,
    val niCutSavingsAnnual: Double,
    val bestYearLabel: String
)

object TaxYearComparisonCalculator {

    /**
     * Compares income tax, Class 1 National Insurance, and take-home pay
     * across 2023/2024, 2024/2025, and 2025/2026 statutory regimes.
     */
    fun compare(
        grossAmount: Double,
        taxCode: String = "1257L",
        isMonthly: Boolean = true,
        region: TaxRegion = TaxRegion.UK_STANDARD,
        pensionRatePercent: Double = 5.0,
        salarySacrifice: Double = 0.0
    ): MultiYearComparison {
        val annualGross = if (isMonthly) grossAmount * 12.0 else grossAmount
        val annualSacrifice = if (isMonthly) salarySacrifice * 12.0 else salarySacrifice
        val adjustedGross = max(0.0, annualGross - annualSacrifice)

        val annualPension = adjustedGross * (pensionRatePercent / 100.0)
        val taxablePay = max(0.0, adjustedGross - annualPension - 12570.0)

        // 1. 2023/2024 Regime (12% NI Main Rate, 20%/40%/45% Income Tax)
        val tax2023 = computeAnnualIncomeTax(taxablePay, region, is2025Scottish = false)
        val ni2023 = computeAnnualNi(adjustedGross, mainRate = 0.12)
        val net2023 = max(0.0, annualGross - annualPension - tax2023 - ni2023 - annualSacrifice)

        // 2. 2024/2025 Regime (8% NI Main Rate cut, 20%/40%/45% Income Tax)
        val tax2024 = computeAnnualIncomeTax(taxablePay, region, is2025Scottish = false)
        val ni2024 = computeAnnualNi(adjustedGross, mainRate = 0.08)
        val net2024 = max(0.0, annualGross - annualPension - tax2024 - ni2024 - annualSacrifice)

        // 3. 2025/2026 Regime (8% NI Main Rate, updated Scottish brackets)
        val tax2025 = computeAnnualIncomeTax(taxablePay, region, is2025Scottish = true)
        val ni2025 = computeAnnualNi(adjustedGross, mainRate = 0.08)
        val net2025 = max(0.0, annualGross - annualPension - tax2025 - ni2025 - annualSacrifice)

        fun createSummary(
            label: String,
            gross: Double,
            taxable: Double,
            tax: Double,
            ni: Double,
            pension: Double,
            net: Double,
            baselineNet: Double
        ): TaxYearSummary {
            val effRate = if (gross > 0) ((tax + ni) / gross) * 100.0 else 0.0
            val takeHomePct = if (gross > 0) (net / gross) * 100.0 else 0.0
            val savings = net - baselineNet
            return TaxYearSummary(
                yearLabel = label,
                grossPay = if (isMonthly) gross / 12.0 else gross,
                taxablePay = if (isMonthly) taxable / 12.0 else taxable,
                incomeTax = if (isMonthly) tax / 12.0 else tax,
                nationalInsurance = if (isMonthly) ni / 12.0 else ni,
                pensionContribution = if (isMonthly) pension / 12.0 else pension,
                netPay = if (isMonthly) net / 12.0 else net,
                effectiveTaxRate = effRate,
                takeHomePercentage = takeHomePct,
                annualSavingsVs2023 = savings
            )
        }

        val s2023 = createSummary("2023/2024", annualGross, taxablePay, tax2023, ni2023, annualPension, net2023, net2023)
        val s2024 = createSummary("2024/2025", annualGross, taxablePay, tax2024, ni2024, annualPension, net2024, net2023)
        val s2025 = createSummary("2025/2026", annualGross, taxablePay, tax2025, ni2025, annualPension, net2025, net2023)

        val summaries = listOf(s2023, s2024, s2025)
        val niSavingsAnnual = max(0.0, ni2023 - ni2024)
        val bestYear = summaries.maxByOrNull { it.netPay }?.yearLabel ?: "2024/2025"

        return MultiYearComparison(
            grossPay = grossAmount,
            isMonthly = isMonthly,
            summaries = summaries,
            niCutSavingsAnnual = niSavingsAnnual,
            bestYearLabel = bestYear
        )
    }

    private fun computeAnnualIncomeTax(taxableIncome: Double, region: TaxRegion, is2025Scottish: Boolean): Double {
        if (taxableIncome <= 0.0) return 0.0
        return when (region) {
            TaxRegion.UK_STANDARD -> {
                val basic = min(taxableIncome, 37700.0) * 0.20
                val higher = if (taxableIncome > 37700.0) {
                    min(taxableIncome - 37700.0, 125140.0 - 37700.0) * 0.40
                } else 0.0
                val additional = if (taxableIncome > 125140.0) {
                    (taxableIncome - 125140.0) * 0.45
                } else 0.0
                basic + higher + additional
            }
            TaxRegion.SCOTLAND -> {
                // Scottish 6-tier rates
                val starterLimit = 2306.0
                val basicLimit = 13991.0
                val interLimit = 31092.0
                val higherLimit = 62430.0
                val advLimit = 125140.0

                val tStarter = min(taxableIncome, starterLimit) * 0.19
                val tBasic = if (taxableIncome > starterLimit) min(taxableIncome - starterLimit, basicLimit - starterLimit) * 0.20 else 0.0
                val tInter = if (taxableIncome > basicLimit) min(taxableIncome - basicLimit, interLimit - basicLimit) * 0.21 else 0.0
                val tHigher = if (taxableIncome > interLimit) min(taxableIncome - interLimit, higherLimit - interLimit) * 0.42 else 0.0
                val tAdv = if (taxableIncome > higherLimit) min(taxableIncome - higherLimit, advLimit - higherLimit) * 0.45 else 0.0
                val tTop = if (taxableIncome > advLimit) (taxableIncome - advLimit) * 0.48 else 0.0
                tStarter + tBasic + tInter + tHigher + tAdv + tTop
            }
        }
    }

    private fun computeAnnualNi(gross: Double, mainRate: Double): Double {
        val primaryThreshold = 12570.0
        val upperEarningsLimit = 50270.0

        if (gross <= primaryThreshold) return 0.0

        val mainBand = min(gross, upperEarningsLimit) - primaryThreshold
        val mainNi = max(0.0, mainBand * mainRate)
        val upperNi = if (gross > upperEarningsLimit) (gross - upperEarningsLimit) * 0.02 else 0.0

        return mainNi + upperNi
    }
}
