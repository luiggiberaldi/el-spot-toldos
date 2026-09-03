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

    @Test
    fun capitalizesWordsInNames() {
        assertEquals("Toldo Negro", capitalizeWords("toldo negro"))
        assertEquals("Luigi Beraldi", capitalizeWords("luigi beraldi"))
        assertEquals("El Spot Toldos", capitalizeWords("EL SPOT TOLDOS"))
        assertEquals("Toldo 3x3 Blanco", capitalizeWords("toldo 3x3 blanco"))
        assertEquals("María José Pérez", capitalizeWords("maría josé pérez"))
        assertEquals("Toldo (Negro)", capitalizeWords("toldo (negro)"))
        assertEquals("Toldo-Carpa", capitalizeWords("toldo-carpa"))
        assertEquals("", capitalizeWords("   "))
    }

    @Test
    fun parsesCoordinatesFromTextAndUrls() {
        val direct = com.elspot.toldos.location.parseCoordinates("10.142918, -68.016897")
        org.junit.Assert.assertNotNull(direct)
        assertEquals(10.142918, direct!!.first, 0.000001)
        assertEquals(-68.016897, direct.second, 0.000001)

        val urlQ = com.elspot.toldos.location.parseCoordinates("https://maps.google.com/?q=10.142918,-68.016897")
        org.junit.Assert.assertNotNull(urlQ)
        assertEquals(10.142918, urlQ!!.first, 0.000001)
        assertEquals(-68.016897, urlQ.second, 0.000001)

        val urlAt = com.elspot.toldos.location.parseCoordinates("https://www.google.com/maps/place/@10.142918,-68.016897,17z")
        org.junit.Assert.assertNotNull(urlAt)
        assertEquals(10.142918, urlAt!!.first, 0.000001)
        assertEquals(-68.016897, urlAt.second, 0.000001)

        org.junit.Assert.assertNull(com.elspot.toldos.location.parseCoordinates("texto sin coordenadas"))
    }
}
