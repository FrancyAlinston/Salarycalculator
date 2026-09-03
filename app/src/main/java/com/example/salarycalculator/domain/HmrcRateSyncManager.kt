package com.example.salarycalculator.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Dynamic UK HMRC statutory tax rates model.
 * Enables live JSON configuration updates from remote HMRC / cloud endpoints with offline fallbacks.
 */
@Serializable
data class HmrcDynamicRates(
    val version: String = "2024/2025.1",
    val lastUpdated: String = "2024-04-06",
    val personalAllowanceAnnual: Double = 12570.0,
    val basicRateLimitAnnual: Double = 37700.0,
    val additionalRateThresholdAnnual: Double = 125140.0,
    val basicRatePercent: Double = 20.0,
    val higherRatePercent: Double = 40.0,
    val additionalRatePercent: Double = 45.0,
    val niPrimaryThresholdMonthly: Double = 1048.0,
    val niUpperEarningsLimitMonthly: Double = 4189.0,
    val niMainRatePercent: Double = 8.0,
    val niAdditionalRatePercent: Double = 2.0,
    val marriageAllowanceTransfer: Double = 1260.0,
    val blindPersonsAllowance: Double = 3070.0,
    val nationalLivingWage21Plus: Double = 12.21,
    val nationalMinimumWage18To20: Double = 10.00,
    val nationalMinimumWageApprentice: Double = 7.55
)

object HmrcRateSyncManager {
    val STATUTORY_DEFAULT_RATES = HmrcDynamicRates()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Serializes dynamic rates into JSON string.
     */
    fun toJson(rates: HmrcDynamicRates): String {
        return json.encodeToString(rates)
    }

    /**
     * Parses JSON string into HmrcDynamicRates with fallback to statutory defaults.
     */
    fun fromJson(jsonStr: String): HmrcDynamicRates {
        if (jsonStr.isBlank()) return STATUTORY_DEFAULT_RATES
        return try {
            json.decodeFromString<HmrcDynamicRates>(jsonStr)
        } catch (_: Exception) {
            STATUTORY_DEFAULT_RATES
        }
    }

    /**
     * Fetches rates from a remote HTTP/HTTPS JSON endpoint.
     */
    suspend fun fetchRemoteRates(endpointUrl: String): Result<HmrcDynamicRates> = withContext(Dispatchers.IO) {
        try {
            val url = URL(endpointUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val content = reader.readText()
                reader.close()
                val parsed = json.decodeFromString<HmrcDynamicRates>(content)
                Result.success(parsed)
            } else {
                Result.failure(Exception("HTTP Error code: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
