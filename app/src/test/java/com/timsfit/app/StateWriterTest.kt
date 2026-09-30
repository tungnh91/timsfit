package com.timsfit.app

import com.timsfit.core.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class StateWriterTest {
    private class MemoryFile : StateFile {
        var text: String? = null
        var fail = false
        var entered: CountDownLatch? = null
        var release: CountDownLatch? = null
        override fun read() = text
        override fun replace(contents: String) {
            entered?.countDown(); release?.await(5, TimeUnit.SECONDS)
            if (fail) error("Disk unavailable")
            text = contents
        }
    }
    @Test fun rapidEditsAndFinishSerializeAgainstLatestDurableState() {
        val file = MemoryFile(); val store = StateStore(file); val writer = StateWriter(store)
        try {
            writer.load().get(5, TimeUnit.SECONDS)
            writer.change { WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(it, 1, "w"), "w", 2) }.get(5, TimeUnit.SECONDS)
            file.entered = CountDownLatch(1); file.release = CountDownLatch(1)
            val first = writer.change { WorkoutEngine.updateSet(it, "w", "bench-press", 0, 3.0, 8, true) }
            assertTrue(file.entered!!.await(2, TimeUnit.SECONDS))
            val last = writer.change { WorkoutEngine.updateSet(it, "w", "bench-press", 0, 32.5, 9, true) }
            val finish = writer.change { WorkoutEngine.finishWorkout(it, "w", 100) }
            file.release!!.countDown()
            finish.get(5, TimeUnit.SECONDS)
            assertTrue(first.isDone && last.isDone)
            val restored = store.load().workouts.single()
            assertEquals(SetEntry(32.5, 9, true), restored.exercises.first().sets.first())
            assertEquals(100L, restored.completedAt)
        } finally { file.release?.countDown(); writer.close() }
    }
    @Test fun failedWriteRetainsEditsAndFinishUntilRetryWithoutFalseSuccess() {
        val file = MemoryFile(); val store = StateStore(file); val writer = StateWriter(store)
        try {
            writer.load().get(5, TimeUnit.SECONDS)
            writer.change { WorkoutEngine.startWorkout(WorkoutEngine.createWorkout(it, 1, "w"), "w", 2) }.get(5, TimeUnit.SECONDS)
            file.fail = true
            val edit = writer.change { WorkoutEngine.updateSet(it, "w", "bench-press", 0, 30.0, 8, true) }
            writer.flush()
            assertFalse(edit.isDone)
            assertFalse(store.load().workouts.single().exercises.first().sets.first().completed)
            val finish = writer.change { WorkoutEngine.finishWorkout(it, "w", 100) }
            file.fail = false; writer.retry(); writer.flush()
            assertTrue(edit.isDone && finish.isDone)
            assertEquals(30.0, store.load().workouts.single().exercises.first().sets.first().weightLb, 0.0)
        } finally { writer.close() }
    }
}
