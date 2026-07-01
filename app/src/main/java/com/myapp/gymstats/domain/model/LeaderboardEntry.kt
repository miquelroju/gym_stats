package com.myapp.gymstats.domain.model

data class LeaderboardEntry(
    val username: String,
    val exerciseName: String,
    val bestScore: Double,
    val userId: String
)