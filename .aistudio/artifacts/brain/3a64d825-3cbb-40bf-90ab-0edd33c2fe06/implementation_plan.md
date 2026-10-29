# Implementation Plan: Apple-Inspired UI Redesign for Dexter

## 1. Design Vision & Guiding Principles
Transform Dexter's user interface to embody the pinnacle of **Apple's Human Interface Guidelines (HIG)**:
1. **Dark iOS Deep Graphite & Translucent Materials**: True OLED deep graphite canvas (`#08090B`), elevated frosted glass container materials (`#15171E` / `#1C1F28` with subtle translucency), crisp hairline strokes (`#262A36`), and vibrant Apple System Blue (`#0A84FF`) & Electric Indigo (`#5E5CE6`) accents.
2. **Grouped Inset Cards with Squircle Geometry**: Replacing harsh boxes with iOS-style grouped inset lists and cards featuring continuous corner radii (`18.dp` – `24.dp`), generous 16dp–20dp screen margins, and hair-thin border separators.
3. **Clean Typographic Hierarchy**: Tight tracking and disciplined scale modeling iOS typography (Large Title 34sp, Title 1-3 20–28sp, Headline 17sp SemiBold, Body 16sp Regular, Footnote 13sp, Caption 11sp).
4. **Fluid Tactile Interactivity**: Subtle scale feedback on tap, frosted blur backdrops, and an illuminated Voice Orb with glassmorphic refraction.

---

## 2. Architecture & Theming Redesign

```
┌────────────────────────────────────────────────────────────────────────┐
│                        APPLE HIG THEME SYSTEM                          │
├───────────────────────────────────┬────────────────────────────────────┤
│   DEEP GRAPHITE & FROSTED GLASS   │   CLEAN TYPOGRAPHIC SCALE          │
│   • Background: #08090B (OLED)    │   • Large Title (34sp Bold, -0.4sp)│
│   • Inset Card: #15171E / #1C1F28 │   • Headline (17sp SemiBold)       │
│   • Hairline: #262A36 (0.75dp)    │   • Body (16sp Regular, +0.2sp)    │
│   • Accent: #0A84FF / #5E5CE6     │   • Caption / Footnote (12-13sp)   │
├───────────────────────────────────┴────────────────────────────────────┤
│                       GROUPED INSET COMPONENTS                         │
│   • Inset Rounded Cards (20dp)    • Frosted Voice Orb with Glow Ring   │
│   • Segmented Filter Pills (iOS)  • Settings Inset Rows with Chevrons  │
└────────────────────────────────────────────────────────────────────────┘
```

### Component Upgrades:
1. **`Color.kt` & `Theme.kt`**:
   - Introduce Apple System Dark color tokens:
     - `AppleCanvas = #08090B` (Canvas background)
     - `AppleCardBackground = #15171E` (Grouped inset card base)
     - `AppleElevatedSurface = #1C202B` (Top bars, dialogs, sheets)
     - `AppleHairline = #262A36` (Hairline borders & separators)
     - `AppleAccentBlue = #0A84FF` (Primary system tint)
     - `AppleAccentIndigo = #5E5CE6` (Secondary glow tint)
     - `AppleLabelPrimary = #FFFFFF`, `AppleLabelSecondary = #8E8E93`, `AppleLabelTertiary = #636366`
   - Shapes defined with squircle radii: `Small = 12.dp`, `Medium = 18.dp`, `Large = 24.dp`, `ExtraLarge = 32.dp`.
2. **`Type.kt`**:
   - Calibrated scale with strict Apple HIG tracking:
     - `largeTitle`: 34sp, Bold, letterSpacing = -0.4.sp
     - `titleMedium`: 20sp, SemiBold, letterSpacing = -0.2.sp
     - `bodyMedium`: 16sp, Normal, letterSpacing = 0.sp
     - `labelMedium`: 13sp, Medium, letterSpacing = 0.1.sp
     - `labelSmall`: 11sp, Medium, letterSpacing = 0.2.sp
3. **`VoiceOrbFab.kt` & Voice Orb Visualizer**:
   - Re-styled with frosted glass halo, radial gradient refraction, and subtle micro-shadowing.
4. **`VoiceLabScreen.kt`**:
   - Segmented pill controls matching iOS `UISegmentedControl`.
   - Inspection card rendered as a grouped inset card with glowing hairline border and clean monospace token chips.
5. **`ProfileScreen.kt` & `MainActivity.kt`**:
   - Refactored into grouped inset list sections (Account, Intelligence, Security, Diagnostics) with subtle row separators and iOS-style chevron accessories.

---

## 3. Step-by-Step Implementation Plan

### Step 1: Core Design System Tokens (`Color.kt`, `Theme.kt`, `Type.kt`)
- Define the new Apple HIG color palette, squircle shapes, and typographic scale.
- Ensure seamless dark-mode dynamic contrast and accessibility compliance.

### Step 2: Inset Card & Glassmorphic Surface Primitives
- Create reusable `AppleInsetCard` and `AppleGlassSurface` composables with continuous curves and subtle hairline strokes.
- Add iOS-style `AppleSegmentedControl` for category switching.

### Step 3: Redesign Voice Diagnostics & Voice Orb
- Apply the frosted glass look to `VoiceOrbFab.kt`.
- Redesign `VoiceLabScreen.kt` with grouped inset cards, clean tokens, and readable typography.

### Step 4: Redesign Profile & Settings Screen (`ProfileScreen.kt`)
- Modernize the Profile screen with iOS Settings grouped inset sections, crisp icons, and refined toggle switches.

### Step 5: Verification & Compilation
- Run `compile_applet` and `gradle :app:testDebugUnitTest` to guarantee zero regressions and flawless build health.

---

## 4. Verification & Testing Deliverables
1. **Compilation**: Clean Android APK build with updated Compose styling.
2. **Unit Tests**: All 84 unit tests continue to pass without error.
3. **UI Verification**:
   - Smooth grouped inset layout in emulator.
   - Refined frosted glass voice orb and segmented pill chips.
   - Elegant, readable typography with strict hierarchy.
