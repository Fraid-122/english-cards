package com.fraid.englishcards.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palette of the DREAM SWIM design system, theme "Графіт і бузок" (dark only).
private val Background = Color(0xFF0E0F13)
private val Surface = Color(0xFF15171D)
private val SurfaceRaised = Color(0xFF1C1F27)
private val Border = Color(0xFF262A33)
private val BorderStrong = Color(0xFF60697F)
private val Foreground = Color(0xFFE4E6EB)
private val Muted = Color(0xFF9A9FAB)
private val Accent = Color(0xFFA898F0)
private val Accent2 = Color(0xFF8F7CEB)

/** Text on accent, success, warning and danger fills. */
val AccentForeground = Color(0xFF16112B)

/** success: "know" and learned. */
val KnownGreen = Color(0xFF7FC8A9)

/** danger: "don't know" and errors. */
val UnknownRed = Color(0xFFE58A8A)

/** warning: cards in progress. */
val InProgressAmber = Color(0xFFD9B76E)

private val DreamColors = darkColorScheme(
    primary = Accent,
    onPrimary = AccentForeground,
    primaryContainer = SurfaceRaised,
    onPrimaryContainer = Foreground,
    secondary = Accent2,
    onSecondary = AccentForeground,
    secondaryContainer = Accent,
    onSecondaryContainer = AccentForeground,
    tertiary = KnownGreen,
    onTertiary = AccentForeground,
    background = Background,
    onBackground = Foreground,
    surface = Surface,
    onSurface = Foreground,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Background,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceRaised,
    outline = BorderStrong,
    outlineVariant = Border,
    error = UnknownRed,
    onError = AccentForeground,
)

private val DreamShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun EnglishCardsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DreamColors, shapes = DreamShapes, content = content)
}
