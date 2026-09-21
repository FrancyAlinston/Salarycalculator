package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class DutyRotaOcrEngineTest {

    @Test
    fun testShiftTypeFromCode_standardCodes() {
        val day = RotaShiftType.fromCode("D")
        assertTrue(day is RotaShiftType.Day)
        assertEquals(12.0, day.defaultHours, 0.001)
        assertTrue(day.isWorkShift)

        val night = RotaShiftType.fromCode("N")
        assertTrue(night is RotaShiftType.Night)
        assertEquals(12.0, night.defaultHours, 0.001)
        assertTrue(night.isWorkShift)

        val al = RotaShiftType.fromCode("A/L")
        assertTrue(al is RotaShiftType.AnnualLeave)
        assertEquals(12.0, al.defaultHours, 0.001)
        assertFalse(al.isWorkShift)

        val al2 = RotaShiftType.fromCode("AL")
        assertTrue(al2 is RotaShiftType.AnnualLeave)

        val td = RotaShiftType.fromCode("TD")
        assertTrue(td is RotaShiftType.TrainingDay)
        assertEquals(12.0, td.defaultHours, 0.001)

        val off = RotaShiftType.fromCode("-")
        assertTrue(off is RotaShiftType.Off)
        assertEquals(0.0, off.defaultHours, 0.001)
    }

    @Test
    fun testShiftTypeFromCode_customUnknownMarkings() {
        val custom = RotaShiftType.fromCode("SICK")
        assertTrue(custom is RotaShiftType.Custom)
        assertEquals("SICK", custom.code)
        assertEquals("Custom (SICK)", custom.label)
        assertEquals(12.0, custom.defaultHours, 0.001)
    }

    @Test
    fun testReferenceRoster_containsStaffMembers() {
        val roster = DutyRotaOcrEngine.OCTOBER_2026_REFERENCE_STAFF
        assertTrue(roster.isNotEmpty())
        assertTrue(roster.size >= 15)

        val names = roster.map { it.name }
        assertTrue(names.any { it.contains("Francy", ignoreCase = true) })
        assertTrue(names.any { it.contains("Cecilia", ignoreCase = true) })
        assertTrue(names.any { it.contains("Vanessa", ignoreCase = true) })
        assertTrue(names.any { it.contains("Oyebolu", ignoreCase = true) })
        assertTrue(names.any { it.contains("Francis Alabi", ignoreCase = true) })
        assertTrue(names.any { it.contains("Rajeev", ignoreCase = true) })
        assertTrue(names.any { it.contains("Alan Pascua", ignoreCase = true) })
        assertTrue(names.any { it.contains("Michelle Prust", ignoreCase = true) })
    }

    @Test
    fun testFrancyDSilvaShifts_correctNightShifts() {
        val francy = DutyRotaOcrEngine.OCTOBER_2026_REFERENCE_STAFF.first { it.id == "francy_dsilva" }
        assertEquals("Francy D'Silva", francy.name)
        assertEquals(15, francy.totalNightShifts)
        assertEquals(0, francy.totalDayShifts)
        assertEquals(15, francy.totalDaysWorked)
        assertEquals(180.0, francy.totalHours, 0.001)

        val heatmapMap = francy.toHeatmapShiftMap()
        assertEquals(15, heatmapMap.size)
        assertTrue(heatmapMap.containsKey(1))
        assertTrue(heatmapMap.containsKey(2))
        assertTrue(heatmapMap.containsKey(3))
        assertTrue(heatmapMap.containsKey(26))
        assertFalse(heatmapMap.containsKey(4))
    }

    @Test
    fun testLeaveAndTrainingDayCounts() {
        val oyebolu = DutyRotaOcrEngine.OCTOBER_2026_REFERENCE_STAFF.first { it.id == "oyebolu" }
        assertEquals(16, oyebolu.totalNightShifts)
        assertEquals(5, oyebolu.totalAnnualLeaveDays)

        val francis = DutyRotaOcrEngine.OCTOBER_2026_REFERENCE_STAFF.first { it.id == "francis_alabi" }
        assertEquals(14, francis.totalNightShifts)
        assertEquals(4, francis.totalAnnualLeaveDays)

        val rajeev = DutyRotaOcrEngine.OCTOBER_2026_REFERENCE_STAFF.first { it.id == "rajeev_raju" }
        assertEquals(13, rajeev.totalNightShifts)
        assertEquals(4, rajeev.totalAnnualLeaveDays)
        assertEquals(1, rajeev.totalTrainingDays)
    }

    @Test
    fun testRawTextParsing_extractsRoster() {
        val raw = "DUTY ROTA Oct-26 Care Staff Francy D'Silva Cecilia Vanessa"
        val result = DutyRotaOcrEngine.parseFromRawRosterText(raw)
        assertEquals(2026, result.year)
        assertEquals(10, result.month)
        assertEquals(31, result.daysInMonth)
        assertTrue(result.staffMembers.any { it.name.contains("Francy") })
        assertTrue(result.staffMembers.any { it.name.contains("Cecilia") })
    }
}
