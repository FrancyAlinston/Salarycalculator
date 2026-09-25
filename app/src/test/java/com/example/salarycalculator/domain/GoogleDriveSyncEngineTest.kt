package com.example.salarycalculator.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class GoogleDriveSyncEngineTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun backupBundle_driveSerialization_containsGoogleDriveMetadata() {
        val bundle = BackupBundle(
            version = 2,
            googleAccountEmail = "user@example.com",
            googleDriveAutoSync = true,
            googleDriveLastSync = "2026-09-25 12:45:00",
            taxCode = "1257L",
            hourlyRate = 14.50
        )

        val jsonString = json.encodeToString(bundle)
        val decoded = json.decodeFromString<BackupBundle>(jsonString)

        assertEquals("user@example.com", decoded.googleAccountEmail)
        assertTrue(decoded.googleDriveAutoSync)
        assertEquals("2026-09-25 12:45:00", decoded.googleDriveLastSync)
        assertEquals("1257L", decoded.taxCode)
        assertEquals(14.50, decoded.hourlyRate, 0.001)
    }

    @Test
    fun driveAppDataScope_isCorrectUrl() {
        assertEquals("https://www.googleapis.com/auth/drive.appdata", GoogleDriveSyncEngine.DRIVE_APPDATA_SCOPE_URI)
    }
}
