package com.machineguard.mobile.core.session

import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import com.machineguard.mobile.boundedcontexts.iam.domain.UserRole
import com.machineguard.mobile.core.network.ApiJson
import com.machineguard.mobile.core.network.parseApiDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.IOException

/** Resultado de intentar renovar los tokens. */
sealed interface RefreshOutcome {
    data class Refreshed(val tokens: AuthTokens) : RefreshOutcome

    /** El servidor rechazó el refresh token (vencido, ya usado o revocado): la sesión ya no es recuperable. */
    data object Rejected : RefreshOutcome
}

/** Renueva los tokens contra el Core API. Debe lanzar [IOException] si no pudo comunicarse con el servidor. */
fun interface TokenRefresher {
    @Throws(IOException::class)
    fun refresh(refreshToken: String): RefreshOutcome
}

/**
 * Dueño de la sesión del dispositivo: guarda los tokens, expone el [state] y renueva la sesión.
 *
 * La renovación está serializada con un candado. El servidor rota el refresh token en cada uso y revoca toda la sesión
 * si ve reutilizar uno viejo; si dos peticiones recibieran un 401 a la vez y ambas renovaran, la segunda quemaría la
 * sesión. Con el candado, la segunda petición encuentra el token ya renovado por la primera y simplemente lo usa.
 */
class SessionManager(
    private val storage: SessionStorage,
    /** Se invoca al terminar la sesión (cierre voluntario o vencida) para borrar los datos en caché del usuario. */
    private val onSessionCleared: () -> Unit = {},
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val lock = Any()

    /** Lee el almacenamiento seguro y fija el estado inicial. Hay que llamarlo una vez, fuera del hilo principal. */
    fun restore() {
        synchronized(lock) {
            val tokens = storage.readTokens()
            val profile = storage.readProfileJson()?.let(::decodeProfile)
            _state.value = if (tokens != null && profile != null) {
                SessionState.SignedIn(profile)
            } else {
                if (tokens != null || profile != null) storage.clear()
                SessionState.SignedOut()
            }
        }
    }

    fun tokens(): AuthTokens? = storage.readTokens()

    fun signIn(tokens: AuthTokens, profile: UserProfile) {
        synchronized(lock) {
            storage.writeTokens(tokens)
            storage.writeProfileJson(encodeProfile(profile))
            _state.value = SessionState.SignedIn(profile)
        }
    }

    fun updateProfile(profile: UserProfile) {
        synchronized(lock) {
            if (_state.value !is SessionState.SignedIn) return
            storage.writeProfileJson(encodeProfile(profile))
            _state.value = SessionState.SignedIn(profile)
        }
    }

    /** Cierre voluntario: borra la sesión local y los datos en caché. */
    fun signOut() = end(expired = false)

    /** La sesión ya no es recuperable: igual que [signOut] pero avisa al login de que venció. */
    fun expire() = end(expired = true)

    private fun end(expired: Boolean) {
        synchronized(lock) {
            storage.clear()
            _state.value = SessionState.SignedOut(expired)
        }
        onSessionCleared()
    }

    /**
     * Devuelve un access token válido tras un 401, o `null` si la sesión terminó. Llamado por el
     * [okhttp3.Authenticator], de forma bloqueante.
     *
     * @param failedAccessToken el token con el que se hizo la petición que recibió el 401.
     * @throws IOException si no hubo conexión para renovar: el llamador debe verlo como falta de red, no como
     * sesión vencida.
     */
    @Throws(IOException::class)
    fun refreshBlocking(failedAccessToken: String?, refresher: TokenRefresher): String? {
        synchronized(lock) {
            val current = storage.readTokens() ?: return null
            // Otra petición ya renovó mientras esta esperaba el candado.
            if (failedAccessToken != null && current.accessToken != failedAccessToken) return current.accessToken
            val outcome = refresher.refresh(current.refreshToken)
            if (outcome is RefreshOutcome.Refreshed) {
                storage.writeTokens(outcome.tokens)
                return outcome.tokens.accessToken
            }
        }
        expire()
        return null
    }

    @Serializable
    private data class StoredProfile(
        val id: String,
        val organizationId: String,
        val organizationName: String? = null,
        val email: String,
        val fullName: String,
        val role: String,
        val lastLoginAt: String? = null,
    )

    private fun encodeProfile(profile: UserProfile): String = ApiJson.instance.encodeToString(
        StoredProfile(
            profile.id, profile.organizationId, profile.organizationName, profile.email, profile.fullName,
            profile.role.name, profile.lastLoginAt?.toString(),
        ),
    )

    private fun decodeProfile(json: String): UserProfile? = try {
        val stored = ApiJson.instance.decodeFromString<StoredProfile>(json)
        UserProfile(
            stored.id, stored.organizationId, stored.organizationName, stored.email, stored.fullName,
            UserRole.fromApi(stored.role), parseApiDate(stored.lastLoginAt),
        )
    } catch (e: kotlinx.serialization.SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
