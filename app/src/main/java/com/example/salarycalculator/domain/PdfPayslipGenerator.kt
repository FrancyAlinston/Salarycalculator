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

object PdfPayslipGenerator {

    /**
     * Generates an official vector PDF payslip document.
     * Page dimensions: A4 (595 x 842 points).
     */
    fun generatePayslipPdf(context: Context, record: MonthlySalaryRecord): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Palette definition
        val colorPrimary = Color.rgb(27, 38, 59)      // Deep Slate
        val colorSecondary = Color.rgb(65, 90, 119)  // Muted Slate
        val colorAccent = Color.rgb(16, 185, 129)     // Emerald
        val colorDeduction = Color.rgb(225, 29, 72)  // Rose
        val colorBgLight = Color.rgb(248, 250, 252)   // Soft Slate Light
        val colorBorder = Color.rgb(226, 232, 240)    // Divider

        // 1. Header Banner
        paint.color = colorPrimary
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // App & Document Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("SALARY STATEMENT / PAYSLIP", 36f, 42f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("UK PAYE & NATIONAL INSURANCE CALCULATOR", 36f, 62f, paint)

        // Month / Year Badge on Right
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = colorAccent
        val monthText = record.monthYear.uppercase(Locale.getDefault())
        val monthWidth = paint.measureText(monthText)
        canvas.drawText(monthText, 595f - 36f - monthWidth, 48f, paint)

        // 2. Metadata Cards (Side by Side)
        var currentY = 110f

        // Left Card: Schedule & Hours
        paint.color = colorBgLight
        paint.style = Paint.Style.FILL
        val leftCardRect = RectF(36f, currentY, 285f, currentY + 95f)
        canvas.drawRoundRect(leftCardRect, 8f, 8f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(leftCardRect, 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("EMPLOYMENT & SCHEDULE", 48f, currentY + 22f, paint)

        paint.color = colorSecondary
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Standard Hourly Rate: £${"%.2f".format(record.hourlyRate)} / hr", 48f, currentY + 40f, paint)
        canvas.drawText("Schedule: ${record.daysWorked} days × ${record.hoursPerDay} hrs", 48f, currentY + 56f, paint)
        val totalHours = (record.daysWorked * record.hoursPerDay) + record.overtimeHours
        canvas.drawText("Total Hours Logged: ${"%.1f".format(totalHours)} hrs", 48f, currentY + 72f, paint)
        if (record.overtimeHours > 0) {
            canvas.drawText("Overtime: ${record.overtimeHours} hrs @ ${record.overtimeMultiplier}x", 48f, currentY + 86f, paint)
        }

        // Right Card: Tax & Pension Settings
        val rightCardRect = RectF(305f, currentY, 559f, currentY + 95f)
        paint.color = colorBgLight
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rightCardRect, 8f, 8f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rightCardRect, 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("TAX & PENSION PROFILE", 317f, currentY + 22f, paint)

        val taxMonthName = when {
            record.monthYear.contains("Apr", ignoreCase = true) -> "M1 (Apr)"
            record.monthYear.contains("May", ignoreCase = true) -> "M2 (May)"
            record.monthYear.contains("Jun", ignoreCase = true) -> "M3 (Jun)"
            record.monthYear.contains("Jul", ignoreCase = true) -> "M4 (Jul)"
            record.monthYear.contains("Aug", ignoreCase = true) -> "M5 (Aug)"
            record.monthYear.contains("Sep", ignoreCase = true) -> "M6 (Sep)"
            record.monthYear.contains("Oct", ignoreCase = true) -> "M7 (Oct)"
            record.monthYear.contains("Nov", ignoreCase = true) -> "M8 (Nov)"
            record.monthYear.contains("Dec", ignoreCase = true) -> "M9 (Dec)"
            record.monthYear.contains("Jan", ignoreCase = true) -> "M10 (Jan)"
            record.monthYear.contains("Feb", ignoreCase = true) -> "M11 (Feb)"
            record.monthYear.contains("Mar", ignoreCase = true) -> "M12 (Mar)"
            else -> "M5 (Aug)"
        }
        val taxMonthMultiplier = when {
            record.monthYear.contains("Apr", ignoreCase = true) -> 1
            record.monthYear.contains("May", ignoreCase = true) -> 2
            record.monthYear.contains("Jun", ignoreCase = true) -> 3
            record.monthYear.contains("Jul", ignoreCase = true) -> 4
            record.monthYear.contains("Aug", ignoreCase = true) -> 5
            record.monthYear.contains("Sep", ignoreCase = true) -> 6
            record.monthYear.contains("Oct", ignoreCase = true) -> 7
            record.monthYear.contains("Nov", ignoreCase = true) -> 8
            record.monthYear.contains("Dec", ignoreCase = true) -> 9
            record.monthYear.contains("Jan", ignoreCase = true) -> 10
            record.monthYear.contains("Feb", ignoreCase = true) -> 11
            record.monthYear.contains("Mar", ignoreCase = true) -> 12
            else -> 5
        }

        paint.color = colorSecondary
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Tax Code: ${record.taxCode} · Period: $taxMonthName", 317f, currentY + 40f, paint)
        canvas.drawText("Tax Region: ${if (record.taxRegion == TaxRegion.SCOTLAND) "Scotland (6 Rates)" else "UK Standard"}", 317f, currentY + 56f, paint)
        canvas.drawText("Employee Pension: ${"%.1f".format(record.pensionRate)}%", 317f, currentY + 72f, paint)
        canvas.drawText("Student Loan: ${if (record.studentLoanPlan == StudentLoanPlan.NONE) "None" else record.studentLoanPlan.name.replace("_", " ")}", 317f, currentY + 86f, paint)

        // 3. Earnings & Deductions Tables
        currentY += 120f

        // Section Title: Earnings Breakdown
        paint.color = colorPrimary
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("PAYMENTS & EARNINGS", 36f, currentY, paint)

        currentY += 10f
        drawTableRow(canvas, paint, 36f, 559f, currentY, "Item Description", "Hours / Rate", "Amount", isHeader = true)
        currentY += 22f

        val standardPay = (record.daysWorked * record.hoursPerDay) * record.hourlyRate
        drawTableRow(canvas, paint, 36f, 559f, currentY, "Basic Hourly Wages", "${"%.1f".format(record.daysWorked * record.hoursPerDay)} hrs @ £${"%.2f".format(record.hourlyRate)}", "£${"%.2f".format(standardPay)}")
        currentY += 20f

        if (record.overtimeHours > 0) {
            val overtimePay = record.overtimeHours * (record.hourlyRate * record.overtimeMultiplier)
            drawTableRow(canvas, paint, 36f, 559f, currentY, "Overtime Pay", "${record.overtimeHours} hrs @ ${record.overtimeMultiplier}x", "£${"%.2f".format(overtimePay)}")
            currentY += 20f
        }

        paint.color = colorBorder
        paint.strokeWidth = 1f
        canvas.drawLine(36f, currentY, 559f, currentY, paint)
        currentY += 16f

        drawTableRow(canvas, paint, 36f, 559f, currentY, "GROSS TOTAL PAY", "", "£${"%.2f".format(record.grossPay)}", isBold = true)
        currentY += 30f

        // Section Title: Deductions Breakdown
        paint.color = colorPrimary
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("DEDUCTIONS & TAXES", 36f, currentY, paint)

        currentY += 10f
        drawTableRow(canvas, paint, 36f, 559f, currentY, "Deduction Description", "Statutory Rate", "Amount", isHeader = true)
        currentY += 22f

        if (record.salarySacrifice > 0) {
            drawTableRow(canvas, paint, 36f, 559f, currentY, "Salary Sacrifice Schemes", "Pre-Tax", "-£${"%.2f".format(record.salarySacrifice)}", valueColor = colorDeduction)
            currentY += 20f
        }

        if (record.pensionContribution > 0) {
            drawTableRow(canvas, paint, 36f, 559f, currentY, "Workplace Pension Contribution", "${"%.1f".format(record.pensionRate)}% Relief", "-£${"%.2f".format(record.pensionContribution)}", valueColor = colorDeduction)
            currentY += 20f
        }

        drawTableRow(canvas, paint, 36f, 559f, currentY, "PAYE Income Tax (${if (record.taxRegion == TaxRegion.SCOTLAND) "Scotland" else "UK Standard"})", "2024/2025 Bands", "-£${"%.2f".format(record.incomeTax)}", valueColor = colorDeduction)
        currentY += 20f

        drawTableRow(canvas, paint, 36f, 559f, currentY, "Class 1 National Insurance", "Primary 8% / 2%", "-£${"%.2f".format(record.nationalInsurance)}", valueColor = colorDeduction)
        currentY += 20f

        if (record.studentLoanDeduction > 0) {
            drawTableRow(canvas, paint, 36f, 559f, currentY, "Student Loan Repayment", record.studentLoanPlan.displayName, "-£${"%.2f".format(record.studentLoanDeduction)}", valueColor = colorDeduction)
            currentY += 20f
        }

        paint.color = colorBorder
        paint.strokeWidth = 1f
        canvas.drawLine(36f, currentY, 559f, currentY, paint)
        currentY += 16f

        drawTableRow(canvas, paint, 36f, 559f, currentY, "TOTAL DEDUCTIONS", "", "-£${"%.2f".format(record.totalDeductions)}", isBold = true, valueColor = colorDeduction)
        currentY += 36f

        // 4. Net Take-Home Hero Summary Box
        val netCardRect = RectF(36f, currentY, 559f, currentY + 75f)
        paint.color = Color.rgb(236, 253, 245) // Emerald Light Container
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(netCardRect, 10f, 10f, paint)

        paint.color = colorAccent
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(netCardRect, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("NET TAKE-HOME PAY", 56f, currentY + 32f, paint)

        paint.color = colorSecondary
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val takeHomePct = if (record.grossPay > 0) (record.netPay / record.grossPay) * 100.0 else 100.0
        canvas.drawText("Estimated Take-Home: ${"%.1f".format(takeHomePct)}% of Gross Pay", 56f, currentY + 50f, paint)

        paint.color = colorAccent
        paint.textSize = 24f
        paint.isFakeBoldText = true
        val netText = "£${"%,.2f".format(record.netPay)}"
        val netTextWidth = paint.measureText(netText)
        canvas.drawText(netText, 559f - 20f - netTextWidth, currentY + 45f, paint)

        currentY += 90f

        // 5. Year-to-Date (YTD) Estimates Card
        val ytdCardRect = RectF(36f, currentY, 559f, currentY + 48f)
        paint.color = Color.rgb(241, 245, 249)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(ytdCardRect, 6f, 6f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(ytdCardRect, 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("YEAR-TO-DATE (YTD) ESTIMATES · $taxMonthName", 48f, currentY + 16f, paint)

        paint.color = colorSecondary
        paint.textSize = 9f
        paint.isFakeBoldText = false
        val ytdGross = record.grossPay * taxMonthMultiplier
        val ytdTax = record.incomeTax * taxMonthMultiplier
        val ytdNi = record.nationalInsurance * taxMonthMultiplier
        val ytdNet = record.netPay * taxMonthMultiplier

        canvas.drawText("Gross: £${"%,.2f".format(ytdGross)}", 48f, currentY + 34f, paint)
        canvas.drawText("Tax: £${"%,.2f".format(ytdTax)}", 180f, currentY + 34f, paint)
        canvas.drawText("NI: £${"%,.2f".format(ytdNi)}", 300f, currentY + 34f, paint)
        canvas.drawText("Net: £${"%,.2f".format(ytdNet)}", 430f, currentY + 34f, paint)

        currentY += 60f

        // 6. Employer Contribution & Custom Note
        if (record.employerPension > 0) {
            paint.color = colorSecondary
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("• Employer Pension Contribution (3% statutory auto-enrolment): +£${"%.2f".format(record.employerPension)}", 36f, currentY, paint)
            currentY += 16f
        }

        if (record.note.isNotBlank()) {
            paint.color = colorPrimary
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("Notes / Remarks: ${record.note}", 36f, currentY, paint)
            currentY += 20f
        }

        // 6. Footer & Timestamp
        paint.color = colorBorder
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 790f, 559f, 790f, paint)

        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.UK)
        canvas.drawText("Generated on ${dateFormat.format(Date(record.timestamp))} by UK Salary Calculator", 36f, 808f, paint)
        canvas.drawText("Page 1 of 1 · Confidential Payroll Record", 400f, 808f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache directory
        val cleanMonth = record.monthYear.replace(" ", "_").replace("/", "-")
        val exportFile = File(context.cacheDir, "Payslip_${cleanMonth}.pdf")
        FileOutputStream(exportFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()

        return exportFile
    }

    private fun drawTableRow(
        canvas: Canvas,
        paint: Paint,
        leftX: Float,
        rightX: Float,
        y: Float,
        col1: String,
        col2: String,
        col3: String,
        isHeader: Boolean = false,
        isBold: Boolean = false,
        valueColor: Int = Color.BLACK
    ) {
        if (isHeader) {
            paint.color = Color.rgb(241, 245, 249)
            paint.style = Paint.Style.FILL
            canvas.drawRect(leftX, y - 12f, rightX, y + 6f, paint)

            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            canvas.drawText(col1.uppercase(Locale.getDefault()), leftX + 8f, y, paint)
            canvas.drawText(col2.uppercase(Locale.getDefault()), leftX + 220f, y, paint)
            val w = paint.measureText(col3.uppercase(Locale.getDefault()))
            canvas.drawText(col3.uppercase(Locale.getDefault()), rightX - 8f - w, y, paint)
        } else {
            paint.color = if (isBold) Color.rgb(15, 23, 42) else Color.rgb(51, 65, 85)
            paint.textSize = if (isBold) 11f else 10f
            paint.isFakeBoldText = isBold
            canvas.drawText(col1, leftX + 8f, y, paint)

            if (col2.isNotBlank()) {
                paint.color = Color.rgb(100, 116, 139)
                paint.isFakeBoldText = false
                canvas.drawText(col2, leftX + 220f, y, paint)
            }

            paint.color = valueColor
            paint.isFakeBoldText = isBold
            val w = paint.measureText(col3)
            canvas.drawText(col3, rightX - 8f - w, y, paint)
        }
    }

    /**
     * Helper to launch Android Sharesheet with the generated PDF.
     */
    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Payslip PDF"))
    }
}
