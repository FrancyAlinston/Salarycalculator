package com.example.salarycalculator.domain

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

object WearDataSyncManager {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncShiftsToWearable(
        context: Context,
        shifts: Map<String, Double>,
        hourlyRate: Double = 16.25,
        taxCode: String = "1257L",
        standardHours: Double = 12.0
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val dataClient = Wearable.getDataClient(context)
            val putDataMapReq = PutDataMapRequest.create("/salary_sync/shifts").apply {
                dataMap.putString("shifts_json", json.encodeToString(shifts))
                dataMap.putDouble("hourly_rate", hourlyRate)
                dataMap.putString("tax_code", taxCode)
                dataMap.putDouble("standard_shift_hours", standardHours)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
            val request = putDataMapReq.asPutDataRequest().setUrgent()
            Tasks.await(dataClient.putDataItem(request), 3, TimeUnit.SECONDS)
            true
        } catch (_: Exception) {
            false
        }
    }
}
