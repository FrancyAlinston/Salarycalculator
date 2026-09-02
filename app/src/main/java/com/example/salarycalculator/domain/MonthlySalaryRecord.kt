package com.example.salarycalculator.domain

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MonthlySalaryRecord(
    val id: String = UUID.randomUUID().toString(),
    val monthYear: String,
    val timestamp: Long = System.currentTimeMillis(),
    val daysWorked: Double,
    val hoursPerDay: Double,
    val overtimeHours: Double = 0.0,
    val overtimeMultiplier: Double = 1.5,
    val hourlyRate: Double,
    val grossPay: Double,
    val pensionRate: Double = 5.0,
    val pensionContribution: Double = 0.0,
    val employerPension: Double = 0.0,
    val taxablePay: Double,
    val incomeTax: Double,
    val nationalInsurance: Double,
    val studentLoanPlan: StudentLoanPlan = StudentLoanPlan.NONE,
    val studentLoanDeduction: Double = 0.0,
    val totalDeductions: Double,
    val netPay: Double,
    val taxCode: String = "1257L",
    val taxRegion: TaxRegion = TaxRegion.UK_STANDARD,
    val note: String = ""
)
