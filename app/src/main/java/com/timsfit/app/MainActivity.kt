package com.timsfit.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import kotlinx.coroutines.delay
import com.timsfit.core.*
import java.time.*
import java.time.format.DateTimeFormatter

private val Forest = Color(0xFF285C47)
private val Sage = Color(0xFFE4EBDD)
private val Line = Color(0xFFE4E3DB)
private val Ink = Color(0xFF202D27)
private val Canvas = Color(0xFFF7F7F0)
private val Muted = Color(0xFF677168)
private fun Split.title() = name.lowercase().replaceFirstChar { it.uppercase() }
private fun date(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a"))

class MainActivity : ComponentActivity() {
    private val model: FitViewModel by viewModels()
    override fun onStop() {
        model.flush()
        super.onStop()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)), colorScheme = lightColorScheme(primary = Forest, background = Canvas,
                surface = Color.White, onSurface = Ink, onBackground = Ink,
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFFCFBF8),
                surfaceContainer = Color.White,
                surfaceContainerHigh = Color(0xFFF3F1EC),
                surfaceContainerHighest = Color(0xFFEFEDE7),
                surfaceVariant = Color(0xFFEFEDE7),
                surfaceBright = Color.White,
                surfaceDim = Color(0xFFE5E2DB),
                surfaceTint = Forest,
                onSurfaceVariant = Muted,
                outline = Color(0xFF85847C),
                outlineVariant = Color(0xFFDCD9D1))) {
                TimsFit(model)
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
    val goHome: () -> Unit = { if (dirtySets.isNotEmpty()) pendingNavigation = "home" else model.change({ it }) { selectedId = null } }
    BackHandler(enabled = workout != null) { goHome() }
    Surface(color = Canvas, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Row(Modifier.widthIn(max = 840.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Forest, shape = RoundedCornerShape(12.dp)) {
                        Text("t.", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                    Text("TimsFit", fontSize = 22.sp, letterSpacing = (-0.8).sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 10.dp))
                }
            }
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.error?.let { message ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE7E2))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(message, color = Ink)
                        if (!ui.loaded) TextButton(onClick = model::reload, enabled = !ui.busy) { Text("Retry loading") }
                        else TextButton(onClick = model::retry) { Text("Retry saving") }
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
                WorkoutScreen(workout, ui.busy, ui.error != null, onBack = goHome,
                    onSplit = { split -> model.change({ WorkoutEngine.changeWorkoutSplit(it, workout.id, split) }) },
                    onStart = { model.change({ WorkoutEngine.startWorkout(it, workout.id, System.currentTimeMillis()) }) },
                    onFinish = {
                        if (dirtySets.isNotEmpty()) pendingNavigation = "finish"
                        else model.change({ WorkoutEngine.finishWorkout(it, workout.id, System.currentTimeMillis()) }) { selectedId = null }
                    },
                    onDirty = { key, dirty -> dirtySets = if (dirty) (dirtySets + key).distinct() else dirtySets - key },
                    onDiscard = { discard = true },
                    onSet = { exercise, index, weight, reps, weightEdited, repsEdited, saved ->
                        model.change({ WorkoutEngine.updateSet(it, workout.id, exercise, index, weight, reps, true, weightEdited, repsEdited) }) { saved() }
                    })
            }
        }
    }
    if (pendingNavigation != null) AlertDialog(onDismissRequest = { pendingNavigation = null },
        title = { Text("Unsaved set changes") },
        text = { Text("Some values are incomplete or invalid. Continuing keeps the last valid saved values.") },
        confirmButton = { TextButton(onClick = {
            val action = pendingNavigation
            pendingNavigation = null
            if (action == "finish" && workout != null) {
                model.change({ WorkoutEngine.finishWorkout(it, workout.id, System.currentTimeMillis()) }) {
                    dirtySets = emptyList(); selectedId = null
                }
            } else { model.change({ it }) { dirtySets = emptyList(); selectedId = null } }
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
                label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(split.title(), fontWeight = FontWeight.SemiBold) } }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Forest, selectedLabelColor = Color.White))
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
        item { Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")).uppercase(), color = Muted, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
            Text("Your training.", fontSize = 40.sp, lineHeight = 44.sp,
                letterSpacing = (-1.8).sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Sage)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (unfinished == null) {
                    Text("NEXT SESSION", color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("${Split.valueOf(selected).title()} day", fontSize = 32.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.Bold)
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
        } } }
        item { Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            Text("Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            HabitHeatmap(state)
        } }
        item { Text("History", modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (history.isEmpty()) item {
            Surface(Modifier.widthIn(max = 840.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                color = Color.White, border = BorderStroke(1.dp, Line)) {
                Text("No completed workouts.", modifier = Modifier.padding(24.dp), color = Muted, lineHeight = 24.sp)
            }
        }
        items(history, key = { it.id }) { workout ->
            Card(onClick = { onOpen(workout.id) }, modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${workout.split.title()} day", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(date(workout.completedAt!!), color = Muted, fontSize = 13.sp)
                        Text("Duration ${durationText(elapsedSeconds(workout, 0))}", color = Muted, fontSize = 13.sp)
                        val sets = workout.exercises.flatMap { it.sets }
                        Text("${sets.count { it.completed }}/${sets.size} sets completed", color = Muted, fontSize = 13.sp)
                    }
                    Text("View / edit", color = Forest)
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
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
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
                        Box(Modifier.size(17.dp).background(if (d > today) Canvas else if (count > 0) Forest else Color(0xFFEAEEE6), RoundedCornerShape(4.dp))
                            .semantics { contentDescription = "$d: $count completed sessions${if (d > today) ", future day" else ""}" })
                    }
                } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${start.format(DateTimeFormatter.ofPattern("MMM d"))} — Today", fontSize = 12.sp, color = Muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(Forest, RoundedCornerShape(2.dp)))
                    Text("Completed", fontSize = 12.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun WorkoutScreen(workout: Workout, busy: Boolean, saveFailed: Boolean, onBack: () -> Unit, onSplit: (Split) -> Unit, onStart: () -> Unit,
    onFinish: () -> Unit, onDiscard: () -> Unit, onDirty: (String, Boolean) -> Unit,
    onSet: (String, Int, Double, Int, Boolean, Boolean, () -> Unit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("‹  Home") }
            Text("${workout.split.title()} day", fontSize = 40.sp, letterSpacing = (-1.5).sp, fontWeight = FontWeight.Bold)
            Text("${workout.estimatedMinutes} min · ${workout.exercises.size} exercises · Weights in lb", color = Muted)
            Text("Demos open browser · Internet required", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            if (workout.startedAt != null) SessionTimer(workout)
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
            Card(Modifier.widthIn(max = 840.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("EXERCISE ${(workout.exercises.indexOf(exercise) + 1).toString().padStart(2, '0')}",
                        color = Forest, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 21.sp, letterSpacing = (-0.5).sp, modifier = Modifier.weight(1f))
                        DemoLink(exercise)
                    }
                    val perHand = exercise.note.contains("per hand", ignoreCase = true)
                    Text("${exercise.sets.size} × ${exercise.targetReps} reps · ${exercise.restSeconds}s rest${if (perHand) " · lb per hand" else ""}", color = Muted, fontSize = 13.sp)
                    if (workout.startedAt == null) {
                        val weights = exercise.sets.map { it.weightLb }
                        Text(if (weights.distinct().size == 1) "${weightText(weights.first())} lb" else weights.joinToString(" / ") { weightText(it) } + " lb", color = Muted)
                    } else exercise.sets.forEachIndexed { index, set ->
                        key(workout.id, exercise.id, index) {
                            SetEditor(set, index, saveFailed, exercise.name, { dirty -> onDirty("${exercise.id}:$index", dirty) }) { weight, reps, weightEdited, repsEdited, saved -> onSet(exercise.id, index, weight, reps, weightEdited, repsEdited, saved) }
                        }
                    }
                }
            }
        }
        if (workout.completedAt == null) item { Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            if (workout.startedAt != null) {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Finish workout") }
            }
            TextButton(onClick = onDiscard, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Discard unfinished workout") }
        } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SetEditor(set: SetEntry, index: Int, saveFailed: Boolean, exerciseName: String, onDirty: (Boolean) -> Unit,
    onSave: (Double, Int, Boolean, Boolean, () -> Unit) -> Unit) {
    // Never key drafts to saved numbers: an older IO acknowledgment must not move the cursor
    // or replace newer text, including a trailing decimal point.
    var weight by rememberSaveable { mutableStateOf(weightText(set.weightLb)) }
    var reps by rememberSaveable { mutableStateOf(set.reps.toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var clearZeroOnFocus by remember { mutableStateOf(false) }
    var weightFocused by remember { mutableStateOf(false) }
    var weightLocallyEdited by rememberSaveable { mutableStateOf(false) }
    var repsLocallyEdited by rememberSaveable { mutableStateOf(false) }
    // Refresh untouched prefills when an earlier set changes. Never replace an
    // in-progress draft or cursor with an asynchronous save acknowledgment.
    LaunchedEffect(set.weightLb, set.reps) {
        if (!weightLocallyEdited) weight = weightText(set.weightLb)
        if (!repsLocallyEdited) reps = set.reps.toString()
    }
    fun edit(w: String, r: String) {
        if (w == weight && r == reps) return
        val weightChanged = w != weight
        val repsChanged = r != reps
        weightLocallyEdited = weightLocallyEdited || weightChanged
        repsLocallyEdited = repsLocallyEdited || repsChanged
        weight = w; reps = r
        val entry = editedSet(w, r)
        error = if (entry == null) "Enter 0–1500 lb and 1–100 reps." else null
        onDirty(entry == null)
        if (entry != null) {
            onSave(entry.weightLb, entry.reps, weightLocallyEdited, repsLocallyEdited) {}
        }
    }
    Column(Modifier.fillMaxWidth().background(Canvas, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Set ${index + 1}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(if (error != null) "Not saved" else if (editedSet(weight, reps) == set) "Saved ✓" else if (saveFailed) "Not saved" else if (editedSet(weight, reps)?.let { it.weightLb != set.weightLb || it.reps != set.reps } == true) "Saving…" else "", color = Muted, fontSize = 12.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 260.dp || LocalDensity.current.fontScale > 1.3f
            val weightInput: @Composable (Modifier) -> Unit = { fieldModifier ->
                // Clearing the displayed default is not a workout edit. If the user
                // leaves without typing, show the saved zero again without logging a set.
                OutlinedTextField(if (clearZeroOnFocus && weight == "0") "" else weight,
                    { clearZeroOnFocus = false; edit(it, reps) }, label = { Text("Weight (lb)") },
                    modifier = fieldModifier.onFocusChanged { focus ->
                        if (focus.isFocused && !weightFocused) clearZeroOnFocus = weight == "0"
                        if (!focus.isFocused) clearZeroOnFocus = false
                        weightFocused = focus.isFocused
                    }.semantics { contentDescription = "$exerciseName set ${index + 1} weight in lb" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    isError = error != null, shape = RoundedCornerShape(12.dp))
            }
            val repsInput: @Composable (Modifier) -> Unit = { fieldModifier ->
                OutlinedTextField(reps, { edit(weight, it) }, label = { Text("Reps") },
                    modifier = fieldModifier.semantics { contentDescription = "$exerciseName set ${index + 1} reps" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    isError = error != null, shape = RoundedCornerShape(12.dp))
            }
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                weightInput(Modifier.fillMaxWidth())
                repsInput(Modifier.fillMaxWidth())
            } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                weightInput(Modifier.weight(1f))
                repsInput(Modifier.weight(1f))
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}

@Composable
private fun SessionTimer(workout: Workout) {
    var now by remember(workout.id) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.id, workout.completedAt) {
        while (workout.completedAt == null) { now = System.currentTimeMillis(); delay(1000) }
    }
    Surface(color = Sage, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(bottom = 10.dp)) {
        Text("${if (workout.completedAt == null) "Elapsed" else "Duration"} ${durationText(elapsedSeconds(workout, now))}",
            color = Forest, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

@Composable
private fun DemoLink(exercise: ExercisePlan) {
    val url = exerciseDemoUrl(exercise.id) ?: return
    val context = LocalContext.current
    var error by remember { mutableStateOf(false) }
    TextButton(onClick = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) { error = true }
        catch (_: SecurityException) { error = true }
    }, modifier = Modifier.semantics { contentDescription = "Demo for ${exercise.name}. Opens browser; internet required." }) { Text("Demo ↗") }
    if (error) AlertDialog(onDismissRequest = { error = false }, title = { Text("Could not open browser") },
        text = { Text("Install or enable a browser to view the demo. Internet is required.") },
        confirmButton = { TextButton(onClick = { error = false }) { Text("OK") } })
}
