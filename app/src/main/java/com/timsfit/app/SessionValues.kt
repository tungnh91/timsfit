package com.timsfit.app

import com.timsfit.core.SetEntry
import com.timsfit.core.Workout
import java.math.BigDecimal
import java.util.Locale

internal fun weightText(weight: Double): String = BigDecimal.valueOf(weight).stripTrailingZeros().toPlainString()
internal fun editedSet(weight: String, reps: String): SetEntry? {
    val w = weight.toDoubleOrNull() ?: return null
    val r = reps.toIntOrNull() ?: return null
    return if (w.isFinite() && w in 0.0..1500.0 && r in 1..100) SetEntry(w, r, true) else null
}
internal fun elapsedSeconds(workout: Workout, now: Long): Long {
    val start = workout.startedAt ?: return 0
    val end = workout.completedAt ?: now
    if (end <= start) return 0
    // Saturate the pathological overflow case without changing ordinary epoch arithmetic.
    val millis = try { Math.subtractExact(end, start) } catch (_: ArithmeticException) { Long.MAX_VALUE }
    return millis / 1000
}
internal fun durationText(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return if (s < 3600) String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60)
    else String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
}
