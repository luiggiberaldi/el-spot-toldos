package com.elspot.toldos.domain

import com.elspot.toldos.data.RentalDraft
import com.elspot.toldos.data.RentalItemDraft
import com.elspot.toldos.data.RentalMode
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.data.ToldoEntity
import org.junit.Assert.assertEquals
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
    fun twelveHoursUsesHalfOfTheTwentyFourHourTariff() {
        assertEquals(20_000L, calculateRentalTotal(listOf(RentalItemDraft("tent-1", 2, 10_000L)), RentalMode.H24))
        assertEquals(5_000L, calculateRentalTotal(listOf(RentalItemDraft("tent-1", 1, 10_000L)), RentalMode.H12))
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
