package com.myapp.gymstats.di

import android.content.Context
import androidx.room.Room
import com.myapp.gymstats.data.local.AppDatabase
import com.myapp.gymstats.data.local.dao.ExerciseDao
import com.myapp.gymstats.data.local.dao.WorkoutSessionDao
import com.myapp.gymstats.data.local.dao.WorkoutSetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "gymstats.db"
    ).build()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideWorkoutSessionDao(db: AppDatabase): WorkoutSessionDao = db.workoutSessionDao()

    @Provides
    fun provideWorkoutSetDao(db: AppDatabase): WorkoutSetDao = db.workoutSetDao()
}