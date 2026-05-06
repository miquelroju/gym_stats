package com.myapp.gymstats.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.myapp.gymstats.ui.features.auth.AuthViewModel
import com.myapp.gymstats.ui.features.auth.LoginScreen
import com.myapp.gymstats.ui.features.history.HistoryScreen
import com.myapp.gymstats.ui.features.home.HomeScreen
import com.myapp.gymstats.ui.features.session.SessionScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    val startDestination = if (authState.isAuthenticated)
        NavRoutes.Home.route else NavRoutes.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Home.route) {
            HomeScreen(
                userId = authState.userId,
                onNewSession = { navController.navigate(NavRoutes.Session.route) },
                onHistory = { navController.navigate(NavRoutes.History.route) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Session.route) {
            SessionScreen(
                userId = authState.userId,
                onSessionSaved = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.History.route) {
            HistoryScreen(
                userId = authState.userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}