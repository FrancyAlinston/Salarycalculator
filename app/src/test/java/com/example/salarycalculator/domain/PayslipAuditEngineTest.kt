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
    fun testUnderpaymentDetectionWith16ShiftsAgainst15ShiftsPaid() {
        // User worked 16 shifts (12h each = 192.0h total) in September 2026
        val septemberShifts = mapOf(
            1 to 12.0, 3 to 12.0, 5 to 12.0, 7 to 12.0,
            9 to 12.0, 11 to 12.0, 13 to 12.0, 15 to 12.0,
            17 to 12.0, 19 to 12.0, 21 to 12.0, 23 to 12.0,
            25 to 12.0, 27 to 12.0, 29 to 12.0, 30 to 12.0
        )
        assertEquals(16, septemberShifts.size)

        val parsedPayslip = ParsedPayslipData(
            employeeName = "Francy Alinston D'Silva",
            employeeRef = "910",
            niNumber = "RZ021006C",
            employerName = "Waterloo Manor Ltd",
            payPeriod = "September 2026",
            processDate = "30-09-2026",
            taxPeriod = 6,
            taxCode = "1257L",
            basicHours = 179.90,
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
            year = 2026,
            month = 9,
            configuredHourlyRate = 12.82,
            standardShiftDuration = 12.0
        )

        assertEquals(AuditMismatchStatus.UNDERPAID, report.status)
        assertEquals(16, report.timesheetShiftsCount)
        assertEquals(192.0, report.timesheetTotalHours, 0.01)
        assertEquals(179.90, report.payslipPaidBasicHours, 0.01)
        assertEquals(12.10, report.missingHours, 0.01)
        assertEquals(1, report.missingShifts)

        // Missing gross = 12.1h * 12.82 = £155.122
        assertEquals(155.12, report.grossShortfall, 0.1)
        // Net shortfall = gross * (1 - 0.20 - 0.08) = gross * 0.72 = £111.68
        assertEquals(111.68, report.netShortfall, 0.1)
        assertEquals(16, report.workedDays.size)

        // Verify dispute email generation
        val emailBody = PayslipAuditEngine.generateDiscrepancyEmailText(report)
        assertTrue(emailBody.contains("Francy Alinston D'Silva"))
        assertTrue(emailBody.contains("910"))
        assertTrue(emailBody.contains("RZ021006C"))
        assertTrue(emailBody.contains("Waterloo Manor Ltd"))
        assertTrue(emailBody.contains("16 shifts"))
        assertTrue(emailBody.contains("192.00 hours"))
        assertTrue(emailBody.contains("179.90 hours"))
        assertTrue(emailBody.contains("Tue 01 Sep 2026"))
        assertTrue(emailBody.contains("Wed 30 Sep 2026"))

        val subject = PayslipAuditEngine.generateDisputeEmailSubject(report)
        assertTrue(subject.contains("Francy Alinston D'Silva"))
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
            standardShiftDuration = 12.0
        )

        assertEquals(AuditMismatchStatus.IN_SYNC, report.status)
        assertEquals(0.0, report.missingHours, 0.01)
        assertEquals(0, report.missingShifts)
        assertEquals(0.0, report.grossShortfall, 0.01)
    }
}
