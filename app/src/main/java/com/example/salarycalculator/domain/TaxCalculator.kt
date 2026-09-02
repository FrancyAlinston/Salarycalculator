package com.example.salarycalculator.domain

import kotlin.math.max

data class SalaryReport(
    val grossPay: Double,
    val taxablePay: Double,
    val incomeTax: Double,
    val nationalInsurance: Double,
    val netPay: Double
)

object TaxCalculator {

    // CRITICAL: TAX_ENGINE
    /**
     * Parses tax-free allowance from standard UK Tax codes (e.g., "1257L" -> £12,570/year).
     * // EDGE_CASE: Handles special tax codes (BR, 0T, etc.)
     */
    fun parseTaxFreeAllowance(taxCode: String, isMonthly: Boolean = true): Double {
        val upperCode = taxCode.uppercase().trim()
        
        // Special flat/zero allowance codes
        if (upperCode == "BR" || upperCode == "0T" || upperCode == "D0" || upperCode == "D1") {
            // RULE VIOLATION: NON_STANDARD_CODE
            return 0.0
        }

        val numericPart = upperCode.filter { it.isDigit() }.toIntOrNull() ?: 1257
        val yearlyAllowance = numericPart * 10.0
        return if (isMonthly) yearlyAllowance / 12 else yearlyAllowance
    }

    /**
     * Calculates UK Income Tax and Class 1 Primary National Insurance for a given gross pay.
     * Uses UK 2024/2025 standard tax bands.
     * Sequence: Hours/Overtime -> Gross Pay -> Tax-Free Allowance -> Taxable Income -> PAYE Tax -> NI -> Net Pay
     */
    // CRITICAL: TAX_ENGINE
    fun calculateTax(grossPay: Double, taxCode: String, isMonthly: Boolean = true): SalaryReport {
        // Zero / Negative bounds protection
        val safeGross = max(0.0, grossPay)

        // 1. Income Tax Calculation
        val allowance = parseTaxFreeAllowance(taxCode, isMonthly)
        val taxablePay = max(0.0, safeGross - allowance)

        // 2024/2025 Yearly Bands -> converted to monthly if isMonthly = true
        val basicRateLimit = if (isMonthly) 37700.0 / 12 else 37700.0
        val higherRateLimit = if (isMonthly) 125140.0 / 12 else 125140.0

        var incomeTax = 0.0

        if (taxablePay > 0) {
            val basicBand = minOf(taxablePay, basicRateLimit)
            incomeTax += basicBand * 0.20

            if (taxablePay > basicRateLimit) {
                val higherBand = minOf(taxablePay - basicRateLimit, higherRateLimit - basicRateLimit)
                incomeTax += higherBand * 0.40

                if (taxablePay > higherRateLimit) {
                    val additionalBand = taxablePay - higherRateLimit
                    incomeTax += additionalBand * 0.45
                }
            }
        }

        // 2. Class 1 Primary National Insurance (2024/2025 rates: 8% main rate, 2% upper rate)
        // Primary Threshold (PT): £1,048/month (£12,576/year)
        // Upper Earnings Limit (UEL): £4,189/month (£50,268/year)
        val pt = if (isMonthly) 1048.0 else 1048.0 * 12
        val uel = if (isMonthly) 4189.0 else 4189.0 * 12

        var nationalInsurance = 0.0
        if (safeGross > pt) {
            val niBasicBand = minOf(safeGross - pt, uel - pt)
            nationalInsurance += niBasicBand * 0.08 // 8% main rate

            if (safeGross > uel) {
                val niAdditionalBand = safeGross - uel
                nationalInsurance += niAdditionalBand * 0.02 // 2% additional rate
            }
        }

        // 3. Net Pay Calculation with non-negative bounds
        val netPay = max(0.0, safeGross - incomeTax - nationalInsurance)

        return SalaryReport(
            grossPay = safeGross,
            taxablePay = taxablePay,
            incomeTax = incomeTax,
            nationalInsurance = nationalInsurance,
            netPay = netPay
        )
    }
}
