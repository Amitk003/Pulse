package com.pulse.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pulse.camera.AnalyzerStats
import com.pulse.camera.CameraCapture
import com.pulse.camera.MediapipePoseDetector
import com.pulse.data.CloneState
import com.pulse.data.CloneStateManager
import com.pulse.data.LevelUpInfo
import com.pulse.data.PulseRepository
import com.pulse.movement.Exercise
import com.pulse.movement.SetResult
import kotlinx.coroutines.launch

private enum class Screen {
    SELECT,
    RECORD,
    RESULT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PulseApp()
            }
        }
    }
}

@Composable
private fun PulseApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PulseRepository(context.applicationContext) }
    val clone by repository.cloneFlow.collectAsStateWithLifecycle(
        initialValue = CloneStateManager.fresh()
    )
    var screen by remember { mutableStateOf(Screen.SELECT) }
    var exercise by remember { mutableStateOf(Exercise.SQUAT) }
    var result by remember { mutableStateOf<SetResult?>(null) }
    var stats by remember { mutableStateOf(AnalyzerStats(accepted = 0, dropped = 0)) }
    var levelInfo by remember { mutableStateOf<LevelUpInfo?>(null) }
    when (screen) {
        Screen.SELECT -> SelectScreen(
            clone = clone,
            onPick = {
                exercise = it
                levelInfo = null
                screen = Screen.RECORD
            },
            onSimulateBreak = {
                scope.launch { repository.shiftLastTrainedBy(8) }
            }
        )
        Screen.RECORD -> RecordScreen(
            exercise = exercise,
            onFinish = { finished, finishedStats ->
                result = finished
                stats = finishedStats
                if (finished == null) {
                    levelInfo = null
                    screen = Screen.RESULT
                } else {
                    scope.launch {
                        levelInfo = repository.saveResult(finished)
                        screen = Screen.RESULT
                    }
                }
            },
            onCancel = { screen = Screen.SELECT }
        )
        Screen.RESULT -> ResultScreen(
            result = result,
            stats = stats,
            levelInfo = levelInfo,
            onAgain = { screen = Screen.RECORD },
            onSelect = { screen = Screen.SELECT }
        )
    }
}

@Composable
private fun SelectScreen(
    clone: CloneState,
    onPick: (Exercise) -> Unit,
    onSimulateBreak: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pulse", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text("Train a set. Grow your clone.")
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Clone level ${clone.level}", style = MaterialTheme.typography.titleMedium)
                Text("Strength ${"%.1f".format(clone.strength)} - ${clone.xp} XP")
                Text("Form ${clone.formMastery.toInt()} - trained ${clone.consistency.times(5).toInt()} of 5 days")
            }
        }
        Spacer(Modifier.height(16.dp))
        for (exercise in Exercise.entries) {
            Button(
                onClick = { onPick(exercise) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text(exercise.id.replaceFirstChar { it.uppercase() })
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSimulateBreak) {
            Text("Simulate +8 days (debug)")
        }
    }
}

@Composable
private fun RecordScreen(
    exercise: Exercise,
    onFinish: (SetResult?, AnalyzerStats) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val main = remember { Handler(Looper.getMainLooper()) }
    var reps by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<PreviewView?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val detector = remember { MediapipePoseDetector() }
    val capture = remember { CameraCapture(detector) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val view = preview
    LaunchedEffect(view, hasPermission) {
        if (view != null && hasPermission) {
            try {
                capture.start(context, owner, view, exercise) { r, h ->
                    main.post {
                        reps = r
                        hint = h
                    }
                }
            } catch (e: Exception) {
                main.post { hint = "Camera failed: ${e.message}" }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { capture.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).also { preview = it } },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(exercise.id, style = MaterialTheme.typography.titleMedium)
                    Text("$reps reps", style = MaterialTheme.typography.displayLarge)
                    if (hint.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(hint)
                    }
                }
            }

            if (!hasPermission) {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Camera is needed to count reps.")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Allow camera")
                            }
                            OutlinedButton(onClick = onCancel) { Text("Back") }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        capture.stop()
                        onFinish(capture.finish(), capture.stats())
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Finish set") }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(
    result: SetResult?,
    stats: AnalyzerStats,
    levelInfo: LevelUpInfo?,
    onAgain: () -> Unit,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (result == null) {
            Text("No result for this set.", style = MaterialTheme.typography.titleLarge)
        } else {
            Text(result.exercise, style = MaterialTheme.typography.titleMedium)
            Text("${result.reps} reps", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text("Form score: ${result.formScore}")
            Text("Time: ${result.durationSec}s")
            Text("Analyzer: ${stats.accepted} frames, ${stats.dropped} dropped")
            if (levelInfo != null) {
                Spacer(Modifier.height(8.dp))
                Text("+${levelInfo.xpGained} XP")
                if (levelInfo.comebackApplied) {
                    Text("Comeback bonus x1.75")
                }
                if (levelInfo.decayed) {
                    Text("Clone rested long, strength dipped first")
                }
                if (levelInfo.leveledUp) {
                    Text("Level up! Now level ${levelInfo.newLevel}")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (result.mistakes.isEmpty()) {
                Text("Clean set. No mistakes found.")
            } else {
                Text("Watch out for:")
                for (mistake in result.mistakes) {
                    Text("- $mistake")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAgain) { Text("Train again") }
            OutlinedButton(onClick = onSelect) { Text("Exercises") }
        }
    }
}
