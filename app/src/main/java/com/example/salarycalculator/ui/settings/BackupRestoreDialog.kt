package com.example.salarycalculator.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isRestoring) "Restore Ledger" else "Cloud & Data Backup",
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
                        text = "Paste the contents of your backup JSON file below to restore all monthly records, employer profiles, and settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        label = { Text("Backup JSON Data") },
                        placeholder = { Text("{\"version\":1, \"records\":[...], ...}") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )

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
                        text = "Export an encrypted/shareable JSON backup bundle containing your entire salary history, employer profiles, custom deductions, and settings to Google Drive, Files, or WhatsApp.",
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
                            Text("Backup Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Button(
                                onClick = {
                                    scope.launch {
                                        val records = salaryRepository.getSalaryHistory().first()
                                        val profiles = salaryRepository.getEmployerProfiles().first()
                                        val deductions = salaryRepository.getCustomDeductions().first()
                                        val taxCode = salaryRepository.getTaxCode().first()
                                        val region = salaryRepository.getTaxRegion().first()
                                        val year = salaryRepository.getTaxYear().first()
                                        val pension = salaryRepository.getPensionRate().first()
                                        val rate = salaryRepository.getDefaultHourlyRate().first()
                                        val marriage = salaryRepository.getHasMarriageAllowance().first()
                                        val blind = salaryRepository.getHasBlindPersonsAllowance().first()

                                        val bundle = BackupBundle(
                                            records = records,
                                            profiles = profiles,
                                            customDeductions = deductions,
                                            taxCode = taxCode,
                                            taxRegion = region,
                                            taxYear = year,
                                            pensionRate = pension,
                                            hourlyRate = rate,
                                            hasMarriageAllowance = marriage,
                                            hasBlindPersonsAllowance = blind
                                        )

                                        val backupFile = LedgerBackupManager.createBackupFile(context, bundle)
                                        LedgerBackupManager.shareBackup(context, backupFile)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Full Backup JSON")
                            }

                            OutlinedButton(
                                onClick = { isRestoring = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore from JSON")
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
                                statusMessage = "Successfully restored ${bundle.records.size} records & ${bundle.profiles.size} profiles!"
                            }
                        } else {
                            isSuccess = false
                            statusMessage = "Invalid backup format. Please check your JSON data."
                        }
                    }
                ) {
                    Text("Apply Restore")
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
