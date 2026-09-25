package com.example.salarycalculator.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.FileOutputStream

@Serializable
data class BackupBundle(
    val version: Int = 2,
    val timestamp: Long = System.currentTimeMillis(),
    val records: List<MonthlySalaryRecord> = emptyList(),
    val profiles: List<EmployerProfile> = emptyList(),
    val customDeductions: List<CustomDeduction> = emptyList(),
    val taxCode: String = "1257L",
    val taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    val taxYear: TaxYear = TaxYear.YEAR_2024_2025,
    val pensionRate: Double = 5.0,
    val hourlyRate: Double = 12.71,
    val defaultHoursPerDay: Double = 12.0,
    val studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
    val overtimeMultiplier: Double = 1.5,
    val weekendOvertimeMultiplier: Double = 1.5,
    val bankHolidayOvertimeMultiplier: Double = 2.0,
    val hasMarriageAllowance: Boolean = false,
    val hasBlindPersonsAllowance: Boolean = false,
    val activeProfileId: String? = null,
    val annualShiftScheduleJson: String = "",
    val employerShiftAssignmentsJson: String = "",
    val payScheduleType: String = "LAST_FRIDAY",
    val payScheduleFixedDay: Int = 25,
    val payScheduleCutoffLeadDays: Int = 5,
    val themeMode: String = "SYSTEM",
    val themePalette: String = "OCEAN",
    val biometricLockEnabled: Boolean = false,
    val biometricHistoryLockEnabled: Boolean = false,
    val biometricTimeoutSeconds: Long = 0L,
    val payrollContactEmail: String = "",
    val preferredCurrency: String = "EUR",
    val secondaryCurrency: String = "USD",
    val customEurRate: Double = ConvertedCurrencies.DEFAULT_EUR_RATE,
    val customUsdRate: Double = ConvertedCurrencies.DEFAULT_USD_RATE,
    val cachedCurrencyRatesJson: String = "",
    val syncthingServerUrl: String = "https://syncthing.449100.xyz",
    val syncthingApiKey: String = "",
    val syncthingFolderId: String = "salary-calculator",
    val syncthingAutoSync: Boolean = false,
    val customCloudEndpoint: String = "",
    val customCloudToken: String = "",
    val autoCloudSyncEnabled: Boolean = false,
    val autoBackupFrequency: String = "DISABLED",
    val googleAccountEmail: String = "",
    val googleDriveAutoSync: Boolean = true,
    val googleDriveLastSync: String = "",
    val annualLeaveHourlyRate: Double = 0.0,
    val annualLeaveHoursPerDay: Double = 7.5,
    val weeklyOvertimeThreshold: Double = 37.5,
    val deviceName: String = "",
    val deviceModel: String = "",
    val deviceId: String = ""
)

object LedgerBackupManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * Compiles a comprehensive BackupBundle snapshot containing all user settings,
     * employer profiles, records, and full shift heatmap schedules.
     */
    suspend fun createFullBackupBundle(repository: SalaryRepository): BackupBundle {
        return BackupBundle(
            version = 2,
            timestamp = System.currentTimeMillis(),
            records = repository.getSalaryHistory().first(),
            profiles = repository.getEmployerProfiles().first(),
            customDeductions = repository.getCustomDeductions().first(),
            taxCode = repository.getTaxCode().first(),
            taxRegion = repository.getTaxRegion().first(),
            taxYear = repository.getTaxYear().first(),
            pensionRate = repository.getPensionRate().first(),
            hourlyRate = repository.getDefaultHourlyRate().first(),
            defaultHoursPerDay = repository.getDefaultHoursPerDay().first(),
            studentLoanPlan = repository.getStudentLoanPlan().first(),
            overtimeMultiplier = repository.getOvertimeMultiplier().first(),
            weekendOvertimeMultiplier = repository.getWeekendOvertimeMultiplier().first(),
            bankHolidayOvertimeMultiplier = repository.getBankHolidayOvertimeMultiplier().first(),
            hasMarriageAllowance = repository.getHasMarriageAllowance().first(),
            hasBlindPersonsAllowance = repository.getHasBlindPersonsAllowance().first(),
            activeProfileId = repository.getActiveProfileId().first(),
            annualShiftScheduleJson = repository.getAnnualShiftSchedule().first(),
            employerShiftAssignmentsJson = repository.getShiftEmployerAssignmentsJson().first(),
            payScheduleType = repository.getPayScheduleConfig().first().type.name,
            payScheduleFixedDay = repository.getPayScheduleConfig().first().fixedDay,
            payScheduleCutoffLeadDays = repository.getPayScheduleConfig().first().cutoffLeadDays,
            themeMode = repository.getThemeMode().first().name,
            themePalette = repository.getThemePalette().first().name,
            biometricLockEnabled = repository.getBiometricLockEnabled().first(),
            biometricHistoryLockEnabled = repository.getBiometricHistoryLockEnabled().first(),
            biometricTimeoutSeconds = repository.getBiometricTimeoutSeconds().first(),
            payrollContactEmail = repository.getPayrollContactEmail().first(),
            preferredCurrency = repository.getPreferredCurrency().first(),
            secondaryCurrency = repository.getSecondaryCurrency().first(),
            customEurRate = repository.getCustomEurRate().first(),
            customUsdRate = repository.getCustomUsdRate().first(),
            cachedCurrencyRatesJson = repository.getCachedCurrencyRatesJson().first(),
            syncthingServerUrl = repository.getSyncthingServerUrl().first(),
            syncthingApiKey = repository.getSyncthingApiKey().first(),
            syncthingFolderId = repository.getSyncthingFolderId().first(),
            syncthingAutoSync = repository.getSyncthingAutoSync().first(),
            customCloudEndpoint = repository.getCustomCloudEndpoint().first(),
            customCloudToken = repository.getCustomCloudToken().first(),
            autoCloudSyncEnabled = repository.getAutoCloudSyncEnabled().first(),
            autoBackupFrequency = repository.getAutoBackupFrequency().first().name,
            googleAccountEmail = repository.getGoogleAccountEmail().first(),
            googleDriveAutoSync = repository.getGoogleDriveAutoSync().first(),
            googleDriveLastSync = repository.getGoogleDriveLastSync().first(),
            annualLeaveHourlyRate = repository.getAnnualLeaveHourlyRate().first(),
            annualLeaveHoursPerDay = repository.getAnnualLeaveHoursPerDay().first(),
            weeklyOvertimeThreshold = repository.getWeeklyOvertimeThreshold().first(),
            deviceName = getDeviceName(),
            deviceModel = getDeviceModel()
        )
    }

    fun getDeviceName(): String {
        return try {
            val manufacturer = android.os.Build.MANUFACTURER ?: "Android"
            val model = android.os.Build.MODEL ?: "Device"
            val capitalizedManufacturer = manufacturer.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
            }
            if (model.startsWith(capitalizedManufacturer, ignoreCase = true)) {
                model
            } else {
                "$capitalizedManufacturer $model"
            }
        } catch (_: Exception) {
            "Android Device"
        }
    }

    fun getDeviceModel(): String {
        return try {
            android.os.Build.MODEL ?: "Device"
        } catch (_: Exception) {
            "Device"
        }
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
     * Parses and validates JSON backup content across modern and legacy backup schemas.
     */
    fun parseBackupJson(jsonString: String): Result<BackupBundle> {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Backup JSON cannot be empty"))
        }

        // 1. Try direct typed deserialization
        try {
            val bundle = json.decodeFromString<BackupBundle>(trimmed)
            return Result.success(bundle)
        } catch (_: Exception) {}

        // 2. Resilient fallback for older version exports or altered schemas
        return try {
            val root = json.parseToJsonElement(trimmed).jsonObject

            // Safely parse records
            val recordsList = mutableListOf<MonthlySalaryRecord>()
            root["records"]?.let { recElem ->
                if (recElem is kotlinx.serialization.json.JsonArray) {
                    for (item in recElem) {
                        try {
                            recordsList.add(json.decodeFromString<MonthlySalaryRecord>(item.toString()))
                        } catch (_: Exception) {
                            try {
                                val rObj = item.jsonObject
                                val days = rObj["daysWorked"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                                val hours = rObj["hoursPerDay"]?.jsonPrimitive?.doubleOrNull ?: 12.0
                                val rate = rObj["hourlyRate"]?.jsonPrimitive?.doubleOrNull ?: 12.71
                                val otHours = rObj["overtimeHours"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                                val otMult = rObj["overtimeMultiplier"]?.jsonPrimitive?.doubleOrNull
                                    ?: rObj["overtimeRateMultiplier"]?.jsonPrimitive?.doubleOrNull ?: 1.5
                                val gross = rObj["grossPay"]?.jsonPrimitive?.doubleOrNull
                                    ?: (days * hours * rate + otHours * rate * otMult)
                                val incTax = rObj["incomeTax"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                                val ni = rObj["nationalInsurance"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                                val net = rObj["netPay"]?.jsonPrimitive?.doubleOrNull ?: (gross - incTax - ni)

                                recordsList.add(
                                    MonthlySalaryRecord(
                                        id = rObj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString(),
                                        monthYear = rObj["monthYear"]?.jsonPrimitive?.content ?: "Unknown",
                                        timestamp = rObj["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
                                        daysWorked = days,
                                        hoursPerDay = hours,
                                        overtimeHours = otHours,
                                        overtimeMultiplier = otMult,
                                        hourlyRate = rate,
                                        grossPay = gross,
                                        salarySacrifice = rObj["salarySacrifice"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                                        pensionRate = rObj["pensionRate"]?.jsonPrimitive?.doubleOrNull ?: 5.0,
                                        pensionContribution = rObj["pensionContribution"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                                        employerPension = rObj["employerPension"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                                        taxablePay = rObj["taxablePay"]?.jsonPrimitive?.doubleOrNull ?: gross,
                                        incomeTax = incTax,
                                        nationalInsurance = ni,
                                        studentLoanPlan = rObj["studentLoanPlan"]?.jsonPrimitive?.content?.let {
                                            try { StudentLoanPlan.valueOf(it) } catch (_: Exception) { StudentLoanPlan.NONE }
                                        } ?: StudentLoanPlan.NONE,
                                        studentLoanDeduction = rObj["studentLoanDeduction"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                                        totalDeductions = rObj["totalDeductions"]?.jsonPrimitive?.doubleOrNull ?: (incTax + ni),
                                        netPay = net,
                                        taxCode = rObj["taxCode"]?.jsonPrimitive?.content ?: "1257L",
                                        taxRegion = rObj["taxRegion"]?.jsonPrimitive?.content?.let {
                                            try { TaxRegion.valueOf(it) } catch (_: Exception) { TaxRegion.UK_STANDARD }
                                        } ?: TaxRegion.UK_STANDARD,
                                        note = rObj["note"]?.jsonPrimitive?.content ?: ""
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            // Safely parse profiles
            val profilesList = mutableListOf<EmployerProfile>()
            root["profiles"]?.let { profElem ->
                if (profElem is kotlinx.serialization.json.JsonArray) {
                    for (item in profElem) {
                        try {
                            profilesList.add(json.decodeFromString<EmployerProfile>(item.toString()))
                        } catch (_: Exception) {}
                    }
                }
            }

            // Safely parse custom deductions
            val deductionsList = mutableListOf<CustomDeduction>()
            root["customDeductions"]?.let { dedElem ->
                if (dedElem is kotlinx.serialization.json.JsonArray) {
                    for (item in dedElem) {
                        try {
                            deductionsList.add(json.decodeFromString<CustomDeduction>(item.toString()))
                        } catch (_: Exception) {}
                    }
                }
            }

            val taxCode = root["taxCode"]?.jsonPrimitive?.content ?: "1257L"
            val taxRegion = root["taxRegion"]?.jsonPrimitive?.content?.let {
                try { TaxRegion.valueOf(it) } catch (_: Exception) { TaxRegion.UK_STANDARD }
            } ?: TaxRegion.UK_STANDARD

            val taxYear = root["taxYear"]?.jsonPrimitive?.content?.let {
                try { TaxYear.valueOf(it) } catch (_: Exception) { TaxYear.YEAR_2024_2025 }
            } ?: TaxYear.YEAR_2024_2025

            val pensionRate = root["pensionRate"]?.jsonPrimitive?.doubleOrNull ?: 5.0
            val hourlyRate = root["hourlyRate"]?.jsonPrimitive?.doubleOrNull ?: 12.71
            val defaultHours = root["defaultHoursPerDay"]?.jsonPrimitive?.doubleOrNull ?: 12.0

            val studentLoan = root["studentLoanPlan"]?.jsonPrimitive?.content?.let {
                try { StudentLoanPlan.valueOf(it) } catch (_: Exception) { StudentLoanPlan.NONE }
            } ?: StudentLoanPlan.NONE

            val otMult = root["overtimeMultiplier"]?.jsonPrimitive?.doubleOrNull ?: 1.5
            val marriage = root["hasMarriageAllowance"]?.jsonPrimitive?.booleanOrNull ?: false
            val blind = root["hasBlindPersonsAllowance"]?.jsonPrimitive?.booleanOrNull ?: false
            val activeProf = root["activeProfileId"]?.jsonPrimitive?.content

            // Shift Heatmap Data
            val shiftSchedule = when {
                root["annualShiftScheduleJson"]?.jsonPrimitive?.content?.isNotBlank() == true ->
                    root["annualShiftScheduleJson"]!!.jsonPrimitive.content
                root["annualShiftSchedule"] != null ->
                    root["annualShiftSchedule"].toString()
                else -> ""
            }

            val shiftAssignments = root["employerShiftAssignmentsJson"]?.jsonPrimitive?.content ?: ""
            val payScheduleType = root["payScheduleType"]?.jsonPrimitive?.content ?: "LAST_FRIDAY"
            val payScheduleFixedDay = root["payScheduleFixedDay"]?.jsonPrimitive?.intOrNull ?: 25
            val payScheduleCutoffLeadDays = root["payScheduleCutoffLeadDays"]?.jsonPrimitive?.intOrNull ?: 5
            val themeMode = root["themeMode"]?.jsonPrimitive?.content ?: "SYSTEM"
            val themePalette = root["themePalette"]?.jsonPrimitive?.content ?: "OCEAN"
            val prefCurrency = root["preferredCurrency"]?.jsonPrimitive?.content ?: "EUR"
            val secCurrency = root["secondaryCurrency"]?.jsonPrimitive?.content ?: "USD"
            val syncthingUrl = root["syncthingServerUrl"]?.jsonPrimitive?.content ?: "https://syncthing.449100.xyz"
            val syncthingKey = root["syncthingApiKey"]?.jsonPrimitive?.content ?: ""
            val syncthingFolder = root["syncthingFolderId"]?.jsonPrimitive?.content ?: "salary-calculator"
            val syncthingAuto = root["syncthingAutoSync"]?.jsonPrimitive?.booleanOrNull ?: false
            val customCloudEndpoint = root["customCloudEndpoint"]?.jsonPrimitive?.content ?: ""
            val customCloudToken = root["customCloudToken"]?.jsonPrimitive?.content ?: ""
            val autoCloudSync = root["autoCloudSyncEnabled"]?.jsonPrimitive?.booleanOrNull ?: false
            val autoFreq = root["autoBackupFrequency"]?.jsonPrimitive?.content ?: "DISABLED"
            val googleEmail = root["googleAccountEmail"]?.jsonPrimitive?.content ?: ""
            val googleAuto = root["googleDriveAutoSync"]?.jsonPrimitive?.booleanOrNull ?: true
            val googleLast = root["googleDriveLastSync"]?.jsonPrimitive?.content ?: ""
            val devName = root["deviceName"]?.jsonPrimitive?.content ?: ""
            val devModel = root["deviceModel"]?.jsonPrimitive?.content ?: ""
            val devId = root["deviceId"]?.jsonPrimitive?.content ?: ""

            val bundle = BackupBundle(
                version = root["version"]?.jsonPrimitive?.intOrNull ?: 1,
                timestamp = root["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
                records = recordsList,
                profiles = profilesList,
                customDeductions = deductionsList,
                taxCode = taxCode,
                taxRegion = taxRegion,
                taxYear = taxYear,
                pensionRate = pensionRate,
                hourlyRate = hourlyRate,
                defaultHoursPerDay = defaultHours,
                studentLoanPlan = studentLoan,
                overtimeMultiplier = otMult,
                hasMarriageAllowance = marriage,
                hasBlindPersonsAllowance = blind,
                activeProfileId = activeProf,
                annualShiftScheduleJson = shiftSchedule,
                employerShiftAssignmentsJson = shiftAssignments,
                payScheduleType = payScheduleType,
                payScheduleFixedDay = payScheduleFixedDay,
                payScheduleCutoffLeadDays = payScheduleCutoffLeadDays,
                themeMode = themeMode,
                themePalette = themePalette,
                preferredCurrency = prefCurrency,
                secondaryCurrency = secCurrency,
                syncthingServerUrl = syncthingUrl,
                syncthingApiKey = syncthingKey,
                syncthingFolderId = syncthingFolder,
                syncthingAutoSync = syncthingAuto,
                customCloudEndpoint = customCloudEndpoint,
                customCloudToken = customCloudToken,
                autoCloudSyncEnabled = autoCloudSync,
                autoBackupFrequency = autoFreq,
                googleAccountEmail = googleEmail,
                googleDriveAutoSync = googleAuto,
                googleDriveLastSync = googleLast,
                deviceName = devName,
                deviceModel = devModel,
                deviceId = devId
            )

            Result.success(bundle)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse backup JSON: ${e.localizedMessage}"))
        }
    }

    /**
     * Restores a BackupBundle into the active repository, restoring ALL settings,
     * employer profiles, records, shift schedules, and preferences.
     */
    suspend fun restoreBundle(
        repository: SalaryRepository,
        bundle: BackupBundle
    ) {
        AutoBackupObserver.setRestoring(true)
        try {
            // 1. Restore Core Tax & Wage Settings
            repository.setTaxCode(bundle.taxCode)
            repository.setTaxRegion(bundle.taxRegion)
            repository.setTaxYear(bundle.taxYear)
            repository.setPensionRate(bundle.pensionRate)
            repository.setDefaultHourlyRate(bundle.hourlyRate)
            if (bundle.defaultHoursPerDay > 0.0) {
                repository.setDefaultHoursPerDay(bundle.defaultHoursPerDay)
            }
            repository.setStudentLoanPlan(bundle.studentLoanPlan)
            if (bundle.overtimeMultiplier > 0.0) {
                repository.setOvertimeMultiplier(bundle.overtimeMultiplier)
            }
            if (bundle.weekendOvertimeMultiplier > 0.0) {
                repository.setWeekendOvertimeMultiplier(bundle.weekendOvertimeMultiplier)
            }
            if (bundle.bankHolidayOvertimeMultiplier > 0.0) {
                repository.setBankHolidayOvertimeMultiplier(bundle.bankHolidayOvertimeMultiplier)
            }
            repository.setHasMarriageAllowance(bundle.hasMarriageAllowance)
            repository.setHasBlindPersonsAllowance(bundle.hasBlindPersonsAllowance)
            if (bundle.annualLeaveHourlyRate >= 0.0) {
                repository.setAnnualLeaveHourlyRate(bundle.annualLeaveHourlyRate)
            }
            if (bundle.annualLeaveHoursPerDay > 0.0) {
                repository.setAnnualLeaveHoursPerDay(bundle.annualLeaveHoursPerDay)
            }
            if (bundle.weeklyOvertimeThreshold >= 0.0) {
                repository.setWeeklyOvertimeThreshold(bundle.weeklyOvertimeThreshold)
            }

            // 2. Restore Employer Profiles & Active Selection
            for (profile in bundle.profiles) {
                repository.saveEmployerProfile(profile)
            }
            if (bundle.activeProfileId != null) {
                repository.setActiveProfileId(bundle.activeProfileId)
            }

            // 3. Restore Custom Deductions
            for (deduction in bundle.customDeductions) {
                repository.saveCustomDeduction(deduction)
            }

            // 4. Restore Monthly Records
            for (record in bundle.records) {
                repository.saveSalaryRecord(record)
            }

            // 5. Restore Full Shift Heatmap Schedule & Shift Employer Mappings
            if (bundle.annualShiftScheduleJson.isNotBlank()) {
                repository.saveAnnualShiftScheduleJson(bundle.annualShiftScheduleJson)
            }
            if (bundle.employerShiftAssignmentsJson.isNotBlank()) {
                repository.saveShiftEmployerAssignmentsJson(bundle.employerShiftAssignmentsJson)
            }

            // 6. Restore Pay Schedule Configuration
            val restoredScheduleType = try {
                PayScheduleType.valueOf(bundle.payScheduleType)
            } catch (_: Exception) {
                PayScheduleType.LAST_FRIDAY_OF_MONTH
            }
            val currentConfig = repository.getPayScheduleConfig().first()
            repository.setPayScheduleConfig(
                currentConfig.copy(
                    type = restoredScheduleType,
                    fixedDay = if (bundle.payScheduleFixedDay in 1..31) bundle.payScheduleFixedDay else currentConfig.fixedDay,
                    cutoffLeadDays = if (bundle.payScheduleCutoffLeadDays >= 0) bundle.payScheduleCutoffLeadDays else currentConfig.cutoffLeadDays
                )
            )

            // 7. Restore UI Theme & Biometric Preferences
            if (bundle.themeMode.isNotBlank()) {
                try { repository.setThemeMode(ThemeMode.valueOf(bundle.themeMode)) } catch (_: Exception) {}
            }
            if (bundle.themePalette.isNotBlank()) {
                try { repository.setThemePalette(ThemePalette.valueOf(bundle.themePalette)) } catch (_: Exception) {}
            }
            repository.setBiometricLockEnabled(bundle.biometricLockEnabled)
            repository.setBiometricHistoryLockEnabled(bundle.biometricHistoryLockEnabled)
            if (bundle.biometricTimeoutSeconds >= 0) {
                repository.setBiometricTimeoutSeconds(bundle.biometricTimeoutSeconds)
            }

            // 8. Restore Contact & Currency Settings
            if (bundle.payrollContactEmail.isNotBlank()) {
                repository.setPayrollContactEmail(bundle.payrollContactEmail)
            }
            if (bundle.preferredCurrency.isNotBlank()) {
                repository.setPreferredCurrency(bundle.preferredCurrency)
            }
            if (bundle.secondaryCurrency.isNotBlank()) {
                repository.setSecondaryCurrency(bundle.secondaryCurrency)
            }
            if (bundle.customEurRate > 0.0) repository.setCustomEurRate(bundle.customEurRate)
            if (bundle.customUsdRate > 0.0) repository.setCustomUsdRate(bundle.customUsdRate)
            if (bundle.cachedCurrencyRatesJson.isNotBlank()) {
                repository.setCachedCurrencyRatesJson(bundle.cachedCurrencyRatesJson)
            }

            // 9. Restore Syncthing & Cloud Endpoints
            if (bundle.syncthingServerUrl.isNotBlank()) repository.setSyncthingServerUrl(bundle.syncthingServerUrl)
            if (bundle.syncthingApiKey.isNotBlank()) repository.setSyncthingApiKey(bundle.syncthingApiKey)
            if (bundle.syncthingFolderId.isNotBlank()) repository.setSyncthingFolderId(bundle.syncthingFolderId)
            repository.setSyncthingAutoSync(bundle.syncthingAutoSync)
            if (bundle.customCloudEndpoint.isNotBlank()) repository.setCustomCloudEndpoint(bundle.customCloudEndpoint)
            if (bundle.customCloudToken.isNotBlank()) repository.setCustomCloudToken(bundle.customCloudToken)
            repository.setAutoCloudSyncEnabled(bundle.autoCloudSyncEnabled)
            try {
                repository.setAutoBackupFrequency(AutoBackupFrequency.valueOf(bundle.autoBackupFrequency))
            } catch (_: Exception) {}
            if (bundle.googleAccountEmail.isNotBlank()) repository.setGoogleAccountEmail(bundle.googleAccountEmail)
            repository.setGoogleDriveAutoSync(bundle.googleDriveAutoSync)
            if (bundle.googleDriveLastSync.isNotBlank()) repository.setGoogleDriveLastSync(bundle.googleDriveLastSync)
        } finally {
            AutoBackupObserver.setRestoring(false)
        }
    }
}
