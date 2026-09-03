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
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AnnualShiftPdfGenerator {

    /**
     * Generates a printable A4 vector PDF showing a complete 12-month calendar shift overview with payroll metrics.
     */
    fun generateAnnualShiftPdf(
        context: Context,
        year: Int,
        annualShifts: Map<Int, Map<Int, Double>>,
        hourlyRate: Double = 15.0,
        jobTitle: String = "Primary Employment"
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595x842 pt)
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val monthNames = DateFormatSymbols().months

        // Palette
        val colorPrimary = Color.rgb(20, 90, 160)
        val colorDark = Color.rgb(30, 30, 30)
        val colorMuted = Color.rgb(120, 120, 120)
        val colorBg = Color.rgb(248, 250, 252)
        val colorRegular = Color.rgb(16, 149, 106) // Emerald
        val colorOt10 = Color.rgb(217, 119, 6)    // Amber
        val colorOt12 = Color.rgb(225, 29, 72)    // Rose
        val colorOff = Color.rgb(235, 240, 245)

        // Draw background
        paint.color = colorBg
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header Card
        paint.color = Color.WHITE
        val headerRect = RectF(20f, 20f, 575f, 95f)
        canvas.drawRoundRect(headerRect, 10f, 10f, paint)

        paint.color = colorPrimary
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Salary Calculator — Annual Shift Schedule $year", 35f, 48f, paint)

        // Annual Aggregates
        val totalDays = annualShifts.values.sumOf { m -> m.values.count { it > 0 } }
        val totalHours = annualShifts.values.sumOf { m -> m.values.sum() }
        val totalOt = annualShifts.values.sumOf { m -> m.values.sumOf { maxOf(0.0, it - 8.0) } }
        val totalGross = totalHours * hourlyRate

        paint.color = colorDark
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        val subtitle = "Role: $jobTitle  |  Total Worked: $totalDays days  |  Total Hours: ${"%.0f".format(totalHours)}h (OT: ${"%.0f".format(totalOt)}h)  |  Est. Annual Gross: £${"%,.2f".format(totalGross)}"
        canvas.drawText(subtitle, 35f, 70f, paint)

        paint.color = colorMuted
        paint.textSize = 8f
        val dateGenerated = "Generated on: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.UK).format(Date())}"
        canvas.drawText(dateGenerated, 35f, 85f, paint)

        // Draw 12 Months: 3 columns x 4 rows
        val startX = 20f
        val startY = 110f
        val cardWidth = 175f
        val cardHeight = 165f
        val gapX = 12f
        val gapY = 15f

        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

        for (m in 1..12) {
            val col = (m - 1) % 3
            val row = (m - 1) / 3
            val left = startX + col * (cardWidth + gapX)
            val top = startY + row * (cardHeight + gapY)
            val right = left + cardWidth
            val bottom = top + cardHeight

            // Month Card Background
            paint.color = Color.WHITE
            val monthCard = RectF(left, top, right, bottom)
            canvas.drawRoundRect(monthCard, 8f, 8f, paint)

            // Month Title
            paint.color = colorPrimary
            paint.textSize = 10.5f
            paint.isFakeBoldText = true
            val mName = monthNames.getOrElse(m - 1) { "Month $m" }
            canvas.drawText(mName, left + 10f, top + 16f, paint)

            val monthShifts = annualShifts[m] ?: emptyMap()
            val mDays = monthShifts.values.count { it > 0 }
            val mHrs = monthShifts.values.sum()

            paint.color = colorMuted
            paint.textSize = 7.5f
            paint.isFakeBoldText = false
            canvas.drawText("${mDays}d · ${"%.0f".format(mHrs)}h", right - 42f, top + 16f, paint)

            // Day Headers
            paint.textSize = 7f
            paint.color = colorMuted
            val cellW = (cardWidth - 16f) / 7f
            dayLabels.forEachIndexed { idx, dLabel ->
                val dx = left + 8f + idx * cellW + (cellW / 2f) - 3f
                canvas.drawText(dLabel, dx, top + 28f, paint)
            }

            // Calculate Start Day and Max Days for Month
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, m - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val startDow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0 = Mon
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Draw Day Grid
            val totalGridCells = startDow + maxDays
            val rowsInMonth = (totalGridCells + 6) / 7
            val cellH = 15f

            for (r in 0 until rowsInMonth) {
                for (c in 0 until 7) {
                    val cellIdx = r * 7 + c
                    val dayNum = cellIdx - startDow + 1
                    if (dayNum in 1..maxDays) {
                        val cx = left + 8f + c * cellW
                        val cy = top + 34f + r * cellH
                        val hrs = monthShifts[dayNum] ?: 0.0

                        val cellColor = when {
                            hrs >= 12.0 -> colorOt12
                            hrs >= 10.0 -> colorOt10
                            hrs >= 8.0 -> colorRegular
                            hrs > 0.0 -> colorPrimary
                            else -> colorOff
                        }

                        paint.color = cellColor
                        val dayBox = RectF(cx + 1f, cy + 1f, cx + cellW - 1f, cy + cellH - 1f)
                        canvas.drawRoundRect(dayBox, 3f, 3f, paint)

                        paint.color = if (hrs > 0) Color.WHITE else colorMuted
                        paint.textSize = 6.5f
                        paint.isFakeBoldText = hrs > 0
                        val textX = cx + (cellW / 2f) - (if (dayNum >= 10) 4f else 2f)
                        canvas.drawText("$dayNum", textX, cy + 10f, paint)
                    }
                }
            }
        }

        // Legend at Bottom
        val legendY = 825f
        paint.textSize = 8f
        paint.color = colorDark
        paint.isFakeBoldText = false
        canvas.drawText("Legend: ", 30f, legendY, paint)

        val legendItems = listOf(
            Triple(colorRegular, "8h Regular", 75f),
            Triple(colorOt10, "10h Overtime", 160f),
            Triple(colorOt12, "12h Max Shift", 250f),
            Triple(colorOff, "Day Off", 340f)
        )

        legendItems.forEach { (col, label, lx) ->
            paint.color = col
            canvas.drawRoundRect(RectF(lx, legendY - 8f, lx + 12f, legendY + 2f), 2f, 2f, paint)
            paint.color = colorDark
            canvas.drawText(label, lx + 16f, legendY, paint)
        }

        document.finishPage(page)

        // Write to cache
        val pdfFile = File(context.cacheDir, "Annual_Shift_Schedule_${year}.pdf")
        val outputStream = FileOutputStream(pdfFile)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        return pdfFile
    }

    /**
     * Shares generated annual shift PDF via Android sharesheet.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ${pdfFile.name}"))
    }
}
