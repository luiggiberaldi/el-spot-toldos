package com.elspot.toldos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.elspot.toldos.data.ReciboEntity
import com.elspot.toldos.data.ReceiptPaymentStatus
import com.elspot.toldos.data.ReceiptSnapshot
import com.elspot.toldos.domain.centsToDollarText
import com.elspot.toldos.domain.centsToBolivarText
import com.elspot.toldos.domain.formatDateTime
import com.elspot.toldos.domain.formatDual

@Composable
fun ReceiptsScreen(state: AppUiState, viewModel: AppViewModel) {
    var selected by remember { mutableStateOf<ReciboEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            "Recibos",
            "${state.receipts.size} emitido${if (state.receipts.size == 1) "" else "s"}"
        )
        if (state.receipts.isEmpty()) {
            EmptyState(
                "Todavía no hay recibos",
                "Emite un recibo desde el detalle de un alquiler.",
                Icons.Default.Description
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.receipts, key = { it.id }) { receipt ->
                    ReceiptRow(
                        receipt = receipt,
                        currentConfig = state.config,
                        onOpen = { selected = receipt },
                        onSharePdf = { viewModel.shareReceipt(receipt) },
                        onShareWhatsApp = { viewModel.shareReceiptWhatsApp(receipt) },
                        onShareText = { viewModel.shareReceipt(receipt, textOnly = true) }
                    )
                }
            }
        }
    }

    selected?.let { receipt ->
        ReceiptDetailDialog(
            receipt = receipt,
            viewModel = viewModel,
            onDismiss = { selected = null },
            onSharePdf = { viewModel.shareReceipt(receipt) },
            onShareWhatsApp = { viewModel.shareReceiptWhatsApp(receipt) },
            onShareText = { viewModel.shareReceipt(receipt, textOnly = true) }
        )
    }
}

@Composable
private fun ReceiptRow(
    receipt: ReciboEntity,
    currentConfig: com.elspot.toldos.data.ConfigSnapshot,
    onOpen: () -> Unit,
    onSharePdf: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareText: () -> Unit
) {
    val snapshot = remember(receipt.id, receipt.snapshotJson) {
        ReceiptSnapshot.fromJson(receipt.snapshotJson)
    }
    val config = snapshot?.let {
        currentConfig.copy(exchangeRate = it.exchangeRate)
    } ?: currentConfig
    val clientName = snapshot?.clientName ?: "Cliente"
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${receipt.folio} · $clientName", fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatDateTime(receipt.emitidoEn)} · ${receipt.concepto}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val status = ReceiptPaymentStatus.from(receipt.estadoPago)
                Text(formatDual(receipt.montoCents, config), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(status.label, color = if (status == ReceiptPaymentStatus.PAID) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = onShareWhatsApp) {
                    Icon(Icons.Default.Chat, contentDescription = "Enviar por WhatsApp", tint = MaterialTheme.colorScheme.primary)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ver recibo") },
                            onClick = { menuOpen = false; onOpen() },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Compartir PDF") },
                            onClick = { menuOpen = false; onSharePdf() },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Compartir texto") },
                            onClick = { menuOpen = false; onShareText() },
                            leadingIcon = { Icon(Icons.Default.IosShare, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptDetailDialog(
    receipt: ReciboEntity,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareText: () -> Unit
) {
    var snapshot by remember(receipt.id) { mutableStateOf<ReceiptSnapshot?>(null) }
    LaunchedEffect(receipt.id, receipt.snapshotJson) {
        snapshot = viewModel.snapshotFor(receipt)
    }
    val data = snapshot
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recibo ${receipt.folio}") },
        text = {
            if (data == null) {
                Text("Cargando datos del recibo…")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(data.businessName, fontWeight = FontWeight.Bold)
                    Text("Cliente: ${data.clientName}")
                    Text("Alquiler: ${data.rentalFolio}")
                    Text("Modalidad: ${data.mode.label}")
                    Text("Inicio: ${formatDateTime(data.startAt)}")
                    Text("Devolución: ${formatDateTime(data.returnAt)}", color = MaterialTheme.colorScheme.secondary)
                    if (data.eventAddress.isNotBlank()) Text("Dirección: ${data.eventAddress}")
                    Text("Estado: ${data.paymentStatus.label}", color = if (data.paymentStatus == ReceiptPaymentStatus.PAID) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                    Text("Monto: ${centsToDollarText(data.amountCents)}", fontWeight = FontWeight.Bold)
                    val bs = centsToBolivarText(data.amountCents, data.exchangeRate)
                    if (bs.isNotBlank()) Text("Equivalente: $bs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Concepto: ${data.concept}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onShareWhatsApp) { Icon(Icons.Default.Chat, contentDescription = null); Text("WhatsApp") }
                TextButton(onClick = onShareText) { Text("Mensaje") }
                TextButton(onClick = onSharePdf) { Text("PDF") }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Cerrar") } }
    )
}
