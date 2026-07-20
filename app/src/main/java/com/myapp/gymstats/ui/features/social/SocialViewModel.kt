package com.myapp.gymstats.ui.features.social

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.CheckinFeedEntry
import com.myapp.gymstats.domain.model.Friend
import com.myapp.gymstats.domain.model.FriendSearchResult
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
    val isCheckingIn: Boolean = false,
    val myFriendCode: String = "",
    val searchCode: String = "",
    val searchResult: FriendSearchResult? = null,
    val searchError: String? = null,
    val isSearching: Boolean = false,
    val friendAdded: Boolean = false,
    val friends: List<Friend> = emptyList(),
    val isLoadingFriends: Boolean = false
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
                val feed = repository.getCheckinFeedFriends(userId)
                val friendCode = repository.getMyFriendCode(userId) ?: ""
                val checkedIn = repository.hasCheckedInToday(userId)
                val streak = repository.getUserStreak(userId)
                val friends = repository.getMyFriends(userId)

                _uiState.value = SocialUiState(
                    isLoading = false,
                    feed = feed,
                    hasCheckedInToday = checkedIn,
                    myStreak = streak,
                    myFriendCode = friendCode,
                    friends = friends
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

    fun updateSearchCode(code: String) {
        _uiState.value = _uiState.value.copy(searchCode = code, searchResult = null, searchError = null)
    }

    fun searchFriend() {
        val code = _uiState.value.searchCode.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchError = null)
            val result = repository.findUserByFriendCode(code)
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                searchResult = result,
                searchError = if (result == null) "No se encontró ningún usuario con ese código" else null
            )
        }
    }

    fun addFriend(friendId: String) {
        viewModelScope.launch {
            repository.addFriend(currentUserId, friendId)
            _uiState.value = _uiState.value.copy(friendAdded = true, searchResult = null, searchCode = "")
            load(currentUserId)
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            repository.removeFriend(currentUserId, friendId)
            load(currentUserId)
        }
    }
}
