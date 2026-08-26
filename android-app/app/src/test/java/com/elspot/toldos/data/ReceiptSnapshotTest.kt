package com.elspot.toldos.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptSnapshotTest {
    @Test
    fun nativeSnapshotRoundTripsModeMoneyGpsAndItems() {
        val original = ReceiptSnapshot(
            id = "receipt-1",
            folio = "REC-0001",
            rentalFolio = "ALQ-0001",
            rentalId = "rental-1",
            emittedAt = 1_700_000_000_000L,
            concept = "Abono",
            amountCents = 5_000L,
            paymentStatus = ReceiptPaymentStatus.PAID,
            businessName = "EL SPOT",
            businessRif = "J-123",
            businessPhone = "0412-0000000",
            businessAddress = "Caracas",
            exchangeRate = 36.5,
            logoUri = "",
            clientName = "Ana",
            clientDocument = "V-1",
            clientPhone = "0412-1",
            clientAddress = "Centro",
            mode = RentalMode.H12,
            startAt = 1_700_000_000_000L,
            returnAt = 1_700_043_200_000L,
            eventAddress = "Av. Principal",
            latitude = 10.5,
            longitude = -66.9,
            rentalTotalCents = 10_000L,
            rentalDepositCents = 5_000L,
            items = listOf(ReceiptItemSnapshot("Toldo 4x4", "4x4 m", 1, 5_000L))
        )

        val restored = ReceiptSnapshot.fromJson(original.toJson())

        assertNotNull(restored)
        assertEquals(RentalMode.H12, restored?.mode)
        assertEquals(5_000L, restored?.amountCents)
        assertEquals(ReceiptPaymentStatus.PAID, restored?.paymentStatus)
        assertEquals(36.5, restored?.exchangeRate ?: 0.0, 0.001)
        assertEquals(10.5, restored?.latitude ?: 0.0, 0.001)
        assertEquals(1, restored?.items?.size)
        assertEquals("Toldo 4x4", restored?.items?.first()?.name)
    }

    @Test
    fun legacyPwaSnapshotAcceptsLocalIsoDateAndDefaultsMissingModeTo24Hours() {
        val rental = JSONObject()
            .put("folio", "ALQ-0007")
            .put("fechaInicio", "2026-08-25T10:30:00")
            .put("direccion", "Valencia")
            .put("montoTotal", 100.0)
            .put("abono", 20.0)
            .put("items", JSONArray().put(JSONObject().put("nombre", "Toldo").put("cantidad", 1).put("tarifa", 100.0)))
        val legacy = JSONObject()
            .put("id", "receipt-legacy")
            .put("folio", "REC-0007")
            .put("emitidoEn", "2026-08-25T09:00:00")
            .put("monto", 20.0)
            .put("datos", JSONObject()
                .put("negocio", JSONObject().put("nombre", "EL SPOT").put("tasaBs", 36.5))
                .put("cliente", JSONObject().put("nombre", "Cliente legado"))
                .put("alquiler", rental))

        val snapshot = ReceiptSnapshot.fromLegacyJson(legacy)

        assertNotNull(snapshot)
        assertEquals(RentalMode.H24, snapshot?.mode)
        assertEquals("Cliente legado", snapshot?.clientName)
        assertEquals(ReceiptPaymentStatus.DUE, snapshot?.paymentStatus)
        assertEquals(2_000L, snapshot?.amountCents)
        assertTrue((snapshot?.startAt ?: 0L) > 0L)
        assertEquals(1, snapshot?.items?.size)
    }
}
