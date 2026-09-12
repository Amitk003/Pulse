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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pulse.app.ui.ExerciseVisuals
import com.pulse.app.ui.components.PulseButton
import com.pulse.app.ui.components.PulseCard
import com.pulse.app.ui.components.SectionHeader
import com.pulse.app.ui.components.pulseClickable
import com.pulse.app.ui.theme.PulseColors
import com.pulse.app.ui.theme.PulseShape
import com.pulse.app.ui.theme.PulseSpacing
import com.pulse.movement.Exercise

@Composable
fun DetailScreen(
    exercise: Exercise,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val visual = ExerciseVisuals.forExercise(exercise)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PulseSpacing.lg)
    ) {
        Spacer(Modifier.height(PulseSpacing.md))

        // Top bar: back arrow + title.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = PulseColors.TextPrimary,
                modifier = Modifier
                    .size(40.dp)
                    .pulseClickable { onBack() }
            )
            Spacer(Modifier.width(PulseSpacing.sm))
            Text(
                text = visual.title,
                style = MaterialTheme.typography.headlineMedium,
                color = PulseColors.TextPrimary
            )
        }

        Spacer(Modifier.height(PulseSpacing.sm))
        Text(
            text = visual.category,
            style = MaterialTheme.typography.bodyMedium,
            color = PulseColors.TextSecondary
        )

        Spacer(Modifier.height(PulseSpacing.xl))

        // Hero: tinted icon block, no fake imagery.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(PulseShape.card)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(visual.accent.copy(alpha = 0.25f), PulseColors.Surface)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                visual.icon,
                contentDescription = visual.title,
                tint = visual.accent,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(Modifier.height(PulseSpacing.xl))

        SectionHeader(title = "Target")
        Spacer(Modifier.height(PulseSpacing.sm))
        PulseCard(contentPadding = PulseSpacing.md) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(visual.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Adjust,
                        contentDescription = null,
                        tint = visual.accent
                    )
                }
                Spacer(Modifier.width(PulseSpacing.md))
                Text(
                    text = visual.target,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PulseColors.TextPrimary
                )
            }
        }

        Spacer(Modifier.height(PulseSpacing.xl))

        SectionHeader(title = "How it works")
        Spacer(Modifier.height(PulseSpacing.sm))
        PulseCard(contentPadding = PulseSpacing.md) {
            StepRow("Camera checks your form")
            StepRow("Complete reps")
            StepRow("Earn XP")
        }

        Spacer(Modifier.height(PulseSpacing.xxl))

        PulseButton(text = "Start Workout", onClick = onStart)

        Spacer(Modifier.height(PulseSpacing.xl))
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun StepRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(PulseColors.Accent)
        )
        Spacer(Modifier.width(PulseSpacing.md))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = PulseColors.TextSecondary
        )
    }
}
