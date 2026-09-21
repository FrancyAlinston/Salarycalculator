package com.example.salarycalculator.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Status indicating the alignment between recorded timesheet shifts and paid payslip metrics.
 */
enum class AuditMismatchStatus(val label: String, val isProblem: Boolean) {
    UNDERPAID("Underpayment Detected", true),
    OVERPAID("Overpayment Detected", true),
    IN_SYNC("Exact Timesheet Match", false),
    RATE_DISCREPANCY("Hourly Rate Discrepancy", true)
}

/**
 * Detailed representation of an individual worked shift day in the audited month.
 */
data class WorkedShiftDay(
    val dayOfMonth: Int,
    val dateFormatted: String,
    val hours: Double,
    val isOvertime: Boolean,
    val shiftTypeDescription: String
)

/**
 * Comprehensive diagnostic audit comparing recorded timesheet shifts against paid payslip figures.
 */
data class PayslipAuditReport(
    val employeeName: String,
    val employeeRef: String,
    val niNumber: String,
    val employerName: String,
    val taxPeriod: Int?,
    val payPeriod: String,
    val processDate: String?,
    val taxCode: String,
    val status: AuditMismatchStatus,
    val timesheetShiftsCount: Int,
    val timesheetTotalHours: Double,
    val timesheetStandardHours: Double,
    val timesheetOvertimeHours: Double,
    val timesheetExpectedGross: Double,
    val timesheetExpectedNet: Double,
    val payslipPaidBasicHours: Double,
    val payslipPaidRate: Double,
    val payslipPaidBankHolidayHours: Double,
    val payslipPaidBankHolidayRate: Double,
    val payslipPaidGross: Double,
    val payslipPaidTax: Double,
    val payslipPaidNI: Double,
    val payslipPaidNet: Double,
    val missingHours: Double,
    val missingShifts: Int,
    val grossShortfall: Double,
    val payeShortfall: Double,
    val niShortfall: Double,
    val netShortfall: Double,
    val workedDays: List<WorkedShiftDay>,
    val discrepancySummary: String
)

object PayslipAuditEngine {

    /**
     * Cross-references an imported or manually entered payslip against the DataStore shift schedule.
     */
    fun auditPayslipAgainstTimesheet(
        payslip: ParsedPayslipData,
        monthShifts: Map<Int, Double>,
        year: Int,
        month: Int,
        configuredHourlyRate: Double = 12.82,
        standardShiftDuration: Double = 12.0
    ): PayslipAuditReport {
        val effectiveRate = if (payslip.basicRate > 0.0) payslip.basicRate else configuredHourlyRate
        val effectiveStdShift = if (standardShiftDuration > 0.0) standardShiftDuration else 12.0

        // Parse recorded days worked in the month from heatmap
        val workedDays = mutableListOf<WorkedShiftDay>()
        var timesheetTotalHours = 0.0
        var timesheetStandardHours = 0.0
        var timesheetOvertimeHours = 0.0
        var timesheetShiftsCount = 0

        val sortedDays = monthShifts.keys.filter { it in 1..31 }.sorted()
        for (day in sortedDays) {
            val hrs = monthShifts[day] ?: 0.0
            if (hrs != 0.0) {
                timesheetShiftsCount++
                val actualHrs = abs(hrs)
                val isOt = hrs < 0.0 || actualHrs > effectiveStdShift
                timesheetTotalHours += actualHrs

                if (hrs < 0.0) {
                    timesheetOvertimeHours += actualHrs
                } else if (actualHrs > effectiveStdShift) {
                    timesheetStandardHours += effectiveStdShift
                    timesheetOvertimeHours += (actualHrs - effectiveStdShift)
                } else {
                    timesheetStandardHours += actualHrs
                }

                val dateStr = try {
                    val ld = LocalDate.of(year, month, day)
                    ld.format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.ENGLISH))
                } catch (_: Exception) {
                    "Day $day"
                }

                val desc = when {
                    hrs < 0.0 -> "${actualHrs}h Dedicated OT Shift"
                    actualHrs >= 12.0 -> "${actualHrs}h Standard Care Shift"
                    actualHrs >= 8.0 -> "${actualHrs}h Standard Shift"
                    else -> "${actualHrs}h Part-Time Shift"
                }

                workedDays.add(
                    WorkedShiftDay(
                        dayOfMonth = day,
                        dateFormatted = dateStr,
                        hours = actualHrs,
                        isOvertime = isOt,
                        shiftTypeDescription = desc
                    )
                )
            }
        }

        // Expected gross calculation from timesheet
        val timesheetExpectedGross = (timesheetStandardHours * effectiveRate) + (timesheetOvertimeHours * effectiveRate * 1.5)
        val taxResult = TaxCalculator.calculateTax(
            grossPay = timesheetExpectedGross,
            taxCode = payslip.taxCode.ifBlank { "1257L" },
            isMonthly = true
        )
        val timesheetExpectedNet = taxResult.netPay

        // Paid metrics from payslip
        val paidBasicHours = if (payslip.basicHours > 0.0) {
            payslip.basicHours
        } else if (payslip.grossPay > 0.0 && effectiveRate > 0.0) {
            payslip.grossPay / effectiveRate
        } else {
            0.0
        }

        val paidShiftsEstimate = if (effectiveStdShift > 0.0) {
            (paidBasicHours / effectiveStdShift).roundToInt()
        } else {
            (paidBasicHours / 12.0).roundToInt()
        }

        // Variance calculations
        val missingHours = timesheetTotalHours - paidBasicHours
        val missingShifts = timesheetShiftsCount - paidShiftsEstimate
        val grossShortfall = (timesheetTotalHours - paidBasicHours) * effectiveRate

        // Marginal deductions on shortfall (20% PAYE + 8% Class 1 NI = 28% total deduction, 72% net retention)
        val payeShortfall = if (grossShortfall > 0.0) grossShortfall * 0.20 else 0.0
        val niShortfall = if (grossShortfall > 0.0) grossShortfall * 0.08 else 0.0
        val netShortfall = if (grossShortfall > 0.0) grossShortfall - payeShortfall - niShortfall else 0.0

        // Determine Status
        val status = when {
            missingHours >= 1.0 -> AuditMismatchStatus.UNDERPAID
            missingHours <= -1.0 -> AuditMismatchStatus.OVERPAID
            abs(payslip.basicRate - configuredHourlyRate) > 0.05 && payslip.basicRate > 0.0 -> AuditMismatchStatus.RATE_DISCREPANCY
            else -> AuditMismatchStatus.IN_SYNC
        }

        val summary = when (status) {
            AuditMismatchStatus.UNDERPAID -> {
                val shiftTxt = if (missingShifts > 0) "$missingShifts missing shift${if (missingShifts > 1) "s" else ""}" else "${"%.1f".format(missingHours)}h missing"
                "Shortfall of $shiftTxt (${ "%.2f".format(missingHours) } hrs). You are owed approximately £${ "%.2f".format(grossShortfall) } gross / £${ "%.2f".format(netShortfall) } net take-home."
            }
            AuditMismatchStatus.OVERPAID -> {
                "Payslip includes ${"%.2f".format(abs(missingHours))} more hours than logged in your monthly heatmap schedule."
            }
            AuditMismatchStatus.RATE_DISCREPANCY -> {
                "Paid hourly rate (£${ "%.2f".format(payslip.basicRate) }) differs from your configured rate (£${ "%.2f".format(configuredHourlyRate) })."
            }
            AuditMismatchStatus.IN_SYNC -> {
                "Exact match! Paid hours (${ "%.2f".format(paidBasicHours) }h) align with your recorded heatmap shifts (${ "%.2f".format(timesheetTotalHours) }h across $timesheetShiftsCount shifts)."
            }
        }

        val monthName = try {
            LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK))
        } catch (_: Exception) {
            "$month/$year"
        }

        return PayslipAuditReport(
            employeeName = payslip.employeeName ?: "Francy Alinston D'Silva",
            employeeRef = payslip.employeeRef ?: "910",
            niNumber = payslip.niNumber ?: "RZ021006C",
            employerName = payslip.employerName ?: "Waterloo Manor Ltd",
            taxPeriod = payslip.taxPeriod ?: 6,
            payPeriod = payslip.payPeriod.ifBlank { monthName },
            processDate = payslip.processDate ?: "30-09-2026",
            taxCode = payslip.taxCode.ifBlank { "1257L" },
            status = status,
            timesheetShiftsCount = timesheetShiftsCount,
            timesheetTotalHours = timesheetTotalHours,
            timesheetStandardHours = timesheetStandardHours,
            timesheetOvertimeHours = timesheetOvertimeHours,
            timesheetExpectedGross = timesheetExpectedGross,
            timesheetExpectedNet = timesheetExpectedNet,
            payslipPaidBasicHours = paidBasicHours,
            payslipPaidRate = effectiveRate,
            payslipPaidBankHolidayHours = payslip.bankHolidayHours,
            payslipPaidBankHolidayRate = payslip.bankHolidayRate,
            payslipPaidGross = payslip.grossPay,
            payslipPaidTax = payslip.incomeTax,
            payslipPaidNI = payslip.nationalInsurance,
            payslipPaidNet = payslip.netPay,
            missingHours = missingHours,
            missingShifts = missingShifts,
            grossShortfall = grossShortfall,
            payeShortfall = payeShortfall,
            niShortfall = niShortfall,
            netShortfall = netShortfall,
            workedDays = workedDays,
            discrepancySummary = summary
        )
    }

    /**
     * Generates a formal email subject line for the dispute inquiry.
     */
    fun generateDisputeEmailSubject(report: PayslipAuditReport): String {
        val refPart = if (report.employeeRef.isNotBlank()) " (Ref: ${report.employeeRef})" else ""
        return "Urgent: Payslip Hours & Payment Discrepancy - ${report.employeeName}$refPart - ${report.payPeriod}"
    }

    /**
     * Formats a comprehensive, professional dispute email body with itemized shift dates and calculations.
     */
    fun generateDiscrepancyEmailText(report: PayslipAuditReport): String {
        val sb = StringBuilder()
        sb.append("Dear Payroll & Human Resources Team,\n\n")
        sb.append("I am writing to respectfully query a discrepancy identified on my recent payslip for ${report.payPeriod}.\n\n")

        sb.append("=== EMPLOYEE & PAYSLIP DETAILS ===\n")
        sb.append("• Employee Name: ${report.employeeName}\n")
        if (report.employeeRef.isNotBlank()) sb.append("• Employee / Payroll Ref: ${report.employeeRef}\n")
        if (report.niNumber.isNotBlank()) sb.append("• National Insurance No: ${report.niNumber}\n")
        if (report.employerName.isNotBlank()) sb.append("• Employer: ${report.employerName}\n")
        if (report.taxPeriod != null) sb.append("• Tax Period: Month ${report.taxPeriod}\n")
        if (!report.processDate.isNullOrBlank()) sb.append("• Process Date: ${report.processDate}\n")
        sb.append("• Tax Code: ${report.taxCode}\n\n")

        sb.append("=== DISCREPANCY SUMMARY ===\n")
        sb.append("According to my verified duty rota and timesheet, I worked ${report.timesheetShiftsCount} shifts totaling ${ "%.2f".format(report.timesheetTotalHours) } hours in ${report.payPeriod}.\n")
        sb.append("However, my payslip indicates payment for ${ "%.2f".format(report.payslipPaidBasicHours) } hours (equivalent to approximately ${ (report.payslipPaidBasicHours / 12.0).roundToInt() } shifts).\n\n")

        sb.append("• Timesheet Recorded Hours: ${ "%.2f".format(report.timesheetTotalHours) } hrs (${report.timesheetShiftsCount} shifts)\n")
        sb.append("• Payslip Paid Basic Hours: ${ "%.2f".format(report.payslipPaidBasicHours) } hrs\n")
        val missingShiftsLabel = if (report.missingShifts > 0) " (${report.missingShifts} shift)" else ""
        sb.append("• Outstanding Missing Hours: ${ "%.2f".format(report.missingHours) } hrs$missingShiftsLabel\n")
        sb.append("• Basic Hourly Rate: £${ "%.2f".format(report.payslipPaidRate) }/hr\n")
        sb.append("• Missing Gross Shortfall: £${ "%.2f".format(report.grossShortfall) }\n")
        sb.append("• Estimated Net Underpayment: £${ "%.2f".format(report.netShortfall) } (after statutory 20% PAYE & 8% NI)\n\n")

        sb.append("=== ITEMIZED DATES WORKED IN ${report.payPeriod.uppercase()} ===\n")
        if (report.workedDays.isEmpty()) {
            sb.append("• Please refer to attached duty rota schedule.\n")
        } else {
            report.workedDays.forEachIndexed { idx, day ->
                sb.append("${idx + 1}. ${day.dateFormatted} — ${day.shiftTypeDescription}\n")
            }
        }
        sb.append("\nTotal: ${report.workedDays.size} shifts (${ "%.2f".format(report.timesheetTotalHours) } hours)\n\n")

        sb.append("=== REQUEST FOR RESOLUTION ===\n")
        sb.append("Could you please review your timesheet records against the dates listed above and arrange a supplementary adjustment payment or include the missing £${ "%.2f".format(report.grossShortfall) } gross in the upcoming payroll cycle?\n\n")
        sb.append("Please let me know if you require any further timesheet copies or shift sign-off sheets from my ward / unit manager.\n\n")
        sb.append("Thank you for your assistance.\n\n")
        sb.append("Yours sincerely,\n")
        sb.append("${report.employeeName}\n")

        return sb.toString()
    }

    /**
     * Dispatches an Android ACTION_SENDTO intent to open the user's default email client.
     */
    fun dispatchDisputeEmail(
        context: Context,
        report: PayslipAuditReport,
        recipientEmail: String = ""
    ) {
        val subject = generateDisputeEmailSubject(report)
        val body = generateDiscrepancyEmailText(report)

        val mailtoUri = Uri.parse("mailto:${Uri.encode(recipientEmail)}")
        val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(emailIntent)
        } catch (_: Exception) {
            // Fallback to standard ACTION_SEND
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, "Send Dispute Email via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    /**
     * Copies the full dispute email text directly to the system clipboard.
     */
    fun copyDiscrepancyTextToClipboard(context: Context, report: PayslipAuditReport): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = generateDiscrepancyEmailText(report)
            val clip = ClipData.newPlainText("Payslip Dispute Email", text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }
}
