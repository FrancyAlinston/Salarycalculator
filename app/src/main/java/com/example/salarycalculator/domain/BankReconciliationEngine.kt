package com.example.salarycalculator.domain

import kotlin.math.abs

enum class ReconciliationStatus(val displayName: String) {
    EXACT_MATCH("Exact Match (0.00 Variance)"),
    VARIANCE_DETECTED("Variance Detected"),
    UNMATCHED_CREDIT("Unmatched Credit Deposit"),
    UNMATCHED_PAYSLIP("Unreconciled Payslip")
}

data class BankTransaction(
    val date: String,
    val description: String,
    val amount: Double
)

data class ReconciliationItem(
    val bankDate: String,
    val description: String,
    val bankAmount: Double,
    val matchedPayslipMonth: String?,
    val payslipNetPay: Double?,
    val variance: Double,
    val status: ReconciliationStatus,
    val notes: String
)

data class BankReconciliationSummary(
    val totalCreditsFound: Int,
    val totalMatchedCount: Int,
    val totalDiscrepanciesCount: Int,
    val totalReconciledAmount: Double,
    val items: List<ReconciliationItem>
)

object BankReconciliationEngine {

    fun parseCsv(csvContent: String): List<BankTransaction> {
        val transactions = mutableListOf<BankTransaction>()
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val header = lines.first().lowercase()
        val hasHeader = header.contains("date") || header.contains("description") || header.contains("amount")
        val dataRows = if (hasHeader) lines.drop(1) else lines

        for (line in dataRows) {
            val tokens = parseCsvLine(line)
            if (tokens.size >= 3) {
                val date = tokens[0].trim().replace("\"", "")
                val description = tokens[1].trim().replace("\"", "")
                val amountStr = tokens[2].trim().replace("\"", "").replace("£", "").replace(",", "")
                val amount = amountStr.toDoubleOrNull() ?: continue

                // Only reconcile positive inflows / salary credits
                if (amount > 0) {
                    transactions.add(BankTransaction(date = date, description = description, amount = amount))
                }
            }
        }
        return transactions
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        for (char in line) {
            if (char == '\"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(char)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    fun reconcile(
        transactions: List<BankTransaction>,
        savedRecords: List<MonthlySalaryRecord>
    ): BankReconciliationSummary {
        val items = mutableListOf<ReconciliationItem>()
        val matchedRecordIds = mutableSetOf<String>()

        for (tx in transactions) {
            // Find closest matching saved payslip record
            var bestMatch: MonthlySalaryRecord? = null
            var minDiff = Double.MAX_VALUE

            for (record in savedRecords) {
                if (record.monthYear !in matchedRecordIds) {
                    val diff = abs(tx.amount - record.netPay)
                    if (diff < minDiff && diff <= (record.netPay * 0.35)) { // within 35% range
                        minDiff = diff
                        bestMatch = record
                    }
                }
            }

            if (bestMatch != null) {
                matchedRecordIds.add(bestMatch.monthYear)
                val variance = tx.amount - bestMatch.netPay
                val status = if (abs(variance) < 0.05) {
                    ReconciliationStatus.EXACT_MATCH
                } else {
                    ReconciliationStatus.VARIANCE_DETECTED
                }

                val notes = if (status == ReconciliationStatus.EXACT_MATCH) {
                    "Exact net paycheck deposit matched to ${bestMatch.monthYear} ledger."
                } else if (variance > 0) {
                    "Bank deposit is £${"%,.2f".format(variance)} higher than recorded net pay (unlogged bonus or overtime)."
                } else {
                    "Bank deposit is £${"%,.2f".format(abs(variance))} lower than recorded net pay (unrecorded post-tax deductions)."
                }

                items.add(
                    ReconciliationItem(
                        bankDate = tx.date,
                        description = tx.description,
                        bankAmount = tx.amount,
                        matchedPayslipMonth = bestMatch.monthYear,
                        payslipNetPay = bestMatch.netPay,
                        variance = variance,
                        status = status,
                        notes = notes
                    )
                )
            } else {
                items.add(
                    ReconciliationItem(
                        bankDate = tx.date,
                        description = tx.description,
                        bankAmount = tx.amount,
                        matchedPayslipMonth = null,
                        payslipNetPay = null,
                        variance = 0.0,
                        status = ReconciliationStatus.UNMATCHED_CREDIT,
                        notes = "No matching monthly payslip found in local salary ledger."
                    )
                )
            }
        }

        val exactAndVariance = items.filter { it.status == ReconciliationStatus.EXACT_MATCH || it.status == ReconciliationStatus.VARIANCE_DETECTED }
        val exactMatches = items.count { it.status == ReconciliationStatus.EXACT_MATCH }
        val variances = items.count { it.status == ReconciliationStatus.VARIANCE_DETECTED }
        val totalReconciled = exactAndVariance.sumOf { it.bankAmount }

        return BankReconciliationSummary(
            totalCreditsFound = transactions.size,
            totalMatchedCount = exactMatches,
            totalDiscrepanciesCount = variances,
            totalReconciledAmount = totalReconciled,
            items = items
        )
    }

    fun generateSampleCsv(records: List<MonthlySalaryRecord>): String {
        val sb = StringBuilder()
        sb.append("Date,Description,Amount\n")
        if (records.isNotEmpty()) {
            records.take(6).forEachIndexed { index, record ->
                val monthNum = (index + 1).toString().padStart(2, '0')
                sb.append("28/$monthNum/2025,EMPLOYER PAYROLL BACS,${"%.2f".format(record.netPay)}\n")
            }
        } else {
            sb.append("28/01/2025,PRIMARY EMPLOYMENT BACS,1676.19\n")
            sb.append("28/02/2025,PRIMARY EMPLOYMENT BACS,1676.19\n")
            sb.append("28/03/2025,PRIMARY EMPLOYMENT BACS,1740.00\n")
            sb.append("15/04/2025,HMRC TAX REBATE DIRECT,320.00\n")
        }
        return sb.toString()
    }
}
