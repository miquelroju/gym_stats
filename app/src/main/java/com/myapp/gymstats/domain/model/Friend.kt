package com.myapp.gymstats.domain.model

data class Friend(
    val userId: String,
    val username: String,
    val avatarEmoji: String,
    val friendCode: String
)