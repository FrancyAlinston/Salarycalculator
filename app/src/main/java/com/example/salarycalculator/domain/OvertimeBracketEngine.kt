package com.example.salarycalculator.domain

import kotlin.math.max

// CRITICAL: TAX_ENGINE
data class OvertimeBracketInput(
    val baseAnnualGross: Double,
    val hourlyRate: Double = 15.0,
    val overtimeMultiplier: Double = 1.5,
    val monthlyOvertimeHours: Double = 10.0,
    val pensionSacrificeRate: Double = 5.0
)

data class OvertimeBracketResult(
    val baseAnnualGross: Double,
    val annualOvertimeEarnings: Double,
    val totalAnnualGross: Double,
    val taxableGrossAfterPension: Double,
    
    // 40% Higher Rate Band Headroom (£50,270)
    val higherRateThreshold: Double = 50270.0,
    val higherRateHeadroomPounds: Double,
    val isHigherRateCrossed: Boolean,
    val maxMonthlyOvertimeHoursBeforeHigherRate: Double,
    
    // 60% Marginal Tax Trap Headroom (£100,000)
    val taxTrapThreshold: Double = 100000.0,
    val taxTrapHeadroomPounds: Double,
    val isTaxTrapCrossed: Boolean,
    val maxMonthlyOvertimeHoursBeforeTaxTrap: Double,

    // Recommended Pension Sacrifice Remedy
    val effectiveOvertimeHourlyRate: Double,
    val recommendedPensionSacrificeToStayBelowHigherRate: Double,
    val recommendedPensionSacrificeToStayBelowTaxTrap: Double
)

object OvertimeBracketEngine {

    // CRITICAL: TAX_ENGINE
    /**
     * Calculates tax bracket thresholds, headroom capacity, and maximum workable overtime hours.
     */
    fun calculate(input: OvertimeBracketInput): OvertimeBracketResult {
        val effectiveOvertimeHourlyRate = input.hourlyRate * input.overtimeMultiplier
        val monthlyOvertimeEarnings = input.monthlyOvertimeHours * effectiveOvertimeHourlyRate
        val annualOvertimeEarnings = monthlyOvertimeEarnings * 12.0
        val totalAnnualGross = input.baseAnnualGross + annualOvertimeEarnings

        val pensionSacrificeAmount = totalAnnualGross * (input.pensionSacrificeRate / 100.0)
        val taxableGrossAfterPension = max(0.0, totalAnnualGross - pensionSacrificeAmount)

        val higherRateThreshold = 50270.0
        val taxTrapThreshold = 100000.0

        // Higher Rate (£50,270) Analysis
        val higherRateHeadroomPounds = max(0.0, higherRateThreshold - taxableGrossAfterPension)
        val isHigherRateCrossed = taxableGrossAfterPension > higherRateThreshold

        val maxMonthlyOtHoursHigherRate = if (effectiveOvertimeHourlyRate > 0.0) {
            val baseTaxable = input.baseAnnualGross * (1.0 - input.pensionSacrificeRate / 100.0)
            val availablePounds = max(0.0, higherRateThreshold - baseTaxable)
            (availablePounds / 12.0) / (effectiveOvertimeHourlyRate * (1.0 - input.pensionSacrificeRate / 100.0))
        } else {
            0.0
        }

        // 60% Tax Trap (£100,000) Analysis
        val taxTrapHeadroomPounds = max(0.0, taxTrapThreshold - taxableGrossAfterPension)
        val isTaxTrapCrossed = taxableGrossAfterPension > taxTrapThreshold

        val maxMonthlyOtHoursTaxTrap = if (effectiveOvertimeHourlyRate > 0.0) {
            val baseTaxable = input.baseAnnualGross * (1.0 - input.pensionSacrificeRate / 100.0)
            val availablePounds = max(0.0, taxTrapThreshold - baseTaxable)
            (availablePounds / 12.0) / (effectiveOvertimeHourlyRate * (1.0 - input.pensionSacrificeRate / 100.0))
        } else {
            0.0
        }

        // Sacrifice remedies
        val sacrificeForHigherRate = if (isHigherRateCrossed) (taxableGrossAfterPension - higherRateThreshold) / 12.0 else 0.0
        val sacrificeForTaxTrap = if (isTaxTrapCrossed) (taxableGrossAfterPension - taxTrapThreshold) / 12.0 else 0.0

        return OvertimeBracketResult(
            baseAnnualGross = input.baseAnnualGross,
            annualOvertimeEarnings = annualOvertimeEarnings,
            totalAnnualGross = totalAnnualGross,
            taxableGrossAfterPension = taxableGrossAfterPension,
            higherRateHeadroomPounds = higherRateHeadroomPounds,
            isHigherRateCrossed = isHigherRateCrossed,
            maxMonthlyOvertimeHoursBeforeHigherRate = maxMonthlyOtHoursHigherRate,
            taxTrapHeadroomPounds = taxTrapHeadroomPounds,
            isTaxTrapCrossed = isTaxTrapCrossed,
            maxMonthlyOvertimeHoursBeforeTaxTrap = maxMonthlyOtHoursTaxTrap,
            effectiveOvertimeHourlyRate = effectiveOvertimeHourlyRate,
            recommendedPensionSacrificeToStayBelowHigherRate = sacrificeForHigherRate,
            recommendedPensionSacrificeToStayBelowTaxTrap = sacrificeForTaxTrap
        )
    }
}
