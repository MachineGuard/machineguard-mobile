package com.machineguard.mobile.core

import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import com.machineguard.mobile.boundedcontexts.iam.domain.UserRole
import com.machineguard.mobile.core.cache.FileLocalCache
import com.machineguard.mobile.core.data.RemoteSource
import com.machineguard.mobile.core.network.ApiClient
import com.machineguard.mobile.core.network.HttpClientFactory
import com.machineguard.mobile.core.session.AuthTokens
import com.machineguard.mobile.core.session.SessionManager
import com.machineguard.mobile.core.session.SessionStorage
import okhttp3.mockwebserver.MockWebServer
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

val FIXED_NOW: Instant = Instant.parse("2026-11-10T12:00:00Z")
val FIXED_CLOCK: Clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
val FIXED_OFFSET_NOW: OffsetDateTime = OffsetDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC)

class InMemorySessionStorage : SessionStorage {
    var tokens: AuthTokens? = null
    var profileJson: String? = null

    override fun readTokens() = tokens

    override fun writeTokens(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override fun readProfileJson() = profileJson

    override fun writeProfileJson(json: String) {
        profileJson = json
    }

    override fun clear() {
        tokens = null
        profileJson = null
    }
}

val SAMPLE_PROFILE = UserProfile(
    id = "u-1",
    organizationId = "o-1",
    organizationName = "Cadena de Frío SAC",
    email = "ana@example.com",
    fullName = "Ana Torres",
    role = UserRole.ADMIN,
    lastLoginAt = null,
)

/** Todo lo que necesita una prueba de la capa de datos, conectado a un [MockWebServer]. */
class DataLayerEnv(val server: MockWebServer, cacheDir: File) {
    val storage = InMemorySessionStorage()
    val cleared = AtomicInteger()
    val cache = FileLocalCache(cacheDir)
    val sessions = SessionManager(storage) {
        cleared.incrementAndGet()
        cache.clear()
    }
    private val baseUrl = server.url("/").toString()

    /** Mismo cliente que usa la app, con tiempos de espera cortos para que las pruebas no se demoren. */
    val http = HttpClientFactory.create(baseUrl, sessions, FIXED_CLOCK).newBuilder()
        .readTimeout(400, TimeUnit.MILLISECONDS)
        .connectTimeout(400, TimeUnit.MILLISECONDS)
        .build()
    val api = ApiClient(http, baseUrl)
    val remote = RemoteSource(api, cache, FIXED_CLOCK)

    fun signIn(access: String = "A1", refresh: String = "R1") {
        sessions.signIn(AuthTokens(access, refresh, FIXED_NOW.plusSeconds(900)), SAMPLE_PROFILE)
    }
}
