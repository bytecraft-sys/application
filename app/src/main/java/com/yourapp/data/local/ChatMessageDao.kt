package com.yourapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.paging.PagingSource
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessage>)

    @Update
    suspend fun updateAll(messages: List<ChatMessage>)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun pagingSource(): PagingSource<Int, ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun pagingSourceForSession(sessionId: String): PagingSource<Int, ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ChatMessage?>

    @Query("SELECT * FROM chat_messages WHERE isSynced = 0 AND lastSyncedAt < :threshold ORDER BY timestamp ASC")
    fun observeUnsyncedBefore(threshold: Long): Flow<List<ChatMessage>>

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND content LIKE '%' || :query || '%' ORDER BY timestamp ASC")
    fun searchMessagesInSession(sessionId: String, query: String): Flow<List<ChatMessage>>
}
