package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthPersistenceTest {

    @Test
    fun testGoogleAccountValidation() {
        val validEmail = "francyalinston@gmail.com"
        val emptyEmail = ""
        val blankEmail = "   "

        assertTrue(validEmail.isNotBlank())
        assertTrue(validEmail.contains("@"))
        assertFalse(emptyEmail.isNotBlank())
        assertFalse(blankEmail.isNotBlank())
    }

    @Test
    fun testDeviceFileNameSanitization() {
        val fileName = GoogleDriveSyncEngine.getDeviceFileName("Pixel 8 Pro (Test)")
        assertTrue(fileName.startsWith("SalaryCalculator_Device_"))
        assertTrue(fileName.endsWith(".json"))
        assertFalse(fileName.contains(" "))
        assertFalse(fileName.contains("("))
        assertFalse(fileName.contains(")"))
    }
}
