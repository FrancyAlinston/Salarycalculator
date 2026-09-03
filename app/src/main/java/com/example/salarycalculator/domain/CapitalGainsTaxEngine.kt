package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min

enum class AssetType(val displayName: String, val basicRate: Double, val higherRate: Double) {
    SHARES_AND_OTHER("Shares, Crypto & Other Assets", 0.10, 0.20),
    RESIDENTIAL_PROPERTY("Residential Property", 0.18, 0.24)
}

data class CapitalGainsReport(
    val disposalProceeds: Double,
    val allowableCosts: Double,
    val totalGain: Double,
    val annualExemptionUsed: Double,
    val taxableGain: Double,
    val taxableIncome: Double,
    val remainingBasicRateBand: Double,
    val gainTaxedAtBasicRate: Double,
    val basicRateTax: Double,
    val gainTaxedAtHigherRate: Double,
    val higherRateTax: Double,
    val totalCgtDue: Double,
    val effectiveCgtRatePercent: Double,
    val notes: List<String>
)

object CapitalGainsTaxEngine {

    const val STATUTORY_CGT_ANNUAL_EXEMPTION = 3000.0 // 2024/25 & 2025/26 statutory threshold
    const val BASIC_RATE_INCOME_BAND = 37700.0

    /**
     * Calculates UK Capital Gains Tax (CGT) considering £3,000 exemption, asset type, and taxable income band absorption.
     */
    // CRITICAL: TAX_ENGINE
    fun calculateCgt(
        disposalProceeds: Double,
        acquisitionAndAllowableCosts: Double,
        annualTaxableIncome: Double,
        assetType: AssetType = AssetType.SHARES_AND_OTHER,
        customExemptionOverride: Double? = null
    ): CapitalGainsReport {
        val proceeds = max(0.0, disposalProceeds)
        val costs = max(0.0, acquisitionAndAllowableCosts)
        val totalGain = max(0.0, proceeds - costs)

        val exemption = customExemptionOverride ?: STATUTORY_CGT_ANNUAL_EXEMPTION
        val exemptionUsed = min(totalGain, exemption)
        val taxableGain = max(0.0, totalGain - exemptionUsed)

        // Remaining basic rate band available
        val remainingBasicBand = max(0.0, BASIC_RATE_INCOME_BAND - annualTaxableIncome)

        val gainAtBasic = min(taxableGain, remainingBasicBand)
        val gainAtHigher = max(0.0, taxableGain - gainAtBasic)

        val basicTax = gainAtBasic * assetType.basicRate
        val higherTax = gainAtHigher * assetType.higherRate
        val totalTax = basicTax + higherTax

        val effectiveRate = if (totalGain > 0) (totalTax / totalGain) * 100.0 else 0.0

        val notes = mutableListOf<String>()
        notes.add("🛡️ £${"%,.0f".format(exemptionUsed)} statutory Annual Exempt Amount applied.")

        if (taxableGain <= 0.0) {
            notes.add("✅ Zero CGT payable — entire gain is covered by your £3,000 annual exemption.")
        } else {
            if (gainAtBasic > 0) {
                notes.add("📊 £${"%,.2f".format(gainAtBasic)} taxed at ${(assetType.basicRate * 100).toInt()}% basic rate.")
            }
            if (gainAtHigher > 0) {
                notes.add("📈 £${"%,.2f".format(gainAtHigher)} taxed at ${(assetType.higherRate * 100).toInt()}% higher rate.")
            }
            notes.add("💰 Total Capital Gains Tax payable: £${"%,.2f".format(totalTax)} (${"%.1f".format(effectiveRate)}% effective rate on total gain).")
        }

        return CapitalGainsReport(
            disposalProceeds = proceeds,
            allowableCosts = costs,
            totalGain = totalGain,
            annualExemptionUsed = exemptionUsed,
            taxableGain = taxableGain,
            taxableIncome = annualTaxableIncome,
            remainingBasicRateBand = remainingBasicBand,
            gainTaxedAtBasicRate = gainAtBasic,
            basicRateTax = basicTax,
            gainTaxedAtHigherRate = gainAtHigher,
            higherRateTax = higherTax,
            totalCgtDue = totalTax,
            effectiveCgtRatePercent = effectiveRate,
            notes = notes
        )
    }
}
