package com.timsfit.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pure state transitions. Callers persist the returned snapshot before reporting success. */
object WorkoutEngine {
    private data class Exercise(val id: String, val name: String, val reps: Int, val rest: Int, val increment: Double)
    private val plans = mapOf(
        Split.PUSH to listOf(
            Exercise("bench-press", "Barbell bench press", 8, 120, 5.0),
            Exercise("incline-dumbbell-press", "Incline dumbbell press", 10, 90, 5.0),
            Exercise("lateral-raise", "Dumbbell lateral raise", 12, 75, 2.5),
            Exercise("triceps-pushdown", "Cable triceps pushdown", 12, 90, 5.0)
        ),
        Split.PULL to listOf(
            Exercise("lat-pulldown", "Lat pulldown", 10, 120, 5.0),
            Exercise("cable-row", "Seated cable row", 10, 120, 5.0),
            Exercise("reverse-fly", "Dumbbell reverse fly", 12, 75, 2.5),
            Exercise("dumbbell-curl", "Dumbbell curl", 12, 75, 2.5)
        ),
        Split.LEGS to listOf(
            Exercise("squat", "Barbell squat", 8, 120, 5.0),
            Exercise("romanian-deadlift", "Dumbbell Romanian deadlift", 10, 120, 5.0),
            Exercise("leg-curl", "Machine leg curl", 12, 90, 5.0),
            Exercise("calf-raise", "Machine calf raise", 12, 90, 5.0)
        )
    )

    // Stable ID tie-break makes imported histories with equal timestamps deterministic.
    private fun latest(state: AppState, split: Split? = null): Workout? = state.workouts
        .filter { it.completedAt != null && (split == null || it.split == split) }
        .maxWithOrNull(compareBy<Workout> { it.completedAt }.thenBy { it.id })

    fun nextSplit(state: AppState): Split = when (latest(state)?.split) {
        null, Split.LEGS -> Split.PUSH
        Split.PUSH -> Split.PULL
        Split.PULL -> Split.LEGS
    }

    fun createWorkout(state: AppState, now: Long, id: String, selectedSplit: Split? = null): AppState {
        require(id.isNotBlank()) { "Workout ID must not be blank." }
        if (state.workouts.any { it.completedAt == null }) return state
        require(state.workouts.none { it.id == id }) { "Workout ID already exists: $id." }
        val workout = generateWorkout(state, now, id, selectedSplit ?: nextSplit(state))
        return state.copy(workouts = state.workouts + workout)
    }

    private fun generateWorkout(state: AppState, now: Long, id: String, split: Split): Workout {
        val history = latest(state, split)
        val exercises = plans.getValue(split).map { spec ->
            val prior = history?.exercises?.find { it.id == spec.id }
            val completed = prior?.sets.orEmpty().filter { it.completed }
            val fallback = completed.minOfOrNull { it.weightLb } ?: 0.0
            val increase = prior?.sets?.let { sets -> sets.size == 3 && sets.all { it.completed && it.reps >= spec.reps } } == true
            val sets = List(3) { index ->
                val previous = prior?.sets?.getOrNull(index)
                val weight = if (previous?.completed == true) previous.weightLb else fallback
                val suggested = if (increase && weight > 0.0) (weight + spec.increment).coerceAtMost(1500.0) else weight
                SetEntry(suggested, spec.reps, false)
            }
            val note = when {
                completed.isEmpty() -> "Choose your starting weight."
                increase && completed.any { it.weightLb > 0.0 && it.weightLb < 1500.0 } -> {
                    val increment = if (spec.increment % 1.0 == 0.0) spec.increment.toInt().toString() else spec.increment.toString()
                    "Targets met last time. Suggested +$increment lb."
                }
                else -> "Repeat your previous working weights."
            } + if (completed.isNotEmpty() && prior?.sets?.any { !it.completed } == true) {
                " Unfinished sets use your lightest completed load."
            } else ""
            val loadNote = if (spec.name.contains("dumbbell", ignoreCase = true)) " Weight is per hand." else ""
            ExercisePlan(spec.id, spec.name, spec.reps, spec.rest, sets, note + loadNote)
        }
        // Five-minute warmup, one minute setup/transition per exercise, 45 seconds
        // execution per set, and prescribed rest between sets (not after the last).
        val seconds = 5 * 60 + exercises.sumOf { 60 + it.sets.size * 45 + (it.sets.size - 1) * it.restSeconds }
        return Workout(id, split, now, null, null, exercises, (seconds + 59) / 60)
    }

    fun changeWorkoutSplit(state: AppState, workoutId: String, split: Split): AppState {
        val workout = findWorkout(state, workoutId)
        require(workout.startedAt == null && workout.completedAt == null) {
            "Only an unstarted workout can change its split."
        }
        if (workout.split == split) return state
        return replace(state, generateWorkout(state, workout.createdAt, workout.id, split))
    }

    private fun findWorkout(state: AppState, id: String): Workout =
        state.workouts.find { it.id == id } ?: throw IllegalArgumentException("Workout not found: $id.")

    private fun replace(state: AppState, workout: Workout): AppState =
        state.copy(workouts = state.workouts.map { if (it.id == workout.id) workout else it })

    fun startWorkout(state: AppState, workoutId: String, now: Long): AppState {
        val workout = findWorkout(state, workoutId)
        if (workout.startedAt != null) return state
        require(workout.completedAt == null) { "A completed workout cannot be started." }
        return replace(state, workout.copy(startedAt = now))
    }

    fun updateSet(state: AppState, workoutId: String, exerciseId: String, setIndex: Int, weightLb: Double, reps: Int, completed: Boolean): AppState {
        require(weightLb.isFinite() && weightLb in 0.0..1500.0) { "Weight must be a finite number from 0 to 1500 lb." }
        require(reps in 1..100) { "Reps must be from 1 to 100." }
        val workout = findWorkout(state, workoutId)
        require(workout.startedAt != null) { "Start the workout before logging sets." }
        val exercise = workout.exercises.find { it.id == exerciseId }
            ?: throw IllegalArgumentException("Exercise not found: $exerciseId.")
        require(setIndex in exercise.sets.indices) { "Set index is out of range: $setIndex." }
        val updated = exercise.copy(sets = exercise.sets.mapIndexed { index, set ->
            if (index == setIndex) SetEntry(weightLb, reps, completed) else set
        })
        return replace(state, workout.copy(exercises = workout.exercises.map { if (it.id == exerciseId) updated else it }))
    }

    fun finishWorkout(state: AppState, workoutId: String, now: Long): AppState {
        val workout = findWorkout(state, workoutId)
        if (workout.completedAt != null) return state
        require(workout.startedAt != null) { "Start the workout before finishing." }
        require(workout.exercises.any { e -> e.sets.any { it.completed } }) { "Complete at least one set before finishing." }
        return replace(state, workout.copy(completedAt = now))
    }

    fun discardWorkout(state: AppState, workoutId: String): AppState {
        val workout = findWorkout(state, workoutId)
        require(workout.completedAt == null) { "Completed workouts cannot be discarded." }
        return state.copy(workouts = state.workouts.filterNot { it.id == workoutId })
    }

    fun completedDayCounts(state: AppState, zone: ZoneId): Map<LocalDate, Int> = state.workouts
        .mapNotNull { it.completedAt }
        .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        .groupingBy { it }.eachCount()
}
