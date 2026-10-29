package com.aistudio.dexter.ui.theme

import androidx.compose.ui.graphics.Color

// Apple Human Interface Guidelines (HIG) Dark System Palette
val AppleCanvas = Color(0xFF08090B)           // OLED Deep Graphite Background
val AppleCardBackground = Color(0xFF15171E)   // Grouped Inset Card Surface
val AppleElevatedSurface = Color(0xFF1C202B)  // Elevated Dialogs, Sheets, & Top Bars
val AppleGlassSurface = Color(0xCC181B24)     // Translucent Frosted Glass
val AppleHairline = Color(0xFF262A36)         // Subtle 0.75dp Hairline Border
val AppleHairlineLight = Color(0x33FFFFFF)    // Translucent Hairline Divider

val AppleAccentBlue = Color(0xFF0A84FF)       // Apple System Blue (Primary Action)
val AppleAccentIndigo = Color(0xFF5E5CE6)     // Apple System Indigo (Glow / Secondary)
val AppleAccentCyan = Color(0xFF64D2FF)       // Apple System Cyan (Accent)
val AppleAccentMint = Color(0xFF63E6E2)       // Apple System Mint

val AppleSuccess = Color(0xFF30D158)          // Apple System Green
val AppleWarning = Color(0xFFFF9F0A)          // Apple System Orange
val AppleError = Color(0xFFFF453A)            // Apple System Red

val AppleLabelPrimary = Color(0xFFFFFFFF)     // High-Contrast Primary Typography
val AppleLabelSecondary = Color(0xFF8E8E93)   // Secondary Caption / Metadata
val AppleLabelTertiary = Color(0xFF636366)    // Subtle Subtitle / Hint
val AppleFillTertiary = Color(0x1FFFFFFF)     // Translucent Button & Chip Fill

// Backward Compatibility Aliases mapped to Apple HIG Palette
val DexterBackground = AppleCanvas
val DexterSurface = AppleCardBackground
val DexterSurfaceVariant = AppleElevatedSurface
val DexterPrimary = AppleAccentBlue
val DexterPrimaryGlow = AppleAccentCyan
val DexterOnPrimary = Color(0xFFFFFFFF)
val DexterOnBackground = AppleLabelPrimary
val DexterOnSurface = Color(0xFFF2F2F7)
val DexterOnSurfaceVariant = AppleLabelSecondary

val DexterSuccess = AppleSuccess
val DexterWarning = AppleWarning
val DexterError = AppleError

val PriorityCritical = AppleError
val PriorityHigh = AppleWarning
val PriorityNormal = AppleAccentBlue
val PriorityLow = AppleLabelSecondary
val PrioritySomeday = AppleLabelTertiary
