package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.infrastructure

import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AcknowledgementResult
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Alert
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertQuery
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertsRepository
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.CorrectiveAction
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Incident
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentDetail
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentQuery
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentsRepository
import com.machineguard.mobile.core.data.DataResult
import com.machineguard.mobile.core.data.RemoteSource
import com.machineguard.mobile.core.network.ApiJson

private const val ALERTS = "/api/v1/alerts"
private const val INCIDENTS = "/api/v1/incidents"

/** Prefijos de caché de lo que cambia cuando se reconoce una alerta o se atiende un incidente. */
private val CACHE_PREFIXES_AFFECTED_BY_WRITES = listOf("alerts", "alert:", "incidents", "incident:")

class AlertsRepositoryImpl(private val remote: RemoteSource) : AlertsRepository {

    override suspend fun getAlerts(query: AlertQuery): DataResult<List<Alert>> = remote.get(
        cacheKey = "alerts?zone=${query.monitoringZoneId.orEmpty()}&status=${query.status?.name.orEmpty()}",
        path = ALERTS,
        query = mapOf("monitoringZoneId" to query.monitoringZoneId, "status" to query.status?.name),
        parse = ::parseAlerts,
    )

    override suspend fun getAlert(alertId: String): DataResult<Alert> =
        remote.get(cacheKey = "alert:$alertId", path = "$ALERTS/$alertId", parse = ::parseAlert)
}

class IncidentsRepositoryImpl(private val remote: RemoteSource) : IncidentsRepository {

    override suspend fun getIncidents(query: IncidentQuery): DataResult<List<Incident>> = remote.get(
        cacheKey = "incidents?zone=${query.monitoringZoneId.orEmpty()}&status=${query.status?.name.orEmpty()}" +
            "&from=${query.from ?: ""}&to=${query.to ?: ""}&limit=${query.limit ?: ""}",
        path = INCIDENTS,
        query = mapOf(
            "monitoringZoneId" to query.monitoringZoneId,
            "status" to query.status?.name,
            "from" to query.from?.toString(),
            "to" to query.to?.toString(),
            "limit" to query.limit?.toString(),
        ),
        parse = ::parseIncidents,
    )

    override suspend fun getIncident(incidentId: String): DataResult<IncidentDetail> =
        remote.get(cacheKey = "incident:$incidentId", path = "$INCIDENTS/$incidentId", parse = ::parseIncidentDetail)

    override suspend fun acknowledgeAlert(alertId: String, note: String?): DataResult<AcknowledgementResult> =
        remote.post(
            path = "$ALERTS/$alertId/acknowledgements",
            jsonBody = ApiJson.instance.encodeToString(AcknowledgeRequestDto.serializer(), AcknowledgeRequestDto(note.blankToNull())),
            invalidatePrefixes = CACHE_PREFIXES_AFFECTED_BY_WRITES,
            parse = ::parseAcknowledgement,
        )

    override suspend fun registerCorrectiveAction(
        incidentId: String,
        description: String,
        notes: String?,
    ): DataResult<CorrectiveAction> = remote.post(
        path = "$INCIDENTS/$incidentId/corrective-actions",
        jsonBody = ApiJson.instance.encodeToString(
            CorrectiveActionRequestDto.serializer(),
            CorrectiveActionRequestDto(description.trim(), notes.blankToNull()),
        ),
        invalidatePrefixes = CACHE_PREFIXES_AFFECTED_BY_WRITES,
        parse = ::parseCorrectiveAction,
    )

    override suspend fun resolveIncident(incidentId: String, notes: String?): DataResult<Incident> = remote.post(
        path = "$INCIDENTS/$incidentId/resolve",
        jsonBody = ApiJson.instance.encodeToString(ResolveRequestDto.serializer(), ResolveRequestDto(notes.blankToNull())),
        invalidatePrefixes = CACHE_PREFIXES_AFFECTED_BY_WRITES,
        parse = ::parseIncident,
    )

    private fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
