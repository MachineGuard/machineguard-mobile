package com.machineguard.mobile.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Configuración JSON común. Tolera campos nuevos del servidor y omite los nulos al enviar. */
object ApiJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
}

/**
 * Lee un decimal del JSON como texto literal (`12.30` queda `12.30`), sin pasar por `Double`, para no perder precisión
 * en las lecturas. Acepta tanto números como cadenas.
 */
object BigDecimalLiteralSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return BigDecimal((element as JsonPrimitive).content)
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }
}

/** Fecha ISO 8601 del servidor, o `null` si falta o no se entiende (nunca lanza). */
fun parseApiDate(value: String?): OffsetDateTime? = try {
    value?.let { OffsetDateTime.parse(it) }
} catch (e: DateTimeParseException) {
    null
}
