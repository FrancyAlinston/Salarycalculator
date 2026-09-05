package com.example.salarycalculator.domain

import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
enum class PayScheduleType(val displayName: String, val description: String) {
    LAST_FRIDAY_OF_MONTH(
        "Last Friday of Month (Sunday Cutoff)",
        "Paid on the last Friday of each month. Cutoff is the preceding Sunday at 23:59."
    ),
    LAST_WORKING_DAY(
        "Last Working Day of Month",
        "Paid on the last Monday–Friday of the calendar month with a standard lead-time cutoff."
    ),
    FIXED_DAY_OF_MONTH(
        "Fixed Day of Month",
        "Paid on a set calendar day (e.g. 25th or 28th) with a configurable lead-time cutoff."
    ),
    CALENDAR_MONTH(
        "Standard Calendar Month (1st – End)",
        "Standard 1st day of month to the final calendar day."
    ),
    FOUR_WEEKLY(
        "Four-Weekly (28-day cycle)",
        "13 payroll periods per year recurring every 4 weeks."
    ),
    BI_WEEKLY(
        "Bi-Weekly (14-day cycle)",
        "26 payroll periods per year recurring every 2 weeks."
    )
}

@Serializable
data class PayScheduleConfig(
    val type: PayScheduleType = PayScheduleType.LAST_FRIDAY_OF_MONTH,
    val fixedDay: Int = 28,
    val cutoffLeadDays: Int = 5, // e.g. Sunday before Friday = 5 days
    val adjustForBankHolidays: Boolean = true
)

@Serializable
data class PayPeriodInfo(
    val year: Int,
    val month: Int, // 1..12
    val payYear: Int,
    val payMonth: Int,
    val payDay: Int,
    val cutoffYear: Int,
    val cutoffMonth: Int,
    val cutoffDay: Int,
    val startYear: Int,
    val startMonth: Int,
    val startDay: Int,
    val isBankHolidayAdjusted: Boolean = false
) {
    fun isCutoffDay(y: Int, m: Int, d: Int): Boolean {
        return y == cutoffYear && m == cutoffMonth && d == cutoffDay
    }

    fun isPayDay(y: Int, m: Int, d: Int): Boolean {
        return y == payYear && m == payMonth && d == payDay
    }

    /**
     * Checks if a given calendar day in the selected month falls strictly within this pay period's cutoff window.
     */
    fun isDayIncludedInCurrentPayslip(y: Int, m: Int, d: Int): Boolean {
        // If year and month match selected month, any day <= cutoffDay in this month is included in this payslip.
        // Days > cutoffDay roll over into the next cycle.
        return if (y == cutoffYear && m == cutoffMonth) {
            d <= cutoffDay
        } else if (y < cutoffYear || (y == cutoffYear && m < cutoffMonth)) {
            true
        } else {
            false
        }
    }
}

@Serializable
data class ShiftPayrollSplit(
    val inCycleDays: Int,
    val inCycleHours: Double,
    val inCycleOtHours: Double,
    val inCycleStandardHours: Double,
    val previousRolloverDays: Int = 0,
    val previousRolloverHours: Double = 0.0,
    val previousRolloverOtHours: Double = 0.0,
    val previousRolloverStandardHours: Double = 0.0,
    val rolloverDays: Int,
    val rolloverHours: Double,
    val rolloverOtHours: Double,
    val totalMonthDays: Int,
    val totalMonthHours: Double,
    val cutoffDay: Int,
    val payDay: Int
) {
    val totalPaidDays: Int get() = inCycleDays + previousRolloverDays
    val totalPaidHours: Double get() = inCycleHours + previousRolloverHours
    val totalPaidStandardHours: Double get() = inCycleStandardHours + previousRolloverStandardHours
    val totalPaidOtHours: Double get() = inCycleOtHours + previousRolloverOtHours
}

// CRITICAL: TAX_ENGINE
object PayScheduleEngine {

    /**
     * Computes the exact pay date, cutoff date, and cycle start date for a specific month and year.
     */
    fun calculatePayPeriod(year: Int, month: Int, config: PayScheduleConfig = PayScheduleConfig()): PayPeriodInfo {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        when (config.type) {
            PayScheduleType.LAST_FRIDAY_OF_MONTH -> {
                // Find last Friday of this month
                var lastFriday = maxDays
                for (d in maxDays downTo 1) {
                    cal.set(year, month - 1, d)
                    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                        lastFriday = d
                        break
                    }
                }

                // Cutoff is the Sunday immediately preceding the last Friday (Friday - 5 days)
                cal.set(year, month - 1, lastFriday)
                cal.add(Calendar.DAY_OF_MONTH, -5)
                val cutoffYear = cal.get(Calendar.YEAR)
                val cutoffMonth = cal.get(Calendar.MONTH) + 1
                val cutoffDay = cal.get(Calendar.DAY_OF_MONTH)

                var adjustedPayDay = lastFriday
                var isHolidayAdjusted = false

                // If Dec 25 or Dec 26 falls on the last Friday, bring forward to Wednesday/Thursday
                if (config.adjustForBankHolidays && month == 12) {
                    if (lastFriday == 25 || lastFriday == 26) {
                        adjustedPayDay = 24 // Christmas Eve
                        isHolidayAdjusted = true
                    }
                }

                // Start date is previous month's cutoff + 1 day
                val prevMonth = if (month == 1) 12 else month - 1
                val prevYear = if (month == 1) year - 1 else year
                val prevCutoffCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, prevYear)
                    set(Calendar.MONTH, prevMonth - 1)
                    val prevMax = getActualMaximum(Calendar.DAY_OF_MONTH)
                    var prevFri = prevMax
                    for (d in prevMax downTo 1) {
                        set(prevYear, prevMonth - 1, d)
                        if (get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                            prevFri = d
                            break
                        }
                    }
                    set(prevYear, prevMonth - 1, prevFri)
                    add(Calendar.DAY_OF_MONTH, -5) // previous cutoff Sunday
                    add(Calendar.DAY_OF_MONTH, 1)  // cycle starts next Monday
                }

                return PayPeriodInfo(
                    year = year,
                    month = month,
                    payYear = year,
                    payMonth = month,
                    payDay = adjustedPayDay,
                    cutoffYear = cutoffYear,
                    cutoffMonth = cutoffMonth,
                    cutoffDay = cutoffDay,
                    startYear = prevCutoffCal.get(Calendar.YEAR),
                    startMonth = prevCutoffCal.get(Calendar.MONTH) + 1,
                    startDay = prevCutoffCal.get(Calendar.DAY_OF_MONTH),
                    isBankHolidayAdjusted = isHolidayAdjusted
                )
            }

            PayScheduleType.LAST_WORKING_DAY -> {
                var lastWorkingDay = maxDays
                for (d in maxDays downTo 1) {
                    cal.set(year, month - 1, d)
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                    if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                        lastWorkingDay = d
                        break
                    }
                }
                cal.set(year, month - 1, lastWorkingDay)
                cal.add(Calendar.DAY_OF_MONTH, -config.cutoffLeadDays)
                val cutoffYear = cal.get(Calendar.YEAR)
                val cutoffMonth = cal.get(Calendar.MONTH) + 1
                val cutoffDay = cal.get(Calendar.DAY_OF_MONTH)

                return PayPeriodInfo(
                    year = year,
                    month = month,
                    payYear = year,
                    payMonth = month,
                    payDay = lastWorkingDay,
                    cutoffYear = cutoffYear,
                    cutoffMonth = cutoffMonth,
                    cutoffDay = cutoffDay,
                    startYear = year,
                    startMonth = month,
                    startDay = 1
                )
            }

            PayScheduleType.FIXED_DAY_OF_MONTH -> {
                val payDay = config.fixedDay.coerceIn(1, maxDays)
                cal.set(year, month - 1, payDay)
                cal.add(Calendar.DAY_OF_MONTH, -config.cutoffLeadDays)
                val cutoffYear = cal.get(Calendar.YEAR)
                val cutoffMonth = cal.get(Calendar.MONTH) + 1
                val cutoffDay = cal.get(Calendar.DAY_OF_MONTH)

                return PayPeriodInfo(
                    year = year,
                    month = month,
                    payYear = year,
                    payMonth = month,
                    payDay = payDay,
                    cutoffYear = cutoffYear,
                    cutoffMonth = cutoffMonth,
                    cutoffDay = cutoffDay,
                    startYear = year,
                    startMonth = month,
                    startDay = 1
                )
            }

            else -> {
                // CALENDAR_MONTH, FOUR_WEEKLY, BI_WEEKLY fallback
                return PayPeriodInfo(
                    year = year,
                    month = month,
                    payYear = year,
                    payMonth = month,
                    payDay = maxDays,
                    cutoffYear = year,
                    cutoffMonth = month,
                    cutoffDay = maxDays,
                    startYear = year,
                    startMonth = month,
                    startDay = 1
                )
            }
        }
    }

    /**
     * Splits shift entries for a given month into:
     * 1. Hours falling on or before the Cutoff Date in the current month.
     * 2. Rollover hours brought forward from AFTER the previous month's cutoff date.
     * 3. Hours worked after current month's Cutoff Date (rolling over into the next month's payroll).
     */
    fun calculateShiftPayrollSplit(
        year: Int,
        month: Int,
        currentMonthShifts: Map<Int, Double>,
        previousMonthShifts: Map<Int, Double> = emptyMap(),
        config: PayScheduleConfig = PayScheduleConfig()
    ): ShiftPayrollSplit {
        val payPeriod = calculatePayPeriod(year, month, config)
        val cutoffDay = payPeriod.cutoffDay

        var inCycleDays = 0
        var inCycleHours = 0.0
        var inCycleOtHours = 0.0
        var inCycleStdHours = 0.0

        var rolloverDays = 0
        var rolloverHours = 0.0
        var rolloverOtHours = 0.0

        currentMonthShifts.forEach { (day, hours) ->
            if (hours > 0) {
                val std = minOf(8.0, hours)
                val ot = maxOf(0.0, hours - 8.0)

                if (day <= cutoffDay) {
                    inCycleDays++
                    inCycleHours += hours
                    inCycleStdHours += std
                    inCycleOtHours += ot
                } else {
                    rolloverDays++
                    rolloverHours += hours
                    rolloverOtHours += ot
                }
            }
        }

        // Process previous month's post-cutoff rollover into this month
        var prevRolloverDays = 0
        var prevRolloverHours = 0.0
        var prevRolloverOtHours = 0.0
        var prevRolloverStdHours = 0.0

        if (previousMonthShifts.isNotEmpty()) {
            val prevMonth = if (month == 1) 12 else month - 1
            val prevYear = if (month == 1) year - 1 else year
            val prevPayPeriod = calculatePayPeriod(prevYear, prevMonth, config)
            val prevCutoffDay = prevPayPeriod.cutoffDay

            previousMonthShifts.forEach { (day, hours) ->
                if (day > prevCutoffDay && hours > 0) {
                    val std = minOf(8.0, hours)
                    val ot = maxOf(0.0, hours - 8.0)
                    prevRolloverDays++
                    prevRolloverHours += hours
                    prevRolloverStdHours += std
                    prevRolloverOtHours += ot
                }
            }
        }

        val totalMonthDays = inCycleDays + rolloverDays
        val totalMonthHours = inCycleHours + rolloverHours

        return ShiftPayrollSplit(
            inCycleDays = inCycleDays,
            inCycleHours = inCycleHours,
            inCycleOtHours = inCycleOtHours,
            inCycleStandardHours = inCycleStdHours,
            previousRolloverDays = prevRolloverDays,
            previousRolloverHours = prevRolloverHours,
            previousRolloverOtHours = prevRolloverOtHours,
            previousRolloverStandardHours = prevRolloverStdHours,
            rolloverDays = rolloverDays,
            rolloverHours = rolloverHours,
            rolloverOtHours = rolloverOtHours,
            totalMonthDays = totalMonthDays,
            totalMonthHours = totalMonthHours,
            cutoffDay = cutoffDay,
            payDay = payPeriod.payDay
        )
    }

    /**
     * Calculates the entire 12-month payroll schedule for a given year.
     */
    fun calculateAnnualSchedule(year: Int, config: PayScheduleConfig = PayScheduleConfig()): List<PayPeriodInfo> {
        return (1..12).map { m ->
            calculatePayPeriod(year, m, config)
        }
    }
}
