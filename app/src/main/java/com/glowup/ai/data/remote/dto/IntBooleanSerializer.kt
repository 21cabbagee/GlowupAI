package com.glowup.ai.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer to handle backend integer-as-boolean fields.
 * Backend sends 0/1 integers for boolean fields like `is_baseline` and `enabled`.
 * This serializer converts: 0 → false, any non-zero → true
 */
object IntBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntBoolean", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeInt() != 0

    override fun serialize(
        encoder: Encoder,
        value: Boolean,
    ) {
        encoder.encodeInt(if (value) 1 else 0)
    }
}
