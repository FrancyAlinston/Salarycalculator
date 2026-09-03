package com.example.salarycalculator.domain

import kotlin.math.max
import kotlin.math.min

data class SelfEmployedTaxReport(
    val employmentGross: Double,
    val employmentTaxPaid: Double,
    val selfEmployedTurnover: Double,
    val allowableExpenses: Double,
    val tradingAllowanceClaimed: Double,
    val netSelfEmployedProfit: Double,
    val totalTaxableIncome: Double,
    val totalIncomeTaxLiability: Double,
    val remainingIncomeTaxDue: Double,
    val class4Ni: Double,
    val class2Ni: Double,
    val totalSelfAssessmentLiability: Double,
    val paymentsOnAccountRequired: Boolean,
    val firstPaymentOnAccountJan31: Double,
    val secondPaymentOnAccountJul31: Double,
    val totalFirstYearCashOutlayJan31: Double, // Balancing payment + 1st Payment on Account
    val notes: List<String>
)

object SelfEmployedTaxEngine {

    const val TRADING_ALLOWANCE_MAX = 1000.0
    const val CLASS4_LOWER_PROFITS_LIMIT = 12570.0
    const val CLASS4_UPPER_PROFITS_LIMIT = 50270.0
    const val CLASS4_MAIN_RATE = 0.06 // 6% in 2024/25
    const val CLASS4_HIGHER_RATE = 0.02 // 2% above £50,270
    const val PAYMENTS_ON_ACCOUNT_THRESHOLD = 1000.0

    /**
     * Calculates UK Self-Assessment Tax, Class 4 NI, Payments on Account, and Combined PAYE liabilities.
     */
    // CRITICAL: TAX_ENGINE
    fun calculateSelfEmployedTax(
        employmentGross: Double,
        employmentTaxPaid: Double,
        turnover: Double,
        expenses: Double,
        useTradingAllowanceIfBetter: Boolean = true,
        taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
        personalAllowance: Double = 12570.0
    ): SelfEmployedTaxReport {
        val grossTurnover = max(0.0, turnover)
        val claimedExpenses: Double
        val claimedTradingAllowance: Double

        if (useTradingAllowanceIfBetter && expenses < TRADING_ALLOWANCE_MAX && grossTurnover > 0) {
            claimedTradingAllowance = min(grossTurnover, TRADING_ALLOWANCE_MAX)
            claimedExpenses = 0.0
        } else {
            claimedTradingAllowance = 0.0
            claimedExpenses = max(0.0, expenses)
        }

        val netProfit = max(0.0, grossTurnover - claimedExpenses - claimedTradingAllowance)
        val totalGross = max(0.0, employmentGross) + netProfit

        // Calculate Class 4 NI on self-employed profit
        val class4Ni: Double = when {
            netProfit <= CLASS4_LOWER_PROFITS_LIMIT -> 0.0
            netProfit <= CLASS4_UPPER_PROFITS_LIMIT -> (netProfit - CLASS4_LOWER_PROFITS_LIMIT) * CLASS4_MAIN_RATE
            else -> {
                val mainBand = (CLASS4_UPPER_PROFITS_LIMIT - CLASS4_LOWER_PROFITS_LIMIT) * CLASS4_MAIN_RATE
                val upperBand = (netProfit - CLASS4_UPPER_PROFITS_LIMIT) * CLASS4_HIGHER_RATE
                mainBand + upperBand
            }
        }

        val class2Ni = 0.0 // Abolished as mandatory in 2024/25

        // Total Income Tax Liability
        val taxableIncome = max(0.0, totalGross - personalAllowance)
        val totalTaxLiability: Double = when (taxRegion) {
            TaxRegion.SCOTLAND -> calculateScottishIncomeTax(taxableIncome)
            TaxRegion.UK_STANDARD -> calculateUkStandardIncomeTax(taxableIncome)
        }

        val remainingIncomeTaxDue = max(0.0, totalTaxLiability - employmentTaxPaid)
        val totalSaLiability = remainingIncomeTaxDue + class4Ni + class2Ni

        // Payments on Account test:
        // Due if SA bill >= £1,000 and < 80% was collected at source via PAYE
        val isLessThan80PercentPaidAtSource = if (totalTaxLiability > 0) {
            (employmentTaxPaid / totalTaxLiability) < 0.80
        } else false

        val paymentsOnAccountRequired = totalSaLiability >= PAYMENTS_ON_ACCOUNT_THRESHOLD && (employmentGross == 0.0 || isLessThan80PercentPaidAtSource)

        val poaEach = if (paymentsOnAccountRequired) totalSaLiability * 0.50 else 0.0
        val totalJan31Outlay = totalSaLiability + poaEach // Balancing bill for current year + 1st advance payment for next year

        val notes = mutableListOf<String>()
        if (claimedTradingAllowance > 0) {
            notes.add("💼 £${"%,.0f".format(claimedTradingAllowance)} HMRC Trading Allowance automatically claimed (better than actual expenses).")
        }
        if (paymentsOnAccountRequired) {
            notes.add("📅 Payments on Account triggered: 50% (£${"%,.2f".format(poaEach)}) due 31 January and 50% (£${"%,.2f".format(poaEach)}) due 31 July.")
            notes.add("🚨 Total January 31 Cash Outlay: £${"%,.2f".format(totalJan31Outlay)} (balancing bill + 1st payment on account).")
        } else {
            notes.add("✅ No Payments on Account required (total SA liability under £1,000 or >80% paid via PAYE).")
        }

        return SelfEmployedTaxReport(
            employmentGross = employmentGross,
            employmentTaxPaid = employmentTaxPaid,
            selfEmployedTurnover = grossTurnover,
            allowableExpenses = claimedExpenses,
            tradingAllowanceClaimed = claimedTradingAllowance,
            netSelfEmployedProfit = netProfit,
            totalTaxableIncome = taxableIncome,
            totalIncomeTaxLiability = totalTaxLiability,
            remainingIncomeTaxDue = remainingIncomeTaxDue,
            class4Ni = class4Ni,
            class2Ni = class2Ni,
            totalSelfAssessmentLiability = totalSaLiability,
            paymentsOnAccountRequired = paymentsOnAccountRequired,
            firstPaymentOnAccountJan31 = poaEach,
            secondPaymentOnAccountJul31 = poaEach,
            totalFirstYearCashOutlayJan31 = totalJan31Outlay,
            notes = notes
        )
    }

    private fun calculateUkStandardIncomeTax(taxable: Double): Double {
        return when {
            taxable <= 0.0 -> 0.0
            taxable <= 37700.0 -> taxable * 0.20
            taxable <= 125140.0 -> (37700.0 * 0.20) + ((taxable - 37700.0) * 0.40)
            else -> (37700.0 * 0.20) + ((125140.0 - 37700.0) * 0.40) + ((taxable - 125140.0) * 0.45)
        }
    }

    private fun calculateScottishIncomeTax(taxable: Double): Double {
        return when {
            taxable <= 0.0 -> 0.0
            taxable <= 2306.0 -> taxable * 0.19
            taxable <= 13991.0 -> (2306.0 * 0.19) + ((taxable - 2306.0) * 0.20)
            taxable <= 31092.0 -> (2306.0 * 0.19) + ((13991.0 - 2306.0) * 0.20) + ((taxable - 13991.0) * 0.21)
            taxable <= 62430.0 -> (2306.0 * 0.19) + ((13991.0 - 2306.0) * 0.20) + ((31092.0 - 13991.0) * 0.21) + ((taxable - 31092.0) * 0.42)
            taxable <= 125140.0 -> (2306.0 * 0.19) + ((13991.0 - 2306.0) * 0.20) + ((31092.0 - 13991.0) * 0.21) + ((62430.0 - 31092.0) * 0.42) + ((taxable - 62430.0) * 0.45)
            else -> (2306.0 * 0.19) + ((13991.0 - 2306.0) * 0.20) + ((31092.0 - 13991.0) * 0.21) + ((62430.0 - 31092.0) * 0.42) + ((125140.0 - 62430.0) * 0.45) + ((taxable - 125140.0) * 0.48)
        }
    }
}
