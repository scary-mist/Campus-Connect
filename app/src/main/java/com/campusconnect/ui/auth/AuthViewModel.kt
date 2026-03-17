package com.campusconnect.ui.auth

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

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthChecking: Boolean = true,  // true until initial auth check completes
    val isLoggedIn: Boolean = false,
    val isOnboarded: Boolean = false,
    val error: String? = null,
    val userId: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            if (user != null) {
                val profileResult = authRepository.getUserProfile(user.uid)
                val profile = profileResult.getOrNull()
                _uiState.value = AuthUiState(
                    isAuthChecking = false,
                    isLoggedIn = true,
                    isOnboarded = profile?.isOnboarded == true,
                    userId = user.uid
                )
            } else {
                _uiState.value = AuthUiState(isAuthChecking = false)
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithEmail(email, password)
            result.fold(
                onSuccess = { user ->
                    val profileResult = authRepository.getUserProfile(user.uid)
                    val profile = profileResult.getOrNull()
                    _uiState.value = AuthUiState(
                        isAuthChecking = false,
                        isLoggedIn = true,
                        isOnboarded = profile?.isOnboarded == true,
                        userId = user.uid
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Sign in failed"
                    )
                }
            )
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.registerWithEmail(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState(
                        isAuthChecking = false,
                        isLoggedIn = true,
                        isOnboarded = false,
                        userId = user.uid
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Registration failed"
                    )
                }
            )
        }
    }

    fun completeOnboarding(name: String, college: String, department: String, year: Int, bio: String, interests: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val uid = authRepository.currentUser?.uid ?: return@launch
            val email = authRepository.currentUser?.email ?: ""

            val user = User(
                uid = uid,
                name = name,
                email = email,
                college = college,
                department = department,
                year = year,
                bio = bio,
                interests = interests,
                isOnboarded = true
            )

            val result = authRepository.createUserProfile(user)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState(
                        isAuthChecking = false,
                        isLoggedIn = true,
                        isOnboarded = true,
                        userId = uid
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to save profile"
                    )
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState(isAuthChecking = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
