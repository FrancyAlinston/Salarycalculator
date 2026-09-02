package com.example.salarycalculator.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

// CRITICAL: DATASTORE_PERSISTENCE
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SalaryRepository(private val context: Context) {

    private val TAX_CODE_KEY = stringPreferencesKey("tax_code")
    private val DEFAULT_HOURLY_RATE_KEY = doublePreferencesKey("default_hourly_rate")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val TAX_REGION_KEY = stringPreferencesKey("tax_region")
    private val PENSION_RATE_KEY = doublePreferencesKey("pension_rate")
    private val STUDENT_LOAN_PLAN_KEY = stringPreferencesKey("student_loan_plan")
    private val OVERTIME_MULTIPLIER_KEY = doublePreferencesKey("overtime_multiplier")

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
}
