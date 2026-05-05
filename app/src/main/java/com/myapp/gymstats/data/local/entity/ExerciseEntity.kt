package com.myapp.gymstats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    val id: String,                 // UUID String, sincronitzable amb Supabase
    val name: String,
    val muscleGroup: String,
    val equipment: String = ""
)