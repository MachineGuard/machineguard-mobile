package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail

import androidx.annotation.StringRes
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertSeverity
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentStatus
import com.machineguard.mobile.R
import com.machineguard.mobile.core.data.AppError
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val dateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

fun OffsetDateTime?.label(): String = this?.format(dateTime) ?: "—"

/** Tiempo transcurrido legible: "5 min", "2 h 10 min", "1 d 3 h". */
fun elapsedLabel(from: OffsetDateTime?, to: OffsetDateTime): String {
    if (from == null) return "—"
    val d = Duration.between(from, to).coerceAtLeast(Duration.ZERO)
    val days = d.toDays()
    val hours = d.toHours() % 24
    val minutes = d.toMinutes() % 60
    return when {
        days > 0 -> "$days d $hours h"
        d.toHours() > 0 -> "${d.toHours()} h $minutes min"
        else -> "${d.toMinutes()} min"
    }
}

@StringRes
fun AlertSeverity.labelRes(): Int = when (this) {
    AlertSeverity.LOW -> R.string.alerts_severity_low
    AlertSeverity.MEDIUM -> R.string.alerts_severity_medium
    AlertSeverity.HIGH -> R.string.alerts_severity_high
    AlertSeverity.CRITICAL -> R.string.alerts_severity_critical
    AlertSeverity.UNKNOWN -> R.string.alerts_severity_unknown
}

@StringRes
fun IncidentStatus.labelRes(): Int = when (this) {
    IncidentStatus.OPEN -> R.string.incident_status_open
    IncidentStatus.ACKNOWLEDGED -> R.string.incident_status_acknowledged
    IncidentStatus.ESCALATED -> R.string.incident_status_escalated
    IncidentStatus.RESOLVED -> R.string.incident_status_resolved
    IncidentStatus.UNKNOWN -> R.string.incident_status_unknown
}

@StringRes
fun TimelineStep.Kind.titleRes(): Int = when (this) {
    TimelineStep.Kind.RAISED -> R.string.incident_step_raised
    TimelineStep.Kind.ACKNOWLEDGED -> R.string.incident_step_acknowledged
    TimelineStep.Kind.ESCALATED -> R.string.incident_step_escalated
    TimelineStep.Kind.CORRECTIVE_ACTION -> R.string.incident_step_action
    TimelineStep.Kind.RESOLVED -> R.string.incident_step_resolved
}

/** Mensaje amigable para el usuario; el texto crudo del servidor solo se usa como motivo de un conflicto. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Offline -> R.string.incident_error_offline
    AppError.Timeout -> R.string.incident_error_timeout
    AppError.SessionExpired -> R.string.incident_error_session_expired
    AppError.InvalidCredentials -> R.string.incident_error_invalid_credentials
    is AppError.Forbidden -> R.string.incident_error_forbidden
    is AppError.NotFound -> R.string.incident_error_not_found
    is AppError.Conflict -> R.string.incident_error_conflict
    is AppError.Invalid -> R.string.incident_error_invalid
    is AppError.TooManyRequests -> R.string.incident_error_too_many
    is AppError.Server -> R.string.incident_error_server
    is AppError.Unknown -> R.string.incident_error_unknown
}

/** Motivo que envía el servidor al rechazar una acción por regla de negocio (solo en conflictos), si lo hay. */
fun AppError.serverReason(): String? = (this as? AppError.Conflict)?.detail?.takeIf { it.isNotBlank() }

@StringRes
fun ActionKind.successRes(): Int = when (this) {
    ActionKind.ACKNOWLEDGE -> R.string.incident_ok_acknowledge
    ActionKind.CORRECTIVE_ACTION -> R.string.incident_ok_action
    ActionKind.RESOLVE -> R.string.incident_ok_resolve
}
