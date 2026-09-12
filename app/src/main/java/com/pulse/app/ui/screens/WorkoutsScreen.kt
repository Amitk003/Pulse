package com.pulse.app.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pulse.app.ui.ExerciseVisuals
import com.pulse.app.ui.components.PulseCard
import com.pulse.app.ui.components.PulseChip
import com.pulse.app.ui.components.pulseClickable
import com.pulse.app.ui.theme.PulseColors
import com.pulse.app.ui.theme.PulseShape
import com.pulse.app.ui.theme.PulseSpacing
import com.pulse.movement.Exercise

@Composable
fun WorkoutsScreen(
    onBack: () -> Unit,
    onPickExercise: (Exercise) -> Unit
) {
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Lower Body", "Upper Body", "Core")
    val visible = ExerciseVisuals.categoryFilter(filter)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PulseSpacing.lg)
    ) {
        Spacer(Modifier.height(PulseSpacing.md))
        Text(
            text = "Workouts",
            style = MaterialTheme.typography.headlineMedium,
            color = PulseColors.TextPrimary
        )
        Spacer(Modifier.height(PulseSpacing.lg))

        Row(horizontalArrangement = Arrangement.spacedBy(PulseSpacing.sm)) {
            for (f in filters) {
                PulseChip(text = f, selected = filter == f, onClick = { filter = f })
            }
        }

        Spacer(Modifier.height(PulseSpacing.lg))

        if (visible.isEmpty()) {
            Text(
                text = "No core workouts yet. Squat, push-up and hinge are ready to train.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseColors.TextSecondary
            )
        }
        for (visual in visible) {
            WorkoutCard(visual = visual, onClick = { onPickExercise(visual.exercise) })
            Spacer(Modifier.height(PulseSpacing.sm))
        }

        Spacer(Modifier.height(PulseSpacing.xxl))
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun WorkoutCard(
    visual: com.pulse.app.ui.ExerciseVisual,
    onClick: () -> Unit
) {
    PulseCard(onClick = onClick, contentPadding = PulseSpacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(PulseShape.small)
                    .background(visual.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    visual.icon,
                    contentDescription = visual.title,
                    tint = visual.accent
                )
            }
            Spacer(Modifier.width(PulseSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = visual.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PulseColors.TextPrimary
                )
                Text(
                    text = visual.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseColors.TextSecondary
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = PulseColors.TextMuted
            )
        }
    }
}
