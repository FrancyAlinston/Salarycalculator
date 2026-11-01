package com.example.salarycalculator

import com.example.salarycalculator.domain.RealtimeMultiDeviceSyncEngine

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.example.salarycalculator.domain.DutyShiftReminderWorker
import com.example.salarycalculator.domain.GoogleDriveSyncEngine
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.domain.ThemeMode
import com.example.salarycalculator.domain.WearShiftNotificationManager
import com.example.salarycalculator.theme.SalaryCalculatorTheme
import com.example.salarycalculator.ui.auth.GoogleAuthGateScreen

class MainActivity : FragmentActivity() {

    private var lastPauseTimestamp: Long = 0L
    private var requiresReauth by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onPause() {
        super.onPause()
        lastPauseTimestamp = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (lastPauseTimestamp > 0L) {
            // Signal re-auth check in Compose
            requiresReauth = true
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val salaryRepo = SalaryRepository(this@MainActivity)
            RealtimeMultiDeviceSyncEngine.checkAndSyncRemoteChanges(this@MainActivity, salaryRepo, force = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val salaryRepo = SalaryRepository(this)
        WearShiftNotificationManager.createNotificationChannels(this)
        DutyShiftReminderWorker.schedulePeriodicCheck(this)
        com.example.salarycalculator.domain.LiveFxWorker.schedulePeriodicSync(this)
        com.example.salarycalculator.domain.LiveFxWorker.runOneTimeSync(this)
        com.example.salarycalculator.domain.AutoBackupObserver.startObserving(this, lifecycleScope)
        com.example.salarycalculator.domain.CloudSyncWorker.scheduleAutoSync(this, true)
        handleBackupIntent(intent, salaryRepo)

        // Real-time multi-device cloud synchronization across phones, watch, and NAS
        RealtimeMultiDeviceSyncEngine.startContinuousSync(this, lifecycleScope)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by salaryRepo.getThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
            val themePalette by salaryRepo.getThemePalette().collectAsState(initial = com.example.salarycalculator.domain.ThemePalette.OCEAN)
            val isBiometricEnabled by salaryRepo.getBiometricLockEnabled().collectAsState(initial = false)
            val timeoutSeconds by salaryRepo.getBiometricTimeoutSeconds().collectAsState(initial = 0L)
            val googleEmail by salaryRepo.getGoogleAccountEmail().collectAsState(initial = null)
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var isAuthenticated by remember { mutableStateOf(!isBiometricEnabled) }

            // Auto-detect existing authorized Google session on device if DataStore was empty
            LaunchedEffect(googleEmail) {
                if (googleEmail != null && googleEmail!!.isBlank()) {
                    val lastAccount = GoogleDriveSyncEngine.getSignedInAccount(context)
                    if (lastAccount != null && !lastAccount.email.isNullOrBlank()) {
                        salaryRepo.setGoogleAccountEmail(lastAccount.email ?: "")
                        salaryRepo.setGoogleAccountName(lastAccount.displayName ?: "")
                    }
                }
            }

            // Evaluate background-to-foreground timeout
            LaunchedEffect(requiresReauth) {
                if (requiresReauth) {
                    val elapsedSeconds = (System.currentTimeMillis() - lastPauseTimestamp) / 1000L
                    if (isBiometricEnabled && elapsedSeconds >= timeoutSeconds) {
                        isAuthenticated = false
                        showBiometricPrompt { success ->
                            isAuthenticated = success
                        }
                    }
                    requiresReauth = false
                }
            }

            // Trigger biometric prompt on initial launch if lock is enabled
            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isAuthenticated) {
                    showBiometricPrompt { success ->
                        isAuthenticated = success
                    }
                } else if (!isBiometricEnabled) {
                    isAuthenticated = true
                }
            }

            SalaryCalculatorTheme(themeMode = themeMode, themePalette = themePalette) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        // Initial loading state while DataStore is first read
                        googleEmail == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        // MANDATORY GOOGLE AUTHENTICATION GATE:
                        // If no Google account is signed in, user cannot see or access the app
                        googleEmail!!.isBlank() -> {
                            GoogleAuthGateScreen(
                                salaryRepository = salaryRepo,
                                onAuthenticated = { account ->
                                    scope.launch {
                                        salaryRepo.setGoogleAccountEmail(account.email ?: "")
                                        salaryRepo.setGoogleAccountName(account.displayName ?: "")
                                    }
                                }
                            )
                        }

                        // Biometric Lock Screen Overlay (if biometrics enabled)
                        isBiometricEnabled && !isAuthenticated -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = "Salary Calculator Locked",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Please authenticate with your fingerprint, face, or device PIN to access your salary records.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                showBiometricPrompt { success ->
                                                    isAuthenticated = success
                                                }
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Unlock with Biometrics", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Main Application Interface
                        else -> {
                            MainNavigation(salaryRepo)
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onResult(false)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(false)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Salary Calculator")
            .setSubtitle("Confirm your identity to view confidential payroll details")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleBackupIntent(intent, SalaryRepository(this))
    }

    private fun handleBackupIntent(intent: android.content.Intent?, salaryRepo: SalaryRepository) {
        if (intent == null) return
        val restoreJsonB64 = intent.getStringExtra("restore_json_b64")
        val restoreJson = if (!restoreJsonB64.isNullOrBlank()) {
            try {
                String(android.util.Base64.decode(restoreJsonB64, android.util.Base64.DEFAULT), Charsets.UTF_8)
            } catch (_: Exception) { null }
        } else {
            intent.getStringExtra("restore_json")
        }
        val restoreFilePath = intent.getStringExtra("restore_file_path")
        val uri = intent.data

        if (restoreFilePath.isNullOrBlank() && restoreJson.isNullOrBlank() && uri == null) return

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val jsonContent = when {
                !restoreJson.isNullOrBlank() -> restoreJson
                !restoreFilePath.isNullOrBlank() -> {
                    try { java.io.File(restoreFilePath).readText() } catch (_: Exception) { null }
                }
                uri != null -> {
                    try {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            java.io.BufferedReader(java.io.InputStreamReader(stream)).readText()
                        }
                    } catch (_: Exception) { null }
                }
                else -> null
            }

            if (!jsonContent.isNullOrBlank()) {
                val parseResult = com.example.salarycalculator.domain.LedgerBackupManager.parseBackupJson(jsonContent)
                if (parseResult.isSuccess) {
                    com.example.salarycalculator.domain.LedgerBackupManager.restoreBundle(salaryRepo, parseResult.getOrThrow())
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "2026 Rota & Shifts Imported Successfully!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
