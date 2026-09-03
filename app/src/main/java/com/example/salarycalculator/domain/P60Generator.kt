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

object P60Generator {

    /**
     * Generates an official HMRC-styled A4 vector PDF End-of-Year Certificate (P60).
     */
    fun generateP60Pdf(
        context: Context,
        taxYearLabel: String,
        records: List<MonthlySalaryRecord>,
        employeeName: String = "Valued Employee",
        employerName: String = "Primary Employment",
        payeRef: String = "120/AB54321"
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Palette definition
        val colorPrimary = Color.rgb(15, 23, 42)      // Slate 900
        val colorSecondary = Color.rgb(71, 85, 105)   // Slate 600
        val colorGold = Color.rgb(217, 119, 6)        // Gold / Amber
        val colorAccent = Color.rgb(16, 185, 129)     // Emerald
        val colorDeduction = Color.rgb(225, 29, 72)  // Rose
        val colorBgLight = Color.rgb(248, 250, 252)   // Soft Slate Light
        val colorBorder = Color.rgb(203, 213, 225)    // Slate 300

        // Aggregated totals across all records
        val totalGross = records.sumOf { it.grossPay }
        val totalTaxable = records.sumOf { it.taxablePay }
        val totalTax = records.sumOf { it.incomeTax }
        val totalNI = records.sumOf { it.nationalInsurance }
        val totalPension = records.sumOf { it.pensionContribution }
        val totalStudentLoan = records.sumOf { it.studentLoanDeduction }
        val totalNet = records.sumOf { it.netPay }
        val finalTaxCode = records.firstOrNull()?.taxCode ?: "1257L"

        // 1. Top Header Banner
        paint.color = colorPrimary
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 95f, paint)

        // Gold Accent Bar
        paint.color = colorGold
        canvas.drawRect(0f, 95f, 595f, 98f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("P60 END OF YEAR CERTIFICATE", 36f, 44f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("HM Revenue & Customs · Tax Year to 5 April $taxYearLabel", 36f, 66f, paint)

        // Official Form Tag
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = colorGold
        canvas.drawText("FORM P60", 595f - 36f - paint.measureText("FORM P60"), 48f, paint)

        // 2. Identification Details Box
        var currentY = 115f
        paint.color = colorBgLight
        paint.style = Paint.Style.FILL
        val idRect = RectF(36f, currentY, 559f, currentY + 70f)
        canvas.drawRoundRect(idRect, 8f, 8f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(idRect, 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas.drawText("EMPLOYEE & EMPLOYER DETAILS", 48f, currentY + 20f, paint)

        paint.color = colorSecondary
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        canvas.drawText("Employee Name: $employeeName", 48f, currentY + 38f, paint)
        canvas.drawText("Final Tax Code: $finalTaxCode", 48f, currentY + 54f, paint)

        canvas.drawText("Employer: $employerName", 310f, currentY + 38f, paint)
        canvas.drawText("PAYE Reference: $payeRef", 310f, currentY + 54f, paint)

        // 3. Section A: Pay and Income Tax Details
        currentY += 85f
        paint.color = colorPrimary
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("1. PAY AND INCOME TAX DETAILS", 36f, currentY, paint)

        currentY += 10f
        drawTableRow(canvas, paint, 36f, 559f, currentY, "Statutory Category", "Details", "Amount", isHeader = true)
        currentY += 22f

        drawTableRow(canvas, paint, 36f, 559f, currentY, "Total Pay in this Employment", "Total Gross Earnings", "£${"%,.2f".format(totalGross)}", isBold = true)
        currentY += 20f

        drawTableRow(canvas, paint, 36f, 559f, currentY, "Total Taxable Pay", "After Pension Relief & Allowances", "£${"%,.2f".format(totalTaxable)}")
        currentY += 20f

        drawTableRow(canvas, paint, 36f, 559f, currentY, "Total PAYE Income Tax Deducted", "HMRC PAYE Rates", "£${"%,.2f".format(totalTax)}", isBold = true, valueColor = colorDeduction)
        currentY += 20f

        paint.color = colorBorder
        canvas.drawLine(36f, currentY, 559f, currentY, paint)
        currentY += 25f

        // 4. Section B: National Insurance Contributions
        paint.color = colorPrimary
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("2. NATIONAL INSURANCE CONTRIBUTIONS", 36f, currentY, paint)

        currentY += 10f
        drawTableRow(canvas, paint, 36f, 559f, currentY, "NIC Category", "Rate Band", "Amount", isHeader = true)
        currentY += 22f

        drawTableRow(canvas, paint, 36f, 559f, currentY, "Class 1 Employee NIC Paid", "Primary Threshold 8% & UEL 2%", "£${"%,.2f".format(totalNI)}", isBold = true, valueColor = colorDeduction)
        currentY += 20f

        paint.color = colorBorder
        canvas.drawLine(36f, currentY, 559f, currentY, paint)
        currentY += 25f

        // 5. Section C: Other Deductions (Student Loan & Pension)
        paint.color = colorPrimary
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("3. OTHER STATUTORY & SCHEME DEDUCTIONS", 36f, currentY, paint)

        currentY += 10f
        drawTableRow(canvas, paint, 36f, 559f, currentY, "Deduction Type", "Relief Status", "Amount", isHeader = true)
        currentY += 22f

        if (totalStudentLoan > 0) {
            drawTableRow(canvas, paint, 36f, 559f, currentY, "Student Loan Repayments Deducted", "Direct to SLC", "£${"%,.2f".format(totalStudentLoan)}", valueColor = colorDeduction)
            currentY += 20f
        }

        if (totalPension > 0) {
            drawTableRow(canvas, paint, 36f, 559f, currentY, "Workplace Pension Contributions", "Net Pay Arrangement", "£${"%,.2f".format(totalPension)}", valueColor = colorDeduction)
            currentY += 20f
        }

        paint.color = colorBorder
        canvas.drawLine(36f, currentY, 559f, currentY, paint)
        currentY += 30f

        // 6. Annual Summary Hero Box
        val netRect = RectF(36f, currentY, 559f, currentY + 80f)
        paint.color = Color.rgb(236, 253, 245)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(netRect, 10f, 10f, paint)

        paint.color = colorAccent
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(netRect, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorPrimary
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL ANNUAL NET TAKE-HOME PAY", 56f, currentY + 34f, paint)

        paint.color = colorSecondary
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val totalDeducted = totalTax + totalNI + totalStudentLoan + totalPension
        canvas.drawText("Total Deductions (Tax + NI + Loan + Pension): £${"%,.2f".format(totalDeducted)}", 56f, currentY + 54f, paint)

        paint.color = colorAccent
        paint.textSize = 24f
        paint.isFakeBoldText = true
        val netText = "£${"%,.2f".format(totalNet)}"
        val netWidth = paint.measureText(netText)
        canvas.drawText(netText, 559f - 20f - netWidth, currentY + 46f, paint)

        // 7. Footer Declaration
        paint.color = colorBorder
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 785f, 559f, 785f, paint)

        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.UK)
        canvas.drawText("This certificate should be kept in a safe place. You will need it if you make a claim for tax credits or refund.", 36f, 802f, paint)
        canvas.drawText("Generated on ${dateFormat.format(Date())} · Verified Salary Calculator Record", 36f, 814f, paint)

        pdfDocument.finishPage(page)

        val cleanYear = taxYearLabel.replace(" ", "_").replace("/", "-")
        val exportFile = File(context.cacheDir, "P60_Certificate_${cleanYear}.pdf")
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

    fun shareP60(context: Context, file: File) {
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
        context.startActivity(Intent.createChooser(sendIntent, "Share Official P60 Certificate"))
    }
}
