package com.myapp.gymstats.ui.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.usecase.GetSessionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<WorkoutSession> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getSessionHistory: GetSessionHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadSessions(userId: String) {
        viewModelScope.launch {
            getSessionHistory(userId).collect { sessions ->
                _uiState.value = HistoryUiState(
                    isLoading = false,
                    sessions = sessions
                )
            }
        }
    }
}