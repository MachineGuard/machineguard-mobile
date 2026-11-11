package com.machineguard.mobile.boundedcontexts.iam.infrastructure

import com.machineguard.mobile.boundedcontexts.iam.domain.AuthFailure
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthResult
import com.machineguard.mobile.boundedcontexts.iam.domain.Role
import com.machineguard.mobile.boundedcontexts.iam.domain.TokenStore
import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// ---- DTOs --------------------------------------------------------------------------
// IMPORTANTE: los nombres de campo salen del Capítulo IV, pero la documentación no fija
// el JSON exacto. Confirmarlos con quien tenga el Core API (Pedro Lecca) y ajustar aquí.

@Serializable
private data class LoginRequestDto(val email: String, val password: String)

@Serializable
private data class AuthResponseDto(val accessToken: String, val refreshToken: String)

@Serializable
private data class LogoutRequestDto(val refreshToken: String)

@Serializable
private data class UserDto(
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
)

private fun UserDto.toDomain() = UserProfile(
    fullName = fullName,
    email = email,
    role = Role.fromApi(role),
)

private fun Int.toFailure(): AuthFailure = when (this) {
    401 -> AuthFailure.InvalidCredentials
    403 -> AuthFailure.AccountDisabled
    in 500..599 -> AuthFailure.Server
    else -> AuthFailure.Unknown
}

/**
 * Autenticación contra el Core API.
 *
 * Pendiente con `feat/data-layer`: cuando el access token vence, `restoreSession` debería
 * usar `POST /api/v1/auth/refresh` antes de rendirse. Si el cliente HTTP compartido de
 * Diego ya existe, se puede reemplazar [client] por ese cliente.
 */
class CoreApiAuthRepository(
    baseUrl: String,
    private val tokenStore: TokenStore,
    private val client: OkHttpClient = OkHttpClient(),
) : AuthRepository {

    private val baseUrl = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = json.encodeToString(LoginRequestDto.serializer(), LoginRequestDto(email, password))
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/login")
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val failure: AuthResult.Failure? = client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val auth = json.decodeFromString(
                            AuthResponseDto.serializer(),
                            response.body?.string().orEmpty(),
                        )
                        tokenStore.save(auth.accessToken, auth.refreshToken)
                        null
                    } else {
                        AuthResult.Failure(response.code.toFailure())
                    }
                }
                failure ?: fetchProfile()
            } catch (e: IOException) {
                AuthResult.Failure(AuthFailure.Network)
            } catch (e: IllegalArgumentException) {
                // Incluye SerializationException: el JSON no tiene el formato esperado.
                AuthResult.Failure(AuthFailure.Server)
            }
        }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                val access = tokenStore.accessToken()
                val refresh = tokenStore.refreshToken()
                if (access != null && refresh != null) {
                    val payload = json.encodeToString(LogoutRequestDto.serializer(), LogoutRequestDto(refresh))
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/auth/logout")
                        .header("Authorization", "Bearer $access")
                        .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                    client.newCall(request).execute().close()
                }
            } catch (e: IOException) {
                // Sin red: igual se cierra la sesión local.
            } finally {
                tokenStore.clear()
            }
        }
    }

    override suspend fun restoreSession(): UserProfile? = withContext(Dispatchers.IO) {
        if (tokenStore.accessToken() == null) return@withContext null
        try {
            when (val result = fetchProfile()) {
                is AuthResult.Success -> result.profile
                is AuthResult.Failure -> {
                    // 401: token vencido. TODO(feat/data-layer): intentar /auth/refresh aquí.
                    if (result.reason == AuthFailure.InvalidCredentials) tokenStore.clear()
                    null
                }
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** `GET /api/v1/users/me` con el access token guardado. Se ejecuta en un hilo de IO. */
    private fun fetchProfile(): AuthResult {
        val token = tokenStore.accessToken() ?: return AuthResult.Failure(AuthFailure.Unknown)
        val request = Request.Builder()
            .url("$baseUrl/api/v1/users/me")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val user = json.decodeFromString(UserDto.serializer(), response.body?.string().orEmpty())
                AuthResult.Success(user.toDomain())
            } else {
                AuthResult.Failure(response.code.toFailure())
            }
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
