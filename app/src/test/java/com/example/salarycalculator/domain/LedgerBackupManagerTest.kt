package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class LedgerBackupManagerTest {

    @Test
    fun testParseV2BackupBundle() {
        val jsonV2 = """
            {
                "version": 2,
                "timestamp": "2026-09-25T11:00:00Z",
                "records": [
                    {
                        "id": "rec_01",
                        "monthYear": "August 2026",
                        "timestamp": 1724580000000,
                        "daysWorked": 16.0,
                        "hoursPerDay": 12.0,
                        "hourlyRate": 13.50,
                        "overtimeHours": 10.0,
                        "overtimeRateMultiplier": 1.5,
                        "bonus": 100.0,
                        "commission": 0.0,
                        "taxCode": "1257L",
                        "taxRegion": "UK_STANDARD",
                        "taxYear": "YEAR_2024_2025",
                        "pensionRate": 5.0,
                        "hasMarriageAllowance": false,
                        "hasBlindPersonsAllowance": false,
                        "studentLoanPlan": "PLAN_2",
                        "customDeductions": [],
                        "note": "August shift calibration",
                        "employerProfileId": "emp_01"
                    }
                ],
                "profiles": [
                    {
                        "id": "emp_01",
                        "name": "Acme Care Ltd",
                        "hourlyRate": 13.50,
                        "hoursPerDay": 12.0,
                        "taxCode": "1257L",
                        "pensionRate": 5.0,
                        "colorArgb": 4282608478
                    }
                ],
                "customDeductions": [],
                "taxCode": "1257L",
                "taxRegion": "UK_STANDARD",
                "taxYear": "YEAR_2024_2025",
                "pensionRate": 5.0,
                "hourlyRate": 13.50,
                "defaultHoursPerDay": 12.0,
                "studentLoanPlan": "PLAN_2",
                "overtimeMultiplier": 1.5,
                "hasMarriageAllowance": false,
                "hasBlindPersonsAllowance": false,
                "annualShiftScheduleJson": "{\"2026-8-1\":12.0,\"2026-8-2\":12.0}",
                "employerShiftAssignmentsJson": "{\"2026-8-1\":\"emp_01\"}",
                "payScheduleType": "LAST_FRIDAY_OF_MONTH",
                "payScheduleFixedDay": 28,
                "payScheduleCutoffLeadDays": 5,
                "themeMode": "DARK",
                "themePalette": "OCEAN",
                "preferredCurrency": "INR",
                "secondaryCurrency": "USD"
            }
        """.trimIndent()

        val parseResult = LedgerBackupManager.parseBackupJson(jsonV2)
        assertTrue("Parsing v2 bundle should succeed", parseResult.isSuccess)
        val bundle = parseResult.getOrThrow()

        assertEquals(2, bundle.version)
        assertEquals(1, bundle.records.size)
        assertEquals(1, bundle.profiles.size)
        assertEquals("1257L", bundle.taxCode)
        assertEquals("INR", bundle.preferredCurrency)
        assertEquals("USD", bundle.secondaryCurrency)
        assertEquals("{\"2026-8-1\":12.0,\"2026-8-2\":12.0}", bundle.annualShiftScheduleJson)
        assertEquals("{\"2026-8-1\":\"emp_01\"}", bundle.employerShiftAssignmentsJson)
    }

    @Test
    fun testParseLegacyV1BackupBundle() {
        // Older legacy backup without new fields like annualShiftScheduleJson, preferredCurrency, etc.
        val legacyV1Json = """
            {
                "version": 1,
                "timestamp": "2024-05-01T10:00:00Z",
                "records": [
                    {
                        "id": "rec_legacy",
                        "monthYear": "May 2024",
                        "timestamp": 1714560000000,
                        "daysWorked": 14.0,
                        "hoursPerDay": 12.0,
                        "hourlyRate": 12.50,
                        "overtimeHours": 0.0,
                        "overtimeRateMultiplier": 1.5,
                        "bonus": 0.0,
                        "commission": 0.0,
                        "taxCode": "1257L",
                        "taxRegion": "UK_STANDARD",
                        "taxYear": "YEAR_2024_2025",
                        "pensionRate": 3.0,
                        "hasMarriageAllowance": false,
                        "hasBlindPersonsAllowance": false,
                        "studentLoanPlan": "NONE",
                        "customDeductions": [],
                        "note": "Legacy record",
                        "employerProfileId": null
                    }
                ],
                "profiles": [],
                "customDeductions": [],
                "taxCode": "1257L",
                "taxRegion": "UK_STANDARD",
                "taxYear": "YEAR_2024_2025",
                "pensionRate": 3.0,
                "hourlyRate": 12.50,
                "hasMarriageAllowance": false,
                "hasBlindPersonsAllowance": false
            }
        """.trimIndent()

        val parseResult = LedgerBackupManager.parseBackupJson(legacyV1Json)
        assertTrue("Parsing legacy v1 bundle should succeed with backward compatibility defaults", parseResult.isSuccess)
        val bundle = parseResult.getOrThrow()

        assertEquals(1, bundle.records.size)
        assertEquals("May 2024", bundle.records[0].monthYear)
        assertEquals("1257L", bundle.taxCode)
        assertEquals("EUR", bundle.preferredCurrency) // Default fallback
        assertEquals("USD", bundle.secondaryCurrency) // Default fallback
        assertEquals("", bundle.annualShiftScheduleJson)
    }
}
