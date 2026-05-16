package com.yourapp.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MessageMetaConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromMessageMeta(meta: MessageMeta): String = json.encodeToString(meta)

    @TypeConverter
    fun toMessageMeta(value: String): MessageMeta = json.decodeFromString(value)
}
