package com.machineguard.mobile.boundedcontexts.iam.domain

import com.machineguard.mobile.core.data.DataResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Sesión y perfil del usuario. Es el único punto que conoce las credenciales: los demás repositorios usan el token
 * de forma transparente y, si la sesión termina, esta interfaz lo notifica en [session].
 *
 * Contrato con el Core API: `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/users/me`.
 */
interface AuthRepository {

    /**
     * Estado de la sesión, siempre disponible. Cambia a [SessionState.SignedOut] con `expired = true` cuando la
     * renovación de sesión falla en cualquier petición, sin que la pantalla tenga que hacer nada.
     */
    val session: StateFlow<SessionState>

    /**
     * Inicia sesión. Errores posibles: `InvalidCredentials`, `TooManyRequests`, `Offline`, `Timeout`, `Invalid`.
     * Al terminar bien, [session] pasa a `SignedIn` y los tokens quedan guardados en el almacenamiento seguro.
     */
    suspend fun login(email: String, password: String): DataResult<UserProfile>

    /**
     * Cierra la sesión. Revoca la renovación en el servidor si hay conexión, pero **siempre** borra la sesión local y
     * la caché de datos, aunque la red falle; por eso el resultado normalmente se puede ignorar.
     */
    suspend fun logout(): DataResult<Unit>

    /** Vuelve a leer el perfil del servidor (`GET /api/v1/users/me`) y actualiza [session]. */
    suspend fun refreshProfile(): DataResult<UserProfile>
}
