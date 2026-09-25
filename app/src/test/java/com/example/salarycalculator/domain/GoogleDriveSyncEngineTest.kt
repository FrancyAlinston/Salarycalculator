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
            hourlyRate = 14.50,
            deviceName = "Google Pixel 8 Pro",
            deviceModel = "Pixel 8 Pro"
        )

        val jsonString = json.encodeToString(bundle)
        val decoded = json.decodeFromString<BackupBundle>(jsonString)

        assertEquals("user@example.com", decoded.googleAccountEmail)
        assertTrue(decoded.googleDriveAutoSync)
        assertEquals("2026-09-25 12:45:00", decoded.googleDriveLastSync)
        assertEquals("1257L", decoded.taxCode)
        assertEquals(14.50, decoded.hourlyRate, 0.001)
        assertEquals("Google Pixel 8 Pro", decoded.deviceName)
        assertEquals("Pixel 8 Pro", decoded.deviceModel)
    }

    @Test
    fun getDeviceFileName_sanitizesSpecialCharacters() {
        val fileName1 = GoogleDriveSyncEngine.getDeviceFileName("Google Pixel 8 Pro")
        assertEquals("SalaryCalculator_Device_Google_Pixel_8_Pro.json", fileName1)

        val fileName2 = GoogleDriveSyncEngine.getDeviceFileName("Redmi Note 10 Pro / Global")
        assertEquals("SalaryCalculator_Device_Redmi_Note_10_Pro___Global.json", fileName2)
    }

    @Test
    fun remoteDeviceBackup_serializationAndProperties() {
        val backup = RemoteDeviceBackup(
            fileId = "drive-file-12345",
            fileName = "SalaryCalculator_Device_Pixel_8_Pro.json",
            deviceName = "Google Pixel 8 Pro",
            deviceModel = "Pixel 8 Pro",
            modifiedTime = "2026-09-25 13:45:00",
            shiftCount = 16,
            recordCount = 3,
            hourlyRate = 12.82,
            taxCode = "1257L",
            timestamp = 1790342400000L,
            isCurrentDevice = true
        )

        val str = json.encodeToString(backup)
        val decoded = json.decodeFromString<RemoteDeviceBackup>(str)

        assertEquals("drive-file-12345", decoded.fileId)
        assertEquals("Google Pixel 8 Pro", decoded.deviceName)
        assertEquals(16, decoded.shiftCount)
        assertEquals(3, decoded.recordCount)
        assertEquals(12.82, decoded.hourlyRate, 0.001)
        assertTrue(decoded.isCurrentDevice)
    }

    @Test
    fun driveAppDataScope_isCorrectUrl() {
        assertEquals("https://www.googleapis.com/auth/drive.appdata", GoogleDriveSyncEngine.DRIVE_APPDATA_SCOPE_URI)
    }
}
