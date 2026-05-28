package com.example.salarycalculator.domain

import com.example.salarycalculator.data.SettingsRepository
import com.example.salarycalculator.data.ShiftDao
import com.example.salarycalculator.data.ShiftEvent
import com.example.salarycalculator.data.TemplateDao
import com.example.salarycalculator.data.ShiftTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SalaryRepository(
    private val shiftDao: ShiftDao,
    private val templateDao: TemplateDao,
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

    fun getAllTemplates(): Flow<List<ShiftTemplate>> {
        return templateDao.getAllTemplates()
    }

    suspend fun addTemplate(template: ShiftTemplate) {
        withContext(Dispatchers.IO) {
            templateDao.insertTemplate(template)
        }
    }

    suspend fun updateTemplate(template: ShiftTemplate) {
        withContext(Dispatchers.IO) {
            templateDao.updateTemplate(template)
        }
    }

    suspend fun deleteTemplate(template: ShiftTemplate) {
        withContext(Dispatchers.IO) {
            templateDao.deleteTemplate(template)
        }
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
        val templatesFlow = templateDao.getAllTemplates()

        return combine(shiftsFlow, defaultRateFlow, taxCodeFlow, templatesFlow) { shifts, defaultRate, taxCode, templates ->
            var grossPay = 0.0
            var totalHours = 0.0
            
            val templateMap = templates.associateBy { it.id }
            val breakdownMap = mutableMapOf<String, TemplateEarnings>()

            for (shift in shifts) {
                val template = templateMap[shift.templateId]
                val templateName = template?.name ?: "Basic"
                // If a shift has no template or hours, fallback to 0. 
                val hours = shift.hours
                val rate = shift.hourlyRate ?: template?.defaultRate ?: defaultRate
                
                // Subtract unpaid break from hours if applicable
                val effectiveHours = maxOf(0.0, hours - (template?.unpaidBreakHours ?: 0.0))
                
                totalHours += effectiveHours
                val shiftAmount = effectiveHours * rate
                grossPay += shiftAmount

                val current = breakdownMap[templateName] ?: TemplateEarnings(templateName, 0.0, rate, 0.0)
                breakdownMap[templateName] = current.copy(
                    units = current.units + effectiveHours,
                    amount = current.amount + shiftAmount
                )
            }

            val report = TaxCalculator.calculateTax(grossPay, taxCode, isMonthly = true)
            report.copy(
                totalHours = totalHours,
                earningsBreakdown = breakdownMap.values.toList()
            )
        }
    }
}
