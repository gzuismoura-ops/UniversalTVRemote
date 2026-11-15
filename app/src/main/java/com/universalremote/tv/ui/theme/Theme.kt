package com.universalremote.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = PrimaryPurple, onPrimary = Color(0xFF241333), secondary = SecondaryTeal,
    onSecondary = Color(0xFF00201D), background = Background, onBackground = TextPrimary,
    surface = SurfaceDark, onSurface = TextPrimary, error = ErrorPink, onError = Color.Black
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF6840A4), secondary = Color(0xFF006B62), background = Color(0xFFF8F5FC),
    surface = Color(0xFFFFFFFF), error = Color(0xFFB3261E)
)

@Composable
fun UniversalTVTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = AppTypography, content = content)
}
