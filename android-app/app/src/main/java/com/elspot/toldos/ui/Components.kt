package com.elspot.toldos.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elspot.toldos.data.RentalStatus
import com.elspot.toldos.data.TentStatus
import com.elspot.toldos.domain.formatDateTime
import com.elspot.toldos.domain.formatDual
import java.util.Calendar

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = com.elspot.toldos.R.drawable.marca_elspot),
        contentDescription = "Logo oficial EL SPOT",
        modifier = modifier
            .size(width = 118.dp, height = 54.dp)
    )
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    secondary: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!secondary.isNullOrBlank()) {
                Text(secondary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(3.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Info,
    action: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            action?.invoke()
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
    )
}

@Composable
fun <T> ChoiceField(
    label: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    emptyMessage: String = "Sin opciones disponibles",
    emptyHint: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val isEmpty = options.isEmpty()
    val selectedText = options.firstOrNull { it.first == selected }?.second

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            contentPadding = ButtonDefaults.ContentPadding,
            border = if (isEmpty && selectedText == null) {
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                )
            } else ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = selectedText ?: if (isEmpty) emptyMessage else "Selecciona",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selectedText != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else if (isEmpty) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                if (isEmpty && selectedText == null) Icons.Default.Info else Icons.Default.ExpandMore,
                contentDescription = if (isEmpty) "Sin opciones disponibles" else "Abrir opciones",
                tint = if (isEmpty && selectedText == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(12.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
        ) {
            if (isEmpty) {
                DropdownMenuItem(
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                emptyMessage,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (!emptyHint.isNullOrBlank()) {
                                Text(
                                    emptyHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Text(
                                "Toca para cerrar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        leadingIcon = if (value == selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    val displaySupportingText = supportingText ?: if (isEmpty && selectedText == null) emptyHint else null
    if (!displaySupportingText.isNullOrBlank()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 4.dp, top = 3.dp)
        ) {
            if (isEmpty && selectedText == null) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                displaySupportingText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isEmpty && selectedText == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MoneySummary(
    totalCents: Long,
    depositCents: Long,
    config: com.elspot.toldos.data.ConfigSnapshot,
    modifier: Modifier = Modifier
) {
    val balance = (totalCents - depositCents).coerceAtLeast(0L)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
    ) {
        Column {
            Text(
                "Resumen del alquiler",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)
            )
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                MoneyRow("Total", formatDual(totalCents, config), emphasized = true)
                MoneyRow("Abono", formatDual(depositCents, config))
            }
            Divider(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pendiente", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    formatDual(balance, config),
                    fontWeight = FontWeight.Bold,
                    color = if (balance > 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MoneyRow(label: String, value: String, emphasized: Boolean = false, danger: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusBadge(status: RentalStatus) {
    val (background, foreground) = when (status) {
        RentalStatus.ACTIVE -> Color(0xFF123653) to Color(0xFF8BCEFF)
        RentalStatus.DELIVERED -> Color(0xFF3D3219) to Color(0xFFFFD479)
        RentalStatus.RETURNED -> Color(0xFF0B3D46) to Color(0xFFB7F3FA)
        RentalStatus.CANCELLED -> Color(0xFF293241) to Color(0xFFB8C5D4)
    }
    StatusBadgeText(status.label, background, foreground)
}

@Composable
fun StatusBadge(status: TentStatus) {
    val (background, foreground) = when (status) {
        TentStatus.AVAILABLE -> Color(0xFF0B3D46) to Color(0xFFB7F3FA)
        TentStatus.RENTED -> Color(0xFF123653) to Color(0xFF8BCEFF)
        TentStatus.REPAIR -> Color(0xFF3D3219) to Color(0xFFFFD479)
        TentStatus.RETIRED -> Color(0xFF293241) to Color(0xFFB8C5D4)
    }
    StatusBadgeText(status.label, background, foreground)
}

@Composable
private fun StatusBadgeText(text: String, background: Color, foreground: Color) {
    Surface(color = background, shape = RoundedCornerShape(50)) {
        Text(
            text,
            color = foreground,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun DateTimeField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val calendar = Calendar.getInstance().apply { timeInMillis = value }
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val updated = Calendar.getInstance().apply {
                        timeInMillis = value
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                    }
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            updated.set(Calendar.HOUR_OF_DAY, hour)
                            updated.set(Calendar.MINUTE, minute)
                            onValueChange(updated.timeInMillis)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDateTime(value), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun GpsSummary(latitude: Double?, longitude: Double?) {
    if (latitude == null || longitude == null) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(17.dp))
        Text(
            "${"%.6f".format(latitude)}, ${"%.6f".format(longitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WarningText(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(17.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    danger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (danger) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
            ) { Text(confirmLabel) }
        }
    )
}

@Composable
fun ErrorMessage(text: String?) {
    if (!text.isNullOrBlank()) {
        Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
