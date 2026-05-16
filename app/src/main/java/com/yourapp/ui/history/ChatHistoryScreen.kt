package com.yourapp.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourapp.data.local.ChatSession
import kotlinx.coroutines.flow.collectLatest

private val BrandColor = Color(0xFF1D9E75)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatHistoryScreen(
    viewModel: ChatHistoryViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToNewChat: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.historyState.collectAsState()
    var showClearAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateToChat -> onNavigateToChat(event.sessionId)
                UiEvent.NavigateToNewChat -> onNavigateToNewChat()
            }
        }
    }

    Scaffold(
        topBar = {
            ChatHistoryTopBar(
                state = state,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onToggleSearch = viewModel::onToggleSearch,
                onDeleteSelected = viewModel::onDeleteSelectedSessions,
                onCancelMultiSelect = viewModel::onCancelMultiSelect,
                onClearAllClick = { showClearAllDialog = true },
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onNewChat,
                containerColor = BrandColor,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Box(modifier = modifier.padding(padding).fillMaxSize()) {
            if (state.sessions.isEmpty() && !state.isLoading) {
                EmptyState(onNewChat = viewModel::onNewChat)
            } else {
                SessionList(
                    state = state,
                    onSessionClick = viewModel::onSessionClicked,
                    onSessionLongClick = viewModel::onToggleSelectSession,
                    onDeleteSession = viewModel::onDeleteSession,
                    onPinSession = viewModel::onPinSession
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandColor)
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear all history?") },
            text = { Text("This will permanently delete all your conversations. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onClearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Delete all", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHistoryTopBar(
    state: ChatHistoryState,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCancelMultiSelect: () -> Unit,
    onClearAllClick: () -> Unit,
    onBack: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            when {
                state.isMultiSelectMode -> Text("${state.selectedSessions.size} selected")
                state.isSearchActive -> {
                    TextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search chats...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> Text("Chats")
            }
        },
        navigationIcon = {
            if (state.isMultiSelectMode) {
                IconButton(onClick = onCancelMultiSelect) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            when {
                state.isMultiSelectMode -> {
                    IconButton(onClick = onDeleteSelected) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                    }
                }
                state.isSearchActive -> {
                    IconButton(onClick = onToggleSearch) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                }
                else -> {
                    IconButton(onClick = onToggleSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* New chat handled by FAB */ }) {
                        Icon(Icons.Default.EditNote, contentDescription = "New chat")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Clear all history") },
                                onClick = {
                                    showMenu = false
                                    onClearAllClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionList(
    state: ChatHistoryState,
    onSessionClick: (String) -> Unit,
    onSessionLongClick: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onPinSession: (String, Boolean) -> Unit
) {
    val grouped = remember(state.filteredSessions) {
        groupSessions(state.filteredSessions)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        grouped.forEach { (header, sessions) ->
            stickyHeader {
                SectionHeader(title = header, isPinned = header == "Pinned")
            }
            items(sessions, key = { it.id }) { session ->
                ChatSessionItem(
                    session = session,
                    isSelected = state.selectedSessions.contains(session.id),
                    isMultiSelectMode = state.isMultiSelectMode,
                    onClick = { onSessionClick(session.id) },
                    onLongClick = { onSessionLongClick(session.id) },
                    onDelete = { onDeleteSession(session.id) },
                    onPin = { onPinSession(session.id, !session.isPinned) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, isPinned: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPinned) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChatSessionItem(
    session: ChatSession,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        modifier = Modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) BrandColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(checkedColor = BrandColor)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = BrandColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = session.lastMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = session.updatedAt.toRelativeTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${session.messageCount}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            )
                        }
                    }
                }

                if (session.isPinned) {
                    IconButton(onClick = onPin) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Unpin",
                            tint = BrandColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onNewChat: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No conversations yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = "Start a new chat to begin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNewChat,
            colors = ButtonDefaults.buttonColors(containerColor = BrandColor)
        ) {
            Text("Start chatting")
        }
    }
}

private fun groupSessions(sessions: List<ChatSession>): Map<String, List<ChatSession>> {
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L
    
    val pinned = sessions.filter { it.isPinned }
    val unpinned = sessions.filter { !it.isPinned }
    
    val groups = mutableMapOf<String, MutableList<ChatSession>>()
    
    if (pinned.isNotEmpty()) groups["Pinned"] = pinned.toMutableList()
    
    unpinned.forEach { session ->
        val diff = now - session.updatedAt
        val group = when {
            diff < dayMs -> "Today"
            diff < 2 * dayMs -> "Yesterday"
            diff < 7 * dayMs -> "Last 7 days"
            diff < 30 * dayMs -> "Last 30 days"
            else -> "Older"
        }
        groups.getOrPut(group) { mutableListOf() }.add(session)
    }
    return groups
}
