package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class MultiJobAggregatorEngineTest {

    @Test
    fun testIndependentClass1NiPerEmployment() {
        // Job 1: £30,000 (NI on 30000 - 12576 = 17424 * 8% = £1,393.92)
        // Job 2: £15,000 (NI on 15000 - 12576 = 2424 * 8% = £193.92)
        // Total combined NI = £1,587.84
        val jobs = listOf(
            JobInput(title = "Primary", grossSalary = 30000.0, taxCode = "1257L", pensionPercentage = 0.0),
            JobInput(title = "Secondary", grossSalary = 15000.0, taxCode = "BR", pensionPercentage = 0.0)
        )

        val summary = MultiJobAggregatorEngine.calculateMultiJob(jobs, region = TaxRegion.UK_STANDARD)

        assertEquals(45000.0, summary.totalAnnualGross, 0.01)
        assertEquals(1393.92, summary.jobs[0].annualNi, 0.5)
        assertEquals(193.92, summary.jobs[1].annualNi, 0.5)
        assertEquals(1587.84, summary.totalAnnualNi, 1.0)
    }

    @Test
    fun testCombinedPayeIncomeTaxAcrossBands() {
        // Combined Gross = £60,000 (Job 1: £40,000, Job 2: £20,000)
        // Total allowance = £12,570. Taxable = £47,430.
        // Basic rate band (£37,700 * 20%) = £7,540.
        // Higher rate band (£47,430 - £37,700 = £9,730 * 40%) = £3,892.
        // Total Tax = £11,432.
        val jobs = listOf(
            JobInput(title = "Job 1", grossSalary = 40000.0, taxCode = "1257L", pensionPercentage = 0.0),
            JobInput(title = "Job 2", grossSalary = 20000.0, taxCode = "BR", pensionPercentage = 0.0)
        )

        val summary = MultiJobAggregatorEngine.calculateMultiJob(jobs, region = TaxRegion.UK_STANDARD)

        assertEquals(60000.0, summary.totalAnnualGross, 0.01)
        assertEquals(11432.0, summary.totalAnnualTax, 1.0)
        assertTrue(summary.totalAnnualNet > 0.0)
        assertEquals(summary.totalAnnualGross - summary.totalAnnualTax - summary.totalAnnualNi, summary.totalAnnualNet, 1.0)
    }

    @Test
    fun testMarginalTaxTrapDetectionAbove100k() {
        val jobs = listOf(
            JobInput(title = "Day Job", grossSalary = 70000.0, taxCode = "1257L", pensionPercentage = 0.0),
            JobInput(title = "Contract Job", grossSalary = 40000.0, taxCode = "D0", pensionPercentage = 0.0)
        )

        val summary = MultiJobAggregatorEngine.calculateMultiJob(jobs, region = TaxRegion.UK_STANDARD)

        assertEquals(110000.0, summary.totalAnnualGross, 0.01)
        assertTrue(summary.isMarginalTrapReached)
        // £10,000 over £100k -> £5,000 personal allowance loss
        assertEquals(5000.0, summary.personalAllowanceTaperLoss, 0.01)
    }

    @Test
    fun testScottishRegionMultiJob() {
        val jobs = listOf(
            JobInput(title = "Job 1", grossSalary = 25000.0, taxCode = "S1257L", pensionPercentage = 0.0),
            JobInput(title = "Job 2", grossSalary = 10000.0, taxCode = "SBR", pensionPercentage = 0.0)
        )

        val summary = MultiJobAggregatorEngine.calculateMultiJob(jobs, region = TaxRegion.SCOTLAND)

        assertEquals(35000.0, summary.totalAnnualGross, 0.01)
        assertTrue(summary.totalAnnualTax > 0.0)
        assertTrue(summary.overallEffectiveRate > 0.0)
    }
}
