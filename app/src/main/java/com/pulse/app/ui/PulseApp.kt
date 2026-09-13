package com.pulse.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pulse.app.ArenaLauncher
import com.pulse.app.ui.components.PulseHaptic
import com.pulse.app.ui.components.pulseClickable
import com.pulse.app.ui.components.rememberHaptics
import com.pulse.app.ui.screens.ActiveWorkoutScreen
import com.pulse.app.ui.screens.CompleteScreen
import com.pulse.app.ui.screens.DetailScreen
import com.pulse.app.ui.screens.HomeScreen
import com.pulse.app.ui.screens.ProgressScreen
import com.pulse.app.ui.screens.WorkoutsScreen
import com.pulse.app.ui.screens.PulseSplash
import com.pulse.app.ui.theme.PulseColors
import com.pulse.app.ui.theme.PulseShape
import com.pulse.app.ui.theme.PulseSizes
import com.pulse.app.ui.theme.PulseSpacing
import com.pulse.camera.AnalyzerStats
import com.pulse.data.LevelUpInfo
import com.pulse.movement.Exercise
import com.pulse.movement.SetResult

/** Navigation destinations. */
sealed interface Dest {
    data object Home : Dest
    data object Workouts : Dest
    data object Progress : Dest
    data class Detail(val exercise: Exercise) : Dest
    data class Active(val exercise: Exercise) : Dest
    data object Complete : Dest
}

/** What one finished set earned, passed to the complete screen. */
data class CompletedSet(
    val result: SetResult,
    val stats: AnalyzerStats,
    val info: LevelUpInfo,
    val xpBefore: Int
)

@Composable
fun PulseApp(viewModel: PulseViewModel) {
    val context = LocalContext.current
    val haptics = rememberHaptics()

    // Simple nav state. Back handling: active > detail > tabs.
    var backStack by remember {
        mutableStateOf(listOf<Dest>(Dest.Home))
    }
    var completed by remember { mutableStateOf<CompletedSet?>(null) }
    var arenaMessage by remember { mutableStateOf<String?>(null) }
    var showSplash by remember { mutableStateOf(true) }

    val current = backStack.last()
    val showNav = current is Dest.Home || current is Dest.Workouts || current is Dest.Progress

    fun push(dest: Dest) {
        backStack = backStack + dest
    }

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        backStack = backStack.dropLast(1)
        return true
    }

    // System back pops our stack instead of leaving the app.
    BackHandler(enabled = backStack.size > 1) { pop() }

    Box(
        Modifier
            .fillMaxSize()
            .background(PulseColors.Background)
    ) {
        if (showSplash) {
            PulseSplash(onDone = { showSplash = false })
        } else AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) +
                    slideInVertically(animationSpec = tween(220)) { it / 24 })
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "screens",
            contentKey = { dest -> dest.keyId() }
        ) { dest ->
            when (dest) {
                is Dest.Home -> HomeScreen(
                    viewModel = viewModel,
                    arenaMessage = arenaMessage,
                    onPlayArena = {
                        haptics(PulseHaptic.LIGHT)
                        // Demo wiring: replace player_1 with a real identity later.
                        val clone = viewModel.clone.value
                        val snapshot = ArenaLauncher.createPlayerSnapshot(
                            playerId = "player_1",
                            cloneLevel = clone.level,
                            strength = clone.strength,
                            formMastery = clone.formMastery,
                            consistency = clone.consistency,
                            recovery = clone.recovery,
                            gameXp = clone.xp
                        )
                        ArenaLauncher.pendingResultCallback = { json ->
                            if (json != null) {
                                viewModel.applyFightResult(json) { reward ->
                                    arenaMessage = when {
                                        reward == null ->
                                            "Arena closed with no result. No XP given."
                                        reward.xpGained >= com.pulse.data.FightRewards.WIN_XP ->
                                            "Arena won! +${reward.xpGained} XP."
                                        reward.xpGained > 0 ->
                                            "Arena done. +${reward.xpGained} XP."
                                        else -> "Arena closed. No XP given."
                                    }
                                }
                            } else {
                                arenaMessage =
                                    "Back from arena. Fight result not returned by this build."
                            }
                        }
                        arenaMessage = when (ArenaLauncher.launch(context, snapshot)) {
                            ArenaLauncher.Result.LAUNCHED -> "Arena launched. Return here after the fight."
                            ArenaLauncher.Result.EMBEDDED_LAUNCHED -> null
                            ArenaLauncher.Result.NOT_INSTALLED ->
                                "Install the Pulse Arena game first, then try again."
                        }
                    },
                    onPickExercise = { push(Dest.Detail(it)) },
                    onOpenWorkouts = { push(Dest.Workouts) },
                    onOpenProgress = { push(Dest.Progress) }
                )

                is Dest.Workouts -> WorkoutsScreen(
                    onBack = { pop() },
                    onPickExercise = { push(Dest.Detail(it)) }
                )

                is Dest.Progress -> ProgressScreen(viewModel = viewModel)

                is Dest.Detail -> DetailScreen(
                    exercise = dest.exercise,
                    onBack = { pop() },
                    onStart = { push(Dest.Active(dest.exercise)) }
                )

                is Dest.Active -> ActiveWorkoutScreen(
                    exercise = dest.exercise,
                    onExit = { pop() },
                    onFinished = { result, stats ->
                        if (result == null) {
                            // No result (invalid set): return without rewards.
                            pop()
                        } else {
                            val xpBefore = viewModel.clone.value.xp
                            viewModel.saveResult(result) { info ->
                                completed = CompletedSet(result, stats, info, xpBefore)
                                push(Dest.Complete)
                            }
                        }
                    }
                )

                is Dest.Complete -> CompleteScreen(
                    completed = completed,
                    onTrainAgain = {
                        completed?.let { c ->
                            backStack = backStack.dropLast(1) // pop Complete
                            push(Dest.Active(c.result.exercise.let { Exercise.fromId(it) ?: Exercise.SQUAT }))
                        }
                    },
                    onHome = {
                        completed = null
                        backStack = listOf(Dest.Home)
                    }
                )
            }
        }

        if (showNav) {
            BottomNav(
                current = current,
                onSelect = { dest ->
                    haptics(PulseHaptic.LIGHT)
                    if (dest != current) {
                        backStack = listOf(Dest.Home, dest)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = PulseSpacing.lg, vertical = PulseSpacing.md)
            )
        }
    }
}

private fun Dest.keyId(): String = when (this) {
    is Dest.Home -> "home"
    is Dest.Workouts -> "workouts"
    is Dest.Progress -> "progress"
    is Dest.Detail -> "detail-${exercise.id}"
    is Dest.Active -> "active-${exercise.id}"
    is Dest.Complete -> "complete"
}

@Composable
private fun BottomNav(
    current: Dest,
    onSelect: (Dest) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("Home", Dest.Home, Icons.Filled.Home),
        Triple("Workouts", Dest.Workouts, Icons.Filled.FitnessCenter),
        Triple("Progress", Dest.Progress, Icons.Filled.Insights)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PulseSizes.NavHeight)
            .clip(PulseShape.card)
            .background(PulseColors.Surface)
            .border(1.dp, PulseColors.Border, PulseShape.card)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for ((label, dest, icon) in items) {
                val selected = current::class == dest::class
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pulseClickable(haptic = null) { onSelect(dest) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (selected) PulseColors.Accent else PulseColors.TextMuted
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) PulseColors.Accent else PulseColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}
