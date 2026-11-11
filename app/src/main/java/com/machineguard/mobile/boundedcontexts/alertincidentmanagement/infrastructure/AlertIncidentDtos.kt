package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.infrastructure

import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Acknowledgement
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertReading
import java.math.BigDecimal
import com.machineguard.mobile.core.network.BigDecimalLiteralSerializer
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AcknowledgementResult
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Alert
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertSeverity
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertStatus
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.CorrectiveAction
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Incident
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentDetail
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentStatus
import com.machineguard.mobile.core.network.ApiJson
import com.machineguard.mobile.core.network.parseApiDate
import com.machineguard.mobile.shared.domain.EnvironmentalVariable
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** Formas JSON de `AlertIncidentResources` del Core API (solo los campos que usa la app). */
@Serializable
internal data class AlertDto(
    val id: String,
    val incidentId: String? = null,
    val monitoringZoneId: String = "",
    val severity: String? = null,
    val status: String? = null,
    val message: String = "",
    val raisedAt: String? = null,
    val acknowledgedAt: String? = null,
    val escalatedAt: String? = null,
    val historical: Boolean = false,
    /** Lectura que originó la alerta; ausente en alertas anteriores a que el Core la expusiera. */
    val environmentalVariable: String? = null,
    @Serializable(with = BigDecimalLiteralSerializer::class)
    val measuredValue: BigDecimal? = null,
    @Serializable(with = BigDecimalLiteralSerializer::class)
    val minimumValue: BigDecimal? = null,
    @Serializable(with = BigDecimalLiteralSerializer::class)
    val maximumValue: BigDecimal? = null,
)

@Serializable
internal data class IncidentDto(
    val id: String,
    val originMeasurementId: String? = null,
    val monitoringZoneId: String = "",
    val environmentalVariable: String? = null,
    val status: String? = null,
    val openedAt: String? = null,
    val resolvedAt: String? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val historical: Boolean = false,
)

@Serializable
internal data class AcknowledgementDto(
    val acknowledgedBy: String,
    val acknowledgedAt: String? = null,
    val note: String? = null,
)

@Serializable
internal data class CorrectiveActionDto(
    val id: String,
    val incidentId: String = "",
    val description: String = "",
    val performedBy: String = "",
    val performedAt: String? = null,
    val notes: String? = null,
)

@Serializable
internal data class IncidentDetailDto(
    val incident: IncidentDto,
    val alert: AlertDto? = null,
    val acknowledgement: AcknowledgementDto? = null,
    val correctiveActions: List<CorrectiveActionDto> = emptyList(),
    val withinSafeRange: Boolean = false,
    val resolutionAllowed: Boolean = false,
    val resolutionBlockReasons: List<String> = emptyList(),
)

@Serializable
internal data class AlertAcknowledgementDto(
    val alertId: String,
    val incidentId: String,
    val acknowledgedBy: String,
    val acknowledgedAt: String? = null,
    val note: String? = null,
    val alertStatus: String? = null,
    val incidentStatus: String? = null,
)

// Cuerpos de las escrituras. El autor nunca se envía: el servidor usa el usuario del token.
@Serializable
internal data class AcknowledgeRequestDto(val note: String? = null)

@Serializable
internal data class CorrectiveActionRequestDto(val description: String, val notes: String? = null)

@Serializable
internal data class ResolveRequestDto(val notes: String? = null)

internal fun AlertDto.toDomain(): Alert = Alert(
    id = id,
    incidentId = incidentId,
    monitoringZoneId = monitoringZoneId,
    severity = AlertSeverity.fromApi(severity),
    status = AlertStatus.fromApi(status),
    message = message,
    raisedAt = parseApiDate(raisedAt),
    acknowledgedAt = parseApiDate(acknowledgedAt),
    escalatedAt = parseApiDate(escalatedAt),
    historical = historical,
    reading = readingOrNull(),
)

private fun AlertDto.readingOrNull(): AlertReading? {
    val variable = EnvironmentalVariable.fromApiOrNull(environmentalVariable) ?: return null
    return AlertReading(variable, measuredValue ?: return null, minimumValue ?: return null, maximumValue ?: return null)
}

internal fun IncidentDto.toDomain(): Incident = Incident(
    id = id,
    originMeasurementId = originMeasurementId,
    monitoringZoneId = monitoringZoneId,
    environmentalVariable = EnvironmentalVariable.fromApiOrNull(environmentalVariable),
    status = IncidentStatus.fromApi(status),
    openedAt = parseApiDate(openedAt),
    resolvedAt = parseApiDate(resolvedAt),
    resolvedBy = resolvedBy,
    resolutionNotes = resolutionNotes?.takeIf { it.isNotBlank() },
    historical = historical,
)

internal fun CorrectiveActionDto.toDomain(): CorrectiveAction = CorrectiveAction(
    id = id,
    incidentId = incidentId,
    description = description,
    performedBy = performedBy,
    performedAt = parseApiDate(performedAt),
    notes = notes?.takeIf { it.isNotBlank() },
)

internal fun IncidentDetailDto.toDomain(): IncidentDetail = IncidentDetail(
    incident = incident.toDomain(),
    alert = alert?.toDomain(),
    acknowledgement = acknowledgement?.let {
        Acknowledgement(it.acknowledgedBy, parseApiDate(it.acknowledgedAt), it.note?.takeIf { note -> note.isNotBlank() })
    },
    correctiveActions = correctiveActions.map { it.toDomain() },
    withinSafeRange = withinSafeRange,
    resolutionAllowed = resolutionAllowed,
    resolutionBlockReasons = resolutionBlockReasons,
)

internal fun AlertAcknowledgementDto.toDomain(): AcknowledgementResult = AcknowledgementResult(
    alertId = alertId,
    incidentId = incidentId,
    acknowledgedBy = acknowledgedBy,
    acknowledgedAt = parseApiDate(acknowledgedAt),
    note = note?.takeIf { it.isNotBlank() },
    alertStatus = AlertStatus.fromApi(alertStatus),
    incidentStatus = IncidentStatus.fromApi(incidentStatus),
)

internal fun parseAlerts(body: String): List<Alert> =
    ApiJson.instance.decodeFromString(ListSerializer(AlertDto.serializer()), body).map { it.toDomain() }

internal fun parseAlert(body: String): Alert = ApiJson.instance.decodeFromString(AlertDto.serializer(), body).toDomain()

internal fun parseIncidents(body: String): List<Incident> =
    ApiJson.instance.decodeFromString(ListSerializer(IncidentDto.serializer()), body).map { it.toDomain() }

internal fun parseIncident(body: String): Incident =
    ApiJson.instance.decodeFromString(IncidentDto.serializer(), body).toDomain()

internal fun parseIncidentDetail(body: String): IncidentDetail =
    ApiJson.instance.decodeFromString(IncidentDetailDto.serializer(), body).toDomain()

internal fun parseCorrectiveAction(body: String): CorrectiveAction =
    ApiJson.instance.decodeFromString(CorrectiveActionDto.serializer(), body).toDomain()

internal fun parseAcknowledgement(body: String): AcknowledgementResult =
    ApiJson.instance.decodeFromString(AlertAcknowledgementDto.serializer(), body).toDomain()
