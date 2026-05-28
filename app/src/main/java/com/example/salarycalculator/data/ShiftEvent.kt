package com.example.salarycalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "shift_events")
data class ShiftEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // Stored as ISO string (e.g., "2024-05-28")
    val type: ShiftType,
    val hours: Double,
    val hourlyRate: Double? = null // Null means use default rate from settings
)

enum class ShiftType {
    WORKING_DAY,
    ANNUAL_LEAVE,
    OVERTIME,
    CUSTOM_SHIFT
}
