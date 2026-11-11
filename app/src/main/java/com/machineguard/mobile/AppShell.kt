package com.machineguard.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.activity.compose.BackHandler
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail.IncidentDetailScreen
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail.IncidentDetailViewModel
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.AlertsScreen
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation.DashboardScreen
import com.machineguard.mobile.boundedcontexts.iam.presentation.SettingsScreen

private fun AppDestination.icon(): ImageVector = when (this) {
    AppDestination.Dashboard -> Icons.Outlined.Home
    AppDestination.Alerts -> Icons.Outlined.Notifications
    AppDestination.Settings -> Icons.Outlined.Settings
}

@Composable
fun AppShell() {
    var selected by rememberSaveable { mutableStateOf(AppDestination.Dashboard) }
    // Keeps each tab's saved UI state (for example the alerts filter) when switching tabs.
    val tabState = rememberSaveableStateHolder()
    // Incidente abierto desde la lista de alertas; mientras exista se muestra su detalle sobre la navegación.
    var openIncidentId by rememberSaveable { mutableStateOf<String?>(null) }

    openIncidentId?.let { incidentId ->
        IncidentDetailRoute(incidentId = incidentId, onBack = { openIncidentId = null })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == selected,
                        onClick = { selected = destination },
                        icon = { Icon(destination.icon(), contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            tabState.SaveableStateProvider(selected.name) {
                when (selected) {
                    AppDestination.Dashboard -> DashboardScreen()
                    AppDestination.Alerts -> AlertsScreen(onOpenIncident = { openIncidentId = it })
                    AppDestination.Settings -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun IncidentDetailRoute(incidentId: String, onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val session by container.authRepository.session.collectAsState()
    val canManage = (session as? SessionState.SignedIn)?.user?.canManageIncidents == true
    val factory = remember(incidentId, canManage) {
        viewModelFactory {
            initializer { IncidentDetailViewModel(incidentId, container.incidentsRepository, canManage) }
        }
    }
    BackHandler(onBack = onBack)
    IncidentDetailScreen(
        viewModel = viewModel(key = "incident-$incidentId-$canManage", factory = factory),
        onBack = onBack,
    )
}
