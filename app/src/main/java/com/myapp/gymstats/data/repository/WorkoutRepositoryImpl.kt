package com.myapp.gymstats.data.repository

import android.util.Log
import com.myapp.gymstats.data.local.dao.ExerciseDao
import com.myapp.gymstats.data.local.dao.WorkoutSessionDao
import com.myapp.gymstats.data.local.dao.WorkoutSetDao
import com.myapp.gymstats.data.local.entity.toDomain
import com.myapp.gymstats.data.local.entity.toEntity
import com.myapp.gymstats.data.remote.CheckinFeedDto
import com.myapp.gymstats.data.remote.DailyCheckinDto
import com.myapp.gymstats.data.remote.ExerciseDto
import com.myapp.gymstats.data.remote.LeaderboardEntryDto
import com.myapp.gymstats.data.remote.SupabaseClientProvider
import com.myapp.gymstats.data.remote.UserProfileDto
import com.myapp.gymstats.data.remote.UserSettingsDto
import com.myapp.gymstats.data.remote.toDto
import com.myapp.gymstats.domain.model.CheckinFeedEntry
import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.LeaderboardEntry
import com.myapp.gymstats.domain.model.UserSettings
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.model.WorkoutSet
import com.myapp.gymstats.domain.repository.WorkoutRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
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

    // -------
    override suspend fun getLeaderboard(muscleGroup: String): List<LeaderboardEntry> {
        return runCatching {
            val params = buildJsonObject {
                put("p_muscle_group", muscleGroup)
            }
            client.postgrest.rpc("get_leaderboard", params)
                .decodeList<LeaderboardEntryDto>()
                .map {
                    LeaderboardEntry(
                        username = it.username,
                        exerciseName = it.exercise,
                        bestScore = it.bestScore,
                        userId = it.userId
                    )
                }
        }.getOrElse {
            Log.w("WorkoutRepo", "Leaderboard fetch failed: ${it.message}")
            emptyList()
        }
    }

    override suspend fun saveUserProfile(userId: String, username: String) {
        runCatching {
            client.from("user_profiles").upsert(
                UserProfileDto(id = userId, username = username)
            )
        }.onFailure { Log.w("WorkoutRepo", "Profile save failed: ${it.message}") }
    }

    override suspend fun getUserProfile(userId: String): String? {
        return runCatching {
            client.from("user_profiles")
                .select{
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserProfileDto>()?.username
        }.getOrNull()
    }

    override suspend fun getSessionWithSets(sessionId: String): WorkoutSession {
        val session = sessionDao.getSessionById(sessionId)
            ?: return WorkoutSession("", "", "", "")
        /* val sets = setDao.getSetsBySession(sessionId)
            .map { entities -> entities.map { it.toDomain() } }
            .let { flow ->
                var result = emptyList<WorkoutSet>()
                flow.collect { result = it }
                result
            }
        */
        val sets = setDao.getSetsBySession(sessionId).first()
            .map { it.toDomain() }
        return session.toDomain().copy(sets = sets)
    }
    // --- Check-in social -----------------------------------------
    override suspend fun checkInToday(userId: String) {
        runCatching {
            val today = LocalDate.now().toString()
            client.from("daily_checkins").insert(
                DailyCheckinDto(userId = userId, date = today)
            )
        }.onFailure{ Log.w("WorkoutRepo", "Checkin failed: ${it.message}") }
    }

    override suspend fun hasCheckedInToday(userId: String): Boolean {
        return runCatching {
            val today = LocalDate.now().toString()
            client.from("daily_checkins")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }
                .decodeList<DailyCheckinDto>()
                .isNotEmpty()
        }.getOrElse { false }
    }

    override suspend fun getCheckinFeed(): List<CheckinFeedEntry> {
        return runCatching {
            client.postgrest.rpc("get_checkin_feed")
                .decodeList<CheckinFeedDto>()
                .map {
                    CheckinFeedEntry(
                        userId = it.userId,
                        username = it.username,
                        avatarEmoji = it.avatarEmoji,
                        checkedIn = it.checkedIn
                    )
                }
        }.getOrElse { emptyList() }
    }

    override suspend fun getUserStreak(userId: String): Int {
        return runCatching {
            val params = buildJsonObject { put("p_user_id", userId) }
            val result = client.postgrest.rpc("get_user_streak", params)
            result.data.trim().toIntOrNull() ?: 0
        }.getOrElse {
            Log.e("WorkoutRepo", "Streak fetch failed: ${it.message}")
            0
        }
    }

    // --- Configuració --------------------------------------------
    override suspend fun getUserSettings(userId: String): UserSettings {
        return runCatching {
            withTimeout(8000) {
                client.from("user_settings")
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserSettingsDto>()
                    ?.let {
                        UserSettings(
                            restTimerSeconds = it.restTimerSeconds,
                            expectedGapDays = it.expectedGapDays,
                            graceDays = it.graceDays,
                            notificationsEnabled = it.notificationsEnabled
                        )
                    } ?: UserSettings()
            }
        }.getOrElse { UserSettings() }
    }

    override suspend fun saveUserSettings(userId: String, settings: UserSettings) {
        runCatching {
            client.from("user_settings").upsert(
                UserSettingsDto(
                    userId = userId,
                    restTimerSeconds = settings.restTimerSeconds,
                    expectedGapDays = settings.expectedGapDays,
                    graceDays = settings.graceDays,
                    notificationsEnabled = settings.notificationsEnabled
                )
            )
        }.onFailure { Log.w("WorkoutRepo", "Settings save failed: ${it.message}") }
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