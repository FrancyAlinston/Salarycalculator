package com.example.salarycalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.salarycalculator.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { MainScreen(onItemClick = {}) }
  }

  @Test
  fun uiElements_exist() {
    composeTestRule.onNodeWithText("Salary Calculator").assertExists()
    composeTestRule.onNodeWithText("Gross Salary").assertExists()
    composeTestRule.onNodeWithText("Tax Rate (%)").assertExists()
    composeTestRule.onNodeWithText("Calculate Net Salary").assertExists()
  }

  @Test
  fun calculation_updatesNetSalaryDisplay() {
    composeTestRule.onNodeWithText("Gross Salary").performTextInput("5000")
    composeTestRule.onNodeWithText("Tax Rate (%)").performTextInput("20")
    composeTestRule.onNodeWithText("Calculate Net Salary").performClick()
    composeTestRule.onNodeWithText("Net Salary: $4000.00").assertExists()
  }
}
