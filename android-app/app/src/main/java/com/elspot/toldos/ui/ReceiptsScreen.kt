package com.elspot.toldos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
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
import com.elspot.toldos.domain.capitalizeWords
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
    val clientName = capitalizeWords(snapshot?.clientName ?: "Cliente")
    var menuOpen by remember { mutableStateOf(false) }

    val status = ReceiptPaymentStatus.resolve(
        status = receipt.estadoPago,
        concept = receipt.concepto,
        rentalTotalCents = snapshot?.rentalTotalCents ?: 0L,
        rentalDepositCents = snapshot?.rentalDepositCents ?: 0L
    )
    val isPaid = status == ReceiptPaymentStatus.PAID

    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${receipt.folio} · $clientName", fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatDateTime(receipt.emitidoEn)} · ${receipt.concepto}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatDual(receipt.montoCents, config),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPaid) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            if (isPaid) "PAGADO" else "POR PAGAR",
                            color = if (isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
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
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Recibo ${receipt.folio}", fontWeight = FontWeight.Bold)
                    if (data != null) {
                        Text(
                            formatDateTime(data.emittedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            if (data == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isPaid = data.isActuallyPaid

                    // 1. Banner de Estado
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPaid) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (isPaid) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                if (isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    if (isPaid) "PAGADO · DINERO RECIBIDO" else "POR PAGAR · CUENTA DE COBRO",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    if (isPaid) "Comprobante de pago verificado y solvente." else "Documento pendiente de cobro emitido al cliente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. Monto del Recibo
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "MONTO DEL RECIBO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    centsToDollarText(data.amountCents),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val bs = centsToBolivarText(data.amountCents, data.exchangeRate)
                                if (bs.isNotBlank()) {
                                    Text(
                                        bs,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            }
                            if (data.exchangeRate > 0.0) {
                                Text(
                                    "Tasa BCV: Bs. ${String.format(java.util.Locale.US, "%.2f", data.exchangeRate)} / $",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    data.concept,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 3. Información del Servicio
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "INFORMACIÓN DEL SERVICIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            ReceiptDetailField(
                                icon = Icons.Default.Person,
                                label = "Cliente",
                                value = data.clientName + if (data.clientDocument.isNotBlank()) " · ${data.clientDocument}" else ""
                            )
                            if (data.clientPhone.isNotBlank()) {
                                ReceiptDetailField(
                                    icon = Icons.Default.Phone,
                                    label = "Teléfono",
                                    value = data.clientPhone
                                )
                            }
                            ReceiptDetailField(
                                icon = Icons.Default.Event,
                                label = "Alquiler",
                                value = "${data.rentalFolio} · ${data.mode.label}"
                            )
                            if (data.startAt > 0L) {
                                ReceiptDetailField(
                                    icon = Icons.Default.Schedule,
                                    label = "Fecha de entrega",
                                    value = formatDateTime(data.startAt)
                                )
                            }
                            val returnTime = if (data.returnAt > 0L) data.returnAt else if (data.startAt > 0L) data.startAt + data.mode.hours * 3600000L else 0L
                            if (returnTime > 0L) {
                                ReceiptDetailField(
                                    icon = Icons.Default.Schedule,
                                    label = "Fecha de devolución",
                                    value = formatDateTime(returnTime)
                                )
                            }
                            if (data.eventAddress.isNotBlank()) {
                                ReceiptDetailField(
                                    icon = Icons.Default.Place,
                                    label = "Dirección",
                                    value = data.eventAddress
                                )
                            }
                            if (data.eventReference.isNotBlank()) {
                                ReceiptDetailField(
                                    icon = Icons.Default.NearMe,
                                    label = "Referencia",
                                    value = data.eventReference
                                )
                            }
                        }
                    }

                    // 4. Estado de Cuenta del Alquiler
                    val balance = (data.rentalTotalCents - data.rentalDepositCents).coerceAtLeast(0L)
                    val settledRental = data.rentalTotalCents > 0L && balance == 0L
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "ESTADO DE CUENTA DEL ALQUILER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total del alquiler:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(centsToDollarText(data.rentalTotalCents), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Abonado registrado:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(centsToDollarText(data.rentalDepositCents), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Saldo pendiente:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                if (settledRental) {
                                    Text("✓ Alquiler saldado", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                } else {
                                    Text(centsToDollarText(balance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onShareWhatsApp) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp")
                }
                OutlinedButton(onClick = onSharePdf) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
                TextButton(onClick = onShareText) {
                    Text("Mensaje")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun ReceiptDetailField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp).padding(top = 2.dp)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}
