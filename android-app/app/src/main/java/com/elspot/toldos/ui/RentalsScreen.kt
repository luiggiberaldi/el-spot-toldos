package com.elspot.toldos.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.elspot.toldos.domain.capitalizeWords
import com.elspot.toldos.domain.centsToDollarText
import com.elspot.toldos.domain.formatDateTime
import com.elspot.toldos.domain.formatDual
import com.elspot.toldos.domain.parseDollarCents
import com.elspot.toldos.location.LocationService
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
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
                Text("${rental.folio} · ${capitalizeWords(client?.nombre ?: "Cliente eliminado")}", fontWeight = FontWeight.SemiBold)
                Text("${if (RentalMode.from(rental.modalidad) == RentalMode.H12) "12 horas" else "24 horas"} · devuelve ${formatDateTime(rental.devolucion)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total ${formatDual(rental.montoTotalCents, state.config)} · Pendiente ${formatDual(balance, state.config)}", style = MaterialTheme.typography.bodySmall, color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                if (rental.direccion.isNotBlank()) Text(rental.direccion, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (rental.latitud != null && rental.longitud != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    AssistChip(
                        onClick = { com.elspot.toldos.location.openGoogleMaps(context, rental.latitud, rental.longitud, "Alquiler ${rental.folio}") },
                        label = { Text("Ver en Google Maps", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
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
private fun FormSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badge: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!badge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            content()
        }
    }
}

private fun saveImageToInternalStorage(context: android.content.Context, sourceUri: android.net.Uri): String? {
    return try {
        val photosDir = java.io.File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val destFile = java.io.File(photosDir, "delivery_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

private fun createCameraTempFile(context: android.content.Context): Pair<java.io.File, android.net.Uri> {
    val cachePhotosDir = java.io.File(context.cacheDir, "photos")
    if (!cachePhotosDir.exists()) cachePhotosDir.mkdirs()
    val tempFile = java.io.File.createTempFile("cam_", ".jpg", cachePhotosDir)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
    return tempFile to uri
}

@Composable
private fun QuickClientDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, document: String, phone: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var document by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar nuevo cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre y apellido *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = document,
                    onValueChange = { document = it },
                    label = { Text("Cédula / RIF (opcional)") },
                    placeholder = { Text("V-12345678") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono / WhatsApp (opcional)") },
                    placeholder = { Text("0412-1234567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección habitual (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        error = "El nombre es obligatorio."
                    } else {
                        onSave(name.trim(), document.trim(), phone.trim(), address.trim())
                    }
                }
            ) {
                Text("Guardar cliente")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var locationReference by remember(initial) { mutableStateOf(initial?.referenciaUbicacion ?: "") }
    var latitude by remember(initial) { mutableStateOf(initial?.latitud) }
    var longitude by remember(initial) { mutableStateOf(initial?.longitud) }
    var freight by remember(initial) { mutableStateOf(initial?.let { if (it.fleteCents > 0L) "%.2f".format(it.fleteCents / 100.0) else "" } ?: "") }
    var deliveryPhotoUri by remember(initial) { mutableStateOf(initial?.fotoEntregaUri.orEmpty()) }
    var reminderActive by remember(initial) { mutableStateOf(true) }
    var reminderMinutes by remember(initial) { mutableStateOf(120) }
    var showQuickClientDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
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
            scope.launch {
                capturing = true
                locationError = null
                try {
                    val result = location.current()
                    latitude = result.latitude
                    longitude = result.longitude
                } catch (t: Throwable) {
                    locationError = t.message ?: "No se pudo obtener la ubicación."
                } finally {
                    capturing = false
                }
            }
        } else {
            locationError = "Permiso de ubicación denegado. Actívalo en los ajustes del dispositivo."
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            val savedPath = saveImageToInternalStorage(context, tempCameraUri!!)
            if (savedPath != null) {
                deliveryPhotoUri = savedPath
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val savedPath = saveImageToInternalStorage(context, uri)
            if (savedPath != null) {
                deliveryPhotoUri = savedPath
            }
        }
    }
    val parsedFreight = if (freight.isBlank()) 0L else parseDollarCents(freight)
    val freightCents = parsedFreight ?: 0L
    val totalCents = remember(lines.toList(), mode, freightCents) {
        val itemsTotal = lines.sumOf { (parseDollarCents(it.tariff) ?: 0L) * (it.quantity.toIntOrNull()?.coerceAtLeast(0) ?: 0) }
        itemsTotal + freightCents
    }
    val parsedDeposit = if (deposit.isBlank()) 0L else parseDollarCents(deposit)
    val depositCents = parsedDeposit ?: 0L
    val returnAt = calculateReturnAt(startAt, mode.hours)
    val pendingBalance = (totalCents - depositCents).coerceAtLeast(0L)

    val onSaveAction = {
        val valid = lines.mapNotNull { line ->
            val tent = state.tents.firstOrNull { it.id == line.tentId }
            val qty = line.quantity.toIntOrNull()
            val tariff = parseDollarCents(line.tariff)
            if (tent != null && qty != null && qty > 0 && tariff != null && tariff >= 0) RentalItemDraft(tent.id, qty, tariff) else null
        }
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
            parsedFreight == null -> error = "Indica un flete válido."
            parsedDeposit == null -> error = "Indica un abono válido."
            depositCents > totalCents -> error = "El abono no puede superar el total."
            else -> {
                error = null
                onSave(
                    RentalDraft(
                        id = initial?.id,
                        clientId = clientId,
                        items = valid,
                        mode = mode,
                        startAt = startAt,
                        address = address,
                        locationReference = locationReference,
                        latitude = latitude,
                        longitude = longitude,
                        fleteCents = freightCents,
                        fotoEntregaUri = deliveryPhotoUri,
                        reminderActive = reminderActive,
                        reminderMinutes = reminderMinutes,
                        totalCents = totalCents,
                        depositCents = depositCents,
                        status = status,
                        notes = notes
                    )
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (initial == null) "Nuevo alquiler" else "Editar ${initial.folio}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (initial == null) "Registra el servicio y la reserva" else "Modifica los datos del servicio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar modal")
                            }
                        },
                        actions = {
                            Button(
                                onClick = onSaveAction,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Guardar", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        formatDual(totalCents, state.config),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        if (pendingBalance > 0L) "Saldo pendiente" else "Completamente pagado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (pendingBalance > 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        formatDual(pendingBalance, state.config),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pendingBalance > 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Button(
                                onClick = onSaveAction,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (initial == null) "Guardar alquiler" else "Actualizar alquiler",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (error != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = error!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // 1. Cliente
                    item {
                        FormSectionCard(
                            title = "Cliente",
                            icon = Icons.Default.People
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ChoiceField(
                                        label = "Cliente",
                                        selected = clientId,
                                        options = state.clients.map { it.id to capitalizeWords(it.nombre) },
                                        onSelected = { clientId = it },
                                        emptyMessage = "Sin clientes registrados",
                                        emptyHint = "Primero agrega clientes o usa '+ Nuevo'."
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showQuickClientDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Nuevo")
                                }
                            }
                            if (showQuickClientDialog) {
                                QuickClientDialog(
                                    onDismiss = { showQuickClientDialog = false },
                                    onSave = { name, doc, phone, addr ->
                                        viewModel.saveClient(null, name, doc, phone, addr, "") { newClient ->
                                            clientId = newClient.id
                                            showQuickClientDialog = false
                                        }
                                    }
                                )
                            }
                            val selectedClient = state.clients.firstOrNull { it.id == clientId }
                            if (selectedClient != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (selectedClient.telefono.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Phone,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    selectedClient.telefono,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (selectedClient.direccion.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    selectedClient.direccion,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Toldos
                    item {
                        FormSectionCard(
                            title = "Toldos y equipamiento",
                            icon = Icons.Default.Inventory2,
                            badge = "${lines.size} toldo${if (lines.size > 1) "s" else ""}"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                lines.forEachIndexed { index, line ->
                                    RentalLineEditor(
                                        index = index,
                                        line = line,
                                        state = state,
                                        mode = mode,
                                        currentRentalId = initial?.id,
                                        allowInventory = status == RentalStatus.ACTIVE || status == RentalStatus.DELIVERED,
                                        canRemove = lines.size > 1,
                                        onChange = { lines[index] = it },
                                        onRemove = { if (lines.size > 1) lines.removeAt(index) }
                                    )
                                }
                                OutlinedButton(
                                    onClick = { lines.add(RentalLineState()) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Agregar otro toldo")
                                }
                            }
                        }
                    }

                    // 3. Duración y tiempos
                    item {
                        FormSectionCard(
                            title = "Duración y tiempos",
                            icon = Icons.Default.CalendarMonth,
                            badge = mode.label
                        ) {
                            ChoiceField(
                                label = "Modalidad de alquiler",
                                selected = mode,
                                options = RentalMode.entries.map { it to "${it.label} · ${if (it == RentalMode.H12) "Tarifa 12h" else "Tarifa base 24h"}" },
                                onSelected = { selected ->
                                    mode = selected
                                    lines.replaceAll { line ->
                                        val tent = state.tents.firstOrNull { it.id == line.tentId }
                                        val price = if (selected == RentalMode.H12) tent?.tarifa12hCents ?: tent?.tarifaCents?.div(2) else tent?.tarifaCents
                                        line.copy(tariff = price?.let { "%.2f".format(it / 100.0) } ?: line.tariff)
                                    }
                                }
                            )

                            DateTimeField("Inicio del alquiler", startAt, { startAt = it })

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            "Fecha y hora acordada de entrega/devolución:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            formatDateTime(returnAt),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            "Calculado automáticamente (+${mode.hours} horas según modalidad)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Recordatorio de devolución",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Alerta automática previa al vencimiento",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = reminderActive,
                                    onCheckedChange = { reminderActive = it }
                                )
                            }

                            if (reminderActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(60 to "1h antes", 120 to "2h antes", 180 to "3h antes").forEach { (mins, label) ->
                                        AssistChip(
                                            onClick = { reminderMinutes = mins },
                                            label = { Text(label) },
                                            colors = if (reminderMinutes == mins) {
                                                AssistChipDefaults.assistChipColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            } else {
                                                AssistChipDefaults.assistChipColors()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Ubicación de entrega
                    item {
                        FormSectionCard(
                            title = "Ubicación del evento",
                            icon = Icons.Default.LocationOn
                        ) {
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Dirección del evento") },
                                placeholder = { Text("Calle, avenida, sector, número de casa...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (location.hasPermission()) {
                                            scope.launch {
                                                capturing = true
                                                locationError = null
                                                try {
                                                    val result = location.current()
                                                    latitude = result.latitude
                                                    longitude = result.longitude
                                                } catch (t: Throwable) {
                                                    locationError = t.message ?: "No se pudo obtener la ubicación."
                                                } finally {
                                                    capturing = false
                                                }
                                            }
                                        } else {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    enabled = !capturing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(if (capturing) "Capturando GPS…" else "Capturar coordenadas GPS", maxLines = 1)
                                }
                            }

                            if (latitude != null && longitude != null) {
                                GpsSummary(
                                    latitude = latitude,
                                    longitude = longitude,
                                    onClear = { latitude = null; longitude = null }
                                )
                            }

                            ErrorMessage(locationError)

                            OutlinedTextField(
                                value = locationReference,
                                onValueChange = { locationReference = it },
                                label = { Text("Punto de referencia (opcional)") },
                                placeholder = { Text("Ej: frente a la plaza, portón negro, casa de dos plantas") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // 5. Cobro y abono
                    item {
                        FormSectionCard(
                            title = "Cobro y abono",
                            icon = Icons.Default.ReceiptLong
                        ) {
                            OutlinedTextField(
                                value = freight,
                                onValueChange = { freight = it.replace(',', '.') },
                                label = { Text("Flete / Transporte ($) · opcional") },
                                placeholder = { Text("0.00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = deposit,
                                    onValueChange = { deposit = it.replace(',', '.') },
                                    label = { Text("Abono inicial ($)") },
                                    placeholder = { Text("0.00") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                    modifier = Modifier.weight(1f)
                                )
                                ChoiceField(
                                    label = "Estado",
                                    selected = status,
                                    options = RentalStatus.entries.map { it to it.label },
                                    onSelected = { status = it },
                                    modifier = Modifier.weight(1.1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = { deposit = "%.2f".format(totalCents / 100.0) },
                                    label = { Text("Total ($${"%.2f".format(totalCents / 100.0)})") },
                                    enabled = totalCents > 0L
                                )
                                AssistChip(
                                    onClick = { deposit = "%.2f".format(totalCents / 200.0) },
                                    label = { Text("50% ($${"%.2f".format(totalCents / 200.0)})") },
                                    enabled = totalCents > 0L
                                )
                                AssistChip(
                                    onClick = { deposit = "" },
                                    label = { Text("Limpiar") },
                                    enabled = deposit.isNotBlank()
                                )
                            }

                            MoneySummary(totalCents, depositCents, state.config)
                        }
                    }

                    // 6. Observaciones
                    item {
                        FormSectionCard(
                            title = "Observaciones",
                            icon = Icons.Default.Edit
                        ) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Notas o instrucciones de entrega") },
                                placeholder = { Text("Ej: el cliente retirará o requiere apoyo para el armado...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }

                    // 7. Comprobante de entrega (foto)
                    item {
                        FormSectionCard(
                            title = "Comprobante de entrega",
                            icon = Icons.Default.CameraAlt,
                            badge = if (deliveryPhotoUri.isNotBlank()) "1 foto adjunta" else null
                        ) {
                            val photoBitmap = remember(deliveryPhotoUri) {
                                if (deliveryPhotoUri.isNotBlank()) {
                                    try {
                                        val file = java.io.File(deliveryPhotoUri)
                                        if (file.exists()) {
                                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                                        } else null
                                    } catch (e: Exception) { null }
                                } else null
                            }

                            if (photoBitmap != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Image(
                                        bitmap = photoBitmap,
                                        contentDescription = "Foto comprobante de entrega",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val (file, uri) = createCameraTempFile(context)
                                                tempCameraUri = uri
                                                cameraLauncher.launch(uri)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Tomar otra", maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { galleryLauncher.launch("image/*") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Galería", maxLines = 1)
                                        }
                                        IconButton(
                                            onClick = { deliveryPhotoUri = "" }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar foto", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Fotografía del estado de entrega e instalación (opcional):",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val (file, uri) = createCameraTempFile(context)
                                                tempCameraUri = uri
                                                cameraLauncher.launch(uri)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Cámara")
                                        }
                                        OutlinedButton(
                                            onClick = { galleryLauncher.launch("image/*") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Galería")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (error != null) {
                        item {
                            ErrorMessage(error)
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

private fun availableUnitsForTent(state: AppUiState, tentId: String, exceptRentalId: String?): Int {
    val tent = state.tents.firstOrNull { it.id == tentId } ?: return 0
    val occupied = state.rentalItems.filter { it.toldoId == tentId && it.alquilerId != exceptRentalId }.filter { item -> state.rentals.any { rental -> rental.id == item.alquilerId && (rental.estado == RentalStatus.ACTIVE.name || rental.estado == RentalStatus.DELIVERED.name) } }.sumOf { it.cantidad }
    return (tent.unidades - occupied).coerceAtLeast(0)
}

@Composable
private fun RentalLineEditor(
    index: Int,
    line: RentalLineState,
    state: AppUiState,
    mode: RentalMode,
    currentRentalId: String?,
    allowInventory: Boolean,
    canRemove: Boolean,
    onChange: (RentalLineState) -> Unit,
    onRemove: () -> Unit
) {
    val options = state.tents
        .filter { TentStatus.from(it.estado) != TentStatus.RETIRED }
        .filter { !allowInventory || TentStatus.from(it.estado) != TentStatus.REPAIR }
        .filter { !allowInventory || it.id == line.tentId || availableUnitsForTent(state, it.id, currentRentalId) > 0 }
        .map { tent ->
            tent.id to "${capitalizeWords(tent.nombre)} (${tent.tamano.ifBlank { "sin tamaño" }} · ${availableUnitsForTent(state, tent.id, currentRentalId)}/${tent.unidades} disp.)"
        }

    val parsedTariff = parseDollarCents(line.tariff) ?: 0L
    val qty = line.quantity.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val lineSubtotalCents = parsedTariff * qty

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Toldo ${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (lineSubtotalCents > 0L) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Subtotal: ${centsToDollarText(lineSubtotalCents)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Quitar toldo ${index + 1}",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            ChoiceField(
                label = "Toldo",
                selected = line.tentId,
                options = options,
                onSelected = { id ->
                    val tent = state.tents.firstOrNull { it.id == id }
                    val price = if (mode == RentalMode.H12) tent?.tarifa12hCents ?: tent?.tarifaCents?.div(2) else tent?.tarifaCents
                    onChange(line.copy(tentId = id, tariff = price?.let { "%.2f".format(it / 100.0) } ?: line.tariff))
                },
                emptyMessage = "Sin toldos disponibles",
                emptyHint = if (state.tents.isEmpty()) "Registra toldos en la sección Toldos." else "No hay toldos con inventario disponible."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = line.quantity,
                    onValueChange = { onChange(line.copy(quantity = it)) },
                    label = { Text("Cantidad", maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = line.tariff,
                    onValueChange = { onChange(line.copy(tariff = it.replace(',', '.'))) },
                    label = {
                        Text(
                            if (mode == RentalMode.H12) "Precio 12h ($)" else "Precio 24h ($)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

@Composable
private fun RentalDetailDialog(rental: AlquilerEntity, state: AppUiState, viewModel: AppViewModel, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onReceipt: () -> Unit) {
    val client = state.clients.firstOrNull { it.id == rental.clienteId }
    var rentalItems by remember(rental.id) { mutableStateOf<List<AlquilerItemEntity>>(emptyList()) }
    var showFullPhoto by remember { mutableStateOf(false) }
    val detailBitmap = remember(rental.fotoEntregaUri) {
        if (rental.fotoEntregaUri.isNotBlank()) {
            try {
                val file = java.io.File(rental.fotoEntregaUri)
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else null
            } catch (e: Exception) { null }
        } else null
    }
    LaunchedEffect(rental.id) { rentalItems = viewModel.rentalItems(rental.id) }
    val balance = (rental.montoTotalCents - rental.abonoCents).coerceAtLeast(0L)
    val mode = RentalMode.from(rental.modalidad)

    if (showFullPhoto && detailBitmap != null) {
        Dialog(onDismissRequest = { showFullPhoto = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Comprobante de entrega", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { showFullPhoto = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                    Image(
                        bitmap = detailBitmap,
                        contentDescription = "Foto de entrega completa",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alquiler ${rental.folio}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(RentalStatus.from(rental.estado))
                        Text("Creado ${formatDateTime(rental.creadoEn)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { Text("Cliente: ${client?.nombre ?: "Cliente eliminado"}", fontWeight = FontWeight.SemiBold) }
                item { Text("Modalidad: ${mode.label}") }
                item { Text("Inicio: ${formatDateTime(rental.inicio)}") }
                item { Text("Devolución: ${formatDateTime(rental.devolucion)}", color = MaterialTheme.colorScheme.secondary) }
                item { Text("Dirección: ${rental.direccion.ifBlank { "—" }}") }
                if (rental.referenciaUbicacion.isNotBlank()) item { Text("Referencia: ${rental.referenciaUbicacion}") }
                item { GpsSummary(rental.latitud, rental.longitud) }
                item { Divider() }
                item { Text("Toldos", fontWeight = FontWeight.SemiBold) }
                items(rentalItems, key = { "${it.alquilerId}-${it.linea}" }) { line ->
                    val tent = state.tents.firstOrNull { it.id == line.toldoId }
                    Text("${line.cantidad} × ${capitalizeWords(tent?.nombre ?: "Toldo eliminado")} — ${centsToDollarText(line.tarifaCents * line.cantidad)}", style = MaterialTheme.typography.bodySmall)
                }
                if (rental.fleteCents > 0L) {
                    item {
                        Text("Flete / Transporte: ${centsToDollarText(rental.fleteCents)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
                if (detailBitmap != null) {
                    item { Divider() }
                    item {
                        Text("Comprobante de entrega (foto)", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showFullPhoto = true }
                        ) {
                            Image(
                                bitmap = detailBitmap,
                                contentDescription = "Foto de entrega",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Ver foto", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                item { MoneySummary(rental.montoTotalCents, rental.abonoCents, state.config) }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Text("Eliminar")
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null)
                    Text("Editar")
                }
            }
        },
        confirmButton = {
            Button(onClick = onReceipt, enabled = rental.montoTotalCents > 0L) {
                Icon(Icons.Default.ReceiptLong, null)
                Spacer(Modifier.size(6.dp))
                Text("Emitir recibo")
            }
        }
    )
}

@Composable
private fun ReceiptFormDialog(rental: AlquilerEntity, state: AppUiState, viewModel: AppViewModel, onDismiss: () -> Unit) {
    val balance = (rental.montoTotalCents - rental.abonoCents).coerceAtLeast(0L)
    val settled = balance <= 0L
    var amount by remember(rental) { mutableStateOf(((if (balance > 0L) balance else rental.abonoCents) / 100.0).toString()) }
    var concept by remember(rental) { mutableStateOf(if (balance > 0L) "Saldo pendiente del alquiler" else "Abono ya recibido del alquiler") }
    var paymentStatus by remember(rental) { mutableStateOf(if (balance > 0L) ReceiptPaymentStatus.PAID else ReceiptPaymentStatus.DUE) }
    val amountCents = if (settled) rental.abonoCents else (parseDollarCents(amount) ?: 0L)
    var createdReceipt by remember { mutableStateOf<com.elspot.toldos.data.ReciboEntity?>(null) }
    LaunchedEffect(Unit) { viewModel.events.collect { event -> if (event is AppEvent.ReceiptCreated && event.receipt.alquilerId == rental.id) createdReceipt = event.receipt } }
    createdReceipt?.let { receipt -> AlertDialog(onDismissRequest = onDismiss, title = { Text("Recibo ${receipt.folio} emitido") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(42.dp)); Text("El recibo fue guardado y está listo para compartir por WhatsApp."); Text("Puedes enviar el PDF con el mensaje profesional o compartir solo el resumen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, dismissButton = { Row { TextButton(onClick = { viewModel.shareReceiptWhatsApp(receipt) }) { Icon(Icons.Default.Chat, null); Text("WhatsApp") }; TextButton(onClick = { viewModel.shareReceipt(receipt, true) }) { Text("Mensaje") }; TextButton(onClick = { viewModel.shareReceipt(receipt) }) { Text("PDF") } } }, confirmButton = { Button(onClick = onDismiss) { Text("Cerrar") } }); return }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emitir recibo · ${rental.folio}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pendiente actual: ${formatDual(balance, state.config)}", color = MaterialTheme.colorScheme.secondary)
                if (settled) {
                    // Alquiler saldado: el recibo documenta el abono recibido; el monto no se edita.
                    Text(
                        "Monto del comprobante: ${formatDual(rental.abonoCents, state.config)} (abono ya recibido)",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.replace(',', '.') },
                        label = { Text("Monto a cancelar ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                    )
                }
                OutlinedTextField(concept, { concept = it }, label = { Text("Concepto") }, minLines = 2)
                // Cuando el alquiler ya está saldado, emitir un comprobante no debe leerse como "Por pagar".
                ChoiceField(
                    "Estado del recibo", paymentStatus,
                    listOf(
                        ReceiptPaymentStatus.PAID to "Pagado · registrar como abono",
                        ReceiptPaymentStatus.DUE to if (balance <= 0L) "Comprobante · alquiler ya saldado" else "Por pagar · no registrar abono"
                    ),
                    { paymentStatus = it }
                )
                if (amountCents > 0) Text("Se emitirá: ${formatDual(amountCents, state.config)}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = { Button(onClick = { viewModel.emitReceipt(rental.id, amountCents, concept, paymentStatus) }, enabled = amountCents > 0L && (paymentStatus == ReceiptPaymentStatus.DUE || amountCents <= balance)) { Text("Emitir recibo") } }
    )
}

