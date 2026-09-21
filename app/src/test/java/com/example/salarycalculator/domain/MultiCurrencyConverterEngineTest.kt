package com.example.salarycalculator.domain

import org.junit.Assert.*
import org.junit.Test

class MultiCurrencyConverterEngineTest {

    @Test
    fun testFiatAndCryptoConversions() {
        val annualGbp = 30000.0 // £30,000 / year = £2,500 / month

        // Test Monthly Conversion across ALL categories
        val allItems = MultiCurrencyConverterEngine.convertAmount(
            annualGbpAmount = annualGbp,
            period = PayPeriod.MONTHLY,
            categoryFilter = CurrencyCategory.ALL
        )

        val eurItem = allItems.firstOrNull { it.currency.code == "EUR" }
        assertNotNull(eurItem)
        assertEquals(2500.0 * 1.18, eurItem!!.convertedAmount, 0.01)

        val usdItem = allItems.firstOrNull { it.currency.code == "USD" }
        assertNotNull(usdItem)
        assertEquals(2500.0 * 1.31, usdItem!!.convertedAmount, 0.01)

        val btcItem = allItems.firstOrNull { it.currency.code == "BTC" }
        assertNotNull(btcItem)
        assertEquals(2500.0 * 0.000021, btcItem!!.convertedAmount, 0.000001)

        // Test Category Filtering
        val fiatOnly = MultiCurrencyConverterEngine.convertAmount(
            annualGbpAmount = annualGbp,
            period = PayPeriod.MONTHLY,
            categoryFilter = CurrencyCategory.FIAT
        )
        assertTrue(fiatOnly.all { it.currency.category == CurrencyCategory.FIAT })
        assertFalse(fiatOnly.any { it.currency.code == "BTC" })

        val cryptoOnly = MultiCurrencyConverterEngine.convertAmount(
            annualGbpAmount = annualGbp,
            period = PayPeriod.MONTHLY,
            categoryFilter = CurrencyCategory.CRYPTO
        )
        assertTrue(cryptoOnly.all { it.currency.category == CurrencyCategory.CRYPTO })
        assertTrue(cryptoOnly.any { it.currency.code == "BTC" })
        assertTrue(cryptoOnly.any { it.currency.code == "ETH" })
        assertTrue(cryptoOnly.any { it.currency.code == "SOL" })
    }

    @Test
    fun testMergeRatesWithDefaults() {
        val liveOverrides = mapOf(
            "EUR" to 1.22,
            "BTC" to 0.000025
        )

        val merged = MultiCurrencyConverterEngine.mergeRatesWithDefaults(liveOverrides)
        val eur = merged.first { it.code == "EUR" }
        val btc = merged.first { it.code == "BTC" }
        val usd = merged.first { it.code == "USD" }

        assertEquals(1.22, eur.gbpRate, 0.001)
        assertEquals(0.000025, btc.gbpRate, 0.0000001)
        assertEquals(1.31, usd.gbpRate, 0.001) // Preserved default
    }
}
