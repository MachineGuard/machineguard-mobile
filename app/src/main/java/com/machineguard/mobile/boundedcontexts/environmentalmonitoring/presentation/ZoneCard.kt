package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ZoneCard(zone: ZoneCardUiModel, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(zone.name, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            zone.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                zone.condition.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (zone.condition == ZoneCondition.OUT_OF_RANGE) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
            )
            Text("Sensores: ${zone.sensorsOnline} en línea · ${zone.sensorsOffline} sin conexión · ${zone.sensorsInactive} inactivos")
            HorizontalDivider()
            if (zone.condition == ZoneCondition.STALE && zone.readings.isNotEmpty()) {
                Text("Últimas lecturas registradas; la información está desactualizada.")
            }
            if (zone.readings.isEmpty()) Text("Sin lecturas disponibles")
            zone.readings.forEach { reading ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${reading.pointName} · ${reading.variable.label}", style = MaterialTheme.typography.labelLarge)
                    Text(
                        reading.value?.let { "${formatReading(it)} ${reading.variable.unit}" } ?: "Sin lectura",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("Registrada: ${formatReadingTime(reading.recordedAt)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Última actualización: ${formatReadingTime(zone.lastUpdatedAt)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun formatReading(value: BigDecimal): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 2
}.format(value)

internal fun formatReadingTime(value: OffsetDateTime?): String = value?.atZoneSameInstant(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) ?: "Sin fecha disponible"
