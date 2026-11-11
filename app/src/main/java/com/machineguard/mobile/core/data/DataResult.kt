package com.machineguard.mobile.core.data

import java.time.OffsetDateTime

/** De dónde salen los datos de una respuesta correcta. */
enum class DataSource {
    /** Respuesta reciente del Core API. */
    NETWORK,

    /**
     * Última copia guardada en el dispositivo. Se devuelve cuando no hay conexión (o el servidor no responde) y existe
     * una lectura anterior; la pantalla debería indicarlo, por ejemplo con "Última actualización: ...".
     */
    CACHE,
}

/**
 * Resultado de toda operación de un repositorio. Los repositorios nunca lanzan excepciones de red: las convierten en
 * [Failure] con un [AppError] que la pantalla puede traducir a un mensaje amigable.
 */
sealed interface DataResult<out T> {

    /**
     * @property source [DataSource.CACHE] si los datos vienen de la copia local.
     * @property fetchedAt cuándo se obtuvieron del servidor (para la copia local, cuándo se guardó); nulo si no aplica.
     */
    data class Success<T>(
        val data: T,
        val source: DataSource = DataSource.NETWORK,
        val fetchedAt: OffsetDateTime? = null,
    ) : DataResult<T> {
        val isFromCache: Boolean get() = source == DataSource.CACHE
    }

    data class Failure(val error: AppError) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data), source, fetchedAt)
    is DataResult.Failure -> this
}

fun <T> DataResult<T>.getOrNull(): T? = (this as? DataResult.Success)?.data

fun <T> DataResult<T>.errorOrNull(): AppError? = (this as? DataResult.Failure)?.error
