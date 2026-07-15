package com.myapp.gymstats.widget

data class StreakWidgetState(
    val streak: Int = 0,
    val checkedInToday: Boolean = false,
    val isLoading: Boolean = true
)