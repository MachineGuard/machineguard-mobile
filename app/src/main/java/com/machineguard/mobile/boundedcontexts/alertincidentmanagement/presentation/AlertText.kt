package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.machineguard.mobile.R
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertReading
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation.formatReading
import com.machineguard.mobile.shared.domain.EnvironmentalVariable

/**
 * Texto de la alerta en el idioma del teléfono, armado con la lectura que envía el Core. Si la alerta no trae
 * lectura (alertas antiguas), se muestra [message], el texto original del servidor.
 */
@Composable
fun alertText(message: String, reading: AlertReading?, historical: Boolean, zoneName: String?): String {
    if (reading == null) return message
    val variable = stringResource(
        when (reading.variable) {
            EnvironmentalVariable.TEMPERATURE -> R.string.dashboard_variable_temperature
            EnvironmentalVariable.HUMIDITY -> R.string.dashboard_variable_humidity
        },
    )
    val unit = stringResource(
        when (reading.variable) {
            EnvironmentalVariable.TEMPERATURE -> R.string.alerts_unit_temperature
            EnvironmentalVariable.HUMIDITY -> R.string.alerts_unit_humidity
        },
    )
    val value = formatReading(reading.value)
    val min = formatReading(reading.minimum)
    val max = formatReading(reading.maximum)
    val text = if (zoneName != null) {
        stringResource(R.string.alerts_reading_in_zone, variable, zoneName, value, unit, min, max)
    } else {
        stringResource(R.string.alerts_reading, variable, value, unit, min, max)
    }
    return if (historical) stringResource(R.string.alerts_reading_historical, text) else text
}
