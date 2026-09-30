package com.timsfit.app

import android.app.Application
import android.util.AtomicFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.timsfit.core.AppState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    private val mutableUi = MutableStateFlow(FitUiState())
    val ui: StateFlow<FitUiState> = mutableUi
    private val writer = StateWriter(StateStore(AtomicStateFile(File(application.filesDir, "training-log-v1.json")))) {
        mutableUi.value = it
    }
    init { reload() }
    fun reload() { writer.load() }
    fun retry() { writer.retry() }
    fun change(transform: (AppState) -> AppState, onSaved: (AppState) -> Unit = {}) {
        writer.change(transform).thenAccept { state -> viewModelScope.launch { onSaved(state) } }
    }
    fun flush() {
        try { writer.flush() } catch (_: java.util.concurrent.TimeoutException) {
            // Keep Saving visible; the IO worker continues. Never acknowledge an unfinished write.
        }
    }
    override fun onCleared() { writer.close() }
}
