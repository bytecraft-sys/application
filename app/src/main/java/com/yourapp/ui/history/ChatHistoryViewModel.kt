package com.yourapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.data.local.ChatSession
import com.yourapp.data.repository.ChatSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatHistoryState(
    val sessions: List<ChatSession> = emptyList(),
    val filteredSessions: List<ChatSession> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val selectedSessions: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false
)

sealed class UiEvent {
    data class NavigateToChat(val sessionId: String) : UiEvent()
    object NavigateToNewChat : UiEvent()
}

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val repository: ChatSessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatHistoryState())
    val historyState: StateFlow<ChatHistoryState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            combine(
                repository.getAllSessions(),
                _state
            ) { sessions, currentState ->
                val filtered = if (currentState.searchQuery.isBlank()) {
                    sessions
                } else {
                    sessions.filter {
                        it.title.contains(currentState.searchQuery, ignoreCase = true) ||
                                it.lastMessage.contains(currentState.searchQuery, ignoreCase = true)
                    }
                }
                currentState.copy(
                    sessions = sessions,
                    filteredSessions = filtered,
                    isLoading = false
                )
            }.collect { updatedState ->
                _state.value = updatedState
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onToggleSearch() {
        _state.update {
            val nextActive = !it.isSearchActive
            it.copy(
                isSearchActive = nextActive,
                searchQuery = if (!nextActive) "" else it.searchQuery
            )
        }
    }

    fun onDeleteSession(id: String) {
        viewModelScope.launch {
            repository.softDeleteSession(id)
        }
    }

    fun onPinSession(id: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.pinSession(id, isPinned)
        }
    }

    fun onToggleSelectSession(id: String) {
        _state.update { currentState ->
            val newSelected = if (currentState.selectedSessions.contains(id)) {
                currentState.selectedSessions - id
            } else {
                currentState.selectedSessions + id
            }
            currentState.copy(
                selectedSessions = newSelected,
                isMultiSelectMode = newSelected.isNotEmpty()
            )
        }
    }

    fun onCancelMultiSelect() {
        _state.update { it.copy(selectedSessions = emptySet(), isMultiSelectMode = false) }
    }

    fun onDeleteSelectedSessions() {
        viewModelScope.launch {
            _state.value.selectedSessions.forEach { id ->
                repository.softDeleteSession(id)
            }
            onCancelMultiSelect()
        }
    }

    fun onClearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllSessions()
        }
    }

    fun onNewChat() {
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.NavigateToNewChat)
        }
    }

    fun onSessionClicked(id: String) {
        if (_state.value.isMultiSelectMode) {
            onToggleSelectSession(id)
        } else {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.NavigateToChat(id))
            }
        }
    }
}
