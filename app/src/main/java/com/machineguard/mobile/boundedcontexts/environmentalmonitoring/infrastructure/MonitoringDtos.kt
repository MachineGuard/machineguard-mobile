package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.infrastructure

import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Freshness
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Measurement
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.MeasurementSource
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.MonitoringPoint
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Zone
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZoneCondition
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZoneStatus
import com.machineguard.mobile.core.network.ApiJson
import com.machineguard.mobile.core.network.BigDecimalLiteralSerializer
import com.machineguard.mobile.core.network.parseApiDate
import com.machineguard.mobile.shared.domain.EnvironmentalVariable
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.math.BigDecimal

/** Formas JSON de `EnvironmentalMonitoringResources` del Core API (solo los campos que usa la app). */
@Serializable
internal data class PointDto(
    val id: String,
    val name: String = "",
    val location: String? = null,
    val status: String? = null,
)

@Serializable
internal data class MeasurementDto(
    val id: String,
    val monitoringZoneId: String,
    val monitoringPointId: String,
    val environmentalVariable: String? = null,
    @Serializable(with = BigDecimalLiteralSerializer::class)
    val measuredValue: BigDecimal? = null,
    val recordedAt: String? = null,
    val source: String? = null,
    val freshness: String? = null,
)

@Serializable
internal data class ZoneDto(
    val id: String,
    val name: String = "",
    val description: String? = null,
    val status: String? = null,
    val environmentalCondition: String? = null,
    val sensorsOnline: Int = 0,
    val sensorsOffline: Int = 0,
    val sensorsInactive: Int = 0,
    val lastUpdatedAt: String? = null,
    val points: List<PointDto> = emptyList(),
    val latestMeasurements: List<MeasurementDto> = emptyList(),
)

/**
 * Una lectura sin variable conocida, sin valor o sin fecha no se puede mostrar sin inventar datos (por ejemplo, un
 * cero): se descarta en lugar de rellenarla.
 */
internal fun MeasurementDto.toDomainOrNull(): Measurement? {
    val variable = EnvironmentalVariable.fromApiOrNull(environmentalVariable) ?: return null
    val value = measuredValue ?: return null
    val recordedAt = parseApiDate(recordedAt) ?: return null
    return Measurement(
        id = id,
        zoneId = monitoringZoneId,
        pointId = monitoringPointId,
        variable = variable,
        value = value,
        recordedAt = recordedAt,
        source = MeasurementSource.fromApi(source),
        freshness = Freshness.fromApi(freshness),
    )
}

internal fun PointDto.toDomain(): MonitoringPoint =
    MonitoringPoint(id = id, name = name, location = location, active = status != "INACTIVE")

internal fun ZoneDto.toDomain(): Zone = Zone(
    id = id,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    status = ZoneStatus.fromApi(status),
    condition = ZoneCondition.fromApi(environmentalCondition),
    sensorsOnline = sensorsOnline,
    sensorsOffline = sensorsOffline,
    sensorsInactive = sensorsInactive,
    lastUpdatedAt = parseApiDate(lastUpdatedAt),
    points = points.map { it.toDomain() },
    latestMeasurements = latestMeasurements.mapNotNull { it.toDomainOrNull() },
)

internal fun parseZones(body: String): List<Zone> =
    ApiJson.instance.decodeFromString(ListSerializer(ZoneDto.serializer()), body).map { it.toDomain() }

internal fun parseZone(body: String): Zone = ApiJson.instance.decodeFromString(ZoneDto.serializer(), body).toDomain()

internal fun parseMeasurements(body: String): List<Measurement> =
    ApiJson.instance.decodeFromString(ListSerializer(MeasurementDto.serializer()), body).mapNotNull { it.toDomainOrNull() }
