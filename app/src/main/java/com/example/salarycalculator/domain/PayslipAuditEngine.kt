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
 * Selectable tone and formatting templates for generated payroll dispute emails.
 */
enum class DisputeEmailTemplate(val displayName: String, val subtitle: String) {
    FORMAL("Formal (HR Standard)", "Polite, comprehensive breakdown with full employee credentials"),
    DIRECT("Direct (Brief)", "Concise summary table and bulleted shortfall for quick processing"),
    URGENT("Urgent (Priority)", "High-priority notice requesting expedited interim BACS payment")
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
    val payCycleStartDate: String,
    val payCycleCutoffDate: String,
    val payDate: String,
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
    val postCutoffRolloverDays: List<WorkedShiftDay> = emptyList(),
    val discrepancySummary: String
)

object PayslipAuditEngine {

    /**
     * Cross-references an imported or manually entered payslip against the DataStore shift schedule,
     * strictly observing the employer's timesheet cutoff window (Previous Cutoff + 1 -> Current Cutoff).
     */
    fun auditPayslipAgainstTimesheet(
        payslip: ParsedPayslipData,
        monthShifts: Map<Int, Double>,
        previousMonthShifts: Map<Int, Double> = emptyMap(),
        year: Int,
        month: Int,
        configuredHourlyRate: Double = 12.82,
        standardShiftDuration: Double = 12.0,
        payScheduleConfig: PayScheduleConfig = PayScheduleConfig(),
        useCutoffWindow: Boolean = true
    ): PayslipAuditReport {
        val effectiveRate = if (payslip.basicRate > 0.0) payslip.basicRate else configuredHourlyRate
        val effectiveStdShift = if (standardShiftDuration > 0.0) standardShiftDuration else 12.0

        val payPeriodInfo = PayScheduleEngine.calculatePayPeriod(year, month, payScheduleConfig)
        val cutoffDay = payPeriodInfo.cutoffDay
        val startDay = payPeriodInfo.startDay
        val startMonth = payPeriodInfo.startMonth
        val startYear = payPeriodInfo.startYear

        val prevMonth = if (month == 1) 12 else month - 1
        val prevYear = if (month == 1) year - 1 else year

        val startDateFormatted = try {
            LocalDate.of(startYear, startMonth, startDay).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        } catch (_: Exception) {
            "$startDay/$startMonth/$startYear"
        }

        val cutoffDateFormatted = try {
            LocalDate.of(payPeriodInfo.cutoffYear, payPeriodInfo.cutoffMonth, cutoffDay).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        } catch (_: Exception) {
            "$cutoffDay/$month/$year"
        }

        val payDateFormatted = try {
            LocalDate.of(payPeriodInfo.payYear, payPeriodInfo.payMonth, payPeriodInfo.payDay).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        } catch (_: Exception) {
            "${payPeriodInfo.payDay}/$month/$year"
        }

        val qualifyingWorkedDays = mutableListOf<WorkedShiftDay>()
        val postCutoffRolloverDays = mutableListOf<WorkedShiftDay>()

        var timesheetTotalHours = 0.0
        var timesheetStandardHours = 0.0
        var timesheetOvertimeHours = 0.0

        if (useCutoffWindow) {
            // 1. Process previous month's post-cutoff rollover shifts (between previous cutoff + 1 and end of previous month)
            if (previousMonthShifts.isNotEmpty()) {
                val prevSortedDays = previousMonthShifts.keys.filter { it in startDay..31 }.sorted()
                for (day in prevSortedDays) {
                    val hrs = previousMonthShifts[day] ?: 0.0
                    if (hrs != 0.0) {
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
                            val ld = LocalDate.of(prevYear, prevMonth, day)
                            ld.format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.ENGLISH))
                        } catch (_: Exception) {
                            "Day $day"
                        }

                        qualifyingWorkedDays.add(
                            WorkedShiftDay(
                                dayOfMonth = day,
                                dateFormatted = dateStr,
                                hours = actualHrs,
                                isOvertime = isOt,
                                shiftTypeDescription = "${actualHrs}h Shift (${LocalDate.of(prevYear, prevMonth, 1).format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))} Post-Cutoff Rollover)"
                            )
                        )
                    }
                }
            }

            // 2. Process current month's shifts on or before the cutoff date
            val currentSortedDays = monthShifts.keys.filter { it in 1..31 }.sorted()
            for (day in currentSortedDays) {
                val hrs = monthShifts[day] ?: 0.0
                if (hrs != 0.0) {
                    val actualHrs = abs(hrs)
                    val isOt = hrs < 0.0 || actualHrs > effectiveStdShift

                    val dateStr = try {
                        val ld = LocalDate.of(year, month, day)
                        ld.format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.ENGLISH))
                    } catch (_: Exception) {
                        "Day $day"
                    }

                    if (day <= cutoffDay) {
                        timesheetTotalHours += actualHrs
                        if (hrs < 0.0) {
                            timesheetOvertimeHours += actualHrs
                        } else if (actualHrs > effectiveStdShift) {
                            timesheetStandardHours += effectiveStdShift
                            timesheetOvertimeHours += (actualHrs - effectiveStdShift)
                        } else {
                            timesheetStandardHours += actualHrs
                        }

                        qualifyingWorkedDays.add(
                            WorkedShiftDay(
                                dayOfMonth = day,
                                dateFormatted = dateStr,
                                hours = actualHrs,
                                isOvertime = isOt,
                                shiftTypeDescription = when {
                                    hrs < 0.0 -> "${actualHrs}h Dedicated OT Shift (In-Cycle)"
                                    actualHrs >= 12.0 -> "${actualHrs}h Standard Care Shift (In-Cycle)"
                                    actualHrs >= 8.0 -> "${actualHrs}h Standard Shift (In-Cycle)"
                                    else -> "${actualHrs}h Part-Time Shift (In-Cycle)"
                                }
                            )
                        )
                    } else {
                        // Post-Cutoff shifts rolling over to next month
                        postCutoffRolloverDays.add(
                            WorkedShiftDay(
                                dayOfMonth = day,
                                dateFormatted = dateStr,
                                hours = actualHrs,
                                isOvertime = isOt,
                                shiftTypeDescription = "${actualHrs}h Shift (Post-Cutoff · Rolls to Next Payslip)"
                            )
                        )
                    }
                }
            }
        } else {
            // Full calendar month mode (1st..end of month)
            val sortedDays = monthShifts.keys.filter { it in 1..31 }.sorted()
            for (day in sortedDays) {
                val hrs = monthShifts[day] ?: 0.0
                if (hrs != 0.0) {
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

                    qualifyingWorkedDays.add(
                        WorkedShiftDay(
                            dayOfMonth = day,
                            dateFormatted = dateStr,
                            hours = actualHrs,
                            isOvertime = isOt,
                            shiftTypeDescription = when {
                                hrs < 0.0 -> "${actualHrs}h Dedicated OT Shift"
                                actualHrs >= 12.0 -> "${actualHrs}h Standard Care Shift"
                                actualHrs >= 8.0 -> "${actualHrs}h Standard Shift"
                                else -> "${actualHrs}h Part-Time Shift"
                            }
                        )
                    )
                }
            }
        }

        val timesheetShiftsCount = qualifyingWorkedDays.size

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
                "Shortfall of $shiftTxt (${ "%.2f".format(missingHours) } hrs) in pay cycle ($startDateFormatted – $cutoffDateFormatted). You are owed approximately £${ "%.2f".format(grossShortfall) } gross / £${ "%.2f".format(netShortfall) } net take-home."
            }
            AuditMismatchStatus.OVERPAID -> {
                "Payslip includes ${"%.2f".format(abs(missingHours))} more hours than logged in your timesheet for cycle ($startDateFormatted – $cutoffDateFormatted)."
            }
            AuditMismatchStatus.RATE_DISCREPANCY -> {
                "Paid hourly rate (£${ "%.2f".format(payslip.basicRate) }) differs from your configured rate (£${ "%.2f".format(configuredHourlyRate) })."
            }
            AuditMismatchStatus.IN_SYNC -> {
                "Exact match! Paid hours (${ "%.2f".format(paidBasicHours) }h) align with your qualifying timesheet shifts (${ "%.2f".format(timesheetTotalHours) }h across $timesheetShiftsCount shifts in cycle $startDateFormatted – $cutoffDateFormatted)."
            }
        }

        val monthName = try {
            LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
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
            payCycleStartDate = startDateFormatted,
            payCycleCutoffDate = cutoffDateFormatted,
            payDate = payDateFormatted,
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
            workedDays = qualifyingWorkedDays,
            postCutoffRolloverDays = postCutoffRolloverDays,
            discrepancySummary = summary
        )
    }

    /**
     * Generates a formal subject line for the discrepancy email.
     */
    fun generateDisputeEmailSubject(
        report: PayslipAuditReport,
        template: DisputeEmailTemplate = DisputeEmailTemplate.FORMAL
    ): String {
        val refPart = if (report.employeeRef.isNotBlank()) " (Ref: ${report.employeeRef})" else ""
        return when (template) {
            DisputeEmailTemplate.FORMAL ->
                "Payslip Discrepancy Inquiry (${report.payCycleStartDate} to ${report.payCycleCutoffDate}) - ${report.employeeName}$refPart"
            DisputeEmailTemplate.DIRECT ->
                "Timesheet Variance Query - ${report.payPeriod} - ${report.employeeName}$refPart"
            DisputeEmailTemplate.URGENT ->
                "URGENT: Payroll Underpayment Correction Required (${report.payCycleStartDate} to ${report.payCycleCutoffDate}) - ${report.employeeName}$refPart"
        }
    }

    /**
     * Formats a professional dispute email body according to the selected template, strictly observing the cutoff window.
     */
    fun generateDiscrepancyEmailText(
        report: PayslipAuditReport,
        template: DisputeEmailTemplate = DisputeEmailTemplate.FORMAL
    ): String {
        val sb = StringBuilder()

        when (template) {
            DisputeEmailTemplate.FORMAL -> {
                sb.append("Dear Payroll & Human Resources Team,\n\n")
                sb.append("I am writing to respectfully query a discrepancy identified on my payslip for ${report.payPeriod} (Pay Date: ${report.payDate}).\n\n")

                sb.append("=== EMPLOYEE & PAYROLL DETAILS ===\n")
                sb.append("• Employee Name: ${report.employeeName}\n")
                if (report.employeeRef.isNotBlank()) sb.append("• Employee / Payroll Ref: ${report.employeeRef}\n")
                if (report.niNumber.isNotBlank()) sb.append("• National Insurance No: ${report.niNumber}\n")
                if (report.employerName.isNotBlank()) sb.append("• Employer: ${report.employerName}\n")
                if (report.taxPeriod != null) sb.append("• Tax Period: Month ${report.taxPeriod}\n")
                if (!report.processDate.isNullOrBlank()) sb.append("• Process Date: ${report.processDate}\n")
                sb.append("• Tax Code: ${report.taxCode}\n")
                sb.append("• Timesheet Pay Cycle: ${report.payCycleStartDate} to ${report.payCycleCutoffDate} (Cutoff: ${report.payCycleCutoffDate} at 23:59)\n\n")

                sb.append("=== DISCREPANCY SUMMARY ===\n")
                sb.append("In accordance with our payroll schedule, this pay period accounts for shifts worked between the previous cutoff date (${report.payCycleStartDate}) and this month's cutoff date (${report.payCycleCutoffDate}).\n\n")
                sb.append("During this exact cutoff window, my verified timesheet records ${report.timesheetShiftsCount} worked shifts totaling ${ "%.2f".format(report.timesheetTotalHours) } hours.\n")
                sb.append("However, the payslip provides payment for only ${ "%.2f".format(report.payslipPaidBasicHours) } basic hours (equivalent to approximately ${ (report.payslipPaidBasicHours / 12.0).roundToInt() } shifts).\n\n")

                sb.append("• Qualifying Timesheet Hours: ${ "%.2f".format(report.timesheetTotalHours) } hrs (${report.timesheetShiftsCount} shifts in cycle)\n")
                sb.append("• Payslip Paid Basic Hours: ${ "%.2f".format(report.payslipPaidBasicHours) } hrs\n")
                val missingShiftsLabel = if (report.missingShifts > 0) " (${report.missingShifts} shift)" else ""
                sb.append("• Outstanding Missing Hours: ${ "%.2f".format(report.missingHours) } hrs$missingShiftsLabel\n")
                sb.append("• Basic Hourly Rate: £${ "%.2f".format(report.payslipPaidRate) }/hr\n")
                sb.append("• Missing Gross Shortfall: £${ "%.2f".format(report.grossShortfall) }\n")
                sb.append("• Estimated Net Underpayment: £${ "%.2f".format(report.netShortfall) } (after statutory 20% PAYE & 8% NI)\n\n")

                sb.append("=== ITEMIZED DATES WORKED IN THIS PAY CYCLE (${report.payCycleStartDate.uppercase()} – ${report.payCycleCutoffDate.uppercase()}) ===\n")
                if (report.workedDays.isEmpty()) {
                    sb.append("• Please refer to attached timesheet record.\n")
                } else {
                    report.workedDays.forEachIndexed { idx, day ->
                        sb.append("${idx + 1}. ${day.dateFormatted} — ${day.shiftTypeDescription}\n")
                    }
                }
                sb.append("\nTotal In-Cycle Worked: ${report.workedDays.size} shifts (${ "%.2f".format(report.timesheetTotalHours) } hours)\n\n")

                sb.append("=== REQUEST FOR RESOLUTION ===\n")
                sb.append("Could you kindly cross-check your timesheet records against the in-cycle dates listed above and arrange a supplementary BACS adjustment payment or include the outstanding £${ "%.2f".format(report.grossShortfall) } gross in the upcoming payroll run?\n\n")
                sb.append("Please let me know if you need any additional timesheet sign-off sheets from my ward / unit manager.\n\n")
                sb.append("Thank you very much for your time and assistance.\n\n")
                sb.append("Yours sincerely,\n")
                sb.append("${report.employeeName}\n")
            }

            DisputeEmailTemplate.DIRECT -> {
                sb.append("Hi Payroll Team,\n\n")
                sb.append("Please review an hours variance on my payslip for ${report.payPeriod} (${report.employeeName}${if (report.employeeRef.isNotBlank()) ", Ref: ${report.employeeRef}" else ""}):\n\n")

                sb.append("• Pay Cycle Window: ${report.payCycleStartDate} to ${report.payCycleCutoffDate}\n")
                sb.append("• Timesheet Hours Worked: ${ "%.2f".format(report.timesheetTotalHours) } hrs (${report.timesheetShiftsCount} shifts)\n")
                sb.append("• Payslip Paid Basic Hours: ${ "%.2f".format(report.payslipPaidBasicHours) } hrs\n")
                sb.append("• Unpaid Shortfall: ${ "%.2f".format(report.missingHours) } hrs (Gross Shortfall: £${ "%.2f".format(report.grossShortfall) } / ~£${ "%.2f".format(report.netShortfall) } net)\n")
                sb.append("• Basic Rate: £${ "%.2f".format(report.payslipPaidRate) }/hr\n\n")

                sb.append("In-Cycle Worked Dates:\n")
                if (report.workedDays.isEmpty()) {
                    sb.append("• (See attached timesheet)\n")
                } else {
                    report.workedDays.forEachIndexed { idx, day ->
                        sb.append("${idx + 1}. ${day.dateFormatted} (${day.hours}h)\n")
                    }
                }
                sb.append("\nPlease let me know when the missing £${ "%.2f".format(report.grossShortfall) } gross adjustment will be processed.\n\n")
                sb.append("Thanks,\n")
                sb.append("${report.employeeName}\n")
            }

            DisputeEmailTemplate.URGENT -> {
                sb.append("Dear Payroll Management,\n\n")
                sb.append("URGENT: I am writing to notify you of a critical underpayment on my payslip for ${report.payPeriod} (Pay Date: ${report.payDate}).\n\n")

                sb.append("During the active pay cycle (${report.payCycleStartDate} to ${report.payCycleCutoffDate}), I completed ${report.timesheetShiftsCount} shifts (${ "%.2f".format(report.timesheetTotalHours) } hrs), but was paid for only ${ "%.2f".format(report.payslipPaidBasicHours) } basic hours.\n\n")
                sb.append("This leaves a severe shortfall of ${ "%.2f".format(report.missingHours) } unpaid hours, amounting to £${ "%.2f".format(report.grossShortfall) } gross (approximately £${ "%.2f".format(report.netShortfall) } net take-home deficit).\n\n")

                sb.append("=== EMPLOYEE DETAILS ===\n")
                sb.append("• Employee: ${report.employeeName}\n")
                if (report.employeeRef.isNotBlank()) sb.append("• Ref: ${report.employeeRef}\n")
                if (report.niNumber.isNotBlank()) sb.append("• NI No: ${report.niNumber}\n")
                sb.append("• Tax Code: ${report.taxCode}\n\n")

                sb.append("=== IN-CYCLE SHIFTS WORKED ===\n")
                if (report.workedDays.isEmpty()) {
                    sb.append("• Please refer to attached timesheet record.\n")
                } else {
                    report.workedDays.forEachIndexed { idx, day ->
                        sb.append("${idx + 1}. ${day.dateFormatted} — ${day.shiftTypeDescription}\n")
                    }
                }

                sb.append("\nAs this shortfall directly affects my essential living commitments, I kindly request that an urgent interim BACS payment or priority adjustment be arranged within 24–48 hours.\n\n")
                sb.append("Thank you for your prompt attention to this matter.\n\n")
                sb.append("Yours sincerely,\n")
                sb.append("${report.employeeName}\n")
            }
        }

        return sb.toString()
    }

    /**
     * Dispatches an Android ACTION_SENDTO / ACTION_SEND intent to open the user's email client,
     * optionally attaching a generated Timesheet PDF file if provided.
     */
    fun dispatchDisputeEmail(
        context: Context,
        report: PayslipAuditReport,
        recipientEmail: String = "",
        template: DisputeEmailTemplate = DisputeEmailTemplate.FORMAL,
        attachmentFile: java.io.File? = null
    ) {
        val subject = generateDisputeEmailSubject(report, template)
        val body = generateDiscrepancyEmailText(report, template)

        if (attachmentFile != null && attachmentFile.exists()) {
            EmailExporter.dispatchEmailWithAttachment(
                context = context,
                file = attachmentFile,
                subject = subject,
                bodyText = body,
                recipientEmail = recipientEmail
            )
            return
        }

        val mailtoUri = Uri.parse("mailto:${Uri.encode(recipientEmail)}")
        val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(emailIntent)
        } catch (_: Exception) {
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
    fun copyDiscrepancyTextToClipboard(
        context: Context,
        report: PayslipAuditReport,
        template: DisputeEmailTemplate = DisputeEmailTemplate.FORMAL
    ): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = generateDiscrepancyEmailText(report, template)
            val clip = ClipData.newPlainText("Payslip Dispute Email", text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }
}
