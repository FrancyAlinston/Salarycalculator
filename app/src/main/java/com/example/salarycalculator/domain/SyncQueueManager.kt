package com.example.salarycalculator.domain

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

@Serializable
enum class SyncItemType {
    TAX_PACK_ZIP,
    P60_PDF,
    SA100_PDF,
    CSV_EXPORT,
    FULL_DATA_BUNDLE
}

@Serializable
enum class SyncStatus {
    QUEUED,
    SYNCING,
    SUCCESS,
    FAILED
}

@Serializable
data class SyncQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val itemType: SyncItemType,
    val serverUrl: String,
    val username: String = "",
    val tokenOrPassword: String = "",
    val protocol: String = "WEBDAV",
    val addedTimestamp: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val status: SyncStatus = SyncStatus.QUEUED,
    val errorMessage: String? = null
)

class SyncQueueWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val queue = SyncQueueManager.loadQueue(applicationContext)
        val pending = queue.filter { it.status == SyncStatus.QUEUED || it.status == SyncStatus.FAILED }

        if (pending.isEmpty()) {
            return Result.success()
        }

        var allSucceeded = true

        for (item in pending) {
            val file = File(item.filePath)
            if (!file.exists()) {
                SyncQueueManager.updateItemStatus(applicationContext, item.id, SyncStatus.FAILED, "File not found locally")
                continue
            }

            SyncQueueManager.updateItemStatus(applicationContext, item.id, SyncStatus.SYNCING)

            val result = CloudDriveExporter.uploadFile(
                file = file,
                endpointUrl = item.serverUrl,
                username = item.username,
                passwordOrToken = item.tokenOrPassword,
                authType = item.protocol
            )

            if (result.isSuccess) {
                SyncQueueManager.updateItemStatus(applicationContext, item.id, SyncStatus.SUCCESS)
            } else {
                allSucceeded = false
                SyncQueueManager.updateItemStatus(
                    applicationContext,
                    item.id,
                    SyncStatus.FAILED,
                    result.message
                )
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }
}

object SyncQueueManager {

    private const val QUEUE_FILE_NAME = "sync_queue.json"
    private const val UNIQUE_WORK_NAME = "salary_sync_queue_worker"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val _queueState = MutableStateFlow<List<SyncQueueItem>>(emptyList())
    val queueState: Flow<List<SyncQueueItem>> = _queueState.asStateFlow()

    fun enqueue(context: Context, item: SyncQueueItem) {
        val current = loadQueue(context).toMutableList()
        current.add(item)
        saveQueue(context, current)
        scheduleWorker(context)
    }

    fun loadQueue(context: Context): List<SyncQueueItem> {
        val file = File(context.filesDir, QUEUE_FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText()
            val list = json.decodeFromString<List<SyncQueueItem>>(content)
            _queueState.value = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveQueue(context: Context, items: List<SyncQueueItem>) {
        val file = File(context.filesDir, QUEUE_FILE_NAME)
        try {
            file.writeText(json.encodeToString(items))
            _queueState.value = items
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateItemStatus(context: Context, id: String, status: SyncStatus, error: String? = null) {
        val current = loadQueue(context).map {
            if (it.id == id) {
                it.copy(
                    status = status,
                    attempts = it.attempts + 1,
                    errorMessage = error
                )
            } else it
        }
        saveQueue(context, current)
    }

    fun clearCompleted(context: Context) {
        val remaining = loadQueue(context).filter { it.status != SyncStatus.SUCCESS }
        saveQueue(context, remaining)
    }

    fun scheduleWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncQueueWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
