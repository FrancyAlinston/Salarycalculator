package com.example.salarycalculator.wear.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class TaxYear(val displayName: String) {
    YEAR_2024_2025("2024 / 2025"),
    YEAR_2025_2026("2025 / 2026")
}

enum class TaxRegion(val displayName: String) {
    UK_STANDARD("England, Wales & NI"),
    SCOTLAND("Scotland (6-Tier)")
}

enum class StudentLoanPlan(val displayName: String, val annualThreshold: Double, val rate: Double) {
    NONE("No Student Loan", 0.0, 0.0),
    PLAN_1("Plan 1 (£24,990 / 9%)", 24990.0, 0.09),
    PLAN_2("Plan 2 (£27,295 / 9%)", 27295.0, 0.09),
    PLAN_4("Plan 4 Scottish (£31,395 / 9%)", 31395.0, 0.09),
    POSTGRADUATE("Postgraduate (£21,000 / 6%)", 21000.0, 0.06)
}

enum class WearDutyState {
    NIGHT_DUTY,
    OVERTIME,
    ANNUAL_LEAVE,
    SICK_LEAVE,
    OFF_DUTY
}

data class WearWeekDayNode(
    val dateKey: String,
    val dayLetter: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val dutyState: WearDutyState,
    val hours: Double
)

data class WearTodayDutySummary(
    val dateKey: String,
    val formattedDate: String,
    val dutyState: WearDutyState,
    val hasDuty: Boolean,
    val title: String,
    val shortStatus: String,
    val badgeLabel: String,
    val subtitle: String,
    val shiftHours: Double,
    val nextDutyFormatted: String,
    val weekDays: List<WearWeekDayNode>,
    val shiftsThisWeek: Int
)

data class WearPayrollSplit(
    val payDay: Int,
    val cutoffDay: Int,
    val previousCutoffDay: Int,
    val inCycleStandardShifts: Int = 0,
    val inCycleOtShifts: Int = 0,
    val inCycleAnnualLeaveDays: Int = 0,
    val inCycleSspDays: Int = 0,
    val inCycleStandardHours: Double = 0.0,
    val inCycleOtHours: Double = 0.0,
    val inCycleAnnualLeaveHours: Double = 0.0,
    val rolloverInShifts: Int = 0,
    val rolloverInStandardHours: Double = 0.0,
    val rolloverInOtHours: Double = 0.0,
    val rolloverInAnnualLeaveHours: Double = 0.0,
    val postCutoffRolloverOutShifts: Int = 0,
    val postCutoffRolloverOutHours: Double = 0.0,
    val calendarNightShifts: Int = 0,
    val calendarOtShifts: Int = 0,
    val calendarAnnualLeaveDays: Int = 0,
    val calendarSspDays: Int = 0,
    val calendarTotalHours: Double = 0.0
) {
    val totalPaidShifts: Int
        get() = inCycleStandardShifts + inCycleOtShifts + inCycleAnnualLeaveDays + rolloverInShifts
    val totalPaidStandardHours: Double
        get() = inCycleStandardHours + rolloverInStandardHours
    val totalPaidOtHours: Double
        get() = inCycleOtHours + rolloverInOtHours
    val totalPaidAnnualLeaveHours: Double
        get() = inCycleAnnualLeaveHours + rolloverInAnnualLeaveHours
    val totalPaidHours: Double
        get() = totalPaidStandardHours + totalPaidOtHours + totalPaidAnnualLeaveHours
    val calendarTotalShifts: Int
        get() = calendarNightShifts + calendarOtShifts + calendarAnnualLeaveDays + calendarSspDays
}

data class SalaryReport(
    val grossPay: Double,
    val standardPay: Double = grossPay,
    val overtimePay: Double = 0.0,
    val annualLeavePay: Double = 0.0,
    val sspPay: Double = 0.0,
    val bonusPay: Double = 0.0,
    val pensionContribution: Double = 0.0,
    val taxablePay: Double = 0.0,
    val incomeTax: Double,
    val nationalInsurance: Double,
    val studentLoanDeduction: Double = 0.0,
    val totalDeductions: Double,
    val netPay: Double,
    val totalHours: Double = 0.0,
    val shiftCount: Int = 0,
    val payrollSplit: WearPayrollSplit? = null
)

object TaxCalculator {

    const val ANNUAL_LEAVE_SENTINEL = -999.0
    const val SSP_SENTINEL = -888.0

    fun classifyShiftState(hours: Double, standardShiftHours: Double = 12.0): WearDutyState {
        return when {
            hours == ANNUAL_LEAVE_SENTINEL -> WearDutyState.ANNUAL_LEAVE
            hours == SSP_SENTINEL -> WearDutyState.SICK_LEAVE
            hours < 0.0 || hours > standardShiftHours -> WearDutyState.OVERTIME
            hours > 0.0 -> WearDutyState.NIGHT_DUTY
            else -> WearDutyState.OFF_DUTY
        }
    }

    /**
     * Evaluates whether a given day has duty or not (showing "Night Duty" on night shift days)
     * along with the 7-day Mon-Sun weekly rota strip and next scheduled shift.
     */
    fun resolveTodayDutySummary(
        date: LocalDate,
        shifts: Map<String, Double>,
        standardShiftHours: Double = 12.0
    ): WearTodayDutySummary {
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dateKey = date.format(isoFormatter)
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
        val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
        val formattedDate = String.format(Locale.ENGLISH, "%s, %02d %s", dayName, date.dayOfMonth, monthName)

        val rawHours = shifts[dateKey] ?: 0.0
        val dutyState = classifyShiftState(rawHours, standardShiftHours)

        // Find next scheduled duty day within the next 45 days
        var nextDutyText = "No upcoming shift logged"
        for (offset in 1..45) {
            val candidate = date.plusDays(offset.toLong())
            val candidateKey = candidate.format(isoFormatter)
            val candidateHours = shifts[candidateKey] ?: 0.0
            val candidateState = classifyShiftState(candidateHours, standardShiftHours)
            if (candidateState == WearDutyState.NIGHT_DUTY || candidateState == WearDutyState.OVERTIME) {
                val cDay = candidate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                val cMonth = candidate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                nextDutyText = if (offset == 1) {
                    "Tomorrow • 20:00"
                } else {
                    String.format(Locale.ENGLISH, "%s, %02d %s", cDay, candidate.dayOfMonth, cMonth)
                }
                break
            }
        }

        // Build Mon..Sun week strip for the week containing `date`
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
        var shiftsThisWeek = 0
        val weekDays = (0..6).map { idx ->
            val d = monday.plusDays(idx.toLong())
            val k = d.format(isoFormatter)
            val h = shifts[k] ?: 0.0
            val st = classifyShiftState(h, standardShiftHours)
            if (st == WearDutyState.NIGHT_DUTY || st == WearDutyState.OVERTIME) {
                shiftsThisWeek++
            }
            WearWeekDayNode(
                dateKey = k,
                dayLetter = dayLetters[idx],
                dayOfMonth = d.dayOfMonth,
                isToday = (d == date),
                dutyState = st,
                hours = h
            )
        }

        return when (dutyState) {
            WearDutyState.NIGHT_DUTY -> WearTodayDutySummary(
                dateKey = dateKey,
                formattedDate = formattedDate,
                dutyState = dutyState,
                hasDuty = true,
                title = "Night Duty",
                shortStatus = "Night Duty",
                badgeLabel = "ON DUTY TONIGHT",
                subtitle = "20:00 – 08:00 • ${rawHours.toInt()}h Shift",
                shiftHours = rawHours,
                nextDutyFormatted = nextDutyText,
                weekDays = weekDays,
                shiftsThisWeek = shiftsThisWeek
            )
            WearDutyState.OVERTIME -> {
                val absH = abs(rawHours).toInt()
                WearTodayDutySummary(
                    dateKey = dateKey,
                    formattedDate = formattedDate,
                    dutyState = dutyState,
                    hasDuty = true,
                    title = "Night Duty",
                    shortStatus = "Night Duty",
                    badgeLabel = "OVERTIME SHIFT • ${absH}H",
                    subtitle = "20:00 – 08:00 • Overtime",
                    shiftHours = abs(rawHours),
                    nextDutyFormatted = nextDutyText,
                    weekDays = weekDays,
                    shiftsThisWeek = shiftsThisWeek
                )
            }
            WearDutyState.ANNUAL_LEAVE -> WearTodayDutySummary(
                dateKey = dateKey,
                formattedDate = formattedDate,
                dutyState = dutyState,
                hasDuty = false,
                title = "Annual Leave",
                shortStatus = "Annual Leave",
                badgeLabel = "PAID LEAVE DAY",
                subtitle = "Next Duty: $nextDutyText",
                shiftHours = standardShiftHours,
                nextDutyFormatted = nextDutyText,
                weekDays = weekDays,
                shiftsThisWeek = shiftsThisWeek
            )
            WearDutyState.SICK_LEAVE -> WearTodayDutySummary(
                dateKey = dateKey,
                formattedDate = formattedDate,
                dutyState = dutyState,
                hasDuty = false,
                title = "Sick Leave",
                shortStatus = "Sick Leave",
                badgeLabel = "STATUTORY SICK LEAVE",
                subtitle = "Next Duty: $nextDutyText",
                shiftHours = 0.0,
                nextDutyFormatted = nextDutyText,
                weekDays = weekDays,
                shiftsThisWeek = shiftsThisWeek
            )
            WearDutyState.OFF_DUTY -> WearTodayDutySummary(
                dateKey = dateKey,
                formattedDate = formattedDate,
                dutyState = dutyState,
                hasDuty = false,
                title = "Off Duty",
                shortStatus = "Off Duty",
                badgeLabel = "REST DAY TODAY",
                subtitle = "Next: $nextDutyText",
                shiftHours = 0.0,
                nextDutyFormatted = nextDutyText,
                weekDays = weekDays,
                shiftsThisWeek = shiftsThisWeek
            )
        }
    }

    // CRITICAL: TAX_ENGINE
    fun parseTaxFreeAllowance(
        taxCode: String,
        isMonthly: Boolean = true,
        taxMonth: Int = 0
    ): Double {
        val upperCode = taxCode.uppercase().trim()
        if (upperCode in listOf("BR", "0T", "D0", "D1")) {
            return 0.0
        }
        val numericPart = upperCode.filter { it.isDigit() }.toIntOrNull() ?: 1257
        val yearlyAllowance = numericPart * 10.0
        if (!isMonthly) return yearlyAllowance

        return if (taxMonth in 1..12 && numericPart == 1257) {
            when (taxMonth) {
                5 -> 1048.42 // Month 5 (August) HMRC Cumulative Table A statutory allowance
                else -> yearlyAllowance / 12.0
            }
        } else {
            yearlyAllowance / 12.0
        }
    }

    /**
     * Computes Last Friday Pay Day and Preceding Sunday Cutoff Day (Friday - 5 days) for a YearMonth.
     */
    fun getPayDayAndCutoff(yearMonth: YearMonth): Pair<Int, Int> {
        val lastDay = yearMonth.atEndOfMonth()
        val lastFridayDate = lastDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
        val lastFriday = lastFridayDate.dayOfMonth
        val cutoffDay = max(1, lastFriday - 5)
        val adjustedPayDay = if (yearMonth.monthValue == 12 && (lastFriday == 25 || lastFriday == 26)) 24 else lastFriday
        return Pair(adjustedPayDay, cutoffDay)
    }

    // CRITICAL: TAX_ENGINE
    fun calculateTax(
        grossPay: Double,
        taxCode: String = "1257L",
        isMonthly: Boolean = true,
        region: TaxRegion = TaxRegion.UK_STANDARD,
        taxYear: TaxYear = TaxYear.YEAR_2024_2025,
        pensionRatePercent: Double = 0.0,
        studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
        totalHours: Double = 0.0,
        shiftCount: Int = 0,
        standardPay: Double = grossPay,
        overtimePay: Double = 0.0,
        annualLeavePay: Double = 0.0,
        sspPay: Double = 0.0,
        taxMonth: Int = 0,
        payrollSplit: WearPayrollSplit? = null
    ): SalaryReport {
        val safeGross = max(0.0, grossPay)
        val periodDivisor = if (isMonthly) 12.0 else 52.0

        // 1. Workplace Pension (Net Pay Arrangement relief reduces taxable pay)
        val pensionAmount = max(0.0, safeGross * (pensionRatePercent / 100.0))
        val postPensionGross = max(0.0, safeGross - pensionAmount)

        // 2. Personal Allowance
        val allowance = parseTaxFreeAllowance(taxCode, isMonthly, taxMonth)
        val taxablePay = max(0.0, postPensionGross - allowance)

        // 3. PAYE Income Tax
        val tax = if (region == TaxRegion.SCOTLAND) {
            calculateScottishTax(taxablePay, periodDivisor)
        } else {
            calculateUkStandardTax(taxablePay, periodDivisor)
        }

        // 4. National Insurance (Class 1 Primary)
        val ni = calculateClass1NI(safeGross, periodDivisor)

        // 5. Student Loan
        val studentLoanDeduction = if (studentLoanPlan != StudentLoanPlan.NONE && safeGross > (studentLoanPlan.annualThreshold / periodDivisor)) {
            val loanThreshold = studentLoanPlan.annualThreshold / periodDivisor
            (safeGross - loanThreshold) * studentLoanPlan.rate
        } else {
            0.0
        }

        val totalDeductions = pensionAmount + tax + ni + studentLoanDeduction
        val netPay = max(0.0, safeGross - totalDeductions)

        return SalaryReport(
            grossPay = safeGross,
            standardPay = standardPay,
            overtimePay = overtimePay,
            annualLeavePay = annualLeavePay,
            sspPay = sspPay,
            pensionContribution = pensionAmount,
            taxablePay = taxablePay,
            incomeTax = tax,
            nationalInsurance = ni,
            studentLoanDeduction = studentLoanDeduction,
            totalDeductions = totalDeductions,
            netPay = netPay,
            totalHours = totalHours,
            shiftCount = shiftCount,
            payrollSplit = payrollSplit
        )
    }

    private fun calculateUkStandardTax(taxable: Double, divisor: Double): Double {
        val basicBand = 37700.0 / divisor
        val higherBand = (125140.0 - 12570.0) / divisor

        var remaining = taxable
        var tax = 0.0

        if (remaining > 0.0) {
            val taxedAtBasic = min(remaining, basicBand)
            tax += taxedAtBasic * 0.20
            remaining -= taxedAtBasic
        }

        if (remaining > 0.0) {
            val taxedAtHigher = min(remaining, higherBand - basicBand)
            tax += taxedAtHigher * 0.40
            remaining -= taxedAtHigher
        }

        if (remaining > 0.0) {
            tax += remaining * 0.45
        }

        return max(0.0, tax)
    }

    private fun calculateScottishTax(taxable: Double, divisor: Double): Double {
        val bStarter = 2306.0 / divisor
        val bBasic = (13991.0 - 2306.0) / divisor
        val bInter = (31092.0 - 13991.0) / divisor
        val bHigher = (62430.0 - 31092.0) / divisor
        val bAdvanced = (125140.0 - 62430.0) / divisor

        var remaining = taxable
        var tax = 0.0

        val tStarter = min(remaining, bStarter); tax += tStarter * 0.19; remaining -= tStarter
        if (remaining > 0.0) { val tBasic = min(remaining, bBasic); tax += tBasic * 0.20; remaining -= tBasic }
        if (remaining > 0.0) { val tInter = min(remaining, bInter); tax += tInter * 0.21; remaining -= tInter }
        if (remaining > 0.0) { val tHigher = min(remaining, bHigher); tax += tHigher * 0.42; remaining -= tHigher }
        if (remaining > 0.0) { val tAdv = min(remaining, bAdvanced); tax += tAdv * 0.45; remaining -= tAdv }
        if (remaining > 0.0) { tax += remaining * 0.48 }

        return max(0.0, tax)
    }

    private fun calculateClass1NI(gross: Double, divisor: Double): Double {
        val pt = 1048.0 * (12.0 / divisor) // £1,048/month
        val uel = 4189.0 * (12.0 / divisor) // £4,189/month

        if (gross <= pt) return 0.0

        var ni = 0.0
        val mainEarnings = min(gross, uel) - pt
        if (mainEarnings > 0.0) {
            ni += mainEarnings * 0.08 // 8% main rate
        }

        if (gross > uel) {
            ni += (gross - uel) * 0.02 // 2% additional rate
        }

        return max(0.0, ni)
    }
}
