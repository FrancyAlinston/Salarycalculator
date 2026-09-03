package com.example.salarycalculator.domain

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

enum class AutoBackupFrequency(val displayName: String, val intervalDays: Long) {
    DISABLED("Disabled", 0),
    WEEKLY("Weekly (7 Days)", 7),
    MONTHLY("Monthly (30 Days)", 30)
}

class ScheduledBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = SalaryRepository(applicationContext)
        val frequency = repository.getAutoBackupFrequency().first()

        if (frequency == AutoBackupFrequency.DISABLED) {
            return Result.success()
        }

        return try {
            val records = repository.getSalaryHistory().first()
            if (records.isNotEmpty()) {
                val backupDir = File(applicationContext.filesDir, "scheduled_backups")
                if (!backupDir.exists()) backupDir.mkdirs()

                val timestamp = System.currentTimeMillis()
                val zipFile = TaxPackZipExporter.createTaxPackZip(
                    context = applicationContext,
                    historyRecords = records,
                    taxYearLabel = "2024/2025"
                )

                val targetFile = File(backupDir, "backup_taxpack_${timestamp}.zip")
                zipFile.copyTo(targetFile, overwrite = true)

                // Enqueue to cloud sync if enabled
                val cloudEndpoint = repository.getCustomCloudEndpoint().first()
                if (cloudEndpoint.isNotBlank()) {
                    val queueItem = SyncQueueItem(
                        fileName = targetFile.name,
                        filePath = targetFile.absolutePath,
                        itemType = SyncItemType.TAX_PACK_ZIP,
                        serverUrl = cloudEndpoint
                    )
                    SyncQueueManager.enqueue(applicationContext, queueItem)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME = "salary_scheduled_backup_worker"

        fun scheduleBackup(context: Context, frequency: AutoBackupFrequency) {
            val workManager = WorkManager.getInstance(context)

            if (frequency == AutoBackupFrequency.DISABLED) {
                workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
                return
            }

            val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(
                frequency.intervalDays,
                TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
