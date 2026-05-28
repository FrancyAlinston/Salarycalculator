package com.example.salarycalculator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates")
    fun getAllTemplates(): Flow<List<ShiftTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTemplate(template: ShiftTemplate)

    @Update
    fun updateTemplate(template: ShiftTemplate)

    @Delete
    fun deleteTemplate(template: ShiftTemplate)
}
