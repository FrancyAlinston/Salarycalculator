package com.example.salarycalculator.domain

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = SalaryRepository(applicationContext)
        val endpoint = repository.getCustomCloudEndpoint().first()
        val token = repository.getCustomCloudToken().first()

        if (endpoint.isBlank()) {
            return Result.success()
        }

        return try {
            val records = repository.getSalaryHistory().first()
            val profiles = repository.getEmployerProfiles().first()
            val deductions = repository.getCustomDeductions().first()
            val taxCode = repository.getTaxCode().first()
            val region = repository.getTaxRegion().first()
            val year = repository.getTaxYear().first()
            val pension = repository.getPensionRate().first()
            val rate = repository.getDefaultHourlyRate().first()
            val marriage = repository.getHasMarriageAllowance().first()
            val blind = repository.getHasBlindPersonsAllowance().first()

            val bundle = BackupBundle(
                records = records,
                profiles = profiles,
                customDeductions = deductions,
                taxCode = taxCode,
                taxRegion = region,
                taxYear = year,
                pensionRate = pension,
                hourlyRate = rate,
                hasMarriageAllowance = marriage,
                hasBlindPersonsAllowance = blind
            )

            val pushResult = CustomCloudSyncManager.pushBackup(endpoint, token, bundle)
            if (pushResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
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
                    .setRequiresBatteryNotLow(true)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<CloudSyncWorker>(24, TimeUnit.HOURS)
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
