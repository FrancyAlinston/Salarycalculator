package com.example.salarycalculator.domain

import kotlin.math.max

// CRITICAL: TAX_ENGINE
data class TaxFreeChildcareInput(
    val annualGrossIncome: Double,
    val existingPensionContributions: Double = 0.0,
    val existingGiftAidDonations: Double = 0.0,
    val eligibleChildrenCount: Int = 1, // Under 11
    val disabledChildrenCount: Int = 0, // Under 17
    val annualChildcareSpendPerChild: Double = 8000.0,
    val has30HoursFreeChildcare: Boolean = true,
    val averageChildcareHourlyCost: Double = 6.0 // Standard UK average hourly rate
)

data class TaxFreeChildcareResult(
    val adjustedNetIncome: Double,
    val isEligible: Boolean,
    val isCliffEdgeHit: Boolean,
    val excessOver100k: Double,
    
    // Annual Benefit Values
    val governmentTopUpAnnual: Double,
    val freeChildcareHoursAnnualValue: Double,
    val totalChildcareBenefitValue: Double,
    val outOfPocketSpendAnnual: Double,

    // Pension Sacrifice Remedy
    val requiredPensionSacrificeForEligibility: Double,
    val netTakeHomeCostOfSacrifice: Double,
    val totalAnnualGainBySacrificing: Double,
    val returnOnSacrificePercentage: Double
)

object TaxFreeChildcareEngine {

    // CRITICAL: TAX_ENGINE
    /**
     * Calculates UK Tax-Free Childcare subsidies, 30-hours free childcare value, and the £100k cliff-edge penalty.
     */
    fun calculate(input: TaxFreeChildcareInput): TaxFreeChildcareResult {
        val totalChildren = input.eligibleChildrenCount + input.disabledChildrenCount

        // 1. Calculate Adjusted Net Income (ANI)
        // ANI = Gross Income - (Gross Pension Contributions + Gross Gift Aid)
        val adjustedNetIncome = max(0.0, input.annualGrossIncome - input.existingPensionContributions - (input.existingGiftAidDonations * 1.25))

        val isEligible = adjustedNetIncome <= 100000.0 && totalChildren > 0
        val isCliffEdgeHit = adjustedNetIncome > 100000.0 && totalChildren > 0
        val excessOver100k = if (isCliffEdgeHit) adjustedNetIncome - 100000.0 else 0.0

        if (totalChildren == 0) {
            return TaxFreeChildcareResult(
                adjustedNetIncome = adjustedNetIncome,
                isEligible = false,
                isCliffEdgeHit = false,
                excessOver100k = 0.0,
                governmentTopUpAnnual = 0.0,
                freeChildcareHoursAnnualValue = 0.0,
                totalChildcareBenefitValue = 0.0,
                outOfPocketSpendAnnual = 0.0,
                requiredPensionSacrificeForEligibility = 0.0,
                netTakeHomeCostOfSacrifice = 0.0,
                totalAnnualGainBySacrificing = 0.0,
                returnOnSacrificePercentage = 0.0
            )
        }

        // 2. Tax-Free Childcare 20% Top-Up (Max £2,000/child under 11, £4,000/child disabled)
        val standardTopUpPerChild = minOf(input.annualChildcareSpendPerChild * 0.20, 2000.0)
        val disabledTopUpPerChild = minOf(input.annualChildcareSpendPerChild * 0.20, 4000.0)

        val potentialTopUp = (standardTopUpPerChild * input.eligibleChildrenCount) + (disabledTopUpPerChild * input.disabledChildrenCount)
        val actualTopUp = if (isEligible) potentialTopUp else 0.0

        // 3. 30 Hours Free Childcare (38 term weeks * 30 hrs = 1,140 hrs/yr per eligible preschool child)
        val hoursPerYear = 38.0 * 30.0 // 1,140 hours
        val potentialFreeHoursValue = if (input.has30HoursFreeChildcare) {
            hoursPerYear * input.averageChildcareHourlyCost * totalChildren
        } else {
            0.0
        }
        val actualFreeHoursValue = if (isEligible) potentialFreeHoursValue else 0.0

        val totalChildcareBenefitValue = actualTopUp + actualFreeHoursValue
        val potentialTotalBenefitValue = potentialTopUp + potentialFreeHoursValue

        val totalSpend = input.annualChildcareSpendPerChild * totalChildren
        val outOfPocketSpend = max(0.0, totalSpend - totalChildcareBenefitValue)

        // 4. Pension Sacrifice Remedy for £100k Cliff Edge
        val requiredSacrifice = if (isCliffEdgeHit) excessOver100k + 1.0 else 0.0
        // In the 40%/60% band, net cost of sacrifice is only 40% of the gross sacrifice (due to 40% tax relief + 2% NI)
        val netCostOfSacrifice = requiredSacrifice * 0.58 // ~58% net cost after 40% tax + 2% NI relief
        val totalGain = if (isCliffEdgeHit) potentialTotalBenefitValue - netCostOfSacrifice else 0.0
        val roi = if (netCostOfSacrifice > 0.0) (totalGain / netCostOfSacrifice) * 100.0 else 0.0

        return TaxFreeChildcareResult(
            adjustedNetIncome = adjustedNetIncome,
            isEligible = isEligible,
            isCliffEdgeHit = isCliffEdgeHit,
            excessOver100k = excessOver100k,
            governmentTopUpAnnual = actualTopUp,
            freeChildcareHoursAnnualValue = actualFreeHoursValue,
            totalChildcareBenefitValue = totalChildcareBenefitValue,
            outOfPocketSpendAnnual = outOfPocketSpend,
            requiredPensionSacrificeForEligibility = requiredSacrifice,
            netTakeHomeCostOfSacrifice = netCostOfSacrifice,
            totalAnnualGainBySacrificing = totalGain,
            returnOnSacrificePercentage = roi
        )
    }
}
