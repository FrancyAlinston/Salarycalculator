package com.example.salarycalculator.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object EmailExporter {

    /**
     * Launches email client intent with pre-filled subject, body summary, and file attachment.
     */
    fun dispatchEmailWithAttachment(
        context: Context,
        file: File,
        subject: String,
        bodyText: String,
        recipientEmail: String = ""
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (file.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "zip" -> "application/zip"
                "csv" -> "text/csv"
                else -> "*/*"
            }
            if (recipientEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyText)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Send Email via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
