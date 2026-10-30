package com.example.salarycalculator.wear.domain

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val Context.wearDataStore by preferencesDataStore(name = "wear_salary_prefs")

class WearSalaryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val HOURLY_RATE_KEY = doublePreferencesKey("wear_hourly_rate")
        val TAX_CODE_KEY = stringPreferencesKey("wear_tax_code")
        val STANDARD_SHIFT_HOURS_KEY = doublePreferencesKey("wear_standard_shift_hours")
        val PENSION_RATE_KEY = doublePreferencesKey("wear_pension_rate")
        val SHIFTS_JSON_KEY = stringPreferencesKey("wear_shifts_json")
        val TAX_REGION_KEY = stringPreferencesKey("wear_tax_region")
    }

    val hourlyRateFlow: Flow<Double> = context.wearDataStore.data.map { prefs ->
        prefs[HOURLY_RATE_KEY] ?: 16.25
    }

    val taxCodeFlow: Flow<String> = context.wearDataStore.data.map { prefs ->
        prefs[TAX_CODE_KEY] ?: "1257L"
    }

    val standardShiftHoursFlow: Flow<Double> = context.wearDataStore.data.map { prefs ->
        prefs[STANDARD_SHIFT_HOURS_KEY] ?: 12.0
    }

    val shiftsFlow: Flow<Map<String, Double>> = context.wearDataStore.data.map { prefs ->
        val raw = prefs[SHIFTS_JSON_KEY] ?: "{}"
        try {
            json.decodeFromString<Map<String, Double>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun setHourlyRate(rate: Double) {
        context.wearDataStore.edit { prefs ->
            prefs[HOURLY_RATE_KEY] = rate
        }
    }

    suspend fun setTaxCode(code: String) {
        context.wearDataStore.edit { prefs ->
            prefs[TAX_CODE_KEY] = code
        }
    }

    suspend fun setStandardShiftHours(hours: Double) {
        context.wearDataStore.edit { prefs ->
            prefs[STANDARD_SHIFT_HOURS_KEY] = hours
        }
    }

    suspend fun toggleShift(dateStr: String, standardHours: Double = 12.0) {
        context.wearDataStore.edit { prefs ->
            val raw = prefs[SHIFTS_JSON_KEY] ?: "{}"
            val currentMap: MutableMap<String, Double> = try {
                json.decodeFromString<Map<String, Double>>(raw).toMutableMap()
            } catch (_: Exception) {
                mutableMapOf()
            }

            val currentHours = currentMap[dateStr] ?: 0.0
            val nextHours = when {
                currentHours == 0.0 -> standardHours // Standard Shift (e.g. 12.0)
                currentHours > 0.0 -> -standardHours // Overtime Shift (e.g. -12.0)
                else -> 0.0 // Off (0.0)
            }

            if (nextHours == 0.0) {
                currentMap.remove(dateStr)
            } else {
                currentMap[dateStr] = nextHours
            }

            prefs[SHIFTS_JSON_KEY] = json.encodeToString(currentMap)
        }
    }

    suspend fun updateAllShifts(shifts: Map<String, Double>) {
        context.wearDataStore.edit { prefs ->
            prefs[SHIFTS_JSON_KEY] = json.encodeToString(shifts)
        }
    }

    suspend fun calculateMonthSalary(yearMonth: YearMonth): SalaryReport {
        val prefs = context.wearDataStore.data.first()
        val rate = prefs[HOURLY_RATE_KEY] ?: 16.25
        val taxCode = prefs[TAX_CODE_KEY] ?: "1257L"
        val pensionRate = prefs[PENSION_RATE_KEY] ?: 0.0
        val rawShifts = prefs[SHIFTS_JSON_KEY] ?: "{}"
        val shifts = try {
            json.decodeFromString<Map<String, Double>>(rawShifts)
        } catch (_: Exception) {
            emptyMap()
        }

        val prefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        var totalHours = 0.0
        var shiftCount = 0

        shifts.forEach { (date, hours) ->
            if (date.startsWith(prefix) && hours != 0.0) {
                shiftCount++
                totalHours += abs(hours)
            }
        }

        val grossPay = totalHours * rate
        return TaxCalculator.calculateTax(
            grossPay = grossPay,
            taxCode = taxCode,
            isMonthly = true,
            pensionRatePercent = pensionRate,
            totalHours = totalHours,
            shiftCount = shiftCount
        )
    }
}
