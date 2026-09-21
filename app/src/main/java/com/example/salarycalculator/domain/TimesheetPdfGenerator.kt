package com.example.salarycalculator.domain

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates an official, printable A4 PDF Timesheet Attendance & Discrepancy Verification Document
 * with side-by-side variance analysis and manager sign-off boxes.
 */
object TimesheetPdfGenerator {

    /**
     * Generates a vector PDF timesheet verification document (A4: 595 x 842 pt).
     */
    fun generateTimesheetPdf(context: Context, report: PayslipAuditReport): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Palette
        val colorPrimary = Color.rgb(30, 41, 59)      // Slate 800
        val colorSecondary = Color.rgb(71, 85, 105)  // Slate 600
        val colorAccent = Color.rgb(16, 185, 129)     // Emerald
        val colorWarning = Color.rgb(225, 29, 72)    // Rose 600
        val colorBgLight = Color.rgb(248, 250, 252)   // Slate 50
        val colorBorder = Color.rgb(226, 232, 240)    // Slate 200

        // 1. Header Banner
        paint.color = colorPrimary
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 85f, paint)

        // Document Title
        paint.color = Color.WHITE
        paint.textSize = 17f
        paint.isFakeBoldText = true
        canvas.drawText("TIMESHEET ATTENDANCE & AUDIT STATEMENT", 36f, 38f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("OFFICIAL WORK SCHEDULE & PAYSLIP DISCREPANCY VERIFICATION", 36f, 56f, paint)

        // Pay Period Badge on Right
        paint.textSize = 13f
        paint.isFakeBoldText = true
        paint.color = colorAccent
        val periodText = report.payPeriod.uppercase(Locale.ENGLISH)
        val periodWidth = paint.measureText(periodText)
        canvas.drawText(periodText, 595f - 36f - periodWidth, 42f, paint)

        paint.textSize = 9f
        paint.color = if (report.status.isProblem) colorWarning else colorAccent
        val statusText = report.status.label.uppercase(Locale.ENGLISH)
        val statusWidth = paint.measureText(statusText)
        canvas.drawText(statusText, 595f - 36f - statusWidth, 58f, paint)

        // 2. Metadata Cards (Side by Side)
        var currentY = 100f

        // Left Card: Employee Details
        val cardHeight = 85f
        paint.color = colorBgLight
        paint.style = Paint.Style.FILL
        val leftCardRect = RectF(36f, currentY, 285f, currentY + cardHeight)
        canvas.drawRoundRect(leftCardRect, 6f, 6f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(leftCardRect, 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("EMPLOYEE & IDENTITY", 48f, currentY + 18f, paint)

        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        paint.color = colorSecondary
        canvas.drawText("Name: ${report.employeeName}", 48f, currentY + 34f, paint)
        canvas.drawText("Payroll Ref: ${report.employeeRef.ifBlank { "N/A" }}", 48f, currentY + 48f, paint)
        canvas.drawText("NI Number: ${report.niNumber.ifBlank { "N/A" }}", 48f, currentY + 62f, paint)
        canvas.drawText("Tax Code: ${report.taxCode} · Month ${report.taxPeriod ?: 6}", 48f, currentY + 76f, paint)

        // Right Card: Employer & Pay Cycle Window
        val rightCardRect = RectF(300f, currentY, 559f, currentY + cardHeight)
        paint.color = colorBgLight
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rightCardRect, 6f, 6f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rightCardRect, 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("PAYROLL & CUTOFF CYCLE", 312f, currentY + 18f, paint)

        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        paint.color = colorSecondary
        canvas.drawText("Employer: ${report.employerName.ifBlank { "Payroll Dept" }}", 312f, currentY + 34f, paint)
        canvas.drawText("Pay Cycle: ${report.payCycleStartDate} to ${report.payCycleCutoffDate}", 312f, currentY + 48f, paint)
        canvas.drawText("Cutoff Date: ${report.payCycleCutoffDate} (23:59)", 312f, currentY + 62f, paint)
        canvas.drawText("Pay Date: ${report.payDate} · Process: ${report.processDate ?: "N/A"}", 312f, currentY + 76f, paint)

        currentY += cardHeight + 12f

        // 3. Variance & Audit Summary Container
        val summaryHeight = 62f
        val summaryRect = RectF(36f, currentY, 559f, currentY + summaryHeight)
        paint.color = if (report.status.isProblem) Color.rgb(255, 241, 242) else Color.rgb(240, 253, 244)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(summaryRect, 6f, 6f, paint)

        paint.color = if (report.status.isProblem) Color.rgb(254, 205, 211) else Color.rgb(187, 247, 208)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(summaryRect, 6f, 6f, paint)

        // Summary Metric Columns
        paint.style = Paint.Style.FILL
        val colWidth = (559f - 36f) / 4f

        // Col 1: Timesheet Hours
        paint.color = colorSecondary
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("QUALIFYING WORKED", 48f, currentY + 18f, paint)
        paint.color = colorPrimary
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("${"%.1f".format(report.timesheetTotalHours)} hrs", 48f, currentY + 36f, paint)
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        paint.color = colorSecondary
        canvas.drawText("${report.timesheetShiftsCount} shifts in cycle", 48f, currentY + 49f, paint)

        // Col 2: Paid Basic Hours
        val col2X = 36f + colWidth
        paint.color = colorSecondary
        paint.textSize = 8f
        canvas.drawText("PAYSLIP PAID BASIC", col2X, currentY + 18f, paint)
        paint.color = colorPrimary
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("${"%.2f".format(report.payslipPaidBasicHours)} hrs", col2X, currentY + 36f, paint)
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        paint.color = colorSecondary
        canvas.drawText("@ £${"%.2f".format(report.payslipPaidRate)}/hr", col2X, currentY + 49f, paint)

        // Col 3: Missing Hours
        val col3X = 36f + colWidth * 2f
        paint.color = colorSecondary
        paint.textSize = 8f
        canvas.drawText("MISSING VARIANCE", col3X, currentY + 18f, paint)
        paint.color = if (report.status.isProblem) colorWarning else colorAccent
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("${"%.2f".format(report.missingHours)} hrs", col3X, currentY + 36f, paint)
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        paint.color = colorSecondary
        canvas.drawText(if (report.missingShifts > 0) "${report.missingShifts} shift(s) shortfall" else "Hours variance", col3X, currentY + 49f, paint)

        // Col 4: Gross & Net Shortfall
        val col4X = 36f + colWidth * 3f
        paint.color = colorSecondary
        paint.textSize = 8f
        canvas.drawText("DEFICIT CLAIM", col4X, currentY + 18f, paint)
        paint.color = if (report.status.isProblem) colorWarning else colorAccent
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("£${"%.2f".format(report.grossShortfall)} Gross", col4X, currentY + 36f, paint)
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        paint.color = colorSecondary
        canvas.drawText("~£${"%.2f".format(report.netShortfall)} Net (72%)", col4X, currentY + 49f, paint)

        currentY += summaryHeight + 14f

        // 4. Itemized Shift Attendance Table
        paint.color = colorPrimary
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("IN-CYCLE VERIFIED SHIFTS WORKED (${report.payCycleStartDate.uppercase()} – ${report.payCycleCutoffDate.uppercase()})", 36f, currentY, paint)
        currentY += 8f

        // Table Header
        val tableHeaderY = currentY
        paint.color = Color.rgb(241, 245, 249)
        paint.style = Paint.Style.FILL
        canvas.drawRect(36f, tableHeaderY, 559f, tableHeaderY + 20f, paint)

        paint.color = colorSecondary
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("#", 44f, tableHeaderY + 14f, paint)
        canvas.drawText("WORKED DATE", 68f, tableHeaderY + 14f, paint)
        canvas.drawText("SHIFT TYPE / DESCRIPTION", 220f, tableHeaderY + 14f, paint)
        canvas.drawText("HOURS", 420f, tableHeaderY + 14f, paint)
        canvas.drawText("CYCLE STATUS", 490f, tableHeaderY + 14f, paint)

        currentY = tableHeaderY + 20f

        // Table Rows
        paint.isFakeBoldText = false
        paint.textSize = 8.5f
        val rowHeight = 17f

        val maxShiftsToDraw = minOf(report.workedDays.size, 18)
        for (i in 0 until maxShiftsToDraw) {
            val day = report.workedDays[i]
            val rowY = currentY + (i * rowHeight)

            if (i % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, rowY, 559f, rowY + rowHeight, paint)
            }

            paint.color = colorPrimary
            paint.style = Paint.Style.FILL
            canvas.drawText("${i + 1}", 44f, rowY + 12f, paint)
            canvas.drawText(day.dateFormatted, 68f, rowY + 12f, paint)
            canvas.drawText(day.shiftTypeDescription, 220f, rowY + 12f, paint)
            canvas.drawText("${"%.1f".format(day.hours)} hrs", 420f, rowY + 12f, paint)

            paint.color = colorAccent
            paint.isFakeBoldText = true
            canvas.drawText("In-Cycle", 490f, rowY + 12f, paint)
            paint.isFakeBoldText = false
        }

        currentY += (maxShiftsToDraw * rowHeight) + 4f

        // Table Footer Divider & Summary Line
        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(36f, currentY, 559f, currentY, paint)

        currentY += 12f
        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("Total In-Cycle Hours Logged: ${"%.2f".format(report.timesheetTotalHours)} hrs (${report.timesheetShiftsCount} shifts)", 36f, currentY, paint)

        currentY += 16f

        // 5. Verification & Sign-Off Container
        val signBoxHeight = 82f
        val signBoxRect = RectF(36f, currentY, 559f, currentY + signBoxHeight)
        paint.color = colorBgLight
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(signBoxRect, 6f, 6f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(signBoxRect, 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("FORMAL CERTIFICATION & SIGN-OFF", 48f, currentY + 16f, paint)

        paint.textSize = 8f
        paint.isFakeBoldText = false
        paint.color = colorSecondary
        canvas.drawText("I confirm that the above worked shift schedule accurately reflects verified ward / unit attendance.", 48f, currentY + 29f, paint)

        // Employee Signature Line
        paint.color = colorSecondary
        canvas.drawText("Employee Signature: _______________________", 48f, currentY + 54f, paint)
        canvas.drawText("Date: ___/___/2026", 48f, currentY + 70f, paint)

        // Manager Signature Line
        canvas.drawText("Line / Ward Manager: _______________________", 300f, currentY + 54f, paint)
        canvas.drawText("Print Name / Date: ________________________", 300f, currentY + 70f, paint)

        // 6. Footer
        paint.color = colorSecondary
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        val timeStamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date())
        canvas.drawText("Generated via Salary Calculator App · Audit Ref: ${report.employeeRef.ifBlank { "PAYROLL" }}-${report.payPeriod.replace(" ", "")} · $timeStamp", 36f, 820f, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "timesheets").apply { mkdirs() }
        val safeName = report.employeeName.replace(" ", "_").replace("'", "")
        val safePeriod = report.payPeriod.replace(" ", "_")
        val file = File(outputDir, "Timesheet_Audit_${safeName}_${safePeriod}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Shares the generated Timesheet verification PDF via Android Intent.
     */
    fun shareTimesheetPdf(context: Context, report: PayslipAuditReport) {
        val file = generateTimesheetPdf(context, report)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Timesheet Verification Statement - ${report.employeeName} (${report.payPeriod})")
            putExtra(Intent.EXTRA_TEXT, "Please find attached the official timesheet attendance & discrepancy statement for ${report.employeeName} (${report.payPeriod}).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Timesheet Statement PDF via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
