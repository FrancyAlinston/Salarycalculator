package com.example.salarycalculator.domain

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * PhoneWearableListenerService
 * Listens for incoming shift modifications and rota updates originating directly from
 * the Samsung Galaxy Watch companion app via Google Play Services Wearable MessageClient.
 */
class PhoneWearableListenerService : WearableListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/salary_sync/watch_to_phone_update") {
            val payload = String(messageEvent.data, Charsets.UTF_8)
            scope.launch {
                try {
                    val shifts = json.decodeFromString<Map<String, Double>>(payload)
                    val repository = SalaryRepository(applicationContext)
                    RealtimeMultiDeviceSyncEngine.applyWatchShiftUpdate(applicationContext, repository, shifts)
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
