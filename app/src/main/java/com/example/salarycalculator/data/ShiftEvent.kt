package com.example.salarycalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shifts")
data class ShiftEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    val templateId: String,
    val hours: Double,
    val hourlyRate: Double? = null,
    val notes: String = ""
)
