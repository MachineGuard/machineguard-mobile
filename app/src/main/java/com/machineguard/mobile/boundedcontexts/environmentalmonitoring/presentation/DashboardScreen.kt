package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen() {
    // No repository is available yet. Sample readings exist only in debug previews.
    DashboardContent(DashboardUiState.Unavailable, onRetry = {})
}

/** State and events belong to the caller. This UI never fetches or fabricates readings. */
@Composable
fun DashboardContent(state: DashboardUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dashboard", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                    Text("Zonas y monitoreo ambiental", style = MaterialTheme.typography.bodyLarge)
                }
            }
            when (state) {
                DashboardUiState.Loading -> item(key = "loading") {
                    DashboardMessage("Cargando zonas…") { CircularProgressIndicator() }
                }
                DashboardUiState.Empty -> item(key = "empty") {
                    DashboardMessage("No hay zonas disponibles.")
                }
                DashboardUiState.Unavailable -> item(key = "unavailable") {
                    DashboardMessage("Monitoreo no disponible", "Todavía no es posible consultar las zonas desde esta versión.")
                }
                DashboardUiState.Error -> item(key = "error") {
                    DashboardMessage("No pudimos cargar las zonas", "Vuelve a intentarlo para consultar las lecturas.") {
                        Button(onClick = onRetry) { Text("Reintentar") }
                    }
                }
                is DashboardUiState.Content -> items(state.zones, key = { "zone:${it.id}" }) { ZoneCard(it) }
            }
        }
    }
}

@Composable
private fun DashboardMessage(title: String, description: String? = null, action: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) }
        action()
    }
}
