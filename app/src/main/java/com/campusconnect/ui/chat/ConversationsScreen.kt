package com.campusconnect.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.campusconnect.ui.components.LoadingSpinner
import com.campusconnect.ui.components.PullRefreshLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.campusconnect.data.model.Conversation
import com.campusconnect.ui.theme.GradientEnd
import com.campusconnect.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    chatViewModel: ChatViewModel,
    onChatDetail: (String) -> Unit
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val currentUserId = chatViewModel.currentUserId

    // State for the "Remove Friend" confirmation dialog
    var conversationToRemove by remember { mutableStateOf<Conversation?>(null) }

    // Confirmation dialog
    conversationToRemove?.let { convo ->
        val otherUserId = convo.participants.firstOrNull { it != currentUserId } ?: ""
        val otherName = convo.participantNames[otherUserId] ?: "Unknown"

        AlertDialog(
            onDismissRequest = { conversationToRemove = null },
            title = { Text("Remove Friend") },
            text = { Text("Remove $otherName as a friend? This will delete your conversation and remove them from your friends list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatViewModel.removeFriend(otherUserId)
                        conversationToRemove = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        PullRefreshLayout(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { chatViewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner(color = GradientStart)
            }
        } else if (uiState.conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No conversations yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Wave at someone to start chatting!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(uiState.conversations, key = { it.id }) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        currentUserId = currentUserId ?: "",
                        onClick = { onChatDetail(conversation.id) },
                        onRemove = { conversationToRemove = conversation }
                    )
                    Divider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit,
    onRemove: () -> Unit = {}
) {
    val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: ""
    val otherName = conversation.participantNames[otherUserId] ?: "Unknown"
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = otherName.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = otherName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (conversation.lastTimestamp > 0) {
                    Text(
                        text = dateFormat.format(Date(conversation.lastTimestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (conversation.lastMessage.isNotEmpty()) {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Three-dot menu for remove friend
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Remove Friend", color = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        showMenu = false
                        onRemove()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}
