package com.example.salarycalculator.domain

import kotlin.math.max

/**
 * // CRITICAL: TAX_ENGINE
 * Salary Sacrifice Company Car & EV Benefit-in-Kind (BiK) Calculation Engine.
 *
 * Models UK HMRC Company Car tax rules:
 * - P11D Value (list price + options + delivery, inc VAT)
 * - Powertrain & Tax Year BiK Rate schedule (2024/25: 2% EV, 2025/26: 3% EV, 2026/27: 4% EV, 2027/28: 5% EV)
 * - Gross Salary Sacrifice lease vs Income Tax & National Insurance savings
 * - Annual & Monthly Class 1A BiK personal tax charge
 * - Net monthly take-home salary impact vs private retail lease
 */
enum class PowertrainType(val displayName: String, val baseBik2024: Double, val baseBik2025: Double, val baseBik2026: Double, val baseBik2027: Double) {
    PURE_EV("Pure Electric (EV - 0g/km)", 2.0, 3.0, 4.0, 5.0),
    PHEV_LONG_RANGE("Plug-in Hybrid (130+ mi range)", 2.0, 3.0, 4.0, 5.0),
    PHEV_STANDARD("Plug-in Hybrid (40-69 mi range)", 8.0, 9.0, 10.0, 11.0),
    HYBRID_PETROL("Full Hybrid / Low CO2 (100g/km)", 25.0, 26.0, 27.0, 28.0),
    PETROL_STANDARD("Petrol ICE (125g/km)", 30.0, 31.0, 32.0, 33.0),
    DIESEL_STANDARD("Diesel ICE (inc 4% surcharge)", 34.0, 35.0, 36.0, 37.0)
}

enum class BikTaxYear(val displayName: String) {
    YEAR_2024_2025("2024/2025 (2% EV)"),
    YEAR_2025_2026("2025/2026 (3% EV)"),
    YEAR_2026_2027("2026/2027 (4% EV)"),
    YEAR_2027_2028("2027/2028 (5% EV)")
}

data class CompanyCarBikResult(
    val p11dValue: Double,
    val powertrain: PowertrainType,
    val taxYear: BikTaxYear,
    val bikPercentage: Double,
    val annualBikTaxableValue: Double,
    val monthlyBikTaxableValue: Double,
    val grossMonthlySalarySacrifice: Double,
    val annualGrossSacrifice: Double,
    val employeeMarginalTaxRate: Double, // e.g. 0.40
    val employeeNiRate: Double, // e.g. 0.02 or 0.08
    val monthlyTaxSaved: Double,
    val monthlyNiSaved: Double,
    val totalMonthlyRelief: Double,
    val monthlyBikTaxPayable: Double,
    val netMonthlyCost: Double,
    val annualNetCost: Double,
    val monthlySavingsVsPrivate: Double, // vs retail private lease
    val totalSavingsPercentage: Double
)

object CompanyCarBikEngine {

    fun calculateCompanyCar(
        p11dValue: Double,
        powertrain: PowertrainType = PowertrainType.PURE_EV,
        taxYear: BikTaxYear = BikTaxYear.YEAR_2024_2025,
        grossMonthlySalarySacrifice: Double = 500.0,
        annualGrossIncome: Double = 60000.0,
        isScottish: Boolean = false
    ): CompanyCarBikResult {
        val safeP11d = max(0.0, p11dValue)
        val safeMonthlySacrifice = max(0.0, grossMonthlySalarySacrifice)
        val annualGrossSacrifice = safeMonthlySacrifice * 12.0

        // 1. Determine BiK Percentage based on Tax Year
        val bikRate = when (taxYear) {
            BikTaxYear.YEAR_2024_2025 -> powertrain.baseBik2024
            BikTaxYear.YEAR_2025_2026 -> powertrain.baseBik2025
            BikTaxYear.YEAR_2026_2027 -> powertrain.baseBik2026
            BikTaxYear.YEAR_2027_2028 -> powertrain.baseBik2027
        }

        // 2. Annual & Monthly Taxable BiK Value
        val annualBikValue = safeP11d * (bikRate / 100.0)
        val monthlyBikValue = annualBikValue / 12.0

        // 3. Marginal Tax and NI Rates
        val (marginalTaxRate, niRate) = if (annualGrossIncome > 125140.0) {
            Pair(if (isScottish) 0.48 else 0.45, 0.02)
        } else if (annualGrossIncome > 50270.0) {
            Pair(if (isScottish) 0.42 else 0.40, 0.02)
        } else if (annualGrossIncome > 12570.0) {
            Pair(if (isScottish) 0.20 else 0.20, 0.08)
        } else {
            Pair(0.0, 0.0)
        }

        // 4. Monthly Tax & NI Relief on Gross Sacrifice
        val monthlyTaxSaved = safeMonthlySacrifice * marginalTaxRate
        val monthlyNiSaved = safeMonthlySacrifice * niRate
        val totalMonthlyRelief = monthlyTaxSaved + monthlyNiSaved

        // 5. Monthly BiK Tax Payable by Employee
        val monthlyBikTaxPayable = monthlyBikValue * marginalTaxRate

        // 6. Net Monthly Cost to Employee Take-Home
        val netMonthlyCost = max(0.0, safeMonthlySacrifice - totalMonthlyRelief + monthlyBikTaxPayable)
        val annualNetCost = netMonthlyCost * 12.0

        // 7. Savings comparison vs Private PCH Lease (assumed gross lease + VAT without tax relief)
        val privateRetailMonthlyEstimate = safeMonthlySacrifice * 1.20 // retail VAT & margin
        val monthlySavings = max(0.0, privateRetailMonthlyEstimate - netMonthlyCost)
        val totalSavingsPercentage = if (privateRetailMonthlyEstimate > 0.0) {
            (monthlySavings / privateRetailMonthlyEstimate) * 100.0
        } else 0.0

        return CompanyCarBikResult(
            p11dValue = safeP11d,
            powertrain = powertrain,
            taxYear = taxYear,
            bikPercentage = bikRate,
            annualBikTaxableValue = annualBikValue,
            monthlyBikTaxableValue = monthlyBikValue,
            grossMonthlySalarySacrifice = safeMonthlySacrifice,
            annualGrossSacrifice = annualGrossSacrifice,
            employeeMarginalTaxRate = marginalTaxRate,
            employeeNiRate = niRate,
            monthlyTaxSaved = monthlyTaxSaved,
            monthlyNiSaved = monthlyNiSaved,
            totalMonthlyRelief = totalMonthlyRelief,
            monthlyBikTaxPayable = monthlyBikTaxPayable,
            netMonthlyCost = netMonthlyCost,
            annualNetCost = annualNetCost,
            monthlySavingsVsPrivate = monthlySavings,
            totalSavingsPercentage = totalSavingsPercentage
        )
    }
}
