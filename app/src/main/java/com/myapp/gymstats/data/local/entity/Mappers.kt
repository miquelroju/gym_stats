package com.myapp.gymstats.data.local.entity

import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.WorkoutSession
import com.myapp.gymstats.domain.model.WorkoutSet

// --- Exercises ------------------------------
fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment
)

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment
)

// --- WorkoutSession -------------------------
fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = id,
    userId = userId,
    date = date,
    notes = notes
)

fun WorkoutSession.toEntity() = WorkoutSessionEntity(
    id = id,
    userId = userId,
    date = date,
    notes = notes,
    synced = false
)

// --- WorkoutSet -------------------------
fun WorkoutSetEntity.toDomain(exerciseName: String = "") = WorkoutSet(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg
)

fun WorkoutSet.toEntity() = WorkoutSetEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg,
    synced = false
)

