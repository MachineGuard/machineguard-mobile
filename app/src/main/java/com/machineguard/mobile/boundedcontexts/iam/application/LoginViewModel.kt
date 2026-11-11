package com.machineguard.mobile.boundedcontexts.iam.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthFailure
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthResult
import com.machineguard.mobile.boundedcontexts.iam.domain.CredentialsValidator
import com.machineguard.mobile.boundedcontexts.iam.domain.EmailError
import com.machineguard.mobile.boundedcontexts.iam.domain.PasswordError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: EmailError? = null,
    val passwordError: PasswordError? = null,
    /** Error devuelto por el servidor o la red tras enviar el formulario. */
    val failure: AuthFailure? = null,
)

class LoginViewModel(private val sessionManager: SessionManager) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, failure = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordError = null, failure = null) }
    }

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submit() {
        val current = _state.value
        if (current.isLoading) return

        val emailError = CredentialsValidator.validateEmail(current.email)
        val passwordError = CredentialsValidator.validatePassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(isLoading = true, failure = null) }
        viewModelScope.launch {
            when (val result = sessionManager.login(current.email, current.password)) {
                // La sesión pasa a SignedIn y AuthGate muestra la app; se limpia el formulario.
                is AuthResult.Success -> _state.value = LoginUiState()
                is AuthResult.Failure -> _state.update {
                    it.copy(isLoading = false, failure = result.reason)
                }
            }
        }
    }
}
