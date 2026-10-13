# Dexter Android App — Feature Specification v1

**Status:** FINALIZED for MVP  
**Target:** Android 8+ (API 26+), Vivo V11 (MediaTek Helio P60, 6 GB RAM)  
**Core Identity:** A voice-first, fully offline personal operating system.  
**Primary Interface:** Voice ("Hey Dexter" / tap-to-talk)  
**Secondary Interface:** Minimal on-screen dashboard  

---

## 1. Core Voice Engine

### 1.1 — Voice Input

| Attribute | Specification |
| :--- | :--- |
| **Trigger** | **Hands-Free "Hey Dexter" (Alexa Mode - Primary MVP)**, or Volume-Down Double-Press, or Tap Voice Orb |
| **STT Engine** | Android native `SpeechRecognizer` (primary) + bundled `whisper.cpp` `tiny.en` (offline fallback) |
| **Data Entry** | **100% Natural Language**: User never types or knows SQL or code. Dexter's NLU auto-maps speech to DB entries. |
| **Recording** | Streaming `AudioRecord` with energy-threshold VAD. Auto-stops on silence. No fixed cutoff. |
| **Barge-in** | Dexter's TTS stops immediately when user speaks |
| **Hallucination Filter** | Strips: "thank you", "thanks for watching", "please subscribe", "you" from Whisper output |
| **Wake-word prefix** | "Hey Dexter", "Dexter", "OK Dexter" are stripped before intent routing |

### 1.2 — Voice Output

| Attribute | Specification |
| :--- | :--- |
| **TTS Engine** | Android native `TextToSpeech` API |
| **Voice** | Default system TTS voice (en-IN preferred, configurable) |
| **Latency** | < 100ms first audio from response text |
| **Concurrency** | TTS runs on a daemon thread, never blocks main thread |

### 1.3 — Intent Routing (Two-Tier)

**Tier 1 — Deterministic Fast-Path (~1–5 ms)**  
Kotlin regex + keyword maps for high-frequency common commands:
- "list tasks", "pending tasks", "show reminders", "daily briefing", "stop", "cancel", etc.
- Negation safety: "don't complete task 4", "do not mark that done".

**Tier 2 — Needle 3 Neural Engine (~1.3s warm)**  
- Triggered when Tier 1 returns no match.
- HTTP POST to `http://127.0.0.1:8081/complete` with user text + tool schema.
- Returns structured JSON: `{intent, parameters, confidence}`.
- Confidence < 0.30 → clarification request, no action.
- All parameter extraction done in Kotlin — model never invents IDs.

---

## 2. Task Management

### 2.1 — Create Task
- **Voice:** "Create a task to buy milk", "Add fix the leak to my tasks", "Task: call the dentist"
- **Output:** Spoken confirmation + task appears instantly in Task Feed.
- **Priority:** NORMAL by default. User can specify: "urgent task", "low priority task".
- **Fields stored:** `title`, `priority`, `created_at`, `status=PENDING`.

### 2.2 — List Pending Tasks
- **Voice:** "What are my pending tasks?", "Show my unfinished tasks", "What do I need to do?"
- **Output:** Spoken count + top 5 tasks by priority. Full list on screen.
- **Screen:** Task Feed shows all PENDING tasks, sorted by priority then creation date.

### 2.3 — Complete Task
- **Voice:** "Complete task 3", "Mark task 'Buy milk' done", "Finish task 4"
- **Safety:** Dexter always confirms: *"Mark 'Buy milk' complete. Confirm?"*
- **Output:** `updated_at` + `archived_at` set. Task moves to Completed section.

### 2.4 — Complete All Tasks
- **Voice:** "Mark all tasks complete", "Complete everything"
- **Safety:** Confirms with count: *"Mark all 7 pending tasks complete. Confirm?"*

### 2.5 — Cancel Task
- **Voice:** "Cancel task 4", "Remove the dentist task"
- **Safety:** Confirms before setting `archived_at`.

### 2.6 — List Completed Tasks
- **Voice:** "Show completed tasks", "What have I finished?"
- **Output:** Last 10 completed tasks, spoken summary of count.

---

## 3. Reminders & Alarms

### 3.1 — Set Reminder
- **Voice:** "Remind me to call mom at 5 PM", "Set an alarm for 7 AM", "Remind me in 30 minutes to take medicine"
- **Time parsing:** Absolute (`2 PM`, `14:30`) and relative (`in 30 minutes`, `in 2 hours`).
- **Storage:** `alarms` table with epoch `alarm_time`, `label`, `enabled=1`.
- **Delivery:** `AlarmManager.setExactAndAllowWhileIdle()` → `DexterAlarmReceiver` → heads-up notification + TTS voice alert + vibration.

### 3.2 — List Reminders
- **Voice:** "What reminders do I have?", "Show my alarms"
- **Output:** All `enabled=1` alarms, sorted by `alarm_time`. Spoken count + nearest alarm time.

### 3.3 — Cancel Reminders
- **Voice:** "Cancel all reminders", "Turn off my alarms"
- **Safety:** Confirms count before setting `enabled=0` on all active alarms.

---

## 4. Finance

### 4.1 — Log Expense
- **Voice:** "Spent 350 on lunch", "Paid 1,200 for electricity", "I spent 500 bucks on groceries"
- **Amount parsing:** Handles "bucks", "rupees", "₹", "thousand", "k" (e.g., "5k" = 5000).
- **Storage:** `finance_transactions` table: `amount_minor` (paise, integer), `description`, `type=DEBIT`, `account_id=1` (Main Wallet default), `category_id`.
- **Output:** *"Logged ₹350 for lunch."*

### 4.2 — Summarize Expenses
- **Voice:** "How much did I spend?", "What are my total expenses?", "Show me today's spending"
- **Output:** Total spend today + total this month. Spoken + displayed on Finance screen.
- **Calculation:** `SUM(amount_minor) WHERE type=DEBIT` — computed from transactions, never from a stored total field.

---

## 5. Journal & Notes

### 5.1 — Log Note
- **Voice:** "Take a note: meeting rescheduled to Monday", "Save this: project deadline pushed to Friday", "Journal entry: had a great day"
- **Storage:** `journal_entries` table: `content`, `source=VOICE`, `created_at`.
- **Output:** *"Note saved."*

### 5.2 — List Notes
- **Voice:** "Show my notes", "What did I journal today?", "Read my recent notes"
- **Output:** Last 5 entries, displayed on Journal screen. Spoken: count + most recent entry content (truncated to 100 chars).

---

## 6. Goals

### 6.1 — Create Goal
- **Voice:** "Set a goal to read 12 books this year", "My goal is to run a half marathon", "Create goal: learn Python"
- **Storage:** `goals` table: `name`, `goal_type=OUTCOME`, `priority=NORMAL`, `status=ACTIVE`.
- **Output:** *"Goal 'Read 12 books' created."*

### 6.2 — List Goals
- **Voice:** "Show my goals", "What are my goals?"
- **Output:** All `ACTIVE` goals. Spoken count + names. Full list on Goals screen.

---

## 7. Projects

### 7.1 — Create Project
- **Voice:** "Create a project called Home Automation", "Start project: website redesign"
- **Storage:** `projects` table: `name`, `status=PLANNED`, `priority=NORMAL`.

### 7.2 — List Projects
- **Voice:** "Show my projects", "What projects are active?"
- **Output:** All non-archived projects. Spoken count.

---

## 8. Daily Briefing

### 8.1 — Morning Briefing
- **Voice:** "Daily briefing", "What do I need to know today?", "Give me my morning summary"
- **Auto-trigger:** Optionally at a user-set morning time (e.g., 7:30 AM via AlarmManager).
- **Content synthesized:**
  1. Count and top priority pending tasks.
  2. Reminders due today.
  3. Most recent journal note.
  4. Total expenses this month.
  5. Active goals count.
- **Storage:** `briefing_runs` table records every briefing: `type=MORNING`, `summary` text, `created_at`.
- **Output:** Spoken full briefing (30–60 seconds) + summary card on Home screen.

---

## 9. System & Audit

### 9.1 — Voice Turn Logging
- Every voice turn (regardless of success) is logged to `system_events`:
  - `event_type = VOICE_TURN`
  - `payload = {text, intent, confidence, result}`
  - `occurred_at = epoch`

### 9.2 — Action Audit
- Every database write (CREATE, UPDATE, DELETE) is logged to `audit_logs`:
  - `table_name`, `record_id`, `action`, `old_value_json`, `new_value_json`, `occurred_at`.

### 9.3 — Service Status
- A persistent foreground notification shows Dexter service state:
  - 🟢 Listening  
  - 🔵 Processing  
  - 🔴 Needle offline (degraded, Tier 1 only)

---

## 10. Phase 2 Features (Out of MVP Scope)

| Feature | Reason Deferred |
| :--- | :--- |
| Wake-word detection ("Hey Dexter") | Requires DSP-level always-on audio — high battery impact without proper tuning |
| Relationships / Personal CRM | Needs phonetic contact matching + integration with Android ContactsContract |
| Wellbeing tracking | Sensor APIs + Health Connect integration — separate module |
| Documents / RAG search | SQLite FTS5 ready in schema, needs document ingestion pipeline UI |
| Recurring task series | `task_series` table exists, scheduling engine needs dedicated cron module |
| Multi-language STT | Hindi/Urdu Whisper models need separate download + storage |
| Cloud backup | Privacy-first design — local backup only in v1 |
| Android widget | Jetpack Glance widget for task count + next alarm |
