package com.example.huellitasya.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Esquema fijo de marca: sin dynamic color, para que MaterialTheme.colorScheme refleje Figma.
private val HuellitasColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenBackground,
    onPrimaryContainer = Color.Black,
    secondary = GreenAccent,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = GrayText,
    outline = BorderSubtle,
    error = ErrorRed,
)

@Composable
fun HuellitasYATheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HuellitasColorScheme,
        typography = Typography,
        content = content,
    )
}
