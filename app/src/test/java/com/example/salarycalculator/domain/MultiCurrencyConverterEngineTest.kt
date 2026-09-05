package com.example.salarycalculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiCurrencyConverterEngineTest {

    @Test
    fun testCurrencyConversionMonthly() {
        // £36,000 Annual Net = £3,000 Monthly Net
        // 1 GBP = 1.18 EUR -> €3,540.00
        // 1 GBP = 1.31 USD -> $3,930.00
        val items = MultiCurrencyConverterEngine.convertAmount(
            annualGbpAmount = 36000.0,
            period = PayPeriod.MONTHLY
        )
        val eur = items.find { it.currency.code == "EUR" }!!
        val usd = items.find { it.currency.code == "USD" }!!

        assertEquals(3540.0, eur.convertedAmount, 0.01)
        assertEquals(3930.0, usd.convertedAmount, 0.01)
        assertTrue(eur.formattedString.contains("3,540.00"))
        assertTrue(usd.formattedString.contains("3,930.00"))
    }

    @Test
    fun testCurrencyConversionAnnual() {
        val items = MultiCurrencyConverterEngine.convertAmount(
            annualGbpAmount = 50000.0,
            period = PayPeriod.ANNUAL
        )
        val usd = items.find { it.currency.code == "USD" }!!
        assertEquals(65500.0, usd.convertedAmount, 0.01)
    }
}
