package com.pulse.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pulse.app.ui.theme.PulseColors
import com.pulse.app.ui.theme.PulseShape
import com.pulse.app.ui.theme.PulseSizes
import com.pulse.app.ui.theme.PulseSpacing

// ---------------------------------------------------------------------------
// Haptics. Light taps only, driven by a platform View, never per frame.
// ---------------------------------------------------------------------------

enum class PulseHaptic { LIGHT, MEDIUM, STRONG }

@Composable
fun rememberHaptics(): (PulseHaptic) -> Unit {
    val view = LocalView.current
    return { strength ->
        val constant = when (strength) {
            PulseHaptic.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
            PulseHaptic.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
            PulseHaptic.STRONG -> HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }
}

// ---------------------------------------------------------------------------
// Press scale + click in one modifier. Scale slightly down, return on release.
// ---------------------------------------------------------------------------

@Composable
fun Modifier.pulseClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    haptic: PulseHaptic? = PulseHaptic.LIGHT,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = rememberHaptics()
    androidx.compose.runtime.LaunchedEffect(pressed) {
        if (pressed && haptic != null) haptics(haptic)
    }
    return this
        .graphicsLayer {
            val scale = if (pressed) pressedScale else 1f
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

@Composable
fun PulseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    contentPadding: Dp = PulseSpacing.lg,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = PulseShape.button
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PulseSizes.Button)
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(PulseColors.Accent)
                } else {
                    Modifier
                        .background(PulseColors.Surface)
                        .border(1.dp, PulseColors.Border, shape)
                }
            )
            .pulseClickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            if (leading != null) Spacer(Modifier.width(PulseSpacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    if (filled) Color.White else PulseColors.TextPrimary
                } else {
                    PulseColors.TextMuted
                }
            )
            if (trailing != null) {
                Spacer(Modifier.width(PulseSpacing.sm))
                trailing.invoke()
            }
        }
    }
}

/** Small pill button (filters, secondary actions). */
@Composable
fun PulseChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(PulseShape.chip)
            .then(
                if (selected) {
                    Modifier.background(PulseColors.Accent)
                } else {
                    Modifier
                        .background(PulseColors.Surface)
                        .border(1.dp, PulseColors.Border, PulseShape.chip)
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pulseClickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else PulseColors.TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = PulseSpacing.lg,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = PulseShape.card
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(PulseColors.Surface)
        .border(1.dp, PulseColors.Border, shape)
    val final = if (onClick != null) {
        base.pulseClickable { onClick() }
    } else {
        base
    }
    Column(final.padding(contentPadding), content = content)
}

/** Section header with optional trailing action text. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailing: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = PulseColors.TextMuted
        )
        if (trailing != null && onTrailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = PulseColors.Accent,
                modifier = Modifier.pulseClickable(haptic = null) { onTrailing() }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// XP bar. Animates smoothly between values.
// ---------------------------------------------------------------------------

@Composable
fun XpProgressBar(
    xp: Int,
    xpPerLevel: Int,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    animate: Boolean = true
) {
    val fraction = (xp.toFloat() / xpPerLevel.toFloat()).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 900),
        label = "xpFraction"
    )
    val shown = if (animate) animated else fraction
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PulseShape.chip)
            .background(PulseColors.SurfaceHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(shown)
                .height(height)
                .clip(PulseShape.chip)
                .background(PulseColors.Accent)
        )
    }
}

/** Big animated number, used for reps and XP counters. */
@Composable
fun AnimatedCountText(
    value: Int,
    style: TextStyle,
    color: Color = PulseColors.TextPrimary,
    modifier: Modifier = Modifier
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 450),
        label = "count"
    )
    Text(
        text = "$animated",
        style = style,
        color = color,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

// ---------------------------------------------------------------------------
// Level badge + stat item
// ---------------------------------------------------------------------------

@Composable
fun LevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickable = if (onClick != null) {
        Modifier.pulseClickable(haptic = PulseHaptic.LIGHT) { onClick() }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(clickable)
            .clip(PulseShape.chip)
            .background(PulseColors.Accent)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Lv. $level",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = PulseColors.TextPrimary
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accent
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PulseColors.TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Week strip: M T W T F S S with trained days highlighted.
// ---------------------------------------------------------------------------

@Composable
fun WeekStrip(
    daysTrained: List<Boolean>,
    modifier: Modifier = Modifier
) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..6) {
            val trained = daysTrained.getOrElse(i) { false }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = labels[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseColors.TextMuted
                )
                Spacer(Modifier.height(PulseSpacing.xs))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (trained) PulseColors.Accent else PulseColors.SurfaceHigh)
                        .border(
                            1.dp,
                            if (trained) PulseColors.Accent else PulseColors.Border,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (trained) {
                        Text(
                            "✓",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/** Small circular pulse that plays whenever [trigger] increases. */
@Composable
fun RepPulse(trigger: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var lastTrigger by remember { mutableIntStateOf(trigger) }
    var pulseKey by remember { mutableIntStateOf(0) }
    if (trigger > lastTrigger) {
        lastTrigger = trigger
        pulseKey++
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (pulseKey > 0) {
            key(pulseKey) {
                val progress = remember(pulseKey) {
                    androidx.compose.animation.core.Animatable(0f)
                }
                LaunchedEffect(pulseKey) {
                    progress.animateTo(
                        1f,
                        animationSpec = tween(durationMillis = 550, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    )
                }
                val p = progress.value
                if (p < 1f) {
                    Box(
                        Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                scaleX = 1f + p * 0.45f
                                scaleY = 1f + p * 0.45f
                                alpha = (1f - p) * 0.5f
                            }
                            .clip(CircleShape)
                            .border(3.dp, PulseColors.Accent, CircleShape)
                    )
                }
            }
        }
        content()
    }
}
