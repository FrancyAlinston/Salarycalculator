package com.example.salarycalculator.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SyncthingSyncDialog(
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedUrl by salaryRepository.getSyncthingServerUrl().collectAsState(initial = "https://syncthing.449100.xyz")
    val savedApiKey by salaryRepository.getSyncthingApiKey().collectAsState(initial = "")
    val savedFolderId by salaryRepository.getSyncthingFolderId().collectAsState(initial = "salary-calculator")
    val savedAutoSync by salaryRepository.getSyncthingAutoSync().collectAsState(initial = false)

    var serverUrlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var folderIdInput by remember(savedFolderId) { mutableStateOf(savedFolderId) }
    var autoSyncEnabled by remember(savedAutoSync) { mutableStateOf(savedAutoSync) }
    var showApiKey by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showHelpGuide by remember { mutableStateOf(false) }
    var localBackups by remember { mutableStateOf<List<SyncthingBackupFile>>(emptyList()) }
    var showBackupsList by remember { mutableStateOf(false) }

    val localSyncDir = remember { SyncthingSyncEngine.getSyncthingDirectory(context) }

    LaunchedEffect(Unit) {
        localBackups = SyncthingSyncEngine.listLocalBackups(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Syncthing Continuous Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    text = "Seamlessly synchronize encrypted payroll ledgers, rota shifts, and tax settings with your NAS Syncthing instance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Syncthing Directory Badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local Syncthing Folder", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(localSyncDir.absolutePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                }

                // Syncthing Server URL
                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    label = { Text("Syncthing Server / Domain URL") },
                    placeholder = { Text("https://syncthing.449100.xyz") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                // Syncthing API Key
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Syncthing API Key") },
                    placeholder = { Text("Enter API key from Syncthing GUI") },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Hide API Key" else "Show API Key",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                // Syncthing Target Folder ID
                OutlinedTextField(
                    value = folderIdInput,
                    onValueChange = { folderIdInput = it },
                    label = { Text("Syncthing Folder ID (Optional)") },
                    placeholder = { Text("salary-calculator") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                // Status Message Banner
                statusMessage?.let { msg ->
                    Surface(
                        color = if (isSuccess) Emerald60.copy(alpha = 0.15f) else Rose60.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccess) Emerald60 else Rose60,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSuccess) Emerald60 else Rose60
                            )
                        }
                    }
                }

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Test Connection
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                statusMessage = "Testing Syncthing REST API..."
                                val result = SyncthingSyncEngine.testPing(serverUrlInput, apiKeyInput)
                                isSuccess = result.isSuccess
                                statusMessage = result.map { it.message }.getOrElse { it.localizedMessage ?: "Connection failed" }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading && serverUrlInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Syncthing Connection")
                    }

                    // 2. Export & Sync Now
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                statusMessage = "Generating encrypted backup bundle..."

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

                                val exportResult = SyncthingSyncEngine.exportBackup(
                                    context = context,
                                    bundle = bundle,
                                    serverUrl = serverUrlInput,
                                    apiKey = apiKeyInput,
                                    folderId = folderIdInput.ifBlank { "salary-calculator" }
                                )

                                isSuccess = exportResult.isSuccess
                                if (exportResult.isSuccess) {
                                    val file = exportResult.getOrThrow()
                                    localBackups = SyncthingSyncEngine.listLocalBackups(context)
                                    statusMessage = "Backup saved to Syncthing folder (${file.name})! Remote scan triggered."
                                } else {
                                    statusMessage = "Export failed: ${exportResult.exceptionOrNull()?.localizedMessage}"
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync to Syncthing Folder Now")
                    }

                    // 3. View / Restore from Syncthing Backups
                    OutlinedButton(
                        onClick = {
                            localBackups = SyncthingSyncEngine.listLocalBackups(context)
                            showBackupsList = !showBackupsList
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showBackupsList) "Hide Syncthing Backups" else "Restore from Syncthing (${localBackups.size})")
                    }

                    // Backup Files List Accordion
                    AnimatedVisibility(visible = showBackupsList) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (localBackups.isEmpty()) {
                                Text(
                                    text = "No backup files found in Syncthing directory yet. Tap 'Sync to Syncthing Folder Now' above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                localBackups.take(5).forEach { backupFile ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(backupFile.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text("${backupFile.formattedDate} · ${backupFile.formattedSize}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        isLoading = true
                                                        val readResult = SyncthingSyncEngine.loadBackupFromFile(backupFile.file)
                                                        if (readResult.isSuccess) {
                                                            val b = readResult.getOrThrow()
                                                            LedgerBackupManager.restoreBundle(salaryRepository, b)
                                                            isSuccess = true
                                                            statusMessage = "Restored successfully from ${backupFile.name}!"
                                                        } else {
                                                            isSuccess = false
                                                            statusMessage = "Restore failed: ${readResult.exceptionOrNull()?.localizedMessage}"
                                                        }
                                                        isLoading = false
                                                    }
                                                }
                                            ) {
                                                Text("Restore", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Auto-Sync Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Sync on Changes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Automatically update Syncthing files on shift edits and saves.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it }
                    )
                }

                // Help & Setup Guide Accordion
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHelpGuide = !showHelpGuide }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Syncthing Setup Guide", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Icon(if (showHelpGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                        }

                        AnimatedVisibility(visible = showHelpGuide) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("1. Open your Syncthing GUI at https://syncthing.449100.xyz", style = MaterialTheme.typography.bodySmall)
                                Text("2. Click Actions → Settings → General → copy the API Key.", style = MaterialTheme.typography.bodySmall)
                                Text("3. In Syncthing, add a folder with Folder ID 'salary-calculator' mapped to your NAS backup location.", style = MaterialTheme.typography.bodySmall)
                                Text("4. On your Pixel 8 Pro, install Syncthing and pair the local 'Documents/SalaryCalculator' folder with your NAS folder.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        salaryRepository.setSyncthingServerUrl(serverUrlInput)
                        salaryRepository.setSyncthingApiKey(apiKeyInput)
                        salaryRepository.setSyncthingFolderId(folderIdInput)
                        salaryRepository.setSyncthingAutoSync(autoSyncEnabled)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
