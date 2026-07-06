package com.myapp.gymstats.domain.repository

import com.myapp.gymstats.domain.model.CheckinFeedEntry
import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.LeaderboardEntry
import com.myapp.gymstats.domain.model.UserSettings
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

    // Leaderboard
    suspend fun getLeaderboard(muscleGroup: String): List<LeaderboardEntry>

    // Perfil
    suspend fun saveUserProfile(userId: String, username: String)
    suspend fun getUserProfile(userId: String): String? // returns username

    // Historial amb sets inclosos (per a pantalla de detall)
    suspend fun getSessionWithSets(sessionId: String): WorkoutSession

    // Check-in social
    suspend fun checkInToday(userId: String)
    suspend fun hasCheckedInToday(userId: String): Boolean
    suspend fun getCheckinFeed(): List<CheckinFeedEntry>
    suspend fun getUserStreak(userId: String): Int

    // Configuració
    suspend fun getUserSettings(userId: String): UserSettings
    suspend fun saveUserSettings(userId: String, settings: UserSettings)

    // Sincro amb Supabase
    suspend fun syncPendingData()
}
