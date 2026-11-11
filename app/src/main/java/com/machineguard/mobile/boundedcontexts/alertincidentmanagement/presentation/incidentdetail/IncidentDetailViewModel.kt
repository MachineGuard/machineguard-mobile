package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentsRepository
import com.machineguard.mobile.core.data.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Detalle de una alerta y atención del incidente (reconocer, registrar acción correctiva y resolver).
 * Solo depende de [IncidentsRepository]; la app nunca envía quién actúa, el servidor usa el token.
 */
class IncidentDetailViewModel(
    private val incidentId: String,
    private val incidents: IncidentsRepository,
    canManage: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(IncidentDetailUiState(canManage = canManage))
    val state: StateFlow<IncidentDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch { fetch() }
    }

    fun dismissMessage() = _state.update { it.copy(actionFailure = null, lastSuccess = null) }

    fun acknowledge(note: String?) {
        val alertId = _state.value.detail?.alert?.id ?: return
        if (!_state.value.canAcknowledge) return
        submit(ActionKind.ACKNOWLEDGE) { incidents.acknowledgeAlert(alertId, note.clean()) }
    }

    fun registerCorrectiveAction(description: String, notes: String?) {
        val text = description.trim()
        if (text.isEmpty() || !_state.value.canRegisterAction) return
        submit(ActionKind.CORRECTIVE_ACTION) { incidents.registerCorrectiveAction(incidentId, text, notes.clean()) }
    }

    fun resolve(notes: String?) {
        if (!_state.value.canResolve) return
        submit(ActionKind.RESOLVE) { incidents.resolveIncident(incidentId, notes.clean()) }
    }

    private fun submit(action: ActionKind, call: suspend () -> DataResult<*>) {
        _state.update { it.copy(submitting = true, actionFailure = null, lastSuccess = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is DataResult.Success -> {
                    fetch()
                    _state.update { it.copy(submitting = false, lastSuccess = action) }
                }
                is DataResult.Failure -> {
                    // Un 409 suele significar que otro usuario ya actuó: se recarga para mostrar el estado real.
                    _state.update { it.copy(submitting = false, actionFailure = ActionFailure(action, result.error)) }
                    fetch()
                }
            }
        }
    }

    private suspend fun fetch() {
        when (val result = incidents.getIncident(incidentId)) {
            is DataResult.Success -> _state.update {
                it.copy(loading = false, detail = result.data, loadError = null, fromCache = result.isFromCache)
            }
            is DataResult.Failure -> _state.update {
                // Si ya hay datos en pantalla se conservan; el error solo se muestra cuando no hay nada.
                it.copy(loading = false, loadError = if (it.detail == null) result.error else null)
            }
        }
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
