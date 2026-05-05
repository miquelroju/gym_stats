package com.myapp.gymstats.domain.usecase

import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.model.WorkoutSet
import com.myapp.gymstats.domain.repository.WorkoutRepository
import javax.inject.Inject

class SaveWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(session: WorkoutSession, sets: List<WorkoutSet>) {
        repository.saveSession(session)
        sets.forEach { repository.saveSet(it) }
        repository.syncPendingData()
    }
}