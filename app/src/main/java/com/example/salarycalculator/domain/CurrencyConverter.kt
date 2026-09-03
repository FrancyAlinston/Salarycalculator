package com.example.salarycalculator.domain

data class ConvertedCurrencies(
    val gbpAmount: Double,
    val eurAmount: Double,
    val usdAmount: Double,
    val gbpToEurRate: Double = DEFAULT_EUR_RATE,
    val gbpToUsdRate: Double = DEFAULT_USD_RATE
) {
    companion object {
        const val DEFAULT_EUR_RATE = 1.19
        const val DEFAULT_USD_RATE = 1.31
    }
}

object CurrencyConverter {

    fun convert(
        gbpAmount: Double,
        eurRate: Double = ConvertedCurrencies.DEFAULT_EUR_RATE,
        usdRate: Double = ConvertedCurrencies.DEFAULT_USD_RATE
    ): ConvertedCurrencies {
        val safeGbp = maxOf(0.0, gbpAmount)
        return ConvertedCurrencies(
            gbpAmount = safeGbp,
            eurAmount = safeGbp * eurRate,
            usdAmount = safeGbp * usdRate,
            gbpToEurRate = eurRate,
            gbpToUsdRate = usdRate
        )
    }
}
