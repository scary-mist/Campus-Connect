package com.campusconnect.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.data.model.Comment
import com.campusconnect.data.model.Post
import com.campusconnect.data.repository.AuthRepository
import com.campusconnect.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isPosting: Boolean = false,
    val postCreated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUser?.uid

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            if (uid == null) {
                _uiState.value = _uiState.value.copy(posts = emptyList(), isLoading = false, isRefreshing = false)
                return@launch
            }

            try {
                // Get the current user's friends list
                val profileResult = authRepository.getUserProfile(uid)
                val profile = profileResult.getOrNull()
                val friendIds = profile?.friends ?: emptyList()

                // Show own posts + friends' posts
                val visibleUserIds = (friendIds + uid).distinct()

                feedRepository.getPostsFeed(visibleUserIds).collect { posts ->
                    _uiState.value = _uiState.value.copy(posts = posts, isLoading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                // On any error, show empty feed instead of hanging on loading
                _uiState.value = _uiState.value.copy(posts = emptyList(), isLoading = false, isRefreshing = false, error = e.message)
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadPosts()
    }

    fun createPost(
        content: String,
        imageUri: Uri? = null,
        linkUrl: String? = null,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val user = authRepository.currentUser ?: return@launch
            val profileResult = authRepository.getUserProfile(user.uid)
            val profile = profileResult.getOrNull() ?: return@launch

            _uiState.value = _uiState.value.copy(isPosting = true)

            // Upload image first if selected
            var uploadedImageUrl: String? = null
            if (imageUri != null) {
                val uploadResult = feedRepository.uploadPostImage(imageUri)
                uploadResult.fold(
                    onSuccess = { url ->
                        uploadedImageUrl = url
                        android.util.Log.d("FeedViewModel", "Image uploaded: $url")
                    },
                    onFailure = { e ->
                        android.util.Log.e("FeedViewModel", "Image upload FAILED: ${e.message}", e)
                        _uiState.value = _uiState.value.copy(
                            isPosting = false,
                            error = "Image upload failed: ${e.message}"
                        )
                        return@launch
                    }
                )
            }

            // Append link to content if provided
            val finalContent = if (!linkUrl.isNullOrBlank()) "$content\n\n🔗 $linkUrl" else content

            val post = Post(
                authorId = user.uid,
                authorName = profile.name,
                authorPhotoUrl = profile.photoUrl,
                content = finalContent,
                imageUrl = uploadedImageUrl,
                tags = tags
            )

            android.util.Log.d("FeedViewModel", "Creating post with imageUrl=$uploadedImageUrl")

            val result = feedRepository.createPost(post)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isPosting = false, postCreated = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        error = e.localizedMessage
                    )
                }
            )
        }
    }

    fun clearPostCreated() {
        _uiState.value = _uiState.value.copy(postCreated = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            feedRepository.toggleLike(postId, uid)
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            feedRepository.getComments(postId).collect { comments ->
                _comments.value = comments
            }
        }
    }

    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            val user = authRepository.currentUser ?: return@launch
            val profileResult = authRepository.getUserProfile(user.uid)
            val profile = profileResult.getOrNull() ?: return@launch

            val comment = Comment(
                postId = postId,
                authorId = user.uid,
                authorName = profile.name,
                authorPhotoUrl = profile.photoUrl,
                content = content
            )

            feedRepository.addComment(postId, comment)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            feedRepository.deletePost(postId)
        }
    }
}
