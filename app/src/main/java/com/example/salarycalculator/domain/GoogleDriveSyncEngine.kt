package com.example.salarycalculator.domain

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class RemoteDeviceBackup(
    val fileId: String,
    val fileName: String,
    val deviceName: String,
    val deviceModel: String = "",
    val modifiedTime: String,
    val shiftCount: Int,
    val recordCount: Int,
    val hourlyRate: Double,
    val taxCode: String,
    val timestamp: Long,
    val isCurrentDevice: Boolean = false
)

/**
 * Google Drive AppData Sync Engine
 * Manages Google Sign-In, multi-device backup separation, and REST API synchronization
 * into the private Google Drive AppData (drive.appdata) folder.
 */
@Suppress("DEPRECATION")
object GoogleDriveSyncEngine {

    const val DRIVE_APPDATA_SCOPE_URI = "https://www.googleapis.com/auth/drive.appdata"
    val DRIVE_APPDATA_SCOPE by lazy { Scope(DRIVE_APPDATA_SCOPE_URI) }
    const val DRIVE_FILE_NAME = "SalaryCalculator_Latest.json"
    private const val DRIVE_API_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    const val WEB_CLIENT_ID = "35214185270-ua6im0rbukgail01ognhcnhb5hbbrb0v.apps.googleusercontent.com"

    fun getDeviceFileName(customDeviceName: String? = null): String {
        val name = customDeviceName?.ifBlank { null } ?: LedgerBackupManager.getDeviceName()
        val sanitized = name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "SalaryCalculator_Device_${sanitized}.json"
    }

    /**
     * Builds GoogleSignInClient configured for Google Drive AppData folder access.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_APPDATA_SCOPE)
            .requestIdToken(WEB_CLIENT_ID)
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Returns the currently signed-in Google account if available and authorized.
     */
    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, DRIVE_APPDATA_SCOPE)) {
            account
        } else {
            null
        }
    }

    /**
     * Fetches OAuth 2.0 access token asynchronously from GoogleAuthUtil.
     */
    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount): Result<String> = withContext(Dispatchers.IO) {
        try {
            val scopeStr = "oauth2:https://www.googleapis.com/auth/drive.appdata"
            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account.account!!, scopeStr)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Searches for existing file by name in Google Drive AppData folder.
     */
    suspend fun findRemoteBackupFileId(accessToken: String, fileName: String = DRIVE_FILE_NAME): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val query = java.net.URLEncoder.encode("name = '$fileName' and 'appDataFolder' in parents and trashed = false", "UTF-8")
            val targetUrl = "$DRIVE_API_FILES_URL?spaces=appDataFolder&q=$query&fields=files(id,name,modifiedTime,size)"
            val conn = URL(targetUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.2")

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(text).jsonObject
                val files = root["files"]?.jsonArray
                if (files != null && files.isNotEmpty()) {
                    val firstFile = files[0].jsonObject
                    val fileId = firstFile["id"]?.jsonPrimitive?.content
                    Result.success(fileId)
                } else {
                    Result.success(null)
                }
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Drive search query failed ($responseCode): $errText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists all device-specific and general backups stored in the user's Google Drive AppData folder.
     */
    suspend fun listRemoteDeviceBackups(
        context: Context,
        account: GoogleSignInAccount
    ): Result<List<RemoteDeviceBackup>> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getAccessToken(context, account)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Failed to get Google token"))
            }
            val token = tokenResult.getOrThrow()

            val query = java.net.URLEncoder.encode("'appDataFolder' in parents and trashed = false", "UTF-8")
            val targetUrl = "$DRIVE_API_FILES_URL?spaces=appDataFolder&q=$query&fields=files(id,name,modifiedTime,size)&pageSize=50"
            val conn = URL(targetUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.2")

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(text).jsonObject
                val files = root["files"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())

                val currentDeviceName = LedgerBackupManager.getDeviceName()
                val currentDeviceModel = LedgerBackupManager.getDeviceModel()
                val backups = mutableListOf<RemoteDeviceBackup>()

                for (fileElem in files) {
                    val fileObj = fileElem.jsonObject
                    val fileId = fileObj["id"]?.jsonPrimitive?.content ?: continue
                    val fileName = fileObj["name"]?.jsonPrimitive?.content ?: continue
                    val modifiedTimeRaw = fileObj["modifiedTime"]?.jsonPrimitive?.content ?: ""

                    // Only process SalaryCalculator json backups
                    if (!fileName.startsWith("SalaryCalculator") || !fileName.endsWith(".json")) continue

                    // Download content to inspect metadata
                    val downloadUrl = "$DRIVE_API_FILES_URL/$fileId?alt=media"
                    val dlConn = URL(downloadUrl).openConnection() as HttpURLConnection
                    dlConn.requestMethod = "GET"
                    dlConn.setRequestProperty("Authorization", "Bearer $token")
                    dlConn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.2")

                    if (dlConn.responseCode == 200) {
                        val jsonString = dlConn.inputStream.bufferedReader().use { it.readText() }
                        val parsedBundle = LedgerBackupManager.parseBackupJson(jsonString).getOrNull()
                        if (parsedBundle != null) {
                            val parsedShifts = SalaryRepository.parseShiftScheduleJson(parsedBundle.annualShiftScheduleJson)
                            val totalShifts = parsedShifts.values.sumOf { it.size }
                            val devName = parsedBundle.deviceName.ifBlank {
                                if (fileName == DRIVE_FILE_NAME) "Primary Cloud Backup"
                                else fileName.removePrefix("SalaryCalculator_Device_").removeSuffix(".json").replace("_", " ")
                            }

                            val isCurrent = (parsedBundle.deviceModel.isNotBlank() && parsedBundle.deviceModel.equals(currentDeviceModel, ignoreCase = true)) ||
                                    devName.equals(currentDeviceName, ignoreCase = true)

                            val formattedTime = if (parsedBundle.timestamp > 0) {
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(parsedBundle.timestamp))
                            } else modifiedTimeRaw

                            backups.add(
                                RemoteDeviceBackup(
                                    fileId = fileId,
                                    fileName = fileName,
                                    deviceName = devName,
                                    deviceModel = parsedBundle.deviceModel,
                                    modifiedTime = formattedTime,
                                    shiftCount = totalShifts,
                                    recordCount = parsedBundle.records.size,
                                    hourlyRate = parsedBundle.hourlyRate,
                                    taxCode = parsedBundle.taxCode,
                                    timestamp = parsedBundle.timestamp,
                                    isCurrentDevice = isCurrent
                                )
                            )
                        }
                    }
                }

                // Sort: current device first, then newest timestamp
                backups.sortWith(compareByDescending<RemoteDeviceBackup> { it.isCurrentDevice }.thenByDescending { it.timestamp })
                Result.success(backups)
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Drive query failed ($responseCode): $errText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadFileToDrive(
        token: String,
        fileName: String,
        jsonBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val existingFileId = findRemoteBackupFileId(token, fileName).getOrNull()

            if (existingFileId != null) {
                // PATCH update
                val patchUrl = "$DRIVE_UPLOAD_URL/$existingFileId?uploadType=media"
                val patchConn = URL(patchUrl).openConnection() as HttpURLConnection
                patchConn.requestMethod = "PATCH"
                patchConn.doOutput = true
                patchConn.setRequestProperty("Authorization", "Bearer $token")
                patchConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                patchConn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.2")
                patchConn.outputStream.use { it.write(jsonBytes) }

                val code = patchConn.responseCode
                if (code in 200..299) {
                    Result.success("Updated $fileName in Google Drive AppData")
                } else {
                    val err = patchConn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Result.failure(Exception("Drive upload failed ($code): $err"))
                }
            } else {
                // POST multipart create
                val metadata = """{"name":"$fileName","parents":["appDataFolder"],"mimeType":"application/json"}""".trimIndent()
                val boundary = "==============SalaryCalcDriveSyncBoundary=="
                val createConn = URL("$DRIVE_UPLOAD_URL?uploadType=multipart").openConnection() as HttpURLConnection
                createConn.requestMethod = "POST"
                createConn.doOutput = true
                createConn.setRequestProperty("Authorization", "Bearer $token")
                createConn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                createConn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.2")

                createConn.outputStream.use { out ->
                    out.write(("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n").toByteArray(Charsets.UTF_8))
                    out.write(("--$boundary\r\nContent-Type: application/json\r\n\r\n").toByteArray(Charsets.UTF_8))
                    out.write(jsonBytes)
                    out.write(("\r\n--$boundary--\r\n").toByteArray(Charsets.UTF_8))
                }

                val code = createConn.responseCode
                if (code in 200..299) {
                    Result.success("Created $fileName in Google Drive AppData")
                } else {
                    val err = createConn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Result.failure(Exception("Drive creation failed ($code): $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads or updates the BackupBundle in Google Drive AppData folder under the device-specific
     * file name as well as SalaryCalculator_Latest.json.
     */
    suspend fun uploadBackupToDrive(
        context: Context,
        account: GoogleSignInAccount,
        bundle: BackupBundle
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getAccessToken(context, account)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Failed to get Google token"))
            }
            val token = tokenResult.getOrThrow()

            val enrichedBundle = if (bundle.deviceName.isBlank()) {
                bundle.copy(
                    deviceName = LedgerBackupManager.getDeviceName(),
                    deviceModel = LedgerBackupManager.getDeviceModel()
                )
            } else bundle

            val jsonContent = json.encodeToString(enrichedBundle)
            val jsonBytes = jsonContent.toByteArray(Charsets.UTF_8)

            // 1. Upload device-specific backup (e.g. SalaryCalculator_Device_Google_Pixel_8_Pro.json)
            val deviceFileName = getDeviceFileName(enrichedBundle.deviceName)
            val devResult = uploadFileToDrive(token, deviceFileName, jsonBytes)

            // 2. Also update SalaryCalculator_Latest.json
            uploadFileToDrive(token, DRIVE_FILE_NAME, jsonBytes)

            devResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads specific BackupBundle by its Google Drive file ID.
     */
    suspend fun downloadBackupByFileId(
        context: Context,
        account: GoogleSignInAccount,
        fileId: String
    ): Result<BackupBundle> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getAccessToken(context, account)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Failed to get Google token"))
            }
            val token = tokenResult.getOrThrow()

            val downloadUrl = "$DRIVE_API_FILES_URL/$fileId?alt=media"
            val conn = URL(downloadUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.2")

            val code = conn.responseCode
            if (code == 200) {
                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                LedgerBackupManager.parseBackupJson(jsonString)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Drive download failed ($code): $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads BackupBundle from Google Drive AppData folder (default/latest).
     */
    suspend fun downloadBackupFromDrive(
        context: Context,
        account: GoogleSignInAccount
    ): Result<BackupBundle> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getAccessToken(context, account)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Failed to get Google token"))
            }
            val token = tokenResult.getOrThrow()

            val fileId = findRemoteBackupFileId(token, DRIVE_FILE_NAME).getOrNull()
                ?: return@withContext Result.failure(Exception("No Salary Calculator backup found in Google Drive AppData."))

            val downloadUrl = "$DRIVE_API_FILES_URL/$fileId?alt=media"
            val conn = URL(downloadUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.2")

            val code = conn.responseCode
            if (code == 200) {
                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                LedgerBackupManager.parseBackupJson(jsonString)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Drive download failed ($code): $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
