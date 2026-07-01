package com.myapp.gymstats.ui.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.Score
import com.myapp.gymstats.domain.model.WorkoutSet
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseBest (
    val exercise: Exercise,
    val bestScore: Double,
    val bestReps: Int,
    val bestKg: Float
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val bestsByMuscleGroup: Map<String, List<ExerciseBest>> = emptyMap(),
    val selectedGroup: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            val exercises = repository.getAllExercises().first()
            val result = mutableMapOf<String, MutableList<ExerciseBest>>()

            exercises.forEach { exercise ->
                val sets = repository.getExerciseProgress(exercise.id, userId).first()

                if (sets.isNotEmpty()) {
                    val best =  sets.maxByOrNull { Score.calculate(it.weightKg, it.reps) }!!

                    val group = exercise.muscleGroup
                    result.getOrPut(group) { mutableListOf() }.add(
                        ExerciseBest(
                            exercise = exercise,
                            bestScore = Score.calculate(best.weightKg, best.reps),
                            bestReps = best.reps,
                            bestKg = best.weightKg
                        )
                    )
                }
            }

            _uiState.value = StatsUiState(
                isLoading = false,
                bestsByMuscleGroup = result,
                selectedGroup = result.keys.firstOrNull()
            )
        }
    }

    fun selectGroup(group: String) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
    }
}
