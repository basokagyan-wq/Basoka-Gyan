package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BasitDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = CyberBlack,
    primaryContainer = SurfaceCard,
    onPrimaryContainer = CyanGlow,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = PurpleGlow,
    tertiary = AmberWarning,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = CrimsonRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BasitDarkColorScheme,
        typography = Typography,
        content = content
    )
}

