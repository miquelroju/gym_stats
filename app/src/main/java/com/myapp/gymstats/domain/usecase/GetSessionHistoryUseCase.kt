package com.myapp.gymstats.domain.usecase

import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSessionHistoryUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    operator fun invoke(userId: String): Flow<List<WorkoutSession>> =
        repository.getSessionsByUser(userId)
}