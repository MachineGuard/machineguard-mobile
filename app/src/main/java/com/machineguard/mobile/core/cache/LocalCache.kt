package com.machineguard.mobile.core.cache

import java.time.OffsetDateTime

/** Copia local de una respuesta del servidor. [payload] es el JSON tal como llegó. */
data class CacheEntry(val payload: String, val savedAt: OffsetDateTime)

/**
 * Caché de la última respuesta correcta de cada consulta, para mostrar datos cuando no hay conexión.
 *
 * Solo respalda a la red (nunca la reemplaza) y **se borra al cerrar o perder la sesión**: contiene datos de la
 * organización del usuario.
 */
interface LocalCache {
    fun read(key: String): CacheEntry?

    fun write(key: String, payload: String, savedAt: OffsetDateTime)

    /** Elimina las entradas cuya clave empieza con [prefix] (por ejemplo, las alertas tras reconocer una). */
    fun removeByPrefix(prefix: String)

    fun clear()
}
