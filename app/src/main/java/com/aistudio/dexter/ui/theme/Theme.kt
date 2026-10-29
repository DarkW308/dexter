package com.aistudio.dexter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Apple Human Interface Guidelines Dark System Color Scheme
private val AppleDarkColorScheme = darkColorScheme(
    primary = AppleAccentBlue,
    onPrimary = Color.White,
    primaryContainer = AppleElevatedSurface,
    onPrimaryContainer = AppleAccentCyan,
    secondary = AppleAccentIndigo,
    onSecondary = Color.White,
    tertiary = AppleAccentCyan,
    background = AppleCanvas,
    onBackground = AppleLabelPrimary,
    surface = AppleCardBackground,
    onSurface = AppleLabelPrimary,
    surfaceVariant = AppleElevatedSurface,
    onSurfaceVariant = AppleLabelSecondary,
    outline = AppleHairline,
    outlineVariant = AppleHairlineLight,
    error = AppleError,
    onError = Color.White
)

// Apple Squircle & Continuous Corner Radius Shapes
val AppleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DexterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppleDarkColorScheme,
        typography = AppleTypography,
        shapes = AppleShapes,
        content = content
    )
}
