package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class PayslipAuditEngineTest {

    @Test
    fun testWaterlooManorPayslipParsing() {
        val samplePayslipOcr = """
            Ref. Employee Name Process Date N.I. Number
            910 Francy Alinston D'Silva 30-09-2026 RZ021006C
            Payments Units Rate Amount
            Basic 179.90 12.82 2306.32
            Bank Holiday 7.25 6.41 46.47
            Deductions Amount
            PAYE Tax 260.80
            National Insurance 104.38
            Waterloo Manor Ltd
            Tax Period: 6
            Tax Code: 1257L
            Payment Method: BACS
            Payment Period: MONTHLY
            This Period
            Total Gross Pay 2352.79
            Gross for Tax 2352.79
            Tax Paid 260.80
            Earnings for NI 2352.79
            National Insurance 104.38
            Net Pay 1987.61
        """.trimIndent()

        val parsed = PayslipParserEngine.parsePayslipText(samplePayslipOcr)

        assertEquals("Francy Alinston D'Silva", parsed.employeeName)
        assertEquals("910", parsed.employeeRef)
        assertEquals("RZ021006C", parsed.niNumber)
        assertEquals("30-09-2026", parsed.processDate)
        assertEquals(6, parsed.taxPeriod)
        assertEquals("1257L", parsed.taxCode)
        assertEquals(179.90, parsed.basicHours, 0.01)
        assertEquals(12.82, parsed.basicRate, 0.01)
        assertEquals(2306.32, parsed.basicAmount, 0.01)
        assertEquals(7.25, parsed.bankHolidayHours, 0.01)
        assertEquals(6.41, parsed.bankHolidayRate, 0.01)
        assertEquals(46.47, parsed.bankHolidayAmount, 0.01)
        assertEquals(2352.79, parsed.grossPay, 0.01)
        assertEquals(260.80, parsed.incomeTax, 0.01)
        assertEquals(104.38, parsed.nationalInsurance, 0.01)
        assertEquals(1987.61, parsed.netPay, 0.01)
        assertEquals("Waterloo Manor Ltd", parsed.employerName)
    }

    @Test
    fun testUnderpaymentDetectionWithCutoffWindowAwareness() {
        // August post-cutoff shifts (after Aug 23 cutoff): 5 shifts (60h)
        val augustShifts = mapOf(
            24 to 12.0, 26 to 12.0, 28 to 12.0, 30 to 12.0, 31 to 12.0
        )

        // September shifts: 11 in-cycle shifts (before/on Sep 20 cutoff) + 3 post-cutoff shifts
        val septemberShifts = mapOf(
            1 to 12.0, 3 to 12.0, 5 to 12.0, 7 to 12.0,
            9 to 12.0, 11 to 12.0, 13 to 12.0, 15 to 12.0,
            17 to 12.0, 19 to 12.0, 20 to 12.0,
            // Post-cutoff shifts (rolling into October)
            22 to 12.0, 24 to 12.0, 26 to 12.0
        )

        val parsedPayslip = ParsedPayslipData(
            employeeName = "Francy Alinston D'Silva",
            employeeRef = "910",
            niNumber = "RZ021006C",
            employerName = "Waterloo Manor Ltd",
            payPeriod = "September 2026",
            processDate = "30-09-2026",
            taxPeriod = 6,
            taxCode = "1257L",
            basicHours = 179.90, // Paid for ~15 shifts
            basicRate = 12.82,
            basicAmount = 2306.32,
            bankHolidayHours = 7.25,
            bankHolidayRate = 6.41,
            bankHolidayAmount = 46.47,
            grossPay = 2352.79,
            incomeTax = 260.80,
            nationalInsurance = 104.38,
            netPay = 1987.61
        )

        val report = PayslipAuditEngine.auditPayslipAgainstTimesheet(
            payslip = parsedPayslip,
            monthShifts = septemberShifts,
            previousMonthShifts = augustShifts,
            year = 2026,
            month = 9,
            configuredHourlyRate = 12.82,
            standardShiftDuration = 12.0,
            payScheduleConfig = PayScheduleConfig(PayScheduleType.LAST_FRIDAY_OF_MONTH),
            useCutoffWindow = true
        )

        assertEquals(AuditMismatchStatus.UNDERPAID, report.status)
        assertEquals("24 Aug 2026", report.payCycleStartDate)
        assertEquals("20 Sep 2026", report.payCycleCutoffDate)
        assertEquals("25 Sep 2026", report.payDate)

        // 5 shifts from August rollover + 11 shifts from September before cutoff = 16 qualifying shifts
        assertEquals(16, report.timesheetShiftsCount)
        assertEquals(192.0, report.timesheetTotalHours, 0.01)
        assertEquals(3, report.postCutoffRolloverDays.size)

        // Verify dispute email generation mentions strictly the in-cycle cutoff window
        val emailBody = PayslipAuditEngine.generateDiscrepancyEmailText(report)
        assertTrue(emailBody.contains("Francy Alinston D'Silva"))
        assertTrue(emailBody.contains("910"))
        assertTrue(emailBody.contains("24 Aug 2026 to 20 Sep 2026"))
        assertTrue(emailBody.contains("Mon 24 Aug 2026"))
        assertTrue(emailBody.contains("Tue 01 Sep 2026"))
        // Assert that post-cutoff details are NOT included in the email
        assertFalse(emailBody.contains("POST-CUTOFF SHIFTS WORKED"))
        assertFalse(emailBody.contains("22 Sep 2026"))

        val subject = PayslipAuditEngine.generateDisputeEmailSubject(report)
        assertTrue(subject.contains("24 Aug 2026 to 20 Sep 2026"))
        assertTrue(subject.contains("910"))
    }

    @Test
    fun testInSyncScheduleScenario() {
        val shifts = mapOf(
            1 to 12.0, 2 to 12.0, 3 to 12.0, 4 to 12.0, 5 to 12.0,
            6 to 12.0, 7 to 12.0, 8 to 12.0, 9 to 12.0, 10 to 12.0,
            11 to 12.0, 12 to 12.0, 13 to 12.0, 14 to 12.0, 15 to 12.0
        )
        val payslip = ParsedPayslipData(
            basicHours = 180.0,
            basicRate = 12.82,
            grossPay = 2307.60,
            netPay = 1950.00
        )

        val report = PayslipAuditEngine.auditPayslipAgainstTimesheet(
            payslip = payslip,
            monthShifts = shifts,
            year = 2026,
            month = 9,
            configuredHourlyRate = 12.82,
            standardShiftDuration = 12.0,
            useCutoffWindow = false
        )

        assertEquals(AuditMismatchStatus.IN_SYNC, report.status)
        assertEquals(0.0, report.missingHours, 0.01)
        assertEquals(0, report.missingShifts)
        assertEquals(0.0, report.grossShortfall, 0.01)
    }
}
