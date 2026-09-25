package com.timsfit.app

import com.timsfit.core.*
import org.json.JSONArray
import org.json.JSONObject

/** All access is serialized by the state holder. A failed read locks writes until a successful retry. */
interface StateFile {
    fun read(): String?
    fun replace(contents: String)
}
class StateStore(private val file: StateFile) {
    private var blocked = false
    fun load(): AppState = try {
        val result = file.read()?.let(::decode) ?: AppState()
        blocked = false
        result
    } catch (error: Exception) {
        blocked = true
        throw IllegalStateException("Could not open your saved log. The original file has been kept. ${error.message.orEmpty()}", error)
    }
    fun save(state: AppState) {
        check(!blocked) { "Your saved log could not be read. Retry loading before making changes." }
        file.replace(encode(state))
    }
    private fun encode(state: AppState): String = JSONObject().put("version", 1).put("workouts", JSONArray().apply {
        state.workouts.forEach { w -> put(JSONObject().put("id", w.id).put("split", w.split.name)
            .put("createdAt", w.createdAt).put("startedAt", w.startedAt ?: JSONObject.NULL)
            .put("completedAt", w.completedAt ?: JSONObject.NULL).put("estimatedMinutes", w.estimatedMinutes)
            .put("exercises", JSONArray().apply { w.exercises.forEach { e ->
                put(JSONObject().put("id", e.id).put("name", e.name).put("targetReps", e.targetReps)
                    .put("restSeconds", e.restSeconds).put("note", e.note).put("sets", JSONArray().apply {
                        e.sets.forEach { s -> put(JSONObject().put("weightLb", s.weightLb).put("reps", s.reps).put("completed", s.completed)) }
                    }))
            } })) }
    }).toString()
    private fun decode(text: String): AppState {
        val root = JSONObject(text)
        require(root.getInt("version") == 1) { "This log uses an unsupported storage version." }
        val workouts = root.getJSONArray("workouts").objects().map { w ->
            Workout(w.getString("id"), Split.valueOf(w.getString("split")), w.getLong("createdAt"),
                w.nullableLong("startedAt"), w.nullableLong("completedAt"), w.getJSONArray("exercises").objects().map { e ->
                    ExercisePlan(e.getString("id"), e.getString("name"), e.getInt("targetReps"), e.getInt("restSeconds"),
                        e.getJSONArray("sets").objects().map { s ->
                            SetEntry(s.getDouble("weightLb"), s.getInt("reps"), s.getBoolean("completed")).also {
                                require(it.weightLb.isFinite() && it.weightLb in 0.0..1500.0 && it.reps in 1..100) { "Invalid saved set." }
                            }
                        }, e.getString("note"))
                }, w.getInt("estimatedMinutes"))
        }
        require(workouts.map { it.id }.distinct().size == workouts.size) { "Duplicate saved workout IDs." }
        require(workouts.count { it.completedAt == null } <= 1) { "Multiple unfinished workouts in saved log." }
        workouts.forEach { w ->
            require(w.id.isNotBlank() && w.exercises.isNotEmpty() && w.estimatedMinutes > 0) { "Invalid saved workout." }
            require(w.completedAt == null || w.startedAt != null) { "Completed workout has no start time." }
            require(w.exercises.map { it.id }.distinct().size == w.exercises.size) { "Duplicate saved exercise IDs." }
        }
        return AppState(workouts)
    }
    private fun JSONObject.nullableLong(key: String): Long? { require(has(key)); return if (isNull(key)) null else getLong(key) }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
}
