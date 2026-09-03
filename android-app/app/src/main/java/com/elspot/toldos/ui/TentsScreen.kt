package com.elspot.toldos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elspot.toldos.data.AppUiState
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.data.ToldoEntity
import com.elspot.toldos.domain.centsToBolivarText
import com.elspot.toldos.domain.centsToDollarText

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
        TentFormDialog(
            initial = editing,
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
                    Text(tent.nombre, fontWeight = FontWeight.SemiBold)
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
    onDismiss: () -> Unit,
    onSave: (TentFormValues) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.nombre ?: "") }
    var size by remember(initial) { mutableStateOf(initial?.tamano ?: "") }
    var tariff by remember(initial) { mutableStateOf(initial?.let { "%.2f".format(it.tarifaCents / 100.0) } ?: "") }
    var tariff12h by remember(initial) { mutableStateOf(initial?.let { "%.2f".format((it.tarifa12hCents ?: it.tarifaCents / 2) / 100.0) } ?: "") }
    var units by remember(initial) { mutableStateOf(initial?.unidades?.toString() ?: "1") }
    var status by remember(initial) { mutableStateOf(TentStatus.from(initial?.estado)) }
    var notes by remember(initial) { mutableStateOf(initial?.notas ?: "") }
    var error by remember(initial) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo toldo" else "Editar toldo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del toldo *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("Tamaño (ej. 3x3m, 6x3m)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tariff,
                        onValueChange = { tariff = it },
                        label = { Text("Precio 24h ($) *", maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tariff12h,
                        onValueChange = { tariff12h = it },
                        label = { Text("Precio 12h ($) *", maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = it },
                        label = { Text("Unidades *", maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceField(
                        label = "Estado",
                        selected = status,
                        options = TentStatus.entries.map { it to it.label },
                        onSelected = { status = it },
                        modifier = Modifier.weight(1.1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                ErrorMessage(error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = {
            Button(onClick = {
                when {
                    name.trim().isBlank() -> error = "El nombre es obligatorio."
                    com.elspot.toldos.domain.parseDollarCents(tariff) == null || com.elspot.toldos.domain.parseDollarCents(tariff12h) == null -> error = "Indica precios válidos para 12 y 24 horas."
                    units.toIntOrNull()?.let { it < 1 } != false -> error = "Las unidades deben ser un número mayor que 0."
                    else -> onSave(TentFormValues(name, size, tariff, tariff12h, units, status, notes))
                }
            }) { Text("Guardar") }
        }
    )
}
