# Dexter — Privacy-Focused Offline Personal AI Assistant

Dexter is a voice-first, offline personal operating system written 100% in Kotlin with Jetpack Compose. Built following clean architecture, deterministic intent verification, and an embedded SQLite storage engine with WAL mode and foreign keys enabled.

## Features

- **Voice Control Hub**: Custom animated Compose Canvas Voice Orb with 5 visual states (`IDLE`, `LISTENING`, `PROCESSING`, `SPEAKING`, `ERROR`).
- **Hands-Free Voice & Natural Language**: Fast-path NLU parsing with negation safety, wake-word stripping ("Hey Dexter", "Ok Dexter"), and offline Text-to-Speech (TTS) with real-time barge-in support.
- **Task Management**: Create, list pending, filter completed, prioritize (Critical, High, Normal, Low), and complete/cancel tasks by voice or touch.
- **Expense & Finance Tracking**: Log debits/credits via voice (e.g., *"Spent 350 on lunch"*, *"Paid 1200 for electricity"*), calculate monthly and daily spending summaries, and review transaction histories.
- **Voice Journal & Notes**: Capture thoughts, reflections, and ideas on the go with zero friction.
- **Reminders & Alarms**: Set relative and absolute reminders, query active alarms, and cancel alarms safely.
- **Goals & Projects**: Set outcome targets and track initiatives.
- **Daily Briefing**: Voice-synthesized morning summary combining pending tasks, next reminders, today's spending, and active goals.
- **Transparent Audit Trail**: Every mutation and system trace is deterministically logged into `audit_logs` and `action_traces`.

## Architecture

- **Language**: Kotlin 2.2 with Coroutines & StateFlow.
- **UI**: Jetpack Compose with Material Design 3 Dark Theme tokens (`#1A1A2E` background, `#4FC3F7` electric blue, `#00E5FF` cyan glow).
- **Database**: Android SQLite (`SQLiteOpenHelper`) with WAL mode (`PRAGMA journal_mode = WAL`) and foreign key constraints enabled.
- **Execution Boundary**: Deterministic Action Executor preventing raw SQL execution and enforcing confirmation prompts for destructive actions.
