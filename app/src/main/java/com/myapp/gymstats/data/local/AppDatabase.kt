package com.myapp.gymstats.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myapp.gymstats.data.local.dao.ExerciseDao
import com.myapp.gymstats.data.local.dao.WorkoutSessionDao
import com.myapp.gymstats.data.local.dao.WorkoutSetDao
import com.myapp.gymstats.data.local.entity.ExerciseEntity
import com.myapp.gymstats.data.local.entity.WorkoutSessionEntity
import com.myapp.gymstats.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSetDao(): WorkoutSetDao
}