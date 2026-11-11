package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.machineguard.mobile.appContainer
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Alert
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertQuery
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertSeverity as DomainAlertSeverity
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertStatus as DomainAlertStatus
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertsRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.core.data.DataResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Carga el listado de alertas completo; los filtros Activas, Reconocidas y Todas se aplican en la pantalla. Sigue la
 * sesión igual que el dashboard.
 */
class AlertsViewModel(
    private val alertsRepository: AlertsRepository,
    session: StateFlow<SessionState>,
) : ViewModel() {
    private val _state = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            session.map { it is SessionState.SignedIn }.distinctUntilChanged().collect { signedIn ->
                if (signedIn) load() else reset()
            }
        }
    }

    /** Reintentar. */
    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        _state.value = AlertsUiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = when (val result = alertsRepository.getAlerts(AlertQuery())) {
                is DataResult.Success -> AlertsUiState.fromAlerts(result.data.map { it.toCardUiModel() })
                is DataResult.Failure -> AlertsUiState.Error
            }
        }
    }

    private fun reset() {
        loadJob?.cancel()
        _state.value = AlertsUiState.Loading
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).appContainer
                AlertsViewModel(container.alertsRepository, container.authRepository.session)
            }
        }
    }
}

/** Único punto que traduce la alerta del dominio a la proyección de la pantalla. */
internal fun Alert.toCardUiModel(): AlertCardUiModel = AlertCardUiModel(
    id = id,
    incidentId = incidentId,
    severity = severity.toUi(),
    message = message,
    status = status.toUi(),
    raisedAt = raisedAt,
    acknowledgedAt = acknowledgedAt,
    escalatedAt = escalatedAt,
)

private fun DomainAlertSeverity.toUi(): AlertSeverity = when (this) {
    DomainAlertSeverity.LOW -> AlertSeverity.LOW
    DomainAlertSeverity.MEDIUM -> AlertSeverity.MEDIUM
    DomainAlertSeverity.HIGH -> AlertSeverity.HIGH
    DomainAlertSeverity.CRITICAL -> AlertSeverity.CRITICAL
    DomainAlertSeverity.UNKNOWN -> AlertSeverity.UNKNOWN
}

private fun DomainAlertStatus.toUi(): AlertStatus = when (this) {
    DomainAlertStatus.PENDING -> AlertStatus.PENDING
    DomainAlertStatus.ACKNOWLEDGED -> AlertStatus.ACKNOWLEDGED
    DomainAlertStatus.ESCALATED -> AlertStatus.ESCALATED
    DomainAlertStatus.CLOSED -> AlertStatus.CLOSED
    DomainAlertStatus.UNKNOWN -> AlertStatus.UNKNOWN
}
