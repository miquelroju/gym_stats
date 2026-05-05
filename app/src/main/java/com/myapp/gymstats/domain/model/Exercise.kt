package com.myapp.gymstats.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String = ""
)