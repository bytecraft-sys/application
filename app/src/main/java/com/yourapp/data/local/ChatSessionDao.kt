package com.yourapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: String): Flow<ChatSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Update
    suspend fun updateSession(session: ChatSession)

    @Query("UPDATE chat_sessions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteSession(id: String)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun hardDeleteSession(id: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    @Query("UPDATE chat_sessions SET isPinned = :isPinned WHERE id = :id")
    suspend fun pinSession(id: String, isPinned: Boolean)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_sessions WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR lastMessage LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun searchSessions(query: String): Flow<List<ChatSession>>

    @Query("UPDATE chat_sessions SET lastMessage = :lastMessage, updatedAt = :updatedAt, messageCount = messageCount + 1 WHERE id = :sessionId")
    suspend fun updateSessionPreview(sessionId: String, lastMessage: String, updatedAt: Long)
}
