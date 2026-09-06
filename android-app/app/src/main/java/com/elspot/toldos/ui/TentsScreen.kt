package com.elspot.toldos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elspot.toldos.data.AppUiState
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.data.ToldoEntity
import com.elspot.toldos.domain.capitalizeWords
import com.elspot.toldos.domain.centsToBolivarText
import com.elspot.toldos.domain.centsToDollarText
import com.elspot.toldos.domain.parseDollarCents

@Composable
fun TentsScreen(state: AppUiState, viewModel: AppViewModel) {
    var editing by remember { mutableStateOf<ToldoEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ToldoEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AppEvent.Notice && event.text == "Toldo guardado") {
                showForm = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            "Toldos",
            "${state.tents.size} modelos · ${availableCapacity(state)} unidades disponibles",
            action = {
                FloatingActionButton(onClick = { editing = null; showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo toldo")
                }
            }
        )
        if (state.tents.isEmpty()) {
            EmptyState(
                "Inventario vacío",
                "Registra tus toldos con su tarifa base de 24 horas y cantidad de unidades.",
                Icons.Default.Inventory2,
                action = { Button(onClick = { editing = null; showForm = true }) { Text("Registrar toldo") } }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.tents, key = { it.id }) { tent ->
                    TentRow(
                        tent = tent,
                        state = state,
                        onEdit = { editing = tent; showForm = true },
                        onDelete = { deleting = tent }
                    )
                }
            }
        }
    }

    if (showForm) {
        val occupied = editing?.let { occupiedUnits(state, it.id) } ?: 0
        TentFormDialog(
            initial = editing,
            occupiedUnits = occupied,
            exchangeRate = state.config.exchangeRate,
            onDismiss = { showForm = false },
            onSave = { values ->
                viewModel.saveTent(
                    existing = editing,
                    name = values.name,
                    size = values.size,
                    tariff = values.tariff,
                    tariff12h = values.tariff12h,
                    units = values.units,
                    status = values.status,
                    notes = values.notes
                )
            }
        )
    }
    deleting?.let { tent ->
        ConfirmDialog(
            title = "Eliminar toldo",
            message = "Se eliminará ${tent.nombre}. Los alquileres históricos conservarán sus datos.",
            confirmLabel = "Eliminar",
            onDismiss = { deleting = null },
            onConfirm = { viewModel.deleteTent(tent.id); deleting = null },
            danger = true
        )
    }
}

private fun occupiedUnits(state: AppUiState, tentId: String): Int = state.rentalItems
    .filter { it.toldoId == tentId }
    .filter { item ->
        state.rentals.any { rental ->
            rental.id == item.alquilerId &&
                (rental.estado == RentalStatus.ACTIVE.name || rental.estado == RentalStatus.DELIVERED.name)
        }
    }
    .sumOf { it.cantidad }

private fun availableCapacity(state: AppUiState): Int = state.tents
    .filter { TentStatus.from(it.estado) != TentStatus.REPAIR && TentStatus.from(it.estado) != TentStatus.RETIRED }
    .sumOf { tent ->
        (tent.unidades - occupiedUnits(state, tent.id)).coerceAtLeast(0)
    }

@Composable
private fun TentRow(
    tent: ToldoEntity,
    state: AppUiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val occupied = occupiedUnits(state, tent.id)
    val manualUnavailable = TentStatus.from(tent.estado) == TentStatus.REPAIR || TentStatus.from(tent.estado) == TentStatus.RETIRED
    val available = if (manualUnavailable) 0 else (tent.unidades - occupied).coerceAtLeast(0)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(capitalizeWords(tent.nombre), fontWeight = FontWeight.SemiBold)
                    StatusBadge(TentStatus.from(tent.estado))
                }
                if (tent.tamano.isNotBlank()) {
                    Text(tent.tamano, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${centsToDollarText(tent.tarifaCents)} / 24 horas",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$available de ${tent.unidades} unidades disponibles",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (available == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
                Text(
                    "${centsToDollarText(tent.tarifa12hCents ?: tent.tarifaCents / 2)} / 12 horas",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                val bs = centsToBolivarText(tent.tarifaCents, state.config.exchangeRate)
                if (bs.isNotBlank()) {
                    Text(bs, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (tent.notas.isNotBlank()) {
                    Text(tent.notas, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar toldo") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar toldo", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

data class TentFormValues(
    val name: String,
    val size: String,
    val tariff: String,
    val tariff12h: String,
    val units: String,
    val status: TentStatus,
    val notes: String
)

@Composable
private fun TentFormDialog(
    initial: ToldoEntity?,
    occupiedUnits: Int = 0,
    exchangeRate: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (TentFormValues) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.nombre ?: "") }
    var size by remember(initial) { mutableStateOf(initial?.tamano ?: "") }
    var tariff by remember(initial) { mutableStateOf(initial?.let { "%.2f".format(it.tarifaCents / 100.0) } ?: "") }
    var tariff12h by remember(initial) { mutableStateOf(initial?.let { "%.2f".format((it.tarifa12hCents ?: it.tarifaCents / 2) / 100.0) } ?: "") }
    var units by remember(initial) { mutableStateOf(initial?.unidades?.toString() ?: "1") }
    var status by remember(initial, occupiedUnits) {
        mutableStateOf(
            if (occupiedUnits > 0) {
                TentStatus.RENTED
            } else {
                val parsed = TentStatus.from(initial?.estado)
                if (parsed == TentStatus.RENTED) TentStatus.AVAILABLE else parsed
            }
        )
    }
    var notes by remember(initial) { mutableStateOf(initial?.notas ?: "") }
    var error by remember(initial) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (initial == null) Icons.Default.AddBusiness else Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            if (initial == null) "Nuevo toldo" else "Editar toldo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (initial != null) {
                            StatusBadge(status = if (occupiedUnits > 0) TentStatus.RENTED else status)
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (occupiedUnits > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                "Este toldo tiene $occupiedUnits unidad(es) en alquiler activo. El estado debe mantenerse en Alquilado y no puedes reducir el total de unidades a menos de $occupiedUnits.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del toldo *") },
                    placeholder = { Text("Ej. Toldo Gazebo Blanco") },
                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = size,
                        onValueChange = { size = it },
                        label = { Text("Tamaño") },
                        placeholder = { Text("Ej. 3x3m, 6x3m") },
                        leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("3x3m", "4x4m", "6x3m", "6x6m").forEach { preset ->
                            val selected = size.trim().equals(preset, ignoreCase = true)
                            AssistChip(
                                onClick = { size = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                border = if (selected) {
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    AssistChipDefaults.assistChipBorder(true)
                                },
                                colors = if (selected) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    AssistChipDefaults.assistChipColors()
                                }
                            )
                        }
                    }
                }

                val tariff24Cents = parseDollarCents(tariff)
                val tariff12Cents = parseDollarCents(tariff12h)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        OutlinedTextField(
                            value = tariff,
                            onValueChange = { tariff = it.replace(',', '.') },
                            label = { Text("Precio 24h ($) *", maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (tariff24Cents != null && exchangeRate > 0.0) {
                            val bsText = centsToBolivarText(tariff24Cents, exchangeRate)
                            if (bsText.isNotBlank()) {
                                Text(
                                    bsText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        OutlinedTextField(
                            value = tariff12h,
                            onValueChange = { tariff12h = it.replace(',', '.') },
                            label = { Text("Precio 12h ($) *", maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (tariff12Cents != null && exchangeRate > 0.0) {
                            val bsText = centsToBolivarText(tariff12Cents, exchangeRate)
                            if (bsText.isNotBlank()) {
                                Text(
                                    bsText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (tariff24Cents != null && tariff24Cents > 0) {
                    val halfPrice = "%.2f".format((tariff24Cents / 2) / 100.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { tariff12h = halfPrice },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sugerir 50% 12h: \$$halfPrice", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Unidades *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val minAllowed = if (occupiedUnits > 0) occupiedUnits else 1
                            val currentUnits = units.toIntOrNull() ?: minAllowed
                            IconButton(
                                onClick = {
                                    if (currentUnits > minAllowed) {
                                        units = (currentUnits - 1).toString()
                                    }
                                },
                                enabled = currentUnits > minAllowed
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Reducir unidades")
                            }
                            OutlinedTextField(
                                value = units,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.all { it.isDigit() }) {
                                        units = input
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            IconButton(
                                onClick = {
                                    units = (currentUnits + 1).toString()
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Aumentar unidades")
                            }
                        }
                        if (occupiedUnits > 0) {
                            Text(
                                "Mínimo $occupiedUnits (en alquiler)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val statusOptions = if (occupiedUnits > 0) {
                            listOf(TentStatus.RENTED to TentStatus.RENTED.label)
                        } else {
                            listOf(
                                TentStatus.AVAILABLE to TentStatus.AVAILABLE.label,
                                TentStatus.REPAIR to TentStatus.REPAIR.label,
                                TentStatus.RETIRED to TentStatus.RETIRED.label
                            )
                        }
                        ChoiceField(
                            label = "Estado",
                            selected = if (occupiedUnits > 0) TentStatus.RENTED else if (status == TentStatus.RENTED) TentStatus.AVAILABLE else status,
                            options = statusOptions,
                            onSelected = { status = it },
                            supportingText = if (occupiedUnits > 0) "Fijado por alquiler activo" else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    placeholder = { Text("Detalles de mantenimiento, almacenamiento...") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                ErrorMessage(error)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedUnits = units.toIntOrNull()
                    when {
                        name.trim().isBlank() -> error = "El nombre es obligatorio."
                        parseDollarCents(tariff) == null || parseDollarCents(tariff12h) == null -> error = "Indica precios válidos para 12 y 24 horas."
                        parsedUnits == null || parsedUnits < 1 -> error = "Las unidades deben ser un número mayor que 0."
                        occupiedUnits > 0 && parsedUnits < occupiedUnits -> error = "No puedes tener menos de $occupiedUnits unidades porque están actualmente alquiladas."
                        else -> {
                            val finalStatus = if (occupiedUnits > 0) TentStatus.RENTED else if (status == TentStatus.RENTED) TentStatus.AVAILABLE else status
                            onSave(
                                TentFormValues(
                                    name = capitalizeWords(name),
                                    size = size.trim(),
                                    tariff = tariff,
                                    tariff12h = tariff12h,
                                    units = units,
                                    status = finalStatus,
                                    notes = notes.trim()
                                )
                            )
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (initial == null) "Guardar toldo" else "Guardar cambios")
            }
        }
    )
}
