package com.myapp.gymstats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myapp.gymstats.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    fun getSetsBySession(sessionId: String): Flow<List<WorkoutSetEntity>>

    // Per a veure el progrés històric d'un exercici concret
    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.sessionId = s.id
        WHERE ws.exerciseId = :exerciseId AND s.userId = :userId
        ORDER BY s.date DESC
    """)
    fun getSetsByExerciseAndUser(
        exerciseId: String,
        userId: String
    ): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE synced = 0")
    suspend fun getUnsyncedSets(): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<WorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: WorkoutSetEntity)

    @Query("UPDATE workout_sets SET synced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionSetsAsSynced(sessionId: String)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsBySession(sessionId: String)
}