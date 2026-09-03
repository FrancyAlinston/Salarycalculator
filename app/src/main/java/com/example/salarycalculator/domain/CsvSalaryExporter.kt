package com.example.salarycalculator.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvSalaryExporter {

    /**
     * Exports a list of MonthlySalaryRecord items into a formatted CSV file.
     */
    fun exportHistoryCsv(context: Context, history: List<MonthlySalaryRecord>): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK)
        val stringBuilder = StringBuilder()

        // CSV Header
        stringBuilder.append("ID,Month_Year,Timestamp,Date_Saved,Days_Worked,Hours_Per_Day,Total_Hours,Overtime_Hours,Overtime_Multiplier,Hourly_Rate_GBP,Gross_Pay_GBP,Salary_Sacrifice_GBP,Pension_Rate_Pct,Employee_Pension_GBP,Employer_Pension_GBP,Taxable_Pay_GBP,PAYE_Income_Tax_GBP,National_Insurance_GBP,Student_Loan_Plan,Student_Loan_Deduction_GBP,Total_Deductions_GBP,Net_Take_Home_GBP,Tax_Code,Tax_Region,Notes\n")

        for (record in history) {
            val totalHours = (record.daysWorked * record.hoursPerDay) + record.overtimeHours
            val dateStr = dateFormat.format(Date(record.timestamp))
            val cleanNote = record.note.replace("\"", "\"\"").replace("\n", " ")

            stringBuilder.append("\"${record.id}\",")
            stringBuilder.append("\"${record.monthYear}\",")
            stringBuilder.append("${record.timestamp},")
            stringBuilder.append("\"$dateStr\",")
            stringBuilder.append("${record.daysWorked},")
            stringBuilder.append("${record.hoursPerDay},")
            stringBuilder.append("${"%.2f".format(totalHours)},")
            stringBuilder.append("${record.overtimeHours},")
            stringBuilder.append("${record.overtimeMultiplier},")
            stringBuilder.append("${"%.2f".format(record.hourlyRate)},")
            stringBuilder.append("${"%.2f".format(record.grossPay)},")
            stringBuilder.append("${"%.2f".format(record.salarySacrifice)},")
            stringBuilder.append("${record.pensionRate},")
            stringBuilder.append("${"%.2f".format(record.pensionContribution)},")
            stringBuilder.append("${"%.2f".format(record.employerPension)},")
            stringBuilder.append("${"%.2f".format(record.taxablePay)},")
            stringBuilder.append("${"%.2f".format(record.incomeTax)},")
            stringBuilder.append("${"%.2f".format(record.nationalInsurance)},")
            stringBuilder.append("\"${record.studentLoanPlan.name}\",")
            stringBuilder.append("${"%.2f".format(record.studentLoanDeduction)},")
            stringBuilder.append("${"%.2f".format(record.totalDeductions)},")
            stringBuilder.append("${"%.2f".format(record.netPay)},")
            stringBuilder.append("\"${record.taxCode}\",")
            stringBuilder.append("\"${record.taxRegion.name}\",")
            stringBuilder.append("\"$cleanNote\"\n")
        }

        val exportFile = File(context.cacheDir, "Salary_History_Export_${System.currentTimeMillis()}.csv")
        FileOutputStream(exportFile).use { fos ->
            fos.write(stringBuilder.toString().toByteArray(Charsets.UTF_8))
        }

        return exportFile
    }

    /**
     * Helper to share the generated CSV via Android Sharesheet.
     */
    fun shareCsv(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/csv"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Salary History CSV"))
    }
}
