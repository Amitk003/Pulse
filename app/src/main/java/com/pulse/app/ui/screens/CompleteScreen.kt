package com.pulse.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulse.app.ui.ExerciseVisuals
import com.pulse.app.ui.CompletedSet
import com.pulse.app.ui.components.PulseButton
import com.pulse.app.ui.components.PulseCard
import com.pulse.app.ui.components.XpProgressBar
import com.pulse.app.ui.components.rememberHaptics
import com.pulse.app.ui.components.PulseHaptic
import com.pulse.app.ui.theme.PulseColors
import com.pulse.app.ui.theme.PulseShape
import com.pulse.app.ui.theme.PulseSpacing
import com.pulse.data.CloneStateManager
import com.pulse.movement.Exercise
import kotlinx.coroutines.delay

/**
 * Reward moment. Shows the finished set, animates XP from the old bar
 * to the new one, and celebrates a level up when one happened.
 */
@Composable
fun CompleteScreen(
    completed: CompletedSet?,
    onTrainAgain: () -> Unit,
    onHome: () -> Unit
) {
    val haptics = rememberHaptics()
    var xpShown by remember { mutableIntStateOf(completed?.xpBefore ?: 0) }
    var reveal by remember { mutableStateOf(false) }
    var celebrated by remember { mutableStateOf(false) }

    val info = completed?.info
    val xpPerLevel = CloneStateManager.XP_PER_LEVEL

    // Animate the count after a short pause, then celebrate once.
    LaunchedEffect(completed) {
        if (completed == null) return@LaunchedEffect
        delay(500)
        xpShown = completed.info.xpGained // drives the count-up label
        reveal = true
        if (completed.info.leveledUp) {
            delay(900)
            if (!celebrated) {
                celebrated = true
                haptics(PulseHaptic.STRONG)
            }
        }
    }

    val xpBefore = completed?.xpBefore ?: 0
    val xpAfter = xpBefore + (info?.xpGained ?: 0)
    val beforeInLevel = xpBefore % xpPerLevel
    val afterInLevel = xpAfter % xpPerLevel
    val barXp = animateIntAsBar(
        from = beforeInLevel,
        to = if (reveal) afterInLevel else beforeInLevel
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PulseSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PulseSpacing.xxl))

        if (completed == null) {
            Text(
                text = "Set saved.",
                style = MaterialTheme.typography.headlineMedium,
                color = PulseColors.TextPrimary
            )
        } else {
            val info = completed.info
            Text(
                text = "SET COMPLETE",
                style = MaterialTheme.typography.labelLarge,
                color = PulseColors.TextSecondary
            )
            Spacer(Modifier.height(PulseSpacing.md))
            Text(
                text = "${completed.result.reps}",
                style = MaterialTheme.typography.displayLarge,
                color = PulseColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "REPS",
                style = MaterialTheme.typography.labelMedium,
                color = PulseColors.TextSecondary
            )
            Spacer(Modifier.height(PulseSpacing.sm))
            Text(
                text = ExerciseVisuals.forExercise(
                    Exercise.fromId(completed.result.exercise) ?: Exercise.SQUAT
                ).title,
                style = MaterialTheme.typography.titleMedium,
                color = PulseColors.TextSecondary
            )

            Spacer(Modifier.height(PulseSpacing.xl))

            // XP reward card with animated bar.
            PulseCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PulseColors.AccentDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = PulseColors.Accent
                        )
                    }
                    Spacer(Modifier.width(PulseSpacing.md))
                    Text(
                        text = "+${info.xpGained} XP",
                        style = MaterialTheme.typography.titleLarge,
                        color = PulseColors.Accent
                    )
                }
                Spacer(Modifier.height(PulseSpacing.md))
                XpProgressBar(xp = barXp, xpPerLevel = xpPerLevel)
                Spacer(Modifier.height(PulseSpacing.xs))
                Text(
                    text = "$afterInLevel / $xpPerLevel XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = PulseColors.TextSecondary
                )
            }

            if (info.leveledUp) {
                Spacer(Modifier.height(PulseSpacing.lg))
                LevelUpCard(newLevel = info.newLevel)
            }

            if (info.comebackApplied) {
                Spacer(Modifier.height(PulseSpacing.sm))
                Text(
                    text = "Comeback bonus x1.75 applied",
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseColors.Green
                )
            }
            if (info.decayed) {
                Spacer(Modifier.height(PulseSpacing.sm))
                Text(
                    text = "Clone rested long, strength dipped first",
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseColors.Orange
                )
            }

            Spacer(Modifier.height(PulseSpacing.md))
            if (completed.result.mistakes.isEmpty()) {
                Text(
                    text = "Great form today!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseColors.Green
                )
            } else {
                Text(
                    text = "Watch out for:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseColors.TextSecondary
                )
                for (mistake in completed.result.mistakes) {
                    Text(
                        text = "· $mistake",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseColors.TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(PulseSpacing.xxl))

        PulseButton(text = "Train Again", onClick = onTrainAgain)
        Spacer(Modifier.height(PulseSpacing.md))
        PulseButton(text = "Back Home", onClick = onHome, filled = false)

        Spacer(Modifier.height(PulseSpacing.xxl))
        Spacer(Modifier.height(48.dp))
    }
}

/** Simple integer tween between two values for the XP bar. */
@Composable
private fun animateIntAsBar(from: Int, to: Int): Int {
    val target by animateFloatAsState(
        targetValue = to.toFloat(),
        animationSpec = tween(durationMillis = 900),
        label = "barXp"
    )
    return target.toInt().coerceAtLeast(from)
}

@Composable
private fun LevelUpCard(newLevel: Int) {
    var scale by remember { mutableStateOf(0.9f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 500),
        label = "levelScale"
    )
    LaunchedEffect(Unit) { scale = 1f }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(PulseShape.card)
            .background(
                Brush.verticalGradient(
                    listOf(PulseColors.Accent.copy(alpha = 0.35f), PulseColors.Surface)
                )
            )
            .padding(PulseSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LEVEL UP",
            style = MaterialTheme.typography.labelLarge,
            color = PulseColors.Accent
        )
        Spacer(Modifier.height(PulseSpacing.xs))
        Text(
            text = "Clone Level $newLevel",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PulseColors.TextPrimary
        )
        Spacer(Modifier.height(PulseSpacing.xs))
        Text(
            text = "Your clone grows stronger",
            style = MaterialTheme.typography.bodySmall,
            color = PulseColors.TextSecondary
        )
    }
}
