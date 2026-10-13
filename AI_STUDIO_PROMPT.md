# Google AI Studio — Dexter Android MVP Master Prompt Pack

This document contains the exact prompts and workflow to generate the complete Dexter Android MVP using **Google AI Studio** (using Gemini 1.5 Pro / Gemini 2.0 Flash with maximum context window).

---

## 📂 Step 0: Files to Upload to AI Studio

Before pasting any prompts, create a new chat in Google AI Studio and attach the following files from this repository:

1. `DEXTER_APP_RULES.md` — The non-negotiable architectural, hardware, battery, and safety rules.
2. `DEXTER_APP_FEATURES.md` — The complete feature set, intent mappings, and voice phrases.
3. `DEXTER_APP_UIUX.md` — Design system, color tokens, layout hierarchy, and state definitions.
4. `dexter/schema.sql` — Authoritative 38-table SQLite schema.
5. `dexter_tools.json` — 18-tool function-calling schema for Needle 3.
6. `action_system.py` — Deterministic ActionExecutor reference logic.
7. `qwen_contract.py` — Contract envelope validation rules.
8. `BENCHMARK_RESULTS.md` — Hardware profile (Helio P60, 71MB Needle 3 RAM footprint).

---

## 🚀 Execution Strategy: 3-Phase Generation

To ensure clean, production-grade, unbloated code without hallucinated libraries or compilation errors, execute in 3 focused prompts:

- **Prompt 1:** Android Project Setup, SQLite Database & Room/SQLite Layer, Action System.
- **Prompt 2:** Native Foreground Service, Daemon Controller (Needle 3 localhost :8081), Native Audio (STT/TTS/Wake-Word & VAD).
- **Prompt 3:** UI Layer (XML ViewBinding, Dark Theme Material You, Animated Voice Orb, Action Confirmations).

---

## 📝 Prompt 1: Core Architecture, Database, & Action Execution Engine

Copy and paste the prompt below into Google AI Studio after uploading the context files:

```markdown
You are an expert Principal Android Systems and Embedded AI Engineer.

I have uploaded the architectural specifications, SQLite schema (`schema.sql`), action executor logic (`action_system.py`), contract validation rules (`qwen_contract.py`), and engineering rules (`DEXTER_APP_RULES.md`) for **Dexter**: a lightweight, privacy-focused, offline-first personal AI assistant.

### Target Environment & Constraints:
- **Device:** Vivo V11 (MediaTek Helio P60 MT6771, 6GB RAM, Android 8.0 / API 26).
- **Minimum SDK:** API 21 (Android 5.0 Lollipop).
- **Target SDK:** API 34.
- **Language:** 100% Kotlin with Coroutines.
- **Architecture:** Clean Architecture with single source of truth in SQLite (`dexter.db`).
- **Dependencies:** Android Room or raw SQLiteOpenHelper with WAL mode and Foreign Keys enabled, OkHttp 4, kotlinx.serialization. NO heavy frameworks (No Compose, No Dagger/Hilt, No Firebase, No RxJava).

### Your Goal for Phase 1:
Generate the complete foundational data and action execution layer in Kotlin:

1. **Gradle Configuration:**
   - Root `build.gradle` and `app/build.gradle` with proper compiler options, minSdk 21, targetSdk 34, packaging options for ARM64 native binaries (`arm64-v8a`), and necessary lightweight dependencies.

2. **Database Engine (`DexterDatabaseHelper` / Room setup):**
   - Implements initialization from the uploaded `schema.sql`.
   - Explicitly runs `PRAGMA foreign_keys = ON;` and `PRAGMA journal_mode = WAL;` on every SQLite connection.
   - Core DAOs/Repositories for MVP entities:
     - `TaskRepository` (create, complete, cancel, list pending/completed).
     - `FinanceRepository` (log expense debit, monthly summary calculation from transaction sums).
     - `AlarmRepository` (insert alarm, cancel alarms, query pending alarms).
     - `JournalRepository` (insert note, list notes).
     - `AuditRepository` (write to `audit_logs` and `system_events`).

3. **Validation & Action Contract Layer (`DexterContractValidator.kt`):**
   - Port the envelope validation logic from `qwen_contract.py`.
   - Validate confidence thresholds (reject < 0.30).
   - Ensure the AI never invents or hallucinated IDs; strictly sanitize parameters.

4. **Deterministic Action Executor (`DexterActionExecutor.kt`):**
   - Port the execution engine from `action_system.py`.
   - Implements safe execution for all 18 tools defined in `dexter_tools.json`.
   - Differentiates between safe read actions (execute immediately) and destructive mutations (require confirmation flag).
   - Writes every change to the SQLite database and logs to `audit_logs`.

Provide complete, compile-ready Kotlin files without skipping code or using placeholders.
```

---

## 📝 Prompt 2: Background Daemon, Localhost Inference, & Alexa-Style Voice Pipeline

Once Phase 1 is generated, send this prompt:

```markdown
We are now implementing Phase 2: The Native Background Voice Pipeline and Needle 3 Daemon Manager.

Reference: `DEXTER_APP_RULES.md` (Rules 3, 4, 5, 10, 13, 14, 16) and `DEXTER_APP_FEATURES.md`.

### Key Constraints:
- **Alexa Mode (Always-On Hands-Free):** The user can trigger Dexter by saying "Hey Dexter" from across the room without touching the screen.
- **Battery Target:** Standby battery drain must not exceed ≤ 3% per hour.
- **Process Isolation:** The Needle 3 engine runs as a precompiled ARM64 C++ binary locally on `http://127.0.0.1:8081`.

### Your Goal for Phase 2:
Generate the production Kotlin code for:

1. **Needle 3 Daemon Controller (`NeedleProcessManager.kt`):**
   - Unpacks the bundled ARM64 binary from `assets/needle/needle3` into `context.filesDir/needle3` on first run and sets executable permissions (`chmod 755`).
   - Starts and monitors the background process (`needle3 --serve :8081`).
   - Implements health check via OkHttp (`GET http://127.0.0.1:8081/health` or simple ping) with automatic restart on crash.
   - Shuts down cleanly when the service stops.

2. **Local Inference Client (`NeedleHttpClient.kt`):**
   - Uses OkHttp to send `/complete` POST requests with prompt and tool schema.
   - Implements timeout limits (connection 2s, read 5s).
   - Implements Tier 1 fallback: if Needle is unreachable, seamlessly route through Tier 1 deterministic regex fast-path.

3. **Foreground Service (`DexterVoiceService.kt`):**
   - Runs as an Android Foreground Service with an un-intrusive persistent notification (`CHANNEL_SERVICE`).
   - Acquires `PARTIAL_WAKE_LOCK` only during active voice transcription/inference.
   - Automatically handles Android Oreo (API 26+) background execution constraints and battery optimization intents (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

4. **Alexa-Style Hands-Free Audio & Voice Pipeline (`DexterAudioEngine.kt`):**
   - Low-power wake-word listening loop running on a dedicated low-priority background thread (`THREAD_PRIORITY_LOWEST`).
   - Audio capture via native `AudioRecord` (16 kHz, 16-bit mono PCM).
   - Voice Activity Detection (VAD) with energy threshold: automatically stops recording after 1.2s of silence.
   - Speech-to-Text (STT): Android native `SpeechRecognizer` (offline mode) with fallback to bundled lightweight `whisper.cpp` engine.
   - Text-to-Speech (TTS): Native `android.speech.tts.TextToSpeech`.
   - **Barge-in Support:** If audio input is detected while TTS is actively speaking, immediately silence TTS and switch to listening.
   - Hallucination filter: automatically discards common silence artifacts (`"thank you"`, `"thanks for watching"`).

5. **Exact Reminders (`DexterAlarmReceiver.kt`):**
   - Uses `AlarmManager.setExactAndAllowWhileIdle()` (API 23+) or `setExact()` (API 21-22) to fire reminders even in deep Doze mode.
   - Dispatches audio alert and heads-up notification upon trigger.

Provide production-grade, thread-safe, memory-safe Kotlin code.
```

---

## 📝 Prompt 3: UI Layer, Material You Theme, Animated Voice Orb, & Zero-Code Entry

Once Phase 2 is generated, send this prompt:

```markdown
We are now implementing Phase 3: The User Interface and Presentation Layer.

Reference: `DEXTER_APP_UIUX.md`, `DEXTER_APP_RULES.md` (Rule 15, 17), and the UI mockups.

### UI Principles & Constraints:
- **Framework:** Standard Android XML Layouts with ViewBinding (lightweight, API 21+ compatible, no Jetpack Compose memory overhead).
- **Design:** Deep Dark Charcoal Theme (`#1A1A2E`), Electric Blue accent (`#4FC3F7`), Cyan Glow (`#00E5FF`).
- **Zero Technical Jargon:** 100% natural language and simple cards. No SQL or technical data types shown to the user.
- **Voice-First:** The central interaction element is an interactive, animated Voice Orb widget.

### Your Goal for Phase 3:
Generate the layouts, custom views, and activities/fragments:

1. **Custom Animated Voice Orb View (`VoiceOrbView.kt` + custom canvas draw):**
   - Custom `View` rendering the concentric glowing rings and pulsating core.
   - Supports 5 visual states with smooth transitions:
     - `IDLE`: Gentle slow breathing pulse (electric blue).
     - `LISTENING`: Fast expanding sonic wave rings (electric cyan).
     - `PROCESSING`: Rotating arc indicator.
     - `SPEAKING`: Bouncing amplitude bars (green accent `#69F0AE`).
     - `ERROR`: Subtle red pulse (`#CF6679`).
   - Handles touch events (tap to talk, long-press to instant record).

2. **Main Hub Activity & Layout (`MainActivity.kt` & `activity_main.xml`):**
   - Clean top bar with Dexter title and small service status dot (🟢 Live / 🟡 Degraded / 🔴 Offline).
   - Center container featuring the large `VoiceOrbView` and dynamic status hint label ("Tap to speak" / "Listening...").
   - Summary card: "Today's Briefing" displaying pending task count, next alarm time, and today's total expenses.
   - Bottom navigation bar: Home, Tasks, Finance, Journal, More.

3. **Task & Data Views:**
   - `TasksActivity.kt` & `item_task.xml`: RecyclerView showing tasks with vertical priority color bars (Red: High, Orange: Normal, Gray: Low), title, and tap-to-complete circular checkbox.
   - `FinanceActivity.kt`: Summary card (total spent this month) and transaction feed.
   - `JournalActivity.kt`: Notes list captured via voice.

4. **Voice Overlay Dialog (`VoiceInteractionDialog.kt`):**
   - Fullscreen modal appearing during voice interaction showing live partial transcription in italic white text.
   - In-line confirmation prompt for destructive actions (e.g., "Complete task: Buy milk? [Confirm] [Cancel]").

5. **Application Lifecycle & Wiring (`DexterApp.kt`):**
   - Initializes database, starts `DexterVoiceService`, binds state updates to UI.

Ensure all layout XMLs use standard MaterialComponents themes and vector drawables with proper color tokens.
```

---

## 🛠️ Step 4: Verification & Android Studio Assembly

After AI Studio outputs the code:
1. Create a new Android Studio project with **Empty Views Activity** (Kotlin, Min SDK 21).
2. Copy the generated files into `app/src/main/java/` and `app/src/main/res/`.
3. Place your ARM64 binaries (`needle3`, model weights) into `app/src/main/assets/needle/`.
4. Run `./gradlew assembleDebug` to produce the APK.
5. Deploy to the Vivo V11 via `adb install -r app-debug.apk`.
