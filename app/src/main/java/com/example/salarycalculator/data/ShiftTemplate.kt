package com.example.salarycalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "templates")
data class ShiftTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Long,
    val defaultHours: Double,
    val unpaidBreakHours: Double = 0.0,
    val defaultRate: Double? = null
)
