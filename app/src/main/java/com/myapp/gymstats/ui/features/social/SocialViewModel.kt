package com.myapp.gymstats.ui.features.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.CheckinFeedEntry
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialUiState(
    val isLoading: Boolean = true,
    val feed: List<CheckinFeedEntry> = emptyList(),
    val hasCheckedInToday: Boolean = false,
    val myStreak: Int = 0,
    val isCheckingIn: Boolean = false
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun load(userId: String) {
        if (userId.isBlank()) return
        currentUserId = userId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val feed = repository.getCheckinFeed()
            val checkedIn = repository.hasCheckedInToday(userId)
            val streak = repository.getUserStreak(userId)

            _uiState.value = SocialUiState(
                isLoading = false,
                feed = feed,
                hasCheckedInToday = checkedIn,
                myStreak = streak
            )
        }
    }

    fun checkIn() {
        if (currentUserId.isBlank() || _uiState.value.hasCheckedInToday) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingIn = true)
            repository.checkInToday(currentUserId)
            load(currentUserId) // recarga feed + racha actualizada
            _uiState.value = _uiState.value.copy(isCheckingIn = false)
        }
    }
}
