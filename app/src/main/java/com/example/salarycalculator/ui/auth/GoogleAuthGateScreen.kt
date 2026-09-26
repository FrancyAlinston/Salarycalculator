package com.example.salarycalculator.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.R
import com.example.salarycalculator.domain.GoogleDriveSyncEngine
import com.example.salarycalculator.domain.LedgerBackupManager
import com.example.salarycalculator.domain.RemoteDeviceBackup
import com.example.salarycalculator.domain.SalaryRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

/**
 * Mandatory Google Authentication Gatekeeper Screen.
 * The user must sign in with a Google account on initial app launch
 * before gaining access to the payroll calculator, shift roster, and history.
 *
 * Includes FEAT-937: Instant Cloud Restore Prompt on First Login.
 */
@Suppress("DEPRECATION")
@Composable
fun GoogleAuthGateScreen(
    salaryRepository: SalaryRepository,
    onAuthenticated: (GoogleSignInAccount) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }
    var isCheckingRemoteBackups by remember { mutableStateOf(false) }
    var isRestoringBackup by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var authenticatedAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var foundRemoteBackups by remember { mutableStateOf<List<RemoteDeviceBackup>>(emptyList()) }

    val googleSignInClient = remember { GoogleDriveSyncEngine.getGoogleSignInClient(context) }

    fun processAccountAuth(account: GoogleSignInAccount) {
        authenticatedAccount = account
        isCheckingRemoteBackups = true
        scope.launch {
            try {
                salaryRepository.setGoogleAccountEmail(account.email ?: "")
                salaryRepository.setGoogleAccountName(account.displayName ?: "")

                // FEAT-937: Check for remote backups on Google Drive
                val backupResult = GoogleDriveSyncEngine.listRemoteDeviceBackups(context, account)
                if (backupResult.isSuccess) {
                    val backups = backupResult.getOrThrow()
                    if (backups.isNotEmpty()) {
                        foundRemoteBackups = backups
                        isCheckingRemoteBackups = false
                        return@launch
                    }
                }
            } catch (_: Exception) {}

            isCheckingRemoteBackups = false
            onAuthenticated(account)
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null && !account.email.isNullOrBlank()) {
                    processAccountAuth(account)
                } else {
                    errorMessage = "Unable to retrieve Google account details. Please try again."
                }
            } catch (e: ApiException) {
                errorMessage = "Google Sign-In failed (Code: ${e.statusCode}): ${e.localizedMessage}"
            } catch (e: Exception) {
                errorMessage = "Authentication error: ${e.localizedMessage}"
            }
        } else {
            errorMessage = "Google Sign-In was cancelled. Sign-in is mandatory to proceed."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Branding Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💰",
                    fontSize = 38.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Salary Calculator",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sign in to secure your payroll & shift records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // FEAT-937: Instant Cloud Restore Prompt if Remote Backups Found
            if (foundRemoteBackups.isNotEmpty() && authenticatedAccount != null) {
                val latestBackup = foundRemoteBackups.maxByOrNull { it.timestamp } ?: foundRemoteBackups.first()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Cloud Backup Found!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "From ${latestBackup.deviceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Scheduled Shifts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${latestBackup.shiftCount} shifts", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Saved Salary Records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${latestBackup.recordCount} payslips", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tax Code & Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${latestBackup.taxCode} • £${"%.2f".format(latestBackup.hourlyRate)}/h", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 1-Tap Cloud Restore Button
                        Button(
                            onClick = {
                                isRestoringBackup = true
                                scope.launch {
                                    try {
                                        val bundleRes = GoogleDriveSyncEngine.downloadBackupByFileId(
                                            context,
                                            authenticatedAccount!!,
                                            latestBackup.fileId
                                        )
                                        if (bundleRes.isSuccess) {
                                            val bundle = bundleRes.getOrThrow()
                                            LedgerBackupManager.restoreBundle(salaryRepository, bundle)
                                            Toast.makeText(
                                                context,
                                                "Restored ${latestBackup.shiftCount} shifts from ${latestBackup.deviceName}!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (_: Exception) {}
                                    isRestoringBackup = false
                                    onAuthenticated(authenticatedAccount!!)
                                }
                            },
                            enabled = !isRestoringBackup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isRestoringBackup) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restoring All Data...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore Everything from Cloud", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { onAuthenticated(authenticatedAccount!!) },
                            enabled = !isRestoringBackup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Start Fresh (Skip Restore)", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else {
                // Benefits Feature Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Why Google Sign-In is required:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AuthFeatureItem(
                            icon = Icons.Default.CloudSync,
                            iconTint = Color(0xFF38BDF8),
                            title = "Automatic Google Drive Cloud Sync",
                            description = "Your confidential payroll calculations, custom tax codes, and history are backed up securely to your private AppData."
                        )

                        AuthFeatureItem(
                            icon = Icons.Default.CalendarMonth,
                            iconTint = Color(0xFF10B981),
                            title = "Multi-Device Shift Heatmap",
                            description = "Sync duty schedules, overtime, annual leave, and sickness across all your phones and tablets seamlessly."
                        )

                        AuthFeatureItem(
                            icon = Icons.Default.Security,
                            iconTint = Color(0xFFA855F7),
                            title = "Zero-Knowledge Confidentiality",
                            description = "Your financial data remains strictly in your personal Google account with no third-party database tracking."
                        )
                    }
                }

                // Error Alert Banner (if sign-in failed)
                if (!errorMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Official Google Sign-In Button
                Button(
                    onClick = {
                        errorMessage = null
                        isSigningIn = true
                        signInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    enabled = !isSigningIn && !isCheckingRemoteBackups,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F2937)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
                ) {
                    if (isSigningIn || isCheckingRemoteBackups) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isCheckingRemoteBackups) "Checking Cloud Backups..." else "Authenticating...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                }

                Text(
                    text = "By signing in, you connect your personal Google account to secure your confidential payroll records.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthFeatureItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
