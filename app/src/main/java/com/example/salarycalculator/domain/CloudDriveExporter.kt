package com.example.salarycalculator.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

data class CloudUploadResult(
    val isSuccess: Boolean,
    val message: String,
    val statusCode: Int = 0,
    val remoteUrl: String? = null
)

object CloudDriveExporter {

    /**
     * Uploads a file directly to a Nextcloud / WebDAV / custom REST endpoint via HTTP PUT.
     */
    suspend fun uploadFile(
        file: File,
        endpointUrl: String,
        username: String? = null,
        passwordOrToken: String? = null,
        authType: String = "Bearer" // "Bearer" or "Basic"
    ): CloudUploadResult = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext CloudUploadResult(false, "Local file not found: ${file.name}")
        }

        try {
            // Append file name if endpoint is a folder
            val targetUrl = if (endpointUrl.endsWith("/")) "$endpointUrl${file.name}" else "$endpointUrl/${file.name}"
            val url = URL(targetUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000

                // Content Type
                val mimeType = when (file.extension.lowercase()) {
                    "zip" -> "application/zip"
                    "pdf" -> "application/pdf"
                    "csv" -> "text/csv"
                    "json" -> "application/json"
                    "ics" -> "text/calendar"
                    else -> "application/octet-stream"
                }
                setRequestProperty("Content-Type", mimeType)
                setRequestProperty("Content-Length", file.length().toString())

                // Auth
                if (!passwordOrToken.isNullOrBlank()) {
                    if (authType.equals("Basic", ignoreCase = true) && !username.isNullOrBlank()) {
                        val authStr = "$username:$passwordOrToken"
                        val encoded = Base64.getEncoder().encodeToString(authStr.toByteArray(Charsets.UTF_8))
                        setRequestProperty("Authorization", "Basic $encoded")
                    } else {
                        setRequestProperty("Authorization", "Bearer $passwordOrToken")
                    }
                }
            }

            val outputStream = connection.outputStream
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            val responseCode = connection.responseCode
            if (responseCode in 200..299 || responseCode == 201 || responseCode == 204) {
                CloudUploadResult(
                    isSuccess = true,
                    message = "Successfully uploaded ${file.name} to cloud drive (HTTP $responseCode).",
                    statusCode = responseCode,
                    remoteUrl = targetUrl
                )
            } else {
                CloudUploadResult(
                    isSuccess = false,
                    message = "Server returned HTTP error: $responseCode (${connection.responseMessage}).",
                    statusCode = responseCode
                )
            }
        } catch (e: Exception) {
            CloudUploadResult(
                isSuccess = false,
                message = "Cloud upload failed: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }
}
