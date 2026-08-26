package com.elspot.toldos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.elspot.toldos.data.AppUiState
import com.elspot.toldos.data.ConfigSnapshot
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    var editing by remember(state.config) { mutableStateOf(state.config) }
    var saved by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AppEvent.Notice && event.text == "Configuración guardada") saved = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader("Configuración", "Datos de EL SPOT, tasa Bs y respaldos")
        SettingsBusinessCard(
            config = editing,
            onChange = { editing = it }
        )
        SettingsNotificationCard(editing) { editing = it }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Respaldo de datos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("Exporta tus registros para conservarlos o migrarlos desde la PWA.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("respaldo-el-spot.json") }) { Text("Exportar") }
                    Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/json")) }) { Text("Importar") }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Actualizaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("Busca una nueva versión publicada en el canal oficial de EL SPOT. Tus datos locales se conservan durante la actualización.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = viewModel::checkForUpdates) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                    Text("Buscar actualización", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Permisos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Las notificaciones se usan para avisos de devolución. La ubicación solo se solicita al capturar GPS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (Build.VERSION.SDK_INT >= 33) {
                    Button(onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) "Notificaciones autorizadas" else "Autorizar notificaciones")
                    }
                }
            }
        }
        Button(onClick = { saved = false; viewModel.saveConfig(editing) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (saved) "Configuración guardada" else "Guardar cambios")
        }
        TextButton(onClick = { showReset = true }, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Text("Restablecer todos los datos")
        }
    }

    if (showReset) {
        ConfirmDialog(
            title = "Restablecer sistema",
            message = "Se eliminarán clientes, toldos, alquileres, recibos y registros internos de este dispositivo.",
            confirmLabel = "Restablecer",
            onDismiss = { showReset = false },
            onConfirm = { viewModel.resetAll(); showReset = false },
            danger = true
        )
    }
}

@Composable
private fun SettingsBusinessCard(
    config: ConfigSnapshot,
    onChange: (ConfigSnapshot) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Datos del negocio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(config.businessName, { onChange(config.copy(businessName = it)) }, label = { Text("Nombre del negocio *") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(config.rif, { onChange(config.copy(rif = it)) }, label = { Text("RIF") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(config.phone, { onChange(config.copy(phone = it)) }, label = { Text("Teléfono") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(config.address, { onChange(config.copy(address = it)) }, label = { Text("Dirección") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(config.exchangeRate.takeIf { it > 0 }?.toString() ?: "", { raw -> onChange(config.copy(exchangeRate = raw.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0)) }, label = { Text("Tasa Bs por 1 $") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.elspot.toldos.R.drawable.marca_elspot),
                    contentDescription = "Logo oficial de EL SPOT",
                    modifier = Modifier
                        .size(76.dp)
                        .background(androidx.compose.ui.graphics.Color.Black, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .padding(5.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Logo oficial del sistema", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("Se usa en toda la aplicación y en el header oscuro de los recibos PDF.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Los montos se guardan en dólares y muestran el equivalente en Bs con esta tasa.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsNotificationCard(config: ConfigSnapshot, onChange: (ConfigSnapshot) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Recordatorios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(config.notificationsEnabled, { onChange(config.copy(notificationsEnabled = it)) })
                Text("Avisarme sobre devoluciones")
            }
            Text("Anticipación: ${config.reminderMinutes} minutos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = config.reminderMinutes.toFloat(),
                onValueChange = { onChange(config.copy(reminderMinutes = it.roundToInt().coerceIn(5, 360))) },
                valueRange = 5f..360f,
                steps = 70
            )
        }
    }
}

