package com.myapp.gymstats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.myapp.gymstats.ui.navigation.NavGraph
import com.myapp.gymstats.ui.theme.GymStatsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymStatsTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}