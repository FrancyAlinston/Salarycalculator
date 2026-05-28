package com.example.salarycalculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val TAX_CODE = stringPreferencesKey("tax_code")
        val DEFAULT_HOURLY_RATE = doublePreferencesKey("default_hourly_rate")
    }

    val taxCodeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[TAX_CODE] ?: "1257L" // Default UK Tax Code
    }

    val defaultHourlyRateFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[DEFAULT_HOURLY_RATE] ?: 11.44 // UK National Living Wage as default example
    }

    suspend fun saveTaxCode(taxCode: String) {
        dataStore.edit { preferences ->
            preferences[TAX_CODE] = taxCode
        }
    }

    suspend fun saveDefaultHourlyRate(rate: Double) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_HOURLY_RATE] = rate
        }
    }
}
