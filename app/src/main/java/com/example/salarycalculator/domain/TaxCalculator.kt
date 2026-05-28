package com.example.salarycalculator.domain

import kotlin.math.max

data class SalaryReport(
    val grossPay: Double,
    val totalHours: Double,
    val taxablePay: Double,
    val incomeTax: Double,
    val nationalInsurance: Double,
    val netPay: Double
)

object TaxCalculator {

    // Simple parser for standard UK Tax codes (e.g. "1257L" -> 12570)
    fun parseTaxFreeAllowance(taxCode: String, isMonthly: Boolean = true): Double {
        val upperCode = taxCode.uppercase().trim()
        val numericPart = upperCode.filter { it.isDigit() }.toIntOrNull() ?: 1257
        val yearlyAllowance = numericPart * 10.0
        return if (isMonthly) yearlyAllowance / 12 else yearlyAllowance
    }

    /**
     * Calculates UK Income Tax and National Insurance for a given gross pay in a given period (usually a month).
     * Note: This uses roughly standard 24/25 UK tax bands for demonstration. 
     */
    fun calculateTax(grossPay: Double, taxCode: String, isMonthly: Boolean = true): SalaryReport {
        // Income Tax
        val allowance = parseTaxFreeAllowance(taxCode, isMonthly)
        val taxablePay = max(0.0, grossPay - allowance)

        // 24/25 Bands (Yearly) -> divide by 12 for monthly
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

        // National Insurance (Class 1 Primary - 24/25 rates, 8% above PT)
        // Primary Threshold (PT): £1,048/month
        // Upper Earnings Limit (UEL): £4,189/month
        val pt = if (isMonthly) 1048.0 else 1048.0 * 12
        val uel = if (isMonthly) 4189.0 else 4189.0 * 12

        var nationalInsurance = 0.0
        if (grossPay > pt) {
            val niBasicBand = minOf(grossPay - pt, uel - pt)
            nationalInsurance += niBasicBand * 0.08 // 8% main rate

            if (grossPay > uel) {
                val niAdditionalBand = grossPay - uel
                nationalInsurance += niAdditionalBand * 0.02 // 2% additional rate
            }
        }

        val netPay = grossPay - incomeTax - nationalInsurance

        return SalaryReport(
            grossPay = grossPay,
            totalHours = 0.0, // Filled in by caller
            taxablePay = taxablePay,
            incomeTax = incomeTax,
            nationalInsurance = nationalInsurance,
            netPay = netPay
        )
    }
}
