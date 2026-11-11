package com.machineguard.mobile.boundedcontexts.iam.domain

/**
 * Contrato de autenticación. Las pantallas y el estado de sesión dependen de esta
 * interfaz; la implementación real (Core API) o de prueba se elige en `IamGraph`.
 */
interface AuthRepository {
    /** `POST /api/v1/auth/login` y luego `GET /api/v1/users/me`. */
    suspend fun login(email: String, password: String): AuthResult

    /** `POST /api/v1/auth/logout`. Siempre debe dejar la sesión local cerrada, aunque falle la red. */
    suspend fun logout()

    /** Intenta reabrir la sesión guardada en el dispositivo. Devuelve `null` si no hay una válida. */
    suspend fun restoreSession(): UserProfile?
}
