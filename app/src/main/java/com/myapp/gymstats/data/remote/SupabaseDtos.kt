package com.myapp.gymstats.data.remote

import com.myapp.gymstats.data.local.entity.ExerciseEntity
import com.myapp.gymstats.data.local.entity.WorkoutSessionEntity
import com.myapp.gymstats.data.local.entity.WorkoutSetEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDto (
    val id: String,
    val name: String,
    @SerialName("muscle_group") val muscleGroup: String,
    val equipment: String = ""
) {
    fun toEntity() = ExerciseEntity(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        equipment = equipment
    )
}

@Serializable
data class WorkoutSessionDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val notes: String = ""
)

@Serializable
data class WorkoutSetDto(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("set_number") val setNumber: Int,
    val reps: Int,
    @SerialName("weight_kg") val weightKg: Float
)

// Extensions de conversió Entity → DTO
fun WorkoutSessionEntity.toDto() = WorkoutSessionDto(
    id = id,
    userId = userId,
    date = date,
    notes = notes
)

fun WorkoutSetEntity.toDto() = WorkoutSetDto(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg
)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String
)

@Serializable
data class LeaderboardEntryDto(
    val username: String,
    val exercise: String,
    @SerialName("best_score") val bestScore: Double,
    @SerialName("user_id") val userId: String
)