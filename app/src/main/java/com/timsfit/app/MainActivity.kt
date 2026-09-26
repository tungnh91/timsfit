package com.timsfit.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.timsfit.core.*
import java.time.*
import java.time.format.DateTimeFormatter

private val Coral = Color(0xFFD94738)
private val Ink = Color(0xFF252521)
private val Canvas = Color(0xFFFAF8F4)
private val Muted = Color(0xFF686862)
private fun Split.title() = name.lowercase().replaceFirstChar { it.uppercase() }
private fun date(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a"))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Coral, background = Canvas,
                surface = Color.White, onSurface = Ink, onBackground = Ink,
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFFCFBF8),
                surfaceContainer = Color.White,
                surfaceContainerHigh = Color(0xFFF3F1EC),
                surfaceContainerHighest = Color(0xFFEFEDE7),
                surfaceVariant = Color(0xFFEFEDE7),
                surfaceBright = Color.White,
                surfaceDim = Color(0xFFE5E2DB),
                surfaceTint = Coral,
                onSurfaceVariant = Muted,
                outline = Color(0xFF85847C),
                outlineVariant = Color(0xFFDCD9D1))) {
                TimsFit(viewModel())
            }
        }
    }
}

@Composable
fun TimsFit(model: FitViewModel) {
    val ui by model.ui.collectAsState()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var dirtySets by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var pendingNavigation by rememberSaveable { mutableStateOf<String?>(null) }
    val workout = ui.state.workouts.find { it.id == selectedId }
    val goHome: () -> Unit = { if (dirtySets.isNotEmpty()) pendingNavigation = "home" else selectedId = null }
    BackHandler(enabled = workout != null) { goHome() }
    Surface(color = Canvas, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("TimsFit", fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.error?.let { message ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE7E2))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(message, color = Ink)
                        if (!ui.loaded) TextButton(onClick = model::reload, enabled = !ui.busy) { Text("Retry loading") }
                        else TextButton(onClick = model::dismissError) { Text("Dismiss") }
                    }
                }
            }
            if (!ui.loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (ui.busy) "Opening your training log…" else "Your saved log is protected. Retry to open it.",
                        modifier = Modifier.padding(24.dp), color = Muted)
                }
            } else if (workout == null) {
                Home(ui.state, ui.busy, onOpen = { selectedId = it }, onCreate = { split ->
                    model.change({ WorkoutEngine.createWorkout(it, System.currentTimeMillis(), java.util.UUID.randomUUID().toString(), split) }) {
                        selectedId = it.workouts.firstOrNull { w -> w.completedAt == null }?.id
                    }
                })
            } else {
                WorkoutScreen(workout, ui.busy, onBack = goHome,
                    onSplit = { split -> model.change({ WorkoutEngine.changeWorkoutSplit(it, workout.id, split) }) },
                    onStart = { model.change({ WorkoutEngine.startWorkout(it, workout.id, System.currentTimeMillis()) }) },
                    onFinish = {
                        if (dirtySets.isNotEmpty()) pendingNavigation = "finish"
                        else model.change({ WorkoutEngine.finishWorkout(it, workout.id, System.currentTimeMillis()) }) { selectedId = null }
                    },
                    onDirty = { key, dirty -> dirtySets = if (dirty) (dirtySets + key).distinct() else dirtySets - key },
                    onDiscard = { discard = true },
                    onSet = { exercise, index, weight, reps, complete, saved ->
                        model.change({ WorkoutEngine.updateSet(it, workout.id, exercise, index, weight, reps, complete) }) { saved() }
                    })
            }
        }
    }
    if (pendingNavigation != null) AlertDialog(onDismissRequest = { pendingNavigation = null },
        title = { Text("Unsaved set changes") },
        text = { Text("Use Save set for each edited set to keep your changes. Continuing will use only the sets already saved.") },
        confirmButton = { TextButton(onClick = {
            val action = pendingNavigation
            pendingNavigation = null
            if (action == "finish" && workout != null) {
                model.change({ WorkoutEngine.finishWorkout(it, workout.id, System.currentTimeMillis()) }) {
                    dirtySets = emptyList(); selectedId = null
                }
            } else { dirtySets = emptyList(); selectedId = null }
        }) { Text("Continue without changes") } },
        dismissButton = { TextButton(onClick = { pendingNavigation = null }) { Text("Keep editing") } })
    if (discard && workout != null) AlertDialog(onDismissRequest = { discard = false },
        title = { Text("Discard this ${workout.split.title()} day?") },
        text = { Text("This unfinished workout and its logged sets will be removed. Your completed history will stay.") },
        confirmButton = { TextButton(enabled = !ui.busy, onClick = {
            model.change({ WorkoutEngine.discardWorkout(it, workout.id) }) { discard = false; dirtySets = emptyList(); selectedId = null }
        }) { Text("Discard workout") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep workout") } })
}

@Composable
private fun SplitSelector(selected: Split, enabled: Boolean, onSelect: (Split) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Split.entries.forEach { split ->
            FilterChip(selected = selected == split, onClick = { onSelect(split) }, enabled = enabled,
                label = { Text(split.title()) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFE7E2), selectedLabelColor = Ink))
        }
    }
}

@Composable
private fun Home(state: AppState, busy: Boolean, onOpen: (String) -> Unit, onCreate: (Split) -> Unit) {
    val unfinished = state.workouts.firstOrNull { it.completedAt == null }
    val suggested = WorkoutEngine.nextSplit(state)
    var selected by rememberSaveable(suggested) { mutableStateOf(suggested.name) }
    val history = state.workouts.filter { it.completedAt != null }.sortedByDescending { it.completedAt }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Card(Modifier.widthIn(max = 840.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (unfinished == null) {
                    Text("Suggested: ${suggested.title()}", color = Muted, fontSize = 14.sp)
                    SplitSelector(Split.valueOf(selected), !busy) { selected = it.name }
                    Text("30–40 min · 4 exercises", color = Muted)
                } else {
                    Text("${unfinished.split.title()} day", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("${unfinished.estimatedMinutes} min · ${unfinished.exercises.size} exercises", color = Muted)
                }
                Button(onClick = { if (unfinished == null) onCreate(Split.valueOf(selected)) else onOpen(unfinished.id) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (unfinished == null) "Create workout" else if (unfinished.startedAt == null) "Open workout" else "Resume")
                }
            }
        } }
        item { Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            Text("Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            HabitHeatmap(state)
        } }
        item { Text("History", modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (history.isEmpty()) item { Text("No completed workouts.",
            modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(), color = Muted, lineHeight = 24.sp) }
        items(history, key = { it.id }) { workout ->
            Card(onClick = { onOpen(workout.id) }, modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${workout.split.title()} day", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(date(workout.completedAt!!), color = Muted, fontSize = 13.sp)
                        val sets = workout.exercises.flatMap { it.sets }
                        Text("${sets.count { it.completed }}/${sets.size} sets completed", color = Muted, fontSize = 13.sp)
                    }
                    Text("View / edit", color = Coral)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HabitHeatmap(state: AppState) {
    val counts = WorkoutEngine.completedDayCounts(state, ZoneId.systemDefault())
    val today = LocalDate.now()
    val start = today.minusWeeks(11).with(java.time.DayOfWeek.MONDAY)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${counts.filterKeys { !it.isBefore(start) && !it.isAfter(today) }.values.sum()} sessions · 12 weeks", color = Muted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("M", "", "W", "", "F", "", "S").forEach { Text(it, fontSize = 10.sp, modifier = Modifier.size(17.dp)) }
                }
                repeat(12) { week -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(7) { day ->
                        val d = start.plusDays((week * 7 + day).toLong())
                        val count = counts[d] ?: 0
                        Box(Modifier.size(17.dp).background(if (d > today) Canvas else if (count > 0) Coral else Color(0xFFE9E6E0), RoundedCornerShape(4.dp))
                            .semantics { contentDescription = "$d: $count completed sessions${if (d > today) ", future day" else ""}" })
                    }
                } }
            }
            Text("${start.format(DateTimeFormatter.ofPattern("MMM d"))} — Today", fontSize = 12.sp, color = Muted)
        }
    }
}

@Composable
private fun WorkoutScreen(workout: Workout, busy: Boolean, onBack: () -> Unit, onSplit: (Split) -> Unit, onStart: () -> Unit,
    onFinish: () -> Unit, onDiscard: () -> Unit, onDirty: (String, Boolean) -> Unit,
    onSet: (String, Int, Double, Int, Boolean, () -> Unit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("‹  Home") }
            Text("${workout.split.title()} day", fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("${workout.estimatedMinutes} min · ${workout.exercises.size} exercises · Weights in lb", color = Muted)
            Spacer(Modifier.height(12.dp))
            if (workout.completedAt != null) {
                Text("Completed ${date(workout.completedAt!!)}", color = Muted)
            } else if (workout.startedAt == null) {
                SplitSelector(workout.split, !busy, onSplit)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Start workout") }
            } else {
                val sets = workout.exercises.flatMap { it.sets }
                Text("${sets.count { it.completed }} of ${sets.size} sets completed", color = Muted)
            }
        } }
        items(workout.exercises, key = { it.id }) { exercise ->
            Card(Modifier.widthIn(max = 840.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    val perHand = exercise.note.contains("per hand", ignoreCase = true)
                    Text("${exercise.sets.size} × ${exercise.targetReps} reps · ${exercise.restSeconds}s rest${if (perHand) " · lb per hand" else ""}", color = Muted, fontSize = 13.sp)
                    val progression = exercise.note.removeSuffix(" Weight is per hand.")
                        .removePrefix("Choose your starting weight.").removePrefix("Repeat your previous working weights.")
                        .removePrefix("Targets met last time. ").trim()
                    if (progression.isNotEmpty()) Text(progression, color = Muted, fontSize = 13.sp)
                    if (workout.startedAt == null) {
                        val weights = exercise.sets.map { it.weightLb }
                        Text(if (weights.distinct().size == 1) "${weights.first()} lb" else weights.joinToString(" / ") + " lb", color = Muted)
                    } else exercise.sets.forEachIndexed { index, set ->
                        key(workout.id, exercise.id, index) {
                            SetEditor(set, index, busy, exercise.name, { dirty -> onDirty("${exercise.id}:$index", dirty) }) { weight, reps, completed, saved -> onSet(exercise.id, index, weight, reps, completed, saved) }
                        }
                    }
                }
            }
        }
        if (workout.completedAt == null) item { Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            if (workout.startedAt != null) {
                Button(onClick = onFinish, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Finish workout") }
            }
            TextButton(onClick = onDiscard, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Discard unfinished workout") }
        } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SetEditor(set: SetEntry, index: Int, busy: Boolean, exerciseName: String, onDirty: (Boolean) -> Unit,
    onSave: (Double, Int, Boolean, () -> Unit) -> Unit) {
    var weight by rememberSaveable(set.weightLb) { mutableStateOf(set.weightLb.toString()) }
    var reps by rememberSaveable(set.reps) { mutableStateOf(set.reps.toString()) }
    var complete by rememberSaveable(set.completed) { mutableStateOf(set.completed) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saved by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Set ${index + 1}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(weight, { weight = it; saved = false; onDirty(true) }, label = { Text("Weight (lb)") },
                modifier = Modifier.weight(1f).semantics { contentDescription = "$exerciseName set ${index + 1} weight in lb" },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, enabled = !busy)
            OutlinedTextField(reps, { reps = it; saved = false; onDirty(true) }, label = { Text("Reps") },
                modifier = Modifier.weight(1f).semantics { contentDescription = "$exerciseName set ${index + 1} reps" },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = !busy)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = complete, onCheckedChange = { complete = it; saved = false; onDirty(true) }, enabled = !busy,
                modifier = Modifier.semantics { contentDescription = "$exerciseName set ${index + 1} completed" })
            Text("Completed", modifier = Modifier.weight(1f), fontSize = 13.sp)
            TextButton(enabled = !busy, onClick = {
                val w = weight.toDoubleOrNull(); val r = reps.toIntOrNull()
                if (w == null || !w.isFinite() || w !in 0.0..1500.0 || r == null || r !in 1..100) {
                    error = "Enter 0–1500 lb and 1–100 reps."
                } else { error = null; onSave(w, r, complete) { saved = true; onDirty(false) } }
            }) { Text(if (saved) "Saved ✓" else "Save set") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}
