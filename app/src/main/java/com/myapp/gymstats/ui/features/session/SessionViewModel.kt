package com.myapp.gymstats.ui.features.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.model.WorkoutSet
import com.myapp.gymstats.domain.usecase.SaveWorkoutSessionUseCase
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class SessionUiState(
    val isLoading: Boolean = false,
    val isSyncingExercises: Boolean = true,
    val exercises: List<Exercise> = emptyList(),
    val currentSets: List<WorkoutSet> = emptyList(),
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val saveWorkoutSession: SaveWorkoutSessionUseCase,
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        syncAndLoadExercises()
    }

    private fun syncAndLoadExercises() {
        viewModelScope.launch {
            repository.syncExercisesFromRemote()
            _uiState.value = _uiState.value.copy(isSyncingExercises = false)
        }
        viewModelScope.launch {
            repository.getAllExercises().collect { exercises ->
                _uiState.value = _uiState.value.copy(
                    exercises = exercises,
                    isSyncingExercises = exercises.isEmpty())
            }
        }
    }

    fun addSet(exerciseId: String, exerciseName: String, reps: Int, weightKg: Float) {
        val currentSets = _uiState.value.currentSets
        val setsForExercise = currentSets.count { it.exerciseId == exerciseId }
        val newSet = WorkoutSet(
            id = UUID.randomUUID().toString(),
            sessionId = "",
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            setNumber = setsForExercise + 1,
            reps = reps,
            weightKg = weightKg
        )
        _uiState.value = _uiState.value.copy(
            currentSets = currentSets + newSet
        )
    }

    fun removeSet(setId: String) {
        _uiState.value = _uiState.value.copy(
            currentSets = _uiState.value.currentSets.filter { it.id != setId }
        )
    }

    fun saveSession(userId: String, notes: String = "") {
        val sets = _uiState.value.currentSets
        if (sets.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSession(
                id = sessionId,
                userId = userId,
                date = LocalDate.now().toString(),
                notes = notes
            )
            val setsWithSession = sets.map { it.copy(sessionId = sessionId) }

            runCatching {
                saveWorkoutSession(session, setsWithSession)
                _uiState.value = SessionUiState(isSaved = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al guardar la sesión"
                )
            }
        }
    }
}