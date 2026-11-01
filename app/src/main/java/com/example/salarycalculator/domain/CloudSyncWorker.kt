package com.example.salarycalculator.domain

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * CloudSyncWorker
 * Background WorkManager periodic worker that keeps phone data synchronized across all devices
 * and the UGOS NAS SQLite / Cloudflare endpoints even when the app is closed.
 */
class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = SalaryRepository(applicationContext)
        return try {
            // 1. Pull and restore any remote changes first
            RealtimeMultiDeviceSyncEngine.checkAndSyncRemoteChanges(applicationContext, repository, force = false)

            // 2. Ensure current state is saved to remote backend
            val bundle = LedgerBackupManager.createFullBackupBundle(repository)
            val endpoints = RealtimeMultiDeviceSyncEngine.getTargetEndpoints(repository)
            val token = repository.getCustomCloudToken().first()

            for (ep in endpoints) {
                try {
                    val pushRes = CustomCloudSyncManager.pushBackup(ep, token, bundle)
                    if (pushRes.isSuccess) {
                        repository.setLastSyncedRemoteTimestamp(bundle.timestamp)
                        break
                    }
                } catch (_: Exception) {}
            }

            // 3. Keep connected watch updated
            try {
                repository.syncShiftsToWearOs()
            } catch (_: Exception) {}

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "salary_cloud_auto_sync"

        fun scheduleAutoSync(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<CloudSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    syncRequest
                )
            } else {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            }
        }
    }
}
