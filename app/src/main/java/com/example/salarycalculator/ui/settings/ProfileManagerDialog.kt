package com.example.salarycalculator.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.*
import com.example.salarycalculator.theme.Emerald60
import kotlinx.coroutines.launch

@Composable
fun ProfileManagerDialog(
    salaryRepository: SalaryRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val profiles by salaryRepository.getEmployerProfiles().collectAsState(initial = emptyList())
    val activeProfileId by salaryRepository.getActiveProfileId().collectAsState(initial = "primary_default")

    var isAddingNew by remember { mutableStateOf(false) }

    // Form inputs for new profile
    var newProfileName by remember { mutableStateOf("") }
    var newEmployerName by remember { mutableStateOf("") }
    var newTaxCode by remember { mutableStateOf("1257L") }
    var newHourlyRate by remember { mutableStateOf("15.00") }
    var newPensionRate by remember { mutableStateOf("5.0") }
    var newTaxRegion by remember { mutableStateOf(TaxRegion.UK_STANDARD) }
    var newStudentLoan by remember { mutableStateOf(StudentLoanPlan.NONE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isAddingNew) "New Employer Profile" else "Employer Profiles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (isAddingNew) {
                // New Profile Creation Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Label") },
                        placeholder = { Text("e.g. Weekend Freelance") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newEmployerName,
                        onValueChange = { newEmployerName = it },
                        label = { Text("Company / Employer Name") },
                        placeholder = { Text("e.g. Acme Studio Ltd") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newHourlyRate,
                        onValueChange = { newHourlyRate = it },
                        label = { Text("Hourly Rate (£/hr)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newTaxCode,
                        onValueChange = { newTaxCode = it },
                        label = { Text("Tax Code (e.g. BR or 1257L)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPensionRate,
                        onValueChange = { newPensionRate = it },
                        label = { Text("Pension Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            } else {
                // Profiles List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Switch between jobs or add a secondary employment with separate tax codes and hourly rates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profiles, key = { it.id }) { profile ->
                            val isActive = profile.id == activeProfileId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            salaryRepository.setActiveProfileId(profile.id)
                                            salaryRepository.setTaxCode(profile.taxCode)
                                            salaryRepository.setDefaultHourlyRate(profile.hourlyRate)
                                            salaryRepository.setPensionRate(profile.pensionRate)
                                            salaryRepository.setTaxRegion(profile.taxRegion)
                                            salaryRepository.setStudentLoanPlan(profile.studentLoanPlan)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isActive) {
                                                Surface(
                                                    color = Emerald60,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Active",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "£${"%.2f".format(profile.hourlyRate)}/hr · Tax Code: ${profile.taxCode} · Pension: ${profile.pensionRate}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (profiles.size > 1 && !profile.isPrimary) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    salaryRepository.deleteEmployerProfile(profile.id)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Profile",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { isAddingNew = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Employer Profile")
                    }
                }
            }
        },
        confirmButton = {
            if (isAddingNew) {
                Button(
                    onClick = {
                        val created = EmployerProfile(
                            name = newProfileName.trim().ifBlank { "Secondary Job" },
                            employerName = newEmployerName.trim(),
                            taxCode = newTaxCode.trim().ifBlank { "BR" },
                            hourlyRate = newHourlyRate.toDoubleOrNull() ?: 15.0,
                            pensionRate = newPensionRate.toDoubleOrNull() ?: 5.0,
                            taxRegion = newTaxRegion,
                            studentLoanPlan = newStudentLoan
                        )
                        scope.launch {
                            salaryRepository.saveEmployerProfile(created)
                            isAddingNew = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Profile", fontWeight = FontWeight.Bold)
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
                    if (isAddingNew) isAddingNew = false else onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
