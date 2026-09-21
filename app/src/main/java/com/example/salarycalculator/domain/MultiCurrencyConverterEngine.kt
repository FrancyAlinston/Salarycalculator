package com.example.salarycalculator.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.max

/**
 * Currency category for multi-currency conversion (Fiat vs Crypto).
 */
enum class CurrencyCategory(val displayName: String) {
    ALL("All Currencies"),
    FIAT("Fiat Currencies"),
    CRYPTO("Crypto Assets")
}

/**
 * Dynamic Multi-Currency & Crypto Rate item.
 * Converts UK GBP (£) gross and net earnings into global currencies and crypto assets.
 */
data class CurrencyRate(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val gbpRate: Double, // e.g. 1 GBP = 1.18 EUR or 1 GBP = 0.000021 BTC
    val category: CurrencyCategory = CurrencyCategory.FIAT
)

enum class PayPeriod(val displayName: String, val annualDivisor: Double) {
    ANNUAL("Annual", 1.0),
    MONTHLY("Monthly", 12.0),
    FOUR_WEEKLY("4-Weekly", 13.0),
    BI_WEEKLY("Bi-Weekly", 26.0),
    WEEKLY("Weekly", 52.0),
    DAILY("Daily (260d)", 260.0),
    HOURLY("Hourly (2080h)", 2080.0)
}

data class ConvertedCurrencyItem(
    val currency: CurrencyRate,
    val convertedAmount: Double,
    val formattedString: String
)

object MultiCurrencyConverterEngine {

    val DEFAULT_RATES = listOf(
        // Fiat Currencies
        CurrencyRate("EUR", "Euro", "€", "🇪🇺", 1.18, CurrencyCategory.FIAT),
        CurrencyRate("USD", "US Dollar", "$", "🇺🇸", 1.31, CurrencyCategory.FIAT),
        CurrencyRate("CAD", "Canadian Dollar", "CA$", "🇨🇦", 1.78, CurrencyCategory.FIAT),
        CurrencyRate("AUD", "Australian Dollar", "A$", "🇦🇺", 1.95, CurrencyCategory.FIAT),
        CurrencyRate("JPY", "Japanese Yen", "¥", "🇯🇵", 192.50, CurrencyCategory.FIAT),
        CurrencyRate("CHF", "Swiss Franc", "Fr", "🇨🇭", 1.12, CurrencyCategory.FIAT),
        CurrencyRate("SGD", "Singapore Dollar", "S$", "🇸🇬", 1.71, CurrencyCategory.FIAT),
        CurrencyRate("INR", "Indian Rupee", "₹", "🇮🇳", 109.80, CurrencyCategory.FIAT),
        CurrencyRate("NZD", "New Zealand Dollar", "NZ$", "🇳🇿", 2.12, CurrencyCategory.FIAT),
        CurrencyRate("AED", "UAE Dirham", "AED", "🇦🇪", 4.81, CurrencyCategory.FIAT),
        CurrencyRate("PLN", "Polish Złoty", "zł", "🇵🇱", 5.12, CurrencyCategory.FIAT),
        CurrencyRate("ZAR", "South African Rand", "R", "🇿🇦", 23.40, CurrencyCategory.FIAT),
        // Crypto Assets
        CurrencyRate("BTC", "Bitcoin", "₿", "🪙", 0.000021, CurrencyCategory.CRYPTO),
        CurrencyRate("ETH", "Ethereum", "Ξ", "⟠", 0.00052, CurrencyCategory.CRYPTO),
        CurrencyRate("SOL", "Solana", "◎", "🟣", 0.0089, CurrencyCategory.CRYPTO)
    )

    /**
     * Merges cached/live rate overrides with standard default metadata.
     */
    fun mergeRatesWithDefaults(rateOverrides: Map<String, Double>): List<CurrencyRate> {
        if (rateOverrides.isEmpty()) return DEFAULT_RATES
        return DEFAULT_RATES.map { defaultRate ->
            val liveRate = rateOverrides[defaultRate.code]
            if (liveRate != null && liveRate > 0.0) {
                defaultRate.copy(gbpRate = liveRate)
            } else {
                defaultRate
            }
        }
    }

    /**
     * Converts annual GBP amount into global currencies and crypto assets across the selected pay horizon.
     */
    fun convertAmount(
        annualGbpAmount: Double,
        period: PayPeriod = PayPeriod.MONTHLY,
        customRates: List<CurrencyRate> = DEFAULT_RATES,
        categoryFilter: CurrencyCategory = CurrencyCategory.ALL
    ): List<ConvertedCurrencyItem> {
        val safeAnnual = max(0.0, annualGbpAmount)
        val periodGbpAmount = safeAnnual / period.annualDivisor

        val filteredRates = when (categoryFilter) {
            CurrencyCategory.ALL -> customRates
            CurrencyCategory.FIAT -> customRates.filter { it.category == CurrencyCategory.FIAT }
            CurrencyCategory.CRYPTO -> customRates.filter { it.category == CurrencyCategory.CRYPTO }
        }

        return filteredRates.map { rate ->
            val converted = periodGbpAmount * rate.gbpRate
            val formatted = formatConvertedValue(rate, converted)
            ConvertedCurrencyItem(
                currency = rate,
                convertedAmount = converted,
                formattedString = formatted
            )
        }
    }

    /**
     * Formats the converted amount with appropriate precision based on currency type.
     */
    fun formatConvertedValue(rate: CurrencyRate, amount: Double): String {
        return when (rate.code) {
            "BTC" -> "${rate.symbol}${String.format(Locale.ENGLISH, "%.6f", amount)}"
            "ETH" -> "${rate.symbol}${String.format(Locale.ENGLISH, "%.4f", amount)}"
            "SOL" -> "${rate.symbol}${String.format(Locale.ENGLISH, "%.3f", amount)}"
            "JPY" -> "${rate.symbol}${String.format(Locale.ENGLISH, "%,.0f", amount)}"
            "INR" -> "${rate.symbol}${String.format(Locale.ENGLISH, "%,.2f", amount)}"
            else -> "${rate.symbol}${String.format(Locale.ENGLISH, "%,.2f", amount)}"
        }
    }

    /**
     * Fetches live fiat and crypto rates asynchronously from public exchange rate APIs.
     */
    suspend fun fetchLiveRatesOnline(): Map<String, Double>? = withContext(Dispatchers.IO) {
        val ratesMap = mutableMapOf<String, Double>()
        try {
            // 1. Fetch Fiat Rates from open exchange rates API
            val fiatUrl = URL("https://open.er-api.com/v6/latest/GBP")
            val conn = fiatUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = Json.parseToJsonElement(responseText).jsonObject
                val ratesObj = rootJson["rates"]?.jsonObject
                if (ratesObj != null) {
                    val fiatKeys = listOf("EUR", "USD", "CAD", "AUD", "JPY", "CHF", "SGD", "INR", "NZD", "AED", "PLN", "ZAR")
                    for (k in fiatKeys) {
                        ratesObj[k]?.jsonPrimitive?.content?.toDoubleOrNull()?.let { r ->
                            if (r > 0.0) ratesMap[k] = r
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
            // Silently fall back to cached / defaults
        }

        try {
            // 2. Fetch Crypto Rates from CoinGecko API
            val cryptoUrl = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=gbp")
            val cryptoConn = cryptoUrl.openConnection() as HttpURLConnection
            cryptoConn.connectTimeout = 4000
            cryptoConn.readTimeout = 4000
            cryptoConn.requestMethod = "GET"

            if (cryptoConn.responseCode == 200) {
                val cryptoText = cryptoConn.inputStream.bufferedReader().use { it.readText() }
                val cryptoJson = Json.parseToJsonElement(cryptoText).jsonObject
                val btcGbp = cryptoJson["bitcoin"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()
                val ethGbp = cryptoJson["ethereum"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()
                val solGbp = cryptoJson["solana"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()

                if (btcGbp != null && btcGbp > 0.0) ratesMap["BTC"] = 1.0 / btcGbp
                if (ethGbp != null && ethGbp > 0.0) ratesMap["ETH"] = 1.0 / ethGbp
                if (solGbp != null && solGbp > 0.0) ratesMap["SOL"] = 1.0 / solGbp
            }
            cryptoConn.disconnect()
        } catch (_: Exception) {
            // Silently fall back to cached / defaults
        }

        if (ratesMap.isNotEmpty()) ratesMap else null
    }
}
