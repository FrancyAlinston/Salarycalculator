package com.example.salarycalculator

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.domain.ThemeMode
import com.example.salarycalculator.theme.SalaryCalculatorTheme

class MainActivity : FragmentActivity() {

    private var lastPauseTimestamp: Long = 0L
    private var requiresReauth by mutableStateOf(false)

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val salaryRepo = SalaryRepository(this)

        enableEdgeToEdge()
        setContent {
            val themeMode by salaryRepo.getThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
            val isBiometricEnabled by salaryRepo.getBiometricLockEnabled().collectAsState(initial = false)
            val timeoutSeconds by salaryRepo.getBiometricTimeoutSeconds().collectAsState(initial = 0L)
            var isAuthenticated by remember { mutableStateOf(!isBiometricEnabled) }

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

            SalaryCalculatorTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (isBiometricEnabled && !isAuthenticated) {
                        // Lock screen overlay
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
                    } else {
                        MainNavigation(salaryRepo)
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
}
