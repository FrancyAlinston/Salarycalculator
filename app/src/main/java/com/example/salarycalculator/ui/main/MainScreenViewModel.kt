package com.example.salarycalculator.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainScreenViewModel : ViewModel() {
    private val _grossSalary = MutableStateFlow("")
    val grossSalary: StateFlow<String> = _grossSalary.asStateFlow()

    private val _taxRate = MutableStateFlow("")
    val taxRate: StateFlow<String> = _taxRate.asStateFlow()

    private val _netSalary = MutableStateFlow(0.0)
    val netSalary: StateFlow<Double> = _netSalary.asStateFlow()

    fun updateGrossSalary(value: String) {
        _grossSalary.value = value
    }

    fun updateTaxRate(value: String) {
        _taxRate.value = value
    }

    fun calculate() {
        val gross = _grossSalary.value.toDoubleOrNull() ?: 0.0
        val tax = _taxRate.value.toDoubleOrNull() ?: 0.0
        val net = gross - (gross * tax / 100)
        _netSalary.value = net
    }
}
