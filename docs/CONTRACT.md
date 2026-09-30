# PoC implementation contract

## Revision 0.3 — history, autosave, timer and demos

The user's latest requirements supersede earlier explicit-save and automatic load-increase behavior. VersionCode3/versionName0.3.0 retains schema1 and the existing application identity.

- Prefill each exercise/set from its most recent completed set in completed workout history, by stable exercise ID and set index. Copy actual weight/reps; do not increase loads. Missing history uses zero/target defaults. Prefills remain uncompleted.
- Display whole weights without `.0`, retaining fractional values. Valid changed input saves automatically and marks that set completed; no Save button or Completed checkbox. Focus or an untouched prefill does not complete a set.
- Serialize edits and finish/navigation actions through ordered atomic disk writes off the main thread. Keep text drafts responsive. Invalid/partial text must not overwrite saved values; show recoverable errors. A write is Saved only after successful persistence. Retain failed writes for retry.
- Session elapsed time derives from persisted startedAt and completedAt: it starts at Start, survives recreation/background/restart and freezes at Finish. Local device clock is the time source.
- Each exercise has a verified exercise-specific demonstration link, opened in an external browser. Handle absence of a browser visibly. Provenance lives in `docs/EXERCISE-LINKS.md`; no embedded media or network dependency for logging.
- Independently verify autosave failure/order, invalid drafts, rapid edits, restart/timer behavior, layout, demo handling and a genuine 0.2→0.3 upgrade. Make a verified Mac backup before every install/test installation.

## Revision 0.2 — less copy, explicit workout choice

User requested removal of motivational/redundant text and ability to override the recommended workout. Keep Kotlin/Compose, persistence, logging and heatmap behavior.

- Home begins with the workout card: compact `Suggested: Push` label, accessible Push / Pull / Legs selector and `Create workout` action. Default selection is the recommendation, but user can select any split. Selection survives recreation. No slogans, motivational heading, explanatory hero or standard-gym boilerplate.
- Draft detail permits the same split selection before Start; switching regenerates that draft from the selected split's own history, retaining workout ID/createdAt. Re-selecting the same split is a no-op. Active or completed workouts cannot be silently replaced or switched. Active home shows Resume; user can finish/discard using existing guarded flows.
- Override does not change rotation until completed. Completing a chosen Legs session means next suggested Push. History-based prefills and all existing protections remain.
- API: `createWorkout(state: AppState, now: Long, id: String, selectedSplit: Split? = null): AppState`; existing calls remain valid. Add `changeWorkoutSplit(state: AppState, workoutId: String, split: Split): AppState` for unstarted drafts only; invalid IDs or started/completed sessions throw IllegalArgumentException. No JSON schema/model changes.
- Remove motivational and redundant screen text, repeated default exercise notes, repeated preview set rows, verbose save/finish guidance. Keep functional labels, weight units (per-hand where relevant), actionable errors, unsaved/discard confirmations, reps/rest information, completion counts and heatmap accessibility. Brief progression information may remain only when useful.
- Acceptance: red–green core and UI tests for explicit selection (all three splits), draft switch, same-split history, rotation after override, active protection and restart persistence. Existing regression suite remains green with intentional text assertions updated. Independent reviewer verifies absence of slogans and full selected-workout flow on narrow and wide emulator, preserving saved data. New versionCode2/versionName0.2.0 debug APK must support install -r without data deletion.

Chief of Staff owns this contract. Kotlin package `com.timsfit.core`, JVM module `:core`; Compose app package `com.timsfit.app`, module `:app`, minSdk 26.

## Core API (exact public names)

All models are Kotlin data classes unless noted. Use String IDs and immutable Lists. Default constructor values are allowed where useful. Epoch timestamps are Long milliseconds. Weights are stored in pounds for this PoC, visibly labeled `lb` throughout; no unit conversion or settings in scope.

```kotlin
enum class Split { PUSH, PULL, LEGS }
data class SetEntry(val weightLb: Double, val reps: Int, val completed: Boolean)
data class ExercisePlan(val id: String, val name: String, val targetReps: Int, val restSeconds: Int, val sets: List<SetEntry>, val note: String)
data class Workout(val id: String, val split: Split, val createdAt: Long, val startedAt: Long?, val completedAt: Long?, val exercises: List<ExercisePlan>, val estimatedMinutes: Int)
data class AppState(val workouts: List<Workout> = emptyList())
object WorkoutEngine {
  fun nextSplit(state: AppState): Split
  fun createWorkout(state: AppState, now: Long, id: String, selectedSplit: Split? = null): AppState
  fun changeWorkoutSplit(state: AppState, workoutId: String, split: Split): AppState
  fun startWorkout(state: AppState, workoutId: String, now: Long): AppState
  fun updateSet(state: AppState, workoutId: String, exerciseId: String, setIndex: Int, weightLb: Double, reps: Int, completed: Boolean): AppState
  fun finishWorkout(state: AppState, workoutId: String, now: Long): AppState
  fun discardWorkout(state: AppState, workoutId: String): AppState
  fun completedDayCounts(state: AppState, zone: java.time.ZoneId): Map<java.time.LocalDate, Int>
}
```

Core functions throw IllegalArgumentException with helpful messages for invalid operations; UI catches and displays them. Missing IDs/indexes are errors, not silent success. All functions return updated immutable state. Timestamp injection makes tests deterministic. No Android or network dependencies in core.

## Behavioral decisions

- One unfinished workout at a time. Create returns the existing state if a draft/active workout exists (UI opens it). Next split follows most recent completed session: Push → Pull → Legs → Push, initially Push. Discard does not advance rotation.
- Generate 4 exercises × 3 sets for each split using free weights and common machines, bounded 30–40 minute estimate that includes warmup/rest. No remote AI.
- Generation follows the revision0.3 history-prefill rules above. Zero is valid for unloaded/bodyweight; reps 1–100 and finite weight 0–1500 are valid.
- Start records startedAt once. Set editing is allowed in started and completed sessions; edited history influences future generation. Finish requires started session and at least one completed set; finish is idempotent (keeps original completion time). Partial sessions are permitted and displayed honestly. No duplicate completions/heatmap counts.
- Duration estimates derive from warmup, execution, rest and transitions.
- History uses completion timestamp in current local timezone; habit heatmap shows completed sessions only. Discard only unfinished workouts. Dates come from actual device clock with injected tests.

## App ownership and persistence

UI specialist owns `app/src/main/java/com/timsfit/app/**` including MainActivity, state holder, JSON file persistence and testable storage seam. Use atomic writes, versioned JSON, local private storage, IO off main thread, save before declaring success, and visible recoverable error without overwriting corrupt data. Keep per-keystroke input drafts in UI and automatically persist valid changed values through the ordered writer. Persist draft creation, start, edits, completion and discard. Reload process state; display local empty/error states. No permission/network/backend dependency. Core specialist owns `core/src/**` only. Bootstrap specialist owns build configuration, manifest/resources/wrapper only. Independent reviewer owns test additions after integration (coordinate overlapping paths).

## Interface

Light warm-white canvas, near-black type, generous spacing, restrained coral primary action, rounded cards. Inspired by simplicity and hierarchy of Airbnb/Apple, not a clone. Reference screenshot informs day/session selection, exercise list and prominent Start action; avoid its dark theme and long duration. Home = suggested session and explicit PPL selection + recent history + habit heatmap. No motivational text or explanatory hero. Workout details = estimated duration, split, exercise sets, start/log/finish. Explicit labels and large targets, scrolling, narrow/wide/foldable layout. Fast local operations, no network on startup or workout interaction. No charts library needed for simple heatmap.
