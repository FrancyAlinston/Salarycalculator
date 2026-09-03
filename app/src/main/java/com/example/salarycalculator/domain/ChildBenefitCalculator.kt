package com.example.salarycalculator.domain

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class ChildBenefitResult(
    val numChildren: Int,
    val annualBenefitEntitlement: Double,
    val monthlyBenefitEntitlement: Double,
    val annualTaxCharge: Double,
    val monthlyTaxCharge: Double,
    val netAnnualBenefit: Double,
    val netMonthlyBenefit: Double,
    val clawbackPercentage: Double
)

object ChildBenefitCalculator {

    // 2024/2025 Statutory Weekly Rates
    const val FIRST_CHILD_WEEKLY = 25.60
    const val SUBSEQUENT_CHILD_WEEKLY = 16.95
    const val THRESHOLD_LOWER = 60000.0
    const val THRESHOLD_UPPER = 80000.0

    /**
     * Calculates Child Benefit entitlement and High Income Child Benefit Charge (HICBC).
     */
    fun calculate(
        annualIncome: Double,
        numChildren: Int
    ): ChildBenefitResult {
        val safeCount = max(0, numChildren)
        if (safeCount == 0) {
            return ChildBenefitResult(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        // Annual Entitlement
        val weeklyTotal = FIRST_CHILD_WEEKLY + (max(0, safeCount - 1) * SUBSEQUENT_CHILD_WEEKLY)
        val annualBenefit = weeklyTotal * 52.0
        val monthlyBenefit = annualBenefit / 12.0

        // HICBC Taper calculation
        var clawbackPct = 0.0
        var annualCharge = 0.0

        if (annualIncome > THRESHOLD_LOWER) {
            if (annualIncome >= THRESHOLD_UPPER) {
                clawbackPct = 100.0
                annualCharge = annualBenefit
            } else {
                val excess = annualIncome - THRESHOLD_LOWER
                // 1% for every £200 over £60,000
                val steps = floor(excess / 200.0)
                clawbackPct = min(100.0, steps * 1.0)
                annualCharge = annualBenefit * (clawbackPct / 100.0)
            }
        }

        val monthlyCharge = annualCharge / 12.0
        val netAnnual = max(0.0, annualBenefit - annualCharge)
        val netMonthly = max(0.0, monthlyBenefit - monthlyCharge)

        return ChildBenefitResult(
            numChildren = safeCount,
            annualBenefitEntitlement = annualBenefit,
            monthlyBenefitEntitlement = monthlyBenefit,
            annualTaxCharge = annualCharge,
            monthlyTaxCharge = monthlyCharge,
            netAnnualBenefit = netAnnual,
            netMonthlyBenefit = netMonthly,
            clawbackPercentage = clawbackPct
        )
    }
}
