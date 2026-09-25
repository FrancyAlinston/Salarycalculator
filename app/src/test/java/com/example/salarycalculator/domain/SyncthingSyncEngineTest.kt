package com.example.salarycalculator.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SyncthingSyncEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun backupBundle_serializationAndDeserialization_preservesAllFields() {
        val sampleBundle = BackupBundle(
            records = emptyList(),
            profiles = emptyList(),
            customDeductions = emptyList(),
            taxCode = "1257L",
            taxRegion = TaxRegion.UK_STANDARD,
            taxYear = TaxYear.YEAR_2024_2025,
            pensionRate = 5.0,
            hourlyRate = 16.50,
            hasMarriageAllowance = true,
            hasBlindPersonsAllowance = false
        )

        val testFile = tempFolder.newFile("SalaryCalculator_Backup_Test.json")
        testFile.writeText(json.encodeToString(sampleBundle))

        val loadResult = SyncthingSyncEngine.loadBackupFromFile(testFile)
        assertTrue(loadResult.isSuccess)

        val loaded = loadResult.getOrThrow()
        assertEquals("1257L", loaded.taxCode)
        assertEquals(16.50, loaded.hourlyRate, 0.001)
        assertEquals(TaxRegion.UK_STANDARD, loaded.taxRegion)
        assertEquals(TaxYear.YEAR_2024_2025, loaded.taxYear)
        assertEquals(5.0, loaded.pensionRate, 0.001)
        assertTrue(loaded.hasMarriageAllowance)
        assertFalse(loaded.hasBlindPersonsAllowance)
    }

    @Test
    fun syncthingBackupFile_formattingHelper_formatsCorrectly() {
        val fakeFile = tempFolder.newFile("SalaryCalculator_Backup_20260925_104000.json")
        fakeFile.writeText("{\"taxCode\":\"1257L\"}")

        val backup = SyncthingBackupFile(
            name = fakeFile.name,
            file = fakeFile,
            lastModified = fakeFile.lastModified(),
            sizeBytes = fakeFile.length()
        )

        assertTrue(backup.formattedSize.contains("KB"))
        assertTrue(backup.formattedDate.isNotEmpty())
    }

    @Test
    fun loadBackupFromFile_nonExistentFile_returnsFailure() {
        val missingFile = File(tempFolder.root, "does_not_exist.json")
        val result = SyncthingSyncEngine.loadBackupFromFile(missingFile)
        assertTrue(result.isFailure)
    }
}
