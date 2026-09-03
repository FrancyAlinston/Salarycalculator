package com.example.salarycalculator.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class FxSyncResult(
    val eurRate: Double,
    val usdRate: Double,
    val timestamp: String,
    val isLive: Boolean = true,
    val message: String = "Success"
)

object LiveFxSyncEngine {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Attempts to fetch live GBP exchange rates from an open currency API (e.g. frankfurter / open.er-api).
     * Falls back to high-accuracy statutory defaults on network unavailability.
     */
    suspend fun fetchLiveRates(customEndpointUrl: String? = null): FxSyncResult = withContext(Dispatchers.IO) {
        val targetUrl = customEndpointUrl ?: "https://open.er-api.com/v6/latest/GBP"
        try {
            val url = URL(targetUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val raw = reader.readText()
                reader.close()

                // Parse rates from standard JSON payload
                var eur = ConvertedCurrencies.DEFAULT_EUR_RATE
                var usd = ConvertedCurrencies.DEFAULT_USD_RATE

                val eurMatch = """"EUR"\s*:\s*([\d.]+)""".toRegex().find(raw)
                if (eurMatch != null) {
                    eur = eurMatch.groupValues[1].toDoubleOrNull() ?: ConvertedCurrencies.DEFAULT_EUR_RATE
                }

                val usdMatch = """"USD"\s*:\s*([\d.]+)""".toRegex().find(raw)
                if (usdMatch != null) {
                    usd = usdMatch.groupValues[1].toDoubleOrNull() ?: ConvertedCurrencies.DEFAULT_USD_RATE
                }

                FxSyncResult(
                    eurRate = eur,
                    usdRate = usd,
                    timestamp = "Live Network Sync",
                    isLive = true,
                    message = "Live exchange rates successfully synchronized."
                )
            } else {
                FxSyncResult(
                    eurRate = ConvertedCurrencies.DEFAULT_EUR_RATE,
                    usdRate = ConvertedCurrencies.DEFAULT_USD_RATE,
                    timestamp = "Default Fallback",
                    isLive = false,
                    message = "HTTP ${connection.responseCode}: Using fallback rates."
                )
            }
        } catch (e: Exception) {
            FxSyncResult(
                eurRate = ConvertedCurrencies.DEFAULT_EUR_RATE,
                usdRate = ConvertedCurrencies.DEFAULT_USD_RATE,
                timestamp = "Offline Default",
                isLive = false,
                message = "Offline: ${e.localizedMessage ?: "Using default rates"}"
            )
        }
    }
}
