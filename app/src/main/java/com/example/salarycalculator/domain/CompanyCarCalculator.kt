package com.example.salarycalculator.domain

import kotlin.math.max

enum class VehicleFuelType(val displayName: String) {
    PURE_ELECTRIC("Pure Electric (EV)"),
    PLUG_IN_HYBRID("Plug-in Hybrid (PHEV)"),
    PETROL_RDE2_DIESEL("Petrol / RDE2 Diesel"),
    NON_RDE2_DIESEL("Non-RDE2 Diesel (+4% Surcharge)")
}

data class CompanyCarResult(
    val p11dValue: Double,
    val fuelType: VehicleFuelType,
    val co2GramsPerKm: Int,
    val electricRangeMiles: Int,
    val bikPercentage: Double,
    val annualCarTaxableBenefit: Double,
    val monthlyCarTaxableBenefit: Double,
    val providesFuel: Boolean,
    val annualFuelTaxableBenefit: Double,
    val monthlyFuelTaxableBenefit: Double,
    val totalAnnualTaxableBenefit: Double,
    val totalMonthlyTaxableBenefit: Double,
    val basicRateAnnualTaxCost: Double,    // 20%
    val basicRateMonthlyTaxCost: Double,
    val higherRateAnnualTaxCost: Double,   // 40%
    val higherRateMonthlyTaxCost: Double,
    val additionalRateAnnualTaxCost: Double, // 45%
    val additionalRateMonthlyTaxCost: Double
)

object CompanyCarCalculator {

    // Statutory Fuel Benefit Charge Multiplier for 2024/2025 and 2025/2026
    private const val FUEL_BENEFIT_MULTIPLIER = 27800.0

    /**
     * Calculates UK HMRC Benefit-in-Kind (BiK) percentage based on CO2 emissions, fuel type, and zero-emission range.
     */
    fun calculateBikPercentage(
        fuelType: VehicleFuelType,
        co2GramsPerKm: Int,
        electricRangeMiles: Int = 0
    ): Double {
        var rate = when {
            co2GramsPerKm <= 0 || fuelType == VehicleFuelType.PURE_ELECTRIC -> 2.0
            co2GramsPerKm in 1..50 -> {
                when {
                    electricRangeMiles >= 130 -> 2.0
                    electricRangeMiles in 70..129 -> 5.0
                    electricRangeMiles in 40..69 -> 8.0
                    electricRangeMiles in 30..39 -> 12.0
                    else -> 14.0
                }
            }
            co2GramsPerKm in 51..54 -> 15.0
            else -> {
                // 55g/km is 16%, +1% for each 5g/km above 50g/km up to 37%
                val stepsAbove50 = max(0, (co2GramsPerKm - 50) / 5)
                (15.0 + stepsAbove50).coerceAtMost(37.0)
            }
        }

        if (fuelType == VehicleFuelType.NON_RDE2_DIESEL) {
            rate = (rate + 4.0).coerceAtMost(37.0)
        }

        return rate
    }

    fun calculate(
        p11dValue: Double,
        fuelType: VehicleFuelType,
        co2GramsPerKm: Int,
        electricRangeMiles: Int = 50,
        providesFuel: Boolean = false
    ): CompanyCarResult {
        val bikPercent = calculateBikPercentage(fuelType, co2GramsPerKm, electricRangeMiles)
        val bikMultiplier = bikPercent / 100.0

        val annualCarBenefit = p11dValue * bikMultiplier
        val monthlyCarBenefit = annualCarBenefit / 12.0

        val annualFuelBenefit = if (providesFuel && fuelType != VehicleFuelType.PURE_ELECTRIC) {
            FUEL_BENEFIT_MULTIPLIER * bikMultiplier
        } else {
            0.0
        }
        val monthlyFuelBenefit = annualFuelBenefit / 12.0

        val totalAnnualBenefit = annualCarBenefit + annualFuelBenefit
        val totalMonthlyBenefit = totalAnnualBenefit / 12.0

        val basicAnnual = totalAnnualBenefit * 0.20
        val basicMonthly = basicAnnual / 12.0

        val higherAnnual = totalAnnualBenefit * 0.40
        val higherMonthly = higherAnnual / 12.0

        val addAnnual = totalAnnualBenefit * 0.45
        val addMonthly = addAnnual / 12.0

        return CompanyCarResult(
            p11dValue = p11dValue,
            fuelType = fuelType,
            co2GramsPerKm = co2GramsPerKm,
            electricRangeMiles = electricRangeMiles,
            bikPercentage = bikPercent,
            annualCarTaxableBenefit = annualCarBenefit,
            monthlyCarTaxableBenefit = monthlyCarBenefit,
            providesFuel = providesFuel,
            annualFuelTaxableBenefit = annualFuelBenefit,
            monthlyFuelTaxableBenefit = monthlyFuelBenefit,
            totalAnnualTaxableBenefit = totalAnnualBenefit,
            totalMonthlyTaxableBenefit = totalMonthlyBenefit,
            basicRateAnnualTaxCost = basicAnnual,
            basicRateMonthlyTaxCost = basicMonthly,
            higherRateAnnualTaxCost = higherAnnual,
            higherRateMonthlyTaxCost = higherMonthly,
            additionalRateAnnualTaxCost = addAnnual,
            additionalRateMonthlyTaxCost = addMonthly
        )
    }
}
