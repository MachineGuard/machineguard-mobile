package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.machineguard.mobile.R
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Informational card: Alert.id is never used as a navigation substitute for incidentId. */
@Composable
fun AlertCard(alert: AlertCardUiModel, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(alert.message.ifBlank { stringResource(R.string.alerts_message_missing) },
                modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.alerts_severity, stringResource(alert.severity.labelResource())),
                style = MaterialTheme.typography.labelLarge,
                color = if (alert.severity == AlertSeverity.CRITICAL || alert.severity == AlertSeverity.HIGH)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.alerts_status, stringResource(alert.status.labelResource())),
                style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.alerts_raised_at, alertDate(alert.raisedAt)), style = MaterialTheme.typography.bodyMedium)
            alert.acknowledgedAt?.let {
                Text(stringResource(R.string.alerts_acknowledged_at, alertDate(it)), style = MaterialTheme.typography.bodySmall)
            }
            alert.escalatedAt?.let {
                Text(stringResource(R.string.alerts_escalated_at, alertDate(it)), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun AlertSeverity.labelResource(): Int = when (this) {
    AlertSeverity.LOW -> R.string.alerts_severity_low
    AlertSeverity.MEDIUM -> R.string.alerts_severity_medium
    AlertSeverity.HIGH -> R.string.alerts_severity_high
    AlertSeverity.CRITICAL -> R.string.alerts_severity_critical
    AlertSeverity.UNKNOWN -> R.string.alerts_severity_unknown
}

private fun AlertStatus.labelResource(): Int = when (this) {
    AlertStatus.PENDING -> R.string.alerts_status_pending
    AlertStatus.ACKNOWLEDGED -> R.string.alerts_status_acknowledged
    AlertStatus.ESCALATED -> R.string.alerts_status_escalated
    AlertStatus.CLOSED -> R.string.alerts_status_closed
    AlertStatus.UNKNOWN -> R.string.alerts_status_unknown
}

@Composable
private fun alertDate(value: OffsetDateTime?): String = value?.atZoneSameInstant(ZoneId.systemDefault())
    ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) ?: stringResource(R.string.alerts_date_missing)
