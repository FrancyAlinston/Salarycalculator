package com.example.salarycalculator.domain

import kotlinx.serialization.Serializable
import kotlin.math.max

/**
 * Types of shift coverage and rota modifications.
 */
enum class ShiftSwapType(val label: String, val description: String) {
    DIRECT_SWAP("Direct Swap", "Exchange a shift with a colleague"),
    PICK_UP_COVER("Pick Up Extra Shift", "Cover a colleague's shift for extra overtime pay"),
    GIVE_AWAY("Give Away Shift", "Surrender a shift to a colleague")
}

/**
 * Shift swap simulation input parameters.
 */
@Serializable
data class ShiftSwapSimulationInput(
    val colleagueName: String,
    val dayOfMonth: Int,
    val swapType: ShiftSwapType,
    val currentShiftHours: Double,
    val newShiftHours: Double,
    val baseHourlyRate: Double = 15.0,
    val currentMonthlyGross: Double,
    val taxCode: String = "1257L",
    val overtimeMultiplier: Double = 1.5,
    val isOvertimeCover: Boolean = true,
    val pensionRate: Double = 0.0,
    val studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
    val region: TaxRegion = TaxRegion.UK_STANDARD
)

/**
 * Computed financial impact of a shift swap or coverage.
 */
@Serializable
data class ShiftSwapAnalysisResult(
    val swapType: ShiftSwapType,
    val colleagueName: String,
    val dayOfMonth: Int,
    val hoursDelta: Double,
    val baselineGrossMonthly: Double,
    val postSwapGrossMonthly: Double,
    val grossDelta: Double,
    val baselineNetMonthly: Double,
    val postSwapNetMonthly: Double,
    val netDelta: Double,
    val taxDelta: Double,
    val niDelta: Double,
    val pensionDelta: Double,
    val studentLoanDelta: Double,
    val marginalRetentionRate: Double,
    val netHourlyYield: Double,
    val summaryAdvice: String
)

/**
 * Shift Swap & Coverage Engine.
 * Evaluates the exact net income impact, marginal PAYE tax, NI drag, and retention rate
 * when care workers swap shifts, pick up overtime cover, or surrender shifts.
 */
object ShiftSwapEngine {

    /**
     * Calculates the full before-and-after statutory tax and net pay impact of a shift swap.
     */
    fun calculateShiftSwapImpact(input: ShiftSwapSimulationInput): ShiftSwapAnalysisResult {
        val hoursDelta = when (input.swapType) {
            ShiftSwapType.DIRECT_SWAP -> input.newShiftHours - input.currentShiftHours
            ShiftSwapType.PICK_UP_COVER -> input.newShiftHours
            ShiftSwapType.GIVE_AWAY -> -input.currentShiftHours
        }

        val effectiveMultiplier = if (input.swapType == ShiftSwapType.PICK_UP_COVER && input.isOvertimeCover) {
            input.overtimeMultiplier
        } else {
            1.0
        }

        val grossDelta = when (input.swapType) {
            ShiftSwapType.DIRECT_SWAP -> {
                if (hoursDelta >= 0) {
                    hoursDelta * input.baseHourlyRate * effectiveMultiplier
                } else {
                    hoursDelta * input.baseHourlyRate
                }
            }
            ShiftSwapType.PICK_UP_COVER -> input.newShiftHours * input.baseHourlyRate * effectiveMultiplier
            ShiftSwapType.GIVE_AWAY -> -input.currentShiftHours * input.baseHourlyRate
        }

        val baselineGross = max(0.0, input.currentMonthlyGross)
        val postSwapGross = max(0.0, baselineGross + grossDelta)

        // Compute baseline tax breakdown
        val baselineTax = TaxCalculator.calculateTax(
            grossPay = baselineGross,
            taxCode = input.taxCode,
            isMonthly = true,
            pensionRatePercent = input.pensionRate,
            studentLoanPlan = input.studentLoanPlan,
            region = input.region
        )

        // Compute post-swap tax breakdown
        val postSwapTax = TaxCalculator.calculateTax(
            grossPay = postSwapGross,
            taxCode = input.taxCode,
            isMonthly = true,
            pensionRatePercent = input.pensionRate,
            studentLoanPlan = input.studentLoanPlan,
            region = input.region
        )

        val netDelta = postSwapTax.netPay - baselineTax.netPay
        val taxDelta = postSwapTax.incomeTax - baselineTax.incomeTax
        val niDelta = postSwapTax.nationalInsurance - baselineTax.nationalInsurance
        val pensionDelta = postSwapTax.pensionContribution - baselineTax.pensionContribution
        val studentLoanDelta = postSwapTax.studentLoanDeduction - baselineTax.studentLoanDeduction

        val marginalRetention = if (grossDelta != 0.0) {
            max(0.0, (netDelta / grossDelta) * 100.0)
        } else {
            100.0
        }

        val netHourlyYield = if (hoursDelta > 0.0) {
            netDelta / hoursDelta
        } else if (hoursDelta < 0.0) {
            netDelta / hoursDelta // Net rate saved per hour given away
        } else {
            input.baseHourlyRate
        }

        val summaryAdvice = when (input.swapType) {
            ShiftSwapType.PICK_UP_COVER -> {
                "Covering ${input.colleagueName}'s ${input.newShiftHours.toInt()}h shift earns +£${"%.2f".format(grossDelta)} gross. After 20% PAYE & 8% NI, you keep +£${"%.2f".format(netDelta)} net (£${"%.2f".format(netHourlyYield)}/h net in hand)."
            }
            ShiftSwapType.GIVE_AWAY -> {
                "Giving up your ${input.currentShiftHours.toInt()}h shift to ${input.colleagueName} reduces gross by £${"%.2f".format(-grossDelta)}, resulting in a net monthly reduction of £${"%.2f".format(-netDelta)}."
            }
            ShiftSwapType.DIRECT_SWAP -> {
                if (grossDelta > 0.0) {
                    "Swapping with ${input.colleagueName} increases pay by +£${"%.2f".format(netDelta)} net take-home."
                } else if (grossDelta < 0.0) {
                    "Swapping with ${input.colleagueName} adjusts take-home pay by -£${"%.2f".format(-netDelta)} net."
                } else {
                    "Equal hours swap with ${input.colleagueName}. No change to monthly gross or net pay."
                }
            }
        }

        return ShiftSwapAnalysisResult(
            swapType = input.swapType,
            colleagueName = input.colleagueName,
            dayOfMonth = input.dayOfMonth,
            hoursDelta = hoursDelta,
            baselineGrossMonthly = baselineGross,
            postSwapGrossMonthly = postSwapGross,
            grossDelta = grossDelta,
            baselineNetMonthly = baselineTax.netPay,
            postSwapNetMonthly = postSwapTax.netPay,
            netDelta = netDelta,
            taxDelta = taxDelta,
            niDelta = niDelta,
            pensionDelta = pensionDelta,
            studentLoanDelta = studentLoanDelta,
            marginalRetentionRate = marginalRetention,
            netHourlyYield = netHourlyYield,
            summaryAdvice = summaryAdvice
        )
    }
}
