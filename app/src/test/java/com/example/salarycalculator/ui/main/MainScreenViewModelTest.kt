package com.example.salarycalculator.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenViewModelTest {

  @Test
  fun initialValues_areDefault() {
    val viewModel = MainScreenViewModel()
    assertEquals("", viewModel.grossSalary.value)
    assertEquals("", viewModel.taxRate.value)
    assertEquals(0.0, viewModel.netSalary.value, 0.001)
  }

  @Test
  fun calculate_computesNetSalaryCorrectly() {
    val viewModel = MainScreenViewModel()
    viewModel.updateGrossSalary("3000")
    viewModel.updateTaxRate("20")
    viewModel.calculate()

    // 3000 - (3000 * 20 / 100) = 2400
    assertEquals(2400.0, viewModel.netSalary.value, 0.001)
  }

  @Test
  fun calculate_withInvalidInputs_defaultsToZero() {
    val viewModel = MainScreenViewModel()
    viewModel.updateGrossSalary("invalid")
    viewModel.updateTaxRate("20")
    viewModel.calculate()

    assertEquals(0.0, viewModel.netSalary.value, 0.001)
  }
}
