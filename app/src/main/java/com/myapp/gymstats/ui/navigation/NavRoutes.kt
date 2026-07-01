package com.myapp.gymstats.ui.navigation

sealed class NavRoutes(val route: String) {
    object Login        : NavRoutes("login")
    object Home         : NavRoutes("home")
    object Session      : NavRoutes("session")
    object History      : NavRoutes("history")
    object Leaderboard  : NavRoutes("leaderboard")
    object Stats        : NavRoutes("stats")
}