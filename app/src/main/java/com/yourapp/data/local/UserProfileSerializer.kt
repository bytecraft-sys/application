package com.yourapp.data.local

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
object UserProfileSerializer : Serializer<UserProfile> {
    override val defaultValue: UserProfile = UserProfile(
        id = "",
        name = "",
        age = 0,
        phone = "",
        selectedTraits = emptyList(),
        createdAt = 0L,
        lastSyncedAt = 0L,
    )

    override suspend fun readFrom(input: InputStream): UserProfile {
        return try {
            ProtoBuf.decodeFromByteArray(UserProfile.serializer(), input.readBytes())
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot read user profile.", exception)
        }
    }

    override suspend fun writeTo(t: UserProfile, output: OutputStream) {
        output.write(ProtoBuf.encodeToByteArray(UserProfile.serializer(), t))
    }
}
