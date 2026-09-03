package com.example.salarycalculator.ui.history

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@Composable
fun CloudDriveExportDialog(
    historyRecords: List<MonthlySalaryRecord>,
    taxYearLabel: String = "2024/2025",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var endpointUrl by remember { mutableStateOf("https://cloud.example.com/remote.php/dav/files/user/Payroll/") }
    var authType by remember { mutableStateOf("Bearer") } // "Bearer" or "Basic"
    var username by remember { mutableStateOf("") }
    var tokenOrPassword by remember { mutableStateOf("") }
    var selectedFileType by remember { mutableStateOf("TaxPackZip") } // "TaxPackZip", "P60Pdf", "Csv"

    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Cloud Drive Direct Upload",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Directly upload your tax year documents to your private cloud storage (Nextcloud, ownCloud, WebDAV, Synology, or private REST API).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Document Selection
                Text("Select Document to Upload:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFileType == "TaxPackZip",
                        onClick = { selectedFileType = "TaxPackZip" },
                        label = { Text("Tax Pack (.zip)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedFileType == "P60Pdf",
                        onClick = { selectedFileType = "P60Pdf" },
                        label = { Text("P60 (.pdf)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedFileType == "Csv",
                        onClick = { selectedFileType = "Csv" },
                        label = { Text("Ledger (.csv)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Endpoint URL
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("Cloud Storage URL / WebDAV Path") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                )

                // Auth Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = authType == "Bearer",
                        onClick = { authType = "Bearer" },
                        label = { Text("Bearer Token / API Key") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = authType == "Basic",
                        onClick = { authType = "Basic" },
                        label = { Text("Basic (User + Pass)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (authType == "Basic") {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = tokenOrPassword,
                    onValueChange = { tokenOrPassword = it },
                    label = { Text(if (authType == "Bearer") "Bearer Token / Secret Key" else "Password") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                // Upload Feedback
                if (uploadStatusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSuccess) Emerald60.copy(alpha = 0.15f) else Rose60.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uploadStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) Emerald60 else Rose60,
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isUploading = true
                        uploadStatusMessage = null

                        // Generate target file
                        val targetFile: File = when (selectedFileType) {
                            "P60Pdf" -> P60Generator.generateP60Pdf(context, taxYearLabel, historyRecords)
                            "Csv" -> CsvSalaryExporter.exportHistoryCsv(context, historyRecords)
                            else -> TaxPackZipExporter.createTaxPackZip(
                                context = context,
                                historyRecords = historyRecords,
                                taxYearLabel = taxYearLabel,
                                year = Calendar.getInstance().get(Calendar.YEAR)
                            )
                        }

                        val result = CloudDriveExporter.uploadFile(
                            file = targetFile,
                            endpointUrl = endpointUrl,
                            username = username,
                            passwordOrToken = tokenOrPassword,
                            authType = authType
                        )

                        isUploading = false
                        isSuccess = result.isSuccess
                        uploadStatusMessage = result.message
                    }
                },
                enabled = !isUploading && endpointUrl.isNotBlank()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("Upload File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
