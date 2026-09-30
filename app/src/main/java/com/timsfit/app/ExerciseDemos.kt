package com.timsfit.app

/** Verified publisher pages; provenance and equipment choices: docs/EXERCISE-LINKS.md. */
internal fun exerciseDemoUrl(id: String): String? = when (id) {
    "bench-press" -> "https://www.muscleandstrength.com/exercises/barbell-bench-press.html"
    "incline-dumbbell-press" -> "https://www.muscleandstrength.com/exercises/incline-dumbbell-bench-press.html"
    "lateral-raise" -> "https://www.muscleandstrength.com/exercises/dumbbell-lateral-raise.html"
    "triceps-pushdown" -> "https://www.muscleandstrength.com/videos/how-to-perfect-your-tricep-pushdown"
    "lat-pulldown" -> "https://www.muscleandstrength.com/exercises/lat-pull-down.html"
    "cable-row" -> "https://www.muscleandstrength.com/exercises/seated-row.html"
    "reverse-fly" -> "https://www.muscleandstrength.com/exercises/bent-over-dumbbell-reverse-fly.html"
    "dumbbell-curl" -> "https://www.muscleandstrength.com/exercises/standing-dumbbell-curl.html"
    "squat" -> "https://www.muscleandstrength.com/exercises/squat.html"
    "romanian-deadlift" -> "https://www.dvidshub.net/video/639938/dumbbell-romanian-deadlift"
    "leg-curl" -> "https://www.muscleandstrength.com/exercises/leg-curl.html"
    "calf-raise" -> "https://www.muscleandstrength.com/exercises/standing-machine-calf-raise"
    else -> null
}
