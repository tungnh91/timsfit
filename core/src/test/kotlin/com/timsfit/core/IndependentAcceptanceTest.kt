package com.timsfit.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

/** Acceptance scenarios authored independently of the engine implementation. */
class IndependentAcceptanceTest {
    private fun start(state: AppState, id: String, time: Long): AppState =
        WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(state, time, id), id, time + 1)

    private fun log(state: AppState, id: String, index: Int, weight: Double, reps: Int = 8, done: Boolean = true): AppState =
        WorkoutEngine.updateSet(state, id, state.workouts.first { it.id == id }.exercises.first().id, index, weight, reps, done)

    private fun complete(state: AppState, id: String, time: Long): AppState =
        WorkoutEngine.finishWorkout(log(start(state, id, time), id, 0, 20.0), id, time + 2)

    private fun returnToPush(state: AppState): AppState =
        complete(complete(state, "pull", 200), "legs", 300)

    @Test fun realSessionCycleReusesDraftAndAdvancesExactlyOncePerFinish() {
        var state = AppState()
        val expected = listOf(Split.PUSH, Split.PULL, Split.LEGS, Split.PUSH)
        expected.forEachIndexed { index, split ->
            val time = index * 100L
            val discarded = WorkoutEngine.createWorkout(state, time, "discard-$index")
            assertEquals(split, discarded.workouts.last().split)
            state = WorkoutEngine.discardWorkout(discarded, "discard-$index")
            assertEquals(split, WorkoutEngine.nextSplit(state))
            val id = "session-$index"
            state = start(state, id, time + 10)
            assertEquals(split, state.workouts.last().split)
            assertEquals(state, WorkoutEngine.createWorkout(state, time + 20, "duplicate"))
            state = log(state, id, 0, 30.0)
            state = WorkoutEngine.finishWorkout(state, id, time + 30)
            assertEquals(state, WorkoutEngine.finishWorkout(state, id, time + 40))
            assertEquals(index + 1, state.workouts.size)
            assertEquals(index + 1, WorkoutEngine.completedDayCounts(state, ZoneId.of("UTC")).values.sum())
        }
    }

    @Test fun mixedZeroAndVariedLoadsProgressAndLaterHistoryEditStopsIncrease() {
        var state = start(AppState(), "push", 100)
        listOf(0.0, 40.0, 55.0).forEachIndexed { index, weight -> state = log(state, "push", index, weight) }
        state = returnToPush(WorkoutEngine.finishWorkout(state, "push", 110))
        val next = WorkoutEngine.createWorkout(state, 400, "next")
        assertEquals(listOf(0.0, 45.0, 60.0), next.workouts.last().exercises.first().sets.map { it.weightLb })
        assertTrue(next.workouts.last().exercises.first().sets.none { it.completed })
        val edited = log(state, "push", 1, 35.0, reps = 7)
        val afterEdit = WorkoutEngine.createWorkout(edited, 400, "edited-next")
        assertEquals(listOf(0.0, 35.0, 55.0), afterEdit.workouts.last().exercises.first().sets.map { it.weightLb })
        assertEquals(110L, edited.workouts.first().completedAt)
        assertEquals(40.0, state.workouts.first().exercises.first().sets[1].weightLb, 0.0)
    }

    @Test fun partialSessionDoesNotTreatEnteredUncompletedLoadAsPerformance() {
        var state = start(AppState(), "push", 100)
        state = log(state, "push", 0, 40.0)
        state = log(state, "push", 1, 50.0)
        state = log(state, "push", 2, 200.0, done = false)
        state = returnToPush(WorkoutEngine.finishWorkout(state, "push", 110))
        val next = WorkoutEngine.createWorkout(state, 400, "next").workouts.last()
        assertEquals(listOf(40.0, 50.0), next.exercises.first().sets.take(2).map { it.weightLb })
        assertTrue(next.exercises.first().sets.last().weightLb <= 50.0)
        assertTrue(next.exercises.drop(1).flatMap { it.sets }.all { it.weightLb == 0.0 && !it.completed })
        assertEquals(2, state.workouts.first().exercises.flatMap { it.sets }.count { it.completed })
    }

    @Test fun completionOrderAndLocalDayAreIndependentOfCreationAndListOrder() {
        val instant = Instant.parse("2026-11-01T07:30:00Z").toEpochMilli()
        var state = complete(AppState(), "push", instant)
        state = complete(state, "pull", instant + 3_600_000)
        state = complete(state, "legs", instant + 7_200_000)
        // Device clocks can change; creation time must not select the latest completion.
        state = state.copy(workouts = state.workouts.reversed().map { it.copy(createdAt = Long.MAX_VALUE - it.createdAt) })
        assertEquals(Split.PUSH, WorkoutEngine.nextSplit(state))
        assertEquals(state, WorkoutEngine.finishWorkout(state, "push", instant + 99_000_000))
        assertEquals(mapOf(LocalDate.of(2026, 11, 1) to 3), WorkoutEngine.completedDayCounts(state, ZoneId.of("America/Los_Angeles")))
        assertEquals(mapOf(LocalDate.of(2026, 10, 31) to 3), WorkoutEngine.completedDayCounts(state, ZoneId.of("Pacific/Honolulu")))
    }

    @Test fun switchingDraftUsesEditedChosenHistoryWithoutCompoundingProgression() {
        var state = WorkoutEngine.createWorkout(AppState(), 100, "legs-history", Split.LEGS)
        state = WorkoutEngine.startWorkout(state, "legs-history", 101)
        listOf(0.0, 40.0, 55.0).forEachIndexed { index, load ->
            state = log(state, "legs-history", index, load)
        }
        state = WorkoutEngine.finishWorkout(state, "legs-history", 110)
        state = WorkoutEngine.createWorkout(state, 200, "draft", Split.PUSH)
        val originalHistory = state.workouts.first()
        state = WorkoutEngine.changeWorkoutSplit(state, "draft", Split.LEGS)
        assertEquals(listOf(0.0, 45.0, 60.0), state.workouts.last().exercises.first().sets.map { it.weightLb })
        repeat(3) {
            state = WorkoutEngine.changeWorkoutSplit(state, "draft", Split.PULL)
            state = WorkoutEngine.changeWorkoutSplit(state, "draft", Split.LEGS)
        }
        assertEquals(listOf(0.0, 45.0, 60.0), state.workouts.last().exercises.first().sets.map { it.weightLb })
        assertEquals(originalHistory, state.workouts.first())
        state = log(state, "legs-history", 1, 30.0, reps = 7)
        state = WorkoutEngine.changeWorkoutSplit(state, "draft", Split.PUSH)
        state = WorkoutEngine.changeWorkoutSplit(state, "draft", Split.LEGS)
        val draft = state.workouts.last()
        assertEquals(listOf(0.0, 30.0, 55.0), draft.exercises.first().sets.map { it.weightLb })
        assertEquals("draft", draft.id)
        assertEquals(200L, draft.createdAt)
        assertNull(draft.startedAt)
        assertTrue(draft.exercises.flatMap { it.sets }.none { it.completed })
        assertEquals(Split.PUSH, WorkoutEngine.nextSplit(state))
    }
}
