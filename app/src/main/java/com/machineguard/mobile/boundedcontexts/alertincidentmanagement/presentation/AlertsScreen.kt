package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import androidx.compose.runtime.Composable
import com.machineguard.mobile.shared.ui.FeaturePlaceholder

/** [onOpenIncident] se invoca con el `incidentId` de la alerta tocada para abrir su detalle. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun AlertsScreen(onOpenIncident: (String) -> Unit = {}) {
    FeaturePlaceholder(
        title = "Alerts",
        description = "Alertas activas y reconocidas, con filtros rápidos.",
    )
}
