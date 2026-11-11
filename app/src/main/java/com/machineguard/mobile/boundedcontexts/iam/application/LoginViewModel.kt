package com.machineguard.mobile.boundedcontexts.iam.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.CredentialsValidator
import com.machineguard.mobile.boundedcontexts.iam.domain.EmailError
import com.machineguard.mobile.boundedcontexts.iam.domain.PasswordError
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.data.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Motivo de un fallo al iniciar sesión, ya traducido a lo que el formulario necesita mostrar. */
enum class LoginFailure {
    InvalidCredentials,
    AccountDisabled,
    TooManyRequests,
    Network,
    Server,
    Unknown,
}

/** Traduce el error de la capa de datos al motivo que muestra el formulario de login. */
fun AppError.toLoginFailure(): LoginFailure = when (this) {
    AppError.InvalidCredentials -> LoginFailure.InvalidCredentials
    is AppError.Forbidden -> LoginFailure.AccountDisabled
    is AppError.TooManyRequests -> LoginFailure.TooManyRequests
    AppError.Offline, AppError.Timeout -> LoginFailure.Network
    is AppError.Server -> LoginFailure.Server
    else -> LoginFailure.Unknown
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: EmailError? = null,
    val passwordError: PasswordError? = null,
    /** Error devuelto por el servidor o la red tras enviar el formulario. */
    val failure: LoginFailure? = null,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
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
            when (val result = authRepository.login(current.email, current.password)) {
                // La sesión pasa a SignedIn y AuthGate muestra la app; se limpia el formulario.
                is DataResult.Success -> _state.value = LoginUiState()
                is DataResult.Failure -> _state.update {
                    it.copy(isLoading = false, failure = result.error.toLoginFailure())
                }
            }
        }
    }
}
