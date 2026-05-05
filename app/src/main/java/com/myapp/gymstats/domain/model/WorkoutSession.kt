package com.myapp.gymstats.domain.model

data class WorkoutSession(
    val id: String,
    val userId: String,
    val date: String,
    val notes: String = "",
    val sets: List<WorkoutSet> = emptyList()
)