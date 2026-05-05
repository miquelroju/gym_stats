package com.myapp.gymstats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: String,             // UUID
    val userId: String,
    val date: String,           // Format yyyy-MM-dd
    val notes: String = "",
    val synced: Boolean = false // false = pendiente de subir a Supabase
)