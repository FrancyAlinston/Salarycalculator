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

    val POPULAR_CURRENCIES = listOf(
        // Primary Global Currencies
        CurrencyRate("EUR", "Euro", "€", "🇪🇺", 1.18, CurrencyCategory.FIAT),
        CurrencyRate("USD", "US Dollar", "$", "🇺🇸", 1.31, CurrencyCategory.FIAT),
        CurrencyRate("INR", "Indian Rupee", "₹", "🇮🇳", 109.80, CurrencyCategory.FIAT),
        CurrencyRate("PHP", "Philippine Peso", "₱", "🇵🇭", 73.50, CurrencyCategory.FIAT),
        CurrencyRate("CAD", "Canadian Dollar", "CA$", "🇨🇦", 1.78, CurrencyCategory.FIAT),
        CurrencyRate("AUD", "Australian Dollar", "A$", "🇦🇺", 1.95, CurrencyCategory.FIAT),
        CurrencyRate("JPY", "Japanese Yen", "¥", "🇯🇵", 192.50, CurrencyCategory.FIAT),
        CurrencyRate("CHF", "Swiss Franc", "Fr", "🇨🇭", 1.12, CurrencyCategory.FIAT),
        CurrencyRate("SGD", "Singapore Dollar", "S$", "🇸🇬", 1.71, CurrencyCategory.FIAT),
        CurrencyRate("NZD", "New Zealand Dollar", "NZ$", "🇳🇿", 2.12, CurrencyCategory.FIAT),
        CurrencyRate("AED", "UAE Dirham", "AED", "🇦🇪", 4.81, CurrencyCategory.FIAT),
        CurrencyRate("SAR", "Saudi Riyal", "SAR", "🇸🇦", 4.91, CurrencyCategory.FIAT),
        CurrencyRate("QAR", "Qatari Riyal", "QR", "🇶🇦", 4.77, CurrencyCategory.FIAT),
        CurrencyRate("PLN", "Polish Złoty", "zł", "🇵🇱", 5.12, CurrencyCategory.FIAT),
        CurrencyRate("ZAR", "South African Rand", "R", "🇿🇦", 23.40, CurrencyCategory.FIAT),
        CurrencyRate("NGN", "Nigerian Naira", "₦", "🇳🇬", 2150.00, CurrencyCategory.FIAT),
        CurrencyRate("KES", "Kenyan Shilling", "KSh", "🇰🇪", 170.50, CurrencyCategory.FIAT),
        CurrencyRate("GHS", "Ghanaian Cedi", "GH₵", "🇬🇭", 20.80, CurrencyCategory.FIAT),
        CurrencyRate("BRL", "Brazilian Real", "R$", "🇧🇷", 7.20, CurrencyCategory.FIAT),
        CurrencyRate("MXN", "Mexican Peso", "Mex$", "🇲🇽", 25.40, CurrencyCategory.FIAT),
        CurrencyRate("SEK", "Swedish Krona", "kr", "🇸🇪", 13.45, CurrencyCategory.FIAT),
        CurrencyRate("NOK", "Norwegian Krone", "kr", "🇳🇴", 13.90, CurrencyCategory.FIAT),
        CurrencyRate("DKK", "Danish Krone", "kr", "🇩🇰", 8.80, CurrencyCategory.FIAT),
        CurrencyRate("HKD", "Hong Kong Dollar", "HK$", "🇭🇰", 10.20, CurrencyCategory.FIAT),
        CurrencyRate("TWD", "Taiwan Dollar", "NT$", "🇹🇼", 41.80, CurrencyCategory.FIAT),
        CurrencyRate("KRW", "South Korean Won", "₩", "🇰🇷", 1750.00, CurrencyCategory.FIAT),
        CurrencyRate("THB", "Thai Baht", "฿", "🇹🇭", 44.50, CurrencyCategory.FIAT),
        CurrencyRate("MYR", "Malaysian Ringgit", "RM", "🇲🇾", 5.70, CurrencyCategory.FIAT),
        CurrencyRate("IDR", "Indonesian Rupiah", "Rp", "🇮🇩", 20300.00, CurrencyCategory.FIAT),
        CurrencyRate("PKR", "Pakistani Rupee", "₨", "🇵🇰", 365.00, CurrencyCategory.FIAT),
        CurrencyRate("BDT", "Bangladeshi Taka", "৳", "🇧🇩", 156.00, CurrencyCategory.FIAT),
        CurrencyRate("TRY", "Turkish Lira", "₺", "🇹🇷", 45.10, CurrencyCategory.FIAT),
        // Crypto Assets
        CurrencyRate("BTC", "Bitcoin", "₿", "🪙", 0.000021, CurrencyCategory.CRYPTO),
        CurrencyRate("ETH", "Ethereum", "Ξ", "⟠", 0.00052, CurrencyCategory.CRYPTO),
        CurrencyRate("SOL", "Solana", "◎", "🟣", 0.0089, CurrencyCategory.CRYPTO),
        CurrencyRate("XRP", "XRP", "XRP", "⚡", 2.30, CurrencyCategory.CRYPTO),
        CurrencyRate("ADA", "Cardano", "ADA", "🔷", 3.80, CurrencyCategory.CRYPTO),
        CurrencyRate("DOGE", "Dogecoin", "Ð", "🐕", 12.50, CurrencyCategory.CRYPTO)
    )

    val DEFAULT_RATES = POPULAR_CURRENCIES

    /**
     * Resolves a CurrencyRate object for ANY currency code (popular or custom).
     */
    fun getCurrencyRate(code: String, rateOverrides: Map<String, Double> = emptyMap()): CurrencyRate {
        val upperCode = code.trim().uppercase()
        val found = POPULAR_CURRENCIES.find { it.code == upperCode }
        val liveRate = rateOverrides[upperCode]

        return if (found != null) {
            if (liveRate != null && liveRate > 0.0) found.copy(gbpRate = liveRate) else found
        } else {
            val symbol = when (upperCode) {
                "GBP" -> "£"
                "EUR" -> "€"
                "USD" -> "$"
                "INR" -> "₹"
                "JPY", "CNY" -> "¥"
                "PHP" -> "₱"
                "KRW" -> "₩"
                "THB" -> "฿"
                "RUB" -> "₽"
                "TRY" -> "₺"
                "NGN" -> "₦"
                "GHS" -> "GH₵"
                "ILS" -> "₪"
                "VND" -> "₫"
                "PLN" -> "zł"
                "BRL" -> "R$"
                else -> upperCode
            }
            CurrencyRate(
                code = upperCode,
                name = upperCode,
                symbol = symbol,
                flag = "🌐",
                gbpRate = liveRate ?: 1.0,
                category = CurrencyCategory.FIAT
            )
        }
    }

    /**
     * Converts a GBP amount to target currency string with symbol and formatting.
     */
    fun convertGbpToTarget(gbpAmount: Double, targetCode: String, rateOverrides: Map<String, Double> = emptyMap()): String {
        val rate = getCurrencyRate(targetCode, rateOverrides)
        val converted = gbpAmount * rate.gbpRate
        return formatConvertedValue(rate, converted)
    }

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
            "JPY", "KRW", "IDR", "VND" -> "${rate.symbol}${String.format(Locale.ENGLISH, "%,.0f", amount)}"
            "INR", "PKR", "BDT", "LKR", "NPR" -> "${rate.symbol}${String.format(Locale.ENGLISH, "%,.2f", amount)}"
            else -> "${rate.symbol}${String.format(Locale.ENGLISH, "%,.2f", amount)}"
        }
    }

    /**
     * Fetches live fiat and crypto rates asynchronously from public exchange rate APIs (160+ currencies).
     */
    suspend fun fetchLiveRatesOnline(): Map<String, Double>? = withContext(Dispatchers.IO) {
        val ratesMap = mutableMapOf<String, Double>()
        try {
            // 1. Fetch ALL Fiat Rates from open.er-api.com
            val fiatUrl = URL("https://open.er-api.com/v6/latest/GBP")
            val conn = fiatUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = Json.parseToJsonElement(responseText).jsonObject
                val ratesObj = rootJson["rates"]?.jsonObject
                if (ratesObj != null) {
                    for ((k, v) in ratesObj) {
                        v.jsonPrimitive.content.toDoubleOrNull()?.let { r ->
                            if (r > 0.0) ratesMap[k.uppercase()] = r
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}

        try {
            // 2. Fetch Crypto Rates from CoinGecko API
            val cryptoUrl = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana,ripple,cardano,dogecoin&vs_currencies=gbp")
            val cryptoConn = cryptoUrl.openConnection() as HttpURLConnection
            cryptoConn.connectTimeout = 6000
            cryptoConn.readTimeout = 6000
            cryptoConn.requestMethod = "GET"

            if (cryptoConn.responseCode == 200) {
                val cryptoText = cryptoConn.inputStream.bufferedReader().use { it.readText() }
                val cryptoJson = Json.parseToJsonElement(cryptoText).jsonObject
                val btcGbp = cryptoJson["bitcoin"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()
                val ethGbp = cryptoJson["ethereum"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()
                val solGbp = cryptoJson["solana"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()
                val xrpGbp = cryptoJson["ripple"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()
                val adaGbp = cryptoJson["cardano"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()
                val dogeGbp = cryptoJson["dogecoin"]?.jsonObject?.get("gbp")?.jsonPrimitive?.content?.toDoubleOrNull()

                if (btcGbp != null && btcGbp > 0.0) ratesMap["BTC"] = 1.0 / btcGbp
                if (ethGbp != null && ethGbp > 0.0) ratesMap["ETH"] = 1.0 / ethGbp
                if (solGbp != null && solGbp > 0.0) ratesMap["SOL"] = 1.0 / solGbp
                if (xrpGbp != null && xrpGbp > 0.0) ratesMap["XRP"] = 1.0 / xrpGbp
                if (adaGbp != null && adaGbp > 0.0) ratesMap["ADA"] = 1.0 / adaGbp
                if (dogeGbp != null && dogeGbp > 0.0) ratesMap["DOGE"] = 1.0 / dogeGbp
            }
            cryptoConn.disconnect()
        } catch (_: Exception) {}

        if (ratesMap.isNotEmpty()) ratesMap else null
    }
}
