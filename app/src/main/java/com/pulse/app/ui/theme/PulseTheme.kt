package com.pulse.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Color. Warm light base, orange reserved for primary action / XP / level.
// ---------------------------------------------------------------------------

object PulseColors {
    val Background = Color(0xFFFFF9F2) // warm off-white
    val Surface = Color(0xFFFFFFFF)
    val SurfaceHigh = Color(0xFFFFF1E4) // warm tinted fill
    val Border = Color(0xFFF0E4D8)
    val Divider = Color(0xFFF5EBE0)

    val Accent = Color(0xFFFF6B35) // warm orange - primary action, XP, level
    val AccentDim = Color(0xFFE05520)
    val AccentDeep = Color(0xFFFFE8DC)
    val Blue = Color(0xFF5B8DEF) // secondary, subtle
    val Green = Color(0xFF3FA37C) // success
    val Orange = Color(0xFFFF8C42) // streak / warning
    val Red = Color(0xFFE05252)
    val Purple = Color(0xFF8B5CF6) // clone accents

    val TextPrimary = Color(0xFF1F1A17) // very dark warm
    val TextSecondary = Color(0xFF6B625C)
    val TextMuted = Color(0xFFA89E96)
}

// ---------------------------------------------------------------------------
// Type scale. System default font for zero added dependencies.
// ---------------------------------------------------------------------------

private val PulseTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
)

// ---------------------------------------------------------------------------
// Metrics. One place for radius, spacing and sizes so screens stay consistent.
// ---------------------------------------------------------------------------

object PulseRadius {
    val Card = 20.dp
    val Button = 16.dp
    val Small = 12.dp
}

object PulseSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object PulseSizes {
    val Button = 52.dp
    val ButtonSmall = 40.dp
    val NavHeight = 64.dp
    val Icon = 22.dp
}

object PulseShape {
    val card = RoundedCornerShape(PulseRadius.Card)
    val button = RoundedCornerShape(PulseRadius.Button)
    val small = RoundedCornerShape(PulseRadius.Small)
    val chip = RoundedCornerShape(50)
}

private val PulseLightScheme = lightColorScheme(
    primary = PulseColors.Accent,
    onPrimary = Color.White,
    primaryContainer = PulseColors.AccentDeep,
    onPrimaryContainer = PulseColors.TextPrimary,
    secondary = PulseColors.Blue,
    onSecondary = Color.White,
    background = PulseColors.Background,
    onBackground = PulseColors.TextPrimary,
    surface = PulseColors.Surface,
    onSurface = PulseColors.TextPrimary,
    surfaceVariant = PulseColors.SurfaceHigh,
    onSurfaceVariant = PulseColors.TextSecondary,
    outline = PulseColors.Border,
    error = PulseColors.Red
)

@Composable
fun PulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PulseLightScheme,
        typography = PulseTypography,
        content = content
    )
}
