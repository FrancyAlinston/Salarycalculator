package com.example.salarycalculator.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@Composable
fun GoogleDriveSyncDialog(
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedEmail by salaryRepository.getGoogleAccountEmail().collectAsState(initial = "")
    val savedAutoSync by salaryRepository.getGoogleDriveAutoSync().collectAsState(initial = true)
    val savedLastSync by salaryRepository.getGoogleDriveLastSync().collectAsState(initial = "")

    var signedInAccount by remember { mutableStateOf<GoogleSignInAccount?>(GoogleDriveSyncEngine.getSignedInAccount(context)) }
    var autoSyncEnabled by remember(savedAutoSync) { mutableStateOf(savedAutoSync) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val googleSignInClient = remember { GoogleDriveSyncEngine.getGoogleSignInClient(context) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                if (account != null) {
                    signedInAccount = account
                    scope.launch {
                        salaryRepository.setGoogleAccountEmail(account.email ?: "")
                        statusMessage = "Connected to Google Account: ${account.email}"
                        isSuccess = true
                    }
                }
            } catch (e: Exception) {
                statusMessage = "Sign-in error: ${e.localizedMessage}"
                isSuccess = false
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
                Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Google Drive Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    text = "Seamlessly sync your shift schedules, employer profiles, rates, and tax calculations across all your Android devices using private Google Drive AppData.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Google Account Connection Status Card
                Surface(
                    color = if (signedInAccount != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (signedInAccount != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                if (signedInAccount != null) Icons.Default.CheckCircle else Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = if (signedInAccount != null) Emerald60 else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (signedInAccount != null) "Connected Google Account" else "Not Connected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = signedInAccount?.email ?: (if (savedEmail.isNotBlank()) savedEmail else "Connect account to enable multi-phone sync"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (savedLastSync.isNotBlank()) {
                            Text(
                                text = "Last Synced: $savedLastSync",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (signedInAccount == null) {
                            Button(
                                onClick = {
                                    signInLauncher.launch(googleSignInClient.signInIntent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In with Google", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        signedInAccount = null
                                        scope.launch {
                                            salaryRepository.setGoogleAccountEmail("")
                                        }
                                        statusMessage = "Signed out of Google Account"
                                        isSuccess = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Automatic Sync Toggle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Real-Time Sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Automatically backup to Google Drive AppData on every change", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = {
                                autoSyncEnabled = it
                                scope.launch {
                                    salaryRepository.setGoogleDriveAutoSync(it)
                                }
                            }
                        )
                    }
                }

                // Action Buttons: Manual Sync & Restore
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val account = signedInAccount ?: GoogleDriveSyncEngine.getSignedInAccount(context)
                            if (account == null) {
                                statusMessage = "Please sign in with your Google account first."
                                isSuccess = false
                                return@Button
                            }
                            isLoading = true
                            statusMessage = null
                            scope.launch {
                                try {
                                    val bundle = LedgerBackupManager.createFullBackupBundle(salaryRepository)
                                    val result = GoogleDriveSyncEngine.uploadBackupToDrive(context, account, bundle)
                                    if (result.isSuccess) {
                                        val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                        salaryRepository.setGoogleDriveLastSync(nowStr)
                                        statusMessage = "Backup successfully uploaded to Google Drive!"
                                        isSuccess = true
                                    } else {
                                        statusMessage = "Sync failed: ${result.exceptionOrNull()?.localizedMessage}"
                                        isSuccess = false
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Sync error: ${e.localizedMessage}"
                                    isSuccess = false
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && signedInAccount != null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup Now", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            val account = signedInAccount ?: GoogleDriveSyncEngine.getSignedInAccount(context)
                            if (account == null) {
                                statusMessage = "Please sign in with your Google account first."
                                isSuccess = false
                                return@FilledTonalButton
                            }
                            isLoading = true
                            statusMessage = null
                            scope.launch {
                                try {
                                    val result = GoogleDriveSyncEngine.downloadBackupFromDrive(context, account)
                                    if (result.isSuccess) {
                                        val bundle = result.getOrThrow()
                                        LedgerBackupManager.restoreBundle(salaryRepository, bundle)
                                        val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                        salaryRepository.setGoogleDriveLastSync(nowStr)
                                        statusMessage = "Successfully restored ${bundle.records.size} salary records and shift schedule from Google Drive!"
                                        isSuccess = true
                                    } else {
                                        statusMessage = "Restore failed: ${result.exceptionOrNull()?.localizedMessage}"
                                        isSuccess = false
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Restore error: ${e.localizedMessage}"
                                    isSuccess = false
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && signedInAccount != null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Drive", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Status Message Display
                AnimatedVisibility(visible = statusMessage != null) {
                    Surface(
                        color = if (isSuccess) Emerald60.copy(alpha = 0.15f) else Rose60.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSuccess) Emerald60 else Rose60),
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
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) Emerald60 else Rose60,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // AppData Information note
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Backups are stored inside your private Google Drive AppData folder. It is hidden from regular Drive files and is strictly accessible by this app across all your signed-in devices.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
