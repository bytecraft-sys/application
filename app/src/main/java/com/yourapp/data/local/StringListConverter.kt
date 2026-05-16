package com.yourapp.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StringListConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromStringList(values: List<String>): String = json.encodeToString(values)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)
}
