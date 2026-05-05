package com.myapp.gymstats.domain.usecase

import com.myapp.gymstats.domain.model.WorkoutSet
import com.myapp.gymstats.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExerciseProgressUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    operator fun invoke(exerciseId: String, userId: String): Flow<List<WorkoutSet>> =
        repository.getExerciseProgress(exerciseId, userId)
}