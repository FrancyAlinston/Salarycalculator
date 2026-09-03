package com.example.salarycalculator.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CustomCloudSyncManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * Tests connectivity to a custom server/endpoint.
     */
    suspend fun testConnection(endpointUrl: String, authToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = endpointUrl.trim()
            if (cleanUrl.isBlank()) return@withContext Result.failure(IllegalArgumentException("Endpoint URL cannot be empty"))

            val url = URL(cleanUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (authToken.isNotBlank()) {
                connection.setRequestProperty("Authorization", if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299 || responseCode == 405) {
                Result.success("Connection successful! Server responded with HTTP $responseCode")
            } else {
                Result.failure(Exception("Server returned HTTP $responseCode: ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads/Pushes BackupBundle JSON to a custom endpoint via HTTP POST / PUT.
     */
    suspend fun pushBackup(
        endpointUrl: String,
        authToken: String,
        bundle: BackupBundle
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = endpointUrl.trim()
            if (cleanUrl.isBlank()) return@withContext Result.failure(IllegalArgumentException("Endpoint URL cannot be empty"))

            val jsonPayload = json.encodeToString(bundle)
            val url = URL(cleanUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (authToken.isNotBlank()) {
                connection.setRequestProperty("Authorization", if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken")
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success("Backup successfully uploaded to private domain! (HTTP $responseCode)")
            } else {
                Result.failure(Exception("Upload failed with HTTP $responseCode: ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads/Pulls BackupBundle JSON from a custom endpoint via HTTP GET.
     */
    suspend fun pullBackup(
        endpointUrl: String,
        authToken: String
    ): Result<BackupBundle> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = endpointUrl.trim()
            if (cleanUrl.isBlank()) return@withContext Result.failure(IllegalArgumentException("Endpoint URL cannot be empty"))

            val url = URL(cleanUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (authToken.isNotBlank()) {
                connection.setRequestProperty("Authorization", if (authToken.startsWith("Bearer ", ignoreCase = true)) authToken else "Bearer $authToken")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val responseString = reader.use { it.readText() }
                val bundle = json.decodeFromString<BackupBundle>(responseString)
                Result.success(bundle)
            } else {
                Result.failure(Exception("Download failed with HTTP $responseCode: ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
