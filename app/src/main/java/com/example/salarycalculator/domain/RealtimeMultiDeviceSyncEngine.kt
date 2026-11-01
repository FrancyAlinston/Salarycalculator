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

    @Volatile
    private var lastLocalEditTimestamp: Long = 0L

    private const val DEFAULT_NAS_BACKUP_URL = "http://192.168.8.100:8085/api/backup"
    private const val DEFAULT_CLOUDFLARE_BACKUP_URL = "https://salary-calculator.449100.xyz/api/backup"

    /**
     * Marks the occurrence of a local user edit so older or stale remote revisions
     * are never pulled over active user modifications.
     */
    fun recordLocalEdit() {
        lastLocalEditTimestamp = System.currentTimeMillis()
    }

    fun getLastLocalEditTimestamp(): Long = lastLocalEditTimestamp

    /**
     * Normalizes seconds to milliseconds if required.
     */
    fun normalizeTimestamp(ts: Long): Long {
        return if (ts in 1L..9_999_999_999L) ts * 1000L else ts
    }

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
                    val rawRemoteTimestamp = fetchRemoteTimestamp(timestampUrl, token)
                    val remoteTimestamp = normalizeTimestamp(rawRemoteTimestamp)
                    android.util.Log.i("MultiDeviceSync", "Probe $endpoint: remoteTs=$remoteTimestamp, localSynced=$localLastSynced, lastLocalEdit=$lastLocalEditTimestamp")

                    val hasNoRecentLocalEdits = (System.currentTimeMillis() - lastLocalEditTimestamp) > 3000L

                    // If remote timestamp exists and is older than our local state, skip downloading only if no forced refresh
                    if (!force && !isLocalEmpty && remoteTimestamp > 0L) {
                        if (remoteTimestamp <= localLastSynced && !hasNoRecentLocalEdits) {
                            continue
                        }
                        if (remoteTimestamp <= lastLocalEditTimestamp) {
                            continue
                        }
                    }

                    // 2. Fetch full backup bundle
                    val pullRes = CustomCloudSyncManager.pullBackup(endpoint, token)
                    if (pullRes.isSuccess) {
                        val bundle = pullRes.getOrThrow()
                        val bundleTime = normalizeTimestamp(
                            if (bundle.timestamp > 0L) bundle.timestamp else remoteTimestamp
                        )

                        val myModel = LedgerBackupManager.getDeviceModel()
                        val isFromAnotherDevice = bundle.deviceModel.isNotBlank() && !bundle.deviceModel.equals(myModel, ignoreCase = true)
                        val contentChanged = bundle.annualShiftScheduleJson.isNotBlank() && bundle.annualShiftScheduleJson != localSchedule

                        val isNewer = (bundleTime > localLastSynced && bundleTime > lastLocalEditTimestamp)
                        val shouldApplyFromPeer = isFromAnotherDevice && contentChanged && hasNoRecentLocalEdits

                        val shouldRestore = force || isLocalEmpty || isNewer || shouldApplyFromPeer
                        android.util.Log.i("MultiDeviceSync", "Evaluated $endpoint: shouldRestore=$shouldRestore (isNewer=$isNewer, shouldApplyFromPeer=$shouldApplyFromPeer, fromAnother=$isFromAnotherDevice, contentChanged=$contentChanged)")

                        if (shouldRestore) {
                            LedgerBackupManager.restoreBundle(repository, bundle)
                            android.util.Log.i("MultiDeviceSync", "Restored bundle successfully from $endpoint! Device=${bundle.deviceModel}")
                            val finalTimestamp = if (bundleTime > 0L) bundleTime else System.currentTimeMillis()
                            repository.setLastSyncedRemoteTimestamp(finalTimestamp)

                            // Sync updated shifts down to connected Wear OS watch immediately
                            try {
                                repository.syncShiftsToWearOs()
                            } catch (_: Exception) {}

                            return@withContext true
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MultiDeviceSync", "Sync check failed on $endpoint: ${e.message}")
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
                val raw = root["created_at"]?.jsonPrimitive?.longOrNull
                    ?: (root["created_at"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toLong() ?: 0L)
                normalizeTimestamp(raw)
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Subscribes to NAS long-poll event for zero-latency change detection.
     */
    private fun longPollRemoteTimestamp(endpoint: String, token: String, sinceMs: Long, timeoutSec: Int = 15): Long {
        return try {
            if (!endpoint.contains(":8085") && !endpoint.contains("192.168.8.100")) return 0L
            val base = endpoint.substringBefore("/api/backup") + "/api/backup/poll"
            val pollUrl = "$base?since=$sinceMs&timeout=$timeoutSec"

            val conn = (URL(pollUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 4000
                readTimeout = (timeoutSec + 5) * 1000
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token")
                }
            }
            if (conn.responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                val root = json.parseToJsonElement(body).jsonObject
                val raw = root["created_at"]?.jsonPrimitive?.longOrNull
                    ?: (root["created_at"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toLong() ?: 0L)
                normalizeTimestamp(raw)
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Triggers an immediate asynchronous push of current state to NAS and Cloudflare.
     */
    fun triggerImmediatePush(repository: SalaryRepository, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch(Dispatchers.IO) {
            try {
                recordLocalEdit()
                val bundle = LedgerBackupManager.createFullBackupBundle(repository)
                val endpoints = getTargetEndpoints(repository)
                val token = try { repository.getCustomCloudToken().first() } catch (_: Exception) { "" }

                for (ep in endpoints) {
                    try {
                        val pushRes = CustomCloudSyncManager.pushBackup(ep, token, bundle)
                        if (pushRes.isSuccess) {
                            repository.setLastSyncedRemoteTimestamp(bundle.timestamp)
                            break
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Starts continuous auto-sync polling in foreground using long-polling on NAS
     * and fast polling on Cloudflare CDN.
     */
    fun startContinuousSync(context: Context, scope: CoroutineScope) {
        if (syncJob?.isActive == true) return
        val repository = SalaryRepository(context)

        syncJob = scope.launch(Dispatchers.IO) {
            // Initial eager check
            checkAndSyncRemoteChanges(context, repository, force = false)

            while (isActive) {
                try {
                    val endpoints = getTargetEndpoints(repository)
                    val nasEndpoint = endpoints.firstOrNull { it.contains(":8085") || it.contains("192.168.8.100") }
                    val token = try { repository.getCustomCloudToken().first() } catch (_: Exception) { "" }
                    val localSynced = repository.getLastSyncedRemoteTimestamp().first()

                    if (nasEndpoint != null) {
                        // Real-time long-poll wait
                        val remoteTs = longPollRemoteTimestamp(nasEndpoint, token, sinceMs = localSynced, timeoutSec = 12)
                        if (remoteTs > 0L && remoteTs > lastLocalEditTimestamp) {
                            checkAndSyncRemoteChanges(context, repository, force = false)
                        } else {
                            delay(1000L)
                        }
                    } else {
                        // Standard periodic check on remote networks
                        delay(4000L)
                        checkAndSyncRemoteChanges(context, repository, force = false)
                    }
                } catch (_: Exception) {
                    delay(3000L)
                }
            }
        }
    }

    fun stopContinuousSync() {
        syncJob?.cancel()
        syncJob = null
    }

    /**
     * Applies a shift update received from the Wear OS smartwatch into the phone's
     * annual schedule JSON, which triggers immediate upload to NAS and Cloudflare.
     */
    suspend fun applyWatchShiftUpdate(
        context: Context,
        repository: SalaryRepository,
        shiftsMap: Map<String, Double>
    ) = withContext(Dispatchers.IO) {
        if (shiftsMap.isEmpty()) return@withContext
        try {
            recordLocalEdit()
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
        } catch (_: Exception) {}
    }
}
