package com.example.salarycalculator.domain

import java.util.Calendar
import kotlin.math.*

object SolarThemeScheduler {

    // Default reference UK coordinates (London / Central UK: ~51.5° N, ~0.12° W)
    private const val DEFAULT_LATITUDE = 51.5074
    private const val DEFAULT_LONGITUDE = -0.1278

    /**
     * Determines whether it is currently night time (between sunset and sunrise) for the UK.
     */
    fun isSolarNightTime(
        latitude: Double = DEFAULT_LATITUDE,
        longitude: Double = DEFAULT_LONGITUDE
    ): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY) + (calendar.get(Calendar.MINUTE) / 60.0)

        val sunrise = calculateSunrise(dayOfYear, latitude, longitude)
        val sunset = calculateSunset(dayOfYear, latitude, longitude)

        return currentHour < sunrise || currentHour >= sunset
    }

    /**
     * Computes approximate sunrise in decimal hours (e.g. 6.5 = 06:30 AM).
     */
    fun calculateSunrise(dayOfYear: Int, latitude: Double, longitude: Double): Double {
        val declination = 23.45 * sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)))
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)

        val cosHourAngle = -tan(latRad) * tan(decRad)
        val hourAngle = Math.toDegrees(acos(cosHourAngle.coerceIn(-1.0, 1.0)))

        val solarNoon = 12.0 - (longitude / 15.0)
        return solarNoon - (hourAngle / 15.0)
    }

    /**
     * Computes approximate sunset in decimal hours (e.g. 19.5 = 07:30 PM).
     */
    fun calculateSunset(dayOfYear: Int, latitude: Double, longitude: Double): Double {
        val declination = 23.45 * sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)))
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val hourAngle = Math.toDegrees(acos((-tan(latRad) * tan(decRad)).coerceIn(-1.0, 1.0)))

        val solarNoon = 12.0 - (longitude / 15.0)
        return solarNoon + (hourAngle / 15.0)
    }
}
