package com.yourapp.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.yourapp.ui.chat.ChatViewModel

@Composable
fun HomeRoute(
    onOpenProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val messages = viewModel.pagedMessages.collectAsLazyPagingItems()
    val chatState by viewModel.state.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val currentSearchIndex by viewModel.currentSearchIndex.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceToTextParser.startListening()
        }
    }

    HomeScreen(
        messages = messages,
        chatState = chatState,
        voiceState = voiceState,
        searchQuery = searchQuery,
        searchResults = searchResults,
        currentSearchIndex = currentSearchIndex,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onNextSearchResult = viewModel::onNextSearchResult,
        onPreviousSearchResult = viewModel::onPreviousSearchResult,
        onClearSearch = viewModel::clearSearch,
        onSendMessage = viewModel::sendMessage,
        onOpenProfile = onOpenProfile,
        onNavigateToHistory = onNavigateToHistory,
        onNewChat = viewModel::startNewChat,
        onToggleVoice = {
            if (voiceState.isSpeaking) {
                viewModel.voiceToTextParser.stopListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onClearChat = viewModel::clearChat
    )
}
