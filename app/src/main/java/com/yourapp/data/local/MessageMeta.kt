package com.yourapp.data.local

import kotlinx.serialization.Serializable

@Serializable
data class MessageMeta(
    val tokens: Int,
    val model: String,
    val durationMs: Long,
)
