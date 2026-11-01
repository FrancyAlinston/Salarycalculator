package com.example.salarycalculator.domain

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Objects

/**
 * AutoBackupObserver
 * Observes all DataStore changes in real-time and automatically saves a complete BackupBundle
 * to the phone's Documents/SalaryCalculator directory (SalaryCalculator_Latest.json and timestamped snapshots)
 * so Syncthing immediately picks up and synchronizes every user change.
 */
object AutoBackupObserver {

    private var observerJob: Job? = null
    private var isRestoringOrInitializing: Boolean = false
    private var lastPayloadFingerprint: Int = 0

    fun setRestoring(restoring: Boolean) {
        isRestoringOrInitializing = restoring
    }

    fun updateFingerprint(bundle: BackupBundle) {
        lastPayloadFingerprint = computeFingerprint(bundle)
    }

    private fun computeFingerprint(bundle: BackupBundle): Int {
        return Objects.hash(
            bundle.records,
            bundle.profiles,
            bundle.customDeductions,
            bundle.taxCode,
            bundle.taxRegion,
            bundle.taxYear,
            bundle.hourlyRate,
            bundle.annualShiftScheduleJson,
            bundle.employerShiftAssignmentsJson,
            bundle.hasMarriageAllowance,
            bundle.hasBlindPersonsAllowance
        )
    }

    /**
     * Starts observing DataStore preference mutations. Debounces by 500ms to batch rapid user keystrokes.
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun startObserving(context: Context, scope: CoroutineScope) {
        if (observerJob != null && observerJob?.isActive == true) return

        val repository = SalaryRepository(context)

        observerJob = scope.launch(Dispatchers.IO) {
            context.dataStore.data
                .distinctUntilChanged()
                .debounce(500L)
                .collectLatest { _ ->
                    if (isRestoringOrInitializing) return@collectLatest

                    try {
                        // Create comprehensive snapshot of current state
                        val bundle = LedgerBackupManager.createFullBackupBundle(repository)

                        // Compute fingerprint of user data payload to ignore internal metadata changes
                        val currentFingerprint = computeFingerprint(bundle)
                        if (currentFingerprint == lastPayloadFingerprint) {
                            // No user payload changes; skip spurious trigger caused by internal sync metadata
                            return@collectLatest
                        }
                        lastPayloadFingerprint = currentFingerprint

                        // Retrieve Syncthing REST details if configured
                        val serverUrl = repository.getSyncthingServerUrl().first()
                        val apiKey = repository.getSyncthingApiKey().first()
                        val folderId = repository.getSyncthingFolderId().first()

                        // Export to local Syncthing directory (SalaryCalculator_Latest.json & timestamped backup)
                        SyncthingSyncEngine.exportBackup(
                            context = context,
                            bundle = bundle,
                            serverUrl = serverUrl,
                            apiKey = apiKey,
                            folderId = folderId
                        )

                        // If Google Drive sync is enabled and account is signed in, sync to Google Drive AppData
                        val googleAutoSync = repository.getGoogleDriveAutoSync().first()
                        if (googleAutoSync) {
                            val account = GoogleDriveSyncEngine.getSignedInAccount(context)
                            if (account != null) {
                                val driveRes = GoogleDriveSyncEngine.uploadBackupToDrive(context, account, bundle)
                                if (driveRes.isSuccess) {
                                    val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                    repository.setGoogleDriveLastSync(nowStr)
                                }
                            }
                        }

                        // NAS SQLite Backend & Cloudflare Web Sync
                        val configuredEndpoint = repository.getCustomCloudEndpoint().first()
                        val targetEndpoints = listOfNotNull(
                            configuredEndpoint.ifBlank { null },
                            "http://192.168.8.100:8085/api/backup",
                            "https://salary-calculator.449100.xyz/api/backup"
                        ).distinct()
                        val token = repository.getCustomCloudToken().first()

                        for (ep in targetEndpoints) {
                            try {
                                val pushRes = CustomCloudSyncManager.pushBackup(ep, token, bundle)
                                if (pushRes.isSuccess) {
                                    repository.setLastSyncedRemoteTimestamp(bundle.timestamp)
                                    break
                                }
                            } catch (_: Exception) {}
                        }

                        // Automatically synchronize updated shifts to connected Wear OS watch
                        try {
                            repository.syncShiftsToWearOs()
                        } catch (_: Exception) {}
                    } catch (_: Exception) {
                        // Resilient error handling - never crash UI thread
                    }
                }
        }
    }

    fun stopObserving() {
        observerJob?.cancel()
        observerJob = null
    }
}
