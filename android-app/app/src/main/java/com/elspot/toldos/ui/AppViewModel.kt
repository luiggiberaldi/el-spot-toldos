package com.elspot.toldos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elspot.toldos.ElSpotApplication
import com.elspot.toldos.data.AlquilerEntity
import com.elspot.toldos.data.AlquilerItemEntity
import com.elspot.toldos.data.AppRepository
import com.elspot.toldos.data.AppUiState
import com.elspot.toldos.data.BitacoraEntity
import com.elspot.toldos.data.ClienteEntity
import com.elspot.toldos.data.ConfigSnapshot
import com.elspot.toldos.data.ReciboEntity
import com.elspot.toldos.data.RentalDraft
import com.elspot.toldos.data.RentalItemDraft
import com.elspot.toldos.data.RentalMode
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.ReceiptPaymentStatus
import com.elspot.toldos.data.ReceiptSnapshot
import com.elspot.toldos.domain.formatVenezuelanDocument
import com.elspot.toldos.domain.formatVenezuelanPhone
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.data.ToldoEntity
import com.elspot.toldos.data.NativeBackupSnapshot
import com.elspot.toldos.data.BackupManager
import com.elspot.toldos.domain.parseDollarCents
import com.elspot.toldos.notifications.NotificationScheduler
import com.elspot.toldos.share.ReceiptPdfService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AppEvent {
    data class Notice(val text: String) : AppEvent
    data class Error(val text: String) : AppEvent
    data class ReceiptCreated(val receipt: ReciboEntity) : AppEvent
    data class BackupImported(val summary: String) : AppEvent
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ElSpotApplication
    private val repository: AppRepository = app.repository
    private val backupManager: BackupManager = app.backupManager
    private val scheduler: NotificationScheduler = app.notificationScheduler
    private val receiptPdf = ReceiptPdfService(application)
    private val operation = MutableStateFlow(OperationState())
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    val state: StateFlow<AppUiState> = combine(repository.state, operation) { data, action ->
        data.copy(busy = action.busy, message = action.message, error = action.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        scheduler.createChannels()
        viewModelScope.launch {
            repository.state.collect { snapshot ->
                if (snapshot.config.notificationsEnabled) {
                    snapshot.rentals
                        .filter { it.estado == RentalStatus.ACTIVE.name || it.estado == RentalStatus.DELIVERED.name }
                        .forEach { scheduleNotifications(it, snapshot.config) }
                } else {
                    scheduler.cancelAllReminders()
                }
            }
        }
    }

    fun clearFeedback() {
        operation.value = OperationState()
    }

    fun saveClient(
        existing: ClienteEntity?,
        name: String,
        document: String,
        phone: String,
        address: String,
        notes: String
    ) {
        if (name.trim().isBlank()) return fail("El nombre del cliente es obligatorio.")
        val entity = ClienteEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            nombre = name.trim(),
            cedula = formatVenezuelanDocument(document),
            telefono = formatVenezuelanPhone(phone),
            email = existing?.email.orEmpty(),
            direccion = address.trim(),
            notas = notes.trim(),
            creadoEn = existing?.creadoEn ?: System.currentTimeMillis()
        )
        runAction("Cliente guardado") { repository.saveClient(entity) }
    }

    fun deleteClient(id: String) = runAction("Cliente eliminado") { repository.deleteClient(id) }

    fun saveTent(
        existing: ToldoEntity?,
        name: String,
        size: String,
        tariff: String,
        tariff12h: String,
        units: String,
        status: TentStatus,
        notes: String
    ) {
        val cents = parseDollarCents(tariff)
        val cents12h = parseDollarCents(tariff12h)
        if (name.trim().isBlank()) return fail("El nombre del toldo es obligatorio.")
        if (cents == null || cents < 0 || cents12h == null || cents12h < 0) return fail("Los precios de 12 y 24 horas deben ser válidos.")
        val parsedUnits = units.trim().toIntOrNull()
        if (parsedUnits == null || parsedUnits < 1) return fail("Las unidades deben ser un número mayor que 0.")
        val entity = ToldoEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            nombre = name.trim(),
            tamano = size.trim(),
            tarifaCents = cents,
            tarifa12hCents = cents12h,
            unidades = parsedUnits,
            estado = status.name,
            notas = notes.trim(),
            creadoEn = existing?.creadoEn ?: System.currentTimeMillis()
        )
        runAction("Toldo guardado") { repository.saveTent(entity) }
    }

    fun deleteTent(id: String) = runAction("Toldo eliminado") { repository.deleteTent(id) }

    fun saveRental(draft: RentalDraft) {
        runAction("Alquiler guardado") {
            val result = repository.saveRental(draft)
            result.getOrThrow().also { saved ->
                val config = repository.snapshot().config
                if (config.notificationsEnabled && (saved.estado == RentalStatus.ACTIVE.name || saved.estado == RentalStatus.DELIVERED.name)) {
                    scheduleNotifications(saved, config)
                } else {
                    scheduler.cancelRental(saved.id)
                }
            }
        }
    }

    fun updateRentalStatus(id: String, status: RentalStatus, reason: String = "") {
        runAction("Estado actualizado") {
            val updated = repository.updateRentalStatus(id, status, reason)
            if (updated == null || status == RentalStatus.RETURNED || status == RentalStatus.CANCELLED) {
                scheduler.cancelRental(id)
            } else if (status == RentalStatus.ACTIVE || status == RentalStatus.DELIVERED) {
                val config = repository.snapshot().config
                if (config.notificationsEnabled) scheduleNotifications(updated, config)
                else scheduler.cancelRental(id)
            }
        }
    }

    fun deleteRental(id: String) {
        runAction("Alquiler eliminado") {
            scheduler.cancelRental(id)
            repository.deleteRental(id)
        }
    }

    fun emitReceipt(rentalId: String, amountCents: Long, concept: String, paymentStatus: ReceiptPaymentStatus) {
        runAction(null) {
            val result = repository.emitReceipt(rentalId, amountCents, concept, paymentStatus)
            result.getOrThrow().also { receipt ->
                _events.emit(AppEvent.ReceiptCreated(receipt))
            }
        }
    }

    fun checkForUpdates() {
        scheduler.checkForUpdatesNow(manual = true)
    }

    fun saveConfig(config: ConfigSnapshot) {
        runAction("Configuración guardada") {
            repository.saveConfig(config)
            if (!config.notificationsEnabled) scheduler.cancelAllReminders()
        }
    }

    fun shareReceipt(receipt: ReciboEntity, textOnly: Boolean = false) {
        runAction(null) {
            val snapshot = repository.receiptSnapshot(receipt)
                ?: error("No se pudo leer el snapshot del recibo.")
            if (textOnly) receiptPdf.shareText(snapshot) else receiptPdf.share(snapshot)
        }
    }

    fun shareReceiptWhatsApp(receipt: ReciboEntity) {
        runAction(null) {
            val snapshot = repository.receiptSnapshot(receipt)
                ?: error("No se pudo leer el snapshot del recibo.")
            receiptPdf.shareWhatsApp(snapshot)
        }
    }

    fun importBackup(uri: android.net.Uri) {
        runAction(null) {
            scheduler.cancelAllReminders()
            val report = backupManager.importFrom(uri)
            _events.emit(AppEvent.BackupImported(report.summary))
        }
    }

    fun exportBackup(uri: android.net.Uri) {
        runAction("Respaldo exportado") { backupManager.exportTo(uri) }
    }

    fun resetAll() {
        runAction("Datos restablecidos") {
            scheduler.cancelAllReminders()
            repository.clearAll()
        }
    }

    suspend fun rentalItems(id: String): List<AlquilerItemEntity> = repository.rentalItems(id)
    suspend fun snapshotFor(receipt: ReciboEntity): ReceiptSnapshot? = repository.receiptSnapshot(receipt)

    private fun scheduleNotifications(rental: AlquilerEntity, config: ConfigSnapshot) {
        scheduler.scheduleRentalReminder(
            rentalId = rental.id,
            rentalFolio = rental.folio,
            returnAt = rental.devolucion,
            reminderMinutes = config.reminderMinutes
        )
        scheduler.scheduleExpiredReminder(rental.id, rental.folio, rental.devolucion)
    }

    private fun runAction(success: String?, block: suspend () -> Unit) {
        if (operation.value.busy) return
        viewModelScope.launch {
            operation.value = OperationState(busy = true)
            try {
                block()
                operation.value = OperationState(message = success)
                if (success != null) _events.emit(AppEvent.Notice(success))
            } catch (error: Throwable) {
                fail(error.message ?: "Ocurrió un error.")
            }
        }
    }

    private fun fail(message: String) {
        operation.value = OperationState(error = message)
        _events.tryEmit(AppEvent.Error(message))
    }

    private data class OperationState(
        val busy: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}
