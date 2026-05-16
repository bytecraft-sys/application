package com.yourapp.data.local

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val age: Int,
    val phone: String,
    val selectedTraits: List<String>,
    val createdAt: Long,
    val lastSyncedAt: Long,
    val otp: String = "",
)
