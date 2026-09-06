package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.round

/**
 * Multi-Rate Shift Differentials & Enhancements Engine.
 *
 * Designed for care workers, healthcare professionals, security, and shift workers
 * whose earnings include unsocial hours uplifts, weekend premiums, bank holiday multipliers,
 * and flat sleep-in duty allowances.
 */
object ShiftRateDifferentialEngine {

    enum class UpliftType {
        HOURLY_ADDITION, // e.g. Base + £2.00/hr
        MULTIPLIER       // e.g. Base * 1.25x
    }

    data class ShiftDifferentialInput(
        val baseHourlyRate: Double = 12.82,
        val standardDayHours: Double = 120.0,
        val nightHours: Double = 36.0,
        val nightUpliftType: UpliftType = UpliftType.HOURLY_ADDITION,
        val nightUpliftValue: Double = 2.00, // +£2.00/hr or 1.25x
        val weekendHours: Double = 24.0,
        val weekendMultiplier: Double = 1.25, // 1.25x
        val bankHolidayHours: Double = 0.0,
        val bankHolidayMultiplier: Double = 2.0, // 2.0x Double Time
        val sleepInDutiesCount: Int = 0,
        val sleepInAllowancePerDuty: Double = 50.0 // £50.00 flat per sleep-in
    )

    data class ShiftDifferentialResult(
        val baseHourlyRate: Double,
        val totalWorkingHours: Double,
        val standardBasePay: Double,
        val nightEffectiveRate: Double,
        val nightTotalPay: Double,
        val nightUpliftGross: Double,
        val weekendEffectiveRate: Double,
        val weekendTotalPay: Double,
        val weekendUpliftGross: Double,
        val bankHolidayEffectiveRate: Double,
        val bankHolidayTotalPay: Double,
        val bankHolidayUpliftGross: Double,
        val sleepInDutiesCount: Int,
        val sleepInTotalPay: Double,
        val totalGrossPay: Double,
        val totalDifferentialUplift: Double,
        val blendedHourlyRate: Double
    )

    /**
     * Calculates shift earnings across standard, night, weekend, bank holiday, and sleep-in components.
     */
    fun calculateDifferentials(input: ShiftDifferentialInput): ShiftDifferentialResult {
        val baseRate = max(0.0, input.baseHourlyRate)
        val dayHours = max(0.0, input.standardDayHours)
        val nightHours = max(0.0, input.nightHours)
        val weekendHours = max(0.0, input.weekendHours)
        val bhHours = max(0.0, input.bankHolidayHours)
        val sleepInCount = max(0, input.sleepInDutiesCount)
        val sleepInRate = max(0.0, input.sleepInAllowancePerDuty)

        // 1. Standard Day Pay
        val standardPay = round(dayHours * baseRate * 100.0) / 100.0

        // 2. Night Pay
        val nightEffectiveRate = when (input.nightUpliftType) {
            UpliftType.HOURLY_ADDITION -> baseRate + max(0.0, input.nightUpliftValue)
            UpliftType.MULTIPLIER -> baseRate * max(1.0, input.nightUpliftValue)
        }
        val nightTotalPay = round(nightHours * nightEffectiveRate * 100.0) / 100.0
        val nightUpliftGross = max(0.0, nightTotalPay - (nightHours * baseRate))

        // 3. Weekend Pay
        val weekendEffectiveRate = baseRate * max(1.0, input.weekendMultiplier)
        val weekendTotalPay = round(weekendHours * weekendEffectiveRate * 100.0) / 100.0
        val weekendUpliftGross = max(0.0, weekendTotalPay - (weekendHours * baseRate))

        // 4. Bank Holiday Pay
        val bhEffectiveRate = baseRate * max(1.0, input.bankHolidayMultiplier)
        val bhTotalPay = round(bhHours * bhEffectiveRate * 100.0) / 100.0
        val bhUpliftGross = max(0.0, bhTotalPay - (bhHours * baseRate))

        // 5. Sleep-in Duties Allowance
        val sleepInTotalPay = round(sleepInCount * sleepInRate * 100.0) / 100.0

        // 6. Totals
        val totalHours = dayHours + nightHours + weekendHours + bhHours
        val totalGross = round((standardPay + nightTotalPay + weekendTotalPay + bhTotalPay + sleepInTotalPay) * 100.0) / 100.0
        val totalUplift = round((nightUpliftGross + weekendUpliftGross + bhUpliftGross + sleepInTotalPay) * 100.0) / 100.0

        val blendedRate = if (totalHours > 0) {
            round((totalGross / totalHours) * 100.0) / 100.0
        } else {
            baseRate
        }

        return ShiftDifferentialResult(
            baseHourlyRate = baseRate,
            totalWorkingHours = totalHours,
            standardBasePay = standardPay,
            nightEffectiveRate = round(nightEffectiveRate * 100.0) / 100.0,
            nightTotalPay = nightTotalPay,
            nightUpliftGross = round(nightUpliftGross * 100.0) / 100.0,
            weekendEffectiveRate = round(weekendEffectiveRate * 100.0) / 100.0,
            weekendTotalPay = weekendTotalPay,
            weekendUpliftGross = round(weekendUpliftGross * 100.0) / 100.0,
            bankHolidayEffectiveRate = round(bhEffectiveRate * 100.0) / 100.0,
            bankHolidayTotalPay = bhTotalPay,
            bankHolidayUpliftGross = round(bhUpliftGross * 100.0) / 100.0,
            sleepInDutiesCount = sleepInCount,
            sleepInTotalPay = sleepInTotalPay,
            totalGrossPay = totalGross,
            totalDifferentialUplift = totalUplift,
            blendedHourlyRate = blendedRate
        )
    }
}
