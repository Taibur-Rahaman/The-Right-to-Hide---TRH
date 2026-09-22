package com.rth.kycliveness.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    onPrimary = Color.White,
    secondary = Color(0xFF1B9AAA),
    onSecondary = Color.White,
    background = Color(0xFFF4F7F5),
    onBackground = Color(0xFF102A23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF102A23),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5CDB95),
    onPrimary = Color(0xFF003822),
    secondary = Color(0xFF7FDBDA),
    background = Color(0xFF0D1B16),
    onBackground = Color(0xFFE7F2EC),
    surface = Color(0xFF13241D),
    onSurface = Color(0xFFE7F2EC),
)

@Composable
fun KycSandboxTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
