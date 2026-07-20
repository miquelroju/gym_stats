package com.myapp.gymstats.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val username: String = "",
    val avatarEmoji: String = "💪",
    val friendCode: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun load(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val username = repository.getUserProfile(userId) ?: ""
            val avatarEmoji = repository.getUserAvatarEmoji(userId) ?: "💪"
            val friendCode = repository.getMyFriendCode(userId) ?: ""
            _uiState.value = ProfileUiState(
                isLoading = false,
                username = username,
                avatarEmoji = avatarEmoji,
                friendCode = friendCode
            )
        }
    }

    fun updateUsername(value: String) {
        _uiState.value = _uiState.value.copy(username = value, saveSuccess = false)
    }

    fun updateAvatarEmoji(value: String) {
        _uiState.value = _uiState.value.copy(avatarEmoji = value, saveSuccess = false)
    }

    fun save() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            runCatching {
                repository.saveUserProfile(
                    currentUserId,
                    _uiState.value.username.trim(),
                    _uiState.value.avatarEmoji
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Error al guardar. Inténtalo de nuevo."
                )
            }
        }
    }
}