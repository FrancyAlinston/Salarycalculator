package com.example.salarycalculator.wear.domain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.roundToInt

data class WearWeatherSnapshot(
    val tempCelsius: Int,
    val conditionShort: String,
    val highCelsius: Int,
    val lowCelsius: Int,
    val weatherCode: Int,
    val updatedAtEpochMs: Long
) {
    val formattedTemp: String get() = "${tempCelsius}°C"
    val formattedRange: String get() = "H:${highCelsius}° L:${lowCelsius}°"
}

data class WearTelemetrySnapshot(
    val stepsToday: Int,
    val stepGoal: Int = 8000,
    val weather: WearWeatherSnapshot
) {
    val stepProgressFraction: Float
        get() = (stepsToday.toFloat() / max(1, stepGoal).toFloat()).coerceIn(0f, 1f)
}

/**
 * Provides real-time Step Count (via hardware Sensor.TYPE_STEP_COUNTER) and
 * local Weather (via Open-Meteo API with SharedPreferences offline cache) for
 * the Casio Classic Digital Watch Face, Quick Tile, and Watch UI.
 */
object WearTelemetryManager {

    private const val PREFS_NAME = "wear_casio_telemetry_prefs"
    private const val KEY_STEP_DATE = "step_baseline_date"
    private const val KEY_STEP_BASELINE = "step_baseline_raw"
    private const val KEY_STEPS_TODAY = "steps_today_cached"
    private const val KEY_WEATHER_TEMP = "weather_temp_c"
    private const val KEY_WEATHER_COND = "weather_cond_short"
    private const val KEY_WEATHER_HIGH = "weather_high_c"
    private const val KEY_WEATHER_LOW = "weather_low_c"
    private const val KEY_WEATHER_CODE = "weather_code"
    private const val KEY_WEATHER_TS = "weather_updated_ms"
    private const val KEY_LAT = "weather_lat"
    private const val KEY_LON = "weather_lon"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var sensorRegistered = false
    private var stepListener: SensorEventListener? = null

    fun ensureStepSensorListening(context: Context) {
        if (sensorRegistered) return
        try {
            val appCtx = context.applicationContext
            val sensorManager = appCtx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val rawTotal = event?.values?.firstOrNull()?.toInt() ?: return
                    recordRawStepCounterReading(appCtx, rawTotal)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorRegistered = sensorManager.registerListener(
                listener,
                stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            stepListener = listener
        } catch (_: Exception) {
        }
    }

    fun recordRawStepCounterReading(context: Context, rawCumulativeSteps: Int) {
        if (rawCumulativeSteps < 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_STEP_DATE, null)
        var baseline = prefs.getInt(KEY_STEP_BASELINE, -1)

        if (savedDate != todayStr || baseline < 0 || rawCumulativeSteps < baseline) {
            baseline = rawCumulativeSteps
            prefs.edit()
                .putString(KEY_STEP_DATE, todayStr)
                .putInt(KEY_STEP_BASELINE, baseline)
                .putInt(KEY_STEPS_TODAY, 0)
                .apply()
        } else {
            val todaySteps = max(0, rawCumulativeSteps - baseline)
            prefs.edit()
                .putInt(KEY_STEPS_TODAY, todaySteps)
                .apply()
        }
    }

    fun getSnapshot(context: Context): WearTelemetrySnapshot {
        ensureStepSensorListening(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_STEP_DATE, null)
        val cachedSteps = if (savedDate == todayStr) {
            prefs.getInt(KEY_STEPS_TODAY, 0)
        } else {
            0
        }

        // If hardware sensor hasn't emitted yet in this boot session, provide cached steps
        val temp = prefs.getInt(KEY_WEATHER_TEMP, 11)
        val cond = prefs.getString(KEY_WEATHER_COND, "CLEAR") ?: "CLEAR"
        val high = prefs.getInt(KEY_WEATHER_HIGH, 14)
        val low = prefs.getInt(KEY_WEATHER_LOW, 7)
        val code = prefs.getInt(KEY_WEATHER_CODE, 0)
        val ts = prefs.getLong(KEY_WEATHER_TS, 0L)

        return WearTelemetrySnapshot(
            stepsToday = cachedSteps,
            stepGoal = 8000,
            weather = WearWeatherSnapshot(
                tempCelsius = temp,
                conditionShort = cond,
                highCelsius = high,
                lowCelsius = low,
                weatherCode = code,
                updatedAtEpochMs = ts
            )
        )
    }

    /**
     * Refreshes local weather from Open-Meteo if cache is older than 20 minutes.
     */
    suspend fun refreshWeatherIfNeeded(context: Context, force: Boolean = false): WearTelemetrySnapshot =
        withContext(Dispatchers.IO) {
            ensureStepSensorListening(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastTs = prefs.getLong(KEY_WEATHER_TS, 0L)

            if (!force && (now - lastTs) < 20 * 60 * 1000L && lastTs > 0L) {
                return@withContext getSnapshot(context)
            }

            try {
                var lat = prefs.getFloat(KEY_LAT, 51.5074f).toDouble()
                var lon = prefs.getFloat(KEY_LON, -0.1278f).toDouble()

                // Attempt lightweight IP geolocation once per day
                if (lastTs == 0L) {
                    try {
                        val geoConn = (URL("https://ipwho.is/").openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 2500
                            readTimeout = 2500
                        }
                        if (geoConn.responseCode in 200..299) {
                            val geoBody = BufferedReader(InputStreamReader(geoConn.inputStream, "UTF-8")).use { it.readText() }
                            val geoObj = json.parseToJsonElement(geoBody).jsonObject
                            val gLat = geoObj["latitude"]?.jsonPrimitive?.doubleOrNull
                            val gLon = geoObj["longitude"]?.jsonPrimitive?.doubleOrNull
                            if (gLat != null && gLon != null) {
                                lat = gLat
                                lon = gLon
                                prefs.edit()
                                    .putFloat(KEY_LAT, lat.toFloat())
                                    .putFloat(KEY_LON, lon.toFloat())
                                    .apply()
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3500
                    readTimeout = 3500
                }
                if (conn.responseCode in 200..299) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                    val root = json.parseToJsonElement(body).jsonObject
                    val currentObj = root["current"]?.jsonObject
                    val dailyObj = root["daily"]?.jsonObject

                    val tempC = currentObj?.get("temperature_2m")?.jsonPrimitive?.doubleOrNull?.roundToInt() ?: 11
                    val wCode = currentObj?.get("weather_code")?.jsonPrimitive?.intOrNull ?: 0
                    val maxArr = dailyObj?.get("temperature_2m_max")?.jsonArray
                    val minArr = dailyObj?.get("temperature_2m_min")?.jsonArray
                    val highC = maxArr?.firstOrNull()?.jsonPrimitive?.doubleOrNull?.roundToInt() ?: (tempC + 3)
                    val lowC = minArr?.firstOrNull()?.jsonPrimitive?.doubleOrNull?.roundToInt() ?: (tempC - 3)
                    val condStr = mapWmoCodeToLabel(wCode)

                    prefs.edit()
                        .putInt(KEY_WEATHER_TEMP, tempC)
                        .putString(KEY_WEATHER_COND, condStr)
                        .putInt(KEY_WEATHER_HIGH, highC)
                        .putInt(KEY_WEATHER_LOW, lowC)
                        .putInt(KEY_WEATHER_CODE, wCode)
                        .putLong(KEY_WEATHER_TS, now)
                        .apply()
                }
            } catch (_: Exception) {
            }

            getSnapshot(context)
        }

    fun mapWmoCodeToLabel(code: Int): String {
        return when (code) {
            0 -> "CLEAR"
            1 -> "FAIR"
            2 -> "PT CLOUD"
            3 -> "OVERCAST"
            45, 48 -> "FOGGY"
            51, 53, 55, 56, 57 -> "DRIZZLE"
            61, 63, 65, 66, 67 -> "RAIN"
            71, 73, 75, 77 -> "SNOW"
            80, 81, 82 -> "SHOWERS"
            85, 86 -> "SNOW SHW"
            95, 96, 99 -> "STORM"
            else -> "CLEAR"
        }
    }

    /**
     * Computes elapsed fraction (0f..1f) of an active 20:00 -> 08:00 night duty shift (`[ACTION: FEAT-932]`).
     */
    fun computeShiftProgressFraction(hasDuty: Boolean, nowTime: LocalTime = LocalTime.now()): Float {
        if (!hasDuty) return 0f
        val hour = nowTime.hour + (nowTime.minute / 60f)
        return when {
            hour >= 20f -> ((hour - 20f) / 12f).coerceIn(0.05f, 1f)
            hour < 8f -> (((hour + 4f)) / 12f).coerceIn(0.05f, 1f)
            else -> 1f // Full ready ring before shift starts
        }
    }
}
