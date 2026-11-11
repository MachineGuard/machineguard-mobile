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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.machineguard.mobile.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ZoneCard(zone: ZoneCardUiModel, modifier: Modifier = Modifier) {
    val dateMissing = stringResource(R.string.dashboard_date_missing)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(zone.name, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            zone.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(zone.condition.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = if (zone.condition == ZoneCondition.OUT_OF_RANGE) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(
                    R.string.dashboard_sensors_summary,
                    zone.sensorsOnline,
                    zone.sensorsOffline,
                    zone.sensorsInactive,
                ),
            )
            HorizontalDivider()
            if (zone.condition == ZoneCondition.STALE && zone.readings.isNotEmpty()) {
                Text(stringResource(R.string.dashboard_stale_notice))
            }
            if (zone.readings.isEmpty()) Text(stringResource(R.string.dashboard_readings_missing))
            zone.readings.forEach { reading ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${reading.pointName ?: stringResource(R.string.dashboard_point_unnamed)} · " +
                            stringResource(reading.variable.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        reading.value?.let { "${formatReading(it)} ${reading.variable.unit}" }
                            ?: stringResource(R.string.dashboard_reading_missing),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(
                            R.string.dashboard_recorded_at,
                            formatReadingTime(reading.recordedAt, dateMissing),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                stringResource(R.string.dashboard_last_updated, formatReadingTime(zone.lastUpdatedAt, dateMissing)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

internal fun formatReading(value: BigDecimal): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 2
}.format(value)

/** [missing] is the text shown when there is no instant; the caller passes the localized string. */
internal fun formatReadingTime(value: OffsetDateTime?, missing: String): String = value?.atZoneSameInstant(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) ?: missing
