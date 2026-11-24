package io.github.murzilinsin.dmhelper.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val battleColors = darkColorScheme(
    primary = Color(0xFFF2C98D),
    onPrimary = Color(0xFF342408),
    primaryContainer = Color(0xFF483821),
    onPrimaryContainer = Color(0xFFFFE0AB),
    background = Color(0xFF171613),
    onBackground = Color(0xFFF8F1E5),
    surface = Color(0xFF171613),
    onSurface = Color(0xFFF8F1E5),
    surfaceVariant = Color(0xFF27241F),
    onSurfaceVariant = Color(0xFFD0C5B5),
    surfaceContainer = Color(0xFF25221D),
    surfaceContainerHigh = Color(0xFF2E2A23),
    outline = Color(0xFF978A76),
    outlineVariant = Color(0xFF514838),
    error = Color(0xFFFFB4AB),
)

@Composable
internal fun DmHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = battleColors, content = content)
}
