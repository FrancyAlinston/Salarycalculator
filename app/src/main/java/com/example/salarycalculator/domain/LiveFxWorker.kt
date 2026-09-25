package com.example.salarycalculator.domain

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LiveFxWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = SalaryRepository(applicationContext)
        val autoUpdate = repository.getAutoUpdateRates().first()

        if (!autoUpdate) {
            return Result.success()
        }

        return try {
            val liveRates = MultiCurrencyConverterEngine.fetchLiveRatesOnline()
            if (liveRates != null && liveRates.isNotEmpty()) {
                val timestamp = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date())
                repository.setCachedCurrencyRates(liveRates, timestamp)
                repository.setCachedCurrencyRatesJson(Json.encodeToString(liveRates))

                // Also update legacy EUR/USD rates if present
                liveRates["EUR"]?.let { repository.setCustomEurRate(it) }
                liveRates["USD"]?.let { repository.setCustomUsdRate(it) }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME = "salary_live_fx_sync_worker"
        private const val UNIQUE_ONE_TIME_WORK_NAME = "salary_live_fx_sync_onetime"

        fun schedulePeriodicSync(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<LiveFxWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runOneTimeSync(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<LiveFxWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
