package com.familiachat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF00897B)
val TealDark = Color(0xFF4DB6AC)
val Coral = Color(0xFFFF7A59)
val ChatBackgroundLight = Color(0xFFF3F1EC)
val ChatBackgroundDark = Color(0xFF12201E)
val BubbleMineLight = Teal
val BubbleTheirsLight = Color(0xFFFFFFFF)
val BubbleMineDark = Color(0xFF0B5B52)
val BubbleTheirsDark = Color(0xFF1E2E2C)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    secondary = Coral,
    background = ChatBackgroundLight,
    surface = Color.White,
    surfaceVariant = BubbleTheirsLight,
    onSurfaceVariant = Color(0xFF2B2B2B),
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Color(0xFF00332E),
    primaryContainer = Color(0xFF00504A),
    secondary = Coral,
    background = ChatBackgroundDark,
    surface = Color(0xFF16211F),
    surfaceVariant = BubbleTheirsDark,
    onSurfaceVariant = Color(0xFFE6E6E6),
)

@Composable
fun FamiliaChatTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
