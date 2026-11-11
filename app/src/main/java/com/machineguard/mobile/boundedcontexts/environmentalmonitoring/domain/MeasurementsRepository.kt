package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain

import com.machineguard.mobile.core.data.DataResult

/**
 * Últimas lecturas por punto y variable, para refrescar solo los valores sin volver a pedir las zonas completas.
 *
 * Contrato con el Core API: `GET /api/v1/environmental-monitoring/measurements/latest?monitoringZoneId=...`.
 * Mismas reglas de caché que [ZonesRepository].
 */
interface MeasurementsRepository {

    /** @param zoneId limita el resultado a una zona; con `null` devuelve las de toda la organización. */
    suspend fun getLatest(zoneId: String? = null): DataResult<List<Measurement>>
}
