package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain

import com.machineguard.mobile.shared.domain.EnvironmentalVariable
import java.time.OffsetDateTime

enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,

    /** Valor que esta versión de la app no conoce. */
    UNKNOWN,
    ;

    companion object {
        fun fromApi(value: String?): AlertSeverity = entries.firstOrNull { it.name == value && it != UNKNOWN } ?: UNKNOWN
    }
}

enum class AlertStatus {
    PENDING,
    ACKNOWLEDGED,
    ESCALATED,
    CLOSED,
    UNKNOWN,
    ;

    companion object {
        fun fromApi(value: String?): AlertStatus = entries.firstOrNull { it.name == value && it != UNKNOWN } ?: UNKNOWN
    }
}

enum class IncidentStatus {
    OPEN,
    ACKNOWLEDGED,
    ESCALATED,
    RESOLVED,
    UNKNOWN,
    ;

    /** Mientras no esté resuelto se puede reconocer, registrar acciones y resolver. */
    val isActive: Boolean get() = this == OPEN || this == ACKNOWLEDGED || this == ESCALATED

    companion object {
        fun fromApi(value: String?): IncidentStatus = entries.firstOrNull { it.name == value && it != UNKNOWN } ?: UNKNOWN
    }
}

/**
 * Alerta de una desviación (`GET /api/v1/alerts`). Se ordenan por [raisedAt] descendente. [incidentId] es el caso al
 * que pertenece; el detalle completo está en [IncidentsRepository.getIncident].
 */
data class Alert(
    val id: String,
    val incidentId: String?,
    val monitoringZoneId: String,
    val severity: AlertSeverity,
    val status: AlertStatus,
    val message: String,
    val raisedAt: OffsetDateTime?,
    val acknowledgedAt: OffsetDateTime?,
    val escalatedAt: OffsetDateTime?,
    /** Verdadero si el incidente de esta alerta ya no refleja la situación actual de la zona. */
    val historical: Boolean,
)

data class Incident(
    val id: String,
    val originMeasurementId: String?,
    val monitoringZoneId: String,
    val environmentalVariable: EnvironmentalVariable?,
    val status: IncidentStatus,
    val openedAt: OffsetDateTime?,
    val resolvedAt: OffsetDateTime?,
    val resolvedBy: String?,
    val resolutionNotes: String?,
    val historical: Boolean,
)

/** Quién reconoció la alerta, cuándo y con qué nota. */
data class Acknowledgement(
    val acknowledgedBy: String,
    val acknowledgedAt: OffsetDateTime?,
    val note: String?,
)

data class CorrectiveAction(
    val id: String,
    val incidentId: String,
    val description: String,
    val performedBy: String,
    val performedAt: OffsetDateTime?,
    val notes: String?,
)

/**
 * Detalle del incidente (`GET /api/v1/incidents/{id}`): el caso, su alerta, el reconocimiento y las acciones.
 *
 * [resolutionAllowed] indica si **el usuario actual** puede resolverlo ahora (ADMIN, incidente activo, con al menos una
 * acción correctiva y lecturas seguras); si es falso, [resolutionBlockReasons] explica por qué. La pantalla debería
 * usar esa bandera para habilitar el botón "Resolver", sin repetir las reglas.
 */
data class IncidentDetail(
    val incident: Incident,
    val alert: Alert?,
    val acknowledgement: Acknowledgement?,
    val correctiveActions: List<CorrectiveAction>,
    val withinSafeRange: Boolean,
    val resolutionAllowed: Boolean,
    val resolutionBlockReasons: List<String>,
)

/** Resultado de reconocer una alerta: ambos quedan en su nuevo estado. */
data class AcknowledgementResult(
    val alertId: String,
    val incidentId: String,
    val acknowledgedBy: String,
    val acknowledgedAt: OffsetDateTime?,
    val note: String?,
    val alertStatus: AlertStatus,
    val incidentStatus: IncidentStatus,
)
