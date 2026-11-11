package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.machineguard.mobile.core.theme.MachineGuardTheme
import java.time.OffsetDateTime

/** Debug-only fixtures. Production never references these examples. */
class AlertsPreviewStates : PreviewParameterProvider<AlertsUiState> {
    override val values = sequenceOf(
        AlertsUiState.Loading,
        AlertsUiState.Empty,
        AlertsUiState.Error,
        AlertsUiState.Unavailable,
        AlertsUiState.fromAlerts(exampleAlerts()),
        // Content exists but the initial Active filter has no matches.
        AlertsUiState.fromAlerts(exampleAlerts().filter { it.status == AlertStatus.CLOSED }),
    )
}

@Preview(name = "Alertas · claro", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Alertas · oscuro", showBackground = true, widthDp = 360, heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Alertas · texto grande", showBackground = true, widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun AlertsPreview(@PreviewParameter(AlertsPreviewStates::class) state: AlertsUiState) {
    MachineGuardTheme { AlertsContent(state, onRetry = {}) }
}

class AlertCardPreviewStates : PreviewParameterProvider<AlertCardUiModel> {
    override val values = exampleAlerts().asSequence() + exampleAlerts().first().copy(
        id = "missing-fields", incidentId = null, message = "", raisedAt = null,
    )
}

@Preview(name = "Alerta · claro", showBackground = true, widthDp = 360)
@Preview(name = "Alerta · oscuro", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Alerta · texto grande", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun AlertCardPreview(@PreviewParameter(AlertCardPreviewStates::class) alert: AlertCardUiModel) {
    MachineGuardTheme { Surface { AlertCard(alert, modifier = Modifier.padding(16.dp)) } }
}

/** Interactive retry demonstrates an event only; it does not simulate a repository. */
@Preview(name = "Reintento de alertas · solo ejemplo", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AlertsRetryPreview() {
    var state by remember { mutableStateOf<AlertsUiState>(AlertsUiState.Error) }
    MachineGuardTheme {
        AlertsContent(state, onRetry = { state = AlertsUiState.fromAlerts(exampleAlerts()) })
    }
}

private fun exampleAlerts(): List<AlertCardUiModel> {
    val raisedAt = OffsetDateTime.parse("2026-11-10T10:15:00-05:00")
    return AlertStatus.entries.mapIndexed { index, status ->
        AlertCardUiModel(
            id = "example-alert-$index",
            incidentId = "example-incident-$index",
            severity = AlertSeverity.entries.reversed()[index],
            message = "Datos de ejemplo: condición ambiental que requiere revisión en el punto de monitoreo de la zona de almacenamiento.",
            status = status,
            raisedAt = raisedAt.minusHours(index.toLong()),
            acknowledgedAt = if (status == AlertStatus.ACKNOWLEDGED || status == AlertStatus.CLOSED)
                raisedAt.minusHours(index.toLong()).plusMinutes(5) else null,
            escalatedAt = if (status == AlertStatus.ESCALATED)
                raisedAt.minusHours(index.toLong()).plusMinutes(10) else null,
        )
    }
}
