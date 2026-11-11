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
                    AppDestination.Alerts -> AlertsScreen()
                    AppDestination.Settings -> SettingsScreen()
                }
            }
        }
    }
}
