package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain

import com.machineguard.mobile.shared.domain.EnvironmentalVariable
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Condición ambiental de una zona. Es distinta del estado administrativo (activa/inactiva): una zona activa puede
 * estar [OUT_OF_RANGE], [OFFLINE], etc. Coincide con `environmentalCondition` del Core API.
 */
enum class ZoneCondition {
    OUT_OF_RANGE,
    NEAR_LIMIT,
    OFFLINE,
    NO_DATA,
    NORMAL,

    /** Valor que esta versión de la app no conoce. La pantalla debería tratarlo como [NO_DATA]. */
    UNKNOWN,
    ;

    companion object {
        fun fromApi(value: String?): ZoneCondition = entries.firstOrNull { it.name == value && it != UNKNOWN } ?: UNKNOWN
    }
}

enum class ZoneStatus {
    ACTIVE,
    INACTIVE,
    UNKNOWN,
    ;

    companion object {
        fun fromApi(value: String?): ZoneStatus = entries.firstOrNull { it.name == value && it != UNKNOWN } ?: UNKNOWN
    }
}

/** Si la lectura es lo bastante reciente para fiarse de ella (según el intervalo de muestreo del sensor). */
enum class Freshness {
    FRESH,
    STALE,
    NO_DATA,
    UNKNOWN,
    ;

    companion object {
        fun fromApi(value: String?): Freshness = entries.firstOrNull { it.name == value && it != UNKNOWN } ?: UNKNOWN
    }
}

enum class MeasurementSource {
    /** Capturada por un sensor y enviada por el gateway: la única que acredita disponibilidad y recuperación. */
    SENSOR,

    /** Ingresada por una persona. */
    MANUAL,

    /** Lecturas anteriores a que el servidor registrara el origen. */
    LEGACY_UNKNOWN,
    ;

    companion object {
        fun fromApi(value: String?): MeasurementSource =
            entries.firstOrNull { it.name == value } ?: LEGACY_UNKNOWN
    }
}

data class MonitoringPoint(
    val id: String,
    val name: String,
    val location: String?,
    val active: Boolean,
)

/**
 * Última lectura de un punto para una variable. El valor es decimal exacto ([BigDecimal]) y nunca se promedia entre
 * puntos. Una variable sin lectura simplemente no aparece en la lista de la zona; no se representa con cero.
 */
data class Measurement(
    val id: String,
    val zoneId: String,
    val pointId: String,
    val variable: EnvironmentalVariable,
    val value: BigDecimal,
    val recordedAt: OffsetDateTime,
    val source: MeasurementSource,
    val freshness: Freshness,
)

/** Zona monitoreada con su proyección actual (`GET /api/v1/environmental-monitoring/zones`). */
data class Zone(
    val id: String,
    val name: String,
    val description: String?,
    val status: ZoneStatus,
    val condition: ZoneCondition,
    val sensorsOnline: Int,
    val sensorsOffline: Int,
    val sensorsInactive: Int,
    val lastUpdatedAt: OffsetDateTime?,
    val points: List<MonitoringPoint>,
    /** Una lectura por punto y variable. Para el nombre del punto, buscar [Measurement.pointId] en [points]. */
    val latestMeasurements: List<Measurement>,
) {
    fun pointName(pointId: String): String? = points.firstOrNull { it.id == pointId }?.name
}
