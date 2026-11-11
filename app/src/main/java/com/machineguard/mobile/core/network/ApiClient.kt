package com.machineguard.mobile.core.network

import com.machineguard.mobile.core.data.AppError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import kotlin.coroutines.resume

/** Respuesta cruda del Core API ya clasificada: cuerpo de una respuesta 2xx, o un [AppError]. */
sealed interface ApiResult {
    data class Success(val body: String, val statusCode: Int) : ApiResult

    data class Failure(val error: AppError) : ApiResult
}

/**
 * Ejecuta peticiones HTTP y traduce todo lo que puede salir mal a un [AppError], de modo que ninguna excepción de red
 * llegue a los repositorios ni a las pantallas. Las peticiones se cancelan junto con la corrutina que las lanzó.
 */
class ApiClient(
    private val client: okhttp3.OkHttpClient,
    baseUrl: String,
) {
    private val base = baseUrl.trimEnd('/')

    suspend fun get(path: String, query: Map<String, String?> = emptyMap()): ApiResult =
        execute(request(path, query).get().build(), AppError.SessionExpired)

    /**
     * @param jsonBody cuerpo ya serializado, o `null` para una petición sin cuerpo.
     * @param public `true` para login y refresh: no llevan token y su 401 no significa "sesión vencida".
     * @param unauthorizedAs error con el que se informa un 401 que no se pudo resolver renovando la sesión.
     */
    suspend fun post(
        path: String,
        jsonBody: String? = null,
        public: Boolean = false,
        unauthorizedAs: AppError = AppError.SessionExpired,
    ): ApiResult {
        val builder = request(path, emptyMap()).post((jsonBody ?: "").toRequestBody(JSON))
        if (public) builder.tag(PublicRequest::class.java, PublicRequest)
        return execute(builder.build(), unauthorizedAs)
    }

    private fun request(path: String, query: Map<String, String?>): Request.Builder {
        val url = try {
            val builder = (base + "/" + path.trimStart('/')).toHttpUrl().newBuilder()
            query.forEach { (name, value) -> if (value != null) builder.addQueryParameter(name, value) }
            builder.build()
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("URL base del API no válida: $base", e)
        }
        return Request.Builder().url(url).header("Accept", "application/json")
    }

    private suspend fun execute(request: Request, unauthorizedAs: AppError): ApiResult =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(ApiResult.Failure(errorFor(e)))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = try {
                        response.use { classify(it, unauthorizedAs) }
                    } catch (e: IOException) {
                        ApiResult.Failure(errorFor(e))
                    }
                    continuation.resume(result)
                }
            })
        }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

internal fun classify(response: Response, unauthorizedAs: AppError): ApiResult {
    val body = response.body?.string().orEmpty()
    if (response.isSuccessful) return ApiResult.Success(body, response.code)
    return ApiResult.Failure(errorFor(response.code, body, response.header("Retry-After"), unauthorizedAs))
}

/** Correspondencia HTTP → [AppError]; ver la tabla en [AppError]. */
internal fun errorFor(code: Int, body: String, retryAfter: String?, unauthorizedAs: AppError): AppError = when (code) {
    400 -> AppError.Invalid(errorDetail(body))
    401 -> unauthorizedAs
    403 -> AppError.Forbidden(errorDetail(body))
    404 -> AppError.NotFound(errorDetail(body))
    408 -> AppError.Timeout
    409 -> AppError.Conflict(errorDetail(body))
    429 -> AppError.TooManyRequests(retryAfter?.trim()?.toLongOrNull())
    in 500..599 -> AppError.Server(code, errorDetail(body))
    else -> AppError.Unknown("HTTP $code")
}

/** Excepciones de red → [AppError]. Solo las de entrada/salida son "sin conexión"; el resto es un fallo inesperado. */
internal fun errorFor(e: IOException): AppError = when {
    e is SocketTimeoutException -> AppError.Timeout
    e is InterruptedIOException && e.message?.contains("timeout", ignoreCase = true) == true -> AppError.Timeout
    else -> AppError.Offline
}

/**
 * Texto de error del servidor. Los formatos que usa el Core API son `{"detail": ...}` (ProblemDetail) y
 * `{"message": ...}` (IAM). Si el cuerpo no es JSON, no hay detalle.
 */
internal fun errorDetail(body: String): String? {
    if (body.isBlank()) return null
    val obj: JsonObject = try {
        ApiJson.instance.parseToJsonElement(body).jsonObject
    } catch (e: Exception) {
        return null
    }
    val text = (obj["detail"] as? JsonPrimitive)?.contentOrNull
        ?: (obj["message"] as? JsonPrimitive)?.contentOrNull
    return text?.take(MAX_DETAIL)
}

private const val MAX_DETAIL = 300
