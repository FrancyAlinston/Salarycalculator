package com.example.salarycalculator.domain

import java.util.Calendar

enum class ShiftPatternType(val displayName: String, val description: String, val cycleLengthDays: Int) {
    FOUR_ON_FOUR_OFF(
        displayName = "4-On 4-Off",
        description = "4 working days followed by 4 days off (8-day repeating cycle)",
        cycleLengthDays = 8
    ),
    CONTINENTAL_2_2_3(
        displayName = "Continental (2-2-3)",
        description = "2 Day shifts, 2 Night shifts, 3 Off, 2 Day, 3 Night, 2 Off (28-day full rotation)",
        cycleLengthDays = 28
    ),
    PITMAN_2_3_2(
        displayName = "Pitman (2-2-3-2-2-3)",
        description = "2 On, 2 Off, 3 On, 2 Off, 2 On, 3 Off (14-day rotating cycle, alternating 3-day weekends)",
        cycleLengthDays = 14
    ),
    THREE_SHIFT_ROTATING(
        displayName = "3-Shift Rotating",
        description = "7 Early, 2 Off, 7 Late, 2 Off, 7 Night, 3 Off (28-day 3-shift rotation)",
        cycleLengthDays = 28
    ),
    STANDARD_MON_FRI(
        displayName = "Mon–Fri Standard (5-On 2-Off)",
        description = "Monday to Friday working days with Saturday and Sunday off",
        cycleLengthDays = 7
    )
}

object ShiftPatternGenerator {

    /**
     * Generates a multi-month shift schedule map (Key: "YYYY-MM" -> Map<DayOfMonth, Hours>)
     * based on an anchor reference date and a specified rotational cycle.
     */
    fun generatePatternShifts(
        year: Int,
        startMonth: Int = 1, // 1..12
        endMonth: Int = 12,  // 1..12
        anchorYear: Int = year,
        anchorMonth: Int = startMonth,
        anchorDay: Int = 1,
        pattern: ShiftPatternType = ShiftPatternType.FOUR_ON_FOUR_OFF,
        dayHours: Double = 12.0,
        nightHours: Double = 12.0
    ): Map<String, Map<Int, Double>> {
        val anchorCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, anchorYear)
            set(Calendar.MONTH, anchorMonth - 1)
            set(Calendar.DAY_OF_MONTH, anchorDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val anchorTimeMillis = anchorCal.timeInMillis
        val msPerDay = 24 * 60 * 60 * 1000L

        val result = mutableMapOf<String, MutableMap<Int, Double>>()

        for (m in startMonth..endMonth) {
            val ymKey = "$year-$m"
            val monthMap = mutableMapOf<Int, Double>()

            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, m - 1)
            }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (d in 1..daysInMonth) {
                cal.set(Calendar.DAY_OF_MONTH, d)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val diffDays = ((cal.timeInMillis - anchorTimeMillis) / msPerDay).toInt()

                val hours = when (pattern) {
                    ShiftPatternType.FOUR_ON_FOUR_OFF -> {
                        // 8-day cycle: Days 0..3 ON, Days 4..7 OFF
                        val cycleDay = ((diffDays % 8) + 8) % 8
                        if (cycleDay < 4) dayHours else 0.0
                    }

                    ShiftPatternType.CONTINENTAL_2_2_3 -> {
                        // 28-day standard Continental:
                        // Week 1: 2D, 2N, 3 Off (days 0..1 Day, 2..3 Night, 4..6 Off)
                        // Week 2: 2D, 3N, 2 Off (days 7..8 Day, 9..11 Night, 12..13 Off)
                        // Week 3: 3D, 2N, 2 Off (days 14..16 Day, 17..18 Night, 19..20 Off)
                        // Week 4: 2D, 2N, 3 Off (days 21..22 Day, 23..24 Night, 25..27 Off)
                        val cycleDay = ((diffDays % 28) + 28) % 28
                        when (cycleDay) {
                            0, 1 -> dayHours
                            2, 3 -> nightHours
                            4, 5, 6 -> 0.0
                            7, 8 -> dayHours
                            9, 10, 11 -> nightHours
                            12, 13 -> 0.0
                            14, 15, 16 -> dayHours
                            17, 18 -> nightHours
                            19, 20 -> 0.0
                            21, 22 -> dayHours
                            23, 24 -> nightHours
                            else -> 0.0
                        }
                    }

                    ShiftPatternType.PITMAN_2_3_2 -> {
                        // 14-day cycle: 2 On, 2 Off, 3 On, 2 Off, 2 On, 3 Off
                        val cycleDay = ((diffDays % 14) + 14) % 14
                        when (cycleDay) {
                            0, 1 -> dayHours
                            2, 3 -> 0.0
                            4, 5, 6 -> dayHours
                            7, 8 -> 0.0
                            9, 10 -> dayHours
                            else -> 0.0
                        }
                    }

                    ShiftPatternType.THREE_SHIFT_ROTATING -> {
                        // 28-day cycle: 7 Early (8h), 2 Off, 7 Late (8h), 2 Off, 7 Night (8h), 3 Off
                        val cycleDay = ((diffDays % 28) + 28) % 28
                        when (cycleDay) {
                            in 0..6 -> dayHours
                            7, 8 -> 0.0
                            in 9..15 -> dayHours
                            16, 17 -> 0.0
                            in 18..24 -> nightHours
                            else -> 0.0
                        }
                    }

                    ShiftPatternType.STANDARD_MON_FRI -> {
                        val dow = cal.get(Calendar.DAY_OF_WEEK)
                        if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) dayHours else 0.0
                    }
                }

                if (hours > 0.0) {
                    monthMap[d] = hours
                }
            }

            result[ymKey] = monthMap
        }

        return result
    }
}
