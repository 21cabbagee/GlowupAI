package com.glowup.ai.data.local

import androidx.room.TypeConverter
import com.glowup.ai.data.remote.NetworkJson
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Room type converters for the handful of cache columns that hold structured data directly
 * (rather than a single JSON-blob-per-row, which is how [GlowUpDatabase]'s cache entities store
 * their full payload — see each entity's doc). Reuses [NetworkJson], the same `Json` instance the
 * network layer uses, so encoding here is byte-for-byte consistent with what the DTOs already
 * (de)serialize.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.let { NetworkJson.encodeToString(ListSerializer(String.serializer()), it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value?.let { runCatching { NetworkJson.decodeFromString(ListSerializer(String.serializer()), it) }.getOrNull() }

    @TypeConverter
    fun fromDoubleMap(value: Map<String, Double>?): String? =
        value?.let { NetworkJson.encodeToString(MapSerializer(String.serializer(), Double.serializer()), it) }

    @TypeConverter
    fun toDoubleMap(value: String?): Map<String, Double>? =
        value?.let {
            runCatching {
                NetworkJson.decodeFromString(MapSerializer(String.serializer(), Double.serializer()), it)
            }.getOrNull()
        }
}
