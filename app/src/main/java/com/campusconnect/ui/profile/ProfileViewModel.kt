package com.campusconnect.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.data.model.User
import com.campusconnect.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUser?.uid

    init {
        loadProfile()
    }

    fun loadProfile(userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val uid = userId ?: authRepository.currentUser?.uid
            if (uid == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            val result = authRepository.getUserProfile(uid)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = ProfileUiState(user = user, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        error = e.localizedMessage
                    )
                }
            )
        }
    }

    fun updateProfile(updates: Map<String, Any>) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = authRepository.updateUserProfile(uid, updates)
            result.fold(
                onSuccess = {
                    loadProfile()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.localizedMessage
                    )
                }
            )
        }
    }

    fun uploadPhoto(imageUri: Uri) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            _uiState.value = _uiState.value.copy(isUploadingPhoto = true, error = null)
            val result = authRepository.uploadProfilePhoto(uid, imageUri)
            result.fold(
                onSuccess = {
                    // Reload profile so UI shows the new photo immediately
                    loadProfile()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingPhoto = false,
                        error = "Photo upload failed: ${e.localizedMessage}"
                    )
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
