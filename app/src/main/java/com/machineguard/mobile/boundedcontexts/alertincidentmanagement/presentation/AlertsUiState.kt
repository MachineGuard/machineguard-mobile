package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import java.time.OffsetDateTime

/** Presentation projections, not DTOs or a replacement for the data team's repository. */
sealed interface AlertsUiState {
    data object Loading : AlertsUiState
    data object Empty : AlertsUiState
    data object Error : AlertsUiState
    data object Unavailable : AlertsUiState

    @ConsistentCopyVisibility
    data class Content internal constructor(val alerts: List<AlertCardUiModel>) : AlertsUiState

    companion object {
        /** Preserve Core's raised_at DESC, id ASC order; never truncate before filtering. */
        fun fromAlerts(alerts: List<AlertCardUiModel>): AlertsUiState =
            if (alerts.isEmpty()) Empty else Content(alerts.toList())
    }
}

data class AlertCardUiModel(
    val id: String,
    val incidentId: String?,
    val severity: AlertSeverity,
    val message: String,
    val status: AlertStatus,
    val raisedAt: OffsetDateTime?,
    val acknowledgedAt: OffsetDateTime?,
    val escalatedAt: OffsetDateTime?,
)

enum class AlertSeverity { LOW, MEDIUM, HIGH, CRITICAL }
enum class AlertStatus { PENDING, ACKNOWLEDGED, ESCALATED, CLOSED }

enum class AlertFilter {
    ACTIVE, ACKNOWLEDGED, ALL;

    /** ACTIVE groups the two statuses used for outstanding alerts in Web. */
    fun select(alerts: List<AlertCardUiModel>): List<AlertCardUiModel> = alerts.filter {
        when (this) {
            ACTIVE -> it.status == AlertStatus.PENDING || it.status == AlertStatus.ESCALATED
            ACKNOWLEDGED -> it.status == AlertStatus.ACKNOWLEDGED
            ALL -> true
        }
    }
}
