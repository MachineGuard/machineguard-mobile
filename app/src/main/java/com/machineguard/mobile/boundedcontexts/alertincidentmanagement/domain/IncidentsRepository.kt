package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain

import com.machineguard.mobile.core.data.DataResult
import java.time.OffsetDateTime

/** Filtros opcionales del historial de incidentes. [from] y [to] filtran por fecha de apertura. */
data class IncidentQuery(
    val monitoringZoneId: String? = null,
    val status: IncidentStatus? = null,
    val from: OffsetDateTime? = null,
    val to: OffsetDateTime? = null,
    /** El servidor acota el valor entre 1 y 500 (100 por defecto). */
    val limit: Int? = null,
)

/**
 * Atención de incidentes: consulta, reconocimiento, acciones correctivas y resolución.
 *
 * Contrato con el Core API: `GET /api/v1/incidents`, `GET /api/v1/incidents/{id}`,
 * `POST /api/v1/alerts/{alertId}/acknowledgements`, `POST /api/v1/incidents/{id}/corrective-actions` y
 * `POST /api/v1/incidents/{id}/resolve`. El autor de cada acción es siempre el usuario autenticado: la app nunca
 * envía quién la hace.
 *
 * Las consultas usan la caché solo como respaldo ante fallos de red. Las **operaciones de escritura nunca usan
 * caché ni se reintentan solas**: si no hay conexión devuelven `Failure(Offline)` y la pantalla decide si reintentar.
 *
 * Errores de escritura habituales: `Forbidden` (el usuario es VIEWER), `NotFound`, y `Conflict` cuando la regla de
 * negocio lo impide (alerta ya reconocida o cerrada, incidente ya resuelto, resolver sin acción correctiva o con
 * lecturas fuera de rango); en ese caso [com.machineguard.mobile.core.data.AppError.detail] trae el motivo.
 */
interface IncidentsRepository {

    suspend fun getIncidents(query: IncidentQuery = IncidentQuery()): DataResult<List<Incident>>

    suspend fun getIncident(incidentId: String): DataResult<IncidentDetail>

    /** Reconoce la alerta; la alerta y su incidente pasan a ACKNOWLEDGED. [note] es opcional (máximo 500 caracteres). */
    suspend fun acknowledgeAlert(alertId: String, note: String? = null): DataResult<AcknowledgementResult>

    /** [description] es obligatoria (máximo 1000 caracteres); [notes] es opcional (máximo 1000). */
    suspend fun registerCorrectiveAction(
        incidentId: String,
        description: String,
        notes: String? = null,
    ): DataResult<CorrectiveAction>

    /** Resuelve el incidente. Conviene consultar antes [IncidentDetail.resolutionAllowed]. [notes] máx. 1000. */
    suspend fun resolveIncident(incidentId: String, notes: String? = null): DataResult<Incident>
}
