package com.myapp.gymstats.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.repository.WorkoutRepository
import com.myapp.gymstats.domain.usecase.GetSessionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState (
    val isLoading: Boolean = true,
    val recentSessions: List<WorkoutSession> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSessionHistory: GetSessionHistoryUseCase,
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun loadSessions(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            getSessionHistory(userId).collect { sessions ->
                _uiState.value = HomeUiState(
                    isLoading = false,
                    recentSessions = sessions.take(5)
                )
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
