package com.mohammed.xvrviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7EC4FF),
    secondary = Color(0xFF4CD3A5),
    background = Color(0xFF0E0E12),
    surface = Color(0xFF17171C)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0066CC),
    secondary = Color(0xFF00A87A),
    background = Color(0xFFF7F7FA),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun XvrViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
