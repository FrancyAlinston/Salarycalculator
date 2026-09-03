package com.example.salarycalculator.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun CustomCloudSyncDialog(
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val savedEndpoint by salaryRepository.getCustomCloudEndpoint().collectAsState(initial = "")
    val savedToken by salaryRepository.getCustomCloudToken().collectAsState(initial = "")

    var endpointInput by remember(savedEndpoint) { mutableStateOf(savedEndpoint) }
    var tokenInput by remember(savedToken) { mutableStateOf(savedToken) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Private Cloud / Domain Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Connect to your self-hosted server, private domain (e.g. Nextcloud, WebDAV, or custom REST API) to sync encrypted payroll backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = endpointInput,
                    onValueChange = { endpointInput = it },
                    label = { Text("Server / API Endpoint URL") },
                    placeholder = { Text("https://cloud.mydomain.com/api/sync") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("API Key / Bearer Token (Optional)") },
                    placeholder = { Text("Bearer secret_token_xyz") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                statusMessage?.let { msg ->
                    Surface(
                        color = if (isSuccess) Emerald60.copy(alpha = 0.15f) else Rose60.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Emerald60 else Rose60,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Actions Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Test Connection
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                statusMessage = "Testing server connection..."
                                val result = CustomCloudSyncManager.testConnection(endpointInput, tokenInput)
                                isSuccess = result.isSuccess
                                statusMessage = result.getOrElse { it.localizedMessage ?: "Connection failed" }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading && endpointInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection")
                    }

                    // 2. Push Backup to Cloud
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                statusMessage = "Uploading backup bundle..."
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

                                val pushResult = CustomCloudSyncManager.pushBackup(endpointInput, tokenInput, bundle)
                                isSuccess = pushResult.isSuccess
                                statusMessage = pushResult.getOrElse { it.localizedMessage ?: "Upload failed" }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading && endpointInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Push Ledger to Cloud")
                    }

                    // 3. Pull Backup from Cloud
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                statusMessage = "Downloading backup from server..."
                                val pullResult = CustomCloudSyncManager.pullBackup(endpointInput, tokenInput)
                                if (pullResult.isSuccess) {
                                    val bundle = pullResult.getOrThrow()
                                    LedgerBackupManager.restoreBundle(salaryRepository, bundle)
                                    isSuccess = true
                                    statusMessage = "Restored ${bundle.records.size} records from private domain!"
                                } else {
                                    isSuccess = false
                                    statusMessage = pullResult.exceptionOrNull()?.localizedMessage ?: "Download failed"
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading && endpointInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pull & Restore from Cloud")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        salaryRepository.setCustomCloudEndpoint(endpointInput.trim())
                        salaryRepository.setCustomCloudToken(tokenInput.trim())
                        onDismiss()
                    }
                }
            ) {
                Text("Save & Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
