package com.example.salarycalculator.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    SOLAR
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
    private val PAY_SCHEDULE_TYPE_KEY = stringPreferencesKey("pay_schedule_type")
    private val PAY_SCHEDULE_FIXED_DAY_KEY = intPreferencesKey("pay_schedule_fixed_day")
    private val PAY_SCHEDULE_CUTOFF_LEAD_DAYS_KEY = intPreferencesKey("pay_schedule_cutoff_lead_days")
    private val DEFAULT_HOURS_PER_DAY_KEY = doublePreferencesKey("default_hours_per_day")
    private val EMPLOYER_SHIFT_ASSIGNMENTS_KEY = stringPreferencesKey("employer_shift_assignments_json")
    private val PAYROLL_CONTACT_EMAIL_KEY = stringPreferencesKey("payroll_contact_email")
    private val CACHED_CURRENCY_RATES_KEY = stringPreferencesKey("cached_currency_rates_json")
    private val CURRENCY_RATES_LAST_FETCH_KEY = stringPreferencesKey("currency_rates_last_fetch")
    private val SYNCTHING_SERVER_URL_KEY = stringPreferencesKey("syncthing_server_url")
    private val SYNCTHING_API_KEY_KEY = stringPreferencesKey("syncthing_api_key")
    private val SYNCTHING_FOLDER_ID_KEY = stringPreferencesKey("syncthing_folder_id")
    private val SYNCTHING_AUTO_SYNC_KEY = booleanPreferencesKey("syncthing_auto_sync")
    private val PREFERRED_CURRENCY_KEY = stringPreferencesKey("preferred_currency")
    private val SECONDARY_CURRENCY_KEY = stringPreferencesKey("secondary_currency")
    private val GOOGLE_ACCOUNT_EMAIL_KEY = stringPreferencesKey("google_account_email")
    private val GOOGLE_DRIVE_AUTO_SYNC_KEY = booleanPreferencesKey("google_drive_auto_sync")
    private val GOOGLE_DRIVE_LAST_SYNC_KEY = stringPreferencesKey("google_drive_last_sync")
    private val AUTO_UPDATE_RATES_KEY = booleanPreferencesKey("auto_update_rates")
    private val ANNUAL_LEAVE_HOURLY_RATE_KEY = doublePreferencesKey("annual_leave_hourly_rate")
    private val ANNUAL_LEAVE_HOURS_PER_DAY_KEY = doublePreferencesKey("annual_leave_hours_per_day")
    private val WEEKLY_OVERTIME_THRESHOLD_KEY = doublePreferencesKey("weekly_overtime_threshold")
    private val SSP_DAILY_RATE_KEY = doublePreferencesKey("ssp_daily_rate")
    private val SSP_WAITING_DAYS_KEY = intPreferencesKey("ssp_waiting_days")
    private val SELECTED_GOOGLE_CALENDAR_ID_KEY = longPreferencesKey("selected_google_calendar_id")
    private val DUTY_ALARM_TIMES_JSON_KEY = stringPreferencesKey("duty_alarm_times_json")
    private val DUTY_SHIFT_START_TIME_KEY = stringPreferencesKey("duty_shift_start_time")

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
            preferences[DEFAULT_HOURLY_RATE_KEY] ?: 12.82
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setDefaultHourlyRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_HOURLY_RATE_KEY] = rate
            // Synchronize with active/primary employer profile
            val jsonStr = preferences[EMPLOYER_PROFILES_KEY] ?: ""
            val currentList = if (jsonStr.isNotBlank()) {
                try {
                    json.decodeFromString<List<EmployerProfile>>(jsonStr).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            val activeId = preferences[ACTIVE_PROFILE_ID_KEY] ?: "primary_default"
            val idx = currentList.indexOfFirst { it.id == activeId }.let { if (it >= 0) it else currentList.indexOfFirst { p -> p.isPrimary } }
            if (idx >= 0) {
                currentList[idx] = currentList[idx].copy(hourlyRate = rate)
            } else {
                val defHours = preferences[DEFAULT_HOURS_PER_DAY_KEY] ?: 12.0
                currentList.add(
                    EmployerProfile(
                        id = activeId,
                        name = "Primary Employment",
                        employerName = "Main Employer",
                        taxCode = preferences[TAX_CODE_KEY] ?: "1257L",
                        hourlyRate = rate,
                        hoursPerDay = defHours,
                        isPrimary = true
                    )
                )
            }
            preferences[EMPLOYER_PROFILES_KEY] = json.encodeToString(currentList)
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
            preferences[PENSION_RATE_KEY] ?: 0.0
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
            val defRate = preferences[DEFAULT_HOURLY_RATE_KEY] ?: 12.82
            val defHours = preferences[DEFAULT_HOURS_PER_DAY_KEY] ?: 12.0
            if (jsonStr.isBlank()) {
                listOf(
                    EmployerProfile(
                        id = "primary_default",
                        name = "Primary Employment",
                        employerName = "Main Employer",
                        taxCode = "1257L",
                        hourlyRate = defRate,
                        hoursPerDay = defHours,
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
                            hourlyRate = defRate,
                            hoursPerDay = defHours,
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
            val parsed = parseShiftScheduleJson(scheduleJson)
            val cleanToSave = if (parsed.isNotEmpty()) encodeShiftSchedule(parsed) else scheduleJson
            preferences[ANNUAL_SHIFT_SCHEDULE_KEY] = cleanToSave
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getPayScheduleConfig(): Flow<PayScheduleConfig> {
        return context.dataStore.data.map { preferences ->
            val typeStr = preferences[PAY_SCHEDULE_TYPE_KEY] ?: PayScheduleType.LAST_FRIDAY_OF_MONTH.name
            val type = try {
                PayScheduleType.valueOf(typeStr)
            } catch (_: Exception) {
                PayScheduleType.LAST_FRIDAY_OF_MONTH
            }
            val fixedDay = preferences[PAY_SCHEDULE_FIXED_DAY_KEY] ?: 28
            val cutoffLeadDays = preferences[PAY_SCHEDULE_CUTOFF_LEAD_DAYS_KEY] ?: 5
            PayScheduleConfig(
                type = type,
                fixedDay = fixedDay,
                cutoffLeadDays = cutoffLeadDays
            )
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setPayScheduleConfig(config: PayScheduleConfig) {
        context.dataStore.edit { preferences ->
            preferences[PAY_SCHEDULE_TYPE_KEY] = config.type.name
            preferences[PAY_SCHEDULE_FIXED_DAY_KEY] = config.fixedDay
            preferences[PAY_SCHEDULE_CUTOFF_LEAD_DAYS_KEY] = config.cutoffLeadDays
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getDefaultHoursPerDay(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[DEFAULT_HOURS_PER_DAY_KEY] ?: 12.0
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setDefaultHoursPerDay(hours: Double) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_HOURS_PER_DAY_KEY] = hours
            // Synchronize with active/primary employer profile
            val jsonStr = preferences[EMPLOYER_PROFILES_KEY] ?: ""
            val currentList = if (jsonStr.isNotBlank()) {
                try {
                    json.decodeFromString<List<EmployerProfile>>(jsonStr).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            val activeId = preferences[ACTIVE_PROFILE_ID_KEY] ?: "primary_default"
            val idx = currentList.indexOfFirst { it.id == activeId }.let { if (it >= 0) it else currentList.indexOfFirst { p -> p.isPrimary } }
            if (idx >= 0) {
                currentList[idx] = currentList[idx].copy(hoursPerDay = hours)
            } else {
                val defRate = preferences[DEFAULT_HOURLY_RATE_KEY] ?: 12.82
                currentList.add(
                    EmployerProfile(
                        id = activeId,
                        name = "Primary Employment",
                        employerName = "Main Employer",
                        taxCode = preferences[TAX_CODE_KEY] ?: "1257L",
                        hourlyRate = defRate,
                        hoursPerDay = hours,
                        isPrimary = true
                    )
                )
            }
            preferences[EMPLOYER_PROFILES_KEY] = json.encodeToString(currentList)
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getMonthShiftSchedule(year: Int, month: Int): Flow<Map<Int, Double>> {
        return context.dataStore.data.map { preferences ->
            val jsonStr = preferences[ANNUAL_SHIFT_SCHEDULE_KEY] ?: ""
            val fullMap = parseShiftScheduleJson(jsonStr)
            fullMap["$year-$month"] ?: emptyMap()
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun saveMonthShiftSchedule(year: Int, month: Int, monthShifts: Map<Int, Double>) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[ANNUAL_SHIFT_SCHEDULE_KEY] ?: ""
            val currentMap = parseShiftScheduleJson(jsonStr).mapValues { it.value.toMutableMap() }.toMutableMap()
            val cleanMonth = monthShifts.filterValues { it != 0.0 }
            if (cleanMonth.isEmpty()) {
                currentMap.remove("$year-$month")
            } else {
                currentMap["$year-$month"] = cleanMonth.toMutableMap()
            }
            preferences[ANNUAL_SHIFT_SCHEDULE_KEY] = encodeShiftSchedule(currentMap)
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getShiftEmployerAssignments(): Flow<Map<String, String>> {
        return context.dataStore.data.map { preferences ->
            val jsonStr = preferences[EMPLOYER_SHIFT_ASSIGNMENTS_KEY] ?: ""
            if (jsonStr.isBlank()) {
                emptyMap()
            } else {
                try {
                    json.decodeFromString<Map<String, String>>(jsonStr)
                } catch (_: Exception) {
                    emptyMap()
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setShiftEmployerAssignment(dateKey: String, employerId: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[EMPLOYER_SHIFT_ASSIGNMENTS_KEY] ?: ""
            val currentMap = if (jsonStr.isBlank()) {
                mutableMapOf()
            } else {
                try {
                    json.decodeFromString<Map<String, String>>(jsonStr).toMutableMap()
                } catch (_: Exception) {
                    mutableMapOf()
                }
            }
            currentMap[dateKey] = employerId
            preferences[EMPLOYER_SHIFT_ASSIGNMENTS_KEY] = json.encodeToString(currentMap)
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun saveShiftEmployerAssignments(assignments: Map<String, String>) {
        context.dataStore.edit { preferences ->
            preferences[EMPLOYER_SHIFT_ASSIGNMENTS_KEY] = json.encodeToString(assignments)
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getPayrollContactEmail(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PAYROLL_CONTACT_EMAIL_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setPayrollContactEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[PAYROLL_CONTACT_EMAIL_KEY] = email.trim()
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getCachedCurrencyRates(): Flow<Map<String, Double>> {
        return context.dataStore.data.map { preferences ->
            val jsonStr = preferences[CACHED_CURRENCY_RATES_KEY] ?: ""
            if (jsonStr.isBlank()) {
                emptyMap()
            } else {
                try {
                    json.decodeFromString<Map<String, Double>>(jsonStr)
                } catch (_: Exception) {
                    emptyMap()
                }
            }
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setCachedCurrencyRates(rates: Map<String, Double>, lastFetchTimestamp: String) {
        context.dataStore.edit { preferences ->
            preferences[CACHED_CURRENCY_RATES_KEY] = json.encodeToString(rates)
            preferences[CURRENCY_RATES_LAST_FETCH_KEY] = lastFetchTimestamp
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getCurrencyRatesLastFetch(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CURRENCY_RATES_LAST_FETCH_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSyncthingServerUrl(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SYNCTHING_SERVER_URL_KEY] ?: "https://syncthing.449100.xyz"
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setSyncthingServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SYNCTHING_SERVER_URL_KEY] = url.trim()
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSyncthingApiKey(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SYNCTHING_API_KEY_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setSyncthingApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[SYNCTHING_API_KEY_KEY] = key.trim()
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSyncthingFolderId(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SYNCTHING_FOLDER_ID_KEY] ?: "salary-calculator"
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setSyncthingFolderId(folderId: String) {
        context.dataStore.edit { preferences ->
            preferences[SYNCTHING_FOLDER_ID_KEY] = folderId.trim()
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSyncthingAutoSync(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[SYNCTHING_AUTO_SYNC_KEY] ?: false
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setSyncthingAutoSync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SYNCTHING_AUTO_SYNC_KEY] = enabled
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getPreferredCurrency(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PREFERRED_CURRENCY_KEY] ?: "EUR"
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setPreferredCurrency(currencyCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_CURRENCY_KEY] = currencyCode.trim().uppercase()
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSecondaryCurrency(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SECONDARY_CURRENCY_KEY] ?: "USD"
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setSecondaryCurrency(currencyCode: String) {
        context.dataStore.edit { preferences ->
            preferences[SECONDARY_CURRENCY_KEY] = currencyCode.trim().uppercase()
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getAutoUpdateRates(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_UPDATE_RATES_KEY] ?: true
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setAutoUpdateRates(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_UPDATE_RATES_KEY] = enabled
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun saveAnnualShiftScheduleJson(jsonStr: String) {
        setAnnualShiftSchedule(jsonStr)
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getShiftEmployerAssignmentsJson(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[EMPLOYER_SHIFT_ASSIGNMENTS_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun saveShiftEmployerAssignmentsJson(jsonStr: String) {
        context.dataStore.edit { preferences ->
            preferences[EMPLOYER_SHIFT_ASSIGNMENTS_KEY] = jsonStr
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getCachedCurrencyRatesJson(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CACHED_CURRENCY_RATES_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setCachedCurrencyRatesJson(jsonStr: String) {
        context.dataStore.edit { preferences ->
            preferences[CACHED_CURRENCY_RATES_KEY] = jsonStr
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getGoogleAccountEmail(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[GOOGLE_ACCOUNT_EMAIL_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setGoogleAccountEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_ACCOUNT_EMAIL_KEY] = email.trim()
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getGoogleDriveAutoSync(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[GOOGLE_DRIVE_AUTO_SYNC_KEY] ?: true
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setGoogleDriveAutoSync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_DRIVE_AUTO_SYNC_KEY] = enabled
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getGoogleDriveLastSync(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[GOOGLE_DRIVE_LAST_SYNC_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setGoogleDriveLastSync(timestampStr: String) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_DRIVE_LAST_SYNC_KEY] = timestampStr
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getAnnualLeaveHourlyRate(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[ANNUAL_LEAVE_HOURLY_RATE_KEY] ?: 0.0 // 0.0 = Default to standard hourly rate
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setAnnualLeaveHourlyRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[ANNUAL_LEAVE_HOURLY_RATE_KEY] = rate
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getAnnualLeaveHoursPerDay(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[ANNUAL_LEAVE_HOURS_PER_DAY_KEY] ?: 0.0 // 0.0 = Default to standard hours per day
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setAnnualLeaveHoursPerDay(hours: Double) {
        context.dataStore.edit { preferences ->
            preferences[ANNUAL_LEAVE_HOURS_PER_DAY_KEY] = hours
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getWeeklyOvertimeThreshold(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[WEEKLY_OVERTIME_THRESHOLD_KEY] ?: 37.5 // Default 37.5h statutory standard
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setWeeklyOvertimeThreshold(threshold: Double) {
        context.dataStore.edit { preferences ->
            preferences[WEEKLY_OVERTIME_THRESHOLD_KEY] = threshold
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSspDailyRate(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[SSP_DAILY_RATE_KEY] ?: 23.35 // UK Statutory SSP: £116.75 / 5 days = £23.35/day
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setSspDailyRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[SSP_DAILY_RATE_KEY] = rate
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSspWaitingDays(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[SSP_WAITING_DAYS_KEY] ?: 3 // Standard statutory 3 waiting days
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setSspWaitingDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[SSP_WAITING_DAYS_KEY] = days
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getSelectedGoogleCalendarId(): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[SELECTED_GOOGLE_CALENDAR_ID_KEY] ?: -1L
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setSelectedGoogleCalendarId(id: Long) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_GOOGLE_CALENDAR_ID_KEY] = id
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getDutyAlarmTimesJson(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[DUTY_ALARM_TIMES_JSON_KEY] ?: ""
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setDutyAlarmTimesJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[DUTY_ALARM_TIMES_JSON_KEY] = json
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    fun getDutyShiftStartTime(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[DUTY_SHIFT_START_TIME_KEY] ?: "07:00"
        }
    }

    // CRITICAL: DATASTORE_PERSISTENCE
    suspend fun setDutyShiftStartTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[DUTY_SHIFT_START_TIME_KEY] = time
        }
    }

    companion object {
        const val ANNUAL_LEAVE_HOURS = -999.0
        const val STATUTORY_SICK_LEAVE_HOURS = -888.0
        fun parseShiftScheduleJson(jsonStr: String): Map<String, Map<Int, Double>> {
            if (jsonStr.isBlank()) return emptyMap()
            val result = mutableMapOf<String, MutableMap<Int, Double>>()
            val defaultYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

            try {
                val element = Json.parseToJsonElement(jsonStr)
                if (element is kotlinx.serialization.json.JsonObject) {
                    element.forEach { (key, value) ->
                        when {
                            // Case 1: Nested object -> {"2026-9": {"1": 12.0, "2": 12.0}} or {"9": {"1": 12.0}}
                            value is kotlinx.serialization.json.JsonObject -> {
                                val monthKey = if (key.contains("-")) {
                                    val parts = key.split("-")
                                    val y = parts.getOrNull(0)?.toIntOrNull() ?: defaultYear
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
                                    "$y-$m"
                                } else {
                                    val m = key.toIntOrNull() ?: 1
                                    "$defaultYear-$m"
                                }
                                val dayMap = result.getOrPut(monthKey) { mutableMapOf() }
                                value.forEach { (dStr, hrsElem) ->
                                    val d = dStr.toIntOrNull()
                                    val hrs = hrsElem.jsonPrimitive.doubleOrNull
                                    if (d != null && d in 1..31 && hrs != null && hrs != 0.0) {
                                        dayMap[d] = hrs
                                    }
                                }
                            }
                            // Case 2: Flat date key -> {"2026-8-1": 12.0} or {"2026-08-01": 12.0}
                            value is kotlinx.serialization.json.JsonPrimitive -> {
                                val hrs = value.doubleOrNull
                                if (hrs != null && hrs != 0.0) {
                                    val parts = key.split("-")
                                    if (parts.size >= 3) {
                                        val y = parts[0].toIntOrNull() ?: defaultYear
                                        val m = parts[1].toIntOrNull() ?: 1
                                        val d = parts[2].toIntOrNull() ?: 1
                                        if (d in 1..31) {
                                            val monthKey = "$y-$m"
                                            val dayMap = result.getOrPut(monthKey) { mutableMapOf() }
                                            dayMap[d] = hrs
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            return result.filterValues { it.isNotEmpty() }
        }

        fun encodeShiftSchedule(schedule: Map<String, Map<Int, Double>>): String {
            val exportMap = schedule.mapValues { (_, dayMap) ->
                dayMap.filterValues { it != 0.0 }.mapKeys { it.key.toString() }
            }.filterValues { it.isNotEmpty() }
            return Json.encodeToString(exportMap)
        }
    }
}
