package com.timsfit.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class WorkoutEngineTest {
    private fun created(id: String = "w") = WorkoutEngine.createWorkout(AppState(), 100, id)
    private fun started() = WorkoutEngine.startWorkout(created(), "w", 200)
    private fun logged() = started().let { s ->
        WorkoutEngine.updateSet(s, "w", s.workouts.single().exercises.first().id, 0, 50.0, 10, true)
    }
    private fun finished() = WorkoutEngine.finishWorkout(logged(), "w", 300)
    private fun invalid(action: () -> Unit) {
        try { action(); fail("Expected IllegalArgumentException") }
        catch (e: IllegalArgumentException) { assertFalse(e.message.isNullOrBlank()) }
    }

    @Test fun rotationUsesLatestCompletionAndIgnoresDraftsAndDiscard() {
        assertEquals(Split.PUSH, WorkoutEngine.nextSplit(AppState()))
        val draft = created()
        assertEquals(Split.PUSH, WorkoutEngine.nextSplit(draft))
        assertEquals(AppState(), WorkoutEngine.discardWorkout(draft, "w"))
        val push = finished().workouts.single()
        for ((split, expected) in listOf(Split.PUSH to Split.PULL, Split.PULL to Split.LEGS, Split.LEGS to Split.PUSH)) {
            val state = AppState(listOf(push.copy(id = "recent", split = split, completedAt = 500), push.copy(id = "old")))
            assertEquals(expected, WorkoutEngine.nextSplit(state))
        }
    }

    @Test fun plansHaveTwelveUncompletedSetsZeroLoadAndRealisticTime() {
        for (split in Split.entries) {
            val prior = finished().workouts.single().copy(split = when(split) { Split.PUSH -> Split.LEGS; Split.PULL -> Split.PUSH; Split.LEGS -> Split.PULL })
            val workout = WorkoutEngine.createWorkout(AppState(listOf(prior)), 400, "next").workouts.last()
            assertEquals(split, workout.split)
            assertEquals(4, workout.exercises.size)
            assertEquals(4, workout.exercises.map { it.id }.distinct().size)
            assertTrue(workout.estimatedMinutes in 30..40)
            val seconds = 5 * 60 + 4 * 60 + workout.exercises.sumOf { it.sets.size * 45 + (it.sets.size - 1) * it.restSeconds }
            assertEquals((seconds + 59) / 60, workout.estimatedMinutes)
            workout.exercises.forEach { e ->
                assertTrue(e.note.contains("0"))
                assertEquals(3, e.sets.size)
                e.sets.forEach { assertEquals(0.0, it.weightLb, 0.0); assertEquals(e.targetReps, it.reps); assertFalse(it.completed) }
            }
        }
    }

    @Test fun oneUnfinishedAndStartAreIdempotent() {
        val draft = created()
        assertEquals(draft, WorkoutEngine.createWorkout(draft, 300, "other"))
        val active = WorkoutEngine.startWorkout(draft, "w", 200)
        assertEquals(active, WorkoutEngine.startWorkout(active, "w", 900))
        assertEquals(active, WorkoutEngine.createWorkout(active, 900, "other"))
        assertNull(draft.workouts.single().startedAt)
    }

    @Test fun finishRequiresStartAndCompletedSetAndIsIdempotent() {
        invalid { WorkoutEngine.finishWorkout(created(), "w", 300) }
        invalid { WorkoutEngine.finishWorkout(started(), "w", 300) }
        val complete = finished()
        assertEquals(complete, WorkoutEngine.finishWorkout(complete, "w", 900))
        assertEquals(300L, complete.workouts.single().completedAt)
        invalid { WorkoutEngine.discardWorkout(complete, "w") }
    }

    @Test fun editsPreserveOldStateAndAllowCompletedHistory() {
        val complete = finished()
        val e = complete.workouts.single().exercises.first()
        val edited = WorkoutEngine.updateSet(complete, "w", e.id, 0, 65.0, 8, true)
        assertEquals(50.0, complete.workouts.single().exercises.first().sets.first().weightLb, 0.0)
        assertEquals(65.0, edited.workouts.single().exercises.first().sets.first().weightLb, 0.0)
        assertEquals(300L, edited.workouts.single().completedAt)
        assertEquals(complete.workouts.single().exercises.drop(1), edited.workouts.single().exercises.drop(1))
    }

    @Test fun validatesAllInputsAndBoundaries() {
        val state = started()
        val eid = state.workouts.single().exercises.first().id
        for (weight in listOf(-1.0, 1500.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            invalid { WorkoutEngine.updateSet(state, "w", eid, 0, weight, 10, true) }
        }
        for (reps in listOf(-1, 0, 101)) invalid { WorkoutEngine.updateSet(state, "w", eid, 0, 0.0, reps, true) }
        for (index in listOf(-1, 3)) invalid { WorkoutEngine.updateSet(state, "w", eid, index, 0.0, 1, true) }
        for (weight in listOf(0.0, 1500.0)) for (reps in listOf(1, 100)) {
            val edited = WorkoutEngine.updateSet(state, "w", eid, 0, weight, reps, true)
            assertEquals(SetEntry(weight, reps, true), edited.workouts.single().exercises.first().sets.first())
        }
        invalid { WorkoutEngine.updateSet(created(), "w", eid, 0, 0.0, 1, true) }
        invalid { WorkoutEngine.updateSet(state, "missing", eid, 0, 0.0, 1, true) }
        invalid { WorkoutEngine.updateSet(state, "w", "missing", 0, 0.0, 1, true) }
        invalid { WorkoutEngine.startWorkout(state, "missing", 400) }
        invalid { WorkoutEngine.finishWorkout(state, "missing", 400) }
        invalid { WorkoutEngine.discardWorkout(state, "missing") }
        invalid { WorkoutEngine.createWorkout(AppState(), 0, " ") }
        invalid { WorkoutEngine.createWorkout(finished(), 400, "w") }
    }

    @Test fun progressionIsPerExerciseAndRequiresAllTargetSets() {
        val workout = finished().workouts.single()
        val history = workout.copy(exercises = workout.exercises.mapIndexed { i, e ->
            e.copy(sets = List(3) { n -> SetEntry(50.0 + n * 5, if (i == 1) e.targetReps - 1 else e.targetReps, i != 2 || n != 2) })
        })
        val recentPull = workout.copy(id = "pull", split = Split.PULL, completedAt = 400)
        val recentLegs = workout.copy(id = "legs", split = Split.LEGS, completedAt = 500)
        val state = AppState(listOf(history, recentPull, recentLegs))
        val generated = WorkoutEngine.createWorkout(state, 600, "new").workouts.last()
        assertEquals(Split.PUSH, generated.split)
        generated.exercises.forEachIndexed { i, e ->
            e.sets.forEachIndexed { n, set ->
                if (i == 0 || i == 3) assertEquals(history.exercises[i].sets[n].weightLb + 5.0, set.weightLb, 0.0)
                else assertEquals(if (i == 2 && n == 2) 50.0 else history.exercises[i].sets[n].weightLb, set.weightLb, 0.0)
                assertEquals(e.targetReps, set.reps)
                assertFalse(set.completed)
            }
            assertTrue(e.note.isNotBlank())
        }
        val edited = WorkoutEngine.updateSet(state, "w", history.exercises[0].id, 0, 25.0, 1, true)
        val afterEdit = WorkoutEngine.createWorkout(edited, 600, "new").workouts.last()
        assertEquals(25.0, afterEdit.exercises.first().sets.first().weightLb, 0.0)
    }

    @Test fun latestSameSplitSeedsAndProgressionNeverExceedsWeightLimit() {
        val base = finished().workouts.single()
        val latest = base.copy(id = "latest", completedAt = 600, exercises = base.exercises.map { e -> e.copy(sets = List(3) { SetEntry(1500.0, e.targetReps, true) }) })
        val legs = base.copy(id = "legs", split = Split.LEGS, completedAt = 700)
        val next = WorkoutEngine.createWorkout(AppState(listOf(latest, legs, base)), 800, "new").workouts.last()
        next.exercises.flatMap { it.sets }.forEach { assertEquals(1500.0, it.weightLb, 0.0) }
    }

    @Test fun zeroLoadNeverProgresses() {
        val base = finished().workouts.single()
        val zero = base.copy(exercises = base.exercises.map { e -> e.copy(sets = List(3) { SetEntry(0.0, e.targetReps, true) }) })
        val legs = base.copy(id = "legs", split = Split.LEGS, completedAt = 700)
        val next = WorkoutEngine.createWorkout(AppState(listOf(zero, legs)), 800, "new").workouts.last()
        next.exercises.flatMap { it.sets }.forEach { assertEquals(0.0, it.weightLb, 0.0) }

    }

    @Test fun heatmapUsesCompletionLocalDateAcrossMidnightAndDst() {
        val base = finished().workouts.single()
        val instants = listOf("2026-03-08T07:59:00Z", "2026-03-08T09:59:00Z", "2026-03-08T10:01:00Z", "2026-11-01T08:30:00Z", "2026-11-01T09:30:00Z")
        val state = AppState(instants.mapIndexed { i, instant -> base.copy(id = "w$i", completedAt = Instant.parse(instant).toEpochMilli()) } + created("draft").workouts)
        assertEquals(mapOf(LocalDate.parse("2026-03-07") to 1, LocalDate.parse("2026-03-08") to 2, LocalDate.parse("2026-11-01") to 2), WorkoutEngine.completedDayCounts(state, ZoneId.of("America/Los_Angeles")))
        assertEquals(3, WorkoutEngine.completedDayCounts(state, ZoneId.of("UTC"))[LocalDate.parse("2026-03-08")])
        assertTrue(WorkoutEngine.completedDayCounts(created(), ZoneId.of("UTC")).isEmpty())
    }
}
