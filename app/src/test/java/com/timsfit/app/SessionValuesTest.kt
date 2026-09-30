package com.timsfit.app

import com.timsfit.core.*
import org.junit.Assert.*
import org.junit.Test

class SessionValuesTest {
    @Test fun weightsAreCompactAndDecimalEditsRemainValid() {
        assertEquals("35", weightText(35.0))
        assertEquals("32.5", weightText(32.5))
        assertEquals(SetEntry(32.0, 8, true), editedSet("32.", "8"))
        assertEquals(SetEntry(0.5, 8, true), editedSet(".5", "8"))
        listOf("", ".", "-1", "NaN", "Infinity", "1501").forEach { assertNull(editedSet(it, "8")) }
        listOf("", "0", "101", "8.5").forEach { assertNull(editedSet("30", it)) }
    }
    @Test fun elapsedUsesPersistedStartAndFinishAndClampsBackwardClock() {
        val draft = WorkoutEngine.createWorkout(AppState(), 0, "w").workouts.single()
        assertEquals(0L, elapsedSeconds(draft, 9000))
        val active = draft.copy(startedAt = 1000)
        assertEquals(61L, elapsedSeconds(active, 62999))
        assertEquals(0L, elapsedSeconds(active, 0))
        assertEquals(60L, elapsedSeconds(active.copy(completedAt = 61000), 900000))
        assertEquals("1:01:01", durationText(3661))
        assertEquals("00:59", durationText(59))
    }
    @Test fun everyBuiltInVariantHasAnHttpsDemoAndUnknownHistoryIsSafe() {
        val ids = Split.entries.flatMap { split -> WorkoutEngine.createWorkout(AppState(), 1, "w", split).workouts.single().exercises.map { it.id } }
        assertEquals(12, ids.distinct().size)
        ids.forEach { assertTrue(exerciseDemoUrl(it)?.startsWith("https://") == true) }
        assertNull(exerciseDemoUrl("unknown-import"))
    }
}
