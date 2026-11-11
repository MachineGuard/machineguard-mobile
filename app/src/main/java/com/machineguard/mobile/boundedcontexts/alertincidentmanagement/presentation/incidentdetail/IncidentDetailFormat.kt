package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail

import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertSeverity
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertStatus
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentStatus
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

fun AlertSeverity.label() = when (this) {
    AlertSeverity.LOW -> "Baja"
    AlertSeverity.MEDIUM -> "Media"
    AlertSeverity.HIGH -> "Alta"
    AlertSeverity.CRITICAL -> "Crítica"
    AlertSeverity.UNKNOWN -> "Sin clasificar"
}

fun AlertStatus.label() = when (this) {
    AlertStatus.PENDING -> "Pendiente"
    AlertStatus.ACKNOWLEDGED -> "Reconocida"
    AlertStatus.ESCALATED -> "Escalada"
    AlertStatus.CLOSED -> "Cerrada"
    AlertStatus.UNKNOWN -> "Desconocido"
}

fun IncidentStatus.label() = when (this) {
    IncidentStatus.OPEN -> "Abierto"
    IncidentStatus.ACKNOWLEDGED -> "Reconocido"
    IncidentStatus.ESCALATED -> "Escalado"
    IncidentStatus.RESOLVED -> "Resuelto"
    IncidentStatus.UNKNOWN -> "Desconocido"
}

/** Mensaje amigable para el usuario; el texto crudo del servidor solo se usa como motivo de un conflicto. */
fun AppError.message(): String = when (this) {
    AppError.Offline -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    AppError.Timeout -> "El servidor tardó demasiado en responder."
    AppError.SessionExpired -> "Tu sesión terminó. Inicia sesión de nuevo."
    AppError.InvalidCredentials -> "Credenciales incorrectas."
    is AppError.Forbidden -> "Tu rol es de solo lectura: no puedes realizar esta acción."
    is AppError.NotFound -> "El incidente ya no existe o no tienes acceso."
    is AppError.Conflict -> detail?.takeIf { it.isNotBlank() } ?: "La acción ya no es posible: el incidente cambió."
    is AppError.Invalid -> "Revisa los datos ingresados."
    is AppError.TooManyRequests -> "Demasiados intentos. Espera un momento."
    is AppError.Server -> "El servidor tuvo un problema. Inténtalo más tarde."
    is AppError.Unknown -> "Ocurrió un error inesperado."
}

fun ActionKind.successMessage() = when (this) {
    ActionKind.ACKNOWLEDGE -> "Alerta reconocida."
    ActionKind.CORRECTIVE_ACTION -> "Acción correctiva registrada."
    ActionKind.RESOLVE -> "Incidente resuelto."
}
