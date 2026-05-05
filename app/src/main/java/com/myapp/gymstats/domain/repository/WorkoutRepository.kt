package com.myapp.gymstats.domain.repository

import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    // Exercicis
    fun getAllExercises(): Flow<List<Exercise>>
    suspend fun syncExercisesFromRemote()

    // Sessions
    fun getSessionsByUser(userId: String): Flow<List<WorkoutSession>>
    suspend fun saveSession(session: WorkoutSession)
    suspend fun deleteSession(sessionId: String)

    // Sets en una sessió
    fun getSetsBySession(sessionId: String): Flow<List<WorkoutSet>>
    suspend fun saveSet(set: WorkoutSet)
    suspend fun deleteSetsBySession(sessionId: String)

    // Progrés històric d'un exercici
    fun getExerciseProgress(
        exerciseId: String,
        userId: String
    ): Flow<List<WorkoutSet>>

    // Sincro amb Supabase
    suspend fun syncPendingData()
}
