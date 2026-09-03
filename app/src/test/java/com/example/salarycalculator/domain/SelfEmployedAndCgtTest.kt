package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfEmployedAndCgtTest {

    @Test
    fun selfEmployed_smallProfit_claimsTradingAllowanceAndZeroNi() {
        val report = SelfEmployedTaxEngine.calculateSelfEmployedTax(
            employmentGross = 0.0,
            employmentTaxPaid = 0.0,
            turnover = 8000.0,
            expenses = 200.0,
            useTradingAllowanceIfBetter = true
        )

        assertEquals(1000.0, report.tradingAllowanceClaimed, 0.01)
        assertEquals(7000.0, report.netSelfEmployedProfit, 0.01)
        assertEquals(0.0, report.class4Ni, 0.01) // Under £12,570 LPL
        assertFalse(report.paymentsOnAccountRequired) // Under £1k tax
    }

    @Test
    fun selfEmployed_highProfit_triggersClass4NiAndPaymentsOnAccount() {
        val report = SelfEmployedTaxEngine.calculateSelfEmployedTax(
            employmentGross = 0.0,
            employmentTaxPaid = 0.0,
            turnover = 40000.0,
            expenses = 5000.0
        )

        assertEquals(35000.0, report.netSelfEmployedProfit, 0.01)
        // Class 4 NI: (35000 - 12570) * 0.06 = 22430 * 0.06 = 1345.80
        assertEquals(1345.80, report.class4Ni, 0.01)
        assertTrue(report.totalSelfAssessmentLiability > 5000.0)
        assertTrue(report.paymentsOnAccountRequired)
        assertEquals(report.totalSelfAssessmentLiability * 0.50, report.firstPaymentOnAccountJan31, 0.01)
    }

    @Test
    fun giftAid_higherRateTaxpayer_calculates25PercentTopUpAnd20PercentRelief() {
        val report = GiftAidOptimizer.calculateGiftAid(
            netDonation = 1000.0,
            annualSalary = 75000.0
        )

        assertEquals(1250.0, report.grossDonationToCharity, 0.01)
        assertEquals(250.0, report.hmrcBasicRateTopUp, 0.01)
        // Higher rate relief: 1250 * 20% = 250
        assertEquals(250.0, report.higherRateTaxReliefClaimable, 0.01)
        assertEquals(750.0, report.effectiveNetCostToDonor, 0.01)
        assertEquals(38950.0, report.expandedBasicRateBandLimit, 0.01)
    }

    @Test
    fun capitalGains_within3kExemption_zeroTaxDue() {
        val report = CapitalGainsTaxEngine.calculateCgt(
            disposalProceeds = 8000.0,
            acquisitionAndAllowableCosts = 6000.0, // Gain = 2000
            annualTaxableIncome = 25000.0,
            assetType = AssetType.SHARES_AND_OTHER
        )

        assertEquals(2000.0, report.totalGain, 0.01)
        assertEquals(2000.0, report.annualExemptionUsed, 0.01)
        assertEquals(0.0, report.taxableGain, 0.01)
        assertEquals(0.0, report.totalCgtDue, 0.01)
    }

    @Test
    fun capitalGains_residentialProperty_applies18And24Rates() {
        // Gain £13,000 -> Exemption £3,000 -> Taxable £10,000
        // Salary £45,000 -> Basic remaining = 37700 - (45000-12570) = 37700 - 32430 = 5270
        // Gain at basic = 5270 * 18% = 948.60
        // Gain at higher = (10000 - 5270) = 4730 * 24% = 1135.20
        // Total = 2083.80
        val report = CapitalGainsTaxEngine.calculateCgt(
            disposalProceeds = 113000.0,
            acquisitionAndAllowableCosts = 100000.0,
            annualTaxableIncome = 32430.0,
            assetType = AssetType.RESIDENTIAL_PROPERTY
        )

        assertEquals(13000.0, report.totalGain, 0.01)
        assertEquals(3000.0, report.annualExemptionUsed, 0.01)
        assertEquals(10000.0, report.taxableGain, 0.01)
        assertEquals(2083.80, report.totalCgtDue, 0.01)
    }

    @Test
    fun solarScheduler_computesValidSunriseSunsetHours() {
        val sunrise = SolarThemeScheduler.calculateSunrise(180, 51.5, -0.12)
        val sunset = SolarThemeScheduler.calculateSunset(180, 51.5, -0.12)

        assertTrue(sunrise in 3.0..9.0)
        assertTrue(sunset in 16.0..23.0)
        assertTrue(sunset > sunrise)
    }
}
