package com.machineguard.mobile.boundedcontexts.iam.application

import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Fuente única de verdad de la sesión. La navegación (AuthGate) observa [state].
 *
 * Si la capa de datos detecta que la sesión ya no se puede renovar (refresh token
 * vencido o revocado), debe llamar a [onSessionExpired].
 */
class SessionManager(private val repository: AuthRepository) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** Reabre la sesión guardada. Solo actúa mientras se está comprobando. */
    suspend fun restore() {
        if (_state.value !is SessionState.Checking) return
        val profile = try {
            repository.restoreSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        _state.value = if (profile != null) SessionState.SignedIn(profile) else SessionState.SignedOut
    }

    suspend fun login(email: String, password: String): AuthResult {
        val result = repository.login(email.trim(), password)
        if (result is AuthResult.Success) {
            _state.value = SessionState.SignedIn(result.profile)
        }
        return result
    }

    /** Cierra la sesión local aunque el servidor no responda. */
    suspend fun logout() {
        try {
            repository.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // La sesión local se cierra de todos modos.
        }
        _state.value = SessionState.SignedOut
    }

    fun onSessionExpired() {
        _state.value = SessionState.SignedOut
    }
}
