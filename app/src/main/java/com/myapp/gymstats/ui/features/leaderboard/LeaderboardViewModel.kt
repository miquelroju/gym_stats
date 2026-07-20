package com.myapp.gymstats.ui.features.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.LeaderboardEntry
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val MUSCLE_GROUPS = listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core")

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val entries: List<LeaderboardEntry> = emptyList(),
    val selectedGroup: String = MUSCLE_GROUPS.first(),
    val friendsOnly: Boolean = false
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun init(userId: String) {
        currentUserId = userId
        loadLeaderboard()
    }

    fun selectMuscleGroup(group: String) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
        loadLeaderboard()
    }

    fun toggleFriendsOnly(friendsOnly: Boolean) {
        _uiState.value = _uiState.value.copy(friendsOnly = friendsOnly)
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        val group = _uiState.value.selectedGroup
        val friendsOnly = _uiState.value.friendsOnly
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val entries = if (friendsOnly && currentUserId.isNotBlank()) {
                repository.getLeaderboardFriends(currentUserId, group)
            } else {
                repository.getLeaderboard(group)
            }
            _uiState.value = _uiState.value.copy(isLoading = false, entries = entries)
        }
    }
}