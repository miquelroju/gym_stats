package com.myapp.gymstats.widget

import android.content.Context
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetRepositoryEntryPoint {
    fun workoutRepository(): WorkoutRepository
}

object WidgetEntryPoint {
    fun getRepository(context: Context): WorkoutRepository {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WidgetRepositoryEntryPoint::class.java
        )
        return entryPoint.workoutRepository()
    }

    // Guarda el userId actual en SharedPreferences cuando el usuario inicia sesión
    fun getCurrentUserId(context: Context): String {
        val prefs = context.getSharedPreferences("gymstats_prefs", Context.MODE_PRIVATE)
        return prefs.getString("current_user_id", "") ?: ""
    }

    fun saveCurrentUserId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences("gymstats_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("current_user_id", userId).apply()
    }
}