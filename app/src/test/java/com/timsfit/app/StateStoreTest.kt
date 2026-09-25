package com.timsfit.app

import com.timsfit.core.*
import org.junit.Assert.*
import org.junit.Test

class StateStoreTest {
    private class MemoryFile(var text: String? = null) : StateFile {
        var fail = false
        override fun read() = text
        override fun replace(contents: String) { if (fail) error("disk full"); text = contents }
    }
    @Test fun emptyFileIsNewState() { assertEquals(AppState(), StateStore(MemoryFile()).load()) }
    @Test fun roundTripPreservesSessionAndSets() {
        val file = MemoryFile()
        val state = AppState(listOf(Workout("day", Split.PULL, 100, 200, 300,
            listOf(ExercisePlan("row", "Cable row", 10, 90,
                listOf(SetEntry(42.5, 9, true)), "Last time")), 35)))
        StateStore(file).save(state)
        assertEquals(state, StateStore(file).load())
    }
    @Test fun corruptFileBlocksWritesAndPreservesOriginal() {
        val file = MemoryFile("not json")
        val store = StateStore(file)
        assertThrows(Exception::class.java) { store.load() }
        assertThrows(Exception::class.java) { store.save(AppState()) }
        assertEquals("not json", file.text)
    }
    @Test fun futureVersionBlocksWrites() {
        val file = MemoryFile("""{"version":99,"workouts":[]}""")
        val store = StateStore(file)
        assertThrows(Exception::class.java) { store.load() }
        assertThrows(Exception::class.java) { store.save(AppState()) }
    }
    @Test fun failedWriteLeavesPriorStateRecoverable() {
        val file = MemoryFile()
        val store = StateStore(file)
        store.load()
        store.save(AppState())
        val original = file.text
        file.fail = true
        assertThrows(Exception::class.java) { store.save(AppState()) }
        assertEquals(original, file.text)
        file.fail = false
        store.save(AppState())
        assertEquals(AppState(), store.load())
    }
}
