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
}
