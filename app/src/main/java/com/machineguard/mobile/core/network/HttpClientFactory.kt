package com.machineguard.mobile.core.network

import com.machineguard.mobile.core.session.SessionManager
import okhttp3.OkHttpClient
import java.time.Clock
import java.util.concurrent.TimeUnit

/** Cliente HTTP de la app. No registra el contenido de las peticiones: llevan tokens y contraseñas. */
object HttpClientFactory {

    fun create(baseUrl: String, sessions: SessionManager, clock: Clock = Clock.systemUTC()): OkHttpClient {
        val plain = base().build()
        val refresher = HttpTokenRefresher(plain, baseUrl, clock)
        return plain.newBuilder()
            .addInterceptor(AuthInterceptor(sessions))
            .authenticator(TokenAuthenticator(sessions, refresher))
            .build()
    }

    private fun base(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        // Sin reintentos silenciosos: una escritura (reconocer, resolver) no debe enviarse dos veces sin que el
        // usuario lo decida.
        .retryOnConnectionFailure(false)
}
