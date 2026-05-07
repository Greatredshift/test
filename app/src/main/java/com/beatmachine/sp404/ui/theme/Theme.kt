package com.beatmachine.sp404.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpRed,
    onPrimary = SpText,
    primaryContainer = SpSurface,
    onPrimaryContainer = SpText,
    secondary = SpOrange,
    onSecondary = SpBlack,
    secondaryContainer = SpDarkSurface,
    onSecondaryContainer = SpText,
    tertiary = SpCyan,
    onTertiary = SpBlack,
    background = SpBlack,
    onBackground = SpText,
    surface = SpDarkSurface,
    onSurface = SpText,
    surfaceVariant = SpSurface,
    onSurfaceVariant = SpTextDim,
    outline = SpSurfaceLight,
    outlineVariant = SpSurface,
    error = SpRed,
    onError = SpText,
    inverseSurface = SpText,
    inverseOnSurface = SpBlack
)

@Composable
fun BeatMachineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
