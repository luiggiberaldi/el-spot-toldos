package com.elspot.toldos.data

import androidx.room.withTransaction
import com.elspot.toldos.domain.calculateRentalTotal
import com.elspot.toldos.domain.validateRentalDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import kotlin.math.roundToLong

/**
 * Orquestador de datos de la APK. Mantiene las operaciones de negocio fuera de
 * Compose y garantiza que los cambios que afectan inventario sean atomicos.
 */
class AppRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore
) {
    private val clients = db.clientes()
    private val tents = db.toldos()
    private val rentals = db.alquileres()
    private val receipts = db.recibos()
    private val log = db.bitacora()

    val state: Flow<AppUiState> = combine(
        clients.observeAll(),
        tents.observeAll(),
        rentals.observeAll(),
        receipts.observeAll(),
        log.observeAll(),
        rentals.observeAllItems(),
        settings.configFlow
    ) { values ->
        AppUiState(
            clients = values[0] as List<ClienteEntity>,
            tents = values[1] as List<ToldoEntity>,
            rentals = values[2] as List<AlquilerEntity>,
            receipts = values[3] as List<ReciboEntity>,
            log = values[4] as List<BitacoraEntity>,
            rentalItems = values[5] as List<AlquilerItemEntity>,
            config = values[6] as ConfigSnapshot
        )
    }

    suspend fun saveClient(entity: ClienteEntity) {
        val capitalized = entity.copy(nombre = com.elspot.toldos.domain.capitalizeWords(entity.nombre))
        clients.insert(capitalized)
        addLog("Cambio", "Cliente", "Cliente actualizado: ${capitalized.nombre}")
    }

    suspend fun deleteClient(id: String) {
        val existing = clients.findById(id)
        clients.deleteById(id)
        addLog("Corrección", "Cliente", "Cliente eliminado: ${existing?.nombre ?: id}")
    }

    suspend fun saveTent(entity: ToldoEntity) {
        require(entity.unidades > 0) { "Las unidades deben ser mayores que 0." }
        val existing = tents.findById(entity.id)
        val activeUnits = rentals.activeItems()
            .filter { it.toldoId == entity.id }
            .sumOf { it.cantidad }
        val inActiveRental = activeUnits > 0
        require(entity.unidades >= activeUnits) {
            "Las unidades no pueden ser menores que las ya asignadas a alquileres activos."
        }
        require(!inActiveRental || TentStatus.from(entity.estado) == TentStatus.RENTED) {
            "Este toldo está asignado a un alquiler activo y debe conservar estado alquilado."
        }
        require(TentStatus.from(entity.estado) != TentStatus.RENTED || inActiveRental) {
            "El estado alquilado solo puede asignarse desde un alquiler activo."
        }
        require(existing != null || TentStatus.from(entity.estado) != TentStatus.RENTED) {
            "Un toldo nuevo debe iniciar como disponible, en reparación o retirado."
        }
        val capitalized = entity.copy(nombre = com.elspot.toldos.domain.capitalizeWords(entity.nombre))
        tents.insert(capitalized)
        addLog("Cambio", "Toldo", "Toldo actualizado: ${capitalized.nombre}")
    }

    suspend fun deleteTent(id: String) {
        val existing = tents.findById(id)
        require(rentals.activeItems().none { it.toldoId == id }) {
            "No puedes eliminar un toldo asignado a un alquiler activo."
        }
        tents.deleteById(id)
        addLog("Corrección", "Toldo", "Toldo eliminado: ${existing?.nombre ?: id}")
    }

    suspend fun saveRental(draft: RentalDraft): Result<AlquilerEntity> {
        return try {
            val now = System.currentTimeMillis()
            val id = draft.id ?: UUID.randomUUID().toString()
            val previous = draft.id?.let { rentals.findById(it) }
            val tentMap = tents.allOnce().associateBy { it.id }
            val occupiedUnitsByOther = rentals.activeItems()
                .filter { it.alquilerId != id }
                .groupingBy { it.toldoId }
                .fold(0) { total, item -> total + item.cantidad }
            validateRentalDraft(
                draft = draft,
                clientExists = clients.findById(draft.clientId) != null,
                tentsById = tentMap,
                occupiedUnitsByTent = occupiedUnitsByOther
            )?.let { validationError ->
                throw IllegalArgumentException(validationError)
            }
            val calculatedTotal = calculateRentalTotal(draft.items, draft.mode)
            val entity = AlquilerEntity(
                id = id,
                folio = previous?.folio ?: settings.nextRentalFolio(),
                clienteId = draft.clientId,
                modalidad = draft.mode.name,
                inicio = draft.startAt,
                devolucion = draft.startAt + draft.mode.hours * 60L * 60L * 1000L,
                direccion = draft.address.trim(),
                latitud = draft.latitude,
                longitud = draft.longitude,
                montoTotalCents = calculatedTotal,
                abonoCents = draft.depositCents,
                estado = draft.status.name,
                notas = draft.notes.trim(),
                creadoEn = previous?.creadoEn ?: now,
                actualizadoEn = now
            )
            db.withTransaction {
                rentals.insert(entity)
                rentals.deleteItems(id)
                rentals.insertItems(
                    draft.items.mapIndexed { index, item ->
                        AlquilerItemEntity(
                            alquilerId = id,
                            linea = index,
                            toldoId = item.tentId,
                            cantidad = item.quantity,
                            tarifaCents = item.tariffCents
                        )
                    }
                )
                syncTentStatuses()
                addLog(
                    if (previous == null) "Nuevo" else "Cambio",
                    "Alquiler",
                    "${if (previous == null) "Creado" else "Actualizado"} ${entity.folio}"
                )
            }
            Result.success(entity)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    suspend fun rentalItems(id: String): List<AlquilerItemEntity> = rentals.itemsFor(id)

    suspend fun receiptSnapshot(entity: ReciboEntity): ReceiptSnapshot? {
        val raw = entity.snapshotJson
        return (ReceiptSnapshot.fromJson(raw)
            ?: runCatching { ReceiptSnapshot.fromLegacyJson(org.json.JSONObject(raw)) }.getOrNull())
            ?.copy(paymentStatus = ReceiptPaymentStatus.from(entity.estadoPago))
    }

    suspend fun updateRentalStatus(id: String, status: RentalStatus, reason: String = ""): AlquilerEntity? {
        val current = rentals.findById(id) ?: return null
        return db.withTransaction {
            if (status == RentalStatus.ACTIVE || status == RentalStatus.DELIVERED) {
                val items = rentals.itemsFor(id)
                val tentMap = tents.allOnce().associateBy { it.id }
                val occupiedUnitsByOther = rentals.activeItems()
                    .filter { it.alquilerId != id }
                    .groupingBy { it.toldoId }
                    .fold(0) { total, item -> total + item.cantidad }
                items.forEach { item ->
                    val tent = tentMap[item.toldoId] ?: error("El toldo de una línea ya no existe.")
                    require(item.cantidad > 0) { "La cantidad del alquiler debe ser mayor que 0." }
                    require(TentStatus.from(tent.estado) !in setOf(TentStatus.REPAIR, TentStatus.RETIRED)) {
                        "No puedes activar un alquiler con un toldo en reparación o retirado."
                    }
                    require(item.cantidad + occupiedUnitsByOther.getOrDefault(item.toldoId, 0) <= tent.unidades) {
                        "La cantidad solicitada supera las unidades disponibles del inventario."
                    }
                }
            }
            val updated = current.copy(estado = status.name, actualizadoEn = System.currentTimeMillis())
            rentals.update(updated)
            syncTentStatuses()
            val suffix = reason.trim().takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
            addLog("Cambio", "Alquiler", "${current.folio} marcado como ${status.label}$suffix")
            updated
        }
    }

    suspend fun deleteRental(id: String) {
        val current = rentals.findById(id)
        db.withTransaction {
            rentals.deleteItems(id)
            rentals.deleteById(id)
            syncTentStatuses()
            addLog("Corrección", "Alquiler", "Alquiler eliminado: ${current?.folio ?: id}")
        }
    }

    suspend fun emitReceipt(
        rentalId: String,
        amountCents: Long,
        concept: String,
        paymentStatus: ReceiptPaymentStatus
    ): Result<ReciboEntity> {
        return try {
            require(amountCents > 0) { "El monto debe ser mayor que 0." }
            val rental = rentals.findById(rentalId) ?: error("No se encontró el alquiler.")
            require(rental.estado != RentalStatus.CANCELLED.name) { "No se puede cobrar un alquiler cancelado." }
            val balance = (rental.montoTotalCents - rental.abonoCents).coerceAtLeast(0L)
            require(amountCents <= rental.montoTotalCents) {
                "El monto no puede superar el total del alquiler."
            }
            val registerDeposit = paymentStatus == ReceiptPaymentStatus.PAID
            require(!registerDeposit || amountCents <= balance) {
                "El abono no puede superar el saldo pendiente."
            }
            val client = clients.findById(rental.clienteId)
            val rentalItems = rentals.itemsFor(rentalId)
            val tentMap = tents.allOnce().associateBy { it.id }
            val config = settings.current()
            val mode = RentalMode.from(rental.modalidad)
            val depositAfterReceipt = if (registerDeposit) {
                (rental.abonoCents + amountCents).coerceAtMost(rental.montoTotalCents)
            } else {
                rental.abonoCents
            }

            val folio = settings.nextReceiptFolio()
            val snapshot = ReceiptSnapshot(
                id = UUID.randomUUID().toString(),
                folio = folio,
                rentalFolio = rental.folio,
                rentalId = rental.id,
                emittedAt = System.currentTimeMillis(),
                concept = concept.trim().ifBlank { "Pago del alquiler" },
                amountCents = amountCents,
                paymentStatus = paymentStatus,
                businessName = config.businessName,
                businessRif = config.rif,
                businessPhone = config.phone,
                businessAddress = config.address,
                exchangeRate = config.exchangeRate,
                logoUri = config.logoUri,
                clientName = client?.nombre ?: "Cliente eliminado",
                clientDocument = client?.cedula ?: "",
                clientPhone = client?.telefono ?: "",
                clientAddress = client?.direccion ?: "",
                mode = mode,
                startAt = rental.inicio,
                returnAt = rental.devolucion,
                eventAddress = rental.direccion,
                latitude = rental.latitud,
                longitude = rental.longitud,
                rentalTotalCents = rental.montoTotalCents,
                rentalDepositCents = depositAfterReceipt,
                items = rentalItems.map {
                    val tent = tentMap[it.toldoId]
                    ReceiptItemSnapshot(
                        name = tent?.nombre ?: "Toldo eliminado",
                        size = tent?.tamano ?: "",
                        quantity = it.cantidad,
                        tariffCents = it.tarifaCents
                    )
                }
            )
            val entity = ReciboEntity(
                id = snapshot.id,
                folio = folio,
                alquilerId = rental.id,
                emitidoEn = snapshot.emittedAt,
                concepto = snapshot.concept,
                montoCents = amountCents,
                snapshotJson = snapshot.toJson(),
                estadoPago = paymentStatus.name
            )
            db.withTransaction {
                receipts.insert(entity)
                if (paymentStatus == ReceiptPaymentStatus.PAID) {
                    rentals.update(
                        rental.copy(
                            abonoCents = (rental.abonoCents + amountCents).coerceAtMost(rental.montoTotalCents),
                            actualizadoEn = System.currentTimeMillis()
                        )
                    )
                }
                addLog("Nuevo", "Recibo", "Emitido $folio para ${rental.folio}")
            }
            Result.success(entity)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    suspend fun saveConfig(config: ConfigSnapshot) {
        require(config.businessName.trim().isNotBlank()) { "El nombre del negocio es obligatorio." }
        val safeRate = if (config.exchangeRate.isFinite()) config.exchangeRate.coerceAtLeast(0.0) else 0.0
        settings.save(
            config.copy(
                businessName = com.elspot.toldos.domain.capitalizeWords(config.businessName),
                rif = com.elspot.toldos.domain.formatVenezuelanDocument(config.rif),
                phone = com.elspot.toldos.domain.formatVenezuelanPhone(config.phone),
                exchangeRate = safeRate
            )
        )
        addLog("Cambio", "Configuración", "Configuración de EL SPOT actualizada")
    }

    suspend fun addLog(type: String, entity: String, description: String) {
        log.insert(
            BitacoraEntity(
                id = UUID.randomUUID().toString(),
                fecha = System.currentTimeMillis(),
                tipo = type,
                entidad = entity,
                descripcion = description
            )
        )
    }

    suspend fun snapshot(): NativeBackupSnapshot = NativeBackupSnapshot(
        clients = clients.allOnce(),
        tents = tents.allOnce(),
        rentals = rentals.allOnce(),
        rentalItems = rentals.allItemsOnce(),
        receipts = receipts.allOnce(),
        log = log.allOnce(),
        config = settings.current()
    )

    suspend fun clearAll() {
        db.withTransaction {
            clients.deleteAll()
            tents.deleteAll()
            rentals.deleteAllItems()
            rentals.deleteAll()
            receipts.deleteAll()
            log.deleteAll()
        }
        settings.save(ConfigSnapshot())
        addLog("Corrección", "Sistema", "Todos los datos fueron restablecidos")
    }

    suspend fun replaceAll(snapshot: NativeBackupSnapshot) {
        db.withTransaction {
            clients.deleteAll()
            tents.deleteAll()
            rentals.deleteAllItems()
            rentals.deleteAll()
            receipts.deleteAll()
            log.deleteAll()
            snapshot.clients.forEach { clients.insert(it) }
            snapshot.tents.forEach { tents.insert(it) }
            snapshot.rentals.forEach { rentals.insert(it) }
            rentals.insertItems(snapshot.rentalItems)
            snapshot.receipts.forEach { receipts.insert(it) }
            snapshot.log.forEach { log.insert(it) }
            settings.save(snapshot.config)
            syncTentStatuses()
            addLog("Cambio", "Respaldo", "Respaldo restaurado en la APK")
        }
    }

    private suspend fun syncTentStatuses() {
        val activeTentIds = rentals.activeItems().map { it.toldoId }.toSet()
        tents.allOnce().forEach { tent ->
            val current = TentStatus.from(tent.estado)
            if (current == TentStatus.REPAIR || current == TentStatus.RETIRED) return@forEach
            val target = if (tent.id in activeTentIds) TentStatus.RENTED else TentStatus.AVAILABLE
            if (current != target) tents.updateStatus(tent.id, target.name)
        }

    }

    companion object {
        fun effectiveTariff(baseCents: Long, mode: RentalMode): Long =
            if (mode == RentalMode.H12) (baseCents / 2.0).roundToLong() else baseCents

        fun calculateTotal(items: List<RentalItemDraft>, mode: RentalMode): Long {
            val subtotal = items.sumOf { it.tariffCents * it.quantity.toLong() }
            return effectiveTariff(subtotal, mode)
        }
    }
}

data class NativeBackupSnapshot(
    val clients: List<ClienteEntity>,
    val tents: List<ToldoEntity>,
    val rentals: List<AlquilerEntity>,
    val rentalItems: List<AlquilerItemEntity>,
    val receipts: List<ReciboEntity>,
    val log: List<BitacoraEntity>,
    val config: ConfigSnapshot
)
