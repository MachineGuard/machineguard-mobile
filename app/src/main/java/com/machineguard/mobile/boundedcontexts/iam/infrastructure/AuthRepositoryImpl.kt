package com.machineguard.mobile.boundedcontexts.iam.infrastructure

import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.data.DataResult
import com.machineguard.mobile.core.network.ApiClient
import com.machineguard.mobile.core.network.ApiJson
import com.machineguard.mobile.core.network.ApiResult
import com.machineguard.mobile.core.session.AuthTokens
import com.machineguard.mobile.core.session.SessionManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerializationException
import java.time.Clock
import java.time.OffsetDateTime

class AuthRepositoryImpl(
    private val api: ApiClient,
    private val sessions: SessionManager,
    private val clock: Clock = Clock.systemUTC(),
) : AuthRepository {

    override val session: StateFlow<SessionState> get() = sessions.state

    override suspend fun login(email: String, password: String): DataResult<UserProfile> {
        val body = ApiJson.instance.encodeToString(LoginRequestDto.serializer(), LoginRequestDto(email.trim(), password))
        return when (val result = api.post(LOGIN, body, public = true, unauthorizedAs = AppError.InvalidCredentials)) {
            is ApiResult.Failure -> DataResult.Failure(result.error)
            is ApiResult.Success -> {
                val auth = decode(result.body, AuthenticationDto.serializer())
                    ?: return DataResult.Failure(AppError.Unknown("Respuesta de inicio de sesión no válida"))
                val profile = auth.user.toDomain(auth.organization?.name)
                val expiresAt = if (auth.expiresIn > 0) clock.instant().plusSeconds(auth.expiresIn) else null
                sessions.signIn(AuthTokens(auth.accessToken, auth.refreshToken, expiresAt), profile)
                DataResult.Success(profile, fetchedAt = OffsetDateTime.now(clock))
            }
        }
    }

    override suspend fun logout(): DataResult<Unit> {
        val refreshToken = sessions.tokens()?.refreshToken
        val result = if (refreshToken == null) {
            null
        } else {
            val body = ApiJson.instance.encodeToString(LogoutRequestDto.serializer(), LogoutRequestDto(refreshToken))
            api.post(LOGOUT, body)
        }
        // La sesión local se borra siempre, aunque el servidor no haya podido revocarla.
        sessions.signOut()
        return if (result is ApiResult.Failure) DataResult.Failure(result.error) else DataResult.Success(Unit)
    }

    override suspend fun refreshProfile(): DataResult<UserProfile> = when (val result = api.get(ME)) {
        is ApiResult.Failure -> DataResult.Failure(result.error)
        is ApiResult.Success -> {
            val user = decode(result.body, UserDto.serializer())
            if (user == null) {
                DataResult.Failure(AppError.Unknown("Respuesta del perfil no válida"))
            } else {
                val organizationName = (sessions.state.value as? SessionState.SignedIn)?.user?.organizationName
                val profile = user.toDomain(organizationName)
                sessions.updateProfile(profile)
                DataResult.Success(profile, fetchedAt = OffsetDateTime.now(clock))
            }
        }
    }

    private fun <T> decode(body: String, serializer: kotlinx.serialization.KSerializer<T>): T? = try {
        ApiJson.instance.decodeFromString(serializer, body)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private companion object {
        const val LOGIN = "/api/v1/auth/login"
        const val LOGOUT = "/api/v1/auth/logout"
        const val ME = "/api/v1/users/me"
    }
}
