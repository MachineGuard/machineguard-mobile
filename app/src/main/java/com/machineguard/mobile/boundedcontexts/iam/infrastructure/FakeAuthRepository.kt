package com.machineguard.mobile.boundedcontexts.iam.infrastructure

import com.machineguard.mobile.boundedcontexts.iam.domain.AuthFailure
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthResult
import com.machineguard.mobile.boundedcontexts.iam.domain.Role
import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import kotlinx.coroutines.delay

/**
 * Implementación de prueba para desarrollar las pantallas sin el Core API.
 *
 * - Credenciales válidas: [DEMO_EMAIL] / [DEMO_PASSWORD].
 * - [NETWORK_ERROR_EMAIL] simula una caída de red (útil para capturar ese estado).
 * - Cualquier otra combinación devuelve credenciales inválidas.
 */
class FakeAuthRepository(private val latencyMillis: Long = 600) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResult {
        delay(latencyMillis)
        return when {
            email.equals(NETWORK_ERROR_EMAIL, ignoreCase = true) ->
                AuthResult.Failure(AuthFailure.Network)
            email.equals(DEMO_EMAIL, ignoreCase = true) && password == DEMO_PASSWORD ->
                AuthResult.Success(DEMO_PROFILE)
            else -> AuthResult.Failure(AuthFailure.InvalidCredentials)
        }
    }

    override suspend fun logout() {
        delay(latencyMillis / 2)
    }

    /** Sin sesión guardada: la app siempre arranca en el login. */
    override suspend fun restoreSession(): UserProfile? {
        delay(latencyMillis / 2)
        return null
    }

    companion object {
        const val DEMO_EMAIL = "demo@machineguard.com"
        const val DEMO_PASSWORD = "Demo1234!"
        const val NETWORK_ERROR_EMAIL = "network@machineguard.com"

        val DEMO_PROFILE = UserProfile(
            fullName = "Demo Warehouse Manager",
            email = DEMO_EMAIL,
            role = Role.ADMIN,
        )
    }
}
