package com.example.salarycalculator.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SalaryRepository(private val context: Context) {

    private val TAX_CODE_KEY = stringPreferencesKey("tax_code")
    private val DEFAULT_HOURLY_RATE_KEY = doublePreferencesKey("default_hourly_rate")

    fun getTaxCode(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[TAX_CODE_KEY] ?: "1257L"
        }
    }

    suspend fun setTaxCode(taxCode: String) {
        context.dataStore.edit { preferences ->
            preferences[TAX_CODE_KEY] = taxCode
        }
    }

    fun getDefaultHourlyRate(): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[DEFAULT_HOURLY_RATE_KEY] ?: 12.71
        }
    }

    suspend fun setDefaultHourlyRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_HOURLY_RATE_KEY] = rate
        }
    }
}
