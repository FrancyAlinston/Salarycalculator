package com.example.salarycalculator.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromShiftType(value: ShiftType): String {
        return value.name
    }

    @TypeConverter
    fun toShiftType(value: String): ShiftType {
        return enumValueOf<ShiftType>(value)
    }
}
