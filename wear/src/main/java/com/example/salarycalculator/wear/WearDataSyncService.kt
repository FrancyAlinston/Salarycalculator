package com.example.salarycalculator.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WearDataSyncService : WearableListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var repository: WearSalaryRepository
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate() {
        super.onCreate()
        repository = WearSalaryRepository(applicationContext)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                if (uri.path == "/salary_sync/shifts") {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val shiftsJson = dataMap.getString("shifts_json", "{}")
                    val hourlyRate = dataMap.getDouble("hourly_rate", 16.25)
                    val taxCode = dataMap.getString("tax_code", "1257L")
                    val standardHours = dataMap.getDouble("standard_shift_hours", 12.0)

                    scope.launch {
                        try {
                            val map = json.decodeFromString<Map<String, Double>>(shiftsJson)
                            repository.updateAllShifts(map)
                            repository.setHourlyRate(hourlyRate)
                            repository.setTaxCode(taxCode)
                            repository.setStandardShiftHours(standardHours)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/salary_sync/quick_update") {
            val payload = String(messageEvent.data)
            scope.launch {
                try {
                    val map = json.decodeFromString<Map<String, Double>>(payload)
                    repository.updateAllShifts(map)
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
