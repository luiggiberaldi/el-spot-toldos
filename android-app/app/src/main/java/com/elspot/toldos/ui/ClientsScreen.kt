package com.elspot.toldos.ui

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elspot.toldos.data.AppUiState
import com.elspot.toldos.data.ClienteEntity

@Composable
fun ClientsScreen(state: AppUiState, viewModel: AppViewModel) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ClienteEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ClienteEntity?>(null) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AppEvent.Notice && event.text == "Cliente guardado") {
                showForm = false
            }
        }
    }

    val filtered = state.clients.filter {
        "${it.nombre} ${it.cedula} ${it.telefono}".contains(query, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            "Clientes",
            "${state.clients.size} registrado${if (state.clients.size == 1) "" else "s"}",
            action = {
                FloatingActionButton(onClick = { editing = null; showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo cliente")
                }
            }
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por nombre, documento o teléfono") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        if (filtered.isEmpty()) {
            EmptyState(
                title = if (state.clients.isEmpty()) "Aún no hay clientes" else "Sin resultados",
                description = if (state.clients.isEmpty()) "Registra el primer cliente para crear alquileres." else "Prueba con otro término de búsqueda.",
                icon = Icons.Default.People,
                action = if (state.clients.isEmpty()) ({
                    Button(onClick = { editing = null; showForm = true }) { Text("Registrar cliente") }
                }) else null
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { client ->
                    ClientRow(
                        client = client,
                        onEdit = { editing = client; showForm = true },
                        onDelete = { deleting = client }
                    )
                }
            }
        }
    }

    if (showForm) {
        ClientFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { values ->
                viewModel.saveClient(editing, values.name, values.document, values.phone, values.address, values.notes)
            }
        )
    }
    deleting?.let { client ->
        ConfirmDialog(
            title = "Eliminar cliente",
            message = "Se eliminará ${client.nombre}. Los alquileres históricos se conservarán.",
            confirmLabel = "Eliminar",
            onDismiss = { deleting = null },
            onConfirm = { viewModel.deleteClient(client.id); deleting = null },
            danger = true
        )
    }
}

@Composable
private fun ClientRow(client: ClienteEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(client.nombre, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                val secondary = listOfNotNull(
                    client.cedula.takeIf { it.isNotBlank() },
                    client.telefono.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (secondary.isNotBlank()) Text(secondary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (client.direccion.isNotBlank()) Text(client.direccion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar cliente") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar cliente", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

data class ClientFormValues(
    val name: String,
    val document: String,
    val phone: String,
    val address: String,
    val notes: String
)

@Composable
private fun ClientFormDialog(initial: ClienteEntity?, onDismiss: () -> Unit, onSave: (ClientFormValues) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.nombre ?: "") }
    var document by remember(initial) { mutableStateOf(initial?.cedula ?: "") }
    var phone by remember(initial) { mutableStateOf(initial?.telefono ?: "") }
    var address by remember(initial) { mutableStateOf(initial?.direccion ?: "") }
    var notes by remember(initial) { mutableStateOf(initial?.notas ?: "") }
    var error by remember(initial) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo cliente" else "Editar cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true)
                OutlinedTextField(document, { document = it }, label = { Text("Cédula / RIF") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Dirección") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, minLines = 2)
                ErrorMessage(error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = {
            Button(onClick = {
                if (name.trim().isBlank()) error = "El nombre es obligatorio."
                else onSave(ClientFormValues(name, document, phone, address, notes))
            }) { Text("Guardar") }
        }
    )
}
