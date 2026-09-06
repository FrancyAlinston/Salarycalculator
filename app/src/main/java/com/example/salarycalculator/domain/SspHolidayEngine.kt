package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.round

/**
 * Statutory Sick Pay (SSP) and Statutory Holiday Pay Accrual Engine.
 *
 * Grounded in UK HMRC & Department for Work and Pensions (DWP) statutory rules for
 * Tax Years 2024/2025 & 2025/2026:
 * - Standard SSP Rate: £116.75 per week.
 * - Qualifying Days: Waiting days rule (first 3 qualifying days unpaid, unless linked PIW within 8 weeks).
 * - Statutory Holiday Accrual: 5.6 weeks annual entitlement (28 days max for 5-day week).
 * - Irregular / Shift Worker Accrual: 12.07% statutory accrual on hours worked (April 2024 reform).
 */
object SspHolidayEngine {

    // CRITICAL: TAX_ENGINE - UK 2024/2025 & 2025/2026 Statutory Rate
    const val WEEKLY_SSP_RATE_2024_2026 = 116.75
    const val STATUTORY_HOLIDAY_ACCRUAL_RATE = 0.1207 // 5.6 / 46.4 = 12.07%
    const val MAX_STATUTORY_HOLIDAY_DAYS = 28.0

    data class SspCalculationResult(
        val totalSickDaysLogged: Int,
        val qualifyingDaysPerWeek: Int,
        val isLinkedPeriod: Boolean,
        val waitingDaysCount: Int,
        val paidSspDaysCount: Int,
        val dailySspRate: Double,
        val totalGrossSsp: Double
    )

    data class HolidayAccrualResult(
        val hoursWorked: Double,
        val hourlyRate: Double,
        val accruedHolidayHours: Double,
        val accruedHolidayPay: Double,
        val rolledUpHourlyRate: Double,
        val daysEquivalent: Double
    )

    data class CombinedStatutoryImpact(
        val sspResult: SspCalculationResult,
        val holidayResult: HolidayAccrualResult,
        val totalStatutoryAddition: Double
    )

    /**
     * Computes Statutory Sick Pay (SSP) entitlement.
     *
     * @param qualifyingDaysPerWeek Number of days employee normally works per week (typically 1 to 7).
     * @param sickDaysLogged Total consecutive working days absent due to illness in this cycle.
     * @param isLinkedPeriod Whether this sickness period links to a previous one within 8 weeks (bypassing 3 waiting days).
     */
    fun calculateSsp(
        qualifyingDaysPerWeek: Int = 5,
        sickDaysLogged: Int = 0,
        isLinkedPeriod: Boolean = false
    ): SspCalculationResult {
        val safeQDays = qualifyingDaysPerWeek.coerceIn(1, 7)
        val safeSickDays = max(0, sickDaysLogged)

        // Waiting days: first 3 qualifying days are unpaid, unless linked
        val waitingDays = if (isLinkedPeriod) 0 else minOf(3, safeSickDays)
        val paidDays = max(0, safeSickDays - waitingDays)

        // Daily rate is weekly rate divided by qualifying days per week
        val dailyRate = WEEKLY_SSP_RATE_2024_2026 / safeQDays
        val totalSsp = round(paidDays * dailyRate * 100.0) / 100.0

        return SspCalculationResult(
            totalSickDaysLogged = safeSickDays,
            qualifyingDaysPerWeek = safeQDays,
            isLinkedPeriod = isLinkedPeriod,
            waitingDaysCount = waitingDays,
            paidSspDaysCount = paidDays,
            dailySspRate = round(dailyRate * 100.0) / 100.0,
            totalGrossSsp = totalSsp
        )
    }

    /**
     * Computes statutory holiday entitlement and accrual for shift / irregular hours workers.
     *
     * Under the April 2024 UK statutory reforms, irregular and part-year workers accrue
     * holiday entitlement at 12.07% of the hours worked in each pay period.
     *
     * @param hoursWorked Total hours worked in the pay period.
     * @param hourlyRate Base hourly wage.
     * @param standardShiftHours Standard hours per shift for calculating days equivalent.
     */
    fun calculateHolidayAccrual(
        hoursWorked: Double,
        hourlyRate: Double,
        standardShiftHours: Double = 12.0
    ): HolidayAccrualResult {
        val safeHours = max(0.0, hoursWorked)
        val safeRate = max(0.0, hourlyRate)
        val safeShift = if (standardShiftHours > 0) standardShiftHours else 8.0

        val accruedHours = round(safeHours * STATUTORY_HOLIDAY_ACCRUAL_RATE * 100.0) / 100.0
        val accruedPay = round(accruedHours * safeRate * 100.0) / 100.0
        val rolledUpRate = round(safeRate * (1.0 + STATUTORY_HOLIDAY_ACCRUAL_RATE) * 100.0) / 100.0
        val daysEquivalent = round((accruedHours / safeShift) * 10.0) / 10.0

        return HolidayAccrualResult(
            hoursWorked = safeHours,
            hourlyRate = safeRate,
            accruedHolidayHours = accruedHours,
            accruedHolidayPay = accruedPay,
            rolledUpHourlyRate = rolledUpRate,
            daysEquivalent = daysEquivalent
        )
    }

    /**
     * Combines SSP and Holiday accrual into a unified statutory adjustment.
     */
    fun calculateCombinedImpact(
        qualifyingDaysPerWeek: Int,
        sickDaysLogged: Int,
        isLinkedPeriod: Boolean,
        hoursWorked: Double,
        hourlyRate: Double,
        standardShiftHours: Double
    ): CombinedStatutoryImpact {
        val ssp = calculateSsp(qualifyingDaysPerWeek, sickDaysLogged, isLinkedPeriod)
        val holiday = calculateHolidayAccrual(hoursWorked, hourlyRate, standardShiftHours)
        val totalAddition = round((ssp.totalGrossSsp + holiday.accruedHolidayPay) * 100.0) / 100.0

        return CombinedStatutoryImpact(
            sspResult = ssp,
            holidayResult = holiday,
            totalStatutoryAddition = totalAddition
        )
    }
}
