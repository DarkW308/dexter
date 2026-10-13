package com.aistudio.dexter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DexterPrimary,
    onPrimary = DexterOnPrimary,
    primaryContainer = DexterSurfaceVariant,
    onPrimaryContainer = DexterPrimaryGlow,
    secondary = DexterPrimaryGlow,
    onSecondary = DexterOnPrimary,
    background = DexterBackground,
    onBackground = DexterOnBackground,
    surface = DexterSurface,
    onSurface = DexterOnSurface,
    surfaceVariant = DexterSurfaceVariant,
    onSurfaceVariant = DexterOnSurfaceVariant,
    error = DexterError,
    onError = DexterOnPrimary
)

@Composable
fun DexterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dexter is strictly a Dark Theme personal OS as requested in DEXTER_APP_UIUX.md
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
