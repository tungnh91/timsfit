package com.timsfit.app

import com.timsfit.core.*

interface StateFile {
    fun read(): String?
    fun replace(contents: String)
}
class StateStore(private val file: StateFile) {
    fun load(): AppState = TODO("Implement versioned decoding")
    fun save(state: AppState): Unit = TODO("Implement durable encoding")
}
