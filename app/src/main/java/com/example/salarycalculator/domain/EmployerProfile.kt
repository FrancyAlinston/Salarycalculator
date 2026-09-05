package com.example.salarycalculator.domain

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EmployerProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val employerName: String = "",
    val taxCode: String = "1257L",
    val hourlyRate: Double = 12.82,
    val daysWorked: Double = 16.0,
    val hoursPerDay: Double = 12.0,
    val pensionRate: Double = 0.0,
    val taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    val studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
    val isPrimary: Boolean = false,
    val colorHex: String = "#0D9488"
)
