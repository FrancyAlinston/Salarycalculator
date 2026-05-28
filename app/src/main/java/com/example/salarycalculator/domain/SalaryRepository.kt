package com.example.salarycalculator.domain

import com.example.salarycalculator.data.SettingsRepository
import com.example.salarycalculator.data.ShiftDao
import com.example.salarycalculator.data.ShiftEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SalaryRepository(
    private val shiftDao: ShiftDao,
    private val settingsRepository: SettingsRepository
) {

    fun getShiftsBetweenDates(startDate: String, endDate: String): Flow<List<ShiftEvent>> {
        return shiftDao.getShiftsBetweenDates(startDate, endDate)
    }

    fun getAllShifts(): Flow<List<ShiftEvent>> {
        return shiftDao.getAllShifts()
    }
    
    fun getShiftsByDate(date: String): Flow<List<ShiftEvent>> {
        return shiftDao.getShiftsByDate(date)
    }

    suspend fun addShift(shift: ShiftEvent) {
        withContext(Dispatchers.IO) {
            shiftDao.insertShift(shift)
        }
    }
    
    suspend fun deleteShift(shift: ShiftEvent) {
        withContext(Dispatchers.IO) {
            shiftDao.deleteShift(shift)
        }
    }

    /**
     * Calculates the salary report for a given date range.
     * It observes shifts and settings, and dynamically calculates the total pay.
     */
    fun getSalaryReport(startDate: String, endDate: String): Flow<SalaryReport> {
        val shiftsFlow = shiftDao.getShiftsBetweenDates(startDate, endDate)
        val defaultRateFlow = settingsRepository.defaultHourlyRateFlow
        val taxCodeFlow = settingsRepository.taxCodeFlow

        return combine(shiftsFlow, defaultRateFlow, taxCodeFlow) { shifts, defaultRate, taxCode ->
            var grossPay = 0.0
            var totalHours = 0.0

            for (shift in shifts) {
                totalHours += shift.hours
                val rateToUse = shift.hourlyRate ?: defaultRate
                grossPay += (shift.hours * rateToUse)
            }

            // In a real app, you might want to determine if the date range spans a month or week
            // to adjust tax thresholds, but for simplicity we assume the user selects a 1-month period.
            val report = TaxCalculator.calculateTax(grossPay, taxCode, isMonthly = true)
            report.copy(totalHours = totalHours)
        }
    }
}
