package com.timsfit.core

import java.time.LocalDate
import java.time.ZoneId

object WorkoutEngine {
    fun nextSplit(state: AppState): Split = TODO()
    fun createWorkout(state: AppState, now: Long, id: String): AppState = TODO()
    fun startWorkout(state: AppState, workoutId: String, now: Long): AppState = TODO()
    fun updateSet(state: AppState, workoutId: String, exerciseId: String, setIndex: Int, weightLb: Double, reps: Int, completed: Boolean): AppState = TODO()
    fun finishWorkout(state: AppState, workoutId: String, now: Long): AppState = TODO()
    fun discardWorkout(state: AppState, workoutId: String): AppState = TODO()
    fun completedDayCounts(state: AppState, zone: ZoneId): Map<LocalDate, Int> = TODO()
}
