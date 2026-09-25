package com.example.salarycalculator.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun BackupRestoreDialog(
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRestoring by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var parsedBundlePreview by remember { mutableStateOf<BackupBundle?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = BufferedReader(InputStreamReader(stream)).readText()
                    restoreJsonInput = content
                    val parseResult = LedgerBackupManager.parseBackupJson(content.trim())
                    if (parseResult.isSuccess) {
                        parsedBundlePreview = parseResult.getOrThrow()
                        isSuccess = true
                        statusMessage = "Backup file loaded successfully. Ready to restore!"
                    } else {
                        parsedBundlePreview = null
                        isSuccess = false
                        statusMessage = "Unable to parse backup file. Please verify JSON format."
                    }
                }
            } catch (e: Exception) {
                isSuccess = false
                statusMessage = "Error reading file: ${e.localizedMessage}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isRestoring) Icons.Default.Restore else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isRestoring) "Restore Ledger & Settings" else "Full Data Backup & Restore",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isRestoring) {
                    Text(
                        text = "Pick a saved .json backup file from device storage or paste the JSON text directly. All shift heatmap schedules, pay rates, employer profiles, and configurations will be restored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { filePickerLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup JSON File")
                    }

                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = {
                            restoreJsonInput = it
                            if (it.isNotBlank()) {
                                val parseResult = LedgerBackupManager.parseBackupJson(it.trim())
                                if (parseResult.isSuccess) {
                                    parsedBundlePreview = parseResult.getOrThrow()
                                    isSuccess = true
                                    statusMessage = "Valid JSON backup detected."
                                } else {
                                    parsedBundlePreview = null
                                }
                            }
                        },
                        label = { Text("Or Paste Backup JSON Data") },
                        placeholder = { Text("{\"version\":2, \"records\":[...], ...}") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        maxLines = 6
                    )

                    parsedBundlePreview?.let { bundle ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Backup Content Preview:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("• ${bundle.records.size} Salary Records", style = MaterialTheme.typography.bodySmall)
                                Text("• ${bundle.profiles.size} Employer Profiles", style = MaterialTheme.typography.bodySmall)
                                Text("• Tax Code: ${bundle.taxCode} (${bundle.taxRegion})", style = MaterialTheme.typography.bodySmall)
                                if (bundle.annualShiftScheduleJson.isNotBlank()) {
                                    Text("• Complete Annual Shift Heatmap Calendar Data Included", style = MaterialTheme.typography.bodySmall, color = Emerald60)
                                }
                                if (bundle.preferredCurrency.isNotBlank()) {
                                    Text("• Currencies: ${bundle.preferredCurrency} / ${bundle.secondaryCurrency}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Emerald60 else Rose60
                        )
                    }
                } else {
                    Text(
                        text = "Export a comprehensive JSON backup bundle containing your entire salary history, shift heatmap calendar schedule across all months/years, employer profiles, custom deductions, and all app settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Backup & Restore Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Button(
                                onClick = {
                                    scope.launch {
                                        val bundle = LedgerBackupManager.createFullBackupBundle(salaryRepository)
                                        val backupFile = LedgerBackupManager.createBackupFile(context, bundle)
                                        LedgerBackupManager.shareBackup(context, backupFile)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Full Comprehensive Backup")
                            }

                            OutlinedButton(
                                onClick = { isRestoring = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore from Backup (JSON / File)")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isRestoring) {
                Button(
                    onClick = {
                        val parseResult = LedgerBackupManager.parseBackupJson(restoreJsonInput.trim())
                        if (parseResult.isSuccess) {
                            val bundle = parseResult.getOrThrow()
                            scope.launch {
                                LedgerBackupManager.restoreBundle(salaryRepository, bundle)
                                isSuccess = true
                                statusMessage = "✓ Restored ${bundle.records.size} records, ${bundle.profiles.size} profiles, shift heatmap data & all settings!"
                            }
                        } else {
                            isSuccess = false
                            statusMessage = "Invalid backup format. Please check your JSON file or content."
                        }
                    },
                    enabled = restoreJsonInput.isNotBlank()
                ) {
                    Text("Apply Full Restore")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isRestoring) isRestoring = false else onDismiss()
                }
            ) {
                Text(if (isRestoring) "Back" else "Close")
            }
        }
    )
}
