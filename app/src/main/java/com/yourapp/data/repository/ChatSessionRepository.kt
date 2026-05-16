package com.yourapp.data.repository

import com.yourapp.data.local.ChatSession
import com.yourapp.data.local.ChatSessionDao
import com.yourapp.data.local.ChatMessage
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSessionRepository @Inject constructor(
    private val chatSessionDao: ChatSessionDao
) {
    fun getAllSessions(): Flow<List<ChatSession>> = chatSessionDao.getAllSessions()

    fun getSessionById(id: String): Flow<ChatSession?> = chatSessionDao.getSessionById(id)

    suspend fun insertSession(session: ChatSession) = chatSessionDao.insertSession(session)

    suspend fun updateSession(session: ChatSession) = chatSessionDao.updateSession(session)

    suspend fun softDeleteSession(id: String) = chatSessionDao.softDeleteSession(id)

    suspend fun hardDeleteSession(id: String) = chatSessionDao.hardDeleteSession(id)

    suspend fun deleteAllSessions() = chatSessionDao.deleteAllSessions()

    suspend fun pinSession(id: String, isPinned: Boolean) = chatSessionDao.pinSession(id, isPinned)

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> = chatSessionDao.getMessagesForSession(sessionId)

    fun searchSessions(query: String): Flow<List<ChatSession>> = chatSessionDao.searchSessions(query)

    suspend fun createNewSession(firstMessage: String): ChatSession {
        val title = if (firstMessage.length > 40) {
            firstMessage.take(40) + "..."
        } else {
            firstMessage
        }
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messageCount = 0,
            lastMessage = "",
            isPinned = false,
            isDeleted = false
        )
        chatSessionDao.insertSession(session)
        return session
    }

    suspend fun updateSessionPreview(sessionId: String, lastMessage: String) {
        val preview = if (lastMessage.length > 80) {
            lastMessage.take(80) + "..."
        } else {
            lastMessage
        }
        chatSessionDao.updateSessionPreview(
            sessionId = sessionId,
            lastMessage = preview,
            updatedAt = System.currentTimeMillis()
        )
    }
}
