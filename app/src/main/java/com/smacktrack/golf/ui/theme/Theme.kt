package com.smacktrack.golf.ui.theme

/**
 * Material 3 light theme configuration for the app.
 *
 * Uses a nature-inspired green palette with OffWhite backgrounds,
 * designed for outdoor readability in bright sunlight.
 */

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = Color.White,
    primaryContainer = LightGreenTint,
    onPrimaryContainer = DarkGreen,
    secondary = DarkGreenLight,
    onSecondary = Color.White,
    secondaryContainer = LightGreenTint,
    onSecondaryContainer = DarkGreen,
    surface = White,
    onSurface = TextPrimary,
    surfaceVariant = LightGray,
    onSurfaceVariant = TextSecondary,
    background = OffWhite,
    onBackground = TextPrimary,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Color(0xFF410002),
    outline = TextTertiary,
    outlineVariant = MidGray
)

@Composable
fun SmackTrackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content
    )
}
