package com.yourapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val phone: String,
    val selectedTraits: List<String>,
    val createdAt: Long,
    val lastSyncedAt: Long,
)
