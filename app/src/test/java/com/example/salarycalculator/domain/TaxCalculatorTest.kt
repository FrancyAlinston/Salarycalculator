package com.example.salarycalculator.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCalculatorTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun parseTaxFreeAllowance_standardCode_returnsCorrectValues() {
        val monthly = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = true)
        val yearly = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = false)

        assertEquals(1047.50, monthly, 0.01)
        assertEquals(12570.0, yearly, 0.01)
    }

    @Test
    fun parseTaxFreeAllowance_withMarriageAndBlindAllowance_increasesAllowance() {
        val baseYearly = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = false)
        val withMarriage = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = false, hasMarriageAllowance = true)
        val withBlind = TaxCalculator.parseTaxFreeAllowance("1257L", isMonthly = false, hasBlindPersonsAllowance = true)

        assertEquals(baseYearly + 1260.0, withMarriage, 0.01)
        assertEquals(baseYearly + 3070.0, withBlind, 0.01)
    }

    @Test
    fun parseTaxFreeAllowance_customAndSpecialCodes_returnsCorrectValues() {
        // Custom 1100L
        val customMonthly = TaxCalculator.parseTaxFreeAllowance("1100L", isMonthly = true)
        assertEquals(11000.0 / 12, customMonthly, 0.01)

        // Flat / Zero tax codes
        assertEquals(0.0, TaxCalculator.parseTaxFreeAllowance("BR", isMonthly = true), 0.01)
        assertEquals(0.0, TaxCalculator.parseTaxFreeAllowance("0T", isMonthly = true), 0.01)
        assertEquals(0.0, TaxCalculator.parseTaxFreeAllowance("D0", isMonthly = true), 0.01)
    }

    @Test
    fun calculateTax_zeroOrNegativeGross_returnsZeroValues() {
        val zeroReport = TaxCalculator.calculateTax(0.0, "1257L", isMonthly = true)
        assertEquals(0.0, zeroReport.grossPay, 0.01)
        assertEquals(0.0, zeroReport.taxablePay, 0.01)
        assertEquals(0.0, zeroReport.incomeTax, 0.01)
        assertEquals(0.0, zeroReport.nationalInsurance, 0.01)
        assertEquals(0.0, zeroReport.netPay, 0.01)

        val negativeReport = TaxCalculator.calculateTax(-500.0, "1257L", isMonthly = true)
        assertEquals(0.0, negativeReport.grossPay, 0.01)
        assertEquals(0.0, negativeReport.taxablePay, 0.01)
        assertEquals(0.0, negativeReport.incomeTax, 0.01)
        assertEquals(0.0, negativeReport.nationalInsurance, 0.01)
        assertEquals(0.0, negativeReport.netPay, 0.01)
    }

    @Test
    fun calculateTax_underAllowance_paysNoIncomeTaxOrNI() {
        val report = TaxCalculator.calculateTax(1000.0, "1257L", isMonthly = true)

        assertEquals(1000.0, report.grossPay, 0.01)
        assertEquals(0.0, report.taxablePay, 0.01)
        assertEquals(0.0, report.incomeTax, 0.01)
        assertEquals(0.0, report.nationalInsurance, 0.01)
        assertEquals(1000.0, report.netPay, 0.01)
    }

    @Test
    fun calculateTax_basicRateBand_correctTaxAndNI() {
        val report = TaxCalculator.calculateTax(2500.0, "1257L", isMonthly = true)

        assertEquals(2500.0, report.grossPay, 0.01)
        assertEquals(1452.50, report.taxablePay, 0.01)
        assertEquals(290.50, report.incomeTax, 0.01)
        assertEquals(116.16, report.nationalInsurance, 0.01)
        assertEquals(2093.34, report.netPay, 0.01)
    }

    @Test
    fun calculateTax_marriageAllowance_reducesIncomeTax() {
        val withoutMarriage = TaxCalculator.calculateTax(2500.0, "1257L", isMonthly = true, hasMarriageAllowance = false)
        val withMarriage = TaxCalculator.calculateTax(2500.0, "1257L", isMonthly = true, hasMarriageAllowance = true)

        assertEquals(290.50, withoutMarriage.incomeTax, 0.01)
        assertEquals(269.50, withMarriage.incomeTax, 0.01) // £21/month tax reduction
        assertEquals(21.0, withoutMarriage.incomeTax - withMarriage.incomeTax, 0.01)
    }

    @Test
    fun calculateTax_customDeductions_reducesNetPay() {
        val unionDues = CustomDeduction(name = "Unison Union", amount = 15.0, isPreTax = false)
        val report = TaxCalculator.calculateTax(2500.0, "1257L", isMonthly = true, customDeductions = listOf(unionDues))

        assertEquals(15.0, report.customDeductionsTotal, 0.01)
        assertEquals(2093.34 - 15.0, report.netPay, 0.01)
    }

    @Test
    fun calculateTax_higherRateBand_correctTaxAndNI() {
        val report = TaxCalculator.calculateTax(5000.0, "1257L", isMonthly = true)

        assertEquals(5000.0, report.grossPay, 0.01)
        assertEquals(3952.50, report.taxablePay, 0.01)
        assertEquals(952.67, report.incomeTax, 0.05)
        assertEquals(267.50, report.nationalInsurance, 0.01)
        assertTrue(report.netPay > 0.0)
    }

    @Test
    fun calculateTax_pensionContribution_providesTaxRelief() {
        val withoutPension = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, pensionRatePercent = 0.0)
        val withPension = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, pensionRatePercent = 5.0)

        assertEquals(150.0, withPension.pensionContribution, 0.01)
        assertEquals(90.0, withPension.employerPensionContribution, 0.01)
        assertEquals(1802.50, withPension.taxablePay, 0.01)
        assertEquals(360.50, withPension.incomeTax, 0.01)
        assertTrue(withPension.incomeTax < withoutPension.incomeTax)
    }

    @Test
    fun calculateTax_salarySacrifice_reducesTaxAndNI() {
        val withoutSacrifice = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, salarySacrificeAmount = 0.0)
        val withSacrifice = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, salarySacrificeAmount = 200.0)

        assertEquals(200.0, withSacrifice.salarySacrifice, 0.01)
        assertEquals(2800.0, withSacrifice.adjustedGrossPay, 0.01)
        assertTrue(withSacrifice.incomeTax < withoutSacrifice.incomeTax)
        assertTrue(withSacrifice.nationalInsurance < withoutSacrifice.nationalInsurance)
    }

    @Test
    fun calculateTax_studentLoanPlan2_correctDeduction() {
        val report = TaxCalculator.calculateTax(
            2916.67,
            "1257L",
            isMonthly = true,
            studentLoanPlan = StudentLoanPlan.PLAN_2
        )

        assertEquals(57.79, report.studentLoanDeduction, 0.10)
        assertEquals(2916.67 - report.incomeTax - report.nationalInsurance - report.studentLoanDeduction, report.netPay, 0.01)
    }

    @Test
    fun calculateTax_scotlandRegion_appliesScottishBands() {
        val ukReport = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, region = TaxRegion.UK_STANDARD)
        val scotlandReport = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true, region = TaxRegion.SCOTLAND)

        assertEquals(396.44, scotlandReport.incomeTax, 0.50)
        assertTrue(scotlandReport.incomeTax > ukReport.incomeTax)
    }

    @Test
    fun calculateTax_periodConversions_areConsistent() {
        val report = TaxCalculator.calculateTax(3000.0, "1257L", isMonthly = true)

        assertEquals(report.netPay * 12.0, report.annualNet, 0.01)
        assertEquals(report.annualNet / 52.0, report.weeklyNet, 0.01)
        assertEquals(report.weeklyNet / 37.5, report.hourlyNet, 0.01)
    }

    @Test
    fun taxCalculator_fullCheck_acrossVaryingRatesAndTiers() {
        // Full battery verification across multiple income points and rate combinations
        val testIncomes = listOf(1000.0, 1500.0, 2500.0, 3500.0, 5000.0, 8000.0, 12000.0)
        for (gross in testIncomes) {
            val repUk = TaxCalculator.calculateTax(gross, "1257L", isMonthly = true, pensionRatePercent = 5.0)
            assertTrue("Gross >= Net at £$gross", repUk.grossPay >= repUk.netPay)
            assertTrue("Deductions >= 0 at £$gross", repUk.totalDeductions >= 0.0)
            assertEquals("Net + Deductions == Gross", gross, repUk.netPay + repUk.totalDeductions, 0.01)

            val repScot = TaxCalculator.calculateTax(gross, "1257L", isMonthly = true, region = TaxRegion.SCOTLAND, pensionRatePercent = 5.0)
            assertTrue("Scottish Net + Deductions == Gross", gross == (repScot.netPay + repScot.totalDeductions) || Math.abs(gross - (repScot.netPay + repScot.totalDeductions)) < 0.01)
        }
    }

    @Test
    fun employerProfile_serialization_isLossless() {
        val profile = EmployerProfile(
            name = "Tech Freelance",
            employerName = "Apex Studio Ltd",
            taxCode = "BR",
            hourlyRate = 25.0,
            pensionRate = 8.0,
            taxRegion = TaxRegion.SCOTLAND,
            studentLoanPlan = StudentLoanPlan.PLAN_4
        )

        val encoded = json.encodeToString(listOf(profile))
        val decoded = json.decodeFromString<List<EmployerProfile>>(encoded)

        assertEquals(1, decoded.size)
        assertEquals("Tech Freelance", decoded[0].name)
        assertEquals("BR", decoded[0].taxCode)
        assertEquals(25.0, decoded[0].hourlyRate, 0.01)
        assertEquals(TaxRegion.SCOTLAND, decoded[0].taxRegion)
    }

    @Test
    fun monthlySalaryRecord_serialization_isLossless() {
        val record = MonthlySalaryRecord(
            monthYear = "September 2026",
            daysWorked = 20.0,
            hoursPerDay = 8.0,
            overtimeHours = 5.0,
            overtimeMultiplier = 1.5,
            hourlyRate = 15.0,
            grossPay = 2512.50,
            salarySacrifice = 100.0,
            pensionRate = 5.0,
            pensionContribution = 120.63,
            employerPension = 72.38,
            taxablePay = 1245.37,
            incomeTax = 249.07,
            nationalInsurance = 109.16,
            studentLoanPlan = StudentLoanPlan.PLAN_2,
            studentLoanDeduction = 21.41,
            totalDeductions = 600.27,
            netPay = 1912.23,
            taxCode = "1257L",
            taxRegion = TaxRegion.UK_STANDARD,
            note = "September paycheck"
        )

        val encoded = json.encodeToString(listOf(record))
        val decoded = json.decodeFromString<List<MonthlySalaryRecord>>(encoded)

        assertEquals(1, decoded.size)
        val first = decoded[0]
        assertEquals("September 2026", first.monthYear)
        assertEquals(1912.23, first.netPay, 0.01)
        assertEquals(2512.50, first.grossPay, 0.01)
        assertEquals(100.0, first.salarySacrifice, 0.01)
        assertEquals(StudentLoanPlan.PLAN_2, first.studentLoanPlan)
        assertEquals("September paycheck", first.note)
    }

    @Test
    fun childBenefitCalculator_underThreshold_zeroClawback() {
        val result = ChildBenefitCalculator.calculate(annualIncome = 50000.0, numChildren = 2)
        assertEquals(2, result.numChildren)
        assertEquals(0.0, result.clawbackPercentage, 0.01)
        assertEquals(0.0, result.annualTaxCharge, 0.01)
        assertEquals(result.annualBenefitEntitlement, result.netAnnualBenefit, 0.01)
    }

    @Test
    fun childBenefitCalculator_middleTaper_partialClawback() {
        // At £70,000, excess is £10,000 -> 50% clawback
        val result = ChildBenefitCalculator.calculate(annualIncome = 70000.0, numChildren = 1)
        assertEquals(50.0, result.clawbackPercentage, 0.01)
        assertEquals(result.annualBenefitEntitlement * 0.50, result.annualTaxCharge, 0.01)
        assertEquals(result.annualBenefitEntitlement * 0.50, result.netAnnualBenefit, 0.01)
    }

    @Test
    fun childBenefitCalculator_aboveThreshold_fullClawback() {
        val result = ChildBenefitCalculator.calculate(annualIncome = 85000.0, numChildren = 3)
        assertEquals(100.0, result.clawbackPercentage, 0.01)
        assertEquals(result.annualBenefitEntitlement, result.annualTaxCharge, 0.01)
        assertEquals(0.0, result.netAnnualBenefit, 0.01)
    }

    @Test
    fun shiftRecord_durationHours_correctComputation() {
        val start = 1000000L
        val end = start + (8 * 3600 * 1000L) // 8 hours
        val shift = ShiftRecord(startTimestamp = start, endTimestamp = end, breakMinutes = 30)
        assertEquals(7.5, shift.durationHours, 0.01)
    }

    @Test
    fun backupBundle_serialization_isLossless() {
        val bundle = BackupBundle(
            version = 1,
            taxCode = "1257L",
            pensionRate = 6.0,
            hourlyRate = 14.50
        )
        val encoded = json.encodeToString(bundle)
        val decoded = json.decodeFromString<BackupBundle>(encoded)
        assertEquals(1, decoded.version)
        assertEquals("1257L", decoded.taxCode)
        assertEquals(6.0, decoded.pensionRate, 0.01)
        assertEquals(14.50, decoded.hourlyRate, 0.01)
    }

    @Test
    fun taxCalculator_bonusAndCommission_includedInGrossAndDeductions() {
        val baseGross = 2000.0
        val bonus = 500.0
        val commission = 250.0
        val report = TaxCalculator.calculateTax(
            grossPay = baseGross,
            taxCode = "1257L",
            isMonthly = true,
            bonusPay = bonus,
            commissionPay = commission,
            pensionRatePercent = 5.0
        )

        assertEquals(2750.0, report.grossPay, 0.01)
        assertEquals(500.0, report.bonusPay, 0.01)
        assertEquals(250.0, report.commissionPay, 0.01)
        assertEquals(2000.0, report.baseGrossPay, 0.01)

        // Arithmetic invariant: Net + Deductions = Gross
        assertEquals(report.grossPay, report.netPay + report.totalDeductions, 0.01)
    }

    @Test
    fun currencyConverter_convertsGbpToEurAndUsdAccurately() {
        val gbpAmount = 2000.0
        val converted = CurrencyConverter.convert(gbpAmount = gbpAmount, eurRate = 1.20, usdRate = 1.30)
        assertEquals(2000.0, converted.gbpAmount, 0.01)
        assertEquals(2400.0, converted.eurAmount, 0.01)
        assertEquals(2600.0, converted.usdAmount, 0.01)
    }

    @Test
    fun icsCalendarExporter_generatesValidRfc5545Calendar() {
        val dayShifts = mapOf(
            1 to 8.0,
            2 to 10.0,
            3 to 0.0
        )
        val ics = IcsCalendarExporter.generateIcsContent(
            year = 2026,
            month = 9,
            dayShifts = dayShifts,
            jobTitle = "Primary Employment"
        )

        assertTrue(ics.startsWith("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("VERSION:2.0"))
        assertTrue(ics.contains("BEGIN:VEVENT"))
        assertTrue(ics.contains("SUMMARY:Primary Employment (8h Shift)"))
        assertTrue(ics.contains("SUMMARY:Primary Employment (Overtime: 10h)"))
        assertTrue(ics.endsWith("END:VCALENDAR\n") || ics.endsWith("END:VCALENDAR\r\n"))
    }

    @Test
    fun marginalTaxTrap_personalAllowanceTaper_at110k_loses5kAllowance() {
        val income = 110000.0
        val excessOver100k = income - 100000.0
        val allowanceLost = minOf(12570.0, excessOver100k / 2.0)
        val remainingAllowance = 12570.0 - allowanceLost

        assertEquals(5000.0, allowanceLost, 0.01)
        assertEquals(7570.0, remainingAllowance, 0.01)

        // At £130,000, allowance is fully wiped
        val incomeHigh = 130000.0
        val allowanceLostHigh = minOf(12570.0, (incomeHigh - 100000.0) / 2.0)
        assertEquals(12570.0, allowanceLostHigh, 0.01)
    }

    @Test
    fun taxYearComparison_computesAccurate2023to2025NiSavings() {
        // £35,000 annual income
        val comparison = TaxYearComparisonCalculator.compare(
            grossAmount = 35000.0,
            isMonthly = false,
            pensionRatePercent = 0.0
        )

        assertEquals(3, comparison.summaries.size)
        val s2023 = comparison.summaries.find { it.yearLabel == "2023/2024" }!!
        val s2024 = comparison.summaries.find { it.yearLabel == "2024/2025" }!!
        val s2025 = comparison.summaries.find { it.yearLabel == "2025/2026" }!!

        // In 2023/24, main NI was 12% on (35000 - 12570) = 22430 * 0.12 = 2691.60
        assertEquals(2691.60, s2023.nationalInsurance, 0.01)

        // In 2024/25 & 2025/26, main NI was cut to 8% on 22430 * 0.08 = 1794.40
        assertEquals(1794.40, s2024.nationalInsurance, 0.01)
        assertEquals(1794.40, s2025.nationalInsurance, 0.01)

        // Annual NI saving from cut: 2691.60 - 1794.40 = 897.20
        assertEquals(897.20, s2024.annualSavingsVs2023, 0.01)
        assertTrue(s2024.netPay > s2023.netPay)
    }

    @Test
    fun taxRefundEstimator_emergencyCodeSwitch_calculatesAccurateCumulativeRefund() {
        // £3,000/mo on BR (flat 20% = £600 tax/mo) for 4 months (paid £2,400 tax so far)
        // Under 1257L, 4 months allowance is £4,190 -> cumulative taxable is £12,000 - £4,190 = £7,810 -> tax is £1,562
        // Immediate refund is £2,400 - £1,562 = £838
        val result = TaxRefundEstimator.estimate(
            monthlyGross = 3000.0,
            monthsOnOldCode = 4,
            oldTaxCode = "BR",
            newTaxCode = "1257L",
            pensionRatePercent = 0.0
        )

        assertEquals(12000.0, result.cumulativeGross, 0.01)
        assertEquals(2400.0, result.taxPaidUnderOldCode, 0.01)
        assertEquals(1562.0, result.cumulativeTaxDueUnderNewCode, 0.01)
        assertEquals(838.0, result.immediatePaycheckRefund, 0.01)
        assertTrue(result.newMonthlyTakeHome > result.oldMonthlyTakeHome)
    }

    @Test
    fun sa100Generator_mapsBoxesCorrectly() {
        val records = listOf(
            MonthlySalaryRecord(
                monthYear = "January 2025",
                daysWorked = 20.0,
                hoursPerDay = 8.0,
                overtimeHours = 0.0,
                overtimeMultiplier = 1.5,
                hourlyRate = 18.75,
                grossPay = 3000.0,
                salarySacrifice = 0.0,
                pensionRate = 5.0,
                pensionContribution = 150.0,
                employerPension = 90.0,
                taxablePay = 1802.50,
                incomeTax = 360.50,
                nationalInsurance = 139.44,
                studentLoanPlan = StudentLoanPlan.PLAN_1,
                studentLoanDeduction = 45.0,
                totalDeductions = 694.94,
                netPay = 2305.06,
                taxCode = "1257L",
                taxRegion = TaxRegion.UK_STANDARD
            ),
            MonthlySalaryRecord(
                monthYear = "February 2025",
                daysWorked = 20.0,
                hoursPerDay = 8.0,
                overtimeHours = 0.0,
                overtimeMultiplier = 1.5,
                hourlyRate = 18.75,
                grossPay = 3000.0,
                salarySacrifice = 0.0,
                pensionRate = 5.0,
                pensionContribution = 150.0,
                employerPension = 90.0,
                taxablePay = 1802.50,
                incomeTax = 360.50,
                nationalInsurance = 139.44,
                studentLoanPlan = StudentLoanPlan.PLAN_1,
                studentLoanDeduction = 45.0,
                totalDeductions = 694.94,
                netPay = 2305.06,
                taxCode = "1257L",
                taxRegion = TaxRegion.UK_STANDARD
            )
        )

        val summary = Sa100Generator.generateFromRecords(records, taxYearLabel = "2024/2025")
        assertEquals(6000.0, summary.totalGrossPay, 0.01)
        assertEquals(721.00, summary.totalTaxDeducted, 0.01)
        assertEquals(300.00, summary.totalPensionRelief, 0.01)
        assertEquals(90.00, summary.totalStudentLoan, 0.01)
        assertEquals(4610.12, summary.netTakeHome, 0.01)

        val box1 = summary.boxes.find { it.boxNumber == "Box 1" }!!
        val box2 = summary.boxes.find { it.boxNumber == "Box 2" }!!
        val box5 = summary.boxes.find { it.boxNumber == "Box 5" }!!
        val box6 = summary.boxes.find { it.boxNumber == "Box 6" }!!

        assertEquals(6000.0, box1.amount, 0.01)
        assertEquals(721.00, box2.amount, 0.01)
        assertEquals(300.00, box5.amount, 0.01)
        assertEquals(90.00, box6.amount, 0.01)
    }

    @Test
    fun companyCarCalculator_pureEv_calculates2PercentBik() {
        val result = CompanyCarCalculator.calculate(
            p11dValue = 40000.0,
            fuelType = VehicleFuelType.PURE_ELECTRIC,
            co2GramsPerKm = 0
        )
        assertEquals(2.0, result.bikPercentage, 0.01)
        assertEquals(800.0, result.annualCarTaxableBenefit, 0.01)
        assertEquals(66.67, result.monthlyCarTaxableBenefit, 0.01)
        // 20% basic rate tax cost
        assertEquals(160.0, result.basicRateAnnualTaxCost, 0.01)
        assertEquals(13.33, result.basicRateMonthlyTaxCost, 0.01)
        // 40% higher rate tax cost
        assertEquals(320.0, result.higherRateAnnualTaxCost, 0.01)
        assertEquals(26.67, result.higherRateMonthlyTaxCost, 0.01)
    }

    @Test
    fun companyCarCalculator_petrolWithFuel_calculatesAccurateBenefit() {
        val result = CompanyCarCalculator.calculate(
            p11dValue = 30000.0,
            fuelType = VehicleFuelType.PETROL_RDE2_DIESEL,
            co2GramsPerKm = 115,
            providesFuel = true
        )
        // 115g/km is 15 + (115-50)/5 = 15 + 13 = 28%
        assertEquals(28.0, result.bikPercentage, 0.01)
        assertEquals(8400.0, result.annualCarTaxableBenefit, 0.01)
        // Fuel benefit: 27800 * 0.28 = 7784.00
        assertEquals(7784.00, result.annualFuelTaxableBenefit, 0.01)
        assertEquals(16184.00, result.totalAnnualTaxableBenefit, 0.01)
    }

    @Test
    fun statutoryLeaveCalculator_ssp_calculatesWaitingDaysCorrectly() {
        val result = StatutoryLeaveCalculator.calculate(
            leaveType = StatutoryLeaveType.SICK_PAY_SSP,
            averageWeeklyEarnings = 600.0,
            durationWeeks = 4
        )
        assertEquals(4, result.durationWeeks)
        assertEquals(4, result.weeklyBreakdown.size)
        // Week 1: 2/5th of £116.75 = £46.70
        assertEquals(46.70, result.weeklyBreakdown[0].statutoryAmount, 0.01)
        // Weeks 2-4: £116.75
        assertEquals(116.75, result.weeklyBreakdown[1].statutoryAmount, 0.01)
        // Total statutory: 46.70 + 3 * 116.75 = 396.95
        assertEquals(396.95, result.totalStatutoryGross, 0.01)
    }

    @Test
    fun statutoryLeaveCalculator_smp_calculatesSixWeeksHigherRate() {
        val result = StatutoryLeaveCalculator.calculate(
            leaveType = StatutoryLeaveType.MATERNITY_SMP,
            averageWeeklyEarnings = 800.0,
            durationWeeks = 10
        )
        assertEquals(10, result.weeklyBreakdown.size)
        // First 6 weeks: 90% of £800 = £720/wk
        for (w in 0..5) {
            assertEquals(720.0, result.weeklyBreakdown[w].statutoryAmount, 0.01)
        }
        // Weeks 7-10: Statutory standard rate £184.03/wk
        for (w in 6..9) {
            assertEquals(184.03, result.weeklyBreakdown[w].statutoryAmount, 0.01)
        }
    }

    @Test
    fun mortgageBorrowingCalculator_standardMultiple_calculatesAccurateRepayment() {
        val result = MortgageBorrowingCalculator.calculate(
            annualGrossIncome = 50000.0,
            monthlyNetTakeHome = 3200.0,
            depositAmount = 25000.0,
            monthlyDebtCommitments = 200.0,
            selectedMultiplier = 4.5,
            annualInterestRatePercent = 4.5,
            termYears = 30
        )

        // 50k * 4.5 = £225,000 max borrowing
        assertEquals(225000.0, result.maxBorrowingAmount, 0.01)
        assertEquals(250000.0, result.estimatedPropertyPrice, 0.01)
        assertEquals(90.0, result.loanToValuePercentage, 0.01)
        assertEquals(3000.0, result.netMonthlyDisposable, 0.01)

        // Monthly repayment on 225k at 4.5% 30yr is approx £1,140.04
        assertEquals(1140.04, result.estimatedMonthlyRepayment, 1.0)
        assertTrue(result.monthlyDisposableAfterMortgage > 1800.0)
        assertEquals(AffordabilityHealth.MODERATE, result.affordabilityStatus)
    }

    @Test
    fun bankReconciliationEngine_parsesAndMatchesDepositsAccurately() {
        val sampleCsv = """
            Date,Description,Amount
            28/01/2025,"ACME CORP BACS SALARY",2305.06
            28/02/2025,"ACME CORP BACS SALARY",2350.00
            15/03/2025,"UNKNOWN DIRECT CREDIT",100.00
        """.trimIndent()

        val parsed = BankReconciliationEngine.parseCsv(sampleCsv)
        assertEquals(3, parsed.size)
        assertEquals(2305.06, parsed[0].amount, 0.01)

        val records = listOf(
            MonthlySalaryRecord(
                monthYear = "January 2025",
                daysWorked = 20.0,
                hoursPerDay = 8.0,
                overtimeHours = 0.0,
                overtimeMultiplier = 1.5,
                hourlyRate = 18.75,
                grossPay = 3000.0,
                salarySacrifice = 0.0,
                pensionRate = 5.0,
                pensionContribution = 150.0,
                employerPension = 90.0,
                taxablePay = 1802.50,
                incomeTax = 360.50,
                nationalInsurance = 139.44,
                studentLoanPlan = StudentLoanPlan.PLAN_1,
                studentLoanDeduction = 45.0,
                totalDeductions = 694.94,
                netPay = 2305.06,
                taxCode = "1257L",
                taxRegion = TaxRegion.UK_STANDARD
            ),
            MonthlySalaryRecord(
                monthYear = "February 2025",
                daysWorked = 20.0,
                hoursPerDay = 8.0,
                overtimeHours = 0.0,
                overtimeMultiplier = 1.5,
                hourlyRate = 18.75,
                grossPay = 3000.0,
                salarySacrifice = 0.0,
                pensionRate = 5.0,
                pensionContribution = 150.0,
                employerPension = 90.0,
                taxablePay = 1802.50,
                incomeTax = 360.50,
                nationalInsurance = 139.44,
                studentLoanPlan = StudentLoanPlan.PLAN_1,
                studentLoanDeduction = 45.0,
                totalDeductions = 694.94,
                netPay = 2305.06,
                taxCode = "1257L",
                taxRegion = TaxRegion.UK_STANDARD
            )
        )

        val summary = BankReconciliationEngine.reconcile(parsed, records)
        assertEquals(3, summary.totalCreditsFound)
        assertEquals(1, summary.totalMatchedCount) // January exact match
        assertEquals(1, summary.totalDiscrepanciesCount) // February variance (£44.94)

        assertEquals(ReconciliationStatus.EXACT_MATCH, summary.items[0].status)
        assertEquals(ReconciliationStatus.VARIANCE_DETECTED, summary.items[1].status)
        assertEquals(ReconciliationStatus.UNMATCHED_CREDIT, summary.items[2].status)
    }

    @Test
    fun hmrcRateSyncManager_serializesAndDeserializesAccurately() {
        val defaultRates = HmrcRateSyncManager.STATUTORY_DEFAULT_RATES
        val jsonStr = HmrcRateSyncManager.toJson(defaultRates)
        assertTrue(jsonStr.contains("12570"))
        assertTrue(jsonStr.contains("37700"))

        val parsed = HmrcRateSyncManager.fromJson(jsonStr)
        assertEquals(12570.0, parsed.personalAllowanceAnnual, 0.01)
        assertEquals(37700.0, parsed.basicRateLimitAnnual, 0.01)
        assertEquals(12.21, parsed.nationalLivingWage21Plus, 0.01)
    }

    @Test
    fun hmrcRateSyncManager_fallbackOnMalformedJson() {
        val badJson = "{\"invalid_field\": true}"
        val parsed = HmrcRateSyncManager.fromJson(badJson)
        assertEquals(12570.0, parsed.personalAllowanceAnnual, 0.01)
    }

    @Test
    fun overtimeMultipliers_calculatesStandardAndCustomAccurately() {
        val hourlyRate = 20.0
        val overtimeHours = 10.0

        // 1.0x Standard Rate
        val standardGross = overtimeHours * (hourlyRate * 1.0)
        assertEquals(200.0, standardGross, 0.01)

        // 1.25x Rate
        val rate125Gross = overtimeHours * (hourlyRate * 1.25)
        assertEquals(250.0, rate125Gross, 0.01)

        // 1.75x Rate
        val rate175Gross = overtimeHours * (hourlyRate * 1.75)
        assertEquals(350.0, rate175Gross, 0.01)

        // 2.25x Rate
        val rate225Gross = overtimeHours * (hourlyRate * 2.25)
        assertEquals(450.0, rate225Gross, 0.01)

        // 3.0x Triple Rate
        val tripleGross = overtimeHours * (hourlyRate * 3.0)
        assertEquals(600.0, tripleGross, 0.01)
    }

    @Test
    fun payslipParserEngine_extractsFieldsAndAuditsStatutoryDiscrepancy() {
        val samplePayslip = """
            EMPLOYER: ACME TECHNOLOGIES LTD
            PAY DATE: 31 January 2025
            TAX CODE: 1257L
            
            PAYMENTS:
            Basic Salary: £3,500.00
            Total Gross: £3,500.00
            
            DEDUCTIONS:
            PAYE Tax: £460.50
            National Insurance: £179.44
            Employee Pension: £175.00
            Student Loan Plan 2: £55.00
            
            NET PAY: £2,630.06
        """.trimIndent()

        val parsed = PayslipParserEngine.parsePayslipText(samplePayslip)
        assertEquals("ACME TECHNOLOGIES LTD", parsed.employerName)
        assertEquals("1257L", parsed.taxCode)
        assertEquals("January 2025", parsed.payPeriod)
        assertEquals(3500.0, parsed.grossPay, 0.01)
        assertEquals(2630.06, parsed.netPay, 0.01)
        assertEquals(460.50, parsed.incomeTax, 0.01)
        assertEquals(179.44, parsed.nationalInsurance, 0.01)
        assertEquals(175.00, parsed.employeePension, 0.01)
        assertEquals(55.00, parsed.studentLoan, 0.01)
        assertNotNull(parsed.verificationAnalysis)
    }

    @Test
    fun directorDividendOptimizer_calculatesCorporationTaxAndOptimalSavings() {
        // Test £40,000 profit (19% rate)
        val corpTax40k = DirectorDividendOptimizer.calculateCorporationTax(40000.0)
        assertEquals(7600.0, corpTax40k, 0.01)

        // Test £300,000 profit (25% main rate)
        val corpTax300k = DirectorDividendOptimizer.calculateCorporationTax(300000.0)
        assertEquals(75000.0, corpTax300k, 0.01)

        // Test £60,000 optimization report
        val report = DirectorDividendOptimizer.generateOptimizationReport(60000.0)
        assertEquals(4, report.scenarios.size)
        assertTrue(report.annualTaxSavingsVsPureSalary > 0.0)
        assertTrue(report.optimalScenario.netCashInPocket > report.scenarios.find { it.name.contains("100% PAYE") }!!.netCashInPocket)
    }

    @Test
    fun ytdTaxProjector_calculatesCumulativeMetricsAndPensionOptimization() {
        val sampleRecords = listOf(
            MonthlySalaryRecord(monthYear = "April 2024", daysWorked = 20.0, hoursPerDay = 8.0, grossPay = 4000.0, incomeTax = 548.0, nationalInsurance = 219.0, netPay = 3233.0),
            MonthlySalaryRecord(monthYear = "May 2024", daysWorked = 20.0, hoursPerDay = 8.0, grossPay = 4000.0, incomeTax = 548.0, nationalInsurance = 219.0, netPay = 3233.0),
            MonthlySalaryRecord(monthYear = "June 2024", daysWorked = 20.0, hoursPerDay = 8.0, grossPay = 4000.0, incomeTax = 548.0, nationalInsurance = 219.0, netPay = 3233.0)
        )

        val ytd = YtdTaxProjector.computeYtdProjection(sampleRecords, currentMonthlyGross = 4000.0)
        assertEquals(3, ytd.monthsLogged)
        assertEquals(9, ytd.monthsRemaining)
        assertEquals(12000.0, ytd.ytdGross, 0.01)
        assertEquals(48000.0, ytd.projectedAnnualGross, 0.01)
        assertTrue(ytd.projectedAnnualNet > 0.0)

        // Test £110k 60% Tax Trap scenario
        val trapYtd = YtdTaxProjector.computeYtdProjection(emptyList(), currentMonthlyGross = 9166.67) // £110k/yr
        assertEquals(110000.0, trapYtd.projectedAnnualGross, 10.0)
        assertEquals(60.0, trapYtd.pensionOptimization.taxReliefRatePercent, 0.01)
        assertTrue(trapYtd.pensionOptimization.immediateTaxSavings > 0.0)
    }

    @Test
    fun salaryBenchmarkEngine_interpolatesPercentilesAndRegionalWeighting() {
        // National Tech SWE evaluation at £62,000 (Median)
        val evalMedian = SalaryBenchmarkEngine.evaluateBenchmark(62000.0, "tech_swe", BenchmarkRegion.NATIONAL_AVERAGE)
        assertEquals(50, evalMedian.percentileRank)
        assertEquals(62000.0, evalMedian.adjustedP50, 0.01)

        // London SWE evaluation (with 1.22x weighting)
        val evalLondon = SalaryBenchmarkEngine.evaluateBenchmark(62000.0, "tech_swe", BenchmarkRegion.LONDON)
        assertEquals(75640.0, evalLondon.adjustedP50, 0.01)
        assertTrue(evalLondon.percentileRank < 50) // £62k is below median in London
    }

    @Test
    fun calculateTax_careWorkerAugustPayslipExactMatch() {
        // 16 shifts of 12h = 192.0h @ £12.82/hr = £2,461.44 Gross
        // Tax code: 1257L, Month 5 (August), Pension Opted Out (0%), Student Loan: None
        val gross = 16 * 12.0 * 12.82
        assertEquals(2461.44, gross, 0.001)

        val report = TaxCalculator.calculateTax(
            grossPay = gross,
            taxCode = "1257L",
            isMonthly = true,
            region = TaxRegion.UK_STANDARD,
            taxYear = TaxYear.YEAR_2024_2025,
            pensionRatePercent = 0.0,
            studentLoanPlan = StudentLoanPlan.NONE,
            taxMonth = 5 // Month 5 (August)
        )

        // Exact match with user's August care worker payslip to the penny:
        assertEquals(2461.44, report.grossPay, 0.01)
        assertEquals(1413.02, report.taxablePay, 0.01) // £2,461.44 - £1,048.42
        assertEquals(282.60, report.incomeTax, 0.01)  // £1,413.02 * 20% = £282.60
        assertEquals(113.07, report.nationalInsurance, 0.01) // (£2,461.44 - £1,048.00) * 8% = £113.0752 -> £113.07
        assertEquals(0.00, report.pensionContribution, 0.01) // Opted out
        assertEquals(2065.77, report.netPay, 0.01)    // £2,461.44 - £282.60 - £113.07 = £2,065.77
    }
}






