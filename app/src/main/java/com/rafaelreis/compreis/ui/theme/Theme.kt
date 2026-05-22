package com.rafaelreis.compreis.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF34C759)
val GreenDark = Color(0xFF248A3D)
val GreenContainer = Color(0xFFDCF5E4)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenDark,
    secondary = Color(0xFF8E8E93),
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F2F7)
)

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = Color.Black,
    primaryContainer = GreenDark,
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF2C2C2E),
    surfaceVariant = Color(0xFF3A3A3C)
)

@Composable
fun CompreisTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
