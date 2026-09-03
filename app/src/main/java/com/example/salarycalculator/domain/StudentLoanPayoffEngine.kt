package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min

data class StudentLoanPayoffReport(
    val plan: StudentLoanPlan,
    val initialBalance: Double,
    val currentAnnualSalary: Double,
    val statutoryInterestRatePercent: Double,
    val monthlyMandatoryRepayment: Double,
    val monthlyVoluntaryRepayment: Double,
    val totalMonthlyRepayment: Double,
    val estimatedYearsToPayoff: Double,
    val willBeWrittenOff: Boolean,
    val writtenOffBalance: Double,
    val totalRepaidLifetime: Double,
    val totalInterestAccrued: Double,
    val standardRepaidWithoutExtra: Double,
    val totalSavingsFromOverpayment: Double,
    val yearlyTrajectory: List<LoanYearPoint>,
    val strategicRecommendation: String
)

data class LoanYearPoint(
    val year: Int,
    val salary: Double,
    val annualRepayment: Double,
    val interestAccrued: Double,
    val remainingBalance: Double
)

object StudentLoanPayoffEngine {

    /**
     * Calculates UK Student Loan Repayment Horizon, Interest Accrual, and Voluntary Overpayment Strategy.
     */
    // CRITICAL: TAX_ENGINE
    fun calculatePayoffTimeline(
        plan: StudentLoanPlan,
        currentBalance: Double,
        annualSalary: Double,
        annualSalaryGrowthPercent: Double = 2.5,
        extraMonthlyOverpayment: Double = 0.0,
        customInterestRatePercent: Double? = null,
        maxYearsUntilWriteOff: Int = 30
    ): StudentLoanPayoffReport {
        if (plan == StudentLoanPlan.NONE || currentBalance <= 0.0) {
            return StudentLoanPayoffReport(
                plan = plan,
                initialBalance = max(0.0, currentBalance),
                currentAnnualSalary = annualSalary,
                statutoryInterestRatePercent = 0.0,
                monthlyMandatoryRepayment = 0.0,
                monthlyVoluntaryRepayment = 0.0,
                totalMonthlyRepayment = 0.0,
                estimatedYearsToPayoff = 0.0,
                willBeWrittenOff = false,
                writtenOffBalance = 0.0,
                totalRepaidLifetime = 0.0,
                totalInterestAccrued = 0.0,
                standardRepaidWithoutExtra = 0.0,
                totalSavingsFromOverpayment = 0.0,
                yearlyTrajectory = emptyList(),
                strategicRecommendation = "No student loan balance active."
            )
        }

        val interestRate = customInterestRatePercent ?: when (plan) {
            StudentLoanPlan.PLAN_1 -> 6.25 // Prevailing commercial bank base cap
            StudentLoanPlan.PLAN_2 -> 7.7  // RPI + variable
            StudentLoanPlan.PLAN_4 -> 6.25
            StudentLoanPlan.POSTGRADUATE -> 7.7
            else -> 0.0
        }

        // 1. Run simulation with voluntary overpayment
        val simWithExtra = runSimulation(
            plan = plan,
            initialBalance = currentBalance,
            startSalary = annualSalary,
            salaryGrowth = annualSalaryGrowthPercent,
            extraMonthly = extraMonthlyOverpayment,
            interestRatePercent = interestRate,
            maxYears = maxYearsUntilWriteOff
        )

        // 2. Run baseline simulation without extra payment
        val simBaseline = runSimulation(
            plan = plan,
            initialBalance = currentBalance,
            startSalary = annualSalary,
            salaryGrowth = annualSalaryGrowthPercent,
            extraMonthly = 0.0,
            interestRatePercent = interestRate,
            maxYears = maxYearsUntilWriteOff
        )

        val savings = max(0.0, simBaseline.totalRepaid - simWithExtra.totalRepaid)

        val firstMonthMandatory = calculateMonthlyDeduction(plan, annualSalary)
        val firstMonthTotal = firstMonthMandatory + extraMonthlyOverpayment

        val recommendation = when {
            simWithExtra.willBeWrittenOff -> {
                "💡 Statutory Write-Off Likely: Because your projected earnings won't clear the balance before the $maxYearsUntilWriteOff-year cutoff (£${"%,.0f".format(simWithExtra.writtenOffAmount)} written off), extra voluntary payments would cost you MORE overall. Stick to mandatory PAYE deductions."
            }
            extraMonthlyOverpayment > 0 && savings > 1000.0 -> {
                "🚀 Early Payoff Advantage: Voluntary overpayments save £${"%,.0f".format(savings)} in interest and clear debt ${"%.1f".format(simBaseline.yearsTaken - simWithExtra.yearsTaken)} years earlier."
            }
            else -> {
                "📊 Standard Repayment: Your salary trajectory will clear the loan in ~${"%.1f".format(simWithExtra.yearsTaken)} years. Overpayment provides minor interest savings."
            }
        }

        return StudentLoanPayoffReport(
            plan = plan,
            initialBalance = currentBalance,
            currentAnnualSalary = annualSalary,
            statutoryInterestRatePercent = interestRate,
            monthlyMandatoryRepayment = firstMonthMandatory,
            monthlyVoluntaryRepayment = extraMonthlyOverpayment,
            totalMonthlyRepayment = firstMonthTotal,
            estimatedYearsToPayoff = simWithExtra.yearsTaken,
            willBeWrittenOff = simWithExtra.willBeWrittenOff,
            writtenOffBalance = simWithExtra.writtenOffAmount,
            totalRepaidLifetime = simWithExtra.totalRepaid,
            totalInterestAccrued = simWithExtra.totalInterest,
            standardRepaidWithoutExtra = simBaseline.totalRepaid,
            totalSavingsFromOverpayment = savings,
            yearlyTrajectory = simWithExtra.trajectory,
            strategicRecommendation = recommendation
        )
    }

    private fun calculateMonthlyDeduction(plan: StudentLoanPlan, salary: Double): Double {
        val annualExcess = max(0.0, salary - plan.annualThreshold)
        return (annualExcess * plan.rate) / 12.0
    }

    private data class SimulationResult(
        val yearsTaken: Double,
        val totalRepaid: Double,
        val totalInterest: Double,
        val willBeWrittenOff: Boolean,
        val writtenOffAmount: Double,
        val trajectory: List<LoanYearPoint>
    )

    private fun runSimulation(
        plan: StudentLoanPlan,
        initialBalance: Double,
        startSalary: Double,
        salaryGrowth: Double,
        extraMonthly: Double,
        interestRatePercent: Double,
        maxYears: Int
    ): SimulationResult {
        var balance = initialBalance
        var salary = startSalary
        var totalRepaid = 0.0
        var totalInterest = 0.0
        var monthCount = 0
        val trajectory = mutableListOf<LoanYearPoint>()

        for (year in 1..maxYears) {
            var yearlyRepaid = 0.0
            var yearlyInterest = 0.0

            for (m in 1..12) {
                if (balance <= 0.0) break

                val monthlyInterest = balance * (interestRatePercent / 100.0) / 12.0
                balance += monthlyInterest
                totalInterest += monthlyInterest
                yearlyInterest += monthlyInterest

                val mandatoryDeduction = calculateMonthlyDeduction(plan, salary)
                val totalPayment = min(balance, mandatoryDeduction + extraMonthly)

                balance = max(0.0, balance - totalPayment)
                totalRepaid += totalPayment
                yearlyRepaid += totalPayment
                monthCount++
            }

            trajectory.add(
                LoanYearPoint(
                    year = year,
                    salary = salary,
                    annualRepayment = yearlyRepaid,
                    interestAccrued = yearlyInterest,
                    remainingBalance = balance
                )
            )

            if (balance <= 0.0) break
            salary *= (1.0 + (salaryGrowth / 100.0))
        }

        val willBeWrittenOff = balance > 0.0
        val writtenOff = if (willBeWrittenOff) balance else 0.0
        val yearsTaken = if (willBeWrittenOff) maxYears.toDouble() else monthCount / 12.0

        return SimulationResult(
            yearsTaken = yearsTaken,
            totalRepaid = totalRepaid,
            totalInterest = totalInterest,
            willBeWrittenOff = willBeWrittenOff,
            writtenOffAmount = writtenOff,
            trajectory = trajectory
        )
    }
}
