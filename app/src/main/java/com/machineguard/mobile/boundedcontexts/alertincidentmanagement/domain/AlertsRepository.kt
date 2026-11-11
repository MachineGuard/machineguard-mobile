package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain

import com.machineguard.mobile.core.data.DataResult

/** Filtros opcionales del listado de alertas. */
data class AlertQuery(
    val monitoringZoneId: String? = null,
    val status: AlertStatus? = null,
)

/**
 * Consulta de alertas (solo lectura).
 *
 * Contrato con el Core API: `GET /api/v1/alerts` y `GET /api/v1/alerts/{alertId}` (ADMIN o VIEWER). El listado no
 * tiene paginación y llega ordenado por `raisedAt` descendente: no hay que reordenar ni truncar antes de filtrar en
 * pantalla (las pestañas Active, Acknowledged y All se pueden resolver en el cliente con la lista completa).
 *
 * Caché: igual que [com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZonesRepository], el
 * servidor se consulta siempre y la última lista correcta (para la consulta sin filtros) cubre los fallos de red.
 */
interface AlertsRepository {

    suspend fun getAlerts(query: AlertQuery = AlertQuery()): DataResult<List<Alert>>

    /** Errores posibles: `NotFound` (no existe o es de otra organización). */
    suspend fun getAlert(alertId: String): DataResult<Alert>
}
