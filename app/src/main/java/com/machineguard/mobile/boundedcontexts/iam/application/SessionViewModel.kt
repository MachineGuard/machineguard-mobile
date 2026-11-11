package com.machineguard.mobile.boundedcontexts.iam.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Expone la sesión a la interfaz y atiende el cierre de sesión desde Settings.
 * La restauración de la sesión guardada la inicia `AppContainer.start`, no este ViewModel.
 */
class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val state: StateFlow<SessionState> = authRepository.session

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    fun signOut() {
        if (_isSigningOut.value) return
        viewModelScope.launch {
            _isSigningOut.value = true
            try {
                // logout() cierra la sesión local aunque el servidor no responda; el resultado no hace falta.
                authRepository.logout()
            } finally {
                _isSigningOut.value = false
            }
        }
    }
}
