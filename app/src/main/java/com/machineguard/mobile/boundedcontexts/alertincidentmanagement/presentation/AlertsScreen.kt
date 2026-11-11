package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.machineguard.mobile.R
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.Factory),
) {
    val state by viewModel.state.collectAsState()
    AlertsContent(state = state, onRetry = viewModel::retry)
}

/** The caller owns loading and retry; only the local display filter is saved here. */
@Composable
fun AlertsContent(state: AlertsUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    var selectedFilter by rememberSaveable { mutableStateOf(AlertFilter.ACTIVE) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.alerts_title), modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.alerts_subtitle), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (state is AlertsUiState.Content || state == AlertsUiState.Empty) {
                item(key = "filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.alerts_filters), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AlertFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(stringResource(filter.labelResource())) },
                                )
                            }
                        }
                        if (selectedFilter == AlertFilter.ACTIVE) {
                            Text(stringResource(R.string.alerts_active_description), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            when (state) {
                AlertsUiState.Loading -> item(key = "loading") {
                    AlertsMessage(stringResource(R.string.alerts_loading)) { CircularProgressIndicator() }
                }
                AlertsUiState.Empty -> item(key = "empty") {
                    AlertsMessage(stringResource(R.string.alerts_empty))
                }
                AlertsUiState.Error -> item(key = "error") {
                    AlertsMessage(stringResource(R.string.alerts_error), stringResource(R.string.alerts_error_description)) {
                        Button(onClick = onRetry) { Text(stringResource(R.string.alerts_retry)) }
                    }
                }
                is AlertsUiState.Content -> {
                    val visible = selectedFilter.select(state.alerts)
                    item(key = "result") {
                        Text(stringResource(R.string.alerts_result_count, visible.size),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    if (visible.isEmpty()) {
                        item(key = "filtered-empty") { AlertsMessage(stringResource(R.string.alerts_filter_empty)) }
                    }
                    items(visible, key = { "alert:${it.id}" }) { AlertCard(it) }
                }
            }
        }
    }
}

private fun AlertFilter.labelResource(): Int = when (this) {
    AlertFilter.ACTIVE -> R.string.alerts_filter_active
    AlertFilter.ACKNOWLEDGED -> R.string.alerts_filter_acknowledged
    AlertFilter.ALL -> R.string.alerts_filter_all
}

@Composable
private fun AlertsMessage(title: String, description: String? = null, action: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) }
        action()
    }
}
