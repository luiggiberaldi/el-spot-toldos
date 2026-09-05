package com.elspot.toldos.domain

import com.elspot.toldos.data.RentalDraft
import com.elspot.toldos.data.RentalItemDraft
import com.elspot.toldos.data.RentalMode
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.data.ToldoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RentalRulesTest {
    private val tent = ToldoEntity(
        id = "tent-1",
        nombre = "Toldo 4x4",
        tamano = "4x4 m",
        tarifaCents = 10_000L,
        unidades = 2,
        estado = TentStatus.AVAILABLE.name,
        notas = "",
        creadoEn = 1L
    )

    private fun draft(
        items: List<RentalItemDraft> = listOf(RentalItemDraft("tent-1", 1, 10_000L)),
        mode: RentalMode = RentalMode.H24,
        depositCents: Long = 0L,
        address: String = "Av. Principal 10",
        latitude: Double? = null,
        longitude: Double? = null
    ) = RentalDraft(
        clientId = "client-1",
        items = items,
        mode = mode,
        startAt = 1_700_000_000_000L,
        address = address,
        latitude = latitude,
        longitude = longitude,
        depositCents = depositCents,
        status = RentalStatus.ACTIVE
    )

    @Test
    fun calculateRentalTotalSumsAllItemLines() {
        assertEquals(20_000L, calculateRentalTotal(listOf(RentalItemDraft("tent-1", 2, 10_000L)), RentalMode.H24))
        assertEquals(7_500L, calculateRentalTotal(listOf(RentalItemDraft("tent-1", 1, 7_500L)), RentalMode.H12))
        assertEquals(7_500L, effectiveTariffCents(7_500L, RentalMode.H12))
        assertEquals(7_500L, effectiveTariffCents(7_500L, RentalMode.H24))
    }

    @Test
    fun totalH12ConDobleMitadDetectaSoloLaFirmaDelError() {
        // Caso real reportado: toldo 24h $7,50 con precio 12h $7,50 guardado; v1.0.4 persistió 375.
        assertTrue(totalH12ConDobleMitad("H12", 375L, 750L))
        // Caso general (sin el valor mágico 375): toldo de $10,00 alquilado 12h.
        assertTrue(totalH12ConDobleMitad("H12", 500L, 1_000L))
        // Línea a mitad de base (toldo sin precio 12h): total correcto 375 sobre línea 375 → no es firma del error.
        assertFalse(totalH12ConDobleMitad("H12", 375L, 375L))
        // Ya consistente o modalidad 24h nunca se toca.
        assertFalse(totalH12ConDobleMitad("H12", 750L, 750L))
        assertFalse(totalH12ConDobleMitad("H24", 375L, 750L))
        assertFalse(totalH12ConDobleMitad("H12", 400L, 750L))
        // Total impares redondeados como round(Σ/2) (188 para Σ=375).
        assertTrue(totalH12ConDobleMitad("H12", 188L, 375L))
    }

    @Test
    fun validDraftCanUseGpsWithoutAddress() {
        val error = validateRentalDraft(
            draft = draft(address = "", latitude = 10.5, longitude = -66.9),
            clientExists = true,
            tentsById = mapOf(tent.id to tent),
            occupiedUnitsByTent = emptyMap()
        )
        assertNull(error)
    }

    @Test
    fun partialGpsIsRejected() {
        val error = validateRentalDraft(
            draft = draft(address = "", latitude = 10.5),
            clientExists = true,
            tentsById = mapOf(tent.id to tent),
            occupiedUnitsByTent = emptyMap()
        )
        assertTrue(error?.contains("latitud") == true)
    }

    @Test
    fun occupiedUnitsCannotBeOverbooked() {
        val error = validateRentalDraft(
            draft = draft(items = listOf(RentalItemDraft("tent-1", 2, 10_000L))),
            clientExists = true,
            tentsById = mapOf(tent.id to tent),
            occupiedUnitsByTent = mapOf("tent-1" to 1)
        )
        assertTrue(error?.contains("disponibles") == true)
    }

    @Test
    fun repairTentCannotBeActivated() {
        val repairTent = tent.copy(estado = TentStatus.REPAIR.name)
        val error = validateRentalDraft(
            draft = draft(),
            clientExists = true,
            tentsById = mapOf(repairTent.id to repairTent),
            occupiedUnitsByTent = emptyMap()
        )
        assertTrue(error?.contains("reparación") == true)
    }

    @Test
    fun depositCannotExceedCalculatedTotal() {
        val error = validateRentalDraft(
            draft = draft(depositCents = 10_001L),
            clientExists = true,
            tentsById = mapOf(tent.id to tent),
            occupiedUnitsByTent = emptyMap()
        )
        assertTrue(error?.contains("superar") == true)
    }

    @Test
    fun duplicateTentLinesAreRejected() {
        val error = validateRentalDraft(
            draft = draft(
                items = listOf(
                    RentalItemDraft("tent-1", 1, 10_000L),
                    RentalItemDraft("tent-1", 1, 10_000L)
                )
            ),
            clientExists = true,
            tentsById = mapOf(tent.id to tent),
            occupiedUnitsByTent = emptyMap()
        )
        assertTrue(error?.contains("repitas") == true)
    }
}
