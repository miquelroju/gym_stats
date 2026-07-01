package com.myapp.gymstats.domain.model

object Score {
    fun calculate(weightKg: Float, reps: Int): Double =
        weightKg * (1.0 + reps / 30.0)

    fun bestForExercise(sets: List<WorkoutSet>, exerciseId: String): Double =
        sets.filter { it.exerciseId == exerciseId }
            .maxOfOrNull { calculate(it.weightKg, it.reps) } ?: 0.0
}