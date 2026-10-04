package com.timsfit.core

import org.junit.Assert.*
import org.junit.Test

class CarryForwardTest {
    private fun started() = WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(AppState(), 1, "w"), "w", 2)

    @Test fun weightCarriesForwardWithoutCompletingOtherSetsOrChangingOtherExercises() {
        val original = started()
        val result = WorkoutEngine.updateSet(original, "w", "bench-press", 0, 15.0, 8, true)
        assertEquals(listOf(SetEntry(15.0, 8, true), SetEntry(15.0, 8, false), SetEntry(15.0, 8, false)), result.workouts.single().exercises.first().sets)
        assertEquals(original.workouts.single().exercises.drop(1), result.workouts.single().exercises.drop(1))
    }

    @Test fun onlyChangedFieldCarriesForwardAndEarlierOrCompletedSetsArePreserved() {
        var state = started()
        state = WorkoutEngine.updateSet(state, "w", "bench-press", 2, 40.0, 6, true)
        state = WorkoutEngine.updateSet(state, "w", "bench-press", 0, 15.0, 8, true)
        state = WorkoutEngine.updateSet(state, "w", "bench-press", 1, 20.0, 10, true)
        assertEquals(listOf(SetEntry(15.0, 8, true), SetEntry(20.0, 10, true), SetEntry(40.0, 6, true)), state.workouts.single().exercises.first().sets)
    }

    @Test fun changingWeightPreservesDifferentRepsInLaterSuggestions() {
        val original = started()
        val workout = original.workouts.single()
        val exercise = workout.exercises.first()
        val state = original.copy(workouts = listOf(workout.copy(exercises = listOf(
            exercise.copy(sets = listOf(SetEntry(0.0, 8, false), SetEntry(5.0, 10, false), SetEntry(10.0, 12, false)))
        ) + workout.exercises.drop(1))))
        val result = WorkoutEngine.updateSet(state, "w", "bench-press", 0, 15.0, 8, true)
        assertEquals(listOf(SetEntry(15.0, 8, true), SetEntry(15.0, 10, false), SetEntry(15.0, 12, false)), result.workouts.single().exercises.first().sets)
    }

    @Test fun queuedRepsEditDoesNotOverwriteWeightFromEarlierSet() {
        var state = WorkoutEngine.updateSet(started(), "w", "bench-press", 0, 15.0, 8, true,
            weightEdited = true, repsEdited = false)
        // The second editor still displays the old weight while the first save is pending.
        state = WorkoutEngine.updateSet(state, "w", "bench-press", 1, 0.0, 10, true,
            weightEdited = false, repsEdited = true)
        assertEquals(listOf(SetEntry(15.0, 8, true), SetEntry(15.0, 10, true), SetEntry(15.0, 10, false)),
            state.workouts.single().exercises.first().sets)
    }

    @Test fun repsCarryForwardAndHistoryEditsStayLocal() {
        var state = WorkoutEngine.updateSet(started(), "w", "bench-press", 0, 0.0, 12, true)
        assertEquals(listOf(12, 12, 12), state.workouts.single().exercises.first().sets.map { it.reps })
        state = WorkoutEngine.finishWorkout(state, "w", 3)
        state = WorkoutEngine.updateSet(state, "w", "bench-press", 0, 25.0, 9, true)
        assertEquals(SetEntry(0.0, 12, false), state.workouts.single().exercises.first().sets[1])
    }
}
