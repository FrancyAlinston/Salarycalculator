package com.example.salarycalculator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shift_events ORDER BY date ASC")
    fun getAllShifts(): Flow<List<ShiftEvent>>

    @Query("SELECT * FROM shift_events WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getShiftsBetweenDates(startDate: String, endDate: String): Flow<List<ShiftEvent>>
    
    @Query("SELECT * FROM shift_events WHERE date = :date")
    fun getShiftsByDate(date: String): Flow<List<ShiftEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShift(shift: ShiftEvent)

    @Update
    fun updateShift(shift: ShiftEvent)

    @Delete
    fun deleteShift(shift: ShiftEvent)
}
