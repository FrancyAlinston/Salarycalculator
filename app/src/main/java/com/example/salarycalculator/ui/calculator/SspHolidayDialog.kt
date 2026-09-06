package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.SspHolidayEngine
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SspHolidayDialog(
    currentHourlyRate: Double,
    currentStandardHours: Double,
    totalLoggedHours: Double,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = SSP, 1 = Holiday Accrual

    // SSP State
    var qualifyingDays by remember { mutableIntStateOf(if (currentStandardHours >= 12.0) 3 else 5) }
    var sickDays by remember { mutableIntStateOf(5) }
    var isLinkedPeriod by remember { mutableStateOf(false) }

    // Holiday Accrual State
    var hoursWorkedInput by remember { mutableDoubleStateOf(if (totalLoggedHours > 0) totalLoggedHours else 160.0) }
    var hourlyRateInput by remember { mutableDoubleStateOf(if (currentHourlyRate > 0) currentHourlyRate else 12.82) }
    var shiftLengthInput by remember { mutableDoubleStateOf(if (currentStandardHours > 0) currentStandardHours else 12.0) }

    val sspResult = remember(qualifyingDays, sickDays, isLinkedPeriod) {
        SspHolidayEngine.calculateSsp(
            qualifyingDaysPerWeek = qualifyingDays,
            sickDaysLogged = sickDays,
            isLinkedPeriod = isLinkedPeriod
        )
    }

    val holidayResult = remember(hoursWorkedInput, hourlyRateInput, shiftLengthInput) {
        SspHolidayEngine.calculateHolidayAccrual(
            hoursWorked = hoursWorkedInput,
            hourlyRate = hourlyRateInput,
            standardShiftHours = shiftLengthInput
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.Healing else Icons.Default.BeachAccess,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Statutory Leave & Pay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "UK HMRC & DWP 2024/25–2025/26",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab Selection Bar
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Sick Pay (SSP)") },
                        icon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Holiday Pay (12.07%)") },
                        icon = { Icon(Icons.Default.BeachAccess, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                if (selectedTab == 0) {
                    // --- SSP SECTION ---
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Qualifying Days per Week",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(3, 4, 5, 6, 7).forEach { qd ->
                                    FilterChip(
                                        selected = qualifyingDays == qd,
                                        onClick = { qualifyingDays = qd },
                                        label = { Text("${qd}d", maxLines = 1) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Sick Days Logged in Pay Cycle",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { if (sickDays > 0) sickDays-- },
                                    enabled = sickDays > 0
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                }
                                Text(
                                    text = "$sickDays days",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { if (sickDays < 28) sickDays++ }
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(3, 4, 5, 7, 10, 14).forEach { d ->
                                    AssistChip(
                                        onClick = { sickDays = d },
                                        label = { Text("${d}d", maxLines = 1) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            // Linked Period Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Linked Sickness Period",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Prior illness within 8 weeks (no waiting days)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isLinkedPeriod,
                                    onCheckedChange = { isLinkedPeriod = it }
                                )
                            }
                        }
                    }

                    // SSP Summary Hero Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Gross Statutory Sick Pay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "£${String.format(Locale.UK, "%.2f", sspResult.totalGrossSsp)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Rate: £${String.format(Locale.UK, "%.2f", sspResult.dailySspRate)}/day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Waiting Days: ${sspResult.waitingDaysCount}d (unpaid)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Paid Days: ${sspResult.paidSspDaysCount}d",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Weekly Rate: £${SspHolidayEngine.WEEKLY_SSP_RATE_2024_2026}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = "ℹ️ Under UK law, SSP is paid starting from the 4th qualifying day of illness. Sickness must last at least 4 consecutive days to qualify. Sickness pay is taxable through PAYE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                } else {
                    // --- HOLIDAY ACCRUAL SECTION ---
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Hours Worked in Pay Period",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(96.0, 120.0, 144.0, 160.0, 192.0).forEach { h ->
                                    FilterChip(
                                        selected = hoursWorkedInput == h,
                                        onClick = { hoursWorkedInput = h },
                                        label = { Text("${h.toInt()}h", maxLines = 1) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (hoursWorkedInput > 0) hoursWorkedInput.toString() else "",
                                    onValueChange = { hoursWorkedInput = it.toDoubleOrNull() ?: 0.0 },
                                    label = { Text("Hours Worked") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = if (hourlyRateInput > 0) hourlyRateInput.toString() else "",
                                    onValueChange = { hourlyRateInput = it.toDoubleOrNull() ?: 0.0 },
                                    label = { Text("Hourly Rate (£)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Holiday Accrual Summary Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Accrued Holiday Pay (12.07%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "£${String.format(Locale.UK, "%.2f", holidayResult.accruedHolidayPay)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Accrued Leave: ${String.format(Locale.UK, "%.2f", holidayResult.accruedHolidayHours)} hrs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "≈ ${holidayResult.daysEquivalent} shifts",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rolled-Up Rate: £${String.format(Locale.UK, "%.2f", holidayResult.rolledUpHourlyRate)}/hr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Accrual: 12.07% (5.6 wks)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = "ℹ️ From April 2024, UK law authorizes the 12.07% statutory accrual calculation method for irregular hours and shift workers (5.6 statutory weeks ÷ 46.4 working weeks).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
