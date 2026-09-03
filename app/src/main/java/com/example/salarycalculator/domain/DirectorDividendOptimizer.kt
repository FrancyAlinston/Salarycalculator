package com.example.salarycalculator.domain

/**
 * Company Director Remuneration Scenario model.
 */
data class DirectorScenarioResult(
    val name: String,
    val description: String,
    val directorSalary: Double,
    val grossDividends: Double,
    val corporationTax: Double,
    val incomeTax: Double,
    val nationalInsurance: Double,
    val dividendTax: Double,
    val totalTaxBurden: Double,
    val netCashInPocket: Double,
    val effectiveTaxRate: Double
)

/**
 * Full comparative optimization report for company director extraction.
 */
data class DirectorOptimizationReport(
    val companyGrossProfit: Double,
    val scenarios: List<DirectorScenarioResult>,
    val optimalScenario: DirectorScenarioResult,
    val annualTaxSavingsVsPureSalary: Double
)

object DirectorDividendOptimizer {

    /**
     * Calculates Corporation Tax based on UK 2024/2025 & 2025/2026 tiered rules.
     */
    fun calculateCorporationTax(taxableProfit: Double): Double {
        if (taxableProfit <= 0.0) return 0.0
        return when {
            taxableProfit <= 50000.0 -> taxableProfit * 0.19 // Small profits rate
            taxableProfit >= 250000.0 -> taxableProfit * 0.25 // Main rate
            else -> {
                // Marginal Relief Formula: 25% of profit - 3/200 * (250,000 - Profit)
                val standardAt25 = taxableProfit * 0.25
                val marginalRelief = (3.0 / 200.0) * (250000.0 - taxableProfit)
                maxOf(0.0, standardAt25 - marginalRelief)
            }
        }
    }

    /**
     * Calculates UK Dividend Tax on gross dividends given director's salary.
     */
    fun calculateDividendTax(salary: Double, dividends: Double): Double {
        if (dividends <= 0.0) return 0.0

        val personalAllowance = 12570.0
        val dividendAllowance = 500.0 // £500 statutory tax-free dividend allowance

        // Salary uses up personal allowance first
        val remainingPersonalAllowance = maxOf(0.0, personalAllowance - salary)
        val taxableDividendsAfterPA = maxOf(0.0, dividends - remainingPersonalAllowance)

        if (taxableDividendsAfterPA <= 0.0) return 0.0

        // Subtract £500 dividend allowance
        val taxableDividends = maxOf(0.0, taxableDividendsAfterPA - dividendAllowance)
        if (taxableDividends <= 0.0) return 0.0

        val basicRateThreshold = 50270.0 // Higher rate kicks in after £50,270 total income
        val higherRateThreshold = 125140.0 // Additional rate kicks in after £125,140 total income

        val currentTotalIncome = maxOf(personalAllowance, salary)
        val basicBandRoom = maxOf(0.0, basicRateThreshold - currentTotalIncome)

        val dividendsInBasic = minOf(taxableDividends, basicBandRoom)
        val remainingAfterBasic = maxOf(0.0, taxableDividends - dividendsInBasic)

        val higherBandRoom = maxOf(0.0, higherRateThreshold - (currentTotalIncome + dividendsInBasic))
        val dividendsInHigher = minOf(remainingAfterBasic, higherBandRoom)
        val dividendsInAdditional = maxOf(0.0, remainingAfterBasic - dividendsInHigher)

        val taxBasic = dividendsInBasic * 0.0875
        val taxHigher = dividendsInHigher * 0.3375
        val taxAdditional = dividendsInAdditional * 0.3935

        return taxBasic + taxHigher + taxAdditional
    }

    /**
     * Evaluates a specific director salary + dividend extraction mix.
     */
    fun evaluateRemuneration(
        name: String,
        description: String,
        companyGrossProfit: Double,
        salary: Double
    ): DirectorScenarioResult {
        val cappedSalary = minOf(companyGrossProfit, salary)

        // Standard PAYE Income Tax & Employee NI on director's salary
        val taxResult = TaxCalculator.calculateTax(
            grossPay = cappedSalary,
            taxCode = "1257L",
            isMonthly = false,
            region = TaxRegion.UK_STANDARD
        )

        // Company profit after director's allowable salary expense
        val taxableCompanyProfit = maxOf(0.0, companyGrossProfit - cappedSalary)
        val corpTax = calculateCorporationTax(taxableCompanyProfit)

        // All post-tax profit extracted as dividends
        val availableDividends = maxOf(0.0, taxableCompanyProfit - corpTax)
        val divTax = calculateDividendTax(cappedSalary, availableDividends)

        val totalTax = corpTax + taxResult.incomeTax + taxResult.nationalInsurance + divTax
        val netCash = (cappedSalary - taxResult.incomeTax - taxResult.nationalInsurance) + (availableDividends - divTax)
        val effectiveRate = if (companyGrossProfit > 0) (totalTax / companyGrossProfit) * 100.0 else 0.0

        return DirectorScenarioResult(
            name = name,
            description = description,
            directorSalary = cappedSalary,
            grossDividends = availableDividends,
            corporationTax = corpTax,
            incomeTax = taxResult.incomeTax,
            nationalInsurance = taxResult.nationalInsurance,
            dividendTax = divTax,
            totalTaxBurden = totalTax,
            netCashInPocket = netCash,
            effectiveTaxRate = effectiveRate
        )
    }

    /**
     * Generates a full multi-scenario comparison report.
     */
    fun generateOptimizationReport(companyGrossProfit: Double): DirectorOptimizationReport {
        val scenarioOptimal = evaluateRemuneration(
            name = "Optimal Mix (£12,570 Salary)",
            description = "Utilizes 100% tax-free Personal Allowance (£12,570) as allowable company expense, taking remainder as dividends.",
            companyGrossProfit = companyGrossProfit,
            salary = 12570.0
        )

        val scenarioSecondaryNi = evaluateRemuneration(
            name = "Secondary NI Threshold (£9,100 Salary)",
            description = "Zero Employee NI and zero Employer NI liability, extracting balance as dividends.",
            companyGrossProfit = companyGrossProfit,
            salary = 9100.0
        )

        val scenarioPureDividends = evaluateRemuneration(
            name = "100% Dividends (£0 Salary)",
            description = "No PAYE payroll run. Company pays full Corporation Tax before extracting net profits as dividends.",
            companyGrossProfit = companyGrossProfit,
            salary = 0.0
        )

        val scenarioPureSalary = evaluateRemuneration(
            name = "100% PAYE Salary",
            description = "Entire company profit extracted as PAYE salary subject to standard Income Tax and Class 1 NI.",
            companyGrossProfit = companyGrossProfit,
            salary = companyGrossProfit
        )

        val scenarios = listOf(scenarioOptimal, scenarioSecondaryNi, scenarioPureDividends, scenarioPureSalary)
        val bestScenario = scenarios.maxByOrNull { it.netCashInPocket } ?: scenarioOptimal
        val savings = maxOf(0.0, bestScenario.netCashInPocket - scenarioPureSalary.netCashInPocket)

        return DirectorOptimizationReport(
            companyGrossProfit = companyGrossProfit,
            scenarios = scenarios,
            optimalScenario = bestScenario,
            annualTaxSavingsVsPureSalary = savings
        )
    }
}
