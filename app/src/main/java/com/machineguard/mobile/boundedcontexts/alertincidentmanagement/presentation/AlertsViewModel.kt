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
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZonesRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.core.data.DataResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Carga el listado de alertas completo; los filtros Activas, Reconocidas y Todas se aplican en la pantalla. Sigue la
 * sesión igual que el dashboard. Si recibe [zonesRepository], muestra el nombre de la zona en lugar de su UUID.
 */
class AlertsViewModel(
    private val alertsRepository: AlertsRepository,
    session: StateFlow<SessionState>,
    private val zonesRepository: ZonesRepository? = null,
) : ViewModel() {
    private val _state = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null
    private var signedIn = false

    init {
        viewModelScope.launch {
            session.map { it is SessionState.SignedIn }.distinctUntilChanged().collect { signedIn ->
                this@AlertsViewModel.signedIn = signedIn
                if (signedIn) load() else reset()
            }
        }
    }

    /** Reintentar. */
    fun retry() = load()

    /**
     * Actualizar al volver a la pestaña o al deslizar hacia abajo. Mantiene las alertas visibles mientras consulta y
     * no hace nada si no hay sesión o ya hay una consulta en curso.
     */
    fun refresh() {
        if (!signedIn || loadJob?.isActive == true) return
        if (_state.value !is AlertsUiState.Content) return load()
        _isRefreshing.value = true
        loadJob = viewModelScope.launch {
            fetch()?.let { _state.value = it }
            _isRefreshing.value = false
        }
    }

    private fun load() {
        loadJob?.cancel()
        _isRefreshing.value = false
        _state.value = AlertsUiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = fetch() ?: AlertsUiState.Error
        }
    }

    /** Devuelve null si falló la consulta de alertas. */
    private suspend fun fetch(): AlertsUiState? = coroutineScope {
        val names = async { zoneNames() }
        when (val result = alertsRepository.getAlerts(AlertQuery())) {
            is DataResult.Success -> {
                val zoneNames = names.await()
                AlertsUiState.fromAlerts(result.data.map { it.toCardUiModel(zoneNames) })
            }
            is DataResult.Failure -> {
                names.cancel()
                null
            }
        }
    }

    /** Si las zonas no cargan, las alertas se muestran igual con el texto original. */
    private suspend fun zoneNames(): Map<String, String> =
        when (val result = zonesRepository?.getZones()) {
            is DataResult.Success -> result.data.associate { it.id to it.name }
            else -> emptyMap()
        }

    private fun reset() {
        loadJob?.cancel()
        _isRefreshing.value = false
        _state.value = AlertsUiState.Loading
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).appContainer
                AlertsViewModel(container.alertsRepository, container.authRepository.session, container.zonesRepository)
            }
        }
    }
}

/** Único punto que traduce la alerta del dominio a la proyección de la pantalla. */
internal fun Alert.toCardUiModel(zoneNames: Map<String, String> = emptyMap()): AlertCardUiModel = AlertCardUiModel(
    id = id,
    incidentId = incidentId,
    severity = severity.toUi(),
    // El Core escribe el UUID de la zona dentro del mensaje; se reemplaza por su nombre cuando se conoce.
    message = zoneNames[monitoringZoneId]?.let { message.replace(monitoringZoneId, it) } ?: message,
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
