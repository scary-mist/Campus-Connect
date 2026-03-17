package com.campusconnect.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.data.model.Conversation
import com.campusconnect.data.model.Message
import com.campusconnect.data.repository.AuthRepository
import com.campusconnect.data.repository.ChatRepository
import com.campusconnect.data.repository.DiscoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val discoverRepository: DiscoverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUser?.uid

    init {
        loadConversations()
    }

    private fun loadConversations() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.getConversations(uid).collect { conversations ->
                _uiState.value = _uiState.value.copy(conversations = conversations, isLoading = false, isRefreshing = false)
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadConversations()
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(conversationId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun sendMessage(conversationId: String, content: String) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            val profileResult = authRepository.getUserProfile(uid)
            val profile = profileResult.getOrNull() ?: return@launch

            val message = Message(
                conversationId = conversationId,
                senderId = uid,
                senderName = profile.name,
                content = content
            )
            chatRepository.sendMessage(conversationId, message)
        }
    }

    /**
     * Remove a friend: deletes from both users' friends lists,
     * removes conversation, and removes friend request docs.
     */
    fun removeFriend(friendUid: String) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            discoverRepository.removeFriend(uid, friendUid)
        }
    }

    suspend fun getOrCreateConversation(otherUserId: String): String? {
        val uid = authRepository.currentUser?.uid ?: return null
        val ownProfile = authRepository.getUserProfile(uid).getOrNull() ?: return null
        val otherProfile = authRepository.getUserProfile(otherUserId).getOrNull() ?: return null

        return chatRepository.getOrCreateConversation(
            currentUid = uid,
            otherUid = otherUserId,
            currentName = ownProfile.name,
            otherName = otherProfile.name,
            currentPhoto = ownProfile.photoUrl,
            otherPhoto = otherProfile.photoUrl
        ).getOrNull()
    }
}
