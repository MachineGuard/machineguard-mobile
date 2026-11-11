package com.machineguard.mobile.core.data

/**
 * Modelo de errores de la capa de datos. Es independiente de OkHttp y del formato de error del servidor, así que las
 * pantallas y los ViewModels no necesitan conocer códigos HTTP ni excepciones.
 *
 * Correspondencia con el Core API:
 *
 * | Situación                                                   | Error                |
 * |-------------------------------------------------------------|----------------------|
 * | Sin red, DNS o conexión rechazada                           | [Offline]            |
 * | El servidor tarda más del tiempo permitido                  | [Timeout]            |
 * | 401 y la renovación de sesión no es posible                 | [SessionExpired]     |
 * | 401 en el login (credenciales incorrectas)                  | [InvalidCredentials] |
 * | 403 (por ejemplo un VIEWER intenta reconocer una alerta)    | [Forbidden]          |
 * | 404 (incluye recursos de otra organización)                 | [NotFound]           |
 * | 409 (regla de negocio: ya reconocida, ya resuelta, ...)     | [Conflict]           |
 * | 400                                                         | [Invalid]            |
 * | 429                                                         | [TooManyRequests]    |
 * | 5xx                                                         | [Server]             |
 * | Cualquier otra cosa, incluida una respuesta ilegible        | [Unknown]            |
 */
sealed interface AppError {

    /** Texto que envió el servidor para este error, si lo hay. Es informativo: no es un mensaje listo para mostrar. */
    val detail: String? get() = null

    data object Offline : AppError

    data object Timeout : AppError

    /** La sesión terminó y no se pudo renovar: hay que volver al login. La capa de datos ya limpió la sesión local. */
    data object SessionExpired : AppError

    /** Correo o contraseña incorrectos (solo el login). */
    data object InvalidCredentials : AppError

    data class Forbidden(override val detail: String? = null) : AppError

    data class NotFound(override val detail: String? = null) : AppError

    /** La operación choca con una regla de negocio; [detail] suele explicar por qué (p. ej. resolución no permitida). */
    data class Conflict(override val detail: String? = null) : AppError

    data class Invalid(override val detail: String? = null) : AppError

    /** Demasiados intentos de login. [retryAfterSeconds] viene de la cabecera Retry-After si el servidor la envió. */
    data class TooManyRequests(val retryAfterSeconds: Long? = null) : AppError

    data class Server(val statusCode: Int, override val detail: String? = null) : AppError

    data class Unknown(override val detail: String? = null) : AppError
}

/** El error pide volver al login. */
val AppError.requiresLogin: Boolean get() = this is AppError.SessionExpired

/** Tiene sentido ofrecer "Reintentar": el problema puede ser pasajero. */
val AppError.isRetryable: Boolean
    get() = this is AppError.Offline || this is AppError.Timeout || this is AppError.Server || this is AppError.Unknown
