package com.example.salarycalculator.domain

import kotlin.math.max

// CRITICAL: TAX_ENGINE
/**
 * Represents individual job input details for multi-job calculation.
 */
data class JobInput(
    val title: String,
    val grossSalary: Double,
    val isAnnual: Boolean = true,
    val taxCode: String = "1257L",
    val pensionPercentage: Double = 5.0,
    val isSalarySacrifice: Boolean = false,
    val studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
    val employerPensionPercentage: Double = 3.0
)

/**
 * Breakdown of deductions and net pay for an individual job.
 */
data class IndividualJobBreakdown(
    val title: String,
    val annualGross: Double,
    val monthlyGross: Double,
    val annualTax: Double,
    val monthlyTax: Double,
    val annualNi: Double,
    val monthlyNi: Double,
    val annualPension: Double,
    val monthlyPension: Double,
    val annualEmployerPension: Double,
    val annualStudentLoan: Double,
    val monthlyStudentLoan: Double,
    val annualNet: Double,
    val monthlyNet: Double,
    val effectiveTaxRate: Double
)

/**
 * Aggregated summary across all jobs.
 */
data class MultiJobSummary(
    val jobs: List<IndividualJobBreakdown>,
    val totalAnnualGross: Double,
    val totalMonthlyGross: Double,
    val totalAnnualTax: Double,
    val totalMonthlyTax: Double,
    val totalAnnualNi: Double,
    val totalMonthlyNi: Double,
    val totalAnnualPension: Double,
    val totalMonthlyPension: Double,
    val totalAnnualEmployerPension: Double,
    val totalAnnualStudentLoan: Double,
    val totalMonthlyStudentLoan: Double,
    val totalAnnualNet: Double,
    val totalMonthlyNet: Double,
    val overallEffectiveRate: Double,
    val overallTakeHomePercentage: Double,
    val isMarginalTrapReached: Boolean,
    val personalAllowanceTaperLoss: Double
)

object MultiJobAggregatorEngine {

    // CRITICAL: TAX_ENGINE
    /**
     * Calculates combined multi-job salary, PAYE income tax, Class 1 NI, and net take-home pay.
     * Rules:
     * 1. Class 1 Primary NI is assessed independently per distinct employment (each employer applies the £12,576 threshold).
     * 2. PAYE Income Tax is assessed on the combined cumulative taxable income across all employments.
     * 3. Personal allowance tapering (£100,000 - £125,140) evaluates total aggregated taxable income.
     */
    fun calculateMultiJob(
        jobs: List<JobInput>,
        taxYear: TaxYear = TaxYear.YEAR_2024_2025,
        region: TaxRegion = TaxRegion.UK_STANDARD
    ): MultiJobSummary {
        if (jobs.isEmpty()) {
            return MultiJobSummary(
                jobs = emptyList(),
                totalAnnualGross = 0.0,
                totalMonthlyGross = 0.0,
                totalAnnualTax = 0.0,
                totalMonthlyTax = 0.0,
                totalAnnualNi = 0.0,
                totalMonthlyNi = 0.0,
                totalAnnualPension = 0.0,
                totalMonthlyPension = 0.0,
                totalAnnualEmployerPension = 0.0,
                totalAnnualStudentLoan = 0.0,
                totalMonthlyStudentLoan = 0.0,
                totalAnnualNet = 0.0,
                totalMonthlyNet = 0.0,
                overallEffectiveRate = 0.0,
                overallTakeHomePercentage = 0.0,
                isMarginalTrapReached = false,
                personalAllowanceTaperLoss = 0.0
            )
        }

        // Standardize annual gross & compute per-job NI, Pension, and Student Loan
        val annualGrossList = jobs.map { if (it.isAnnual) it.grossSalary else it.grossSalary * 12.0 }
        val totalAnnualGross = annualGrossList.sum()

        // 1. Calculate independent Class 1 NI per job
        val perJobNi = jobs.mapIndexed { idx, job ->
            val gross = annualGrossList[idx]
            val salarySacrificeAmount = if (job.isSalarySacrifice) gross * (job.pensionPercentage / 100.0) else 0.0
            val niGross = max(0.0, gross - salarySacrificeAmount)
            calculateAnnualClass1Ni(niGross)
        }

        // 2. Calculate per-job Pension Contributions
        val perJobEmployeePension = jobs.mapIndexed { idx, job ->
            val gross = annualGrossList[idx]
            gross * (job.pensionPercentage / 100.0)
        }
        val perJobEmployerPension = jobs.mapIndexed { idx, job ->
            val gross = annualGrossList[idx]
            gross * (job.employerPensionPercentage / 100.0)
        }

        // 3. Calculate per-job Student Loan (assessed per employment threshold under PAYE)
        val perJobStudentLoan = jobs.mapIndexed { idx, job ->
            val gross = annualGrossList[idx]
            if (job.studentLoanPlan != StudentLoanPlan.NONE && gross > job.studentLoanPlan.annualThreshold) {
                (gross - job.studentLoanPlan.annualThreshold) * job.studentLoanPlan.rate
            } else {
                0.0
            }
        }

        // 4. Calculate Combined Taxable Income & Personal Allowance
        // Evaluate primary personal allowance (Job 1 code or largest allowance code)
        var primaryAllowance = TaxCalculator.parseTaxFreeAllowance(jobs.first().taxCode, isMonthly = false)
        
        // Personal Allowance Tapering: £1 lost for every £2 earned above £100,000
        val personalAllowanceTaperLoss = if (totalAnnualGross > 100000.0) {
            val excess = totalAnnualGross - 100000.0
            val reduction = excess / 2.0
            minOf(primaryAllowance, reduction)
        } else {
            0.0
        }
        val effectiveAllowance = max(0.0, primaryAllowance - personalAllowanceTaperLoss)

        // Pre-tax pension deductions across all jobs
        val totalPensionRelief = jobs.indices.sumOf { idx ->
            if (jobs[idx].isSalarySacrifice) {
                perJobEmployeePension[idx]
            } else {
                perJobEmployeePension[idx] // Net pay relief deduction
            }
        }

        val totalTaxableIncome = max(0.0, totalAnnualGross - totalPensionRelief - effectiveAllowance)

        // 5. Calculate Combined PAYE Income Tax across UK or Scottish bands
        val totalAnnualTax = calculateAnnualIncomeTax(totalTaxableIncome, region)

        // Distribute tax between jobs proportionally or sequentially (Primary uses allowance, secondary pays marginal rate)
        val perJobTax = distributeTaxAcrossJobs(jobs, annualGrossList, totalAnnualTax, effectiveAllowance)

        // Construct individual breakdowns
        val breakdowns = jobs.indices.map { idx ->
            val gross = annualGrossList[idx]
            val tax = perJobTax[idx]
            val ni = perJobNi[idx]
            val pension = perJobEmployeePension[idx]
            val sl = perJobStudentLoan[idx]
            val net = max(0.0, gross - tax - ni - pension - sl)
            val effRate = if (gross > 0.0) ((tax + ni) / gross) * 100.0 else 0.0

            IndividualJobBreakdown(
                title = jobs[idx].title,
                annualGross = gross,
                monthlyGross = gross / 12.0,
                annualTax = tax,
                monthlyTax = tax / 12.0,
                annualNi = ni,
                monthlyNi = ni / 12.0,
                annualPension = pension,
                monthlyPension = pension / 12.0,
                annualEmployerPension = perJobEmployerPension[idx],
                annualStudentLoan = sl,
                monthlyStudentLoan = sl / 12.0,
                annualNet = net,
                monthlyNet = net / 12.0,
                effectiveTaxRate = effRate
            )
        }

        val totalAnnualNi = perJobNi.sum()
        val totalAnnualPension = perJobEmployeePension.sum()
        val totalAnnualEmployerPension = perJobEmployerPension.sum()
        val totalAnnualStudentLoan = perJobStudentLoan.sum()
        val totalAnnualNet = breakdowns.sumOf { it.annualNet }
        val overallEffectiveRate = if (totalAnnualGross > 0.0) ((totalAnnualTax + totalAnnualNi) / totalAnnualGross) * 100.0 else 0.0
        val overallTakeHomePercentage = if (totalAnnualGross > 0.0) (totalAnnualNet / totalAnnualGross) * 100.0 else 0.0
        val isMarginalTrap = totalAnnualGross in 100000.0..125140.0

        return MultiJobSummary(
            jobs = breakdowns,
            totalAnnualGross = totalAnnualGross,
            totalMonthlyGross = totalAnnualGross / 12.0,
            totalAnnualTax = totalAnnualTax,
            totalMonthlyTax = totalAnnualTax / 12.0,
            totalAnnualNi = totalAnnualNi,
            totalMonthlyNi = totalAnnualNi / 12.0,
            totalAnnualPension = totalAnnualPension,
            totalMonthlyPension = totalAnnualPension / 12.0,
            totalAnnualEmployerPension = totalAnnualEmployerPension,
            totalAnnualStudentLoan = totalAnnualStudentLoan,
            totalMonthlyStudentLoan = totalAnnualStudentLoan / 12.0,
            totalAnnualNet = totalAnnualNet,
            totalMonthlyNet = totalAnnualNet / 12.0,
            overallEffectiveRate = overallEffectiveRate,
            overallTakeHomePercentage = overallTakeHomePercentage,
            isMarginalTrapReached = isMarginalTrap,
            personalAllowanceTaperLoss = personalAllowanceTaperLoss
        )
    }

    private fun calculateAnnualClass1Ni(annualGross: Double): Double {
        val primaryThreshold = 12576.0
        val upperEarningsLimit = 50268.0

        if (annualGross <= primaryThreshold) return 0.0

        val mainBandEarnings = minOf(annualGross, upperEarningsLimit) - primaryThreshold
        val mainNi = mainBandEarnings * 0.08 // 8% main rate

        val upperBandEarnings = max(0.0, annualGross - upperEarningsLimit)
        val upperNi = upperBandEarnings * 0.02 // 2% additional rate

        return mainNi + upperNi
    }

    private fun calculateAnnualIncomeTax(taxableIncome: Double, region: TaxRegion): Double {
        if (taxableIncome <= 0.0) return 0.0

        return if (region == TaxRegion.SCOTLAND) {
            // Scottish 6-Tier System
            val starterBand = minOf(taxableIncome, 2306.0)
            val basicBand = if (taxableIncome > 2306.0) minOf(taxableIncome - 2306.0, 11685.0) else 0.0
            val intermediateBand = if (taxableIncome > 13991.0) minOf(taxableIncome - 13991.0, 17103.0) else 0.0
            val higherBand = if (taxableIncome > 31094.0) minOf(taxableIncome - 31094.0, 31336.0) else 0.0
            val advancedBand = if (taxableIncome > 62430.0) minOf(taxableIncome - 62430.0, 62710.0) else 0.0
            val topBand = if (taxableIncome > 125140.0) taxableIncome - 125140.0 else 0.0

            (starterBand * 0.19) +
            (basicBand * 0.20) +
            (intermediateBand * 0.21) +
            (higherBand * 0.42) +
            (advancedBand * 0.45) +
            (topBand * 0.48)
        } else {
            // Standard UK 3-Tier System
            val basicBand = minOf(taxableIncome, 37700.0)
            val higherBand = if (taxableIncome > 37700.0) minOf(taxableIncome - 37700.0, 87440.0) else 0.0
            val additionalBand = if (taxableIncome > 125140.0) taxableIncome - 125140.0 else 0.0

            (basicBand * 0.20) +
            (higherBand * 0.40) +
            (additionalBand * 0.45)
        }
    }

    private fun distributeTaxAcrossJobs(
        jobs: List<JobInput>,
        annualGrossList: List<Double>,
        totalTax: Double,
        allowance: Double
    ): List<Double> {
        if (jobs.size == 1) return listOf(totalTax)
        if (totalTax <= 0.0) return jobs.map { 0.0 }

        // Job 1 absorbs the primary tax code allowance; Job 2 has flat BR / D0 or sequential taxation
        val job1Gross = annualGrossList[0]
        val job1Taxable = max(0.0, job1Gross - allowance)
        val job1Tax = minOf(totalTax, calculateAnnualIncomeTax(job1Taxable, TaxRegion.UK_STANDARD))
        val remainingTax = max(0.0, totalTax - job1Tax)

        val result = mutableListOf(job1Tax)
        val otherGrossSum = annualGrossList.drop(1).sum()

        for (i in 1 until jobs.size) {
            val proportion = if (otherGrossSum > 0.0) annualGrossList[i] / otherGrossSum else 1.0 / (jobs.size - 1)
            result.add(remainingTax * proportion)
        }

        return result
    }
}
