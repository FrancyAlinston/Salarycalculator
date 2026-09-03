package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.pow

data class MortgageBorrowingResult(
    val annualGrossIncome: Double,
    val monthlyNetTakeHome: Double,
    val monthlyDebtCommitments: Double,
    val netMonthlyDisposable: Double,
    val selectedMultiplier: Double,
    val maxBorrowingAmount: Double,
    val depositAmount: Double,
    val estimatedPropertyPrice: Double,
    val loanToValuePercentage: Double,
    val annualInterestRatePercent: Double,
    val termYears: Int,
    val estimatedMonthlyRepayment: Double,
    val monthlyDisposableAfterMortgage: Double,
    val repaymentToNetRatioPercent: Double,
    val affordabilityStatus: AffordabilityHealth
)

enum class AffordabilityHealth(val displayName: String) {
    EXCELLENT("Affordability: Comfortable (<35% Net Pay)"),
    MODERATE("Affordability: Moderate (35% - 45% Net Pay)"),
    STRETCHED("Affordability: Stretched (45% - 55% Net Pay)"),
    HIGH_RISK("Affordability: High Risk (>55% Net Pay)")
}

object MortgageBorrowingCalculator {

    fun calculate(
        annualGrossIncome: Double,
        monthlyNetTakeHome: Double,
        depositAmount: Double = 25000.0,
        monthlyDebtCommitments: Double = 0.0,
        selectedMultiplier: Double = 4.5,
        annualInterestRatePercent: Double = 4.5,
        termYears: Int = 30
    ): MortgageBorrowingResult {
        val netDisposable = max(0.0, monthlyNetTakeHome - monthlyDebtCommitments)
        val maxBorrowing = annualGrossIncome * selectedMultiplier
        val totalPropertyPrice = maxBorrowing + depositAmount

        val ltv = if (totalPropertyPrice > 0.0) {
            (maxBorrowing / totalPropertyPrice) * 100.0
        } else {
            0.0
        }

        // Standard Monthly Amortization Formula: M = P * (r * (1 + r)^n) / ((1 + r)^n - 1)
        val monthlyInterestRate = (annualInterestRatePercent / 100.0) / 12.0
        val totalPayments = (termYears * 12).coerceAtLeast(1)

        val monthlyRepayment = if (monthlyInterestRate > 0.0 && maxBorrowing > 0.0) {
            val factor = (1.0 + monthlyInterestRate).pow(totalPayments.toDouble())
            maxBorrowing * (monthlyInterestRate * factor) / (factor - 1.0)
        } else if (totalPayments > 0) {
            maxBorrowing / totalPayments
        } else {
            0.0
        }

        val remainingAfterMortgage = netDisposable - monthlyRepayment
        val repaymentRatio = if (monthlyNetTakeHome > 0.0) {
            (monthlyRepayment / monthlyNetTakeHome) * 100.0
        } else {
            0.0
        }

        val health = when {
            repaymentRatio <= 35.0 -> AffordabilityHealth.EXCELLENT
            repaymentRatio <= 45.0 -> AffordabilityHealth.MODERATE
            repaymentRatio <= 55.0 -> AffordabilityHealth.STRETCHED
            else -> AffordabilityHealth.HIGH_RISK
        }

        return MortgageBorrowingResult(
            annualGrossIncome = annualGrossIncome,
            monthlyNetTakeHome = monthlyNetTakeHome,
            monthlyDebtCommitments = monthlyDebtCommitments,
            netMonthlyDisposable = netDisposable,
            selectedMultiplier = selectedMultiplier,
            maxBorrowingAmount = maxBorrowing,
            depositAmount = depositAmount,
            estimatedPropertyPrice = totalPropertyPrice,
            loanToValuePercentage = ltv,
            annualInterestRatePercent = annualInterestRatePercent,
            termYears = termYears,
            estimatedMonthlyRepayment = monthlyRepayment,
            monthlyDisposableAfterMortgage = remainingAfterMortgage,
            repaymentToNetRatioPercent = repaymentRatio,
            affordabilityStatus = health
        )
    }
}
