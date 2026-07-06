package com.myapp.gymstats.domain.model

data class UserSettings(
    val restTimerSeconds: Int = 90,
    val expectedGapDays: Int = 1,
    val graceDays: Int = 1,
    val notificationsEnabled: Boolean = true
)