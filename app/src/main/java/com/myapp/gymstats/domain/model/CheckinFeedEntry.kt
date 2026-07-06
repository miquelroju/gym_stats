package com.myapp.gymstats.domain.model

data class CheckinFeedEntry (
    val userId: String,
    val username: String,
    val avatarEmoji: String,
    val checkedIn: Boolean
)