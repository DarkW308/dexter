# Dexter Android App — UI/UX Specification v1

**Status:** FINALIZED for MVP  
**Design System:** Material You (Material 3) — Dark Theme  
**Platform:** Android 8+ (API 26+)  
**Target Device:** Vivo V11 (6.41" FHD+ display, 1080×2340, 402 PPI)

---

## 1. Design Principles

1. **Voice is the UI.** Every screen is secondary to the voice orb. If the user is looking at the screen, something went wrong with voice.
2. **Calm, not busy.** Never more than 3 pieces of information visible simultaneously without scrolling.
3. **Dark and focused.** Eyes-open use at night is common. The UI must not strain the eyes.
4. **Instant feedback.** Every user action gets a visual or audio response within 100 ms.
5. **No hidden gestures.** Every action is visible. No swipe-to-reveal, no hidden menus.

---

## 2. Color System

| Token | Value | Use |
| :--- | :--- | :--- |
| `background` | `#1A1A2E` | All screen backgrounds |
| `surface` | `#252540` | Cards, bottom nav, dialogs |
| `surface_variant` | `#2E2E50` | Input fields, secondary cards |
| `primary` | `#4FC3F7` | Electric blue — Voice Orb, active states, CTAs |
| `primary_glow` | `#00E5FF` | Voice Orb active animation glow |
| `on_primary` | `#000000` | Text on primary buttons |
| `on_background` | `#FFFFFF` | Primary text |
| `on_surface` | `#E0E0E0` | Secondary text, card content |
| `on_surface_variant` | `#9E9E9E` | Timestamps, subtitles, hints |
| `error` | `#CF6679` | Destructive actions, CRITICAL priority bar |
| `warning` | `#FFAB40` | HIGH priority bar, warnings |
| `success` | `#69F0AE` | Completed states, positive confirmations |
| `priority_critical` | `#FF1744` | CRITICAL task bar |
| `priority_high` | `#FF6D00` | HIGH task bar |
| `priority_normal` | `#4FC3F7` | NORMAL task bar |
| `priority_low` | `#546E7A` | LOW/SOMEDAY task bar |

---

## 3. Typography

| Style | Font | Size | Weight | Use |
| :--- | :--- | :--- | :--- | :--- |
| `headline_large` | Roboto | 32 sp | Bold | Screen titles |
| `headline_medium` | Roboto | 24 sp | SemiBold | Card section headers |
| `title_medium` | Roboto | 18 sp | Medium | Task/alarm titles |
| `body_large` | Roboto | 16 sp | Regular | Card body text, TTS transcript |
| `body_medium` | Roboto | 14 sp | Regular | Subtitles, descriptions |
| `label_small` | Roboto Mono | 11 sp | Regular | Timestamps, status chips |

---

## 4. Screen Architecture

```
┌─────────────────────────────────────────────┐
│                                             │
│  HOME SCREEN                                │
│  (Voice Orb + Today's Briefing Card)        │
│                                             │
├──────┬──────────┬──────────┬────────────────┤
│ Home │  Tasks   │ Finance  │ Journal │ More │
└──────┴──────────┴──────────┴────────────────┘
```

**5 primary screens:**

1. **Home** — Voice Orb + Briefing summary card
2. **Tasks** — Full task management (Pending / Completed tabs)
3. **Finance** — Expenses log + monthly summary
4. **Journal** — Notes log + voice capture shortcut
5. **More** — Goals, Projects, Alarms, Settings

---

## 5. Screen Specifications

---

### Screen 1 — Home (Voice Control Hub)

**Primary purpose:** Activate voice input. See today's status at a glance.

![Home Screen](C:\Users\darkw\.gemini\antigravity\brain\644487f3-1d56-4da5-a57b-525a61871211\dexter_home_screen_1791865293499.jpg)

**Layout:**
```
┌─────────────────────────────────┐
│  DEXTER                 🟢 Live │  ← App name + service status chip
├─────────────────────────────────┤
│                                 │
│                                 │
│         ╭───────────╮           │
│        ╱     ●●●     ╲          │  ← Voice Orb (200dp diameter)
│       │    ● ● ● ●    │         │    Idle: slow blue pulse
│        ╲     ●●●     ╱          │    Listening: fast cyan wave rings
│         ╰───────────╯           │    Processing: rotating arc
│                                 │    Speaking: bouncing bars
│          Tap to speak           │  ← Hint label (hidden while active)
│                                 │
├─────────────────────────────────┤
│  🌅 Today's Briefing            │  ← Briefing summary card (surface)
│  3 tasks pending · 1 reminder   │    Tap → full briefing screen
│  at 5 PM · ₹850 spent today    │
│  ──────────────────────────     │
│  📋 Next: Call the doctor       │  ← Next highest priority task
│  ⏰ 5:00 PM – Call mom          │  ← Next alarm
└─────────────────────────────────┘
```

**Voice Orb States:**

| State | Animation | Ring Color | Description |
| :--- | :--- | :--- | :--- |
| `IDLE` | Slow gentle pulse (2s cycle) | `#4FC3F7` 30% opacity | Waiting for tap |
| `LISTENING` | Fast concentric expanding rings | `#00E5FF` | Mic is recording |
| `PROCESSING` | Rotating arc/spinner | `#4FC3F7` | Needle processing |
| `SPEAKING` | Vertical amplitude bars bouncing | `#69F0AE` | TTS playing |
| `ERROR` | Single red flash pulse | `#CF6679` | Action failed or unclear |

**Interactions:**
- Tap orb → opens Voice Input overlay (fullscreen modal).
- Long-press orb → immediate start recording (skip animation intro).
- Pull down briefing card → expands to full Daily Briefing screen.

---

### Screen 2 — Voice Input Overlay (Fullscreen Modal)

**Triggered by:** Tap on Voice Orb.

![Voice Listening State](C:\Users\darkw\.gemini\antigravity\brain\644487f3-1d56-4da5-a57b-525a61871211\dexter_voice_active_state_1791865340507.jpg)

**Layout:**
```
┌─────────────────────────────────┐
│ ✕                          ||||  │  ← Close button + audio level bars
│                                 │
│                                 │
│         ╭─────────────╮         │
│        ╱  ·  ·  ·  ·   ╲        │  ← Enlarged orb with wave rings
│       │  · · · · · · ·  │       │    Active listening animation
│        ╲  ·  ·  ·  ·   ╱        │
│         ╰─────────────╯         │
│                                 │
│   "Remind me to call mom at..." │  ← Live transcript (italic white)
│                                 │
│          Listening...           │  ← State label in cyan
│                                 │
│ ╭─────────────────────────────╮ │
│ │           Cancel            │ │  ← Full-width cancel button
│ ╰─────────────────────────────╯ │
└─────────────────────────────────┘
```

**State transitions:**
- After speech stops → auto-transitions to `PROCESSING` state (spinner on orb).
- After processing → transitions to `SPEAKING` state (green bars on orb) + response text shown.
- On confirmation needed → shows Confirm/Cancel dialog over overlay.
- On `Cancel` or ✕ → closes overlay, orb returns to `IDLE`.

**Confirmation Dialog (inline, not separate screen):**
```
┌─────────────────────────────────────┐
│  ⚡ Confirm Action                   │
│                                     │
│  Set reminder "Call mom" at 5:00 PM │
│                                     │
│  [ Cancel ]          [ Confirm ]    │
└─────────────────────────────────────┘
```

---

### Screen 3 — Tasks

![Tasks Screen](C:\Users\darkw\.gemini\antigravity\brain\644487f3-1d56-4da5-a57b-525a61871211\dexter_tasks_screen_1791865317974.jpg)

**Layout:**
```
┌─────────────────────────────────┐
│ ← Tasks                    ⚙️  │
├─────────────────────────────────┤
│ [Pending (5)] [Completed]       │  ← Filter chips
├─────────────────────────────────┤
│ ▌  HIGH         Buy fresh milk  │  ← Priority bar left (red)
│    3h ago                    ○  │  ← Circle checkbox tap = complete
├─────────────────────────────────┤
│ ▌  NORMAL      Call the doctor  │  ← Orange bar
│    Yesterday                 ○  │
├─────────────────────────────────┤
│ ▌  NORMAL  Fix the leak in...   │  ← Card truncates at 1 line
│    2 days ago                ○  │
└─────────────────────────────────┘
                              [+]   ← FAB for manual task add (secondary)
```

**Interactions:**
- Tap checkbox → shows confirmation snackbar: *"Complete 'Buy fresh milk'? [UNDO]"*
- Long-press task card → shows action sheet: Complete / Cancel / Edit Priority
- Tap `[+]` FAB → opens text input sheet (secondary to voice)
- Pull to refresh → re-queries DB

---

### Screen 4 — Finance

```
┌─────────────────────────────────┐
│ ← Finance                       │
├─────────────────────────────────┤
│  This Month                     │
│  ┌───────────────────────────┐  │
│  │  Total Spent              │  │  ← Summary card
│  │  ₹8,450                   │  │
│  │  23 transactions          │  │
│  └───────────────────────────┘  │
├─────────────────────────────────┤
│  [Today] [This Week] [Month]    │  ← Period filter chips
├─────────────────────────────────┤
│  ↓ ₹350  Lunch         2h ago  │  ← Transaction rows
│  ↓ ₹1200 Electricity   3h ago  │    ↓ = DEBIT (expense)
│  ↓ ₹80   Auto           Today  │    ↑ = CREDIT (income, future)
└─────────────────────────────────┘
```

---

### Screen 5 — Journal

```
┌─────────────────────────────────┐
│ ← Journal                       │
├─────────────────────────────────┤
│  Today, October 13              │
├─────────────────────────────────┤
│  🎤  10:30 AM                   │  ← Voice-captured note (mic icon)
│  "Meeting rescheduled to        │
│   Monday morning"               │
├─────────────────────────────────┤
│  🎤  Yesterday, 8 PM            │
│  "Had a productive session,     │
│   completed the schema design"  │
└─────────────────────────────────┘
```

---

### Screen 6 — More (Overflow Screen)

```
┌─────────────────────────────────┐
│  More                           │
├─────────────────────────────────┤
│  🎯 Goals              →        │
│  📁 Projects           →        │
│  ⏰ Reminders & Alarms →        │
│  📊 Audit Logs         →        │
│  ⚙️ Settings           →        │
└─────────────────────────────────┘
```

---

### Screen 7 — Settings

```
┌─────────────────────────────────┐
│ ← Settings                      │
├─────────────────────────────────┤
│  VOICE                          │
│  STT Engine  [Android / Whisper]│
│  TTS Voice   [en-IN Female ▾]   │
│  Confidence  [0.30 ──●─── 1.0] │
├─────────────────────────────────┤
│  BRIEFING                       │
│  Morning time  [7:30 AM ▾]     │
│  Auto-briefing [ON / OFF]       │
├─────────────────────────────────┤
│  SYSTEM                         │
│  Needle Status  🟢 Running :8081│
│  DB Size        12.4 MB         │
│  Battery Opt.   [Open Settings] │
├─────────────────────────────────┤
│  BACKUP                         │
│  Export DB to SD Card    [→]    │
│  Last backup: Never             │
└─────────────────────────────────┘
```

---

## 6. Component Library

### 6.1 — Voice Orb Component
```kotlin
// Compose Animatable-driven orb with 4 states
// Uses Canvas API for rings
// Animates with spring + tween per state
DexterVoiceOrb(
    state = VoiceOrbState.LISTENING,  // IDLE | LISTENING | PROCESSING | SPEAKING | ERROR
    size = 200.dp,
    primaryColor = Color(0xFF4FC3F7),
    glowColor = Color(0xFF00E5FF),
    onTap = { viewModel.startVoiceInput() }
)
```

### 6.2 — Task Card Component
```kotlin
DexterTaskCard(
    task = task,
    onComplete = { viewModel.requestComplete(task) },
    onLongPress = { showTaskActionSheet(task) }
)
// Priority bar: 4dp wide vertical bar, full card height, left edge
// Checkbox: 24dp circle, right edge
// Title: 1 line max, ellipsis on overflow
// Subtitle: timestamp in label_small style
```

### 6.3 — Confirmation Snackbar
```kotlin
DexterConfirmSnackbar(
    message = "Complete 'Buy fresh milk'?",
    onConfirm = { viewModel.executeComplete(task) },
    onDismiss = { /* no action */ },
    autoDismissMs = 4000  // Auto-dismisses = no action taken
)
```

### 6.4 — Service Status Chip
```kotlin
DexterStatusChip(
    state = ServiceState.LIVE,  // LIVE | DEGRADED | OFFLINE
    // LIVE = green dot "Live"
    // DEGRADED = amber dot "Needle offline"
    // OFFLINE = red dot "Service stopped"
)
```

---

## 7. Navigation Structure

```
NavGraph (Compose NavHost)
├── HomeScreen
│   └── VoiceInputOverlay (dialog destination)
│       └── ConfirmationDialog (dialog destination)
├── TasksScreen
│   └── TaskActionSheet (bottom sheet)
├── FinanceScreen
├── JournalScreen
├── MoreScreen
│   ├── GoalsScreen
│   ├── ProjectsScreen
│   ├── AlarmsScreen
│   ├── AuditLogsScreen
│   └── SettingsScreen
```

**Back navigation:** Standard Android back stack. Bottom nav switches root destinations without adding to backstack (popUpTo + launchSingleTop).

---

## 8. Animation Specifications

| Animation | Duration | Easing | Notes |
| :--- | :--- | :--- | :--- |
| Orb idle pulse | 2000ms cycle | EaseInOut | Infinite loop, opacity 60%→100% |
| Orb → Listening | 300ms | Spring | Scale up 1.0→1.15, brightness +30% |
| Wave rings expand | 800ms | EaseOut | 3 rings, staggered 200ms |
| Orb → Processing | 1200ms cycle | LinearEasing | Rotating arc, infinite |
| Orb → Speaking | 150ms per beat | EaseInOut | Bar heights driven by TTS amplitude |
| Screen transition | 350ms | EaseInOut | Shared axis Y (slide up) |
| Card appear | 200ms | EaseOut | Fade + scale 0.95→1.0, staggered |
| Snackbar enter | 250ms | EaseOut | Slide up from bottom |

---

## 9. Accessibility

- Minimum touch target: 48×48 dp for all interactive elements.
- Voice Orb: `contentDescription = "Dexter voice input. Tap to speak."`.
- Task checkboxes: `contentDescription = "Mark '${task.title}' complete"`.
- All colors meet WCAG AA contrast ratio (4.5:1) against `#1A1A2E` background.
- TTS verbosity: Every screen action announces result audibly (no silent failures).
- System font size respected: SP units used for all text, DP for all layouts.

---

## 10. Empty States

| Screen | Empty State Message | Illustration |
| :--- | :--- | :--- |
| Tasks (no pending) | "All clear! No pending tasks." | ✅ Checkmark |
| Tasks (no completed) | "Nothing completed yet." | — |
| Finance (no transactions) | "No expenses logged." | — |
| Journal (no entries) | "No notes yet. Say 'Take a note'." | 📓 |
| Goals (no goals) | "No goals set. Say 'Set a goal'." | 🎯 |
| Alarms (none) | "No active reminders." | ⏰ |
