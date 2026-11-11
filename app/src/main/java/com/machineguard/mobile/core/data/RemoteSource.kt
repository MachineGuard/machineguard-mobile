package com.machineguard.mobile.core.data

import com.machineguard.mobile.core.cache.LocalCache
import com.machineguard.mobile.core.network.ApiClient
import com.machineguard.mobile.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.time.Clock
import java.time.OffsetDateTime

/**
 * Une el cliente HTTP, la caché y el modelo de resultados. Los repositorios solo indican la ruta y cómo convertir el
 * JSON en modelos de dominio.
 *
 * - **Consultas ([get]):** siempre van al servidor; la respuesta correcta se guarda en caché. Si falla la red (sin
 *   conexión, tiempo agotado o error 5xx) y hay una copia, se devuelve esa copia con `source = CACHE`. Los demás
 *   errores (401, 403, 404, 409...) nunca se tapan con la caché.
 * - **Escrituras ([post]):** nunca usan caché ni se reintentan. Tras una escritura correcta se invalidan las cachés
 *   relacionadas para no mostrar datos que ya cambiaron.
 */
class RemoteSource(
    private val api: ApiClient,
    private val cache: LocalCache,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend fun <T> get(
        cacheKey: String,
        path: String,
        query: Map<String, String?> = emptyMap(),
        parse: (String) -> T,
    ): DataResult<T> = when (val result = api.get(path, query)) {
        is ApiResult.Success -> {
            val parsed = parseOrNull(result.body, parse)
            if (parsed == null) {
                DataResult.Failure(AppError.Unknown("Respuesta del servidor no válida"))
            } else {
                val now = OffsetDateTime.now(clock)
                withContext(Dispatchers.IO) { cache.write(cacheKey, result.body, now) }
                DataResult.Success(parsed, DataSource.NETWORK, now)
            }
        }

        is ApiResult.Failure -> fromCacheOrFailure(cacheKey, result.error, parse)
    }

    suspend fun <T> post(
        path: String,
        jsonBody: String?,
        invalidatePrefixes: List<String> = emptyList(),
        parse: (String) -> T,
    ): DataResult<T> = when (val result = api.post(path, jsonBody)) {
        is ApiResult.Success -> {
            val parsed = parseOrNull(result.body, parse)
            withContext(Dispatchers.IO) { invalidatePrefixes.forEach(cache::removeByPrefix) }
            if (parsed == null) {
                DataResult.Failure(AppError.Unknown("Respuesta del servidor no válida"))
            } else {
                DataResult.Success(parsed, DataSource.NETWORK, OffsetDateTime.now(clock))
            }
        }

        is ApiResult.Failure -> DataResult.Failure(result.error)
    }

    private suspend fun <T> fromCacheOrFailure(
        cacheKey: String,
        error: AppError,
        parse: (String) -> T,
    ): DataResult<T> {
        if (!error.allowsCacheFallback) return DataResult.Failure(error)
        val entry = withContext(Dispatchers.IO) { cache.read(cacheKey) } ?: return DataResult.Failure(error)
        val parsed = parseOrNull(entry.payload, parse) ?: return DataResult.Failure(error)
        return DataResult.Success(parsed, DataSource.CACHE, entry.savedAt)
    }

    private fun <T> parseOrNull(body: String, parse: (String) -> T): T? = try {
        parse(body)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}

/** Errores pasajeros: tiene sentido mostrar la última copia guardada en lugar del error. */
private val AppError.allowsCacheFallback: Boolean
    get() = this is AppError.Offline || this is AppError.Timeout || this is AppError.Server
