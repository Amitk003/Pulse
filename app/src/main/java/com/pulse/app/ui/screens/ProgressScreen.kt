package com.pulse.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.app.ui.ExerciseVisuals
import com.pulse.app.ui.PulseViewModel
import com.pulse.app.ui.components.PulseCard
import com.pulse.app.ui.components.SectionHeader
import com.pulse.app.ui.components.StatItem
import com.pulse.app.ui.components.WeekStrip
import com.pulse.app.ui.components.XpProgressBar
import com.pulse.app.ui.theme.PulseColors
import com.pulse.app.ui.theme.PulseSpacing
import com.pulse.data.CloneStateManager
import com.pulse.movement.Exercise

@Composable
fun ProgressScreen(viewModel: PulseViewModel) {
    val clone by viewModel.clone.collectAsStateWithLifecycle()
    val weekly by viewModel.weekly.collectAsStateWithLifecycle()
    val sessions by viewModel.recentSessions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PulseSpacing.lg)
    ) {
        Spacer(Modifier.height(PulseSpacing.md))
        Text(
            text = "Progress",
            style = MaterialTheme.typography.headlineMedium,
            color = PulseColors.TextPrimary
        )
        Spacer(Modifier.height(PulseSpacing.lg))

        // Clone summary card.
        PulseCard {
            Text(
                text = "Clone Level ${clone.level}",
                style = MaterialTheme.typography.titleLarge,
                color = PulseColors.TextPrimary
            )
            Spacer(Modifier.height(PulseSpacing.sm))
            XpProgressBar(xp = clone.xp, xpPerLevel = CloneStateManager.XP_PER_LEVEL)
            Spacer(Modifier.height(PulseSpacing.xs))
            Text(
                text = "${clone.xp} / ${CloneStateManager.XP_PER_LEVEL} XP",
                style = MaterialTheme.typography.labelMedium,
                color = PulseColors.TextSecondary
            )
            Spacer(Modifier.height(PulseSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Strength", value = "%.1f".format(clone.strength))
                StatItem(label = "Form", value = "${clone.formMastery.toInt()}", accent = PulseColors.Green)
                StatItem(label = "Day Streak", value = "${weekly.streakDays}", accent = PulseColors.Orange)
            }
        }

        Spacer(Modifier.height(PulseSpacing.xl))

        SectionHeader(title = "Weekly Activity")
        Spacer(Modifier.height(PulseSpacing.sm))
        PulseCard(contentPadding = PulseSpacing.md) {
            WeekStrip(daysTrained = weekly.daysTrained)
            Spacer(Modifier.height(PulseSpacing.sm))
            Text(
                text = "${weekly.weekSessionCount} sets this week",
                style = MaterialTheme.typography.bodySmall,
                color = PulseColors.TextSecondary
            )
        }

        Spacer(Modifier.height(PulseSpacing.xl))

        SectionHeader(title = "Recent Workouts")
        Spacer(Modifier.height(PulseSpacing.sm))
        if (sessions.isEmpty()) {
            Text(
                text = "No workouts yet. Finish your first set to see it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseColors.TextSecondary
            )
        }
        for (session in sessions.take(10)) {
            PulseCard(contentPadding = PulseSpacing.md) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        val title = Exercise.fromId(session.exercise)?.let {
                            ExerciseVisuals.forExercise(it).title
                        } ?: session.exercise
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = PulseColors.TextPrimary
                        )
                        Text(
                            text = "${session.reps} reps · ${session.formScore}% form",
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseColors.TextSecondary
                        )
                    }
                    Text(
                        text = "+${session.xpEarned()} XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = PulseColors.Accent
                    )
                }
            }
            Spacer(Modifier.height(PulseSpacing.sm))
        }

        Spacer(Modifier.height(PulseSpacing.xxl))
        Spacer(Modifier.height(72.dp))
    }
}

/**
 * XP estimate for a stored session using the same formula as the
 * progression engine (no comeback multiplier at read time).
 */
private fun com.pulse.data.SessionEntity.xpEarned(): Int =
    reps * formScore
