package com.yourapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yourapp.data.local.ChatMessage
import com.yourapp.data.local.ChatMessageDao
import com.yourapp.data.local.MessageMeta
import com.yourapp.data.repository.ChatMessagePagingSource
import com.yourapp.data.repository.ChatSessionRepository
import com.yourapp.domain.statemachine.ChatState
import com.yourapp.domain.statemachine.ChatStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val chatMessagePagingSource: ChatMessagePagingSource,
    private val chatSessionRepository: ChatSessionRepository,
    private val chatStateMachine: ChatStateMachine,
    val voiceToTextParser: VoiceToTextParser,
) : ViewModel() {
    val state: StateFlow<ChatState> = chatStateMachine.state
    val voiceState = voiceToTextParser.state

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSearchIndex = MutableStateFlow(0)
    val currentSearchIndex: StateFlow<Int> = _currentSearchIndex.asStateFlow()
    private var lastSubmittedMessage: String? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<ChatMessage>> = combine(
        _currentSessionId,
        _searchQuery
    ) { sessionId, query ->
        if (sessionId == null || query.isBlank()) {
            flowOf(emptyList<ChatMessage>())
        } else {
            chatMessageDao.searchMessagesInSession(sessionId, query)
        }
    }.flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedMessages: Flow<PagingData<ChatMessage>> = _currentSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) {
            flowOf(PagingData.empty())
        } else {
            Pager(
                config = PagingConfig(
                    pageSize = CHAT_PAGE_SIZE,
                    initialLoadSize = CHAT_PAGE_SIZE,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory = { chatMessageDao.pagingSourceForSession(sessionId) },
            ).flow.cachedIn(viewModelScope)
        }
    }

    init {
        viewModelScope.launch {
            state.collect { currentState ->
                if (currentState is ChatState.Responding) {
                    val sessionId = _currentSessionId.value
                    if (sessionId != null) {
                        saveAssistantMessage(sessionId, currentState.content)
                        chatSessionRepository.updateSessionPreview(sessionId, currentState.content)
                    }
                }
            }
        }
    }

    private suspend fun saveAssistantMessage(sessionId: String, content: String) {
        val now = System.currentTimeMillis()
        chatMessageDao.insert(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                content = content,
                role = "assistant",
                timestamp = now,
                isSynced = false,
                meta = MessageMeta(
                    tokens = content.split(Regex("\\s+")).size,
                    model = "gemini-3-flash-preview",
                    durationMs = 0L,
                ),
                lastSyncedAt = 0L,
            )
        )
    }

    fun sendMessage(content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return
        lastSubmittedMessage = trimmedContent

        viewModelScope.launch {
            if (_currentSessionId.value == null) {
                val newSession = chatSessionRepository.createNewSession(trimmedContent)
                _currentSessionId.value = newSession.id
            }

            val sessionId = _currentSessionId.value!!
            val now = System.currentTimeMillis()

            chatMessageDao.insert(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    content = trimmedContent,
                    role = "user",
                    timestamp = now,
                    isSynced = false,
                    meta = MessageMeta(
                        tokens = trimmedContent.split(Regex("\\s+")).size,
                        model = "local-input",
                        durationMs = 0L,
                    ),
                    lastSyncedAt = 0L,
                ),
            )
            chatStateMachine.sendMessage(trimmedContent)
        }
    }

    fun retryLastMessage() {
        lastSubmittedMessage?.let(::sendMessage)
    }

    fun loadSession(sessionId: String) {
        _currentSessionId.value = sessionId
    }

    fun startNewChat() {
        _currentSessionId.value = null
        clearSearch()
    }

    fun clearChat() {
        viewModelScope.launch {
            val sessionId = _currentSessionId.value
            if (sessionId != null) {
                chatMessageDao.deleteBySessionId(sessionId)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _currentSearchIndex.value = 0
    }

    fun onNextSearchResult() {
        val results = searchResults.value
        if (results.isNotEmpty()) {
            _currentSearchIndex.value = (_currentSearchIndex.value + 1) % results.size
        }
    }

    fun onPreviousSearchResult() {
        val results = searchResults.value
        if (results.isNotEmpty()) {
            _currentSearchIndex.value = if (_currentSearchIndex.value > 0) {
                _currentSearchIndex.value - 1
            } else {
                results.size - 1
            }
        }
    }

    fun clearSearch() {
        onSearchQueryChanged("")
    }

    override fun onCleared() {
        chatStateMachine.close()
        super.onCleared()
    }

    private companion object {
        private const val CHAT_PAGE_SIZE = 20
    }
}
