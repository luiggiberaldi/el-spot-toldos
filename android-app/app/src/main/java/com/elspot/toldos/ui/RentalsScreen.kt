package com.elspot.toldos.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elspot.toldos.data.AlquilerEntity
import com.elspot.toldos.data.AlquilerItemEntity
import com.elspot.toldos.data.AppUiState
import com.elspot.toldos.data.RentalDraft
import com.elspot.toldos.data.RentalItemDraft
import com.elspot.toldos.data.RentalMode
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.ReceiptPaymentStatus
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.data.ToldoEntity
import com.elspot.toldos.domain.calculateReturnAt
import com.elspot.toldos.domain.centsToDollarText
import com.elspot.toldos.domain.effectiveTariffCents
import com.elspot.toldos.domain.formatDateTime
import com.elspot.toldos.domain.formatDual
import com.elspot.toldos.domain.parseDollarCents
import com.elspot.toldos.location.LocationService
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun RentalsScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    requestedRentalId: String? = null,
    requestNewRental: Boolean = false,
    onRentalRequestHandled: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<AlquilerEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<AlquilerEntity?>(null) }
    var deleting by remember { mutableStateOf<AlquilerEntity?>(null) }
    var receiptRental by remember { mutableStateOf<AlquilerEntity?>(null) }

    LaunchedEffect(requestedRentalId, requestNewRental, state.rentals) {
        if (requestedRentalId != null) {
            state.rentals.firstOrNull { it.id == requestedRentalId }?.let {
                detail = it
                onRentalRequestHandled()
            }
        } else if (requestNewRental) {
            editing = null
            showForm = true
            onRentalRequestHandled()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AppEvent.Notice && event.text == "Alquiler guardado") showForm = false
        }
    }

    val filtered = state.rentals.filter { rental ->
        val client = state.clients.firstOrNull { it.id == rental.clienteId }
        "${rental.folio} ${client?.nombre.orEmpty()} ${rental.direccion}".contains(query, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Alquileres", "${state.rentals.size} registrado${if (state.rentals.size == 1) "" else "s"}", action = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) { Icon(Icons.Default.Add, "Nuevo alquiler") }
        })
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Buscar por folio, cliente o dirección") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        if (filtered.isEmpty()) {
            EmptyState("Sin alquileres", "Registra un alquiler para controlar su devolución y saldo.", Icons.Default.ReceiptLong, action = { Button(onClick = { editing = null; showForm = true }) { Text("Nuevo alquiler") } })
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { rental ->
                    RentalRow(rental, state, { detail = rental }, { editing = rental; showForm = true }, { deleting = rental })
                }
            }
        }
    }

    if (showForm) RentalFormDialog(editing, state, viewModel, { showForm = false }) { viewModel.saveRental(it) }
    detail?.let { requested ->
        val rental = state.rentals.firstOrNull { it.id == requested.id } ?: requested
        RentalDetailDialog(rental, state, viewModel, { detail = null }, { detail = null; editing = rental; showForm = true }, { detail = null; deleting = rental }, { detail = null; receiptRental = rental })
    }
    deleting?.let { rental ->
        ConfirmDialog("Eliminar alquiler", "Se eliminará ${rental.folio}. Los recibos emitidos se conservarán.", "Eliminar", { deleting = null }, { viewModel.deleteRental(rental.id); deleting = null }, danger = true)
    }
    receiptRental?.let { rental -> ReceiptFormDialog(rental, state, viewModel) { receiptRental = null } }
}

@Composable
private fun RentalRow(rental: AlquilerEntity, state: AppUiState, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val client = state.clients.firstOrNull { it.id == rental.clienteId }
    val balance = (rental.montoTotalCents - rental.abonoCents).coerceAtLeast(0L)
    Card(onClick = onOpen, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${rental.folio} · ${client?.nombre ?: "Cliente eliminado"}", fontWeight = FontWeight.SemiBold)
                Text("${if (RentalMode.from(rental.modalidad) == RentalMode.H12) "12 horas" else "24 horas"} · devuelve ${formatDateTime(rental.devolucion)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total ${formatDual(rental.montoTotalCents, state.config)} · Pendiente ${formatDual(balance, state.config)}", style = MaterialTheme.typography.bodySmall, color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                if (rental.direccion.isNotBlank()) Text(rental.direccion, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(RentalStatus.from(rental.estado))
                Row { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar alquiler") }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar alquiler", tint = MaterialTheme.colorScheme.error) } }
            }
        }
    }
}

private data class RentalLineState(val tentId: String = "", val quantity: String = "1", val tariff: String = "")

@Composable
private fun FormLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun FormDivider() {
    Divider(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
}

@Composable
private fun RentalFormDialog(initial: AlquilerEntity?, state: AppUiState, viewModel: AppViewModel, onDismiss: () -> Unit, onSave: (RentalDraft) -> Unit) {
    var clientId by remember(initial) { mutableStateOf(initial?.clienteId ?: "") }
    val lines = remember(initial?.id) { mutableStateListOf<RentalLineState>() }
    LaunchedEffect(initial?.id) {
        lines.clear()
        if (initial == null) lines.add(RentalLineState()) else {
            viewModel.rentalItems(initial.id).forEach { lines.add(RentalLineState(it.toldoId, it.cantidad.toString(), "%.2f".format(it.tarifaCents / 100.0))) }
            if (lines.isEmpty()) lines.add(RentalLineState())
        }
    }
    var mode by remember(initial) { mutableStateOf(RentalMode.from(initial?.modalidad)) }
    var startAt by remember(initial) { mutableStateOf(initial?.inicio ?: System.currentTimeMillis()) }
    var address by remember(initial) { mutableStateOf(initial?.direccion ?: "") }
    var latitude by remember(initial) { mutableStateOf(initial?.latitud) }
    var longitude by remember(initial) { mutableStateOf(initial?.longitud) }
    var deposit by remember(initial) { mutableStateOf(initial?.let { "%.2f".format(it.abonoCents / 100.0) } ?: "") }
    var status by remember(initial) { mutableStateOf(RentalStatus.from(initial?.estado)) }
    var notes by remember(initial) { mutableStateOf(initial?.notas ?: "") }
    var error by remember(initial) { mutableStateOf<String?>(null) }
    var locationError by remember(initial) { mutableStateOf<String?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val location = remember { LocationService(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            scope.launch { capturing = true; locationError = null; try { val result = location.current(); latitude = result.latitude; longitude = result.longitude; if (address.isBlank() && !result.address.isNullOrBlank()) address = result.address } catch (t: Throwable) { locationError = t.message ?: "No se pudo obtener la ubicación." } finally { capturing = false } }
        } else locationError = "Permiso de ubicación denegado. Actívalo en los ajustes del dispositivo."
    }
    val totalCents = remember(lines.toList(), mode) {
        val base = lines.sumOf { (parseDollarCents(it.tariff) ?: 0L) * (it.quantity.toIntOrNull()?.coerceAtLeast(0) ?: 0) }
        if (mode == RentalMode.H12) Math.round(base / 2.0) else base
    }
    val parsedDeposit = if (deposit.isBlank()) 0L else parseDollarCents(deposit)
    val depositCents = parsedDeposit ?: 0L
    val returnAt = calculateReturnAt(startAt, mode.hours)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo alquiler" else "Editar ${initial.folio}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { ChoiceField("Cliente", clientId, state.clients.map { it.id to it.nombre }, { clientId = it }) }
                item { FormLabel("Toldos") }
                items(lines.size, key = { it }) { index -> RentalLineEditor(index, lines[index], state, mode, initial?.id, status == RentalStatus.ACTIVE || status == RentalStatus.DELIVERED, { lines[index] = it }, { if (lines.size > 1) lines.removeAt(index) }) }
                item { TextButton(onClick = { lines.add(RentalLineState()) }) { Icon(Icons.Default.Add, null); Spacer(Modifier.size(5.dp)); Text("Agregar toldo") } }
                item { FormDivider() }
                item { FormLabel("Duración y devolución") }
                item { ChoiceField("Modalidad", mode, RentalMode.entries.map { it to "${it.label} (${if (it == RentalMode.H12) "precio configurado 12h" else "precio configurado 24h"})" }, { selected -> mode = selected; lines.replaceAll { line -> val tent = state.tents.firstOrNull { it.id == line.tentId }; val price = if (selected == RentalMode.H12) tent?.tarifa12hCents ?: tent?.tarifaCents?.div(2) else tent?.tarifaCents; line.copy(tariff = price?.let { "%.2f".format(it / 100.0) } ?: line.tariff) } }) }
                item { DateTimeField("Inicio del alquiler", startAt, { startAt = it }); Text("Devolución calculada: ${formatDateTime(returnAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp)) }
                item { FormDivider() }
                item { FormLabel("Entrega y ubicación") }
                item { OutlinedTextField(address, { address = it }, label = { Text("Dirección del evento") }, minLines = 2) }
                item { OutlinedButton(onClick = { if (location.hasPermission()) scope.launch { capturing = true; locationError = null; try { val result = location.current(); latitude = result.latitude; longitude = result.longitude; if (address.isBlank() && !result.address.isNullOrBlank()) address = result.address } catch (t: Throwable) { locationError = t.message } finally { capturing = false } } else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, enabled = !capturing, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.LocationOn, null); Spacer(Modifier.size(7.dp)); Text(if (capturing) "Capturando ubicación…" else "Capturar ubicación GPS") }; GpsSummary(latitude, longitude); ErrorMessage(locationError) }
                item { FormDivider() }
                item { FormLabel("Pago y estado") }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(deposit, { deposit = it }, label = { Text("Abono ($)") }, singleLine = true, modifier = Modifier.weight(1f)); ChoiceField("Estado", status, RentalStatus.entries.map { it to it.label }, { status = it }, modifier = Modifier.weight(1f)) } }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, minLines = 2) }
                item { MoneySummary(totalCents, depositCents, state.config) }
                item { ErrorMessage(error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = {
            Button(onClick = {
                val valid = lines.mapNotNull { line -> val tent = state.tents.firstOrNull { it.id == line.tentId }; val qty = line.quantity.toIntOrNull(); val tariff = parseDollarCents(line.tariff); if (tent != null && qty != null && qty > 0 && tariff != null && tariff >= 0) RentalItemDraft(tent.id, qty, tariff) else null }
                val malformed = lines.any { it.tentId.isBlank() || it.quantity.toIntOrNull()?.let { q -> q <= 0 } != false || parseDollarCents(it.tariff) == null }
                val managesInventory = status == RentalStatus.ACTIVE || status == RentalStatus.DELIVERED
                when {
                    clientId.isBlank() -> error = "Selecciona un cliente."
                    malformed -> error = "Revisa cada toldo, cantidad y tarifa."
                    valid.isEmpty() -> error = "Agrega al menos un toldo válido."
                    address.isBlank() && (latitude == null || longitude == null) -> error = "Indica la dirección o captura GPS."
                    (latitude == null) != (longitude == null) -> error = "La ubicación GPS debe incluir latitud y longitud."
                    valid.map { it.tentId }.distinct().size != valid.size -> error = "No repitas el mismo toldo en varias líneas."
                    managesInventory && valid.any { it.quantity > availableUnitsForTent(state, it.tentId, initial?.id) } -> error = "La cantidad solicitada supera las unidades disponibles."
                    parsedDeposit == null -> error = "Indica un abono válido."
                    depositCents > totalCents -> error = "El abono no puede superar el total."
                    else -> onSave(RentalDraft(initial?.id, clientId, valid, mode, startAt, address, latitude, longitude, totalCents, depositCents, status, notes))
                }
            }) { Text("Guardar") }
        }
    )
}

private fun availableUnitsForTent(state: AppUiState, tentId: String, exceptRentalId: String?): Int {
    val tent = state.tents.firstOrNull { it.id == tentId } ?: return 0
    val occupied = state.rentalItems.filter { it.toldoId == tentId && it.alquilerId != exceptRentalId }.filter { item -> state.rentals.any { rental -> rental.id == item.alquilerId && (rental.estado == RentalStatus.ACTIVE.name || rental.estado == RentalStatus.DELIVERED.name) } }.sumOf { it.cantidad }
    return (tent.unidades - occupied).coerceAtLeast(0)
}

@Composable
private fun RentalLineEditor(index: Int, line: RentalLineState, state: AppUiState, mode: RentalMode, currentRentalId: String?, allowInventory: Boolean, onChange: (RentalLineState) -> Unit, onRemove: () -> Unit) {
    val options = state.tents.filter { TentStatus.from(it.estado) != TentStatus.RETIRED }.filter { !allowInventory || TentStatus.from(it.estado) != TentStatus.REPAIR }.filter { !allowInventory || it.id == line.tentId || availableUnitsForTent(state, it.id, currentRentalId) > 0 }.map { tent -> tent.id to "${tent.nombre} (${tent.tamano.ifBlank { "sin tamaño" }} · ${availableUnitsForTent(state, tent.id, currentRentalId)}/${tent.unidades} disp.)" }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChoiceField("Toldo ${index + 1}", line.tentId, options, { id -> val tent = state.tents.firstOrNull { it.id == id }; val price = if (mode == RentalMode.H12) tent?.tarifa12hCents ?: tent?.tarifaCents?.div(2) else tent?.tarifaCents; onChange(line.copy(tentId = id, tariff = price?.let { "%.2f".format(it / 100.0) } ?: line.tariff)) })
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { OutlinedTextField(line.quantity, { onChange(line.copy(quantity = it)) }, label = { Text("Cantidad") }, singleLine = true, modifier = Modifier.weight(0.7f)); OutlinedTextField(line.tariff, { onChange(line.copy(tariff = it)) }, label = { Text(if (mode == RentalMode.H12) "Precio 12h ($)" else "Precio 24h ($)") }, singleLine = true, modifier = Modifier.weight(1.3f)); IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Quitar toldo", tint = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
private fun RentalDetailDialog(rental: AlquilerEntity, state: AppUiState, viewModel: AppViewModel, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onReceipt: () -> Unit) {
    val client = state.clients.firstOrNull { it.id == rental.clienteId }
    var rentalItems by remember(rental.id) { mutableStateOf<List<AlquilerItemEntity>>(emptyList()) }
    LaunchedEffect(rental.id) { rentalItems = viewModel.rentalItems(rental.id) }
    val balance = (rental.montoTotalCents - rental.abonoCents).coerceAtLeast(0L)
    val mode = RentalMode.from(rental.modalidad)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Alquiler ${rental.folio}") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusBadge(RentalStatus.from(rental.estado)); Text("Creado ${formatDateTime(rental.creadoEn)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        item { Text("Cliente: ${client?.nombre ?: "Cliente eliminado"}", fontWeight = FontWeight.SemiBold) }
        item { Text("Modalidad: ${mode.label}") }
        item { Text("Inicio: ${formatDateTime(rental.inicio)}") }
        item { Text("Devolución: ${formatDateTime(rental.devolucion)}", color = MaterialTheme.colorScheme.secondary) }
        item { Text("Dirección: ${rental.direccion.ifBlank { "—" }}") }
        item { GpsSummary(rental.latitud, rental.longitud) }
        item { Divider() }
        item { Text("Toldos", fontWeight = FontWeight.SemiBold) }
        items(rentalItems, key = { "${it.alquilerId}-${it.linea}" }) { line -> val tent = state.tents.firstOrNull { it.id == line.toldoId }; Text("${line.cantidad} × ${tent?.nombre ?: "Toldo eliminado"} — ${centsToDollarText(effectiveTariffCents(line.tarifaCents, mode) * line.cantidad)}", style = MaterialTheme.typography.bodySmall) }
        item { MoneySummary(rental.montoTotalCents, rental.abonoCents, state.config) }
    } }, dismissButton = { Row { TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error); Text("Eliminar") }; TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, null); Text("Editar") } } }, confirmButton = { Button(onClick = onReceipt, enabled = rental.montoTotalCents > 0L) { Icon(Icons.Default.ReceiptLong, null); Spacer(Modifier.size(6.dp)); Text("Emitir recibo") } })
}

@Composable
private fun ReceiptFormDialog(rental: AlquilerEntity, state: AppUiState, viewModel: AppViewModel, onDismiss: () -> Unit) {
    val balance = (rental.montoTotalCents - rental.abonoCents).coerceAtLeast(0L)
    var amount by remember(rental) { mutableStateOf(((if (balance > 0L) balance else rental.abonoCents) / 100.0).toString()) }
    var concept by remember(rental) { mutableStateOf(if (balance > 0L) "Saldo pendiente del alquiler" else "Abono ya recibido del alquiler") }
    var paymentStatus by remember(rental) { mutableStateOf(if (balance > 0L) ReceiptPaymentStatus.PAID else ReceiptPaymentStatus.DUE) }
    val amountCents = parseDollarCents(amount) ?: 0L
    var createdReceipt by remember { mutableStateOf<com.elspot.toldos.data.ReciboEntity?>(null) }
    LaunchedEffect(Unit) { viewModel.events.collect { event -> if (event is AppEvent.ReceiptCreated && event.receipt.alquilerId == rental.id) createdReceipt = event.receipt } }
    createdReceipt?.let { receipt -> AlertDialog(onDismissRequest = onDismiss, title = { Text("Recibo ${receipt.folio} emitido") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(42.dp)); Text("El recibo fue guardado y está listo para compartir por WhatsApp."); Text("Puedes enviar el PDF con el mensaje profesional o compartir solo el resumen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, dismissButton = { Row { TextButton(onClick = { viewModel.shareReceiptWhatsApp(receipt) }) { Icon(Icons.Default.Chat, null); Text("WhatsApp") }; TextButton(onClick = { viewModel.shareReceipt(receipt, true) }) { Text("Mensaje") }; TextButton(onClick = { viewModel.shareReceipt(receipt) }) { Text("PDF") } } }, confirmButton = { Button(onClick = onDismiss) { Text("Cerrar") } }); return }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Emitir recibo · ${rental.folio}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Pendiente actual: ${formatDual(balance, state.config)}", color = MaterialTheme.colorScheme.secondary); OutlinedTextField(amount, { amount = it }, label = { Text("Monto a cancelar ($)") }, singleLine = true); OutlinedTextField(concept, { concept = it }, label = { Text("Concepto") }, minLines = 2); ChoiceField("Estado del recibo", paymentStatus, listOf(ReceiptPaymentStatus.PAID to "Pagado · registrar como abono", ReceiptPaymentStatus.DUE to "Por pagar · no registrar abono"), { paymentStatus = it }); if (amountCents > 0) Text("Se emitirá: ${formatDual(amountCents, state.config)}", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }, confirmButton = { Button(onClick = { viewModel.emitReceipt(rental.id, amountCents, concept, paymentStatus) }, enabled = amountCents > 0L && (paymentStatus == ReceiptPaymentStatus.DUE || amountCents <= balance)) { Text("Emitir recibo") } })
}
