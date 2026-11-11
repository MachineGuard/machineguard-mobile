package com.machineguard.mobile.core.session

import java.time.Instant

/**
 * Tokens de la sesión. [refreshToken] rota en cada renovación: el servidor invalida el anterior y, si se reutiliza uno
 * ya usado, revoca la sesión completa. Por eso solo [SessionManager] los renueva y lo hace de a una petición a la vez.
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    /** Cuándo vence el access token; informativo (la renovación se dispara al recibir un 401). */
    val accessExpiresAt: Instant?,
)

/**
 * Almacenamiento persistente de la sesión. La implementación real cifra todo con el Keystore de Android
 * ([EncryptedSessionStorage]); en las pruebas se usa una implementación en memoria.
 *
 * Los métodos son síncronos porque los usa el [okhttp3.Authenticator], que ya corre fuera del hilo principal.
 */
interface SessionStorage {
    fun readTokens(): AuthTokens?

    fun writeTokens(tokens: AuthTokens)

    /** Perfil del usuario en JSON, para abrir la app sin conexión sin pedirlo otra vez. */
    fun readProfileJson(): String?

    fun writeProfileJson(json: String)

    /** Borra tokens y perfil. */
    fun clear()
}
