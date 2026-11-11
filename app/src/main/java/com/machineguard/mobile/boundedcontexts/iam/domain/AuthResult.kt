package com.machineguard.mobile.boundedcontexts.iam.domain

/** Motivos por los que puede fallar un inicio de sesión. */
enum class AuthFailure {
    /** 401: correo o contraseña incorrectos. */
    InvalidCredentials,

    /** 403: usuario u organización inactivos. */
    AccountDisabled,

    /** Sin conexión o tiempo de espera agotado. */
    Network,

    /** 5xx o respuesta con formato inesperado. */
    Server,

    Unknown,
}

sealed interface AuthResult {
    data class Success(val profile: UserProfile) : AuthResult
    data class Failure(val reason: AuthFailure) : AuthResult
}
