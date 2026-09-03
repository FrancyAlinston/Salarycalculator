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

data class Sa100BoxEntry(
    val boxNumber: String,
    val description: String,
    val amount: Double,
    val isDeduction: Boolean = false
)

data class Sa100Summary(
    val taxYearLabel: String,
    val employerName: String,
    val employeeTaxCode: String,
    val totalGrossPay: Double,
    val totalTaxDeducted: Double,
    val totalVariablePay: Double,
    val totalPensionRelief: Double,
    val totalStudentLoan: Double,
    val totalNiPaid: Double,
    val netTakeHome: Double,
    val boxes: List<Sa100BoxEntry>
)

object Sa100Generator {

    fun generateFromRecords(
        records: List<MonthlySalaryRecord>,
        taxYearLabel: String = "2024/2025",
        employerName: String = "Primary Employment"
    ): Sa100Summary {
        val totalGross = records.sumOf { it.grossPay }
        val totalTax = records.sumOf { it.incomeTax }
        val totalPension = records.sumOf { it.pensionContribution }
        val totalStudentLoan = records.sumOf { it.studentLoanDeduction }
        val totalNi = records.sumOf { it.nationalInsurance }
        val totalNet = records.sumOf { it.netPay }
        val firstTaxCode = records.firstOrNull()?.taxCode ?: "1257L"

        val boxes = listOf(
            Sa100BoxEntry("Box 1", "Pay from this employment (gross pay)", totalGross),
            Sa100BoxEntry("Box 2", "UK tax taken off pay (PAYE income tax)", totalTax, isDeduction = true),
            Sa100BoxEntry("Box 3", "Tips and other payments (discretionary earnings)", 0.0),
            Sa100BoxEntry("Box 4", "PAYE tax on bonuses & taxable allowances", 0.0),
            Sa100BoxEntry("Box 5", "Employee pension contributions paid under net pay", totalPension, isDeduction = true),
            Sa100BoxEntry("Box 6", "Student loan deductions taken off by employer", totalStudentLoan, isDeduction = true),
            Sa100BoxEntry("Box 7", "Class 1 Primary National Insurance deducted", totalNi, isDeduction = true)
        )

        return Sa100Summary(
            taxYearLabel = taxYearLabel,
            employerName = employerName,
            employeeTaxCode = firstTaxCode,
            totalGrossPay = totalGross,
            totalTaxDeducted = totalTax,
            totalVariablePay = 0.0,
            totalPensionRelief = totalPension,
            totalStudentLoan = totalStudentLoan,
            totalNiPaid = totalNi,
            netTakeHome = totalNet,
            boxes = boxes
        )
    }

    fun generateFromLive(
        report: SalaryReport,
        taxCode: String = "1257L",
        employerName: String = "Primary Employment",
        taxYearLabel: String = "2024/2025"
    ): Sa100Summary {
        val grossAnnual = report.annualGross
        val taxAnnual = report.incomeTax * 12.0
        val varAnnual = (report.bonusPay + report.commissionPay) * 12.0
        val pensionAnnual = report.pensionContribution * 12.0
        val slAnnual = report.studentLoanDeduction * 12.0
        val niAnnual = report.nationalInsurance * 12.0
        val netAnnual = report.annualNet

        val boxes = listOf(
            Sa100BoxEntry("Box 1", "Pay from this employment (annualized gross)", grossAnnual),
            Sa100BoxEntry("Box 2", "UK tax taken off pay (annualized PAYE tax)", taxAnnual, isDeduction = true),
            Sa100BoxEntry("Box 3", "Tips, bonus and commission payments", varAnnual),
            Sa100BoxEntry("Box 4", "PAYE tax on benefits & discretionary earnings", 0.0),
            Sa100BoxEntry("Box 5", "Employee pension contributions (Net Pay Arrangement)", pensionAnnual, isDeduction = true),
            Sa100BoxEntry("Box 6", "Student loan repayments deducted by employer", slAnnual, isDeduction = true),
            Sa100BoxEntry("Box 7", "Class 1 Primary National Insurance deducted", niAnnual, isDeduction = true)
        )

        return Sa100Summary(
            taxYearLabel = taxYearLabel,
            employerName = employerName,
            employeeTaxCode = taxCode,
            totalGrossPay = grossAnnual,
            totalTaxDeducted = taxAnnual,
            totalVariablePay = varAnnual,
            totalPensionRelief = pensionAnnual,
            totalStudentLoan = slAnnual,
            totalNiPaid = niAnnual,
            netTakeHome = netAnnual,
            boxes = boxes
        )
    }

    /**
     * Generates an official vector PDF summary of HMRC SA100 return employment boxes.
     * Page dimensions: A4 (595 x 842 points).
     */
    fun generatePdf(context: Context, summary: Sa100Summary): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colorPrimary = Color.rgb(27, 38, 59)      // Deep Slate
        val colorSecondary = Color.rgb(65, 90, 119)  // Muted Slate
        val colorAccent = Color.rgb(16, 185, 129)     // Emerald
        val colorDeduction = Color.rgb(225, 29, 72)  // Rose
        val colorBgLight = Color.rgb(248, 250, 252)   // Soft Slate Light
        val colorBorder = Color.rgb(226, 232, 240)    // Divider

        // Header Banner
        paint.color = colorPrimary
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("HMRC SELF-ASSESSMENT (SA100 / SA102)", 36f, 40f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("EMPLOYMENT SUMMARY & TAX RETURN HELPER", 36f, 60f, paint)

        // Tax Year Badge
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = colorAccent
        val yearBadge = "TAX YEAR ${summary.taxYearLabel}"
        val yearWidth = paint.measureText(yearBadge)
        canvas.drawText(yearBadge, 595f - 36f - yearWidth, 48f, paint)

        // Employer & Tax Code Meta Card
        var currentY = 110f
        paint.color = colorBgLight
        val metaRect = RectF(36f, currentY, 595f - 36f, currentY + 60f)
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)

        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = colorSecondary
        paint.textSize = 10f
        canvas.drawText("EMPLOYER / SOURCE", 50f, currentY + 22f, paint)
        canvas.drawText("TAX CODE", 300f, currentY + 22f, paint)
        canvas.drawText("TOTAL TAKE-HOME", 430f, currentY + 22f, paint)

        paint.color = colorPrimary
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText(summary.employerName, 50f, currentY + 44f, paint)
        canvas.drawText(summary.employeeTaxCode, 300f, currentY + 44f, paint)

        paint.color = colorAccent
        canvas.drawText("£${"%,.2f".format(summary.netTakeHome)}", 430f, currentY + 44f, paint)

        currentY += 80f

        // Table Header
        paint.color = colorPrimary
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("HMRC Self Assessment Return Boxes", 36f, currentY, paint)

        currentY += 16f
        paint.color = colorSecondary
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("BOX", 36f, currentY, paint)
        canvas.drawText("DESCRIPTION", 100f, currentY, paint)
        canvas.drawText("AMOUNT (£)", 500f, currentY, paint)

        currentY += 6f
        paint.color = colorBorder
        paint.strokeWidth = 1f
        canvas.drawLine(36f, currentY, 595f - 36f, currentY, paint)

        currentY += 18f

        for (box in summary.boxes) {
            paint.color = colorPrimary
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText(box.boxNumber, 36f, currentY, paint)

            paint.isFakeBoldText = false
            paint.textSize = 10f
            paint.color = colorSecondary
            canvas.drawText(box.description, 100f, currentY, paint)

            val amtText = "£${"%,.2f".format(box.amount)}"
            val amtWidth = paint.measureText(amtText)
            paint.color = if (box.isDeduction && box.amount > 0) colorDeduction else colorPrimary
            paint.isFakeBoldText = true
            canvas.drawText(amtText, 595f - 36f - amtWidth, currentY, paint)

            currentY += 24f
            paint.color = colorBorder
            paint.strokeWidth = 0.5f
            canvas.drawLine(36f, currentY - 6f, 595f - 36f, currentY - 6f, paint)
        }

        // Footer
        currentY = 800f
        paint.color = colorSecondary
        paint.textSize = 9f
        paint.isFakeBoldText = false
        val timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated by Salary Calculator on $timestamp · For HMRC Self-Assessment filing assistance only.", 36f, currentY, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "sa100_exports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "HMRC_SA100_Summary.pdf")
        val outputStream = FileOutputStream(outputFile)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return outputFile
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "HMRC SA100 Self-Assessment Summary")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share HMRC SA100 Summary")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
