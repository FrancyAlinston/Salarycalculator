package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min

enum class StatutoryLeaveType(val displayName: String) {
    SICK_PAY_SSP("Statutory Sick Pay (SSP)"),
    MATERNITY_SMP("Statutory Maternity Pay (SMP)"),
    PATERNITY_SPP("Statutory Paternity Pay (SPP)")
}

data class StatutoryLeaveResult(
    val leaveType: StatutoryLeaveType,
    val regularWeeklyEarnings: Double,
    val durationWeeks: Int,
    val totalStatutoryGross: Double,
    val averageWeeklyStatutoryPay: Double,
    val regularGrossForPeriod: Double,
    val grossIncomeLoss: Double,
    val estimatedTaxDeduction: Double,
    val estimatedNiDeduction: Double,
    val estimatedNetPayForPeriod: Double,
    val regularEstimatedNetForPeriod: Double,
    val weeklyBreakdown: List<WeeklyLeavePayment>
)

data class WeeklyLeavePayment(
    val weekNumber: Int,
    val statutoryAmount: Double,
    val rateDescription: String
)

object StatutoryLeaveCalculator {

    // Statutory rates for 2024/2025 & 2025/2026
    const val SSP_WEEKLY_RATE = 116.75
    const val SMP_STANDARD_WEEKLY_RATE = 184.03
    const val SPP_STANDARD_WEEKLY_RATE = 184.03

    fun calculate(
        leaveType: StatutoryLeaveType,
        averageWeeklyEarnings: Double,
        durationWeeks: Int,
        taxRegion: TaxRegion = TaxRegion.UK_STANDARD
    ): StatutoryLeaveResult {
        val weeks = durationWeeks.coerceIn(1, 52)
        val weeklyBreakdown = mutableListOf<WeeklyLeavePayment>()

        when (leaveType) {
            StatutoryLeaveType.SICK_PAY_SSP -> {
                val maxSspWeeks = min(28, weeks)
                for (w in 1..maxSspWeeks) {
                    val amount = if (w == 1) {
                        // 3 waiting days unpaid in standard 5-day week -> 2 paid days (2/5th of £116.75)
                        SSP_WEEKLY_RATE * (2.0 / 5.0)
                    } else {
                        SSP_WEEKLY_RATE
                    }
                    weeklyBreakdown.add(
                        WeeklyLeavePayment(
                            weekNumber = w,
                            statutoryAmount = amount,
                            rateDescription = if (w == 1) "SSP (First 3 days unpaid waiting)" else "Standard SSP (£116.75/wk)"
                        )
                    )
                }
            }

            StatutoryLeaveType.MATERNITY_SMP -> {
                val maxSmpWeeks = min(39, weeks)
                val ninetyPercentAwe = averageWeeklyEarnings * 0.90
                val standardRate = min(SMP_STANDARD_WEEKLY_RATE, ninetyPercentAwe)

                for (w in 1..maxSmpWeeks) {
                    if (w <= 6) {
                        weeklyBreakdown.add(
                            WeeklyLeavePayment(
                                weekNumber = w,
                                statutoryAmount = ninetyPercentAwe,
                                rateDescription = "Higher Rate (90% Average Weekly Earnings)"
                            )
                        )
                    } else {
                        weeklyBreakdown.add(
                            WeeklyLeavePayment(
                                weekNumber = w,
                                statutoryAmount = standardRate,
                                rateDescription = "Standard Rate (£184.03/wk or 90% AWE)"
                            )
                        )
                    }
                }
            }

            StatutoryLeaveType.PATERNITY_SPP -> {
                val maxSppWeeks = min(2, weeks)
                val standardRate = min(SPP_STANDARD_WEEKLY_RATE, averageWeeklyEarnings * 0.90)

                for (w in 1..maxSppWeeks) {
                    weeklyBreakdown.add(
                        WeeklyLeavePayment(
                            weekNumber = w,
                            statutoryAmount = standardRate,
                            rateDescription = "Standard Paternity Pay (£184.03/wk or 90% AWE)"
                        )
                    )
                }
            }
        }

        val totalStatutoryGross = weeklyBreakdown.sumOf { it.statutoryAmount }
        val regularGrossForPeriod = averageWeeklyEarnings * weeks
        val grossIncomeLoss = max(0.0, regularGrossForPeriod - totalStatutoryGross)
        val avgWeeklyStatutory = if (weeklyBreakdown.isNotEmpty()) totalStatutoryGross / weeklyBreakdown.size else 0.0

        // Estimated annualized extrapolation to compute effective PAYE & NI rates
        val annualizedStatutory = avgWeeklyStatutory * 52.0
        val tempReport = TaxCalculator.calculateTax(
            grossPay = annualizedStatutory / 12.0,
            taxCode = "1257L",
            isMonthly = true,
            region = taxRegion,
            taxYear = TaxYear.YEAR_2024_2025,
            pensionRatePercent = 0.0,
            studentLoanPlan = StudentLoanPlan.NONE
        )

        val effectiveTaxFraction = tempReport.incomeTax / max(1.0, tempReport.grossPay)
        val effectiveNiFraction = tempReport.nationalInsurance / max(1.0, tempReport.grossPay)

        val estimatedTax = totalStatutoryGross * effectiveTaxFraction
        val estimatedNi = totalStatutoryGross * effectiveNiFraction
        val estimatedNet = max(0.0, totalStatutoryGross - estimatedTax - estimatedNi)

        val regularAnnualized = averageWeeklyEarnings * 52.0
        val regularTempReport = TaxCalculator.calculateTax(
            grossPay = regularAnnualized / 12.0,
            taxCode = "1257L",
            isMonthly = true,
            region = taxRegion,
            taxYear = TaxYear.YEAR_2024_2025,
            pensionRatePercent = 0.0,
            studentLoanPlan = StudentLoanPlan.NONE
        )
        val regularNetForPeriod = (regularTempReport.monthlyNet / 4.3333) * weeks

        return StatutoryLeaveResult(
            leaveType = leaveType,
            regularWeeklyEarnings = averageWeeklyEarnings,
            durationWeeks = weeks,
            totalStatutoryGross = totalStatutoryGross,
            averageWeeklyStatutoryPay = avgWeeklyStatutory,
            regularGrossForPeriod = regularGrossForPeriod,
            grossIncomeLoss = grossIncomeLoss,
            estimatedTaxDeduction = estimatedTax,
            estimatedNiDeduction = estimatedNi,
            estimatedNetPayForPeriod = estimatedNet,
            regularEstimatedNetForPeriod = regularNetForPeriod,
            weeklyBreakdown = weeklyBreakdown
        )
    }
}
