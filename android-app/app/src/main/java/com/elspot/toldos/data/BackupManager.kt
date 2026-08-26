package com.elspot.toldos.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.util.Base64
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

class BackupManager(private val context: Context, private val repository: AppRepository) {
    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val json = buildJson(repository.snapshot())
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.writer(Charsets.UTF_8).use { it.write(json) }
        } ?: error("No se pudo abrir el archivo de destino.")
    }

    suspend fun importFrom(uri: Uri): ImportReport = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.use {
            it.reader(Charsets.UTF_8).readText()
        } ?: error("No se pudo leer el respaldo.")
        val parsed = parse(raw)
        repository.replaceAll(parsed.snapshot)
        parsed.report
    }

    private fun buildJson(snapshot: NativeBackupSnapshot): String {
        val root = JSONObject()
            .put("app", "gestor-toldos")
            .put("version", 2)
            .put("exportadoEn", isoNow())
        val data = JSONObject()
        data.put("clientes", JSONArray().apply {
            snapshot.clients.forEach { put(JSONObject()
                .put("id", it.id)
                .put("nombre", it.nombre)
                .put("cedula", it.cedula)
                .put("telefono", it.telefono)
                .put("email", it.email)
                .put("direccion", it.direccion)
                .put("notas", it.notas)
                .put("creadoEn", iso(it.creadoEn))) }
        })
        data.put("toldos", JSONArray().apply {
            snapshot.tents.forEach { put(JSONObject()
                .put("id", it.id)
                .put("nombre", it.nombre)
                .put("tamano", it.tamano)
                .put("tarifa", it.tarifaCents / 100.0)
                .put("tarifa12h", (it.tarifa12hCents ?: it.tarifaCents / 2) / 100.0)
                .put("unidades", it.unidades)
                .put("estado", it.estado.lowercase(Locale.ROOT))
                .put("notas", it.notas)
                .put("creadoEn", iso(it.creadoEn))) }
        })
        val itemsByRental = snapshot.rentalItems.groupBy { it.alquilerId }
        data.put("alquileres", JSONArray().apply {
            snapshot.rentals.forEach { rental ->
                put(JSONObject().apply {
                    put("id", rental.id)
                    put("folio", rental.folio)
                    put("clienteId", rental.clienteId)
                    put("modalidad", if (rental.modalidad == RentalMode.H12.name) "12h" else "24h")
                    put("fechaInicio", iso(rental.inicio))
                    put("fechaDevolucion", iso(rental.devolucion))
                    put("direccion", rental.direccion)
                    put("lat", rental.latitud ?: JSONObject.NULL)
                    put("lng", rental.longitud ?: JSONObject.NULL)
                    put("montoTotal", rental.montoTotalCents / 100.0)
                    put("abono", rental.abonoCents / 100.0)
                    put("estado", rental.statusForExport())
                    put("notas", rental.notas)
                    put("creadoEn", iso(rental.creadoEn))
                    put("actualizadoEn", iso(rental.actualizadoEn))
                    put("items", JSONArray().apply {
                        itemsByRental[rental.id].orEmpty().forEach { item ->
                            put(JSONObject()
                                .put("toldoId", item.toldoId)
                                .put("cantidad", item.cantidad)
                                .put("tarifa", item.tarifaCents / 100.0))
                        }
                    })
                })
            }
        })
        data.put("recibos", JSONArray().apply {
            snapshot.receipts.forEach { receipt ->
                val item = JSONObject()
                    .put("id", receipt.id)
                    .put("folio", receipt.folio)
                    .put("alquilerId", receipt.alquilerId)
                    .put("emitidoEn", iso(receipt.emitidoEn))
                    .put("concepto", receipt.concepto)
                    .put("monto", receipt.montoCents / 100.0)
                    .put("estado", ReceiptPaymentStatus.from(receipt.estadoPago).name.lowercase(Locale.ROOT))
                    .put("snapshot", receipt.snapshotJson)
                ReceiptSnapshot.fromJson(receipt.snapshotJson)?.let { native ->
                    item.put("datos", legacyReceiptData(native))
                }
                put(item)
            }
        })
        val logo = exportLogo(snapshot.config.logoUri)
        data.put("config", JSONObject()
            .put("negocio", JSONObject()
                .put("nombre", snapshot.config.businessName)
                .put("rif", snapshot.config.rif)
                .put("telefono", snapshot.config.phone)
                .put("direccion", snapshot.config.address)
                .put("moneda", "$")
                .put("logo", logo))
            .put("tasaBs", snapshot.config.exchangeRate)
            .put("notificaciones", snapshot.config.notificationsEnabled)
            .put("minutosRecordatorio", snapshot.config.reminderMinutes)
            .put("ultimoFolio", snapshot.config.lastReceiptNumber)
            .put("ultimoFolioAlquiler", snapshot.config.lastRentalNumber))
        data.put("bitacora", JSONArray().apply {
            snapshot.log.forEach { put(JSONObject()
                .put("id", it.id)
                .put("fecha", iso(it.fecha))
                .put("tipo", it.tipo)
                .put("entidad", it.entidad)
                .put("descripcion", it.descripcion)) }
        })
        return root.put("datos", data).toString(2)
    }

    private fun parse(raw: String): ParsedBackup {
        val root = JSONObject(raw)
        require(root.optString("app") == "gestor-toldos") {
            "El archivo no pertenece a EL SPOT."
        }
        val data = root.optJSONObject("datos") ?: error("El respaldo no contiene datos.")
        var corrected = 0
        val clients = mutableListOf<ClienteEntity>()
        val clientsJson = data.optJSONArray("clientes") ?: JSONArray()
        for (i in 0 until clientsJson.length()) {
            val item = clientsJson.optJSONObject(i) ?: continue
            clients += ClienteEntity(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                nombre = item.optString("nombre", "Cliente"),
                cedula = item.optString("cedula"),
                telefono = item.optString("telefono"),
                email = item.optString("email"),
                direccion = item.optString("direccion"),
                notas = item.optString("notas"),
                creadoEn = parseTime(item.optString("creadoEn"))
            )
        }

        val tents = mutableListOf<ToldoEntity>()
        val tentsJson = data.optJSONArray("toldos") ?: JSONArray()
        for (i in 0 until tentsJson.length()) {
            val item = tentsJson.optJSONObject(i) ?: continue
            tents += ToldoEntity(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                nombre = item.optString("nombre", "Toldo"),
                tamano = item.optString("tamano"),
                tarifaCents = amountToCents(item, "tarifa"),
                tarifa12hCents = item.optDouble("tarifa12h", item.optDouble("tarifa", 0.0) / 2.0).let { (it * 100.0).roundToLong() },
                unidades = item.optInt("unidades", 1).coerceAtLeast(1),
                estado = tentStatusFromExport(item.optString("estado")).name,
                notas = item.optString("notas"),
                creadoEn = parseTime(item.optString("creadoEn"))
            )
        }

        val rentals = mutableListOf<AlquilerEntity>()
        val rentalItems = mutableListOf<AlquilerItemEntity>()
        val rentalsJson = data.optJSONArray("alquileres") ?: JSONArray()
        for (i in 0 until rentalsJson.length()) {
            val item = rentalsJson.optJSONObject(i) ?: continue
            val rawMode = item.optString("modalidad")
            val mode = RentalMode.from(rawMode)
            val start = parseTime(
                item.optString("fechaInicio").ifBlank { item.optString("inicio") }
            ).takeIf { it > 0 } ?: parseTime(item.optString("creadoEn"))
            val id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
            val itemArray = item.optJSONArray("items") ?: JSONArray()
            for (line in 0 until itemArray.length()) {
                val lineJson = itemArray.optJSONObject(line) ?: continue
                rentalItems += AlquilerItemEntity(
                    alquilerId = id,
                    linea = line,
                    toldoId = lineJson.optString("toldoId"),
                    cantidad = lineJson.optInt("cantidad", 1).coerceAtLeast(1),
                    tarifaCents = amountToCents(lineJson, "tarifa")
                )
            }
            val total = amountToCents(item, "montoTotal")
            val returnAt = parseTime(
                item.optString("fechaDevolucion").ifBlank {
                    item.optString("devolucion").ifBlank { item.optString("fechaFin") }
                }
            ).takeIf { it > 0 } ?: start + mode.hours * 3_600_000L
            rentals += AlquilerEntity(
                id = id,
                folio = item.optString("folio").ifBlank { "ALQ-${(i + 1).toString().padStart(4, '0')}" },
                clienteId = item.optString("clienteId"),
                modalidad = mode.name,
                inicio = start,
                devolucion = returnAt,
                direccion = item.optString("direccion"),
                latitud = item.optDoubleOrNull("lat"),
                longitud = item.optDoubleOrNull("lng"),
                montoTotalCents = total,
                abonoCents = amountToCents(item, "abono").coerceIn(0, total),
                estado = RentalStatus.from(item.optString("estado")).name,
                notas = item.optString("notas"),
                creadoEn = parseTime(item.optString("creadoEn")).takeIf { it > 0 } ?: start,
                actualizadoEn = parseTime(item.optString("actualizadoEn")).takeIf { it > 0 } ?: start
            )
            if (rawMode.isBlank()) corrected++
        }

        val receipts = mutableListOf<ReciboEntity>()
        val receiptsJson = data.optJSONArray("recibos") ?: JSONArray()
        for (i in 0 until receiptsJson.length()) {
            val item = receiptsJson.optJSONObject(i) ?: continue
            val snapshot = when {
                item.optString("snapshot").isNotBlank() -> item.optString("snapshot")
                item.optJSONObject("datos") != null -> ReceiptSnapshot.fromLegacyJson(item)?.toJson() ?: "{}"
                else -> "{}"
            }
            receipts += ReciboEntity(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                folio = item.optString("folio").ifBlank { "REC-${(i + 1).toString().padStart(4, '0')}" },
                alquilerId = item.optString("alquilerId"),
                emitidoEn = parseTime(item.optString("emitidoEn")),
                concepto = item.optString("concepto", "Pago del alquiler"),
                montoCents = amountToCents(item, "monto"),
                snapshotJson = snapshot,
                estadoPago = ReceiptPaymentStatus.from(item.optString("estado")).name
            )
        }

        val configJson = data.optJSONObject("config") ?: JSONObject()
        val business = configJson.optJSONObject("negocio") ?: JSONObject()
        val config = ConfigSnapshot(
            businessName = business.optString("nombre", "EL SPOT").trim().ifBlank { "EL SPOT" },
            rif = business.optString("rif"),
            phone = business.optString("telefono"),
            address = business.optString("direccion"),
            exchangeRate = configJson.optDouble("tasaBs", 0.0).coerceAtLeast(0.0),
            logoUri = business.optString("logo").ifBlank { business.optString("logoUri") },
            notificationsEnabled = configJson.optBoolean("notificaciones", true),
            reminderMinutes = configJson.optInt("minutosRecordatorio", 60).coerceIn(5, 10080),
            lastReceiptNumber = configJson.optInt("ultimoFolio", receipts.size).coerceAtLeast(0),
            lastRentalNumber = configJson.optInt("ultimoFolioAlquiler", rentals.size).coerceAtLeast(0)
        )

        val logs = mutableListOf<BitacoraEntity>()
        val logsJson = data.optJSONArray("bitacora") ?: JSONArray()
        for (i in 0 until logsJson.length()) {
            val item = logsJson.optJSONObject(i) ?: continue
            logs += BitacoraEntity(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                fecha = parseTime(item.optString("fecha")),
                tipo = item.optString("tipo"),
                entidad = item.optString("entidad"),
                descripcion = item.optString("descripcion")
            )
        }
        return ParsedBackup(
            NativeBackupSnapshot(clients, tents, rentals, rentalItems, receipts, logs, config),
            ImportReport(clients.size, tents.size, rentals.size, receipts.size, corrected)
        )
    }

    private fun legacyReceiptData(snapshot: ReceiptSnapshot): JSONObject {
        return JSONObject()
            .put("folio", snapshot.folio)
            .put("emitidoEn", iso(snapshot.emittedAt))
            .put("concepto", snapshot.concept)
            .put("estado", snapshot.paymentStatus.name.lowercase(Locale.ROOT))
            .put("monto", snapshot.amountCents / 100.0)
            .put("negocio", JSONObject()
                .put("nombre", snapshot.businessName)
                .put("rif", snapshot.businessRif)
                .put("telefono", snapshot.businessPhone)
                .put("direccion", snapshot.businessAddress)
                .put("moneda", "$")
                .put("logo", snapshot.logoUri)
                .put("tasaBs", snapshot.exchangeRate))
            .put("cliente", JSONObject()
                .put("nombre", snapshot.clientName)
                .put("cedula", snapshot.clientDocument)
                .put("telefono", snapshot.clientPhone)
                .put("direccion", snapshot.clientAddress))
            .put("alquiler", JSONObject()
                .put("folio", snapshot.rentalFolio)
                .put("modalidad", snapshot.mode.code)
                .put("fechaInicio", iso(snapshot.startAt))
                .put("fechaDevolucion", iso(snapshot.returnAt))
                .put("direccion", snapshot.eventAddress)
                .put("lat", snapshot.latitude ?: JSONObject.NULL)
                .put("lng", snapshot.longitude ?: JSONObject.NULL)
                .put("montoTotal", snapshot.rentalTotalCents / 100.0)
                .put("abono", snapshot.rentalDepositCents / 100.0)
                .put("items", JSONArray().apply {
                    snapshot.items.forEach { item ->
                        put(JSONObject()
                            .put("nombre", item.name)
                            .put("tamano", item.size)
                            .put("cantidad", item.quantity)
                            .put("tarifa", item.tariffCents / 100.0))
                    }
                }))
    }

    private fun exportLogo(raw: String): String {
        val value = raw.trim()
        if (value.isBlank() || value.startsWith("data:")) return value
        return runCatching {
            val uri = Uri.parse(value)
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().takeIf { it.size <= 5 * 1024 * 1024 }
            } ?: return@runCatching value
            val mime = context.contentResolver.getType(uri)
                ?.takeIf { it.startsWith("image/") }
                ?: "image/png"
            "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        }.getOrDefault(value)
    }

    private fun amountToCents(item: JSONObject, key: String): Long {
        val value = item.opt(key)
        return when (value) {
            is Number -> (value.toDouble() * 100.0).roundToLong()
            is String -> parseDecimalCents(value)
            else -> 0L
        }
    }

    private fun parseDecimalCents(value: String): Long = runCatching {
        val normalized = value.trim().replace(" ", "")
            .let { if (it.contains(',') && it.contains('.')) it.replace(".", "").replace(',', '.') else it.replace(',', '.') }
        (normalized.toDouble() * 100.0).roundToLong()
    }.getOrDefault(0L)

    private fun parseTime(value: String): Long {
        val raw = value.trim()
        if (raw.isBlank() || raw == "null") return 0L
        raw.toLongOrNull()?.let { return it }
        runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()?.let { return it }
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

    private fun iso(millis: Long): String =
        Instant.ofEpochMilli(millis).toString()

    private fun isoNow(): String = iso(System.currentTimeMillis())

    private data class ParsedBackup(val snapshot: NativeBackupSnapshot, val report: ImportReport)
}

data class ImportReport(
    val clients: Int,
    val tents: Int,
    val rentals: Int,
    val receipts: Int,
    val correctedLegacyRentals: Int
) {
    val summary: String
        get() = "$clients clientes, $tents toldos, $rentals alquileres y $receipts recibos importados" +
            if (correctedLegacyRentals > 0) ". $correctedLegacyRentals alquileres antiguos se asumieron de 24 horas." else "."
}

private fun tentStatusFromExport(value: String?): TentStatus = when (value?.lowercase(Locale.ROOT)) {
    "disponible", "available" -> TentStatus.AVAILABLE
    "alquilado", "rented" -> TentStatus.RENTED
    "en_reparacion", "repair", "en reparacion" -> TentStatus.REPAIR
    "retirado", "retired" -> TentStatus.RETIRED
    else -> TentStatus.AVAILABLE
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key).takeUnless { it.isNaN() }
}

private fun AlquilerEntity.statusForExport(): String = when (estado) {
    RentalStatus.ACTIVE.name -> "activo"
    RentalStatus.DELIVERED.name -> "entregado"
    RentalStatus.RETURNED.name -> "devuelto"
    else -> "cancelado"
}
