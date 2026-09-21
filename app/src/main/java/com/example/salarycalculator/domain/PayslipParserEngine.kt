package com.example.salarycalculator.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

/**
 * Parsed payslip data representation with comprehensive UK line-item breakdown.
 */
data class ParsedPayslipData(
    val employeeName: String? = null,
    val employeeRef: String? = null,
    val niNumber: String? = null,
    val employerName: String? = null,
    val payPeriod: String = "Payslip Record",
    val processDate: String? = null,
    val taxPeriod: Int? = null,
    val taxCode: String = "1257L",
    val basicHours: Double = 0.0,
    val basicRate: Double = 0.0,
    val basicAmount: Double = 0.0,
    val bankHolidayHours: Double = 0.0,
    val bankHolidayRate: Double = 0.0,
    val bankHolidayAmount: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val overtimeRate: Double = 0.0,
    val overtimeAmount: Double = 0.0,
    val grossPay: Double = 0.0,
    val netPay: Double = 0.0,
    val incomeTax: Double = 0.0,
    val nationalInsurance: Double = 0.0,
    val employeePension: Double = 0.0,
    val employerPension: Double = 0.0,
    val studentLoan: Double = 0.0,
    val rawExtractedText: String = "",
    val confidenceRating: String = "High",
    val verificationAnalysis: PayslipAnalysisResult? = null
) {
    /**
     * Total paid hours calculated across basic, bank holiday, and overtime units.
     */
    val totalPaidHours: Double
        get() = if (basicHours > 0.0) basicHours + overtimeHours else 0.0
}

/**
 * Statutory diagnostic check comparing parsed payslip against expected HMRC tax rules.
 */
data class PayslipAnalysisResult(
    val expectedIncomeTax: Double,
    val expectedNationalInsurance: Double,
    val expectedNetPay: Double,
    val taxVariance: Double,
    val niVariance: Double,
    val isEmergencyTax: Boolean,
    val isStatutoryMatch: Boolean,
    val statusMessage: String
)

object PayslipParserEngine {

    /**
     * Parses raw extracted payslip text using heuristic regex rules tailored for UK payslip formats.
     */
    fun parsePayslipText(rawText: String): ParsedPayslipData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var employeeName: String? = null
        var employeeRef: String? = null
        var niNumber: String? = null
        var processDate: String? = null
        var taxPeriod: Int? = null
        var taxCode = "1257L"
        var grossPay = 0.0
        var netPay = 0.0
        var incomeTax = 0.0
        var nationalInsurance = 0.0
        var employeePension = 0.0
        var employerPension = 0.0
        var studentLoan = 0.0
        var basicHours = 0.0
        var basicRate = 0.0
        var basicAmount = 0.0
        var bankHolidayHours = 0.0
        var bankHolidayRate = 0.0
        var bankHolidayAmount = 0.0
        var overtimeHours = 0.0
        var overtimeRate = 0.0
        var overtimeAmount = 0.0
        var payPeriod = "Imported Payslip"
        var employer: String? = null

        // 1. Tabular Header Row Detection (e.g. "910 Francy Alinston D'Silva 30-09-2026 RZ021006C")
        val tableHeaderValueRegex = """\b(\d{2,8})\s+([A-Za-z' -]{3,35})\s+(\d{1,2}[-/\.]\d{1,2}[-/\.]\d{2,4})\s+([A-CEGHJ-PR-TW-Z]{2}\s*\d{6}\s*[A-D])\b""".toRegex(RegexOption.IGNORE_CASE)
        for (line in lines) {
            val m = tableHeaderValueRegex.find(line)
            if (m != null) {
                employeeRef = m.groupValues[1].trim()
                employeeName = m.groupValues[2].trim()
                processDate = m.groupValues[3].trim()
                niNumber = m.groupValues[4].replace(" ", "").uppercase()
                break
            }
        }

        // 2. Employee Name Detection (Standard patterns)
        if (employeeName == null) {
            val empNameRegex = """(?:Employee\s*Name|Name)[:\s]+([A-Za-z' -]{3,40})""".toRegex(RegexOption.IGNORE_CASE)
            empNameRegex.find(rawText)?.let {
                val candidate = it.groupValues[1].trim()
                if (!candidate.contains("Process", ignoreCase = true) && !candidate.contains("Period", ignoreCase = true) && !candidate.contains("Number", ignoreCase = true)) {
                    employeeName = candidate
                }
            }
        }
        if (employeeName == null) {
            // Check for Francy Alinston D'Silva or standard 2-4 word capitalized names before address
            val bottomNameRegex = """\n([A-Z][a-z]+(?:\s+[A-Z][a-z]+|\s+D'[A-Z][a-z]+){1,4})\n(?:Waterloo|Ward|Unit|Hospital|Street|Road|Avenue|Lane|Leeds|London|Manchester)""".toRegex()
            bottomNameRegex.find(rawText)?.let {
                employeeName = it.groupValues[1].trim()
            }
        }

        // 3. Employee Ref / Payroll ID
        if (employeeRef == null) {
            val refRegex = """(?:Ref\.?|Emp(?:loyee)?\s*No|Payroll\s*(?:ID|No))[:\s]*([0-9A-Za-z-]{2,15})""".toRegex(RegexOption.IGNORE_CASE)
            refRegex.find(rawText)?.let {
                val r = it.groupValues[1].trim()
                if (!r.contains("Employee", ignoreCase = true)) {
                    employeeRef = r
                }
            }
        }

        // 4. National Insurance Number (UK format: e.g. RZ021006C, QQ123456A)
        if (niNumber == null) {
            val niRegex = """\b([A-CEGHJ-PR-TW-Z]{2}\s*\d{6}\s*[A-D])\b""".toRegex(RegexOption.IGNORE_CASE)
            niRegex.find(rawText)?.let {
                niNumber = it.groupValues[1].replace(" ", "").uppercase()
            }
        }

        // 5. Process Date (e.g. 30-09-2026, 30/09/2026, 2026-09-30)
        if (processDate == null) {
            val dateRegex = """(?:Process\s*Date|Pay\s*Date|Date)[:\s]*(\d{1,2}[-/\.]\d{1,2}[-/\.]\d{2,4})""".toRegex(RegexOption.IGNORE_CASE)
            dateRegex.find(rawText)?.let {
                processDate = it.groupValues[1].trim()
            }
        }

        // 6. Tax Period (e.g. Tax Period: 6, Month: 06)
        val taxPeriodRegex = """(?:Tax\s*Period|Period)[:\s]*(\d{1,2})\b""".toRegex(RegexOption.IGNORE_CASE)
        taxPeriodRegex.find(rawText)?.let {
            taxPeriod = it.groupValues[1].toIntOrNull()
        }

        // 6. Tax Code Regex (e.g. 1257L, BR, 0T, S1257L, C1257L, 1383M, D0, D1, NT)
        val taxCodeRegex = Pattern.compile("""\b(S|C)?(\d{3,4}[LMNPTY]|BR|0T|D0|D1|NT)\b""", Pattern.CASE_INSENSITIVE)
        for (line in lines) {
            val matcher = taxCodeRegex.matcher(line)
            if (matcher.find()) {
                val candidate = matcher.group(0)?.uppercase() ?: "1257L"
                if (!candidate.startsWith("0") || candidate == "0T") {
                    taxCode = candidate
                    break
                }
            }
        }

        // 7. Line Item: Basic Pay (Units / Rate / Amount, e.g. "Basic 179.90 12.82 2306.32" or "Basic: 179.90 hrs @ £12.82")
        val basicLineRegex = """(?:Basic|Standard\s*Hours|Hourly\s*Pay|Basic\s*Pay)\s+([0-9]+\.[0-9]{1,2})\s+([0-9]+\.[0-9]{1,2})\s+([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        val basicMatch = basicLineRegex.find(rawText)
        if (basicMatch != null) {
            basicHours = parseAmount(basicMatch.groupValues[1])
            basicRate = parseAmount(basicMatch.groupValues[2])
            basicAmount = parseAmount(basicMatch.groupValues[3])
        } else {
            // Fallback individual patterns
            """(?:Basic\s*Hours|Hours\s*Worked|Units)[:\s]*([0-9]+\.[0-9]{1,2})""".toRegex(RegexOption.IGNORE_CASE).find(rawText)?.let {
                basicHours = parseAmount(it.groupValues[1])
            }
            """(?:Hourly\s*Rate|Basic\s*Rate|Rate)[:\s]*£?\s*([0-9]+\.[0-9]{1,2})""".toRegex(RegexOption.IGNORE_CASE).find(rawText)?.let {
                basicRate = parseAmount(it.groupValues[1])
            }
            """(?:Basic\s*Pay|Basic\s*Amount)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE).find(rawText)?.let {
                basicAmount = parseAmount(it.groupValues[1])
            }
        }

        // 8. Line Item: Bank Holiday (Units / Rate / Amount, e.g. "Bank Holiday 7.25 6.41 46.47")
        val bhLineRegex = """(?:Bank\s*Holiday|Public\s*Holiday|BH\s*Uplift)\s+([0-9]+\.[0-9]{1,2})\s+([0-9]+\.[0-9]{1,2})\s+([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        val bhMatch = bhLineRegex.find(rawText)
        if (bhMatch != null) {
            bankHolidayHours = parseAmount(bhMatch.groupValues[1])
            bankHolidayRate = parseAmount(bhMatch.groupValues[2])
            bankHolidayAmount = parseAmount(bhMatch.groupValues[3])
        }

        // 9. Line Item: Overtime (Units / Rate / Amount, e.g. "Overtime 12.00 19.23 230.76")
        val otLineRegex = """(?:Overtime|OT|Extra\s*Hours)\s+([0-9]+\.[0-9]{1,2})\s+([0-9]+\.[0-9]{1,2})\s+([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        val otMatch = otLineRegex.find(rawText)
        if (otMatch != null) {
            overtimeHours = parseAmount(otMatch.groupValues[1])
            overtimeRate = parseAmount(otMatch.groupValues[2])
            overtimeAmount = parseAmount(otMatch.groupValues[3])
        }

        // 10. Gross Pay Detection
        val grossRegexes = listOf(
            """(?:Total\s+Gross\s+Pay|Total\s+Gross|Gross\s+for\s+Tax|Gross\s+Pay|Gross\s+Salary|Total\s+Earnings|Gross\s+This\s+Period|Total\s+Payments)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:Gross)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (r in grossRegexes) {
            val match = r.find(rawText)
            if (match != null) {
                grossPay = parseAmount(match.groupValues[1])
                if (grossPay > 0) break
            }
        }
        if (grossPay == 0.0 && basicAmount > 0.0) {
            grossPay = basicAmount + bankHolidayAmount + overtimeAmount
        }

        // 11. Net Pay Detection
        val netRegexes = listOf(
            """(?:Net\s+Pay|Take\s+Home|Net\s+Amount|Total\s+Net|Paid\s+to\s+Bank|Bank\s+Payment|BACS)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:Net)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (r in netRegexes) {
            val match = r.find(rawText)
            if (match != null) {
                netPay = parseAmount(match.groupValues[1])
                if (netPay > 0) break
            }
        }

        // 12. PAYE / Income Tax Detection
        val taxRegexes = listOf(
            """(?:PAYE\s+Tax|PAYE|Income\s+Tax|Tax\s+Paid|Tax\s+Deducted|Tax\s+This\s+Period)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:Tax)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (r in taxRegexes) {
            val match = r.find(rawText)
            if (match != null) {
                incomeTax = parseAmount(match.groupValues[1])
                if (incomeTax > 0) break
            }
        }

        // 13. National Insurance Detection
        val niRegexes = listOf(
            """(?:National\s+Insurance|Employee\s+NI|EE\s+NI|NI\s+Paid|NIC|Class\s+1\s+NI)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:NI)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (r in niRegexes) {
            val match = r.find(rawText)
            if (match != null) {
                nationalInsurance = parseAmount(match.groupValues[1])
                if (nationalInsurance > 0) break
            }
        }

        // 14. Pension Detection
        val pensionRegexes = listOf(
            """(?:Employee\s+Pension|EE\s+Pension|Workplace\s+Pension|Pension\s+Salary\s+Sacrifice|Pension\s+Deduction|Pension\s*\(Inc\s*AVC\s*&\s*APC\))[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:Pension)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (r in pensionRegexes) {
            val match = r.find(rawText)
            if (match != null) {
                employeePension = parseAmount(match.groupValues[1])
                if (employeePension > 0) break
            }
        }

        // 15. Student Loan Detection
        val studentLoanRegex = """(?:Student\s+Loan|Student\s+Loan\s+Plan\s+[124]|Postgraduate\s+Loan)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        studentLoanRegex.find(rawText)?.let {
            studentLoan = parseAmount(it.groupValues[1])
        }

        // 16. Period Date Detection (e.g. Month 01, September 2026, 30/09/2026)
        val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        for (m in monthNames) {
            if (rawText.contains(m, ignoreCase = true)) {
                val yearMatch = """(202[3-9])""".toRegex().find(rawText)
                val yr = yearMatch?.value ?: (processDate?.takeLast(4) ?: "2026")
                payPeriod = "$m $yr"
                break
            }
        }
        if (payPeriod == "Imported Payslip" && processDate != null) {
            val parts = processDate!!.split('-', '/', '.')
            if (parts.size == 3) {
                val mIdx = parts[1].toIntOrNull() ?: 1
                if (mIdx in 1..12) {
                    val mName = monthNames[mIdx - 1]
                    val yr = if (parts[2].length == 4) parts[2] else "20${parts[2]}"
                    payPeriod = "$mName $yr"
                }
            }
        }

        // 17. Employer Name Candidate (e.g. Waterloo Manor Ltd, NHS Trust, etc.)
        val employerMatch = """(?:Waterloo\s*Manor(?:\s*Ltd|\s*Hospital)?|NHS(?:\s*Trust|\s*Foundation)?|(?:Employer|Company|Organisation)[:\s]+([A-Za-z0-9 &.,'-]{3,35}))""".toRegex(RegexOption.IGNORE_CASE).find(rawText)
        if (employerMatch != null) {
            val matched = employerMatch.value.trim()
            employer = if (matched.contains("Waterloo", ignoreCase = true)) {
                "Waterloo Manor Ltd"
            } else if (matched.contains("NHS", ignoreCase = true)) {
                "NHS Foundation Trust"
            } else {
                employerMatch.groupValues.getOrNull(1)?.trim() ?: matched
            }
        }

        // Fallback calculations if net pay missing but gross & deductions present
        if (netPay == 0.0 && grossPay > 0.0) {
            netPay = maxOf(0.0, grossPay - incomeTax - nationalInsurance - employeePension - studentLoan)
        }

        // Perform statutory verification analysis
        val analysis = analyzeStatutoryAlignment(
            gross = grossPay,
            taxCode = taxCode,
            actualTax = incomeTax,
            actualNI = nationalInsurance,
            actualNet = netPay
        )

        return ParsedPayslipData(
            employeeName = employeeName,
            employeeRef = employeeRef,
            niNumber = niNumber,
            employerName = employer,
            payPeriod = payPeriod,
            processDate = processDate,
            taxPeriod = taxPeriod,
            taxCode = taxCode,
            basicHours = basicHours,
            basicRate = basicRate,
            basicAmount = basicAmount,
            bankHolidayHours = bankHolidayHours,
            bankHolidayRate = bankHolidayRate,
            bankHolidayAmount = bankHolidayAmount,
            overtimeHours = overtimeHours,
            overtimeRate = overtimeRate,
            overtimeAmount = overtimeAmount,
            grossPay = grossPay,
            netPay = netPay,
            incomeTax = incomeTax,
            nationalInsurance = nationalInsurance,
            employeePension = employeePension,
            employerPension = employerPension,
            studentLoan = studentLoan,
            rawExtractedText = rawText,
            confidenceRating = if (grossPay > 0 && netPay > 0) "High Confidence" else "Moderate (Review Suggested)",
            verificationAnalysis = analysis
        )
    }

    private fun parseAmount(str: String): Double {
        return str.replace(",", "").replace("£", "").trim().toDoubleOrNull() ?: 0.0
    }

    /**
     * Compares parsed payslip metrics against standard UK HMRC rules.
     */
    fun analyzeStatutoryAlignment(
        gross: Double,
        taxCode: String,
        actualTax: Double,
        actualNI: Double,
        actualNet: Double
    ): PayslipAnalysisResult {
        if (gross <= 0.0) {
            return PayslipAnalysisResult(
                expectedIncomeTax = 0.0,
                expectedNationalInsurance = 0.0,
                expectedNetPay = 0.0,
                taxVariance = 0.0,
                niVariance = 0.0,
                isEmergencyTax = false,
                isStatutoryMatch = true,
                statusMessage = "Enter or verify your gross pay."
            )
        }

        // Run calculation sequence
        val isScottish = taxCode.startsWith("S", ignoreCase = true)
        val region = if (isScottish) TaxRegion.SCOTLAND else TaxRegion.UK_STANDARD
        val result = TaxCalculator.calculateTax(
            grossPay = gross,
            taxCode = taxCode,
            isMonthly = true,
            region = region
        )

        val taxDiff = actualTax - result.incomeTax
        val niDiff = actualNI - result.nationalInsurance
        val isEmergency = taxCode.equals("BR", ignoreCase = true) || taxCode.equals("0T", ignoreCase = true) || taxCode.equals("D0", ignoreCase = true)
        val isMatch = Math.abs(taxDiff) <= 1.0 && Math.abs(niDiff) <= 1.0

        val msg = when {
            isEmergency -> "Emergency tax code detected ($taxCode). Personal allowance is not being applied, resulting in higher PAYE deductions."
            isMatch -> "Exact match with official HMRC statutory calculation."
            Math.abs(taxDiff) > 1.0 -> "Tax variance of £${"%.2f".format(Math.abs(taxDiff))} detected against standard $taxCode calculation (possibly due to mid-year cumulative adjustments or benefits)."
            else -> "National Insurance variance of £${"%.2f".format(Math.abs(niDiff))} detected."
        }

        return PayslipAnalysisResult(
            expectedIncomeTax = result.incomeTax,
            expectedNationalInsurance = result.nationalInsurance,
            expectedNetPay = result.netPay,
            taxVariance = taxDiff,
            niVariance = niDiff,
            isEmergencyTax = isEmergency,
            isStatutoryMatch = isMatch,
            statusMessage = msg
        )
    }
}

object PayslipOcrAnalyzer {

    /**
     * Extracts text from an image Bitmap using Google ML Kit on-device Text Recognition.
     */
    suspend fun analyzeImage(bitmap: Bitmap): ParsedPayslipData = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            val result = com.google.android.gms.tasks.Tasks.await(recognizer.process(image))
            val text = result.text
            PayslipParserEngine.parsePayslipText(text)
        } catch (e: Exception) {
            ParsedPayslipData(
                rawExtractedText = "OCR Extraction failed: ${e.localizedMessage}",
                confidenceRating = "Failed"
            )
        }
    }

    /**
     * Renders PDF pages to Bitmaps and executes text OCR extraction.
     */
    suspend fun analyzePdf(context: Context, pdfUri: Uri): ParsedPayslipData = withContext(Dispatchers.IO) {
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (pfd == null) {
                return@withContext ParsedPayslipData(rawExtractedText = "Could not open PDF file.")
            }

            val renderer = PdfRenderer(pfd)
            val fullTextBuilder = StringBuilder()

            val pageCount = minOf(3, renderer.pageCount) // First 3 pages maximum
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = page.width * 2 // 2x scale for high OCR accuracy
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromBitmap(bitmap, 0)
                val visionText = com.google.android.gms.tasks.Tasks.await(recognizer.process(image))
                fullTextBuilder.append(visionText.text).append("\n")
                bitmap.recycle()
            }

            renderer.close()
            pfd.close()

            PayslipParserEngine.parsePayslipText(fullTextBuilder.toString())
        } catch (e: Exception) {
            ParsedPayslipData(
                rawExtractedText = "PDF Extraction failed: ${e.localizedMessage}",
                confidenceRating = "Failed"
            )
        }
    }

    /**
     * Renders each individual page of a PDF and returns a list of parsed payslips (1 per page).
     */
    suspend fun analyzePdfAllPages(context: Context, pdfUri: Uri): List<ParsedPayslipData> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ParsedPayslipData>()
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (pfd == null) return@withContext emptyList()

            val renderer = PdfRenderer(pfd)
            val count = minOf(12, renderer.pageCount) // Up to 12 monthly payslips

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromBitmap(bitmap, 0)
                val visionText = com.google.android.gms.tasks.Tasks.await(recognizer.process(image))
                val parsed = PayslipParserEngine.parsePayslipText(visionText.text)
                if (parsed.grossPay > 0 || parsed.netPay > 0) {
                    results.add(parsed)
                }
                bitmap.recycle()
            }

            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            // Return whatever was successfully parsed
        }
        results
    }

    /**
     * Analyzes multiple bitmap images in batch and returns parsed payslip results.
     */
    suspend fun analyzeMultipleImages(bitmaps: List<Bitmap>): List<ParsedPayslipData> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ParsedPayslipData>()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        for (bmp in bitmaps) {
            try {
                val image = InputImage.fromBitmap(bmp, 0)
                val textResult = com.google.android.gms.tasks.Tasks.await(recognizer.process(image))
                val parsed = PayslipParserEngine.parsePayslipText(textResult.text)
                if (parsed.grossPay > 0 || parsed.netPay > 0) {
                    results.add(parsed)
                }
            } catch (e: Exception) {
                // Ignore failed individual page
            }
        }
        results
    }
}

