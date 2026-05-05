package com.myapp.gymstats.data.repository

import android.util.Log
import com.myapp.gymstats.data.local.dao.ExerciseDao
import com.myapp.gymstats.data.local.dao.WorkoutSessionDao
import com.myapp.gymstats.data.local.dao.WorkoutSetDao
import com.myapp.gymstats.data.local.entity.toDomain
import com.myapp.gymstats.data.local.entity.toEntity
import com.myapp.gymstats.data.remote.ExerciseDto
import com.myapp.gymstats.data.remote.SupabaseClientProvider
import com.myapp.gymstats.data.remote.toDto
import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.model.WorkoutSet
import com.myapp.gymstats.domain.repository.WorkoutRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
    private val supabaseProvider: SupabaseClientProvider
) : WorkoutRepository  {
    private val client get() = supabaseProvider.client

    // --- Exercicis -----------------------------------------------
    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAllExercises().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun syncExercisesFromRemote() {
        runCatching {
            val remote = client.from("exercises")
                .select()
                .decodeList<ExerciseDto>()
            exerciseDao.insertAll(remote.map { it.toEntity() })
        }.onFailure { e ->
            Log.w("WorkoutRepo", "Exercise sync failed: ${e.message}")
        }
    }

    // --- Sessions ------------------------------------------------
    override fun getSessionsByUser(userId: String): Flow<List<WorkoutSession>> =
        sessionDao.getSessionsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveSession(session: WorkoutSession) {
        sessionDao.insert(session.toEntity())
    }

    override suspend fun deleteSession(sessionId: String) {
        setDao.deleteSetsBySession(sessionId)
        sessionDao.deleteSession(sessionId)
    }

    // --- Sets ----------------------------------------------------
    override fun getSetsBySession(sessionId: String): Flow<List<WorkoutSet>> =
        combine(
            setDao.getSetsBySession(sessionId),
            exerciseDao.getAllExercises()
        ) {
            sets, exercises ->
            val exerciseMap = exercises.associate { it.id to it.name }
            sets.map { it.toDomain(exerciseMap[it.exerciseId] ?: "") }
        }

    override suspend fun saveSet(set: WorkoutSet) {
        setDao.insert(set.toEntity())
    }

    override suspend fun deleteSetsBySession(sessionId: String) {
        setDao.deleteSetsBySession(sessionId)
    }

    // --- Progrés històric ----------------------------------------
    override fun getExerciseProgress(
        exerciseId: String,
        userId: String
    ): Flow<List<WorkoutSet>> =
        setDao.getSetsByExerciseAndUser(exerciseId, userId).map { sets ->
            sets.map { it.toDomain() }
        }

    // --- Sincro amb Supabase -------------------------------------
    override suspend fun syncPendingData() {
        runCatching {
            // Sincronitzar sessions pendents
            val unsyncedSessions = sessionDao.getUnsyncedSessions()
            unsyncedSessions.forEach { session ->
                client.from("workout_sessions").upsert(session.toDto())
                sessionDao.markAsSynced(session.id)
            }

            // Sincronitzar sets pendents
            val unsyncedSets = setDao.getUnsyncedSets()
            if (unsyncedSets.isNotEmpty()) {
                client.from("workout_sets").upsert(unsyncedSets.map { it.toDto() })
                unsyncedSets.forEach {
                    setDao.markSessionSetsAsSynced(it.sessionId)
                }
            }
        }.onFailure { e ->
            // Error silenciós: les dades queden a room amb synced = false
            // i es reintentarà la pròxima vegada que es cridi a syncPendingData()
            Log.w("WorkoutRepo", "Sync failed, will retry later: ${e.message}")
        }
    }
}