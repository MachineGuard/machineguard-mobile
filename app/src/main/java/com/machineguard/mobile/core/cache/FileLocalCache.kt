package com.machineguard.mobile.core.cache

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Caché en archivos dentro de un directorio privado de la app. Cada entrada es un archivo cuya primera línea es la
 * clave y la segunda la fecha de guardado; el resto es el JSON. Se escribe en un archivo temporal y se renombra, así
 * que una escritura interrumpida nunca deja una entrada a medias.
 *
 * Todas las operaciones son tolerantes a fallos: si el disco falla o un archivo está dañado, la entrada se trata como
 * inexistente (y se borra), nunca como un error.
 */
class FileLocalCache(private val directory: File) : LocalCache {

    @Synchronized
    override fun read(key: String): CacheEntry? {
        val file = fileFor(key)
        if (!file.isFile) return null
        return try {
            val text = file.readText()
            val firstBreak = text.indexOf('\n')
            val secondBreak = if (firstBreak < 0) -1 else text.indexOf('\n', firstBreak + 1)
            if (firstBreak < 0 || secondBreak < 0 || text.substring(0, firstBreak) != key) {
                file.delete()
                null
            } else {
                CacheEntry(text.substring(secondBreak + 1), OffsetDateTime.parse(text.substring(firstBreak + 1, secondBreak)))
            }
        } catch (e: IOException) {
            null
        } catch (e: DateTimeParseException) {
            file.delete()
            null
        }
    }

    @Synchronized
    override fun write(key: String, payload: String, savedAt: OffsetDateTime) {
        try {
            directory.mkdirs()
            val target = fileFor(key)
            val temporary = File(directory, target.name + ".tmp")
            temporary.writeText("$key\n$savedAt\n$payload")
            if (!temporary.renameTo(target)) {
                target.delete()
                temporary.renameTo(target)
            }
        } catch (e: IOException) {
            // Sin espacio o sin permisos: la caché es opcional.
        }
    }

    @Synchronized
    override fun removeByPrefix(prefix: String) {
        directory.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            val storedKey = try {
                file.bufferedReader().use { it.readLine() }
            } catch (e: IOException) {
                null
            }
            if (storedKey == null || storedKey.startsWith(prefix)) file.delete()
        }
    }

    @Synchronized
    override fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun fileFor(key: String): File = File(directory, sha256(key) + ".cache")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
