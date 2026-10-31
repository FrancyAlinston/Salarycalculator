package com.example.salarycalculator.wear.domain

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.salarycalculator.wear.watchface.WearCasioWatchFaceService
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
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * [ACTION: FEAT-938] Selectable Casio Retro LCD Color Themes on Watch.
 */
enum class CasioColorTheme(
    val id: Int,
    val label: String,
    val lcdLitHex: String,
    val frameHex: String,
    val accentHex: String,
    val subHeaderHex: String
) {
    STEALTH_WHITE(
        id = 0,
        label = "STEALTH",
        lcdLitHex = "#ECFEFF",
        frameHex = "#2563EB",
        accentHex = "#10B981",
        subHeaderHex = "#F59E0B"
    ),
    EL_GREEN(
        id = 1,
        label = "EL GREEN",
        lcdLitHex = "#4ADE80",
        frameHex = "#059669",
        accentHex = "#34D399",
        subHeaderHex = "#A7F3D0"
    ),
    RETRO_AMBER(
        id = 2,
        label = "AMBER",
        lcdLitHex = "#FBBF24",
        frameHex = "#D97706",
        accentHex = "#F59E0B",
        subHeaderHex = "#FDE68A"
    ),
    CYBER_CYAN(
        id = 3,
        label = "CYAN",
        lcdLitHex = "#38BDF8",
        frameHex = "#0284C7",
        accentHex = "#06B6D4",
        subHeaderHex = "#BAE6FD"
    );

    companion object {
        fun fromId(id: Int): CasioColorTheme = entries.firstOrNull { it.id == id } ?: STEALTH_WHITE
    }
}

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
    val weather: WearWeatherSnapshot,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val colorTheme: CasioColorTheme = CasioColorTheme.STEALTH_WHITE,
    val sigEnabled: Boolean = true,
    val almEnabled: Boolean = true
) {
    val stepProgressFraction: Float
        get() = (stepsToday.toFloat() / max(1, stepGoal).toFloat()).coerceIn(0f, 1f)

    val sigAlmLabel: String
        get() = when {
            sigEnabled && almEnabled -> "SIG•ALM ●"
            sigEnabled -> "SIG ● ALM ○"
            almEnabled -> "SIG ○ ALM ●"
            else -> "SIG•ALM ○"
        }
}

/**
 * Provides real-time Step Count (via hardware Sensor.TYPE_STEP_COUNTER),
 * local Weather (via Open-Meteo API with SharedPreferences offline cache),
 * Watch Battery Percentage (`[ACTION: PEND-930]`), Casio Color Themes (`[ACTION: FEAT-938]`),
 * and Casio Hourly Chime / Shift Start Alert (`[ACTION: FEAT-939]`).
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
    private const val KEY_CASIO_THEME = "casio_color_theme_id"
    private const val KEY_SIG_ENABLED = "casio_sig_hourly_chime"
    private const val KEY_ALM_ENABLED = "casio_alm_shift_alert"
    private const val KEY_LAST_CHIME_HOUR = "casio_last_chime_date_hour"
    private const val KEY_START_ON_CASIO = "casio_default_home_screen"

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

    /**
     * [ACTION: PEND-930] Reads real-time watch battery percentage and charging state.
     */
    fun readBatteryState(context: Context): Pair<Int, Boolean> {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (cap in 1..100) {
                cap
            } else {
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) ((level * 100f) / scale.toFloat()).roundToInt().coerceIn(0, 100) else 100
            }
            pct to isCharging
        } catch (_: Exception) {
            100 to false
        }
    }

    /**
     * [ACTION: FEAT-938] Cycles through the 4 classic Casio LCD color themes.
     */
    fun cycleColorTheme(context: Context): WearTelemetrySnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentId = prefs.getInt(KEY_CASIO_THEME, 0)
        val nextId = (currentId + 1) % CasioColorTheme.entries.size
        prefs.edit().putInt(KEY_CASIO_THEME, nextId).apply()
        return getSnapshot(context)
    }

    /**
     * [ACTION: FEAT-939] Cycles Casio Hourly Chime (`SIG`) and Shift Start Alert (`ALM`).
     */
    fun toggleSigAlm(context: Context): WearTelemetrySnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sig = prefs.getBoolean(KEY_SIG_ENABLED, true)
        val alm = prefs.getBoolean(KEY_ALM_ENABLED, true)
        val (nextSig, nextAlm) = when {
            sig && alm -> true to false
            sig && !alm -> false to true
            !sig && alm -> false to false
            else -> true to true
        }
        prefs.edit()
            .putBoolean(KEY_SIG_ENABLED, nextSig)
            .putBoolean(KEY_ALM_ENABLED, nextAlm)
            .apply()
        vibrateCasioDoubleBeep(context)
        return getSnapshot(context)
    }

    /**
     * [ACTION: FEAT-939] Triggers the Casio hourly double-tick (`SIG` at :00) or Night Duty Start alert (`ALM` at 20:00).
     */
    fun checkAndTriggerCasioSignals(
        context: Context,
        zonedDateTime: ZonedDateTime,
        hasDuty: Boolean
    ) {
        if (zonedDateTime.minute != 0 || zonedDateTime.second > 2) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sig = prefs.getBoolean(KEY_SIG_ENABLED, true)
        val alm = prefs.getBoolean(KEY_ALM_ENABLED, true)
        if (!sig && !alm) return

        val hourKey = "${zonedDateTime.toLocalDate()}_${zonedDateTime.hour}"
        val lastTriggered = prefs.getString(KEY_LAST_CHIME_HOUR, null)
        if (lastTriggered == hourKey) return

        val isShiftStartHour = (zonedDateTime.hour == 20 && hasDuty)
        if (isShiftStartHour && alm) {
            prefs.edit().putString(KEY_LAST_CHIME_HOUR, hourKey).apply()
            vibrateCasioShiftAlarm(context)
        } else if (sig) {
            prefs.edit().putString(KEY_LAST_CHIME_HOUR, hourKey).apply()
            vibrateCasioDoubleBeep(context)
        }
    }

    fun vibrateCasioDoubleBeep(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0L, 30L, 65L, 30L), -1)
            )
        } catch (_: Exception) {
        }
    }

    private fun vibrateCasioShiftAlarm(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0L, 60L, 50L, 60L, 120L, 60L, 50L, 60L), -1)
            )
        } catch (_: Exception) {
        }
    }

    fun isCasioDefaultHomeScreen(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_START_ON_CASIO, false)
    }

    fun setCasioDefaultHomeScreen(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_START_ON_CASIO, enabled).apply()
    }

    /**
     * Launches the Wear OS Watch Face / Live Wallpaper selector so the user can activate
     * Casio Night Duty Digital in 1 tap.
     */
    fun promptSetSystemWatchFace(context: Context) {
        val intents = listOf(
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, WearCasioWatchFaceService::class.java)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent("com.google.android.wearable.action.SET_WATCH_FACE").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
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

        val temp = prefs.getInt(KEY_WEATHER_TEMP, 11)
        val cond = prefs.getString(KEY_WEATHER_COND, "CLEAR") ?: "CLEAR"
        val high = prefs.getInt(KEY_WEATHER_HIGH, 14)
        val low = prefs.getInt(KEY_WEATHER_LOW, 7)
        val code = prefs.getInt(KEY_WEATHER_CODE, 0)
        val ts = prefs.getLong(KEY_WEATHER_TS, 0L)
        val theme = CasioColorTheme.fromId(prefs.getInt(KEY_CASIO_THEME, 0))
        val sig = prefs.getBoolean(KEY_SIG_ENABLED, true)
        val alm = prefs.getBoolean(KEY_ALM_ENABLED, true)
        val (batteryPct, isCharging) = readBatteryState(context)

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
            ),
            batteryPercent = batteryPct,
            isCharging = isCharging,
            colorTheme = theme,
            sigEnabled = sig,
            almEnabled = alm
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
            else -> 1f
        }
    }
}
