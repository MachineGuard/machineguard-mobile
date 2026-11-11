package com.machineguard.mobile.boundedcontexts.iam.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Expone la sesión a la interfaz y atiende el cierre de sesión desde Settings. */
class SessionViewModel(private val sessionManager: SessionManager) : ViewModel() {
    val state: StateFlow<SessionState> = sessionManager.state

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    init {
        viewModelScope.launch { sessionManager.restore() }
    }

    fun signOut() {
        if (_isSigningOut.value) return
        viewModelScope.launch {
            _isSigningOut.value = true
            try {
                sessionManager.logout()
            } finally {
                _isSigningOut.value = false
            }
        }
    }
}
