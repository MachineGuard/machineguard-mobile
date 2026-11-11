package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.infrastructure

import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Measurement
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.MeasurementsRepository
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Zone
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZonesRepository
import com.machineguard.mobile.core.data.DataResult
import com.machineguard.mobile.core.data.RemoteSource

private const val BASE = "/api/v1/environmental-monitoring"

class ZonesRepositoryImpl(private val remote: RemoteSource) : ZonesRepository {

    override suspend fun getZones(): DataResult<List<Zone>> =
        remote.get(cacheKey = "zones", path = "$BASE/zones", parse = ::parseZones)

    override suspend fun getZone(zoneId: String): DataResult<Zone> =
        remote.get(cacheKey = "zone:$zoneId", path = "$BASE/zones/$zoneId", parse = ::parseZone)
}

class MeasurementsRepositoryImpl(private val remote: RemoteSource) : MeasurementsRepository {

    override suspend fun getLatest(zoneId: String?): DataResult<List<Measurement>> = remote.get(
        cacheKey = "measurements-latest:${zoneId.orEmpty()}",
        path = "$BASE/measurements/latest",
        query = mapOf("monitoringZoneId" to zoneId),
        parse = ::parseMeasurements,
    )
}
