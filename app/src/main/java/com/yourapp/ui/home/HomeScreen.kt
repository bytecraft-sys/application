package com.yourapp.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.application.ui.theme.ApplicationTheme
import com.yourapp.data.local.ChatMessage
import com.yourapp.data.local.MessageMeta
import com.yourapp.domain.statemachine.ChatState
import com.yourapp.ui.chat.VoiceToTextParserState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    messages: LazyPagingItems<ChatMessage>,
    chatState: ChatState,
    voiceState: VoiceToTextParserState,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    searchResults: List<ChatMessage> = emptyList(),
    currentSearchIndex: Int = 0,
    onSearchQueryChanged: (String) -> Unit = {},
    onNextSearchResult: () -> Unit = {},
    onPreviousSearchResult: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNewChat: () -> Unit,
    onToggleVoice: () -> Unit,
    onClearChat: () -> Unit,
    onRetry: () -> Unit = {},
) {
    var text by remember { mutableStateOf("") }
    var isTextInputVisible by remember { mutableStateOf(false) }
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var isSearchActive by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    
    val inputOffset by animateFloatAsState(
        targetValue = if (isTextInputVisible) 0f else INPUT_HIDDEN_OFFSET_PX,
        animationSpec = tween(250),
        label = "Text input slide offset",
    )
    val inputAlpha by animateFloatAsState(
        targetValue = if (isTextInputVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "Text input alpha",
    )
    val auraAlpha by remember {
        derivedStateOf {
            (1f - (scrollOffset / 300f)).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(currentSearchIndex, searchResults) {
        if (searchResults.isNotEmpty() && currentSearchIndex < searchResults.size) {
            val targetId = searchResults[currentSearchIndex].id
            val indexInList = (0 until messages.itemCount).indexOfFirst { messages[it]?.id == targetId }
            if (indexInList != -1) {
                listState.animateScrollToItem(indexInList)
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) {
                    scrollOffset = (scrollOffset + -available.y).coerceAtMost(300f)
                } else if (available.y > 0f) {
                    scrollOffset = (scrollOffset - available.y).coerceAtLeast(0f)
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ChatTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                searchResultsCount = searchResults.size,
                currentSearchIndex = currentSearchIndex,
                onSearchToggle = { 
                    isSearchActive = !isSearchActive 
                    if (!isSearchActive) onClearSearch()
                },
                onSearchQueryChanged = onSearchQueryChanged,
                onNextResult = onNextSearchResult,
                onPreviousResult = onPreviousSearchResult,
                onNavigateToHistory = onNavigateToHistory,
                onNewChat = onNewChat,
                onOpenProfile = onOpenProfile
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                AuraCircle(
                    state = if (voiceState.isSpeaking) AuraState.Listening else AuraState.Breathing,
                    amplitude = voiceState.amplitude,
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer(
                            alpha = auraAlpha,
                            translationY = -scrollOffset * AURA_PARALLAX_FACTOR,
                        ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                ChatStateBanner(
                    chatState = chatState,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))

                ChatHistoryList(
                    messages = messages,
                    listState = listState,
                    highlightQuery = if (isSearchActive) searchQuery else null,
                    highlightedMessageId = if (isSearchActive && searchResults.isNotEmpty()) searchResults[currentSearchIndex].id else null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(92.dp))
            }

            if (!isTextInputVisible) {
                InputLauncher(
                    isListening = voiceState.isSpeaking,
                    onShowKeyboard = { isTextInputVisible = true },
                    onToggleVoice = onToggleVoice,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            MessageInput(
                value = text,
                onValueChange = { text = it },
                onDone = {
                    if (text.trim().isNotEmpty()) {
                        onSendMessage(text.trim())
                        text = ""
                        focusManager.clearFocus()
                    }
                },
                onHideKeyboard = {
                    isTextInputVisible = false
                    focusManager.clearFocus()
                },
                onToggleVoice = onToggleVoice,
                isListening = voiceState.isSpeaking,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset {
                        IntOffset(0, inputOffset.roundToInt())
                    }
                    .graphicsLayer(alpha = inputAlpha),
            )
        }
    }
}

@Composable
private fun ChatStateBanner(
    chatState: ChatState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (chatState) {
        ChatState.Idle -> "Ready"
        ChatState.Typing -> "Typing..."
        ChatState.Validating -> "Checking message"
        is ChatState.Processing -> "Thinking..."
        is ChatState.Responding -> "Writing response"
        is ChatState.Error -> chatState.message
    }
    val containerColor = when (chatState) {
        ChatState.Idle -> MaterialTheme.colorScheme.surfaceVariant
        ChatState.Typing -> MaterialTheme.colorScheme.primaryContainer
        ChatState.Validating -> MaterialTheme.colorScheme.secondaryContainer
        is ChatState.Processing -> MaterialTheme.colorScheme.tertiaryContainer
        is ChatState.Responding -> MaterialTheme.colorScheme.primary
        is ChatState.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (chatState) {
        ChatState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        ChatState.Typing -> MaterialTheme.colorScheme.onPrimaryContainer
        ChatState.Validating -> MaterialTheme.colorScheme.onSecondaryContainer
        is ChatState.Processing -> MaterialTheme.colorScheme.onTertiaryContainer
        is ChatState.Responding -> MaterialTheme.colorScheme.onPrimary
        is ChatState.Error -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (chatState is ChatState.Processing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor,
                )
            }
            if (chatState is ChatState.Error) {
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    searchResultsCount: Int,
    currentSearchIndex: Int,
    onSearchToggle: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onNextResult: () -> Unit,
    onPreviousResult: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNewChat: () -> Unit,
    onOpenProfile: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search in chat") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchResultsCount > 0) {
                        Text(
                            text = "${currentSearchIndex + 1} of $searchResultsCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = onPreviousResult) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Previous result")
                        }
                        IconButton(onClick = onNextResult) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Next result")
                        }
                    }
                }
            } else {
                Text("Chat")
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Default.History, contentDescription = "History")
            }
        },
        actions = {
            IconButton(onClick = onSearchToggle) {
                Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onNewChat) {
                Icon(Icons.Default.EditNote, contentDescription = "New chat")
            }
            IconButton(onClick = onOpenProfile) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
            }
        }
    )
}

@Composable
private fun ChatHistoryList(
    messages: LazyPagingItems<ChatMessage>,
    listState: LazyListState,
    highlightQuery: String?,
    highlightedMessageId: String?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(
            count = messages.itemCount,
            key = { index -> messages[index]?.id ?: "placeholder-$index" },
        ) { index ->
            val message = messages[index] ?: return@items
            ChatMessageRow(
                message = message,
                highlightQuery = highlightQuery,
                isHighlightedResult = message.id == highlightedMessageId
            )
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessage,
    highlightQuery: String?,
    isHighlightedResult: Boolean,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == "user"
    val isError = message.content.startsWith("Error:")
    val sender = when {
        isUser -> "You"
        isError -> "Error"
        else -> "AI"
    }
    val timestamp = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }
    
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val backgroundColor = when {
        isUser -> MaterialTheme.colorScheme.primary
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimary
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 2.dp, bottomEnd = 18.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            shape = shape,
            tonalElevation = if (isUser) 4.dp else 1.dp,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .then(
                    if (isHighlightedResult) {
                        Modifier.border(2.dp, Color(0xFF1D9E75), shape)
                    } else Modifier
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = sender,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f),
                    )
                }
                 
                val annotatedContent = remember(message.content, highlightQuery) {
                    if (highlightQuery.isNullOrBlank()) {
                        buildAnnotatedString { append(message.content) }
                    } else {
                        buildAnnotatedString {
                            val text = message.content
                            var start = 0
                            while (start < text.length) {
                                val index = text.indexOf(highlightQuery, start, ignoreCase = true)
                                if (index == -1) {
                                    append(text.substring(start))
                                    break
                                }
                                append(text.substring(start, index))
                                withStyle(
                                    SpanStyle(
                                        background = Color(0xFF1D9E75).copy(alpha = 0.3f),
                                        color = Color.White
                                    )
                                ) {
                                    append(text.substring(index, index + highlightQuery.length))
                                }
                                start = index + highlightQuery.length
                            }
                        }
                    }
                }

                Text(
                    text = annotatedContent,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun InputLauncher(
    isListening: Boolean,
    onShowKeyboard: () -> Unit,
    onToggleVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onShowKeyboard) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Open keyboard input",
                )
            }
            IconButton(onClick = onToggleVoice) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = if (isListening) "Stop listening" else "Start listening",
                    tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    onHideKeyboard: () -> Unit,
    onToggleVoice: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onHideKeyboard) {
                Icon(
                    imageVector = Icons.Default.KeyboardHide,
                    contentDescription = "Hide keyboard input",
                )
            }

            IconButton(onClick = onToggleVoice) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = if (isListening) "Stop listening" else "Start listening",
                    tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("How can I help?", style = MaterialTheme.typography.bodyLarge) },
                singleLine = false,
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onDone() }
                ),
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = onDone, 
                enabled = value.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}

private const val INPUT_HIDDEN_OFFSET_PX = 160f
private const val AURA_PARALLAX_FACTOR = 0.35f
