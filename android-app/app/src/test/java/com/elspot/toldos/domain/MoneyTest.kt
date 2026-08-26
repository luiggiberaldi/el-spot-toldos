package com.elspot.toldos.domain

import com.elspot.toldos.data.ConfigSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesVenezuelanDecimalNotation() {
        assertEquals(10_050L, parseDollarCents("100,50"))
        assertEquals(123_456L, parseDollarCents("1.234,56"))
        assertEquals(9_999L, parseDollarCents("99.99"))
    }

    @Test
    fun formatsDollarAndBolivarUsingManualRate() {
        val text = formatDual(10_000L, ConfigSnapshot(exchangeRate = 36.5))
        assertTrue(text.contains("$"))
        assertTrue(text.contains("Bs"))
        assertTrue(text.contains("3.650"))
    }

    @Test
    fun doesNotShowBolivarWhenRateIsNotConfigured() {
        assertEquals("$ 100,00", centsToDollarText(10_000L))
        assertEquals("", centsToBolivarText(10_000L, 0.0))
    }
}
