package com.example.salarycalculator.domain

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * RealtimeMultiDeviceSyncEngine
 * Orchestrates bidirectional, continuous data synchronization across all connected
 * devices (multiple Android phones, Wear OS Samsung Galaxy Watch, and UGOS NAS SQLite / Cloudflare).
 */
object RealtimeMultiDeviceSyncEngine {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var syncJob: Job? = null
    private var isSyncing: Boolean = false

    private const val DEFAULT_NAS_BACKUP_URL = "http://192.168.8.100:8085/api/backup"
    private const val DEFAULT_CLOUDFLARE_BACKUP_URL = "https://salary-calculator.449100.xyz/api/backup"

    /**
     * Resolves all active target sync endpoints in priority order.
     */
    suspend fun getTargetEndpoints(repository: SalaryRepository): List<String> {
        val configured = try { repository.getCustomCloudEndpoint().first().trim() } catch (_: Exception) { "" }
        return listOfNotNull(
            configured.ifBlank { null },
            DEFAULT_NAS_BACKUP_URL,
            DEFAULT_CLOUDFLARE_BACKUP_URL
        ).distinct()
    }

    /**
     * Checks all remote endpoints for newer revisions and restores them if found.
     * Returns true if a newer revision was detected and restored.
     */
    suspend fun checkAndSyncRemoteChanges(
        context: Context,
        repository: SalaryRepository,
        force: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        if (isSyncing) return@withContext false
        isSyncing = true
        try {
            val localLastSynced = repository.getLastSyncedRemoteTimestamp().first()
            val localRecords = repository.getSalaryHistory().first()
            val localSchedule = repository.getAnnualShiftSchedule().first()
            val isLocalEmpty = localRecords.isEmpty() && localSchedule.isBlank()

            val endpoints = getTargetEndpoints(repository)
            val token = try { repository.getCustomCloudToken().first() } catch (_: Exception) { "" }

            for (endpoint in endpoints) {
                try {
                    // 1. First attempt lightweight timestamp probe
                    val timestampUrl = if (endpoint.endsWith("/api/backup")) "$endpoint/timestamp" else endpoint
                    val remoteTimestamp = fetchRemoteTimestamp(timestampUrl, token)

                    if (!force && !isLocalEmpty && remoteTimestamp > 0L && remoteTimestamp <= localLastSynced) {
                        // Already completely up to date with this endpoint
                        continue
                    }

                    // 2. Fetch full backup bundle
                    val pullRes = CustomCloudSyncManager.pullBackup(endpoint, token)
                    if (pullRes.isSuccess) {
                        val bundle = pullRes.getOrThrow()
                        val bundleTime = if (bundle.timestamp > 0L) bundle.timestamp else (remoteTimestamp * 1000L)

                        val isNewer = force || isLocalEmpty || bundleTime > (localLastSynced + 1000L)
                        val contentDifferent = bundle.annualShiftScheduleJson.isNotBlank() && bundle.annualShiftScheduleJson != localSchedule

                        if (isNewer || contentDifferent) {
                            LedgerBackupManager.restoreBundle(repository, bundle)
                            val finalTimestamp = if (bundleTime > 0L) bundleTime else System.currentTimeMillis()
                            repository.setLastSyncedRemoteTimestamp(finalTimestamp)

                            // Sync updated shifts down to connected Wear OS watch immediately
                            try {
                                repository.syncShiftsToWearOs()
                            } catch (_: Exception) {}

                            return@withContext true
                        }
                    }
                } catch (_: Exception) {
                    // Try next endpoint on network timeout
                }
            }
            false
        } finally {
            isSyncing = false
        }
    }

    /**
     * Probes remote timestamp endpoint quickly (timeout 2500ms).
     */
    private fun fetchRemoteTimestamp(urlStr: String, authToken: String): Long {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 2500
                readTimeout = 2500
                if (authToken.isNotBlank()) {
                    setRequestProperty("Authorization", if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken")
                }
            }
            if (conn.responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                val root = json.parseToJsonElement(body).jsonObject
                root["created_at"]?.jsonPrimitive?.longOrNull
                    ?: (root["created_at"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toLong() ?: 0L)
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Starts continuous auto-sync polling every 12 seconds in foreground.
     */
    fun startContinuousSync(context: Context, scope: CoroutineScope) {
        if (syncJob?.isActive == true) return
        val repository = SalaryRepository(context)

        syncJob = scope.launch(Dispatchers.IO) {
            // Initial eager check
            checkAndSyncRemoteChanges(context, repository, force = false)

            while (isActive) {
                delay(12000L) // Poll every 12 seconds
                try {
                    checkAndSyncRemoteChanges(context, repository, force = false)
                } catch (_: Exception) {}
            }
        }
    }

    fun stopContinuousSync() {
        syncJob?.cancel()
        syncJob = null
    }

    /**
     * Applies a shift update received from the Wear OS smartwatch into the phone's
     * annual schedule JSON, which triggers AutoBackupObserver to upload to NAS and Cloudflare.
     */
    suspend fun applyWatchShiftUpdate(
        context: Context,
        repository: SalaryRepository,
        shiftsMap: Map<String, Double>
    ) = withContext(Dispatchers.IO) {
        if (shiftsMap.isEmpty()) return@withContext
        try {
            val currentRaw = repository.getAnnualShiftSchedule().first()
            val parsedNested: MutableMap<String, MutableMap<Int, Double>> =
                SalaryRepository.parseShiftScheduleJson(currentRaw).mapValues { it.value.toMutableMap() }.toMutableMap()

            // Merge flat shifts (e.g. "2026-11-01" -> 12.0) into nested structure
            shiftsMap.forEach { (dateStr, hours) ->
                val parts = dateStr.split("-")
                if (parts.size >= 3) {
                    val y = parts[0].toIntOrNull() ?: return@forEach
                    val m = parts[1].toIntOrNull() ?: return@forEach
                    val d = parts[2].toIntOrNull() ?: return@forEach
                    val ym = "$y-$m"

                    val monthDays = parsedNested[ym] ?: mutableMapOf()
                    if (hours == 0.0) {
                        monthDays.remove(d)
                    } else {
                        monthDays[d] = hours
                    }
                    if (monthDays.isEmpty()) {
                        parsedNested.remove(ym)
                    } else {
                        parsedNested[ym] = monthDays
                    }
                }
            }

            val updatedJson = SalaryRepository.encodeShiftSchedule(parsedNested)
            repository.setAnnualShiftSchedule(updatedJson)

            // AutoBackupObserver will detect this DataStore change and push to NAS + Cloudflare!
        } catch (_: Exception) {}
    }
}
