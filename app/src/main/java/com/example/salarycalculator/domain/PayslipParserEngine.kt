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
 * Parsed payslip data representation.
 */
data class ParsedPayslipData(
    val employerName: String? = null,
    val payPeriod: String = "Payslip Record",
    val taxCode: String = "1257L",
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
)

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

        var taxCode = "1257L"
        var grossPay = 0.0
        var netPay = 0.0
        var incomeTax = 0.0
        var nationalInsurance = 0.0
        var employeePension = 0.0
        var employerPension = 0.0
        var studentLoan = 0.0
        var payPeriod = "Imported Payslip"
        var employer: String? = null

        // 1. Tax Code Regex (e.g. 1257L, BR, 0T, S1257L, C1257L, 1383M, D0, D1, NT)
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

        // 2. Gross Pay Detection
        val grossRegexes = listOf(
            """(?:Total\s+Gross|Gross\s+Pay|Gross\s+Salary|Total\s+Earnings|Gross\s+This\s+Period|Total\s+Payments)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:Gross)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (r in grossRegexes) {
            val match = r.find(rawText)
            if (match != null) {
                grossPay = parseAmount(match.groupValues[1])
                if (grossPay > 0) break
            }
        }

        // 3. Net Pay Detection
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

        // 4. PAYE / Income Tax Detection
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

        // 5. National Insurance Detection
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

        // 6. Pension Detection
        val pensionRegexes = listOf(
            """(?:Employee\s+Pension|EE\s+Pension|Workplace\s+Pension|Pension\s+Salary\s+Sacrifice|Pension\s+Deduction)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:Pension)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (r in pensionRegexes) {
            val match = r.find(rawText)
            if (match != null) {
                employeePension = parseAmount(match.groupValues[1])
                if (employeePension > 0) break
            }
        }

        // 7. Student Loan Detection
        val studentLoanRegex = """(?:Student\s+Loan|Student\s+Loan\s+Plan\s+[124]|Postgraduate\s+Loan)[:\s]*£?\s*([0-9,]+\.[0-9]{2})""".toRegex(RegexOption.IGNORE_CASE)
        studentLoanRegex.find(rawText)?.let {
            studentLoan = parseAmount(it.groupValues[1])
        }

        // 8. Period Date Detection (e.g. Month 01, January 2025, 31/01/2025)
        val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        for (m in monthNames) {
            if (rawText.contains(m, ignoreCase = true)) {
                val yearMatch = """(202[3-9])""".toRegex().find(rawText)
                val yr = yearMatch?.value ?: "2025"
                payPeriod = "$m $yr"
                break
            }
        }

        // 9. Employer Name Candidate
        val employerMatch = """(?:Employer|Company|Organisation)[:\s]+([A-Za-z0-9 &.,'-]{3,35})""".toRegex(RegexOption.IGNORE_CASE).find(rawText)
        if (employerMatch != null) {
            employer = employerMatch.groupValues[1].trim()
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
            employerName = employer,
            payPeriod = payPeriod,
            taxCode = taxCode,
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
}
