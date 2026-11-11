package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation

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
import java.math.BigDecimal
import java.time.OffsetDateTime

/** Debug-only visual fixtures. Neither the app entry point nor a repository uses them. */
class DashboardPreviewStates : PreviewParameterProvider<DashboardUiState> {
    override val values = sequenceOf(
        DashboardUiState.Loading,
        DashboardUiState.Empty,
        DashboardUiState.Error,
        DashboardUiState.Unavailable,
        exampleContent(),
    )
}

@Preview(name = "Dashboard · claro", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dashboard · oscuro", showBackground = true, widthDp = 360, heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Dashboard · texto grande", showBackground = true, widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun DashboardPreview(@PreviewParameter(DashboardPreviewStates::class) state: DashboardUiState) {
    MachineGuardTheme { DashboardContent(state, onRetry = {}) }
}

class ZoneCardPreviewStates : PreviewParameterProvider<ZoneCardUiModel> {
    override val values = (exampleContent() as DashboardUiState.Content).zones.asSequence()
}

/** Each condition can be inspected without scrolling the dashboard preview. */
@Preview(name = "Zona · claro", showBackground = true, widthDp = 360)
@Preview(name = "Zona · oscuro", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Zona · texto grande", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun ZoneCardPreview(@PreviewParameter(ZoneCardPreviewStates::class) zone: ZoneCardUiModel) {
    MachineGuardTheme {
        Surface { ZoneCard(zone, modifier = Modifier.padding(16.dp)) }
    }
}

/** In interactive preview, retry demonstrates the UI callback, not a network request. */
@Preview(name = "Reintento visual · solo ejemplo", showBackground = true)
@Composable
private fun RetryPreview() {
    var state by remember { mutableStateOf<DashboardUiState>(DashboardUiState.Error) }
    MachineGuardTheme { DashboardContent(state, onRetry = { state = exampleContent() }) }
}

private fun exampleContent(): DashboardUiState {
    val recordedAt = OffsetDateTime.parse("2026-11-10T10:15:00-05:00")
    return DashboardUiState.fromZones(ZoneCondition.entries.reversed().map { condition ->
        ZoneCardUiModel(
            id = condition.name,
            name = "Zona de ejemplo · ${condition.label}",
            description = "Datos de muestra para revisión visual",
            condition = condition,
            sensorsOnline = if (condition == ZoneCondition.OFFLINE) 0 else 1,
            sensorsOffline = if (condition == ZoneCondition.OFFLINE) 1 else 0,
            sensorsInactive = 0,
            lastUpdatedAt = if (condition == ZoneCondition.NO_DATA) null else recordedAt,
            readings = if (condition == ZoneCondition.NO_DATA) emptyList() else listOf(
                PointReadingUiModel("Punto A", ReadingVariable.TEMPERATURE,
                    BigDecimal(if (condition == ZoneCondition.OUT_OF_RANGE) "12.30" else "4.20"), recordedAt),
                PointReadingUiModel("Punto A", ReadingVariable.HUMIDITY, BigDecimal("65.00"), recordedAt),
                PointReadingUiModel("Punto B", ReadingVariable.TEMPERATURE, null, null),
            ),
        )
    })
}
