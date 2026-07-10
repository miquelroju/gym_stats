package com.myapp.gymstats.ui.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.usecase.GetSessionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<WorkoutSession> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getSessionHistory: GetSessionHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun load(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val sessions = getSessionHistory(userId).first()
                _uiState.value = HistoryUiState(isLoading = false, sessions = sessions)
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error loading history", e)
                _uiState.value = HistoryUiState(isLoading = false, sessions = emptyList())
            }
        }
    }
}