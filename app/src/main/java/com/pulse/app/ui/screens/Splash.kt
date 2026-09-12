package com.pulse.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulse.app.ui.theme.PulseColors
import kotlinx.coroutines.delay

/**
 * Minimal opening: a thin heartbeat line draws itself, gives one glow,
 * then Pulse branding fades in. ~1.5s total, then calls onDone once.
 */
@Composable
fun PulseSplash(onDone: () -> Unit) {
    // 0 -> line drawing progress, 1 -> fully drawn.
    var drawProgress by remember { mutableStateOf(0f) }
    var glowAlpha by remember { mutableStateOf(0f) }
    var textAlpha by remember { mutableStateOf(0f) }
    var visible by remember { mutableStateOf(1f) }

    val drawn by animateFloatAsState(
        targetValue = drawProgress,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "heartDraw"
    )
    val glow by animateFloatAsState(
        targetValue = glowAlpha,
        animationSpec = tween(durationMillis = 450),
        label = "heartGlow"
    )
    val text by animateFloatAsState(
        targetValue = textAlpha,
        animationSpec = tween(durationMillis = 500),
        label = "brandText"
    )
    val screenAlpha by animateFloatAsState(
        targetValue = visible,
        animationSpec = tween(durationMillis = 350),
        label = "splashFade"
    )

    LaunchedEffect(Unit) {
        drawProgress = 1f
        delay(600)
        glowAlpha = 1f
        textAlpha = 1f
        delay(400)
        visible = 0f
        delay(300)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseColors.Background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { this.alpha = screenAlpha }
        ) {
            HeartbeatCanvas(progress = drawn, glow = glow, alpha = screenAlpha)
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Pulse",
                style = MaterialTheme.typography.displayMedium,
                color = PulseColors.TextPrimary.copy(alpha = text),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Train • Improve • Level Up",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseColors.TextSecondary.copy(alpha = text),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HeartbeatCanvas(progress: Float, glow: Float, alpha: Float) {
    val accent = PulseColors.Accent
    val blue = PulseColors.Blue
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Flat - small bump - big spike - dip - flat heartbeat shape.
        val path = Path().apply {
            moveTo(0f, midY)
            lineTo(w * 0.28f, midY)
            lineTo(w * 0.36f, midY - h * 0.12f)
            lineTo(w * 0.44f, midY + h * 0.08f)
            lineTo(w * 0.52f, midY - h * 0.55f)
            lineTo(w * 0.60f, midY + h * 0.35f)
            lineTo(w * 0.66f, midY)
            lineTo(w, midY)
        }

        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), partial, true)

        val stroke = Stroke(width = 5f, cap = StrokeCap.Round)
        // Soft glow pass underneath, stronger during the glow moment.
        if (glow > 0.01f) {
            drawPath(
                path = partial,
                brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.35f * glow), blue.copy(alpha = 0.35f * glow))),
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )
        }
        drawPath(
            path = partial,
            brush = Brush.horizontalGradient(listOf(accent, blue)),
            style = stroke
        )
    }
}
