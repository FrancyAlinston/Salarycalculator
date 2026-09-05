package com.example.salarycalculator.domain

import kotlin.math.max

/**
 * Dynamic Multi-Currency Converter Engine.
 * Converts UK GBP (£) gross and net earnings into global currencies across multiple time horizons.
 */
data class CurrencyRate(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val gbpRate: Double // e.g. 1 GBP = 1.18 EUR
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
        CurrencyRate("EUR", "Euro", "€", "🇪🇺", 1.18),
        CurrencyRate("USD", "US Dollar", "$", "🇺🇸", 1.31),
        CurrencyRate("CAD", "Canadian Dollar", "CA$", "🇨🇦", 1.78),
        CurrencyRate("AUD", "Australian Dollar", "A$", "🇦🇺", 1.95),
        CurrencyRate("JPY", "Japanese Yen", "¥", "🇯🇵", 192.50),
        CurrencyRate("CHF", "Swiss Franc", "Fr", "🇨🇭", 1.12),
        CurrencyRate("SGD", "Singapore Dollar", "S$", "🇸🇬", 1.71),
        CurrencyRate("INR", "Indian Rupee", "₹", "🇮🇳", 109.80),
        CurrencyRate("NZD", "New Zealand Dollar", "NZ$", "🇳🇿", 2.12),
        CurrencyRate("AED", "UAE Dirham", "AED", "🇦🇪", 4.81)
    )

    fun convertAmount(
        annualGbpAmount: Double,
        period: PayPeriod = PayPeriod.MONTHLY,
        customRates: List<CurrencyRate> = DEFAULT_RATES
    ): List<ConvertedCurrencyItem> {
        val safeAnnual = max(0.0, annualGbpAmount)
        val periodGbpAmount = safeAnnual / period.annualDivisor

        return customRates.map { rate ->
            val converted = periodGbpAmount * rate.gbpRate
            val formatted = when {
                rate.code == "JPY" -> "${rate.symbol}${String.format("%,.0f", converted)}"
                rate.code == "INR" -> "${rate.symbol}${String.format("%,.2f", converted)}"
                else -> "${rate.symbol}${String.format("%,.2f", converted)}"
            }
            ConvertedCurrencyItem(
                currency = rate,
                convertedAmount = converted,
                formattedString = formatted
            )
        }
    }
}
