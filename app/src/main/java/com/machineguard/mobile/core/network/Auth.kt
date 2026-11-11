package com.machineguard.mobile.core.network

import com.machineguard.mobile.core.session.AuthTokens
import com.machineguard.mobile.core.session.RefreshOutcome
import com.machineguard.mobile.core.session.SessionManager
import com.machineguard.mobile.core.session.TokenRefresher
import kotlinx.serialization.Serializable
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.time.Clock

/**
 * Marca las peticiones que no llevan sesión (login y refresh): no se les agrega el token y un 401 no dispara la
 * renovación (un 401 en el login significa credenciales incorrectas).
 */
object PublicRequest

/** Agrega `Authorization: Bearer <access token>` a toda petición que no sea [PublicRequest]. */
class AuthInterceptor(private val sessions: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.tag(PublicRequest::class.java) != null) return chain.proceed(request)
        val tokens = sessions.tokens() ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer ${tokens.accessToken}").build())
    }
}

/**
 * Ante un 401 renueva la sesión una vez y reintenta la petición con el token nuevo. Si no se puede renovar devuelve
 * `null` y el 401 llega al repositorio, que ya encuentra la sesión cerrada ([SessionManager.expire]).
 *
 * Si la renovación falla por falta de red, la [IOException] se propaga: el llamador la ve como "sin conexión" y la
 * sesión se conserva para cuando vuelva la red.
 */
class TokenAuthenticator(
    private val sessions: SessionManager,
    private val refresher: TokenRefresher,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.tag(PublicRequest::class.java) != null) return null
        if (attempts(response) >= MAX_ATTEMPTS) return null
        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
        val fresh = sessions.refreshBlocking(failedToken, refresher) ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
    }

    private fun attempts(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}

/**
 * Renueva los tokens con `POST /api/v1/auth/refresh`. Usa un cliente **sin** el authenticator para no entrar en bucle.
 * Un 400, 401 o 403 significa que el refresh token ya no sirve. Cualquier otra cosa (5xx, 429, corte de red) se trata
 * como indisponibilidad ([IOException]), no como sesión vencida, para no cerrarle la sesión a alguien porque el
 * servidor se cayó o se saturó.
 */
class HttpTokenRefresher(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val clock: Clock = Clock.systemUTC(),
) : TokenRefresher {

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    private data class RefreshResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long = 0)

    override fun refresh(refreshToken: String): RefreshOutcome {
        val body = ApiJson.instance.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/v1/auth/refresh")
            .post(body)
            .tag(PublicRequest::class.java, PublicRequest)
            .build()
        client.newCall(request).execute().use { response ->
            val code = response.code
            return when {
                response.isSuccessful -> {
                    val text = response.body?.string().orEmpty()
                    val parsed = try {
                        ApiJson.instance.decodeFromString(RefreshResponse.serializer(), text)
                    } catch (e: kotlinx.serialization.SerializationException) {
                        throw IOException("Respuesta de renovación ilegible", e)
                    }
                    val expiresAt = if (parsed.expiresIn > 0) clock.instant().plusSeconds(parsed.expiresIn) else null
                    RefreshOutcome.Refreshed(AuthTokens(parsed.accessToken, parsed.refreshToken, expiresAt))
                }
                code == 400 || code == 401 || code == 403 -> RefreshOutcome.Rejected
                else -> throw IOException("El servidor no pudo renovar la sesión (HTTP $code)")
            }
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
