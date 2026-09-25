package com.example.salarycalculator.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Google Drive AppData Sync Engine
 * Manages Google Sign-In and REST API synchronization to Google Drive AppData (drive.appdata) folder.
 */
@Suppress("DEPRECATION")
object GoogleDriveSyncEngine {

    val DRIVE_APPDATA_SCOPE = Scope("https://www.googleapis.com/auth/drive.appdata")
    private const val DRIVE_FILE_NAME = "SalaryCalculator_Latest.json"
    private const val DRIVE_API_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * Builds GoogleSignInClient configured for Google Drive AppData folder access.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_APPDATA_SCOPE)
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
     * Searches for existing SalaryCalculator_Latest.json in Google Drive AppData folder.
     */
    suspend fun findRemoteBackupFileId(accessToken: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val query = java.net.URLEncoder.encode("name = '$DRIVE_FILE_NAME' and 'appDataFolder' in parents and trashed = false", "UTF-8")
            val targetUrl = "$DRIVE_API_FILES_URL?spaces=appDataFolder&q=$query&fields=files(id,name,modifiedTime,size)"
            val conn = URL(targetUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.1")

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
     * Uploads or updates the BackupBundle in Google Drive AppData folder.
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

            val existingFileId = findRemoteBackupFileId(token).getOrNull()
            val jsonContent = json.encodeToString(bundle)
            val jsonBytes = jsonContent.toByteArray(Charsets.UTF_8)

            if (existingFileId != null) {
                // Update existing file content: PATCH https://www.googleapis.com/upload/drive/v3/files/{fileId}?uploadType=media
                val patchUrl = "$DRIVE_UPLOAD_URL/$existingFileId?uploadType=media"
                val conn = URL(patchUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.1")
                conn.outputStream.use { it.write(jsonBytes) }

                val code = conn.responseCode
                if (code in 200..299) {
                    Result.success("Backup successfully synced to Google Drive AppData!")
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Result.failure(Exception("Drive upload failed ($code): $err"))
                }
            } else {
                // Create multipart upload: POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart
                val metadata = """
                    {"name":"$DRIVE_FILE_NAME","parents":["appDataFolder"],"mimeType":"application/json"}
                """.trimIndent()

                val boundary = "==============SalaryCalcDriveSyncBoundary=="
                val conn = URL("$DRIVE_UPLOAD_URL?uploadType=multipart").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.1")

                conn.outputStream.use { out ->
                    out.write(("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n").toByteArray(Charsets.UTF_8))
                    out.write(("--$boundary\r\nContent-Type: application/json\r\n\r\n").toByteArray(Charsets.UTF_8))
                    out.write(jsonBytes)
                    out.write(("\r\n--$boundary--\r\n").toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    Result.success("Backup file created in Google Drive AppData!")
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Result.failure(Exception("Drive creation failed ($code): $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads BackupBundle from Google Drive AppData folder.
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

            val fileId = findRemoteBackupFileId(token).getOrNull()
                ?: return@withContext Result.failure(Exception("No Salary Calculator backup found in Google Drive AppData."))

            val downloadUrl = "$DRIVE_API_FILES_URL/$fileId?alt=media"
            val conn = URL(downloadUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", "SalaryCalculator-Android/25.1")

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
