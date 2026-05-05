package com.myapp.gymstats.di

import com.myapp.gymstats.data.repository.WorkoutRepositoryImpl
import com.myapp.gymstats.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWorkoutRepository (
        impl: WorkoutRepositoryImpl
    ): WorkoutRepository
}