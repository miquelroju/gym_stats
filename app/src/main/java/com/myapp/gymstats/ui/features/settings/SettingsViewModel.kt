package com.myapp.gymstats.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.UserSettings
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val isSaved: Boolean = false,
    val username: String = "",
    val avatarEmoji: String = "\uD83D\uDCAA",
    val profileSaved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var userId: String = ""

    fun load(userId: String) {
        if (userId.isBlank()) return
        this.userId = userId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val settings = repository.getUserSettings(userId)
                val username = repository.getUserProfile(userId) ?: ""
                val avatarEmoji = repository.getUserAvatarEmoji(userId) ?: "💪"
                _uiState.value = SettingsUiState(
                    isLoading = false,
                    settings = settings,
                    username = username,
                    avatarEmoji = avatarEmoji
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error loading settings", e)
                _uiState.value = SettingsUiState(isLoading = false)
            }
        }
    }

    fun updateUsername(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun updateAvatarEmoji(value: String) {
        _uiState.value = _uiState.value.copy(avatarEmoji = value)
    }

    fun saveProfile() {
        viewModelScope.launch {
            repository.saveUserProfile(userId, _uiState.value.username, _uiState.value.avatarEmoji)
            _uiState.value = _uiState.value.copy(profileSaved = true)
            delay(1500)
            _uiState.value = _uiState.value.copy(profileSaved = false)
        }
    }

    fun updateRestTimer(seconds: Int) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(restTimerSeconds = seconds)
        )
    }

    fun updateExpectedGap(days: Int) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(expectedGapDays = days)
        )
    }

    fun updateGraceDays(days: Int) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(graceDays = days)
        )
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(notificationsEnabled = enabled)
        )
    }

    fun save() {
        viewModelScope.launch {
            repository.saveUserSettings(userId, _uiState.value.settings)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun onSavedDismissed() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }
}