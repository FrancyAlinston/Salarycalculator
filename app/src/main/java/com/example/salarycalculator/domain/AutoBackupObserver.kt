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

/**
 * AutoBackupObserver
 * Observes all DataStore changes in real-time and automatically saves a complete BackupBundle
 * to the phone's Documents/SalaryCalculator directory (SalaryCalculator_Latest.json and timestamped snapshots)
 * so Syncthing immediately picks up and synchronizes every user change.
 */
object AutoBackupObserver {

    private var observerJob: Job? = null
    private var isRestoringOrInitializing: Boolean = false

    fun setRestoring(restoring: Boolean) {
        isRestoringOrInitializing = restoring
    }

    /**
     * Starts observing DataStore preference mutations. Debounces by 500ms to batch rapid user keystrokes.
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun startObserving(context: Context, scope: CoroutineScope) {
        if (observerJob != null && observerJob?.isActive == true) return

        val repository = SalaryRepository(context)

        observerJob = scope.launch(Dispatchers.IO) {
            // Observe the DataStore preferences Flow
            context.dataStore.data
                .distinctUntilChanged()
                .debounce(500L)
                .collectLatest { _ ->
                    if (isRestoringOrInitializing) return@collectLatest

                    try {
                        // Create comprehensive snapshot of current state
                        val bundle = LedgerBackupManager.createFullBackupBundle(repository)

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
