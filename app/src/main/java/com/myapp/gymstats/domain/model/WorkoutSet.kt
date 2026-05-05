package com.myapp.gymstats.domain.model

data class WorkoutSet(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,   // desnormalitzat per a mostrar-se a UI sense joins extra
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float
)