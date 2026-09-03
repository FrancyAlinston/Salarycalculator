package com.example.salarycalculator.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

@Serializable
data class BackupBundle(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val records: List<MonthlySalaryRecord> = emptyList(),
    val profiles: List<EmployerProfile> = emptyList(),
    val customDeductions: List<CustomDeduction> = emptyList(),
    val taxCode: String = "1257L",
    val taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    val taxYear: TaxYear = TaxYear.YEAR_2024_2025,
    val pensionRate: Double = 5.0,
    val hourlyRate: Double = 12.71,
    val hasMarriageAllowance: Boolean = false,
    val hasBlindPersonsAllowance: Boolean = false
)

object LedgerBackupManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * Serializes all repository data into a BackupBundle JSON file.
     */
    fun createBackupFile(
        context: Context,
        bundle: BackupBundle
    ): File {
        val jsonString = json.encodeToString(bundle)
        val file = File(context.cacheDir, "Salary_Backup_${System.currentTimeMillis()}.json")
        FileOutputStream(file).use { fos ->
            fos.write(jsonString.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Shares the backup JSON file via Android Sharesheet.
     */
    fun shareBackup(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/json"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Salary Ledger Backup"))
    }

    /**
     * Parses and validates JSON backup content.
     */
    fun parseBackupJson(jsonString: String): Result<BackupBundle> {
        return try {
            val bundle = json.decodeFromString<BackupBundle>(jsonString)
            Result.success(bundle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores a BackupBundle into the active repository.
     */
    suspend fun restoreBundle(
        repository: SalaryRepository,
        bundle: BackupBundle
    ) {
        // 1. Restore settings
        repository.setTaxCode(bundle.taxCode)
        repository.setTaxRegion(bundle.taxRegion)
        repository.setTaxYear(bundle.taxYear)
        repository.setPensionRate(bundle.pensionRate)
        repository.setDefaultHourlyRate(bundle.hourlyRate)
        repository.setHasMarriageAllowance(bundle.hasMarriageAllowance)
        repository.setHasBlindPersonsAllowance(bundle.hasBlindPersonsAllowance)

        // 2. Restore profiles
        for (profile in bundle.profiles) {
            repository.saveEmployerProfile(profile)
        }

        // 3. Restore custom deductions
        for (deduction in bundle.customDeductions) {
            repository.saveCustomDeduction(deduction)
        }

        // 4. Restore monthly records
        for (record in bundle.records) {
            repository.saveSalaryRecord(record)
        }
    }
}
