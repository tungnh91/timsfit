package com.timsfit.core

import org.junit.Assert.*
import org.junit.Test

class WorkoutSelectionTest {
    private fun completed(split: Split, id: String, time: Long, weight: Double): Workout {
        val draft = WorkoutEngine.createWorkout(AppState(), time - 2, id, split)
        var state = WorkoutEngine.startWorkout(draft, id, time - 1)
        for (exercise in draft.workouts.single().exercises) {
            for (index in exercise.sets.indices) {
                state = WorkoutEngine.updateSet(state, id, exercise.id, index, weight, exercise.targetReps, true)
            }
        }
        return WorkoutEngine.finishWorkout(state, id, time).workouts.single()
    }

    private fun rejects(action: () -> Unit) {
        try { action(); fail("Expected IllegalArgumentException") }
        catch (e: IllegalArgumentException) { assertFalse(e.message.isNullOrBlank()) }
    }

    @Test fun allSplitsCanOverrideWithoutChangingRecommendationUntilCompletion() {
        for (split in Split.entries) {
            val state = WorkoutEngine.createWorkout(AppState(), 10, "chosen", split)
            val workout = state.workouts.single()
            assertEquals(split, workout.split)
            assertEquals(4, workout.exercises.size)
            assertTrue(workout.estimatedMinutes in 30..40)
            assertEquals(Split.PUSH, WorkoutEngine.nextSplit(state))
            assertEquals(AppState(), WorkoutEngine.discardWorkout(state, "chosen"))
            val complete = AppState(listOf(completed(split, "chosen", 30, 20.0)))
            assertEquals(when (split) { Split.PUSH -> Split.PULL; Split.PULL -> Split.LEGS; Split.LEGS -> Split.PUSH }, WorkoutEngine.nextSplit(complete))
        }
        assertEquals(Split.PUSH, WorkoutEngine.createWorkout(AppState(), 1, "default").workouts.single().split)
    }

    @Test fun explicitChoiceUsesLatestHistoryOfThatSplit() {
        val history = AppState(listOf(
            completed(Split.LEGS, "old-legs", 10, 10.0),
            completed(Split.LEGS, "new-legs", 20, 60.0),
            completed(Split.PULL, "pull", 30, 100.0)
        ))
        val workout = WorkoutEngine.createWorkout(history, 40, "selected", Split.LEGS).workouts.last()
        assertEquals(Split.LEGS, workout.split)
        workout.exercises.flatMap { it.sets }.forEach { assertEquals(65.0, it.weightLb, 0.0); assertFalse(it.completed) }
        assertEquals(history.workouts, WorkoutEngine.createWorkout(history, 40, "selected", Split.LEGS).workouts.dropLast(1))
    }

    @Test fun changeDraftRegeneratesChosenHistoryAndRetainsIdentityAndOriginalState() {
        val history = AppState(listOf(completed(Split.LEGS, "history", 10, 60.0)))
        val draft = WorkoutEngine.createWorkout(history, 20, "draft", Split.PUSH)
        val changed = WorkoutEngine.changeWorkoutSplit(draft, "draft", Split.LEGS)
        val expected = WorkoutEngine.createWorkout(history, 20, "draft", Split.LEGS)
        assertEquals(expected, changed)
        assertEquals(Split.PUSH, draft.workouts.last().split)
        assertEquals(history.workouts, changed.workouts.dropLast(1))
        assertSame(changed, WorkoutEngine.changeWorkoutSplit(changed, "draft", Split.LEGS))
        assertEquals(draft, WorkoutEngine.changeWorkoutSplit(changed, "draft", Split.PUSH))
    }

    @Test fun activeAndCompletedChangesRejectIncludingSameSplitAndMissingId() {
        val draft = WorkoutEngine.createWorkout(AppState(), 10, "draft", Split.PULL)
        val active = WorkoutEngine.startWorkout(draft, "draft", 20)
        val complete = AppState(listOf(completed(Split.PULL, "done", 30, 20.0)))
        for (split in Split.entries) {
            rejects { WorkoutEngine.changeWorkoutSplit(active, "draft", split) }
            rejects { WorkoutEngine.changeWorkoutSplit(complete, "done", split) }
        }
        rejects { WorkoutEngine.changeWorkoutSplit(draft, "missing", Split.PUSH) }
        // Defensive guard for loaded completed data even when startedAt is absent.
        val completedWithoutStart = AppState(listOf(complete.workouts.single().copy(startedAt = null)))
        rejects { WorkoutEngine.changeWorkoutSplit(completedWithoutStart, "done", Split.PULL) }
    }

    @Test fun createWithChoicePreservesExistingDraftAndLoggedActiveSession() {
        val draft = WorkoutEngine.createWorkout(AppState(), 10, "draft", Split.PULL)
        val started = WorkoutEngine.startWorkout(draft, "draft", 20)
        val active = WorkoutEngine.updateSet(started, "draft", started.workouts.single().exercises.first().id, 0, 75.0, 8, true)
        for (split in Split.entries) {
            assertSame(draft, WorkoutEngine.createWorkout(draft, 40, "replacement", split))
            assertSame(active, WorkoutEngine.createWorkout(active, 40, "replacement", split))
        }
        assertEquals(SetEntry(75.0, 8, true), active.workouts.single().exercises.first().sets.first())
    }
}
