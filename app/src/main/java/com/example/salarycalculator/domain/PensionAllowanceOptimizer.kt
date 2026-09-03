package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min

/**
 * UK Pension Annual Allowance & Tapering Optimization Engine.
 * Covers 2024/2025 & 2025/2026 HMRC statutory legislation.
 */
data class PensionAllowanceReport(
    val standardAnnualAllowance: Double = 60000.0,
    val isTapered: Boolean,
    val isMpaaApplied: Boolean,
    val baseAllowance: Double,
    val totalCarryForward: Double,
    val totalAvailableAllowance: Double,
    val totalPensionContributions: Double, // Employee + Employer
    val employeeContribution: Double,
    val employerContribution: Double,
    val excessContribution: Double,
    val estimatedTaxCharge: Double,
    val marginalTaxRate: Double,
    val maxRelievedPersonalContribution: Double,
    val remainingAllowance: Double,
    val carryForwardBreakdown: List<CarryForwardYear>,
    val advisoryNotes: List<String>
)

data class CarryForwardYear(
    val taxYearLabel: String,
    val maxStatutoryAllowance: Double,
    val contributedAmount: Double,
    val unusedAllowance: Double
)

object PensionAllowanceOptimizer {

    const val STANDARD_ANNUAL_ALLOWANCE = 60000.0
    const val MPAA_ALLOWANCE = 10000.0
    const val TAPER_THRESHOLD_INCOME = 200000.0
    const val TAPER_ADJUSTED_INCOME_START = 260000.0
    const val TAPER_MINIMUM_ALLOWANCE = 10000.0

    /**
     * Calculates UK Pension Annual Allowance, Tapering, Carry Forward, and Tax Charges.
     */
    // CRITICAL: TAX_ENGINE
    fun calculatePensionAllowance(
        grossEarnings: Double,
        employeePensionPercent: Double,
        employerPensionPercent: Double = 3.0,
        otherTaxableIncome: Double = 0.0,
        hasTriggeredMpaa: Boolean = false,
        unusedYearMinus1: Double = 20000.0,
        unusedYearMinus2: Double = 15000.0,
        unusedYearMinus3: Double = 10000.0,
        taxRegion: TaxRegion = TaxRegion.UK_STANDARD
    ): PensionAllowanceReport {
        val totalEarnings = max(0.0, grossEarnings + otherTaxableIncome)
        val employeeContrib = grossEarnings * (max(0.0, employeePensionPercent) / 100.0)
        val employerContrib = grossEarnings * (max(0.0, employerPensionPercent) / 100.0)
        val totalContrib = employeeContrib + employerContrib

        // Threshold & Adjusted Income for Tapering
        val thresholdIncome = max(0.0, totalEarnings - employeeContrib)
        val adjustedIncome = totalEarnings + employerContrib

        val isTapered = thresholdIncome > TAPER_THRESHOLD_INCOME && adjustedIncome > TAPER_ADJUSTED_INCOME_START

        val baseAllowance = when {
            hasTriggeredMpaa -> MPAA_ALLOWANCE
            isTapered -> {
                val excess = adjustedIncome - TAPER_ADJUSTED_INCOME_START
                val reduction = excess / 2.0
                max(TAPER_MINIMUM_ALLOWANCE, STANDARD_ANNUAL_ALLOWANCE - reduction)
            }
            else -> STANDARD_ANNUAL_ALLOWANCE
        }

        val carryForwards = listOf(
            CarryForwardYear("2023/24 (Y-1)", 60000.0, max(0.0, 60000.0 - unusedYearMinus1), max(0.0, min(60000.0, unusedYearMinus1))),
            CarryForwardYear("2022/23 (Y-2)", 40000.0, max(0.0, 40000.0 - unusedYearMinus2), max(0.0, min(40000.0, unusedYearMinus2))),
            CarryForwardYear("2021/22 (Y-3)", 40000.0, max(0.0, 40000.0 - unusedYearMinus3), max(0.0, min(40000.0, unusedYearMinus3)))
        )

        // MPAA prevents using carry forward
        val totalCarryForward = if (hasTriggeredMpaa) 0.0 else carryForwards.sumOf { it.unusedAllowance }
        val totalAvailableAllowance = baseAllowance + totalCarryForward

        val excessContribution = max(0.0, totalContrib - totalAvailableAllowance)
        val remainingAllowance = max(0.0, totalAvailableAllowance - totalContrib)

        // Determine marginal rate for tax charge
        val marginalTaxRate = when {
            totalEarnings > 125140.0 -> if (taxRegion == TaxRegion.SCOTLAND) 0.48 else 0.45
            totalEarnings > 50270.0 -> if (taxRegion == TaxRegion.SCOTLAND) 0.42 else 0.40
            else -> if (taxRegion == TaxRegion.SCOTLAND) 0.20 else 0.20
        }

        val estimatedTaxCharge = excessContribution * marginalTaxRate

        // Max 100% of relevant UK earnings or £3,600
        val maxRelievedPersonal = max(3600.0, grossEarnings)

        val notes = mutableListOf<String>()
        if (hasTriggeredMpaa) {
            notes.add("⚠️ MPAA Active: Allowance restricted to £10,000/yr with zero carry forward permitted due to flexible pension access.")
        } else if (isTapered) {
            notes.add("📉 Tapered Annual Allowance: Adjusted income (£${"%,.0f".format(adjustedIncome)}) reduces allowance to £${"%,.0f".format(baseAllowance)}.")
        } else {
            notes.add("✅ Full £60,000 standard annual allowance applies.")
        }

        if (totalCarryForward > 0 && !hasTriggeredMpaa) {
            notes.add("💼 £${"%,.0f".format(totalCarryForward)} unused carry forward available from the previous 3 tax years.")
        }

        if (excessContribution > 0) {
            notes.add("🚨 Annual Allowance Excess: £${"%,.2f".format(excessContribution)} exceeds limit. Estimated HMRC tax charge: £${"%,.2f".format(estimatedTaxCharge)}.")
        }

        return PensionAllowanceReport(
            standardAnnualAllowance = STANDARD_ANNUAL_ALLOWANCE,
            isTapered = isTapered,
            isMpaaApplied = hasTriggeredMpaa,
            baseAllowance = baseAllowance,
            totalCarryForward = totalCarryForward,
            totalAvailableAllowance = totalAvailableAllowance,
            totalPensionContributions = totalContrib,
            employeeContribution = employeeContrib,
            employerContribution = employerContrib,
            excessContribution = excessContribution,
            estimatedTaxCharge = estimatedTaxCharge,
            marginalTaxRate = marginalTaxRate,
            maxRelievedPersonalContribution = maxRelievedPersonal,
            remainingAllowance = remainingAllowance,
            carryForwardBreakdown = carryForwards,
            advisoryNotes = notes
        )
    }
}
