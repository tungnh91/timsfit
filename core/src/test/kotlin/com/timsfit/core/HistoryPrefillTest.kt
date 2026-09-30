package com.timsfit.core

import org.junit.Assert.*
import org.junit.Test

class HistoryPrefillTest {
    @Test fun thirtyStaysThirtyAndActualRepsAreRetained() {
        var state = WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(AppState(), 1, "old"), "old", 2)
        repeat(3) { state = WorkoutEngine.updateSet(state, "old", "bench-press", it, 30.0, 9 + it, true) }
        state = WorkoutEngine.finishWorkout(state, "old", 3)
        state = WorkoutEngine.createWorkout(state, 4, "new", Split.PUSH)
        repeat(3) {
            state = WorkoutEngine.changeWorkoutSplit(state, "new", Split.LEGS)
            state = WorkoutEngine.changeWorkoutSplit(state, "new", Split.PUSH)
        }
        assertEquals(listOf(SetEntry(30.0, 9, false), SetEntry(30.0, 10, false), SetEntry(30.0, 11, false)), state.workouts.last().exercises.first().sets)
    }
    @Test fun searchesBackPerSetAcrossWorkoutsIgnoringUntouchedSuggestions() {
        var state = WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(AppState(), 1, "old"), "old", 2)
        state = WorkoutEngine.updateSet(state, "old", "bench-press", 1, 32.5, 7, true)
        state = WorkoutEngine.finishWorkout(state, "old", 3)
        state = WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(state, 4, "recent", Split.PUSH), "recent", 5)
        state = WorkoutEngine.updateSet(state, "recent", "bench-press", 0, 30.0, 8, true)
        state = WorkoutEngine.finishWorkout(state, "recent", 6)
        val sets = WorkoutEngine.createWorkout(state, 7, "new", Split.PUSH).workouts.last().exercises.first().sets
        assertEquals(SetEntry(30.0, 8, false), sets[0])
        assertEquals(SetEntry(32.5, 7, false), sets[1])
        assertEquals(SetEntry(0.0, 8, false), sets[2])
    }
}
