# Dexter Android App — Engineering Rules & Constraints v1

**Status:** FINALIZED  
**Target Hardware:** Vivo V11 (MediaTek Helio P60, 6 GB RAM, Android 8 / API 26)  
**Minimum Supported Android:** API 21 (Android 5.0 Lollipop) — must function on all devices Android 5+  
**Architecture:** Native Android (Kotlin, View-based XML layouts) + Needle 3 (ARM64 C++ Local Brain)  
**Database:** SQLite — single source of truth for all structured data  
**AI Engine:** Two-Tier (Tier 1: Deterministic Fast-Path · Tier 2: Needle 3 Neural, 71 MB RAM)  
**Battery Philosophy:** Dexter must consume ≤ 3% battery per hour in standby Alexa-mode. Battery life is non-negotiable.

---

## RULE 1 — The Database Is the Single Source of Truth

- All structured Dexter data lives in `dexter.db` (SQLite, WAL mode, foreign keys ON).
- No subsystem may maintain a competing source of truth. Not a Room cache, not SharedPreferences for domain data, not an in-memory store.
- Totals, summaries, and derived values **are calculated from transactions**, never stored redundantly.
- The database schema in `schema.sql` is the contract. No table may be added, removed, or altered outside a versioned migration.

```sql
-- Every connection must open with these two pragmas:
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
```

---

## RULE 2 — The AI Model Is an Untrusted Proposer, Never an Executor

```
Voice Input
    ↓
STT (Whisper or Android STT)
    ↓
Tier 1: Deterministic Fast-Path (~1–5 ms, Python/Kotlin)
    ↓ (if unmatched)
Tier 2: Needle 3 Neural Proposal (~1.3s, 71 MB RAM, ARM64 C++)
    ↓
Envelope Contract Validation (Kotlin — mirrors qwen_contract.py rules)
    ↓
ActionExecutor (Kotlin — mirrors action_system.py)
    ↓
SQLite Database
    ↓
TTS Response
```

- The model output is a **proposal object**, never a direct command.
- The model **must never** generate or invent database IDs (task_id, alarm_id, etc.).
- The model **must never** directly write to SQLite.
- The model **must never** execute shell commands in production.
- Every model proposal passes through contract validation before any database write occurs.
- If confidence is below the threshold (default: 0.30), the action is suppressed. The user is asked for clarification.

---

## RULE 3 — Needle 3 Runs as a Localhost Daemon

- The Needle 3 binary (`needle3 --serve`) is bundled in `assets/needle/`.
- On app start, `DexterVoiceService` extracts the binary to `context.filesDir/needle3`, sets it executable, and starts it on `127.0.0.1:8081`.
- The Kotlin adapter talks to Needle via `http://127.0.0.1:8081/complete` (HTTP POST, JSON).
- This mirrors the proven Termux MVP architecture exactly — zero JNI required.
- If Needle crashes, the service restarts it with exponential backoff. The deterministic Tier 1 fast-path continues to function independently.

---

## RULE 4 — Background Execution Uses Foreground Services

- `DexterVoiceService` runs as an Android Foreground Service with a persistent notification.
- The service holds `PARTIAL_WAKE_LOCK` to prevent CPU from sleeping during active inference.
- All reminders use `AlarmManager.setExactAndAllowWhileIdle()` — guaranteed to fire even in Doze mode.
- App must be added to Android battery optimization whitelist during onboarding.
- No background processes rely on `time.sleep()` loops — all scheduling uses `AlarmManager` or `Handler.postDelayed()`.

---

## RULE 5 — Voice Pipeline: Alexa-Mode is the Primary Interface

> **Dexter must feel like Alexa. The user must never need to touch the phone to use Dexter.**

The voice pipeline runs in three modes. Mode 1 is the default and must be the primary daily-use mode for MVP.

### Mode 1 — Always-On Hands-Free (Alexa Mode) ★ PRIMARY
- **Wake-word engine:** `openWakeWord` (Python-side, ARM64 native binary) bundled in `assets/wakeword/`. Runs on a background thread at `16 kHz`, `512-frame` windows. CPU use: ≤ 15% of one A53 core.
- **Trigger:** User says **"Hey Dexter"** → wake-word fires → mic opens full-quality → STT starts.
- **No screen touch required, no button required.** The phone can be across the room.
- **Battery-safe design:** wake-word engine uses a separate low-priority `HandlerThread`. The hot-word model is tiny (< 5 MB) and runs in 8-bit quantized mode to minimize CPU heat.
- **Feedback:** On wake-word detect → short 80ms haptic vibration + soft "ding" earcon sound.

### Mode 2 — Volume-Button Double-Press (Pocket Mode)
- Double-press the volume-down button → activates mic directly (skips wake-word).
- Implemented via `MediaSessionCompat` broadcast intent. Works with screen off.
- Use case: noisy environments where "Hey Dexter" is unreliable.

### Mode 3 — Tap-to-Talk (Screen On)
- Tap the Voice Orb on the Home screen.
- Standard push-to-talk. Recording auto-stops on silence (VAD).

### Shared voice pipeline rules (all modes):
- **STT:** Android native `SpeechRecognizer` (offline mode) as primary. `whisper.cpp` `ggml-tiny.en` (ARM64 NEON, 4 threads) as fallback if native STT is unavailable.
- **VAD (Voice Activity Detection):** Streaming `AudioRecord` at 16 kHz, mono, 16-bit PCM with energy-threshold Voice Activity Detection. Recording auto-stops when silence exceeds 1.2s. **No fixed time cutoff.**
- **TTS:** Android native `android.speech.tts.TextToSpeech` exclusively — zero deadlock risk, sub-10ms latency, works offline on all Android versions.
- **Barge-in:** If user speaks while Dexter is playing TTS audio, TTS is immediately silenced and mic re-opens. This is real-time barge-in, not push-button interruption.
- **Whisper silence hallucination filter:** Post-transcription, discard if text matches: `{"thank you", "thanks for watching", "please subscribe", "you", "bye"}`.
- **Conversation context:** After each voice turn, context is held for 30 seconds. If user speaks again within 30s, "it", "that", "this one" are resolved against the last action.



## RULE 6 — Privacy and Data Residency

- **All data is local.** No user data ever leaves the device.
- No telemetry, analytics, or crash reporting that sends personal data off-device.
- No cloud sync in MVP. Database backup is local only (`/sdcard/DexterBackup/`).
- Raw microphone audio is never persisted beyond the current voice turn.
- The `audit_logs` table records every destructive database action (DELETE, UPDATE on critical fields).

---

## RULE 7 — Destructive Actions Require Confirmation

- Any action that deletes, marks complete, or cancels a record requires explicit user confirmation before execution.
- The confirmation prompt must state exactly what will happen: `"Complete task: Buy milk. Confirm?"`.
- Bulk operations (e.g., `COMPLETE_ALL_TASKS`) require confirmation with the count: `"Mark all 7 pending tasks complete. Confirm?"`.
- Confidence threshold < 0.30: do not execute. Request clarification.
- Confidence 0.30–0.50 on destructive actions: confirm before executing.
- Confidence > 0.50 on safe reads (LIST, SUMMARIZE): execute immediately.

---

## RULE 8 — Domain Isolation

- Each domain (Tasks, Finance, Journal, Goals, etc.) is owned by a single Kotlin Repository class.
- No repository may directly query another domain's tables via raw SQL.
- Cross-domain data (e.g., a briefing that reads tasks + finance + journal) goes through a dedicated `BriefingRepository` that calls individual repositories.
- The `system_events` and `audit_logs` tables are global — all repositories write to them.

---

## RULE 9 — Error Handling and Resilience

- Every database operation is wrapped in a try/catch. Failures are logged to `system_logs`.
- If Needle 3 is unavailable (daemon not started, port 8081 unreachable), fall back to Tier 1 deterministic fast-path only. Never crash the app.
- If STT returns empty or hallucinated text, the voice turn is silently discarded. No phantom actions.
- If TTS fails, display response as on-screen text. Never block the main thread.

---

## RULE 10 — Performance Constraints (Helio P60 Budget)

> All numbers below are hard ceilings on Vivo V11. No exceptions.

| Component | Hard Limit |
| :--- | :--- |
| Needle 3 RAM | ≤ 80 MB RSS |
| Whisper `tiny.en` RAM | ≤ 80 MB RSS |
| openWakeWord model RAM | ≤ 20 MB RSS |
| App UI RAM | ≤ 120 MB |
| **Total app memory (all components)** | **≤ 320 MB** |
| Voice turn latency (Tier 1 deterministic) | ≤ 10 ms |
| Voice turn latency (Tier 2 Needle, warm) | ≤ 2.5 s |
| Wake-word detection latency | ≤ 200 ms |
| Database query (single domain) | ≤ 50 ms |
| UI frame rate | 60 fps on main screens |
| App cold start time | ≤ 3 s |
| **Battery consumption — Alexa standby mode** | **≤ 3% per hour** |
| Battery consumption — active voice turn | ≤ 1% per turn |
| APK size (total installed) | ≤ 60 MB |

**Threading rules:**
- All database queries run on a background `Dispatchers.IO` coroutine. **Never on the main thread.**
- All Needle HTTP calls run on `Dispatchers.IO`. **Never on the main thread.**
- Wake-word engine runs on a dedicated `HandlerThread` at `THREAD_PRIORITY_LOWEST`.
- TTS runs on a daemon thread. Never blocks main thread or voice thread.
- `RecyclerView` with `DiffUtil` for all list views — loads only visible items.



## RULE 11 — Notification Rules

- Dexter must only interrupt the user when the interruption is justified.
- Notification channels:
  - `CHANNEL_CRITICAL` — VVIP alarms, urgent deadlines (full-screen, sound, vibration).
  - `CHANNEL_REMINDER` — User-set reminders (heads-up, sound).
  - `CHANNEL_NORMAL` — Briefings, summaries (silent, drawer only).
  - `CHANNEL_SERVICE` — Foreground service persistent notification (minimal, silent).
- No marketing, promotional, or engagement notifications ever.

---

## RULE 12 — MVP Scope Lock

The MVP includes only these 16 capabilities (matching existing `dexter_tools.json`):

| # | Capability | Domain |
|---|---|---|
| 1 | Create Task | Tasks |
| 2 | Complete Task | Tasks |
| 3 | List Pending Tasks | Tasks |
| 4 | List Completed Tasks | Tasks |
| 5 | Complete All Tasks | Tasks |
| 6 | Cancel Task | Tasks |
| 7 | Log Expense | Finance |
| 8 | Summarize Expenses | Finance |
| 9 | Log Journal Note | Journal |
| 10 | List Journal Notes | Journal |
| 11 | Set Reminder/Alarm | Alarms |
| 12 | List Alarms | Alarms |
| 13 | Cancel Alarms | Alarms |
| 14 | Create Goal | Goals |
| 15 | List Goals | Goals |
| 16 | Create Project | Projects |
| 17 | List Projects | Projects |
| 18 | Daily Briefing | Briefing |

**Out of scope for MVP:** Relationships, Wellbeing, Documents/RAG, Recurring Tasks, Schedule Blocks, Cloud Sync, Multi-language STT.

> **Wake-Word (Hey Dexter) is IN scope for MVP.** It is the primary voice interaction mode.

---

## RULE 13 — Hardware Compatibility (Must Run on Vivo V11)

> Everything is designed around one real device: **Vivo V11, MediaTek Helio P60 (MT6771), Android 8 (API 26), 6 GB RAM, aarch64 (ARM64-v8a).**

All native binaries (Needle 3, openWakeWord, Whisper) must be:
- Compiled for `arm64-v8a` (primary).
- Optionally include `armeabi-v7a` fallback for older 32-bit ARM devices.
- Use NEON SIMD intrinsics where available — the Helio P60 has full ARM NEON support.
- **No x86 / x86_64 binaries needed.** This is a phone, not an emulator.

**OEM-specific rules for Vivo/FunTouch OS:**
- Request battery whitelist explicitly: open `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent during onboarding.
- Do not rely on `BOOT_COMPLETED` broadcast alone — FunTouch OS may delay it. Use AlarmManager to self-reschedule on first boot.
- Audio routing: explicitly request `AudioManager.MODE_NORMAL` and `STREAM_MUSIC` channel for TTS. Do not use `STREAM_VOICE_CALL` (Vivo OEM conflict).

---

## RULE 14 — Minimum Android Version: API 21 (Android 5.0)

The app must compile with `minSdkVersion 21` and run correctly on:

| Android Version | API Level | Notes |
| :--- | :--- | :--- |
| Android 5.0 Lollipop | API 21 | Minimum. Basic voice + DB only. |
| Android 6.0 Marshmallow | API 23 | Runtime permissions (RECORD_AUDIO). |
| Android 7.0 Nougat | API 24 | Background execution limits begin. |
| Android 8.0 Oreo | **API 26** | **Primary target (Vivo V11).** AlarmManager exact + Foreground Service required. |
| Android 10+ | API 29+ | Background location/audio restrictions. Use foreground service. |

**Compatibility rules:**
- Use `Build.VERSION.SDK_INT` guards for any API above 21. Never crash on lower API.
- `AlarmManager.setExactAndAllowWhileIdle()` requires API 23 — wrap in version check, fall back to `setExact()` on API 21–22.
- Notification channels (`NotificationChannel`) require API 26 — wrap in version check.
- Do NOT use `JobScheduler` for voice service — it is throttled on API 23+. Use Foreground Service.
- Do NOT use `WorkManager` for the voice daemon — it cannot guarantee exact timing.

---

## RULE 15 — Language, Framework, and Dependency Rules

> **The app must be light, fast, and maintainable. No heavy frameworks.**

### Required Stack
| Layer | Technology | Why |
| :--- | :--- | :--- |
| **Primary language** | **Kotlin** | Concise, null-safe, Coroutines built-in |
| **UI framework** | **Android XML Views + ViewBinding** | Lighter than Compose, works on API 21+, less RAM |
| **Database ORM** | **Android Room** (on top of SQLite) | Compile-time verified queries, migrations, WAL mode |
| **HTTP (Needle)** | **OkHttp 4** (no Retrofit) | Minimal overhead, direct HTTP to localhost |
| **Concurrency** | **Kotlin Coroutines** | No RxJava, no extra library |
| **JSON** | **kotlinx.serialization** | Compile-time, zero reflection |
| **Audio** | **Android `AudioRecord` + `AudioTrack`** | Native API, zero extra libs |
| **TTS** | **Android `TextToSpeech`** | Built-in, no dependency |
| **Images** | **No image loading library** | MVP has no external images |
| **DI** | **Manual constructor injection** | No Dagger/Hilt in MVP (overengineering) |

### Banned dependencies
- ❌ Jetpack Compose (too heavy for API 21 and Helio P60 GPU overhead)
- ❌ Retrofit (unnecessary abstraction for one localhost endpoint)
- ❌ RxJava (Kotlin Coroutines is sufficient)
- ❌ Firebase (any Firebase SDK — no cloud, no analytics)
- ❌ Google Play Services (must work without them)
- ❌ Glide / Picasso / Coil (no external images in MVP)
- ❌ Dagger / Hilt (over-engineering for MVP scale)
- ❌ Any library > 2 MB that is not a core AI binary

### APK size discipline
- `minifyEnabled true` + `shrinkResources true` in `release` build.
- ProGuard/R8 rules must preserve Room entities and OkHttp.
- Native `.so` binaries (Needle, Whisper, openWakeWord) are excluded from APK minification — they are pre-compiled.

---

## RULE 16 — Battery Optimization: Always-On Mode

> **Dexter runs 24/7. Battery usage must be surgical.**

### What consumes battery in Alexa-mode and how to control it:

| Component | Normal drain | Dexter's approach |
| :--- | :--- | :--- |
| Always-on microphone (raw) | ~8% / hour | ❌ Never keep raw mic always open |
| openWakeWord (8-bit, 16kHz) | ~2% / hour | ✅ Use — tiny model, integer math only |
| Screen on | ~15% / hour | ✅ Screen stays off in standby |
| Needle 3 (idle, not running) | 0% | ✅ Needle only runs during active voice turns |
| AlarmManager wakeup | < 0.1% per wakeup | ✅ Use for reminders — not polling |
| SQLite WAL idle | < 0.1% | ✅ WAL mode = non-blocking reads |
| Foreground service (no computation) | ~0.5% / hour | ✅ Mandatory for background survival |

### Battery rules:
1. **Wake-word thread uses `THREAD_PRIORITY_LOWEST` (`+19`).** Android will throttle it before any other app process.
2. **Needle 3 is only started when a voice turn begins.** It is NOT running continuously.
3. **Screen is always off in Alexa standby.** The foreground service notification keeps the app alive without the screen.
4. **No polling loops.** All timers use `AlarmManager.setExactAndAllowWhileIdle()`.
5. **Audio is captured at 16 kHz, mono, 16-bit** — minimum quality for wake-word. Full 16 kHz is also used for STT (whisper uses 16 kHz natively, no resampling penalty).
6. **Request `WAKE_LOCK` only during active voice processing.** Release it immediately after TTS ends.
7. **Doze Mode compliance:** App must survive Doze. AlarmManager exact intents pierce Doze. Foreground service prevents idle-state suspension.
8. **Battery whitelist prompt during onboarding** — the user must grant `IGNORE_BATTERY_OPTIMIZATIONS` permission explicitly.

---

## RULE 17 — Zero Technical Knowledge: 100% Natural Language & Voice Data Entry

> **The user must NEVER need to know SQL, code, schemas, or syntax. Dexter is an ambient assistant, not a database console.**

### Core Requirements:
1. **Zero SQL / Zero Code Required for User:**
   - The user never types or sees SQL queries, table names, foreign keys, or database jargon.
   - All structured operations are translated behind the scenes from spoken natural language or simple human UI forms.
2. **Natural Language Voice Entry:**
   - Any voice input in casual, unstructured English/Hinglish (e.g., *"spent 450 on fuel"*, *"remind me to check the boiler tomorrow morning"*, *"note down that idea about solar panels"*, *"bought groceries for 1200"*) is parsed automatically by the NLU layer (Tier 1 deterministic fast-path or Tier 2 Needle 3 model).
   - The NLU layer maps conversational requests to schema entities (`amount`, `category`, `due_date`, `title`, `priority`) without the user ever worrying about column structure or data types.
3. **Simple Visual Fallback / No-Code Manual Entry:**
   - If using the UI, simple human input cards exist (title, note, amount field) with zero code or technical terminology.
4. **Conversational Disambiguation:**
   - If an entry is missing a required parameter (e.g., user says *"remind me to call Rahul"* without giving a time), Dexter responds conversationally: *"When would you like to be reminded?"*, letting the user answer naturally without restarting or writing commands.

