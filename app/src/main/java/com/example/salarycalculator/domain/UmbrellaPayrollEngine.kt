package com.example.salarycalculator.domain

import kotlin.math.max

// CRITICAL: TAX_ENGINE
data class UmbrellaPayrollInput(
    val rateType: UmbrellaRateType = UmbrellaRateType.DAILY,
    val rateAmount: Double = 450.0,
    val unitsWorkedPerWeek: Double = 5.0, // 5 days or 37.5 hours
    val weeksWorkedPerYear: Double = 48.0,
    val umbrellaMarginWeekly: Double = 25.0,
    val taxYear: TaxYear = TaxYear.YEAR_2025_2026,
    val taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    val taxCode: String = "1257L",
    val employeePensionPercentage: Double = 5.0,
    val employerPensionPercentage: Double = 3.0,
    val studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
    val isApprenticeshipLevyApplicable: Boolean = true
)

enum class UmbrellaRateType(val displayName: String) {
    DAILY("Daily Rate (£/day)"),
    HOURLY("Hourly Rate (£/hour)")
}

data class UmbrellaPayrollResult(
    // Revenue Invoiced
    val annualInvoicedRevenue: Double,
    val monthlyInvoicedRevenue: Double,
    val weeklyInvoicedRevenue: Double,

    // Company / Employer Deductions
    val annualUmbrellaMargin: Double,
    val monthlyUmbrellaMargin: Double,
    val annualApprenticeshipLevy: Double,
    val monthlyApprenticeshipLevy: Double,
    val annualEmployerNi: Double,
    val monthlyEmployerNi: Double,
    val annualEmployerPension: Double,
    val monthlyEmployerPension: Double,
    val totalAnnualCompanyDeductions: Double,
    val totalMonthlyCompanyDeductions: Double,

    // Resulting Employee Gross Pay
    val annualGrossPay: Double,
    val monthlyGrossPay: Double,
    val weeklyGrossPay: Double,

    // Employee PAYE Deductions
    val annualEmployeeTax: Double,
    val monthlyEmployeeTax: Double,
    val annualEmployeeNi: Double,
    val monthlyEmployeeNi: Double,
    val annualEmployeePension: Double,
    val monthlyEmployeePension: Double,
    val annualStudentLoan: Double,
    val monthlyStudentLoan: Double,
    val totalAnnualEmployeeDeductions: Double,
    val totalMonthlyEmployeeDeductions: Double,

    // Final Net Take-Home Pay
    val annualNetPay: Double,
    val monthlyNetPay: Double,
    val weeklyNetPay: Double,
    val dailyNetPay: Double,

    // Retention Metrics
    val netRetentionPercentage: Double,
    val totalTaxBurdenPercentage: Double,
    val employerNiRateUsed: Double,
    val employerNiThresholdUsed: Double
)

object UmbrellaPayrollEngine {

    // CRITICAL: TAX_ENGINE
    /**
     * Calculates umbrella contractor take-home pay, employer on-costs, and retention metrics.
     */
    fun calculate(input: UmbrellaPayrollInput): UmbrellaPayrollResult {
        // 1. Calculate Gross Invoiced Revenue
        val weeklyInvoicedRevenue = when (input.rateType) {
            UmbrellaRateType.DAILY -> input.rateAmount * input.unitsWorkedPerWeek
            UmbrellaRateType.HOURLY -> input.rateAmount * input.unitsWorkedPerWeek
        }
        val annualInvoicedRevenue = weeklyInvoicedRevenue * input.weeksWorkedPerYear
        val monthlyInvoicedRevenue = annualInvoicedRevenue / 12.0

        if (annualInvoicedRevenue <= 0.0) {
            return UmbrellaPayrollResult(
                annualInvoicedRevenue = 0.0,
                monthlyInvoicedRevenue = 0.0,
                weeklyInvoicedRevenue = 0.0,
                annualUmbrellaMargin = 0.0,
                monthlyUmbrellaMargin = 0.0,
                annualApprenticeshipLevy = 0.0,
                monthlyApprenticeshipLevy = 0.0,
                annualEmployerNi = 0.0,
                monthlyEmployerNi = 0.0,
                annualEmployerPension = 0.0,
                monthlyEmployerPension = 0.0,
                totalAnnualCompanyDeductions = 0.0,
                totalMonthlyCompanyDeductions = 0.0,
                annualGrossPay = 0.0,
                monthlyGrossPay = 0.0,
                weeklyGrossPay = 0.0,
                annualEmployeeTax = 0.0,
                monthlyEmployeeTax = 0.0,
                annualEmployeeNi = 0.0,
                monthlyEmployeeNi = 0.0,
                annualEmployeePension = 0.0,
                monthlyEmployeePension = 0.0,
                annualStudentLoan = 0.0,
                monthlyStudentLoan = 0.0,
                totalAnnualEmployeeDeductions = 0.0,
                totalMonthlyEmployeeDeductions = 0.0,
                annualNetPay = 0.0,
                monthlyNetPay = 0.0,
                weeklyNetPay = 0.0,
                dailyNetPay = 0.0,
                netRetentionPercentage = 0.0,
                totalTaxBurdenPercentage = 0.0,
                employerNiRateUsed = 0.15,
                employerNiThresholdUsed = 5000.0
            )
        }

        // 2. Umbrella Company Margin
        val annualMargin = input.umbrellaMarginWeekly * input.weeksWorkedPerYear
        val monthlyMargin = annualMargin / 12.0

        // 3. Statutory Employer Thresholds & Rates (2025/2026 vs 2024/2025)
        val (employerNiRate, employerNiThreshold) = when (input.taxYear) {
            TaxYear.YEAR_2025_2026 -> Pair(0.150, 5000.0) // 15.0% above £5,000 from April 2025
            TaxYear.YEAR_2024_2025 -> Pair(0.138, 9100.0) // 13.8% above £9,100
        }

        val levyRate = if (input.isApprenticeshipLevyApplicable) 0.005 else 0.0 // 0.5% Apprenticeship Levy

        // Solve for Employee Gross Pay G:
        // Revenue - Margin = G + EmployerNI(G) + Levy(G) + EmployerPension(G)
        // EmployerNI(G) = max(0, G - Threshold) * NiRate
        // Levy(G) = G * 0.005
        // EmployerPension(G) = max(0, min(G, 50270) - 6240) * 0.03
        val availableFund = max(0.0, annualInvoicedRevenue - annualMargin)

        // Iterative precision solver to match exact penny payroll calculation
        var low = 0.0
        var high = availableFund
        var solvedGross = availableFund / (1.0 + employerNiRate + levyRate + 0.03)

        for (iter in 0..50) {
            val candidateGross = (low + high) / 2.0
            val candidateEmployerNi = if (candidateGross > employerNiThreshold) {
                (candidateGross - employerNiThreshold) * employerNiRate
            } else {
                0.0
            }
            val candidateLevy = candidateGross * levyRate
            val candidatePensionEarnings = if (candidateGross > 6240.0) minOf(candidateGross, 50270.0) - 6240.0 else 0.0
            val candidateEmployerPension = candidatePensionEarnings * (input.employerPensionPercentage / 100.0)

            val totalCompanyCosts = candidateGross + candidateEmployerNi + candidateLevy + candidateEmployerPension

            if (totalCompanyCosts < availableFund) {
                low = candidateGross
            } else {
                high = candidateGross
            }
            solvedGross = candidateGross
        }

        val annualGrossPay = solvedGross
        val monthlyGrossPay = annualGrossPay / 12.0
        val weeklyGrossPay = annualGrossPay / input.weeksWorkedPerYear

        val annualEmployerNi = if (annualGrossPay > employerNiThreshold) (annualGrossPay - employerNiThreshold) * employerNiRate else 0.0
        val monthlyEmployerNi = annualEmployerNi / 12.0

        val annualLevy = annualGrossPay * levyRate
        val monthlyLevy = annualLevy / 12.0

        val pensionQualifyingEarnings = if (annualGrossPay > 6240.0) minOf(annualGrossPay, 50270.0) - 6240.0 else 0.0
        val annualEmployerPension = pensionQualifyingEarnings * (input.employerPensionPercentage / 100.0)
        val monthlyEmployerPension = annualEmployerPension / 12.0

        val totalCompanyDeductions = annualMargin + annualEmployerNi + annualLevy + annualEmployerPension

        // 4. Employee PAYE Taxes
        val annualEmployeePension = pensionQualifyingEarnings * (input.employeePensionPercentage / 100.0)
        val monthlyEmployeePension = annualEmployeePension / 12.0

        // Personal Allowance & Taxable Income
        val personalAllowance = TaxCalculator.parseTaxFreeAllowance(input.taxCode, isMonthly = false)
        val taperLoss = if (annualGrossPay > 100000.0) minOf(personalAllowance, (annualGrossPay - 100000.0) / 2.0) else 0.0
        val effectiveAllowance = max(0.0, personalAllowance - taperLoss)

        val taxableIncome = max(0.0, annualGrossPay - annualEmployeePension - effectiveAllowance)
        val annualEmployeeTax = calculateIncomeTax(taxableIncome, input.taxRegion)
        val monthlyEmployeeTax = annualEmployeeTax / 12.0

        // Employee Class 1 NI (8% between £12,576 and £50,268; 2% above)
        val annualEmployeeNi = calculateEmployeeNi(annualGrossPay)
        val monthlyEmployeeNi = annualEmployeeNi / 12.0

        // Student Loan
        val annualStudentLoan = if (input.studentLoanPlan != StudentLoanPlan.NONE && annualGrossPay > input.studentLoanPlan.annualThreshold) {
            (annualGrossPay - input.studentLoanPlan.annualThreshold) * input.studentLoanPlan.rate
        } else {
            0.0
        }
        val monthlyStudentLoan = annualStudentLoan / 12.0

        val totalEmployeeDeductions = annualEmployeeTax + annualEmployeeNi + annualEmployeePension + annualStudentLoan

        // 5. Net Take-Home Pay
        val annualNetPay = max(0.0, annualGrossPay - totalEmployeeDeductions)
        val monthlyNetPay = annualNetPay / 12.0
        val weeklyNetPay = annualNetPay / input.weeksWorkedPerYear
        val dailyNetPay = if (input.unitsWorkedPerWeek > 0.0) weeklyNetPay / input.unitsWorkedPerWeek else 0.0

        val retentionPercentage = if (annualInvoicedRevenue > 0.0) (annualNetPay / annualInvoicedRevenue) * 100.0 else 0.0
        val totalTaxBurden = annualInvoicedRevenue - annualNetPay - annualMargin
        val totalTaxBurdenPercentage = if (annualInvoicedRevenue > 0.0) (totalTaxBurden / annualInvoicedRevenue) * 100.0 else 0.0

        return UmbrellaPayrollResult(
            annualInvoicedRevenue = annualInvoicedRevenue,
            monthlyInvoicedRevenue = monthlyInvoicedRevenue,
            weeklyInvoicedRevenue = weeklyInvoicedRevenue,
            annualUmbrellaMargin = annualMargin,
            monthlyUmbrellaMargin = monthlyMargin,
            annualApprenticeshipLevy = annualLevy,
            monthlyApprenticeshipLevy = monthlyLevy,
            annualEmployerNi = annualEmployerNi,
            monthlyEmployerNi = monthlyEmployerNi,
            annualEmployerPension = annualEmployerPension,
            monthlyEmployerPension = monthlyEmployerPension,
            totalAnnualCompanyDeductions = totalCompanyDeductions,
            totalMonthlyCompanyDeductions = totalCompanyDeductions / 12.0,
            annualGrossPay = annualGrossPay,
            monthlyGrossPay = monthlyGrossPay,
            weeklyGrossPay = weeklyGrossPay,
            annualEmployeeTax = annualEmployeeTax,
            monthlyEmployeeTax = monthlyEmployeeTax,
            annualEmployeeNi = annualEmployeeNi,
            monthlyEmployeeNi = monthlyEmployeeNi,
            annualEmployeePension = annualEmployeePension,
            monthlyEmployeePension = monthlyEmployeePension,
            annualStudentLoan = annualStudentLoan,
            monthlyStudentLoan = monthlyStudentLoan,
            totalAnnualEmployeeDeductions = totalEmployeeDeductions,
            totalMonthlyEmployeeDeductions = totalEmployeeDeductions / 12.0,
            annualNetPay = annualNetPay,
            monthlyNetPay = monthlyNetPay,
            weeklyNetPay = weeklyNetPay,
            dailyNetPay = dailyNetPay,
            netRetentionPercentage = retentionPercentage,
            totalTaxBurdenPercentage = totalTaxBurdenPercentage,
            employerNiRateUsed = employerNiRate,
            employerNiThresholdUsed = employerNiThreshold
        )
    }

    private fun calculateEmployeeNi(annualGross: Double): Double {
        val pt = 12576.0
        val uel = 50268.0

        if (annualGross <= pt) return 0.0

        val mainBand = minOf(annualGross, uel) - pt
        val mainNi = mainBand * 0.08

        val upperBand = max(0.0, annualGross - uel)
        val upperNi = upperBand * 0.02

        return mainNi + upperNi
    }

    private fun calculateIncomeTax(taxableIncome: Double, region: TaxRegion): Double {
        if (taxableIncome <= 0.0) return 0.0

        return if (region == TaxRegion.SCOTLAND) {
            val starterBand = minOf(taxableIncome, 2306.0)
            val basicBand = if (taxableIncome > 2306.0) minOf(taxableIncome - 2306.0, 11685.0) else 0.0
            val intermediateBand = if (taxableIncome > 13991.0) minOf(taxableIncome - 13991.0, 17103.0) else 0.0
            val higherBand = if (taxableIncome > 31094.0) minOf(taxableIncome - 31094.0, 31336.0) else 0.0
            val advancedBand = if (taxableIncome > 62430.0) minOf(taxableIncome - 62430.0, 62710.0) else 0.0
            val topBand = if (taxableIncome > 125140.0) taxableIncome - 125140.0 else 0.0

            (starterBand * 0.19) + (basicBand * 0.20) + (intermediateBand * 0.21) + (higherBand * 0.42) + (advancedBand * 0.45) + (topBand * 0.48)
        } else {
            val basicBand = minOf(taxableIncome, 37700.0)
            val higherBand = if (taxableIncome > 37700.0) minOf(taxableIncome - 37700.0, 87440.0) else 0.0
            val additionalBand = if (taxableIncome > 125140.0) taxableIncome - 125140.0 else 0.0

            (basicBand * 0.20) + (higherBand * 0.40) + (additionalBand * 0.45)
        }
    }
}
