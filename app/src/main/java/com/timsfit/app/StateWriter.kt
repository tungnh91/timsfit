package com.timsfit.app

import com.timsfit.core.AppState
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** One IO worker, ordered transforms (never stale snapshots), and a retained queue on disk failure. */
internal class StateWriter(private val store: StateStore, private val publish: (FitUiState) -> Unit = {}) {
    private data class Change(val transform: (AppState) -> AppState, val result: CompletableFuture<AppState>)
    private val executor = Executors.newSingleThreadExecutor()
    private val pending = ArrayDeque<Change>()
    private var state = AppState()
    private var loaded = false
    private var failed = false
    private val lock = Any()

    fun load(): CompletableFuture<AppState> {
        val result = CompletableFuture<AppState>()
        executor.execute {
            synchronized(lock) {
                try {
                    check(pending.isEmpty()) { "Retry saving your pending changes first." }
                    publish(FitUiState(state, loaded, busy = true))
                    state = store.load(); loaded = true; failed = false
                    publish(FitUiState(state, loaded = true)); result.complete(state)
                } catch (e: Exception) {
                    publish(FitUiState(state, loaded, error = e.message)); result.completeExceptionally(e)
                }
            }
        }
        return result
    }
    fun change(transform: (AppState) -> AppState): CompletableFuture<AppState> {
        val result = CompletableFuture<AppState>()
        synchronized(lock) {
            check(loaded) { "Open your saved log first." }
            pending.add(Change(transform, result))
            if (!failed) publish(FitUiState(state, loaded = true, busy = true))
        }
        executor.execute(::drain)
        return result
    }
    private fun drain() {
        while (true) {
            val change = synchronized(lock) { if (failed) null else pending.peek() } ?: return
            val next = try { change.transform(state) } catch (e: Exception) {
                synchronized(lock) {
                    pending.remove(); publish(FitUiState(state, loaded, busy = pending.isNotEmpty(), error = e.message))
                }
                change.result.completeExceptionally(e)
                continue
            }
            try { store.save(next) } catch (e: Exception) {
                synchronized(lock) {
                    failed = true
                    publish(FitUiState(state, loaded, error = "Not saved. ${e.message ?: "Please retry."}"))
                }
                return
            }
            synchronized(lock) {
                state = next; pending.remove()
                publish(FitUiState(state, loaded = true, busy = pending.isNotEmpty()))
            }
            change.result.complete(next)
        }
    }
    fun retry() {
        synchronized(lock) { failed = false; publish(FitUiState(state, loaded, busy = pending.isNotEmpty())) }
        executor.execute(::drain)
    }
    /** Lifecycle barrier: bounded wait, IO never needs the main thread to finish. */
    fun flush() { executor.submit {}.get(2, TimeUnit.SECONDS) }
    fun close() { executor.shutdown() }
}
