package com.elspot.toldos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elspot.toldos.data.AlquilerEntity
import com.elspot.toldos.data.AppUiState
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.domain.formatDateTime
import com.elspot.toldos.domain.formatDual
import java.util.Calendar

@Composable
fun DashboardScreen(
    state: AppUiState,
    onOpen: (AlquilerEntity) -> Unit,
    onNewRental: () -> Unit = {}
) {
    val now = Calendar.getInstance()
    val income = state.receipts
        .filter {
            val date = Calendar.getInstance().apply { timeInMillis = it.emitidoEn }
            date.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                date.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        }
        .sumOf { it.montoCents }
    val pending = state.rentals
        .filter { it.estado == RentalStatus.ACTIVE.name || it.estado == RentalStatus.DELIVERED.name }
        .sumOf { (it.montoTotalCents - it.abonoCents).coerceAtLeast(0L) }
    val active = state.rentals.count { it.estado == RentalStatus.ACTIVE.name }
    val occupiedUnits = state.rentalItems
        .filter { item -> state.rentals.any { rental -> rental.id == item.alquilerId && (rental.estado == RentalStatus.ACTIVE.name || rental.estado == RentalStatus.DELIVERED.name) } }
        .groupBy { it.toldoId }
        .mapValues { (_, items) -> items.sumOf { it.cantidad } }
    val available = state.tents
        .filter { TentStatus.from(it.estado) != TentStatus.REPAIR && TentStatus.from(it.estado) != TentStatus.RETIRED }
        .sumOf { (it.unidades - (occupiedUnits[it.id] ?: 0)).coerceAtLeast(0) }
    val capacity = state.tents.sumOf { it.unidades }
    val recent = state.rentals.take(5)
    val upcoming = state.rentals
        .filter { it.estado == RentalStatus.ACTIVE.name && it.devolucion > System.currentTimeMillis() }
        .sortedBy { it.devolucion }
        .take(3)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionHeader(
                "Resumen operativo",
                "Control de alquileres y cobros de EL SPOT",
                action = {
                    Button(onClick = onNewRental) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Nuevo alquiler")
                    }
                }
            )
        }
        item {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    "Alquileres activos", active.toString(), icon = Icons.Default.Assignment,
                    tint = Color(0xFF8BCEFF), modifier = Modifier.weight(1f)
                )
                MetricCard(
                    "Unidades disponibles", "${available}/${capacity}", icon = Icons.Default.Inventory2,
                    tint = Color(0xFF55D6E8), modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    "Ingresos del mes", formatDual(income, state.config), icon = Icons.Default.AttachMoney,
                    tint = Color(0xFF55D6E8), modifier = Modifier.weight(1f)
                )
                MetricCard(
                    "Pendiente de cobro", formatDual(pending, state.config), icon = Icons.Default.Schedule,
                    tint = Color(0xFFFFC247), modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Text("Alquileres recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (recent.isEmpty()) {
            item {
                EmptyState(
                    "Todavía no hay alquileres",
                    "Cuando registres el primero aparecerá aquí.",
                    Icons.Default.Assignment
                )
            }
        } else {
            items(recent, key = { it.id }) { rental ->
                RecentRentalCard(
                    rental = rental,
                    clientName = state.clients.firstOrNull { it.id == rental.clienteId }?.nombre ?: "Cliente eliminado",
                    config = state.config,
                    onClick = { onOpen(rental) }
                )
            }
        }
        if (upcoming.isNotEmpty()) {
            item {
                Text("Próximas devoluciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(upcoming, key = { "return-${it.id}" }) { rental ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(rental.folio, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Devolución ${formatDateTime(rental.devolucion)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Saldo ${formatDual((rental.montoTotalCents - rental.abonoCents).coerceAtLeast(0L), state.config)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (rental.montoTotalCents > rental.abonoCents) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun RecentRentalCard(
    rental: AlquilerEntity,
    clientName: String,
    config: com.elspot.toldos.data.ConfigSnapshot,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${rental.folio} · $clientName", fontWeight = FontWeight.SemiBold)
                Text(
                    "${if (rental.modalidad == "H12") "12 horas" else "24 horas"} · devolución ${formatDateTime(rental.devolucion)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(formatDual(rental.montoTotalCents, config), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            StatusBadge(RentalStatus.from(rental.estado))
        }
    }
}
