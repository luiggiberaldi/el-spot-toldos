package com.elspot.toldos.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/** Modalidad comercial del alquiler. La tarifa base del toldo corresponde a 24 horas. */
enum class RentalMode(val hours: Int, val label: String) {
    H12(12, "12 horas"),
    H24(24, "24 horas");

    companion object {
        fun from(value: String?): RentalMode = when (value?.trim()?.uppercase(Locale.ROOT)) {
            "12H", "H12", "12 HORAS", "12 HORAS (MITAD DE TARIFA)" -> H12
            else -> H24
        }
    }

    val code: String get() = if (this == H12) "12h" else "24h"
}

enum class RentalStatus(val label: String) {
    ACTIVE("Activo"),
    DELIVERED("Entregado"),
    RETURNED("Devuelto"),
    CANCELLED("Cancelado");

    companion object {
        fun from(value: String?): RentalStatus {
            return when (value?.trim()?.uppercase(Locale.ROOT)) {
                "ACTIVE", "ACTIVO" -> ACTIVE
                "DELIVERED", "ENTREGADO" -> DELIVERED
                "RETURNED", "DEVUELTO" -> RETURNED
                "CANCELLED", "CANCELADO" -> CANCELLED
                else -> ACTIVE
            }
        }
    }
}

enum class ReceiptPaymentStatus(val label: String) {
    PAID("Pagado"),
    DUE("Por pagar");

    companion object {
        fun from(value: String?): ReceiptPaymentStatus = when (value?.trim()?.uppercase(Locale.ROOT)) {
            "PAID", "PAGADO" -> PAID
            else -> DUE
        }
    }
}

enum class TentStatus(val label: String) {
    AVAILABLE("Disponible"),
    RENTED("Alquilado"),
    REPAIR("En reparación"),
    RETIRED("Retirado");

    companion object {
        fun from(value: String?): TentStatus {
            return when (value?.trim()?.uppercase(Locale.ROOT)) {
                "AVAILABLE", "DISPONIBLE" -> AVAILABLE
                "RENTED", "ALQUILADO" -> RENTED
                "REPAIR", "EN_REPARACION", "EN REPARACION" -> REPAIR
                "RETIRED", "RETIRADO" -> RETIRED
                else -> AVAILABLE
            }
        }
    }
}

data class ConfigSnapshot(
    val businessName: String = "EL SPOT",
    val rif: String = "",
    val phone: String = "",
    val address: String = "",
    val exchangeRate: Double = 0.0,
    val logoUri: String = "",
    val notificationsEnabled: Boolean = true,
    val reminderMinutes: Int = 60,
    val lastReceiptNumber: Int = 0,
    val lastRentalNumber: Int = 0
)

data class RentalItemDraft(
    val tentId: String = "",
    val quantity: Int = 1,
    /** Precio congelado de la modalidad elegida. */
    val tariffCents: Long = 0L
)

data class RentalDraft(
    val id: String? = null,
    val clientId: String = "",
    val items: List<RentalItemDraft> = emptyList(),
    val mode: RentalMode = RentalMode.H24,
    val startAt: Long = System.currentTimeMillis(),
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val totalCents: Long = 0L,
    val depositCents: Long = 0L,
    val status: RentalStatus = RentalStatus.ACTIVE,
    val notes: String = ""
)

data class ReceiptItemSnapshot(
    val name: String,
    val size: String,
    val quantity: Int,
    val tariffCents: Long
)

data class ReceiptSnapshot(
    val id: String,
    val folio: String,
    val rentalFolio: String,
    val rentalId: String,
    val emittedAt: Long,
    val concept: String,
    val amountCents: Long,
    val paymentStatus: ReceiptPaymentStatus = ReceiptPaymentStatus.DUE,
    val businessName: String,
    val businessRif: String,
    val businessPhone: String,
    val businessAddress: String,
    val exchangeRate: Double,
    val logoUri: String,
    val clientName: String,
    val clientDocument: String,
    val clientPhone: String,
    val clientAddress: String,
    val mode: RentalMode,
    val startAt: Long,
    val returnAt: Long,
    val eventAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    val rentalTotalCents: Long,
    val rentalDepositCents: Long,
    val items: List<ReceiptItemSnapshot>
) {
    fun toJson(): String {
        val root = JSONObject()
            .put("id", id)
            .put("folio", folio)
            .put("rentalFolio", rentalFolio)
            .put("rentalId", rentalId)
            .put("emittedAt", emittedAt)
            .put("concept", concept)
            .put("amountCents", amountCents)
            .put("paymentStatus", paymentStatus.name)
            .put("businessName", businessName)
            .put("businessRif", businessRif)
            .put("businessPhone", businessPhone)
            .put("businessAddress", businessAddress)
            .put("exchangeRate", exchangeRate)
            .put("logoUri", logoUri)
            .put("clientName", clientName)
            .put("clientDocument", clientDocument)
            .put("clientPhone", clientPhone)
            .put("clientAddress", clientAddress)
            .put("mode", mode.code)
            .put("startAt", startAt)
            .put("returnAt", returnAt)
            .put("eventAddress", eventAddress)
            .put("latitude", latitude ?: JSONObject.NULL)
            .put("longitude", longitude ?: JSONObject.NULL)
            .put("rentalTotalCents", rentalTotalCents)
            .put("rentalDepositCents", rentalDepositCents)
        val itemArray = JSONArray()
        items.forEach {
            itemArray.put(
                JSONObject()
                    .put("name", it.name)
                    .put("size", it.size)
                    .put("quantity", it.quantity)
                    .put("tariffCents", it.tariffCents)
            )
        }
        return root.put("items", itemArray).toString()
    }

    companion object {
        fun fromJson(raw: String): ReceiptSnapshot? = runCatching {
            val root = JSONObject(raw)
            if (root.optJSONObject("datos") != null) {
                return@runCatching fromLegacyJson(root)
            }
            require(root.has("amountCents") || root.has("montoCents")) { "Snapshot de recibo no reconocido." }
            val itemsJson = root.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (index in 0 until itemsJson.length()) {
                    val item = itemsJson.getJSONObject(index)
                    add(
                        ReceiptItemSnapshot(
                            name = item.optString("name"),
                            size = item.optString("size"),
                            quantity = item.optInt("quantity", 1),
                            tariffCents = item.optLong("tariffCents")
                        )
                    )
                }
            }
            ReceiptSnapshot(
                id = root.optString("id"),
                folio = root.optString("folio"),
                rentalFolio = root.optString("rentalFolio"),
                rentalId = root.optString("rentalId"),
                emittedAt = parseStoredTime(root.optString("emittedAt").ifBlank { root.optString("emitidoEn") }),
                concept = root.optString("concept"),
                amountCents = root.optString("amountCents").toLongOrNull()
                    ?: root.optString("montoCents").toLongOrNull()
                    ?: 0L,
                paymentStatus = ReceiptPaymentStatus.from(
                    root.optString("paymentStatus").ifBlank { root.optString("estadoPago") }
                ),
                businessName = root.optString("businessName", "EL SPOT"),
                businessRif = root.optString("businessRif"),
                businessPhone = root.optString("businessPhone"),
                businessAddress = root.optString("businessAddress"),
                exchangeRate = root.optDouble("exchangeRate", 0.0),
                logoUri = root.optString("logoUri"),
                clientName = root.optString("clientName", "Cliente"),
                clientDocument = root.optString("clientDocument"),
                clientPhone = root.optString("clientPhone"),
                clientAddress = root.optString("clientAddress"),
                mode = RentalMode.from(root.optString("mode").ifBlank { root.optString("modalidad") }),
                startAt = parseStoredTime(root.optString("startAt").ifBlank { root.optString("inicio") }),
                returnAt = parseStoredTime(root.optString("returnAt").ifBlank { root.optString("devolucion") }),
                eventAddress = root.optString("eventAddress"),
                latitude = root.optDoubleOrNull("latitude"),
                longitude = root.optDoubleOrNull("longitude"),
                rentalTotalCents = root.optLong("rentalTotalCents"),
                rentalDepositCents = root.optLong("rentalDepositCents"),
                items = items
            )
        }.getOrNull()

        /** Convierte el formato de recibo de la PWA a un snapshot nativo inmutable. */
        fun fromLegacyJson(receipt: JSONObject): ReceiptSnapshot? {
            val data = receipt.optJSONObject("datos") ?: return null
            return runCatching {
                val business = data.optJSONObject("negocio") ?: JSONObject()
                val client = data.optJSONObject("cliente") ?: JSONObject()
                val rental = data.optJSONObject("alquiler") ?: JSONObject()
                val emittedAt = parseStoredTime(
                    receipt.optString("emitidoEn").ifBlank { data.optString("emitidoEn") }
                ).takeIf { it > 0 } ?: System.currentTimeMillis()
                val startAt = parseStoredTime(rental.optString("fechaInicio"))
                    .takeIf { it > 0 } ?: emittedAt
                val mode = RentalMode.from(rental.optString("modalidad"))
                val returnAt = parseStoredTime(
                    rental.optString("fechaDevolucion").ifBlank { rental.optString("fechaFin") }
                ).takeIf { it > 0 } ?: startAt + mode.hours * 3_600_000L
                val itemArray = rental.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (index in 0 until itemArray.length()) {
                        val item = itemArray.optJSONObject(index) ?: continue
                        add(
                            ReceiptItemSnapshot(
                                name = item.optString("nombre", "Toldo"),
                                size = item.optString("tamano", item.optString("size")),
                                quantity = item.optInt("cantidad", 1).coerceAtLeast(1),
                                tariffCents = (item.optDouble("tarifa", 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
                            )
                        )
                    }
                }
                val folio = receipt.optString("folio").ifBlank { data.optString("folio") }
                ReceiptSnapshot(
                    id = receipt.optString("id").ifBlank { folio },
                    folio = folio,
                    rentalFolio = rental.optString("folio"),
                    rentalId = receipt.optString("alquilerId"),
                    emittedAt = emittedAt,
                    concept = receipt.optString("concepto", data.optString("concepto", "Pago del alquiler")),
                    amountCents = (receipt.optDouble("monto", data.optDouble("monto", 0.0)) * 100.0).roundToLong(),
                    paymentStatus = ReceiptPaymentStatus.from(
                        receipt.optString("estado").ifBlank { data.optString("estado") }
                    ),
                    businessName = business.optString("nombre", "EL SPOT"),
                    businessRif = business.optString("rif"),
                    businessPhone = business.optString("telefono"),
                    businessAddress = business.optString("direccion"),
                    exchangeRate = business.optDouble("tasaBs", 0.0),
                    logoUri = business.optString("logo"),
                    clientName = client.optString("nombre", "Cliente"),
                    clientDocument = client.optString("cedula"),
                    clientPhone = client.optString("telefono"),
                    clientAddress = client.optString("direccion"),
                    mode = mode,
                    startAt = startAt,
                    returnAt = returnAt,
                    eventAddress = rental.optString("direccion"),
                    latitude = rental.optDoubleOrNull("lat"),
                    longitude = rental.optDoubleOrNull("lng"),
                    rentalTotalCents = (rental.optDouble("montoTotal", 0.0) * 100.0).roundToLong(),
                    rentalDepositCents = (rental.optDouble("abono", 0.0) * 100.0).roundToLong(),
                    items = items
                )
            }.getOrNull()
        }
    }
}

private fun parseStoredTime(value: String): Long {
    val raw = value.trim()
    if (raw.isBlank() || raw == "null") return 0L
    raw.toLongOrNull()?.let { return it }
    runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()?.let { return it }
    runCatching { OffsetDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME).toInstant().toEpochMilli() }
        .getOrNull()?.let { return it }
    runCatching {
        LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()?.let { return it }
    return runCatching {
        LocalDate.parse(raw).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrDefault(0L)
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key).takeUnless { it.isNaN() }
}

data class AppUiState(
    val clients: List<ClienteEntity> = emptyList(),
    val tents: List<ToldoEntity> = emptyList(),
    val rentals: List<AlquilerEntity> = emptyList(),
    val rentalItems: List<AlquilerItemEntity> = emptyList(),
    val receipts: List<ReciboEntity> = emptyList(),
    val log: List<BitacoraEntity> = emptyList(),
    val config: ConfigSnapshot = ConfigSnapshot(),
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
