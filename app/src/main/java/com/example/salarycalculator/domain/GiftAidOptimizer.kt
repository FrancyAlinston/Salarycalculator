package com.example.salarycalculator.domain

import kotlin.math.max

data class GiftAidReport(
    val netDonation: Double,
    val grossDonationToCharity: Double,
    val hmrcBasicRateTopUp: Double,
    val userAnnualSalary: Double,
    val marginalTaxRatePercent: Double,
    val higherRateTaxReliefClaimable: Double,
    val effectiveNetCostToDonor: Double,
    val expandedBasicRateBandLimit: Double,
    val notes: List<String>
)

object GiftAidOptimizer {

    const val STANDARD_BASIC_RATE_BAND = 37700.0

    /**
     * Calculates UK Gift Aid tax relief, charity gross-up, and higher-rate band expansion.
     */
    // CRITICAL: TAX_ENGINE
    fun calculateGiftAid(
        netDonation: Double,
        annualSalary: Double,
        taxRegion: TaxRegion = TaxRegion.UK_STANDARD
    ): GiftAidReport {
        val net = max(0.0, netDonation)
        val gross = net * 1.25 // HMRC 25% basic rate top up
        val basicRateTopUp = gross - net

        val marginalRate: Double = when {
            annualSalary > 125140.0 -> if (taxRegion == TaxRegion.SCOTLAND) 48.0 else 45.0
            annualSalary > 50270.0 -> if (taxRegion == TaxRegion.SCOTLAND) 42.0 else 40.0
            else -> 20.0
        }

        // Higher rate relief is the difference between marginal rate and 20% basic rate
        val extraReliefRate = max(0.0, (marginalRate - 20.0) / 100.0)
        val higherRateRelief = gross * extraReliefRate
        val effectiveNetCost = max(0.0, net - higherRateRelief)

        val expandedBand = STANDARD_BASIC_RATE_BAND + gross

        val notes = mutableListOf<String>()
        notes.add("🎁 Charity receives £${"%,.2f".format(gross)} (£${"%,.2f".format(net)} from you + £${"%,.2f".format(basicRateTopUp)} from HMRC).")

        if (higherRateRelief > 0) {
            notes.add("💰 You can claim £${"%,.2f".format(higherRateRelief)} back via Self Assessment / HMRC tax code adjustment.")
            notes.add("✨ Effective Net Cost to you: only £${"%,.2f".format(effectiveNetCost)} for a £${"%,.2f".format(gross)} charity gift.")
            notes.add("📈 Basic rate 20% tax band extended from £37,700 to £${"%,.0f".format(expandedBand)}.")
        } else {
            notes.add("ℹ️ Basic rate taxpayers do not claim additional personal relief, but charity receives full 25% top-up.")
        }

        return GiftAidReport(
            netDonation = net,
            grossDonationToCharity = gross,
            hmrcBasicRateTopUp = basicRateTopUp,
            userAnnualSalary = annualSalary,
            marginalTaxRatePercent = marginalRate,
            higherRateTaxReliefClaimable = higherRateRelief,
            effectiveNetCostToDonor = effectiveNetCost,
            expandedBasicRateBandLimit = expandedBand,
            notes = notes
        )
    }
}
