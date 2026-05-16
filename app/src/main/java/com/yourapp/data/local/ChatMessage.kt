package com.yourapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["lastSyncedAt"]),
        Index(value = ["sessionId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChatMessage(
    @PrimaryKey val id: String,
    val sessionId: String,
    val content: String,
    val role: String,
    val timestamp: Long,
    val isSynced: Boolean,
    val meta: MessageMeta,
    val lastSyncedAt: Long,
)
