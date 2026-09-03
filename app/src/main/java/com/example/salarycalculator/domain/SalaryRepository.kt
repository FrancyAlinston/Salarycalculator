package com.example.salarycalculator.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ThemePalette {
    OCEAN,
    EMERALD,
    VIOLET,
    AMBER
}

// CRITICAL: DATASTORE_PERSISTENCE
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SalaryRepository(private val context: Context) {

    private val TAX_CODE_KEY = stringPreferencesKey("tax_code")
    private val DEFAULT_HOURLY_RATE_KEY = doublePreferencesKey("default_hourly_rate")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val THEME_PALETTE_KEY = stringPreferencesKey("theme_palette")
    private val TAX_REGION_KEY = stringPreferencesKey("tax_region")
    private val TAX_YEAR_KEY = stringPreferencesKey("tax_year")
    private val PENSION_RATE_KEY = doublePreferencesKey("pension_rate")
    private val STUDENT_LOAN_PLAN_KEY = stringPreferencesKey("student_loan_plan")
    private val OVERTIME_MULTIPLIER_KEY = doublePreferencesKey("overtime_multiplier")
    private val MARRIAGE_ALLOWANCE_KEY = booleanPreferencesKey("has_marriage_allowance")
    private val BLIND_ALLOWANCE_KEY = booleanPreferencesKey("has_blind_persons_allowance")
    private val EMPLOYER_PROFILES_KEY = stringPreferencesKey("employer_profiles_json")
    private val ACTIVE_PROFILE_ID_KEY = stringPreferencesKey("active_profile_id")
    private val CUSTOM_DEDUCTIONS_KEY = stringPreferencesKey("custom_deductions_json")
    private val SALARY_HISTORY_KEY = stringPreferencesKey("salary_history_json")
    private val BIOMETRIC_LOCK_KEY = booleanPreferencesKey("biometric_lock_enabled")
    private val BIOMETRIC_HISTORY_LOCK_KEY = booleanPreferencesKey("biometric_history_lock_enabled")
    private val AUTO_BACKUP_FREQUENCY_KEY = stringPreferencesKey("auto_backup_frequency")
    private val ACTIVE_SHIFT_KEY = stringPreferencesKey("active_shift_state_json")
    private val CUSTOM_CLOUD_ENDPOINT_KEY = stringPreferencesKey("custom_cloud_endpoint")
    private val CUSTOM_CLOUD_TOKEN_KEY = stringPreferencesKey("custom_cloud_token")
    private val AUTO_CLOUD_SYNC_KEY = booleanPreferencesKey("auto_cloud_sync_enabled")
    private val CUSTOM_EUR_RATE_KEY = doublePreferencesKey("custom_eur_rate")
    private val CUSTOM_USD_RATE_KEY = doublePreferencesKey("custom_usd_rate")
    private val BIOMETRIC_TIMEOUT_KEY = longPreferencesKey("biometric_timeout_seconds")
    private val WEEKEND_OVERTIME_KEY = doublePreferencesKey("weekend_overtime_multiplier")
    private val BANK_HOLIDAY_OVERTIME_KEY = doublePreferencesKey("bank_holiday_overtime_multiplier")
    private val ANNUAL_SHIFT_SCHEDULE_KEY = stringPreferencesKey("annual_shift_schedule_json")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getAutoCloudSyncEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_CLOUD_SYNC_KEY] ?: false
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setAutoCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CLOUD_SYNC_KEY] = enabled
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getCustomEurRate(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_EUR_RATE_KEY] ?: ConvertedCurrencies.DEFAULT_EUR_RATE
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setCustomEurRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_EUR_RATE_KEY] = rate
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getCustomUsdRate(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_USD_RATE_KEY] ?: ConvertedCurrencies.DEFAULT_USD_RATE
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setCustomUsdRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_USD_RATE_KEY] = rate
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getBiometricTimeoutSeconds(): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[BIOMETRIC_TIMEOUT_KEY] ?: 0L // 0L = Immediate
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setBiometricTimeoutSeconds(seconds: Long) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_TIMEOUT_KEY] = seconds
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getCustomCloudEndpoint(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_CLOUD_ENDPOINT_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setCustomCloudEndpoint(url: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_CLOUD_ENDPOINT_KEY] = url
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getCustomCloudToken(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_CLOUD_TOKEN_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setCustomCloudToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_CLOUD_TOKEN_KEY] = token
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getBiometricLockEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BIOMETRIC_LOCK_KEY] ?: false
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_LOCK_KEY] = enabled
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getActiveShiftState(): Flow<ActiveShiftState> {
        return context.dataStore.data.map { preferences ->
            val jsonStr = preferences[ACTIVE_SHIFT_KEY] ?: ""
            if (jsonStr.isBlank()) {
                ActiveShiftState()
            } else {
                try {
                    json.decodeFromString<ActiveShiftState>(jsonStr)
                } catch (_: Exception) {
                    ActiveShiftState()
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun saveActiveShiftState(state: ActiveShiftState) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_SHIFT_KEY] = json.encodeToString(state)
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getTaxCode(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[TAX_CODE_KEY] ?: "1257L"
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setTaxCode(taxCode: String) {
        context.dataStore.edit { preferences ->
            preferences[TAX_CODE_KEY] = taxCode
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getDefaultHourlyRate(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[DEFAULT_HOURLY_RATE_KEY] ?: 12.71
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setDefaultHourlyRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_HOURLY_RATE_KEY] = rate
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getThemeMode(): Flow<ThemeMode> {
        return context.dataStore.data.map { preferences ->
            val modeStr = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(modeStr)
            } catch (_: Exception) {
                ThemeMode.SYSTEM
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getThemePalette(): Flow<ThemePalette> {
        return context.dataStore.data.map { preferences ->
            val paletteStr = preferences[THEME_PALETTE_KEY] ?: ThemePalette.OCEAN.name
            try {
                ThemePalette.valueOf(paletteStr)
            } catch (_: Exception) {
                ThemePalette.OCEAN
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setThemePalette(palette: ThemePalette) {
        context.dataStore.edit { preferences ->
            preferences[THEME_PALETTE_KEY] = palette.name
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getBiometricHistoryLockEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BIOMETRIC_HISTORY_LOCK_KEY] ?: false
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setBiometricHistoryLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_HISTORY_LOCK_KEY] = enabled
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getAutoBackupFrequency(): Flow<AutoBackupFrequency> {
        return context.dataStore.data.map { preferences ->
            val freqStr = preferences[AUTO_BACKUP_FREQUENCY_KEY] ?: AutoBackupFrequency.DISABLED.name
            try {
                AutoBackupFrequency.valueOf(freqStr)
            } catch (_: Exception) {
                AutoBackupFrequency.DISABLED
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_FREQUENCY_KEY] = frequency.name
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getTaxRegion(): Flow<TaxRegion> {
        return context.dataStore.data.map { preferences ->
            val regionStr = preferences[TAX_REGION_KEY] ?: TaxRegion.UK_STANDARD.name
            try {
                TaxRegion.valueOf(regionStr)
            } catch (_: Exception) {
                TaxRegion.UK_STANDARD
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setTaxRegion(region: TaxRegion) {
        context.dataStore.edit { preferences ->
            preferences[TAX_REGION_KEY] = region.name
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getTaxYear(): Flow<TaxYear> {
        return context.dataStore.data.map { preferences ->
            val yearStr = preferences[TAX_YEAR_KEY] ?: TaxYear.YEAR_2024_2025.name
            try {
                TaxYear.valueOf(yearStr)
            } catch (_: Exception) {
                TaxYear.YEAR_2024_2025
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setTaxYear(taxYear: TaxYear) {
        context.dataStore.edit { preferences ->
            preferences[TAX_YEAR_KEY] = taxYear.name
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getHasMarriageAllowance(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[MARRIAGE_ALLOWANCE_KEY] ?: false
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setHasMarriageAllowance(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MARRIAGE_ALLOWANCE_KEY] = enabled
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getHasBlindPersonsAllowance(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BLIND_ALLOWANCE_KEY] ?: false
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setHasBlindPersonsAllowance(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLIND_ALLOWANCE_KEY] = enabled
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getPensionRate(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[PENSION_RATE_KEY] ?: 5.0
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setPensionRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[PENSION_RATE_KEY] = rate
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getStudentLoanPlan(): Flow<StudentLoanPlan> {
        return context.dataStore.data.map { preferences ->
            val planStr = preferences[STUDENT_LOAN_PLAN_KEY] ?: StudentLoanPlan.NONE.name
            try {
                StudentLoanPlan.valueOf(planStr)
            } catch (_: Exception) {
                StudentLoanPlan.NONE
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setStudentLoanPlan(plan: StudentLoanPlan) {
        context.dataStore.edit { preferences ->
            preferences[STUDENT_LOAN_PLAN_KEY] = plan.name
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getOvertimeMultiplier(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[OVERTIME_MULTIPLIER_KEY] ?: 1.5
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setOvertimeMultiplier(multiplier: Double) {
        context.dataStore.edit { preferences ->
            preferences[OVERTIME_MULTIPLIER_KEY] = multiplier
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getWeekendOvertimeMultiplier(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[WEEKEND_OVERTIME_KEY] ?: 2.0
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setWeekendOvertimeMultiplier(multiplier: Double) {
        context.dataStore.edit { preferences ->
            preferences[WEEKEND_OVERTIME_KEY] = multiplier
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getBankHolidayOvertimeMultiplier(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[BANK_HOLIDAY_OVERTIME_KEY] ?: 2.5
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setBankHolidayOvertimeMultiplier(multiplier: Double) {
        context.dataStore.edit { preferences ->
            preferences[BANK_HOLIDAY_OVERTIME_KEY] = multiplier
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getEmployerProfiles(): Flow<List<EmployerProfile>> {
        return context.dataStore.data.map { preferences ->
            val jsonStr = preferences[EMPLOYER_PROFILES_KEY] ?: ""
            if (jsonStr.isBlank()) {
                listOf(
                    EmployerProfile(
                        id = "primary_default",
                        name = "Primary Employment",
                        employerName = "Main Employer",
                        taxCode = "1257L",
                        hourlyRate = 12.71,
                        isPrimary = true
                    )
                )
            } else {
                try {
                    json.decodeFromString<List<EmployerProfile>>(jsonStr)
                } catch (_: Exception) {
                    listOf(
                        EmployerProfile(
                            id = "primary_default",
                            name = "Primary Employment",
                            employerName = "Main Employer",
                            taxCode = "1257L",
                            hourlyRate = 12.71,
                            isPrimary = true
                        )
                    )
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun saveEmployerProfile(profile: EmployerProfile) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[EMPLOYER_PROFILES_KEY] ?: ""
            val currentList = if (jsonStr.isBlank()) {
                mutableListOf()
            } else {
                try {
                    json.decodeFromString<List<EmployerProfile>>(jsonStr).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }

            currentList.removeAll { it.id == profile.id }
            currentList.add(profile)
            preferences[EMPLOYER_PROFILES_KEY] = json.encodeToString(currentList)
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun deleteEmployerProfile(profileId: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[EMPLOYER_PROFILES_KEY] ?: ""
            if (jsonStr.isNotBlank()) {
                try {
                    val currentList = json.decodeFromString<List<EmployerProfile>>(jsonStr).toMutableList()
                    currentList.removeAll { it.id == profileId }
                    preferences[EMPLOYER_PROFILES_KEY] = json.encodeToString(currentList)
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getActiveProfileId(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[ACTIVE_PROFILE_ID_KEY] ?: "primary_default"
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setActiveProfileId(profileId: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_PROFILE_ID_KEY] = profileId
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getCustomDeductions(): Flow<List<CustomDeduction>> {
        return context.dataStore.data.map { preferences ->
            val jsonStr = preferences[CUSTOM_DEDUCTIONS_KEY] ?: ""
            if (jsonStr.isBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<CustomDeduction>>(jsonStr)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun saveCustomDeduction(deduction: CustomDeduction) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[CUSTOM_DEDUCTIONS_KEY] ?: ""
            val currentList = if (jsonStr.isBlank()) {
                mutableListOf()
            } else {
                try {
                    json.decodeFromString<List<CustomDeduction>>(jsonStr).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }

            currentList.removeAll { it.id == deduction.id }
            currentList.add(deduction)
            preferences[CUSTOM_DEDUCTIONS_KEY] = json.encodeToString(currentList)
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun deleteCustomDeduction(deductionId: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[CUSTOM_DEDUCTIONS_KEY] ?: ""
            if (jsonStr.isNotBlank()) {
                try {
                    val currentList = json.decodeFromString<List<CustomDeduction>>(jsonStr).toMutableList()
                    currentList.removeAll { it.id == deductionId }
                    preferences[CUSTOM_DEDUCTIONS_KEY] = json.encodeToString(currentList)
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSalaryHistory(): Flow<List<MonthlySalaryRecord>> {
        return context.dataStore.data.map { preferences ->
            val jsonStr = preferences[SALARY_HISTORY_KEY] ?: ""
            if (jsonStr.isBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<MonthlySalaryRecord>>(jsonStr)
                        .sortedByDescending { it.timestamp }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun saveSalaryRecord(record: MonthlySalaryRecord) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[SALARY_HISTORY_KEY] ?: ""
            val currentList = if (jsonStr.isBlank()) {
                mutableListOf()
            } else {
                try {
                    json.decodeFromString<List<MonthlySalaryRecord>>(jsonStr).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }

            currentList.removeAll { it.id == record.id || it.monthYear.equals(record.monthYear, ignoreCase = true) }
            currentList.add(0, record)

            val updatedJson = json.encodeToString(currentList)
            preferences[SALARY_HISTORY_KEY] = updatedJson
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun deleteSalaryRecord(recordId: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[SALARY_HISTORY_KEY] ?: ""
            if (jsonStr.isNotBlank()) {
                try {
                    val currentList = json.decodeFromString<List<MonthlySalaryRecord>>(jsonStr).toMutableList()
                    currentList.removeAll { it.id == recordId }
                    preferences[SALARY_HISTORY_KEY] = json.encodeToString(currentList)
                } catch (_: Exception) {
                    // Ignore decoding errors
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun clearSalaryHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(SALARY_HISTORY_KEY)
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getAnnualShiftSchedule(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[ANNUAL_SHIFT_SCHEDULE_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setAnnualShiftSchedule(scheduleJson: String) {
        context.dataStore.edit { preferences ->
            preferences[ANNUAL_SHIFT_SCHEDULE_KEY] = scheduleJson
        }
    }
}
