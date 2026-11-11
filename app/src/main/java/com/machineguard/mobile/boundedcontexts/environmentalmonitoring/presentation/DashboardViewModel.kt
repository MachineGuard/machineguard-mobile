package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.machineguard.mobile.appContainer
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Measurement
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Zone
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZoneCondition as DomainZoneCondition
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZonesRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.core.data.DataResult
import com.machineguard.mobile.shared.domain.EnvironmentalVariable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Carga las zonas del dashboard. Sigue la sesión: se carga al iniciar sesión y vuelve a Loading al cerrarla, para no
 * mostrar lecturas de un usuario anterior.
 */
class DashboardViewModel(
    private val zonesRepository: ZonesRepository,
    session: StateFlow<SessionState>,
) : ViewModel() {
    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            session.map { it is SessionState.SignedIn }.distinctUntilChanged().collect { signedIn ->
                if (signedIn) load() else reset()
            }
        }
    }

    /** Reintentar. Vuelve a consultar el servidor; si no hay conexión, el repositorio devuelve la copia guardada. */
    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        _state.value = DashboardUiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = when (val result = zonesRepository.getZones()) {
                is DataResult.Success -> DashboardUiState.fromZones(result.data.map { it.toCardUiModel() })
                is DataResult.Failure -> DashboardUiState.Error
            }
        }
    }

    private fun reset() {
        loadJob?.cancel()
        _state.value = DashboardUiState.Loading
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).appContainer
                DashboardViewModel(container.zonesRepository, container.authRepository.session)
            }
        }
    }
}

/** Único punto que traduce la zona del dominio a la proyección de la pantalla. */
internal fun Zone.toCardUiModel(): ZoneCardUiModel = ZoneCardUiModel(
    id = id,
    name = name,
    description = description,
    condition = condition.toUi(),
    sensorsOnline = sensorsOnline,
    sensorsOffline = sensorsOffline,
    sensorsInactive = sensorsInactive,
    lastUpdatedAt = lastUpdatedAt,
    readings = latestMeasurements.map { it.toReadingUiModel(this) },
)

private fun Measurement.toReadingUiModel(zone: Zone): PointReadingUiModel = PointReadingUiModel(
    pointName = zone.pointName(pointId) ?: "Punto sin nombre",
    variable = variable.toUi(),
    value = value,
    recordedAt = recordedAt,
)

/** UNKNOWN (valor que esta versión no conoce) se muestra como Sin datos, como indica el dominio. */
private fun DomainZoneCondition.toUi(): ZoneCondition = when (this) {
    DomainZoneCondition.OUT_OF_RANGE -> ZoneCondition.OUT_OF_RANGE
    DomainZoneCondition.STALE -> ZoneCondition.STALE
    DomainZoneCondition.NO_DATA, DomainZoneCondition.UNKNOWN -> ZoneCondition.NO_DATA
    DomainZoneCondition.NORMAL -> ZoneCondition.NORMAL
}

private fun EnvironmentalVariable.toUi(): ReadingVariable = when (this) {
    EnvironmentalVariable.TEMPERATURE -> ReadingVariable.TEMPERATURE
    EnvironmentalVariable.HUMIDITY -> ReadingVariable.HUMIDITY
}
