package com.pulse.app.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.app.ui.ExerciseVisuals
import com.pulse.app.ui.PulseViewModel
import com.pulse.app.ui.components.LevelBadge
import com.pulse.app.ui.components.PulseButton
import com.pulse.app.ui.components.PulseCard
import com.pulse.app.ui.components.SectionHeader
import com.pulse.app.ui.components.StatItem
import com.pulse.app.ui.components.WeekStrip
import com.pulse.app.ui.components.XpProgressBar
import com.pulse.app.ui.components.pulseClickable
import com.pulse.app.ui.theme.PulseColors
import com.pulse.app.ui.theme.PulseShape
import com.pulse.app.ui.theme.PulseSpacing
import com.pulse.data.CloneStateManager
import com.pulse.movement.Exercise

@Composable
fun HomeScreen(
    viewModel: PulseViewModel,
    arenaMessage: String?,
    onPlayArena: () -> Unit,
    onPickExercise: (Exercise) -> Unit,
    onOpenWorkouts: () -> Unit,
    onOpenProgress: () -> Unit
) {
    val clone by viewModel.clone.collectAsStateWithLifecycle()
    val weekly by viewModel.weekly.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PulseSpacing.lg)
    ) {
        Spacer(Modifier.height(PulseSpacing.md))

        // Header: animated heartbeat + PULSE wordmark, avatar + level right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeartbeatMini()
                Spacer(Modifier.width(PulseSpacing.sm))
                Text(
                    text = "PULSE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = PulseColors.TextPrimary
                )
            }
            LevelBadge(
                level = clone.level,
                onClick = onOpenProgress
            )
        }

        Spacer(Modifier.height(PulseSpacing.xl))

        // Bold motto, nothing else.
        Text(
            text = "TRAIN. IMPROVE.\nLEVEL UP.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = PulseColors.TextPrimary
        )

        Spacer(Modifier.height(PulseSpacing.lg))

        // Primary CTA.
        PulseButton(
            text = "Play Arena",
            onClick = onPlayArena,
            leading = {
                Icon(
                    Icons.Filled.SportsEsports,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White
                )
            },
            trailing = {
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        )
        if (arenaMessage != null) {
            Spacer(Modifier.height(PulseSpacing.sm))
            Text(
                text = arenaMessage,
                style = MaterialTheme.typography.bodySmall,
                color = PulseColors.Orange
            )
        }

        Spacer(Modifier.height(PulseSpacing.xl))

        // Clone card.
        SectionHeader(title = "Your Clone", trailing = "Progress", onTrailing = onOpenProgress)
        Spacer(Modifier.height(PulseSpacing.sm))
        PulseCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Clone Level ${clone.level}",
                        style = MaterialTheme.typography.titleMedium,
                        color = PulseColors.TextPrimary
                    )
                    Spacer(Modifier.height(PulseSpacing.xs))
                    Text(
                        text = "${clone.xp} / ${CloneStateManager.XP_PER_LEVEL} XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = PulseColors.TextSecondary
                    )
                }
                if (weekly.streakDays > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = "streak",
                            tint = PulseColors.Orange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${weekly.streakDays} day streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = PulseColors.Orange
                        )
                    }
                }
            }
            Spacer(Modifier.height(PulseSpacing.md))
            XpProgressBar(xp = clone.xp, xpPerLevel = CloneStateManager.XP_PER_LEVEL)
            Spacer(Modifier.height(PulseSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Strength", value = "%.1f".format(clone.strength))
                StatItem(label = "Form", value = "${clone.formMastery.toInt()}", accent = PulseColors.Green)
                StatItem(label = "Recovery", value = "${(clone.recovery * 100).toInt()}%", accent = PulseColors.Blue)
            }
        }

        Spacer(Modifier.height(PulseSpacing.xl))

        // Week strip.
        SectionHeader(title = "Today's Training")
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

        // Workouts.
        SectionHeader(title = "Choose Your Workout", trailing = "See all", onTrailing = onOpenWorkouts)
        Spacer(Modifier.height(PulseSpacing.sm))
        for (visual in ExerciseVisuals.all) {
            WorkoutRow(
                title = visual.title,
                subtitle = visual.category,
                accent = visual.accent,
                icon = visual.icon,
                onClick = { onPickExercise(visual.exercise) }
            )
            Spacer(Modifier.height(PulseSpacing.sm))
        }

        Spacer(Modifier.height(PulseSpacing.xxl)) // breathing room above bottom nav
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun WorkoutRow(
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    PulseCard(onClick = onClick, contentPadding = PulseSpacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(PulseShape.small)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent)
            }
            Spacer(Modifier.width(PulseSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PulseColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseColors.TextSecondary
                )
            }
            Text("→", color = PulseColors.TextMuted, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Tiny looping heartbeat line beside the wordmark. */
@Composable
private fun HeartbeatMini() {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "hb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(1400, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "hbPhase"
    )
    val accent = PulseColors.Accent
    Canvas(
        modifier = Modifier
            .size(width = 34.dp, height = 22.dp)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val path = Path().apply {
            moveTo(0f, midY)
            lineTo(w * 0.18f, midY)
            lineTo(w * 0.30f, midY - h * 0.30f)
            lineTo(w * 0.44f, midY + h * 0.42f)
            lineTo(w * 0.58f, midY - h * 0.55f)
            lineTo(w * 0.70f, midY + h * 0.25f)
            lineTo(w * 0.80f, midY)
            lineTo(w, midY)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        // Draw-heartbeat then rest, looping: 70% of the cycle draws the line.
        val progress = (phase / 0.7f).coerceIn(0f, 1f)
        measure.getSegment(0f, measure.length * progress, partial, true)
        drawPath(
            path = partial,
            color = accent,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

