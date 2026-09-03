package com.example.salarycalculator.domain

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ShiftRecord(
    val id: String = UUID.randomUUID().toString(),
    val startTimestamp: Long,
    val endTimestamp: Long,
    val breakMinutes: Int = 0,
    val note: String = ""
) {
    val durationHours: Double
        get() {
            val millis = maxOf(0L, endTimestamp - startTimestamp)
            val netMinutes = (millis / (1000 * 60)) - breakMinutes
            return maxOf(0.0, netMinutes / 60.0)
        }
}

@Serializable
data class ActiveShiftState(
    val isPunchActive: Boolean = false,
    val startTime: Long = 0L,
    val accumulatedDays: Double = 0.0,
    val accumulatedHours: Double = 0.0
)
