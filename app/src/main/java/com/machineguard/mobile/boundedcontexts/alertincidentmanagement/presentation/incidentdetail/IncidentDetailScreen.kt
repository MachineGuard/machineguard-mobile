package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.machineguard.mobile.R
import androidx.compose.ui.unit.dp
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertSeverity
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentDetail
import com.machineguard.mobile.core.data.isRetryable
import com.machineguard.mobile.core.theme.MgColors
import java.time.OffsetDateTime

private enum class DialogKind { NONE, ACKNOWLEDGE, ACTION, RESOLVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(viewModel: IncidentDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var dialog by rememberSaveable { mutableStateOf(DialogKind.NONE) }

    val context = LocalContext.current
    LaunchedEffect(state.lastSuccess, state.actionFailure) {
        val failure = state.actionFailure?.error
        val message = state.lastSuccess?.let { context.getString(it.successRes()) }
            ?: failure?.let { it.serverReason() ?: context.getString(it.messageRes()) }
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.incident_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.incident_detail_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val detail = state.detail
            when {
                detail != null -> DetailContent(
                    detail = detail,
                    state = state,
                    onAcknowledge = { dialog = DialogKind.ACKNOWLEDGE },
                    onRegisterAction = { dialog = DialogKind.ACTION },
                    onResolve = { dialog = DialogKind.RESOLVE },
                )
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> {
                    val error = state.loadError
                    Column(
                        Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(error?.let { stringResource(it.messageRes()) } ?: stringResource(R.string.incident_detail_load_failed))
                        if (error == null || error.isRetryable) {
                            Button(onClick = viewModel::load) { Text(stringResource(R.string.alerts_retry)) }
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        DialogKind.ACKNOWLEDGE -> NoteDialog(
            title = stringResource(R.string.incident_dialog_ack_title),
            fieldLabel = stringResource(R.string.incident_dialog_note_optional),
            confirm = stringResource(R.string.incident_dialog_ack_confirm),
            required = false,
            maxLength = 500,
            onDismiss = { dialog = DialogKind.NONE },
            onConfirm = { note, _ ->
                dialog = DialogKind.NONE
                viewModel.acknowledge(note)
            },
        )
        DialogKind.ACTION -> NoteDialog(
            title = stringResource(R.string.incident_dialog_action_title),
            fieldLabel = stringResource(R.string.incident_dialog_action_description),
            confirm = stringResource(R.string.incident_dialog_action_confirm),
            required = true,
            maxLength = 1000,
            secondLabel = stringResource(R.string.incident_dialog_action_notes),
            onDismiss = { dialog = DialogKind.NONE },
            onConfirm = { description, notes ->
                dialog = DialogKind.NONE
                viewModel.registerCorrectiveAction(description, notes)
            },
        )
        DialogKind.RESOLVE -> NoteDialog(
            title = stringResource(R.string.incident_dialog_resolve_title),
            fieldLabel = stringResource(R.string.incident_dialog_resolve_notes),
            confirm = stringResource(R.string.incident_dialog_resolve_confirm),
            required = false,
            maxLength = 1000,
            onDismiss = { dialog = DialogKind.NONE },
            onConfirm = { notes, _ ->
                dialog = DialogKind.NONE
                viewModel.resolve(notes)
            },
        )
        DialogKind.NONE -> Unit
    }
}

@Composable
private fun DetailContent(
    detail: IncidentDetail,
    state: IncidentDetailUiState,
    onAcknowledge: () -> Unit,
    onRegisterAction: () -> Unit,
    onResolve: () -> Unit,
) {
    val now = remember(detail) { OffsetDateTime.now() }
    val severity = detail.alert?.severity ?: AlertSeverity.UNKNOWN
    val escalatedAt = detail.alert?.escalatedAt
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.fromCache) {
            Text(
                stringResource(R.string.incident_detail_from_cache),
                color = MgColors.Warn,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(12.dp).background(severity.color(), CircleShape))
                    Text(stringResource(R.string.alerts_severity, stringResource(severity.labelRes())), style = MaterialTheme.typography.titleMedium)
                }
                Text(detail.alert?.message ?: stringResource(R.string.incident_detail_zone_fallback, detail.incident.monitoringZoneId))
                Text(stringResource(R.string.incident_detail_status, stringResource(detail.incident.status.labelRes())))
                val end = detail.incident.resolvedAt ?: now
                Text(
                    stringResource(
                        R.string.incident_detail_elapsed,
                        elapsedLabel(detail.alert?.raisedAt ?: detail.incident.openedAt, end),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (escalatedAt != null) {
                    Text(stringResource(R.string.incident_detail_escalated_on, escalatedAt.label()), color = MgColors.Error)
                }
                if (detail.incident.historical) {
                    Text(
                        stringResource(R.string.incident_detail_historical),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Text(stringResource(R.string.incident_detail_timeline), style = MaterialTheme.typography.titleMedium)
        detail.timeline().forEach { TimelineRow(it) }

        if (state.canManage) {
            Text(stringResource(R.string.incident_detail_attention), style = MaterialTheme.typography.titleMedium)
            Button(onClick = onAcknowledge, enabled = state.canAcknowledge, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.incident_action_acknowledge))
            }
            OutlinedButton(
                onClick = onRegisterAction,
                enabled = state.canRegisterAction,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.incident_action_register))
            }
            Button(onClick = onResolve, enabled = state.canResolve, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.incident_action_resolve))
            }
            if (!state.canResolve && detail.incident.status.isActive) {
                detail.resolutionBlockReasons.forEach {
                    Text("• ${stringResource(blockReasonRes(it))}", style = MaterialTheme.typography.bodySmall, color = MgColors.Slate)
                }
            }
        } else {
            Text(stringResource(R.string.incident_detail_read_only), style = MaterialTheme.typography.bodySmall, color = MgColors.Slate)
        }
    }
}

@Composable
private fun TimelineRow(step: TimelineStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.padding(top = 4.dp).size(14.dp)
                .background(if (step.done) MgColors.Ok else MgColors.Offline, CircleShape),
        )
        Column {
            Text(stringResource(step.kind.titleRes()), style = MaterialTheme.typography.bodyLarge)
            step.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                if (step.done) step.at.label() else stringResource(R.string.incident_detail_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MgColors.Slate,
            )
        }
    }
}

@Composable
private fun NoteDialog(
    title: String,
    fieldLabel: String,
    confirm: String,
    required: Boolean,
    maxLength: Int,
    secondLabel: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var first by rememberSaveable { mutableStateOf("") }
    var second by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it.take(maxLength) },
                    label = { Text(fieldLabel) },
                    supportingText = { Text("${first.length}/$maxLength") },
                )
                if (secondLabel != null) {
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it.take(1000) },
                        label = { Text(secondLabel) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(first, second.ifBlank { null }) },
                enabled = !required || first.isNotBlank(),
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.incident_dialog_cancel)) } },
    )
}

private fun AlertSeverity.color(): Color = when (this) {
    AlertSeverity.CRITICAL, AlertSeverity.HIGH -> MgColors.Error
    AlertSeverity.MEDIUM -> MgColors.Warn
    AlertSeverity.LOW -> MgColors.Ok
    AlertSeverity.UNKNOWN -> MgColors.Offline
}
