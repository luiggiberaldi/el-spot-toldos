package com.elspot.toldos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elspot.toldos.data.AppUiState
import kotlinx.coroutines.launch

private enum class AppTab(
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("Panel", Icons.Default.Dashboard),
    CLIENTS("Clientes", Icons.Default.People),
    TENTS("Toldos", Icons.Default.Inventory2),
    RENTALS("Alquileres", Icons.Default.Assignment),
    RECEIPTS("Recibos", Icons.Default.ReceiptLong),
    SETTINGS("Configuración", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElSpotApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var requestedRentalId by remember { mutableStateOf<String?>(null) }
    var requestNewRental by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.Notice -> snackbar.showSnackbar(event.text)
                is AppEvent.Error -> snackbar.showSnackbar(event.text)
                is AppEvent.BackupImported -> snackbar.showSnackbar(event.summary)
                is AppEvent.ReceiptCreated -> snackbar.showSnackbar("Recibo ${event.receipt.folio} emitido")
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) {
                    BrandMark(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    Text(
                        "OPERACIÓN",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                    )
                    AppTab.entries.forEach { tab ->
                        NavigationDrawerItem(
                            label = { Text(tab.title) },
                            selected = tab == selectedTab,
                            onClick = {
                                selectedTab = tab
                                if (tab != AppTab.RENTALS) {
                                    requestedRentalId = null
                                    requestNewRental = false
                                }
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedTab.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir navegación")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            AppContent(
                tab = selectedTab,
                state = state,
                viewModel = viewModel,
                padding = padding,
                onNavigate = { selectedTab = it },
                onRentalRequested = { rentalId ->
                    requestedRentalId = rentalId
                    requestNewRental = false
                    selectedTab = AppTab.RENTALS
                },
                onNewRentalRequested = {
                    requestedRentalId = null
                    requestNewRental = true
                    selectedTab = AppTab.RENTALS
                },
                requestedRentalId = requestedRentalId,
                requestNewRental = requestNewRental,
                onRentalRequestHandled = {
                    requestedRentalId = null
                    requestNewRental = false
                }
            )
        }
    }
}

@Composable
private fun AppContent(
    tab: AppTab,
    state: AppUiState,
    viewModel: AppViewModel,
    padding: PaddingValues,
    onNavigate: (AppTab) -> Unit,
    onRentalRequested: (String) -> Unit,
    onNewRentalRequested: () -> Unit,
    requestedRentalId: String?,
    requestNewRental: Boolean,
    onRentalRequestHandled: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        when (tab) {
            AppTab.DASHBOARD -> DashboardScreen(
                state = state,
                onOpen = { rental -> onRentalRequested(rental.id) },
                onNewRental = onNewRentalRequested
            )
            AppTab.CLIENTS -> ClientsScreen(state, viewModel)
            AppTab.TENTS -> TentsScreen(state, viewModel)
            AppTab.RENTALS -> RentalsScreen(
                state = state,
                viewModel = viewModel,
                requestedRentalId = requestedRentalId,
                requestNewRental = requestNewRental,
                onRentalRequestHandled = onRentalRequestHandled
            )
            AppTab.RECEIPTS -> ReceiptsScreen(state, viewModel)
            AppTab.SETTINGS -> SettingsScreen(state, viewModel)
        }
    }
}
