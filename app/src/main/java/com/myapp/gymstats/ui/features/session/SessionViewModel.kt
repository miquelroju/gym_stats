package com.myapp.gymstats.ui.features.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.Score
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.model.WorkoutSet
import com.myapp.gymstats.domain.usecase.SaveWorkoutSessionUseCase
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val error: String? = null,
    val motivationMessage: String? = null,
    val restTimerSeconds: Int = 90,
    val timerRemaining: Int = 0,
    val isTimerRunning: Boolean = false
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
                    isSyncingExercises = exercises.isEmpty()
                )
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSession(
                id = sessionId,
                userId = userId,
                date = LocalDate.now().toString(),
                notes = notes
            )
            val setsWithSession = sets.map { it.copy(sessionId = sessionId) }

            try {
                saveWorkoutSession(session, setsWithSession)

                val messages = mutableListOf<String>()
                val exerciseIds = setsWithSession.map { it.exerciseId }.distinct()

                exerciseIds.forEach { exerciseId ->
                    val exerciseName = setsWithSession
                        .first { it.exerciseId == exerciseId }.exerciseName
                    val currentBest = Score.bestForExercise(setsWithSession, exerciseId)

                    val prevSets = repository.getExerciseProgress(exerciseId, userId).first()

                    val previousBest = prevSets
                        .filter { it.sessionId != sessionId }
                        .groupBy { it.sessionId }
                        .mapValues { (_, s) -> Score.bestForExercise(s, exerciseId) }
                        .values.maxOrNull() ?: 0.0

                    if (previousBest > 0.0 && currentBest > previousBest) {
                        val improvement = String.format("%.1f", currentBest - previousBest)
                        messages.add("💪 $exerciseName: +$improvement pts de mejora!")
                    }
                }

                _uiState.value = SessionUiState(
                    isSaved = messages.isEmpty(),
                    motivationMessage = if (messages.isNotEmpty()) messages.joinToString("\n") else null
                )
            } catch (e: Exception) {
                android.util.Log.e("SessionViewModel", "Error saving session", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al guardar la sesión"
                )
            }
        }
    }

    fun onMotivationDismissed() {
        _uiState.value = _uiState.value.copy(
            isSaved = true,
            motivationMessage = null
        )
    }

    private var timerJob: Job? = null

    fun loadRestTimerSetting(userId: String) {
        viewModelScope.launch {
            try {
                val settings = repository.getUserSettings(userId)
                _uiState.value = _uiState.value.copy(
                    restTimerSeconds = settings.restTimerSeconds
                )
            } catch (e: Exception) {
                android.util.Log.e("SessionViewModel", "Error loading rest timer setting", e)
            }
        }
    }

    fun startRestTimer() {
        timerJob?.cancel()
        val totalSeconds = _uiState.value.restTimerSeconds
        _uiState.value = _uiState.value.copy(
            timerRemaining = totalSeconds,
            isTimerRunning = true
        )

        timerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(timerRemaining = remaining)
            }
            _uiState.value = _uiState.value.copy(isTimerRunning = false)
        }
    }

    fun cancelRestTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isTimerRunning = false,
            timerRemaining = 0
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}