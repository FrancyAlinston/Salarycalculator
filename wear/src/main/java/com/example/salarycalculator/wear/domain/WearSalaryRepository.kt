package com.example.salarycalculator.wear.domain

import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.example.salarycalculator.wear.complication.WearShiftComplicationService
import com.example.salarycalculator.wear.tile.WearShiftTileService
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val Context.wearDataStore by preferencesDataStore(name = "wear_salary_prefs")

class WearSalaryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        val HOURLY_RATE_KEY = doublePreferencesKey("wear_hourly_rate")
        val ANNUAL_LEAVE_RATE_KEY = doublePreferencesKey("wear_annual_leave_rate")
        val TAX_CODE_KEY = stringPreferencesKey("wear_tax_code")
        val STANDARD_SHIFT_HOURS_KEY = doublePreferencesKey("wear_standard_shift_hours")
        val OVERTIME_MULTIPLIER_KEY = doublePreferencesKey("wear_overtime_multiplier")
        val PENSION_RATE_KEY = doublePreferencesKey("wear_pension_rate")
        val SHIFTS_JSON_KEY = stringPreferencesKey("wear_shifts_json")
        val TAX_REGION_KEY = stringPreferencesKey("wear_tax_region")
        val USE_CUTOFF_CYCLE_KEY = booleanPreferencesKey("wear_use_cutoff_cycle")
        val NAS_URL_KEY = stringPreferencesKey("wear_nas_url")

        const val DEFAULT_NAS_BACKUP_URL = "http://192.168.8.100:8085/api/backup"

        /**
         * Pre-seeded 2026 Rota Schedule matching the user's UGREEN NAS & Phone database
         * so the Samsung Galaxy Watch 8 Classic displays accurate shifts immediately on launch.
         */
        val DEFAULT_2026_FLAT_SHIFTS: Map<String, Double> by lazy {
            val nested = mapOf(
                "2026-01" to mapOf(3 to 12.0, 4 to 12.0, 5 to 12.0, 6 to 12.0, 11 to 12.0, 12 to 12.0, 13 to 12.0, 14 to 12.0, 19 to 12.0, 20 to 12.0, 21 to 12.0, 22 to 12.0, 27 to 12.0, 28 to 12.0, 29 to 12.0, 30 to 12.0),
                "2026-02" to mapOf(4 to 12.0, 5 to 12.0, 6 to 12.0, 7 to 12.0, 12 to 12.0, 13 to 12.0, 14 to 12.0, 15 to 12.0, 20 to 12.0, 21 to 12.0, 22 to 12.0, 23 to 12.0, 28 to 12.0),
                "2026-03" to mapOf(1 to 12.0, 2 to 12.0, 3 to 12.0, 8 to 12.0, 9 to 12.0, 10 to 12.0, 11 to 12.0, 16 to 12.0, 17 to 12.0, 18 to 12.0, 19 to 12.0, 24 to 12.0, 25 to 12.0, 26 to 12.0, 27 to 12.0),
                "2026-04" to mapOf(2 to 12.0, 3 to 12.0, 4 to 12.0, 5 to 12.0, 10 to 12.0, 11 to 12.0, 12 to 12.0, 13 to 12.0, 18 to 12.0, 19 to 12.0, 20 to 12.0, 21 to 12.0, 26 to 12.0, 27 to 12.0, 28 to 12.0, 29 to 12.0),
                "2026-05" to mapOf(4 to 12.0, 5 to 12.0, 6 to 12.0, 7 to 12.0, 12 to 12.0, 13 to 12.0, 14 to 12.0, 15 to 12.0, 20 to 12.0, 21 to 12.0, 22 to 12.0, 23 to 12.0, 28 to 12.0, 29 to 12.0, 30 to 12.0, 31 to 12.0),
                "2026-06" to mapOf(5 to 12.0, 6 to 12.0, 7 to 12.0, 8 to 12.0, 13 to 12.0, 14 to 12.0, 15 to 12.0, 16 to 12.0, 21 to 12.0, 22 to 12.0, 23 to 12.0, 24 to 12.0, 29 to 12.0, 30 to 12.0),
                "2026-07" to mapOf(1 to 12.0, 3 to 12.0, 5 to 12.0, 6 to 12.0, 7 to 12.0, 11 to 12.0, 12 to 12.0, 15 to -999.0, 16 to -999.0, 17 to -999.0, 20 to 12.0, 21 to 12.0, 22 to 12.0, 25 to 12.0, 26 to 12.0, 28 to 12.0, 29 to 12.0, 30 to 12.0),
                "2026-08" to mapOf(2 to 12.0, 4 to 12.0, 5 to 12.0, 6 to 12.0, 7 to 12.0, 10 to 12.0, 11 to 12.0, 12 to 12.0, 15 to -999.0, 16 to -999.0, 18 to 12.0, 19 to 12.0, 20 to 12.0, 21 to 12.0, 24 to 12.0, 25 to 12.0, 29 to 12.0, 30 to 12.0),
                "2026-09" to mapOf(1 to 12.0, 3 to 12.0, 4 to 12.0, 7 to 12.0, 8 to 12.0, 11 to -12.0, 12 to 12.0, 15 to 12.0, 16 to 12.0, 17 to 12.0, 18 to 12.0, 20 to -12.0, 21 to 12.0, 22 to 12.0, 26 to 12.0, 27 to 12.0, 29 to 12.0, 30 to 12.0),
                "2026-10" to mapOf(1 to 12.0, 2 to 12.0, 6 to 12.0, 7 to 12.0, 8 to 12.0, 9 to 12.0, 11 to 12.0, 13 to 12.0, 14 to 12.0, 15 to 12.0, 16 to 12.0, 19 to 12.0, 20 to 12.0, 21 to -12.0, 24 to 12.0, 25 to -999.0, 26 to 12.0, 29 to 12.0, 30 to 12.0, 31 to 12.0)
            )
            val result = mutableMapOf<String, Double>()
            nested.forEach { (ym, days) ->
                days.forEach { (d, hrs) ->
                    result[String.format(Locale.ROOT, "%s-%02d", ym, d)] = hrs
                }
            }
            result
        }
    }

    val hourlyRateFlow: Flow<Double> = context.wearDataStore.data.map { prefs ->
        prefs[HOURLY_RATE_KEY] ?: 12.82
    }

    val taxCodeFlow: Flow<String> = context.wearDataStore.data.map { prefs ->
        prefs[TAX_CODE_KEY] ?: "1257L"
    }

    val standardShiftHoursFlow: Flow<Double> = context.wearDataStore.data.map { prefs ->
        prefs[STANDARD_SHIFT_HOURS_KEY] ?: 12.0
    }

    val useCutoffCycleFlow: Flow<Boolean> = context.wearDataStore.data.map { prefs ->
        prefs[USE_CUTOFF_CYCLE_KEY] ?: true
    }

    val shiftsFlow: Flow<Map<String, Double>> = context.wearDataStore.data.map { prefs ->
        val raw = prefs[SHIFTS_JSON_KEY]
        if (raw.isNullOrBlank()) {
            DEFAULT_2026_FLAT_SHIFTS
        } else {
            val decoded = parseAnyShiftJson(raw)
            if (decoded.isEmpty()) DEFAULT_2026_FLAT_SHIFTS else decoded
        }
    }

    /**
     * Triggers an immediate update of the Samsung Galaxy Watch 8 Classic Quick Tile (FEAT-927)
     * and Watch Face Complication (FEAT-928).
     */
    fun requestTileAndComplicationRefresh() {
        try {
            TileService.getUpdater(context).requestUpdate(WearShiftTileService::class.java)
        } catch (_: Exception) {
        }
        try {
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, WearShiftComplicationService::class.java)
            ).requestUpdateAll()
        } catch (_: Exception) {
        }
    }

    /**
     * Evaluates whether the specified date has duty or not ("Night Duty" vs "Off Duty")
     * along with the 7-day weekly rota strip for the Quick Tile and Complication.
     */
    suspend fun getTodayDutySummary(date: LocalDate = LocalDate.now()): WearTodayDutySummary {
        val prefs = context.wearDataStore.data.first()
        val stdHours = prefs[STANDARD_SHIFT_HOURS_KEY] ?: 12.0
        val rawShifts = prefs[SHIFTS_JSON_KEY]
        val shifts: Map<String, Double> = if (rawShifts.isNullOrBlank()) {
            DEFAULT_2026_FLAT_SHIFTS
        } else {
            parseAnyShiftJson(rawShifts).ifEmpty { DEFAULT_2026_FLAT_SHIFTS }
        }
        return TaxCalculator.resolveTodayDutySummary(date, shifts, stdHours)
    }

    fun parseAnyShiftJson(raw: String): Map<String, Double> {
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<String, Double>()
        try {
            val element = json.parseToJsonElement(raw)
            if (element is JsonObject) {
                element.forEach { (key, value) ->
                    if (value is JsonObject) {
                        val parts = key.split("-")
                        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
                        value.forEach { (dStr, hElem) ->
                            val d = dStr.toIntOrNull()
                            val h = hElem.jsonPrimitive.doubleOrNull
                            if (d != null && h != null && h != 0.0) {
                                result[String.format(Locale.ROOT, "%04d-%02d-%02d", y, m, d)] = h
                            }
                        }
                    } else {
                        val h = value.jsonPrimitive.doubleOrNull
                        if (h != null && h != 0.0) {
                            val parts = key.split("-")
                            if (parts.size == 3) {
                                val y = parts[0].toIntOrNull() ?: 2026
                                val m = parts[1].toIntOrNull() ?: 1
                                val d = parts[2].toIntOrNull() ?: 1
                                result[String.format(Locale.ROOT, "%04d-%02d-%02d", y, m, d)] = h
                            } else {
                                result[key] = h
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    suspend fun setHourlyRate(rate: Double) {
        context.wearDataStore.edit { prefs ->
            prefs[HOURLY_RATE_KEY] = rate
        }
    }

    suspend fun setTaxCode(code: String) {
        context.wearDataStore.edit { prefs ->
            prefs[TAX_CODE_KEY] = code
        }
    }

    suspend fun setStandardShiftHours(hours: Double) {
        context.wearDataStore.edit { prefs ->
            prefs[STANDARD_SHIFT_HOURS_KEY] = hours
        }
        requestTileAndComplicationRefresh()
    }

    suspend fun setUseCutoffCycle(useCutoff: Boolean) {
        context.wearDataStore.edit { prefs ->
            prefs[USE_CUTOFF_CYCLE_KEY] = useCutoff
        }
    }

    suspend fun toggleShift(dateStr: String, standardHours: Double = 12.0) {
        context.wearDataStore.edit { prefs ->
            val raw = prefs[SHIFTS_JSON_KEY]
            val currentMap: MutableMap<String, Double> = if (raw.isNullOrBlank()) {
                DEFAULT_2026_FLAT_SHIFTS.toMutableMap()
            } else {
                parseAnyShiftJson(raw).ifEmpty { DEFAULT_2026_FLAT_SHIFTS }.toMutableMap()
            }

            val currentHours = currentMap[dateStr] ?: 0.0
            val nextHours = when {
                currentHours == 0.0 -> standardHours // 1. Night Duty (12.0h)
                currentHours > 0.0 -> -standardHours // 2. Overtime (-12.0h)
                currentHours == -standardHours || (currentHours < 0.0 && currentHours != TaxCalculator.ANNUAL_LEAVE_SENTINEL && currentHours != TaxCalculator.SSP_SENTINEL) ->
                    TaxCalculator.ANNUAL_LEAVE_SENTINEL // 3. Annual Leave (A/L)
                currentHours == TaxCalculator.ANNUAL_LEAVE_SENTINEL ->
                    TaxCalculator.SSP_SENTINEL // 4. Sick Leave (SSP)
                else -> 0.0 // 5. Off / Rest
            }

            if (nextHours == 0.0) {
                currentMap.remove(dateStr)
            } else {
                currentMap[dateStr] = nextHours
            }

            prefs[SHIFTS_JSON_KEY] = json.encodeToString(currentMap)
        }
        requestTileAndComplicationRefresh()
        val currentShifts = shiftsFlow.first()
        broadcastShiftChangeToPhoneAndNas(currentShifts)
    }

    suspend fun setShiftForDay(dateStr: String, hours: Double) {
        context.wearDataStore.edit { prefs ->
            val raw = prefs[SHIFTS_JSON_KEY]
            val currentMap: MutableMap<String, Double> = if (raw.isNullOrBlank()) {
                DEFAULT_2026_FLAT_SHIFTS.toMutableMap()
            } else {
                parseAnyShiftJson(raw).ifEmpty { DEFAULT_2026_FLAT_SHIFTS }.toMutableMap()
            }
            if (hours == 0.0) {
                currentMap.remove(dateStr)
            } else {
                currentMap[dateStr] = hours
            }
            prefs[SHIFTS_JSON_KEY] = json.encodeToString(currentMap)
        }
        requestTileAndComplicationRefresh()
        val currentShifts = shiftsFlow.first()
        broadcastShiftChangeToPhoneAndNas(currentShifts)
    }

    suspend fun updateAllShifts(shifts: Map<String, Double>) {
        if (shifts.isEmpty()) return
        context.wearDataStore.edit { prefs ->
            prefs[SHIFTS_JSON_KEY] = json.encodeToString(shifts)
        }
        requestTileAndComplicationRefresh()
        broadcastShiftChangeToPhoneAndNas(shifts)
    }

    /**
     * Pulls latest shifts & settings from the UGOS NAS SQLite server over Wi-Fi.
     */
    /**
     * Broadcasts updated shift changes from the Watch to all connected Android phones
     * via Google Play Services Wearable MessageClient, and directly to the UGOS NAS SQLite
     * backend if Wi-Fi connectivity is available.
     */
    suspend fun broadcastShiftChangeToPhoneAndNas(shifts: Map<String, Double>) = withContext(Dispatchers.IO) {
        if (shifts.isEmpty()) return@withContext
        val payload = json.encodeToString(shifts).toByteArray(Charsets.UTF_8)

        // 1. Send to all connected Android phone nodes via Wearable MessageClient
        try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes, 2, TimeUnit.SECONDS)
            val messageClient = Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/salary_sync/watch_to_phone_update", payload)
            }
        } catch (_: Exception) {}

        // 2. Battery-Safe Direct Wi-Fi sync: ONLY if watch Wi-Fi is already actively connected
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            val isWifiConnected = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true

            if (isWifiConnected) {
                val prefs = context.wearDataStore.data.first()
                val endpoint = prefs[NAS_URL_KEY] ?: DEFAULT_NAS_BACKUP_URL
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 1500
                    readTimeout = 1500
                }
                if (conn.responseCode in 200..299) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                    val root = json.parseToJsonElement(body).jsonObject
                    val backupObj = (if (root.containsKey("backup") && root["backup"] is JsonObject) root["backup"]!!.jsonObject else root).toMutableMap()

                    // Convert flat shifts to nested JSON format
                    val nested = mutableMapOf<String, MutableMap<Int, Double>>()
                    shifts.forEach { (dateStr, h) ->
                        val parts = dateStr.split("-")
                        if (parts.size >= 3) {
                            val ym = "${parts[0]}-${parts[1]}"
                            val d = parts[2].toIntOrNull() ?: return@forEach
                            nested.getOrPut(ym) { mutableMapOf() }[d] = h
                        }
                    }
                    backupObj["annualShiftScheduleJson"] = kotlinx.serialization.json.JsonPrimitive(json.encodeToString(nested))
                    backupObj["timestamp"] = kotlinx.serialization.json.JsonPrimitive(System.currentTimeMillis())

                    val postPayload = json.encodeToString(mapOf(
                        "email" to "francyalinston@gmail.com",
                        "bundle" to backupObj
                    ))

                    val postConn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        connectTimeout = 2000
                        readTimeout = 2000
                    }
                    OutputStreamWriter(postConn.outputStream, "UTF-8").use { writer ->
                        writer.write(postPayload)
                        writer.flush()
                    }
                    postConn.responseCode
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun syncFromNasServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Guard: Only query NAS directly if Wi-Fi is actively connected to avoid waking radios
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            val isWifiConnected = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
            if (!isWifiConnected) return@withContext false

            val prefs = context.wearDataStore.data.first()
            val endpoint = prefs[NAS_URL_KEY] ?: DEFAULT_NAS_BACKUP_URL
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 2500
                readTimeout = 2500
            }
            if (conn.responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                val root = json.parseToJsonElement(body).jsonObject
                val backupObj = if (root.containsKey("backup") && root["backup"] is JsonObject) {
                    root["backup"]!!.jsonObject
                } else {
                    root
                }

                val scheduleStr = backupObj["annualShiftScheduleJson"]?.jsonPrimitive?.content ?: ""
                val parsedShifts = parseAnyShiftJson(scheduleStr)
                val rate = backupObj["defaultHourlyRate"]?.jsonPrimitive?.doubleOrNull
                    ?: backupObj["hourlyRate"]?.jsonPrimitive?.doubleOrNull
                val alRate = backupObj["annualLeaveHourlyRate"]?.jsonPrimitive?.doubleOrNull
                val stdHours = backupObj["defaultHoursPerDay"]?.jsonPrimitive?.doubleOrNull
                val taxCode = backupObj["taxCode"]?.jsonPrimitive?.content

                context.wearDataStore.edit { editPrefs ->
                    if (parsedShifts.isNotEmpty()) {
                        editPrefs[SHIFTS_JSON_KEY] = json.encodeToString(parsedShifts)
                    }
                    if (rate != null && rate > 0.0) editPrefs[HOURLY_RATE_KEY] = rate
                    if (alRate != null && alRate > 0.0) editPrefs[ANNUAL_LEAVE_RATE_KEY] = alRate
                    if (stdHours != null && stdHours > 0.0) editPrefs[STANDARD_SHIFT_HOURS_KEY] = stdHours
                    if (!taxCode.isNullOrBlank()) editPrefs[TAX_CODE_KEY] = taxCode
                }
                requestTileAndComplicationRefresh()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Computes the exact Payroll Cutoff Split & Salary Report for any YearMonth.
     */
    suspend fun calculateMonthSalary(
        yearMonth: YearMonth,
        useCutoffOverride: Boolean? = null
    ): SalaryReport {
        val prefs = context.wearDataStore.data.first()
        val rate = prefs[HOURLY_RATE_KEY] ?: 12.82
        val alRate = prefs[ANNUAL_LEAVE_RATE_KEY] ?: rate
        val taxCode = prefs[TAX_CODE_KEY] ?: "1257L"
        val stdShiftHours = prefs[STANDARD_SHIFT_HOURS_KEY] ?: 12.0
        val otMultiplier = prefs[OVERTIME_MULTIPLIER_KEY] ?: 1.0
        val pensionRate = prefs[PENSION_RATE_KEY] ?: 0.0
        val useCutoff = useCutoffOverride ?: (prefs[USE_CUTOFF_CYCLE_KEY] ?: true)

        val rawShifts = prefs[SHIFTS_JSON_KEY]
        val shifts: Map<String, Double> = if (rawShifts.isNullOrBlank()) {
            DEFAULT_2026_FLAT_SHIFTS
        } else {
            parseAnyShiftJson(rawShifts).ifEmpty { DEFAULT_2026_FLAT_SHIFTS }
        }

        val (payDay, cutoffDay) = TaxCalculator.getPayDayAndCutoff(yearMonth)
        val prevYearMonth = yearMonth.minusMonths(1)
        val (_, prevCutoffDay) = TaxCalculator.getPayDayAndCutoff(prevYearMonth)

        val curPrefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val prevPrefix = prevYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

        // 1. Previous month rollover (days > prevCutoffDay)
        var rolloverInShifts = 0
        var rolloverInStdHours = 0.0
        var rolloverInOtHours = 0.0
        var rolloverInAlHours = 0.0

        for (d in (prevCutoffDay + 1)..prevYearMonth.lengthOfMonth()) {
            val key = String.format(Locale.ROOT, "%s-%02d", prevPrefix, d)
            val hrs = shifts[key] ?: 0.0
            if (hrs != 0.0) {
                when {
                    hrs == TaxCalculator.ANNUAL_LEAVE_SENTINEL -> {
                        rolloverInShifts++
                        rolloverInAlHours += stdShiftHours
                    }
                    hrs == TaxCalculator.SSP_SENTINEL -> {
                        // SSP handled separately
                    }
                    hrs < 0.0 -> {
                        rolloverInShifts++
                        rolloverInOtHours += abs(hrs)
                    }
                    hrs > 0.0 -> {
                        rolloverInShifts++
                        val s = min(stdShiftHours, hrs)
                        val o = max(0.0, hrs - stdShiftHours)
                        rolloverInStdHours += s
                        rolloverInOtHours += o
                    }
                }
            }
        }

        // 2. Current month in-cycle (1..cutoffDay) vs post-cutoff rollover out (>cutoffDay) & full calendar month
        var inCycleStdShifts = 0
        var inCycleOtShifts = 0
        var inCycleAlDays = 0
        var inCycleSspDays = 0
        var inCycleStdHours = 0.0
        var inCycleOtHours = 0.0
        var inCycleAlHours = 0.0

        var rolloverOutShifts = 0
        var rolloverOutHours = 0.0

        var calNightShifts = 0
        var calOtShifts = 0
        var calAlDays = 0
        var calSspDays = 0
        var calStdHours = 0.0
        var calOtHours = 0.0
        var calAlHours = 0.0

        for (d in 1..yearMonth.lengthOfMonth()) {
            val key = String.format(Locale.ROOT, "%s-%02d", curPrefix, d)
            val hrs = shifts[key] ?: 0.0
            if (hrs != 0.0) {
                val inCycle = d <= cutoffDay
                when {
                    hrs == TaxCalculator.ANNUAL_LEAVE_SENTINEL -> {
                        calAlDays++
                        calAlHours += stdShiftHours
                        if (inCycle) {
                            inCycleAlDays++
                            inCycleAlHours += stdShiftHours
                        } else {
                            rolloverOutShifts++
                            rolloverOutHours += stdShiftHours
                        }
                    }
                    hrs == TaxCalculator.SSP_SENTINEL -> {
                        calSspDays++
                        if (inCycle) inCycleSspDays++ else rolloverOutShifts++
                    }
                    hrs < 0.0 -> {
                        val absH = abs(hrs)
                        calOtShifts++
                        calOtHours += absH
                        if (inCycle) {
                            inCycleOtShifts++
                            inCycleOtHours += absH
                        } else {
                            rolloverOutShifts++
                            rolloverOutHours += absH
                        }
                    }
                    hrs > 0.0 -> {
                        val s = min(stdShiftHours, hrs)
                        val o = max(0.0, hrs - stdShiftHours)
                        calNightShifts++
                        calStdHours += s
                        calOtHours += o
                        if (inCycle) {
                            inCycleStdShifts++
                            inCycleStdHours += s
                            inCycleOtHours += o
                        } else {
                            rolloverOutShifts++
                            rolloverOutHours += hrs
                        }
                    }
                }
            }
        }

        val split = WearPayrollSplit(
            payDay = payDay,
            cutoffDay = cutoffDay,
            previousCutoffDay = prevCutoffDay,
            inCycleStandardShifts = inCycleStdShifts,
            inCycleOtShifts = inCycleOtShifts,
            inCycleAnnualLeaveDays = inCycleAlDays,
            inCycleSspDays = inCycleSspDays,
            inCycleStandardHours = inCycleStdHours,
            inCycleOtHours = inCycleOtHours,
            inCycleAnnualLeaveHours = inCycleAlHours,
            rolloverInShifts = rolloverInShifts,
            rolloverInStandardHours = rolloverInStdHours,
            rolloverInOtHours = rolloverInOtHours,
            rolloverInAnnualLeaveHours = rolloverInAlHours,
            postCutoffRolloverOutShifts = rolloverOutShifts,
            postCutoffRolloverOutHours = rolloverOutHours,
            calendarNightShifts = calNightShifts,
            calendarOtShifts = calOtShifts,
            calendarAnnualLeaveDays = calAlDays,
            calendarSspDays = calSspDays,
            calendarTotalHours = calStdHours + calOtHours + calAlHours
        )

        val effectiveStdHours = if (useCutoff) split.totalPaidStandardHours else calStdHours
        val effectiveOtHours = if (useCutoff) split.totalPaidOtHours else calOtHours
        val effectiveAlHours = if (useCutoff) split.totalPaidAnnualLeaveHours else calAlHours
        val effectiveSspDays = if (useCutoff) split.inCycleSspDays else calSspDays
        val effectiveShifts = if (useCutoff) split.totalPaidShifts else split.calendarTotalShifts
        val effectiveTotalHours = effectiveStdHours + effectiveOtHours + effectiveAlHours

        val stdPay = effectiveStdHours * rate
        val otPay = effectiveOtHours * rate * otMultiplier
        val alPay = effectiveAlHours * alRate
        val sspPay = max(0, effectiveSspDays - 3) * 23.35
        val grossPay = stdPay + otPay + alPay + sspPay

        // UK tax month: April = 1 .. August = 5 .. March = 12
        val taxMonth = ((yearMonth.monthValue - 4 + 12) % 12) + 1

        return TaxCalculator.calculateTax(
            grossPay = grossPay,
            taxCode = taxCode,
            isMonthly = true,
            pensionRatePercent = pensionRate,
            totalHours = effectiveTotalHours,
            shiftCount = effectiveShifts,
            standardPay = stdPay,
            overtimePay = otPay,
            annualLeavePay = alPay,
            sspPay = sspPay,
            taxMonth = taxMonth,
            payrollSplit = split
        )
    }
}
