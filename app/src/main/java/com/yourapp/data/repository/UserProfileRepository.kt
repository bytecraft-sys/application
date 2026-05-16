package com.yourapp.data.repository

import androidx.datastore.core.DataStore
import com.yourapp.data.local.UserProfile
import com.yourapp.data.local.UserProfileSerializer
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class UserProfileRepository @Inject constructor(
    private val dataStore: DataStore<UserProfile>,
) {
    val userProfileFlow: Flow<UserProfile?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emitAll(flowOf(UserProfileSerializer.defaultValue))
            } else {
                throw exception
            }
        }
        .map { profile ->
            profile.takeIf { it.id.isNotBlank() && it.name.isNotBlank() }
        }

    suspend fun saveProfile(profile: UserProfile) {
        dataStore.updateData { profile }
    }

    suspend fun clearProfile() {
        dataStore.updateData { UserProfileSerializer.defaultValue }
    }
}
