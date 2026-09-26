package com.example.salarycalculator.widget

import com.example.salarycalculator.R
import com.example.salarycalculator.domain.SalaryRepository

/**
 * Pure helper engine handling state cycling, labels, drawables, and notifications
 * for the interactive Home Screen Widget Tap-to-Log feature (FEAT-930).
 */
object ShiftCycleEngine {

    /**
     * Cycles through: Off (0.0) -> Standard (e.g. 12.0) -> Overtime (-12.0) -> Annual Leave (A/L) -> Sickness (SSP) -> Off (0.0)
     */
    fun getNextShiftState(currentHours: Double, standardHours: Double): Double {
        val effectiveStd = if (standardHours > 0.0) standardHours else 12.0
        return when {
            currentHours == 0.0 -> effectiveStd
            currentHours > 0.0 -> -currentHours
            currentHours == SalaryRepository.ANNUAL_LEAVE_HOURS -> SalaryRepository.STATUTORY_SICK_LEAVE_HOURS
            currentHours == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS -> 0.0
            currentHours < 0.0 -> SalaryRepository.ANNUAL_LEAVE_HOURS
            else -> 0.0
        }
    }

    /**
     * Formats compact status label for the quick widget pill.
     */
    fun getShiftStateLabel(hours: Double): String {
        return when {
            hours == SalaryRepository.ANNUAL_LEAVE_HOURS -> "Today: A/L"
            hours == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS -> "Today: SSP"
            hours < 0.0 -> {
                val ot = kotlin.math.abs(hours)
                val otStr = if (ot % 1.0 == 0.0) "${ot.toInt()}h" else "${ot}h"
                "Today: ${otStr} OT"
            }
            hours > 0.0 -> {
                val hrsStr = if (hours % 1.0 == 0.0) "${hours.toInt()}h" else "${hours}h"
                "Today: $hrsStr"
            }
            else -> "Today: Off"
        }
    }

    /**
     * Returns the matching background pill drawable resource.
     */
    fun getShiftPillBackgroundRes(hours: Double): Int {
        return when {
            hours == SalaryRepository.ANNUAL_LEAVE_HOURS -> R.drawable.widget_pill_blue_bg
            hours == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS -> R.drawable.widget_pill_purple_bg
            hours < 0.0 -> R.drawable.widget_pill_amber_bg
            hours > 0.0 -> R.drawable.widget_pill_emerald_bg
            else -> R.drawable.widget_pill_off_bg
        }
    }

    /**
     * User-facing confirmation message displayed upon tap.
     */
    fun getShiftConfirmationToast(hours: Double): String {
        return when {
            hours == SalaryRepository.ANNUAL_LEAVE_HOURS -> "Logged Today: Annual Leave (A/L)"
            hours == SalaryRepository.STATUTORY_SICK_LEAVE_HOURS -> "Logged Today: Statutory Sick Pay (SSP)"
            hours < 0.0 -> {
                val ot = kotlin.math.abs(hours)
                val otStr = if (ot % 1.0 == 0.0) "${ot.toInt()}h" else "${ot}h"
                "Logged Today: ${otStr} Overtime Shift"
            }
            hours > 0.0 -> {
                val hrsStr = if (hours % 1.0 == 0.0) "${hours.toInt()}h" else "${hours}h"
                "Logged Today: ${hrsStr} Duty Shift"
            }
            else -> "Logged Today: Day Off"
        }
    }
}
