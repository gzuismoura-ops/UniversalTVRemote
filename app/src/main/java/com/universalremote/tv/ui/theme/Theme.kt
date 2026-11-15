package com.universalremote.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color(0xFF17132D),
    secondary = SecondaryTeal,
    onSecondary = Color(0xFF00201D),
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF222C45),
    onSurfaceVariant = TextSecondary,
    primaryContainer = Color(0xFF302B58),
    onPrimaryContainer = Color(0xFFE7E2FF),
    error = ErrorPink,
    onError = Color(0xFF2B0710),
    errorContainer = Color(0xFF4A1D2A),
    onErrorContainer = Color(0xFFFFD9E0),
    outline = Color(0xFF64708F)
)

@Composable
fun UniversalTVTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = AppTypography, content = content)
}
