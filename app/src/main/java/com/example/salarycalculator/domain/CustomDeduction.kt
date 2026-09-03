package com.example.salarycalculator.domain

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CustomDeduction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val isPreTax: Boolean = false // If true, reduces taxable gross prior to tax; if false, deducted post-tax from net
)
