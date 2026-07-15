package com.myapp.gymstats.ui.features.social

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.CheckinFeedEntry
import com.myapp.gymstats.domain.repository.WorkoutRepository
import com.myapp.gymstats.widget.StreakGlanceWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val repository: WorkoutRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun load(userId: String) {
        if (userId.isBlank()) return
        currentUserId = userId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val feed = repository.getCheckinFeed()
                val checkedIn = repository.hasCheckedInToday(userId)
                val streak = repository.getUserStreak(userId)

                _uiState.value = SocialUiState(
                    isLoading = false,
                    feed = feed,
                    hasCheckedInToday = checkedIn,
                    myStreak = streak
                )
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "Error loading social data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    feed = emptyList()
                )
            }
        }
    }

    fun checkIn() {
        if (currentUserId.isBlank() || _uiState.value.hasCheckedInToday) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingIn = true)
            repository.checkInToday(currentUserId)
            load(currentUserId) // recarga feed + racha actualizada
            _uiState.value = _uiState.value.copy(isCheckingIn = false)

            StreakGlanceWidget().updateAll(context)
        }
    }
}
