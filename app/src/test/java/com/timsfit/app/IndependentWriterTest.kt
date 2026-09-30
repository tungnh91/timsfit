package com.timsfit.app

import com.timsfit.core.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.Assert.*
import org.junit.Test

class IndependentWriterTest {
    private class File : StateFile {
        @Volatile var contents: String? = null
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        @Volatile var block = false
        override fun read() = contents
        override fun replace(contents: String) {
            if (block) { entered.countDown(); check(release.await(8, TimeUnit.SECONDS)) }
            this.contents = contents
        }
    }
    @Test fun lifecycleTimeoutDoesNotAcknowledgeOrLosePendingWrite() {
        val file = File(); val writer = StateWriter(StateStore(file))
        try {
            writer.load().get(3, TimeUnit.SECONDS)
            writer.change { WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(it, 1, "w"), "w", 2) }.get(3, TimeUnit.SECONDS)
            file.block = true
            val edit = writer.change { WorkoutEngine.updateSet(it, "w", "bench-press", 0, 30.0, 7, true) }
            assertTrue(file.entered.await(2, TimeUnit.SECONDS))
            assertThrows(TimeoutException::class.java) { writer.flush() }
            assertFalse(edit.isDone)
            assertFalse(StateStore(file).load().workouts.single().exercises.first().sets.first().completed)
            file.release.countDown()
            edit.get(3, TimeUnit.SECONDS)
            assertEquals(SetEntry(30.0, 7, true), StateStore(file).load().workouts.single().exercises.first().sets.first())
        } finally { file.release.countDown(); writer.close() }
    }
    @Test fun rejectedTransformDoesNotDropLaterEditsOrOrderedFinish() {
        val file = File(); val writer = StateWriter(StateStore(file))
        try {
            writer.load().get(3, TimeUnit.SECONDS)
            writer.change { WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(it, 1, "w"), "w", 2) }.get(3, TimeUnit.SECONDS)
            val invalid = writer.change { WorkoutEngine.updateSet(it, "w", "bench-press", 0, Double.NaN, 8, true) }
            writer.change { WorkoutEngine.updateSet(it, "w", "bench-press", 0, 30.0, 7, true) }
            writer.change { WorkoutEngine.updateSet(it, "w", "lateral-raise", 1, 12.5, 9, true) }
            val finish = writer.change { WorkoutEngine.finishWorkout(it, "w", 100) }.get(3, TimeUnit.SECONDS)
            assertTrue(invalid.isCompletedExceptionally)
            assertEquals(2, finish.workouts.single().exercises.flatMap { it.sets }.count { it.completed })
            assertEquals(100L, finish.workouts.single().completedAt)
            assertEquals(finish, StateStore(file).load())
        } finally { writer.close() }
    }
}
