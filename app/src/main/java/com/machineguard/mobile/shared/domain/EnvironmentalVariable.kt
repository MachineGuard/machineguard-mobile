package com.machineguard.mobile.shared.domain

/** Variable ambiental medida por los sensores. Compartida por Monitoreo y por Alertas e Incidentes. */
enum class EnvironmentalVariable {
    TEMPERATURE,
    HUMIDITY,
    ;

    companion object {
        /** Los valores nuevos que agregue el servidor no deben romper la app: se tratan como ausentes. */
        fun fromApiOrNull(value: String?): EnvironmentalVariable? = entries.firstOrNull { it.name == value }
    }
}
