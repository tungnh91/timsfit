package com.timsfit.core

enum class Split { PUSH, PULL, LEGS }
data class SetEntry(val weightLb: Double, val reps: Int, val completed: Boolean)
data class ExercisePlan(val id: String, val name: String, val targetReps: Int, val restSeconds: Int, val sets: List<SetEntry>, val note: String)
data class Workout(val id: String, val split: Split, val createdAt: Long, val startedAt: Long?, val completedAt: Long?, val exercises: List<ExercisePlan>, val estimatedMinutes: Int)
data class AppState(val workouts: List<Workout> = emptyList())
