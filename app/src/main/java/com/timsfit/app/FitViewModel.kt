package com.timsfit.app

import android.app.Application
import android.util.AtomicFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.timsfit.core.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal class AtomicStateFile(file: File) : StateFile {
    private val atomic = AtomicFile(file)
    override fun read(): String? {
        // openRead also recovers the backup left by an interrupted write.
        if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) return null
        return atomic.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
    override fun replace(contents: String) {
        val stream = atomic.startWrite()
        try {
            stream.write(contents.toByteArray(Charsets.UTF_8))
            stream.flush()
            stream.fd.sync()
            atomic.finishWrite(stream)
            // AtomicFile logs some rename failures instead of throwing. Verify the committed bytes
            // before the UI reports success, so those failures remain visible and retryable.
            check(atomic.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() } == contents) {
                "The saved file could not be verified. Please retry."
            }
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }
}

data class FitUiState(val state: AppState = AppState(), val loaded: Boolean = false,
    val busy: Boolean = false, val error: String? = null)

class FitViewModel(application: Application) : AndroidViewModel(application) {
    private val store = StateStore(AtomicStateFile(File(application.filesDir, "training-log-v1.json")))
    private val mutableUi = MutableStateFlow(FitUiState())
    val ui: StateFlow<FitUiState> = mutableUi
    init { reload() }
    fun reload() {
        if (mutableUi.value.busy) return
        mutableUi.value = mutableUi.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val state = withContext(Dispatchers.IO) { store.load() }
                mutableUi.value = FitUiState(state = state, loaded = true)
            } catch (e: Exception) {
                mutableUi.value = mutableUi.value.copy(busy = false, error = e.message ?: "Could not read saved log.")
            }
        }
    }
    fun dismissError() { mutableUi.value = mutableUi.value.copy(error = null) }
    /** Busy is set synchronously on Main, serializing every action through durable IO. */
    fun change(transform: (AppState) -> AppState, onSaved: (AppState) -> Unit = {}) {
        val before = mutableUi.value
        if (before.busy || !before.loaded) return
        mutableUi.value = before.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val next = transform(before.state)
                withContext(Dispatchers.IO) { store.save(next) }
                mutableUi.value = FitUiState(next, loaded = true)
                onSaved(next)
            } catch (e: Exception) {
                mutableUi.value = before.copy(busy = false, error = "Not saved. ${e.message ?: "Please try again."}")
            }
        }
    }
}
