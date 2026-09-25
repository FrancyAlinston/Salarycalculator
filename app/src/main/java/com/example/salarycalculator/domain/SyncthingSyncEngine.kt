package com.example.salarycalculator.domain

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class SyncthingPingResult(
    val success: Boolean,
    val message: String,
    val latencyMs: Long = 0
)

@Serializable
data class SyncthingSystemStatus(
    val myId: String = "",
    val uptimeSeconds: Long = 0,
    val isOnline: Boolean = false,
    val rawJson: String = ""
)

@Serializable
data class SyncthingFolder(
    val id: String,
    val label: String,
    val path: String,
    val paused: Boolean = false
)

data class SyncthingBackupFile(
    val name: String,
    val file: File,
    val lastModified: Long,
    val sizeBytes: Long
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastModified))
    val formattedSize: String
        get() = "%.1f KB".format(sizeBytes / 1024.0)
}

/**
 * Syncthing Sync Engine
 * Provides REST API connectivity with remote Syncthing instances (e.g. syncthing.449100.xyz)
 * alongside automated local file storage for continuous folder synchronization.
 */
object SyncthingSyncEngine {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * Resolves the dedicated local directory where Syncthing monitors backups on the device.
     */
    fun getSyncthingDirectory(context: Context): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val salaryDir = File(docsDir, "SalaryCalculator")
        if (!salaryDir.exists()) {
            salaryDir.mkdirs()
        }
        return if (salaryDir.exists() && salaryDir.canWrite()) {
            salaryDir
        } else {
            val fallback = File(context.getExternalFilesDir(null), "SyncthingBackups")
            if (!fallback.exists()) fallback.mkdirs()
            fallback
        }
    }

    /**
     * Tests connectivity and authentication to Syncthing REST API (/rest/system/ping).
     */
    suspend fun testPing(serverUrl: String, apiKey: String): Result<SyncthingPingResult> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(serverUrl)
            if (cleanUrl.isBlank()) return@withContext Result.failure(IllegalArgumentException("Syncthing URL cannot be empty"))

            val startTime = System.currentTimeMillis()
            val targetUrl = "$cleanUrl/rest/system/ping"
            val connection = URL(targetUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.0")

            if (apiKey.isNotBlank()) {
                connection.setRequestProperty("X-API-Key", apiKey.trim())
            }

            val responseCode = connection.responseCode
            val latency = System.currentTimeMillis() - startTime

            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                if (responseText.contains("pong", ignoreCase = true)) {
                    Result.success(
                        SyncthingPingResult(
                            success = true,
                            message = "Connected to Syncthing! Response: Pong (${latency}ms)",
                            latencyMs = latency
                        )
                    )
                } else {
                    Result.success(
                        SyncthingPingResult(
                            success = true,
                            message = "Connected to Syncthing successfully (HTTP 200, ${latency}ms)",
                            latencyMs = latency
                        )
                    )
                }
            } else if (responseCode == 403 || responseCode == 401) {
                Result.failure(Exception("Authentication failed (HTTP $responseCode). Please verify your Syncthing API Key in Settings."))
            } else {
                Result.failure(Exception("Syncthing returned HTTP $responseCode: ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches remote Syncthing system status (/rest/system/status).
     */
    suspend fun fetchSystemStatus(serverUrl: String, apiKey: String): Result<SyncthingSystemStatus> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(serverUrl)
            val connection = URL("$cleanUrl/rest/system/status").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (apiKey.isNotBlank()) {
                connection.setRequestProperty("X-API-Key", apiKey.trim())
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = json.parseToJsonElement(responseText).jsonObject
                val myId = parsed["myID"]?.jsonPrimitive?.content ?: "Unknown"
                val uptime = parsed["uptime"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

                Result.success(
                    SyncthingSystemStatus(
                        myId = myId,
                        uptimeSeconds = uptime,
                        isOnline = true,
                        rawJson = responseText
                    )
                )
            } else {
                Result.failure(Exception("Status query failed (HTTP $responseCode)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches configured Syncthing folders (/rest/config/folders).
     */
    suspend fun fetchFolders(serverUrl: String, apiKey: String): Result<List<SyncthingFolder>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(serverUrl)
            val connection = URL("$cleanUrl/rest/config/folders").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (apiKey.isNotBlank()) {
                connection.setRequestProperty("X-API-Key", apiKey.trim())
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val array = json.parseToJsonElement(responseText).jsonArray
                val folders = array.map { element ->
                    val obj = element.jsonObject
                    SyncthingFolder(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        label = obj["label"]?.jsonPrimitive?.content ?: obj["id"]?.jsonPrimitive?.content ?: "Folder",
                        path = obj["path"]?.jsonPrimitive?.content ?: "",
                        paused = obj["paused"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    )
                }
                Result.success(folders)
            } else {
                Result.failure(Exception("Failed to fetch folders (HTTP $responseCode)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Triggers remote Syncthing to immediately scan the folder (/rest/db/scan).
     */
    suspend fun triggerFolderScan(serverUrl: String, apiKey: String, folderId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = sanitizeUrl(serverUrl)
            if (cleanUrl.isBlank() || apiKey.isBlank()) {
                return@withContext Result.success("Local backup saved (Remote scan skipped - missing URL or API key)")
            }

            val targetUrl = "$cleanUrl/rest/db/scan?folder=${folderId.trim()}"
            val connection = URL(targetUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("X-API-Key", apiKey.trim())

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success("Remote Syncthing scan triggered successfully (HTTP $responseCode)")
            } else {
                Result.failure(Exception("Remote scan request returned HTTP $responseCode: ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports the active salary ledger and preferences to the local Syncthing directory,
     * writing both a timestamped backup and a fixed "SalaryCalculator_Latest.json",
     * and optionally triggers remote Syncthing indexing.
     */
    suspend fun exportBackup(
        context: Context,
        bundle: BackupBundle,
        serverUrl: String = "",
        apiKey: String = "",
        folderId: String = "salary-calculator"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = getSyncthingDirectory(context)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val timestampedFile = File(dir, "SalaryCalculator_Backup_$timestamp.json")
            val latestFile = File(dir, "SalaryCalculator_Latest.json")

            val jsonString = json.encodeToString(bundle)

            timestampedFile.writeText(jsonString, Charsets.UTF_8)
            latestFile.writeText(jsonString, Charsets.UTF_8)

            // Optionally notify Syncthing server to scan
            if (serverUrl.isNotBlank() && apiKey.isNotBlank()) {
                try {
                    triggerFolderScan(serverUrl, apiKey, folderId)
                } catch (_: Exception) {}
            }

            Result.success(timestampedFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists available backup files inside the Syncthing sync directory.
     */
    fun listLocalBackups(context: Context): List<SyncthingBackupFile> {
        val dir = getSyncthingDirectory(context)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles { f -> f.isFile && f.name.endsWith(".json") && f.name.startsWith("SalaryCalculator_") }
            ?.map { f ->
                SyncthingBackupFile(
                    name = f.name,
                    file = f,
                    lastModified = f.lastModified(),
                    sizeBytes = f.length()
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    /**
     * Reads and decodes a BackupBundle from a given File.
     */
    fun loadBackupFromFile(file: File): Result<BackupBundle> {
        return try {
            if (!file.exists()) return Result.failure(IllegalArgumentException("File does not exist: ${file.absolutePath}"))
            val text = file.readText(Charsets.UTF_8)
            val bundle = json.decodeFromString<BackupBundle>(text)
            Result.success(bundle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeUrl(url: String): String {
        var clean = url.trim()
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length - 1)
        }
        return clean
    }
}
