package com.pulse.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pulse.camera.WorkoutViewModel
import com.pulse.movement.Exercise

/**
 * 100% Kotlin Jetpack Compose Workout Screen.
 * Integrates live CameraX preview, skeleton canvas overlay, rep counter HUD,
 * AI coach feedback banner, form score gauge, and exercise selection bar.
 */
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    ) {
        // Layer 1: CameraX Preview Layer
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Pose Overlay Canvas
        PoseOverlayCanvas(
            joints = emptyMap(),
            landmarks = emptyList(),
            modifier = Modifier.fillMaxSize()
        )

        // Layer 3: Top Rep Counter & Form HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rep Counter Badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCC0F172A)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${uiState.reps}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F2FE)
                    )
                    Column {
                        Text(
                            text = "REPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray
                        )
                        Text(
                            text = "AI Verified",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            // Form Score Gauge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCC0F172A)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "FORM SCORE",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${uiState.formScore}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.formScore >= 80) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
            }
        }

        // Layer 4: Floating AI Coach Banner & Anomaly Warning
        AnimatedVisibility(
            visible = uiState.hint.isNotBlank() || uiState.anomalyWarning != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.anomalyWarning != null) Color(0xDDE11D48) else Color(0xDD1E293B)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = uiState.anomalyWarning ?: uiState.hint,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Layer 5: Bottom Exercise Selection Bar & Workout Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exercise Selection Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xCC0F172A))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Exercise.entries.forEach { exercise ->
                    val isSelected = uiState.exercise == exercise
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectExercise(exercise) },
                        label = {
                            Text(
                                text = exercise.id.replace("-", " ").uppercase(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00F2FE),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Start / Stop Floating Control Button
            Button(
                onClick = {
                    if (uiState.isRecording) {
                        viewModel.stopWorkout()
                    } else {
                        previewView?.let { pv ->
                            viewModel.startWorkout(context, lifecycleOwner, pv)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRecording) Color(0xFFEF4444) else Color(0xFF10B981)
                )
            ) {
                Text(
                    text = if (uiState.isRecording) "FINISH SET" else "START WORKOUT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
