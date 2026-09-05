package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min

/**
 * // CRITICAL: TAX_ENGINE
 * High Income Child Benefit Charge (HICBC) Calculation & Pension Salary Sacrifice Engine.
 *
 * Statutory UK 2024/2025 and 2025/2026 Rules:
 * - Weekly Child Benefit:
 *   - Eldest / Only child: £25.60/week (£1,331.20/year)
 *   - Additional children: £16.95/week (£881.40/year each)
 * - Reformed HICBC Taper Band (April 2024 onward):
 *   - Lower Threshold: £60,000 Adjusted Net Income (ANI)
 *   - Upper Threshold: £80,000 Adjusted Net Income (ANI)
 *   - Taper rate: 1% of Child Benefit charged per £200 of ANI above £60,000
 *   - 100% clawback reached at £80,000+
 * - Pension Salary Sacrifice Remedy:
 *   - Sacrificing income above £60,000 into a pension reduces ANI to £60k,
 *     eliminating the HICBC charge and restoring 100% Child Benefit.
 */
data class HicbcResult(
    val annualGrossIncome: Double,
    val numberOfChildren: Int,
    val annualChildBenefitReceived: Double,
    val monthlyChildBenefitReceived: Double,
    val isTaperApplicable: Boolean,
    val clawbackPercentage: Double, // 0.0 to 100.0
    val annualTaxCharge: Double,
    val monthlyTaxCharge: Double,
    val netChildBenefitRetained: Double,
    val isFullyClawedBack: Boolean,
    // Pension Sacrifice Remedy
    val recommendedPensionSacrifice: Double,
    val restoredChildBenefitAnnual: Double,
    val taxReliefOnSacrifice: Double,
    val netCostOfSacrifice: Double,
    val effectiveRoiPercentage: Double
)

object HicbcEngine {

    const val WEEKLY_RATE_FIRST_CHILD = 25.60
    const val WEEKLY_RATE_SUBSEQUENT_CHILD = 16.95
    const val WEEKS_PER_YEAR = 52.0

    const val HICBC_LOWER_THRESHOLD = 60000.0
    const val HICBC_UPPER_THRESHOLD = 80000.0
    const val HICBC_TAPER_STEP = 200.0 // 1% per £200

    /**
     * Calculates annual Child Benefit and HICBC tax charge.
     */
    fun calculateHicbc(
        annualGrossIncome: Double,
        numberOfChildren: Int,
        existingPensionSacrifice: Double = 0.0
    ): HicbcResult {
        val safeChildren = max(0, numberOfChildren)
        val adjustedNetIncome = max(0.0, annualGrossIncome - existingPensionSacrifice)

        // 1. Calculate Gross Child Benefit Entitlement
        val annualBenefit = if (safeChildren <= 0) {
            0.0
        } else {
            val firstChild = WEEKLY_RATE_FIRST_CHILD * WEEKS_PER_YEAR
            val additionalChildren = (safeChildren - 1) * WEEKLY_RATE_SUBSEQUENT_CHILD * WEEKS_PER_YEAR
            firstChild + additionalChildren
        }
        val monthlyBenefit = annualBenefit / 12.0

        // 2. Calculate HICBC Clawback
        val isTaperApplicable = adjustedNetIncome > HICBC_LOWER_THRESHOLD && safeChildren > 0
        val clawbackPercentage = if (!isTaperApplicable) {
            0.0
        } else if (adjustedNetIncome >= HICBC_UPPER_THRESHOLD) {
            100.0
        } else {
            val excessIncome = adjustedNetIncome - HICBC_LOWER_THRESHOLD
            // 1% per £200, rounded down per HMRC statutory rule
            min(100.0, (excessIncome / HICBC_TAPER_STEP).toInt().toDouble())
        }

        val annualTaxCharge = annualBenefit * (clawbackPercentage / 100.0)
        val monthlyTaxCharge = annualTaxCharge / 12.0
        val netBenefitRetained = max(0.0, annualBenefit - annualTaxCharge)
        val isFullyClawedBack = clawbackPercentage >= 100.0

        // 3. Pension Salary Sacrifice Remedy
        val excessOverThreshold = max(0.0, adjustedNetIncome - HICBC_LOWER_THRESHOLD)
        val recommendedSacrifice = min(excessOverThreshold, adjustedNetIncome)
        val restoredBenefit = annualTaxCharge

        // At £60k - £80k, standard UK marginal rate is 40% Income Tax + 2% NI = 42%
        val taxReliefRate = 0.42
        val taxReliefOnSacrifice = recommendedSacrifice * taxReliefRate
        val netCostOfSacrifice = max(0.0, recommendedSacrifice - taxReliefOnSacrifice - restoredBenefit)

        val totalValueGain = recommendedSacrifice + restoredBenefit // Pension asset added + cash restored
        val effectiveRoi = if (recommendedSacrifice > 0.0) {
            val outOfPocketCash = recommendedSacrifice * (1.0 - taxReliefRate)
            if (outOfPocketCash > 0) {
                ((totalValueGain - outOfPocketCash) / outOfPocketCash) * 100.0
            } else 100.0
        } else 0.0

        return HicbcResult(
            annualGrossIncome = annualGrossIncome,
            numberOfChildren = safeChildren,
            annualChildBenefitReceived = annualBenefit,
            monthlyChildBenefitReceived = monthlyBenefit,
            isTaperApplicable = isTaperApplicable,
            clawbackPercentage = clawbackPercentage,
            annualTaxCharge = annualTaxCharge,
            monthlyTaxCharge = monthlyTaxCharge,
            netChildBenefitRetained = netBenefitRetained,
            isFullyClawedBack = isFullyClawedBack,
            recommendedPensionSacrifice = recommendedSacrifice,
            restoredChildBenefitAnnual = restoredBenefit,
            taxReliefOnSacrifice = taxReliefOnSacrifice,
            netCostOfSacrifice = netCostOfSacrifice,
            effectiveRoiPercentage = effectiveRoi
        )
    }
}
