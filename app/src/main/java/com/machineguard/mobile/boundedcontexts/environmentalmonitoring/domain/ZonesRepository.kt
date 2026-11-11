package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain

import com.machineguard.mobile.core.data.DataResult

/**
 * Zonas y lecturas actuales del dashboard.
 *
 * Contrato con el Core API: `GET /api/v1/environmental-monitoring/zones` y `.../zones/{id}` (ADMIN o VIEWER).
 * Cada zona ya trae sus puntos y la última lectura por punto y variable, así que el dashboard no necesita otra
 * petición para las lecturas.
 *
 * Caché: siempre se consulta el servidor (los datos de monitoreo cambian cada minuto) y la última respuesta correcta
 * se guarda en el dispositivo. Si no hay conexión, estas funciones devuelven esa copia como
 * `Success(source = CACHE)`; solo devuelven `Failure(Offline)` si nunca hubo una respuesta guardada. "Reintentar" y
 * "deslizar para actualizar" son simplemente volver a llamar.
 */
interface ZonesRepository {

    suspend fun getZones(): DataResult<List<Zone>>

    suspend fun getZone(zoneId: String): DataResult<Zone>
}
