package com.pulse.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pulse.app.ui.ExerciseVisuals
import com.pulse.app.ui.components.AnimatedCountText
import com.pulse.app.ui.components.PulseButton
import com.pulse.app.ui.components.PulseHaptic
import com.pulse.app.ui.components.RepPulse
import com.pulse.app.ui.components.pulseClickable
import com.pulse.app.ui.components.rememberHaptics
import com.pulse.app.ui.theme.PulseColors
import com.pulse.app.ui.theme.PulseShape
import com.pulse.app.ui.theme.PulseSpacing
import com.pulse.camera.AnalyzerStats
import com.pulse.camera.CameraCapture
import com.pulse.camera.MediapipePoseDetector
import com.pulse.movement.Exercise
import com.pulse.movement.SetResult

/**
 * Focused recording view. Camera first, minimal overlay: exercise name,
 * live reps, and the current hint from the movement layer. No fake data.
 */
@Composable
fun ActiveWorkoutScreen(
    exercise: Exercise,
    onExit: () -> Unit,
    onFinished: (SetResult?, AnalyzerStats) -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val main = remember { Handler(Looper.getMainLooper()) }
    val haptics = rememberHaptics()

    var reps by remember { mutableIntStateOf(0) }
    var lastReps by remember { mutableIntStateOf(0) }
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
                        if (r > lastReps) {
                            // Rep detected: light tap, only on change, never per frame.
                            haptics(PulseHaptic.LIGHT)
                            lastReps = r
                        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseColors.Background)
    ) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).also { preview = it } },
            modifier = Modifier.fillMaxSize()
        )

        // Top overlay: exit, exercise name, hint chip.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(PulseSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Exit workout",
                    tint = Color.White,
                    modifier = Modifier
                        .clip(PulseShape.chip)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(8.dp)
                        .pulseClickable { onExit() }
                )
                Text(
                    text = ExerciseVisuals.forExercise(exercise).title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "SET 1/1",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), PulseShape.chip)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (hint.isNotBlank()) {
                Spacer(Modifier.height(PulseSpacing.md))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), PulseShape.chip)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // Bottom overlay: big rep counter + controls, gradient scrim for legibility.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(PulseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RepPulse(trigger = reps) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedCountText(
                        value = reps,
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                }
            }
            Text(
                text = "REPS",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(PulseSpacing.sm))
            val hintColor = when {
                hint.isBlank() -> Color.White.copy(alpha = 0.85f)
                hint.startsWith("Keep") || hint.contains("failed") -> Color(0xFFFFB4A0)
                else -> Color(0xFFB8E6C9)
            }
            Text(
                text = if (hint.isBlank()) "Get in frame to start" else hint,
                style = MaterialTheme.typography.bodyMedium,
                color = hintColor,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), PulseShape.chip)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
            Spacer(Modifier.height(PulseSpacing.lg))
            Row(modifier = Modifier.fillMaxWidth()) {
                PulseButton(
                    text = "Finish Set",
                    onClick = {
                        capture.stop()
                        haptics(PulseHaptic.MEDIUM)
                        onFinished(capture.finish(), capture.stats())
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(PulseSpacing.md))
                PulseButton(
                    text = "Cancel",
                    onClick = onExit,
                    modifier = Modifier.weight(1f),
                    filled = false
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
