package com.myapp.gymstats.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String = "",
    val muscles: List<Pair<String, Int>> = emptyList() // musculo, intensidad
)