package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Locale

/** Presentation projections only, not REST DTOs or domain repository contracts. */
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data object Error : DashboardUiState
    @ConsistentCopyVisibility
    data class Content internal constructor(val zones: List<ZoneCardUiModel>) : DashboardUiState

    companion object {
        fun fromZones(zones: List<ZoneCardUiModel>): DashboardUiState =
            if (zones.isEmpty()) Empty else Content(
                zones.sortedWith(compareBy<ZoneCardUiModel> { it.condition.priority }
                    .thenBy { it.name.lowercase(Locale.ROOT) }.thenBy { it.id }),
            )
    }
}

/** Environmental condition, distinct from administrative ACTIVE/INACTIVE status. */
enum class ZoneCondition(val label: String, val priority: Int) {
    OUT_OF_RANGE("Fuera de rango", 0),
    STALE("Desactualizada", 1),
    NO_DATA("Sin datos", 2),
    NORMAL("Normal", 3),
}

data class ZoneCardUiModel(
    val id: String,
    val name: String,
    val description: String?,
    val condition: ZoneCondition,
    val sensorsOnline: Int,
    val sensorsOffline: Int,
    val sensorsInactive: Int,
    val lastUpdatedAt: OffsetDateTime?,
    val readings: List<PointReadingUiModel>,
)

/** Readings stay separate by point and variable; the UI does not average sensors. */
data class PointReadingUiModel(
    val pointName: String,
    val variable: ReadingVariable,
    val value: BigDecimal?,
    val recordedAt: OffsetDateTime?,
)

enum class ReadingVariable(val label: String, val unit: String) {
    TEMPERATURE("Temperatura", "°C"),
    HUMIDITY("Humedad", "% HR"),
}
