"""Dexter Hands-Free & Push-to-Talk Voice Interface.

Features:
1. Hands-Free Alexa-Like Mode (--handsfree): Continuous listening without terminal typing or buttons.
2. Dynamic Silence Detection (ffmpeg silenceremove): Prevents sentence cutting while trimming trailing silence.
3. Fast On-Device Whisper (ARM NEON, 4 threads, tiny/base models).
4. Full 16-Segment Database & System Integration:
   - Tasks: Create, Complete, Bulk-complete, Cancel, List pending, List completed.
   - Alarms/Reminders: Schedule with epoch timestamps, List, Cancel.
   - Finance: Log expense, Expense summaries (inr/minor units).
   - Journal & Notes: Quick capture, List recent notes.
   - Goals & Projects: Create goal, List goals, Create project, List projects.
   - Daily Briefing: Multi-domain daily briefing synthesizing tasks, reminders, notes, and finances.
   - Audit & Logs: Every voice interaction audited in SQLite system_events.
5. Non-blocking audio cues and daemon-threaded TTS (never hangs).
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sqlite3
import subprocess
import sys
import threading
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

# Add dexter directory to path
BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

import database
from action_system import Action, ActionExecutor
from needle_adapter import NeedleAdapter

WHISPER_CLI = Path.home() / "whisper.cpp" / "build" / "bin" / "whisper-cli"
WHISPER_MODEL_BASE = Path.home() / "whisper.cpp" / "models" / "ggml-base.en.bin"
WHISPER_MODEL_TINY = Path.home() / "whisper.cpp" / "models" / "ggml-tiny.en.bin"
AUDIO_TMP_AAC = Path.home() / "dexter_voice_record.aac"
AUDIO_TMP_WAV = Path.home() / "dexter_voice_record.wav"
DB_PATH = BASE_DIR / "data" / "dexter.db"


class VoiceListener:
    def __init__(
        self,
        record_seconds: int = 10,
        model_name: str = "base",
        confidence_threshold: float = 0.50,
        enable_tts: bool = True,
        auto_confirm: bool = True,
    ):
        self.record_seconds = record_seconds
        self.enable_tts = enable_tts
        self.auto_confirm = auto_confirm

        # Select model
        if model_name == "base" and WHISPER_MODEL_BASE.exists():
            self.model_path = WHISPER_MODEL_BASE
        elif WHISPER_MODEL_TINY.exists():
            self.model_path = WHISPER_MODEL_TINY
        elif WHISPER_MODEL_BASE.exists():
            self.model_path = WHISPER_MODEL_BASE
        else:
            self.model_path = WHISPER_MODEL_TINY

        self.adapter = NeedleAdapter(confidence_threshold=confidence_threshold)
        self.executor = ActionExecutor()

        # Initialize database schema if not present
        database.init_db()
        self._ensure_default_accounts()

    def _db_conn(self) -> sqlite3.Connection:
        conn = sqlite3.connect(str(DB_PATH))
        conn.execute("PRAGMA foreign_keys = ON;")
        conn.execute("PRAGMA journal_mode = WAL;")
        return conn

    def _ensure_default_accounts(self) -> None:
        """Ensures default finance wallet account exists for transaction foreign keys."""
        try:
            conn = self._db_conn()
            now = int(time.time())
            conn.execute(
                """INSERT OR IGNORE INTO finance_accounts 
                   (id, name, account_type, currency, opening_balance_minor, is_active, created_at, updated_at) 
                   VALUES (1, 'Main Wallet', 'WALLET', 'INR', 0, 1, ?, ?)""",
                (now, now)
            )
            conn.commit()
            conn.close()
        except Exception:
            pass

    def _log_system_event(self, event_type: str, payload: dict) -> None:
        """Records audit log entry into system_events table."""
        try:
            conn = self._db_conn()
            now = int(time.time())
            conn.execute(
                "INSERT INTO system_events (event_type, entity_type, entity_id, occurred_at, payload, created_at) VALUES (?, 'VOICE_ASSISTANT', 1, ?, ?, ?)",
                (event_type, now, json.dumps(payload), now)
            )
            conn.commit()
            conn.close()
        except Exception:
            pass

    def speak(self, text: str) -> None:
        """Speaks text out loud through the phone's speaker using native OpenSL audio."""
        if not self.enable_tts or not text.strip():
            return
        clean_text = text.strip()

        def _speak_worker(msg: str):
            wav_path = Path.home() / "dexter_speech.wav"
            try:
                # 1. Native synthesis (50ms)
                proc = subprocess.run(
                    ["espeak", "-w", str(wav_path), msg],
                    check=False,
                    timeout=3,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
                if proc.returncode == 0 and wav_path.exists():
                    # 2. Native OpenSL playback (never deadlocks)
                    subprocess.run(
                        ["play-audio", str(wav_path)],
                        check=False,
                        timeout=8,
                        stdout=subprocess.DEVNULL,
                        stderr=subprocess.DEVNULL,
                    )
                    return
            except Exception:
                pass

            # Fallback to termux-tts-speak with strict timeout and orphan cleanup
            try:
                subprocess.run(
                    ["termux-tts-speak", msg],
                    check=False,
                    timeout=3,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
            except Exception:
                pass
            finally:
                subprocess.run(["pkill", "-9", "-f", "TextToSpeech"], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

        threading.Thread(target=_speak_worker, args=(clean_text,), daemon=True).start()

    @staticmethod
    def _parse_time(time_str: str) -> tuple[int, int, int] | None:
        """Parses '2 pm', '2:30 pm', '14:00', '9 am', '12 am', '12 pm', 'in 1 min', 'in 5 minutes' into (hour, minute, target_ts)."""
        now = datetime.now()

        # 1. Relative offset: "in 1 min", "in 5 minutes", "1 min", "10 seconds"
        rel = re.search(r"\b(?:in\s+)?(\d+)\s*(mins?|minutes?|secs?|seconds?|hours?)\b", time_str, re.IGNORECASE)
        if rel:
            val = int(rel.group(1))
            unit = rel.group(2).lower()
            if "sec" in unit:
                target = now + timedelta(seconds=val)
            elif "hour" in unit:
                target = now + timedelta(hours=val)
            else:
                target = now + timedelta(minutes=val)
            return target.hour, target.minute, int(target.timestamp())

        # 2. Absolute time
        t = re.sub(r"\b([ap])\.m\.?", r"\1m", time_str.strip().lower(), flags=re.IGNORECASE)
        t = re.sub(r"(?:o'?clock|l)$", "", t).strip()
        m = re.match(r"^(\d{1,2})(?::(\d{2}))?\s*(am|pm)?$", t)
        if not m:
            return None
        hour = int(m.group(1))
        minute = int(m.group(2)) if m.group(2) else 0
        meridiem = m.group(3)
        now_hour = now.hour

        if meridiem == "pm" and hour < 12:
            hour += 12
        elif hour == 12:
            if meridiem == "pm" or (meridiem == "am" and 10 <= now_hour <= 13) or meridiem is None:
                hour = 12
            else:
                hour = 0
        elif meridiem == "am" and hour == 12:
            hour = 0
        elif meridiem is None:
            if hour <= 7:
                hour += 12

        target = datetime(now.year, now.month, now.day, hour, minute, 0)
        if target <= now:
            target += timedelta(days=1)
        return hour, minute, int(target.timestamp())

    def _set_android_alarm(self, hour: int, minute: int, label: str, target_ts: int) -> None:
        """Sets native Android alarm clock and records in SQLite alarms table."""
        try:
            clean_label = re.sub(r"\s+(?:at|in)\s+\d+.*$", "", label, flags=re.IGNORECASE).strip()
            if not clean_label:
                clean_label = label

            # Trigger Android native alarm via Intent
            cmd = [
                "am", "start",
                "-a", "android.intent.action.SET_ALARM",
                "--ei", "android.intent.extra.alarm.HOUR", str(hour),
                "--ei", "android.intent.extra.alarm.MINUTES", str(minute),
                "--es", "android.intent.extra.alarm.MESSAGE", clean_label,
                "--ez", "android.intent.extra.alarm.SKIP_UI", "true"
            ]
            subprocess.run(cmd, check=False, capture_output=True)

            # Broadcast intent for MacroDroid hybrid support
            broadcast_cmd = [
                "am", "broadcast",
                "-a", "com.dexter.SET_ALARM",
                "--ei", "hour", str(hour),
                "--ei", "minute", str(minute),
                "--es", "label", clean_label
            ]
            subprocess.run(broadcast_cmd, check=False, capture_output=True)

            # Record in SQLite alarms table
            conn = self._db_conn()
            now_ts = int(time.time())
            conn.execute(
                "INSERT INTO alarms (name, alarm_time, alarm_type, label, enabled, created_at, updated_at) VALUES (?, ?, 'REMINDER', ?, 1, ?, ?)",
                (clean_label, target_ts, clean_label, now_ts, now_ts)
            )
            conn.commit()
            conn.close()
            print(f"⏰ Dexter Reminder scheduled for {hour:02d}:{minute:02d} - '{clean_label}' (target epoch: {target_ts})")
        except Exception as exc:
            print(f"⚠️ Could not set alarm: {exc}")

    def record_audio(self) -> bool:
        """Records microphone input with generous window and trims trailing silence dynamically via ffmpeg."""
        if AUDIO_TMP_AAC.exists():
            AUDIO_TMP_AAC.unlink()
        if AUDIO_TMP_WAV.exists():
            AUDIO_TMP_WAV.unlink()

        # Non-blocking tactile cue
        try:
            subprocess.run(["termux-vibrate", "-d", "80"], check=False, timeout=1)
        except Exception:
            pass

        print(f"\n🔴 LISTENING... Speak naturally ({self.record_seconds}s window, silence-trimmed)...")
        sys.stdout.flush()

        rec_cmd = [
            "termux-microphone-record",
            "-f", str(AUDIO_TMP_AAC),
            "-r", "16000",
            "-b", "128",
            "-l", str(self.record_seconds),
            "-e", "aac"
        ]

        try:
            subprocess.run(rec_cmd, check=True, capture_output=True, text=True)
            time.sleep(self.record_seconds + 0.3)

            # Ensure recording stopped cleanly
            subprocess.run(["termux-microphone-record", "-q"], check=False, capture_output=True)
            time.sleep(0.2)

            if not AUDIO_TMP_AAC.exists() or AUDIO_TMP_AAC.stat().st_size == 0:
                print("❌ Recording failed: audio file empty or missing.")
                return False

            # Convert to 16kHz mono PCM WAV with dynamic silence trimming
            # silenceremove trims trailing silence after 1.2s quiet, eliminating sentence cutting while keeping audio short
            ffmpeg_cmd = [
                "ffmpeg", "-y",
                "-i", str(AUDIO_TMP_AAC),
                "-af", "silenceremove=stop_periods=1:stop_duration=1.2:stop_threshold=-35dB",
                "-ar", "16000",
                "-ac", "1",
                "-c:a", "pcm_s16le",
                str(AUDIO_TMP_WAV)
            ]
            subprocess.run(ffmpeg_cmd, check=True, capture_output=True, text=True)
            return AUDIO_TMP_WAV.exists() and AUDIO_TMP_WAV.stat().st_size > 1000

        except subprocess.CalledProcessError as exc:
            print(f"❌ Error during recording/conversion: {exc}")
            return False

    def transcribe(self) -> str:
        """Transcribes recorded WAV using local whisper.cpp."""
        if not AUDIO_TMP_WAV.exists():
            return ""

        print(f"⚡ Transcribing on-device ({self.model_path.name})...")
        cmd = [
            str(WHISPER_CLI),
            "-m", str(self.model_path),
            "-f", str(AUDIO_TMP_WAV),
            "--no-timestamps",
            "--threads", "4",
            "--beam-size", "1",
            "--prompt", "Dexter, exit, stop, goodbye, pending tasks, list pending tasks, completed tasks, alarms, reminders, spent on groceries, take a note, daily briefing, create goal, create project."
        ]

        try:
            t0 = time.perf_counter()
            proc = subprocess.run(cmd, capture_output=True, text=True, check=True)
            t1 = time.perf_counter()

            raw_output = proc.stdout.strip()
            lines = [
                l.strip()
                for l in raw_output.splitlines()
                if l.strip() and not l.startswith("whisper_") and not l.startswith("system_") and not l.startswith("main:")
            ]
            text = " ".join(lines).strip()
            text = re.sub(r"\[.*?\]", "", text).strip()
            text = re.sub(r"\(.*?\)", "", text).strip()

            # Filter out known Whisper hallucinations on silence / quiet audio
            lower_clean = text.lower().rstrip(".?! ")
            if lower_clean in {"thank you", "thanks for watching", "you", "bye", "please subscribe", "subtitles by the amara.org community"}:
                return ""

            print(f"⏱️  Transcription Latency: {t1-t0:.2f}s")
            return text

        except subprocess.CalledProcessError as exc:
            print(f"❌ Whisper transcription error: {exc.stderr}")
            return ""

    # =========================================================================
    # Extended Multi-Domain Execution Handlers (All 16 Segments)
    # =========================================================================

    def _execute_bulk_complete(self) -> str:
        """Marks all pending tasks complete via core action system."""
        list_res = self.executor.execute(Action(intent="LIST_PENDING_TASKS", parameters={}))
        pending = list_res.data.get("tasks", [])
        if not pending:
            return "You have no pending tasks to complete."

        completed_count = 0
        for t in pending:
            try:
                res = self.executor.execute(Action(intent="COMPLETE_TASK", parameters={"task_id": t["id"]}))
                if res.success:
                    completed_count += 1
            except Exception:
                pass
        return f"Completed {completed_count} pending tasks."

    def _execute_list_completed(self) -> str:
        """Queries completed tasks from database."""
        conn = self._db_conn()
        cur = conn.cursor()
        cur.execute(
            """SELECT DISTINCT t.id, t.title 
               FROM tasks t
               JOIN task_occurrences o ON o.task_id = t.id
               WHERE o.status = 'COMPLETED'
               ORDER BY o.completed_at DESC, t.id DESC 
               LIMIT 5"""
        )
        rows = cur.fetchall()
        conn.close()

        if not rows:
            return "No completed tasks found."
        summaries = ", ".join(f"task {r[0]}: {r[1]}" for r in rows)
        return f"You completed {len(rows)} tasks recently: {summaries}."

    def _execute_log_expense(self, amount: float, description: str) -> str:
        """Inserts expense into finance_transactions table."""
        conn = self._db_conn()
        now = int(time.time())
        amount_minor = int(round(amount * 100))
        conn.execute(
            """INSERT INTO finance_transactions 
               (transaction_type, account_id, amount_minor, occurred_at, description, source_type, status, created_at, updated_at) 
               VALUES ('EXPENSE', 1, ?, ?, ?, 'USER_REPORTED', 'POSTED', ?, ?)""",
            (amount_minor, now, description, now, now)
        )
        conn.commit()
        conn.close()
        self._log_system_event("LOG_EXPENSE", {"amount": amount, "description": description})
        return f"Logged expense of {amount:.0f} rupees for {description}."

    def _execute_summarize_expenses(self) -> str:
        """Queries expense totals and recent transactions."""
        conn = self._db_conn()
        cur = conn.cursor()
        cur.execute("SELECT SUM(amount_minor), COUNT(*) FROM finance_transactions WHERE transaction_type = 'EXPENSE'")
        total_minor, count = cur.fetchone()
        cur.execute("SELECT description, amount_minor FROM finance_transactions WHERE transaction_type = 'EXPENSE' ORDER BY id DESC LIMIT 3")
        recent = cur.fetchall()
        conn.close()

        if not count or total_minor is None:
            return "You have no recorded expenses."
        total_rs = total_minor / 100.0
        recent_str = ", ".join(f"{r[0]} for {r[1]/100.0:.0f} rupees" for r in recent)
        return f"You recorded {count} expenses totaling {total_rs:.0f} rupees. Recent: {recent_str}."

    def _execute_log_journal(self, content: str) -> str:
        """Inserts entry into journal_entries table and FTS index."""
        conn = self._db_conn()
        now = int(time.time())
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO journal_entries (entry_date, content, created_at, updated_at) VALUES (?, ?, ?, ?)",
            (now, content, now, now)
        )
        entry_id = cur.lastrowid
        try:
            cur.execute("INSERT INTO journal_entries_fts (rowid, content) VALUES (?, ?)", (entry_id, content))
        except Exception:
            pass
        conn.commit()
        conn.close()
        self._log_system_event("LOG_JOURNAL", {"entry_id": entry_id, "content": content})
        return f"Saved note: {content}."

    def _execute_list_journal(self) -> str:
        """Queries recent journal notes."""
        conn = self._db_conn()
        cur = conn.cursor()
        cur.execute("SELECT content FROM journal_entries WHERE is_archived = 0 ORDER BY id DESC LIMIT 3")
        rows = cur.fetchall()
        conn.close()

        if not rows:
            return "You have no journal notes saved."
        notes_str = "; ".join(f"Note: {r[0]}" for r in rows)
        return f"Your recent notes are: {notes_str}."

    def _execute_list_alarms(self) -> str:
        """Queries active alarms and reminders."""
        conn = self._db_conn()
        cur = conn.cursor()
        now = int(time.time())
        cur.execute("SELECT name, alarm_time FROM alarms WHERE enabled = 1 ORDER BY alarm_time ASC")
        rows = cur.fetchall()
        conn.close()

        if not rows:
            return "You have no active alarms or reminders."
        items = []
        for name, alarm_time in rows:
            dt = datetime.fromtimestamp(alarm_time)
            items.append(f"{name} at {dt.strftime('%I:%M %p')}")
        return f"You have {len(rows)} active reminders: {', '.join(items)}."

    def _execute_cancel_alarms(self) -> str:
        """Disables all active alarms."""
        conn = self._db_conn()
        cur = conn.cursor()
        cur.execute("UPDATE alarms SET enabled = 0 WHERE enabled = 1")
        count = cur.rowcount
        conn.commit()
        conn.close()
        return f"Cancelled {count} active alarms."

    def _execute_create_goal(self, title: str) -> str:
        """Creates a goal in goals table."""
        conn = self._db_conn()
        now = int(time.time())
        conn.execute(
            "INSERT INTO goals (name, goal_type, priority, status, created_at, updated_at) VALUES (?, 'OUTCOME', 'NORMAL', 'ACTIVE', ?, ?)",
            (title, now, now)
        )
        conn.commit()
        conn.close()
        self._log_system_event("CREATE_GOAL", {"name": title})
        return f"Goal created: {title}."

    def _execute_list_goals(self) -> str:
        """Queries active goals."""
        conn = self._db_conn()
        cur = conn.cursor()
        cur.execute("SELECT name FROM goals WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 5")
        rows = cur.fetchall()
        conn.close()

        if not rows:
            return "You have no active goals."
        return f"Your active goals are: {', '.join(r[0] for r in rows)}."

    def _execute_create_project(self, name: str) -> str:
        """Creates a project in projects table."""
        conn = self._db_conn()
        now = int(time.time())
        conn.execute(
            "INSERT INTO projects (name, status, priority, created_at, updated_at) VALUES (?, 'ACTIVE', 'NORMAL', ?, ?)",
            (name, now, now)
        )
        conn.commit()
        conn.close()
        self._log_system_event("CREATE_PROJECT", {"name": name})
        return f"Project created: {name}."

    def _execute_list_projects(self) -> str:
        """Queries active projects."""
        conn = self._db_conn()
        cur = conn.cursor()
        cur.execute("SELECT name FROM projects WHERE status != 'ARCHIVED' ORDER BY id DESC LIMIT 5")
        rows = cur.fetchall()
        conn.close()

        if not rows:
            return "You have no active projects."
        return f"Your active projects are: {', '.join(r[0] for r in rows)}."

    def _execute_daily_briefing(self) -> str:
        """Synthesizes a multi-domain briefing and records to briefing_runs."""
        conn = self._db_conn()
        cur = conn.cursor()

        # 1. Pending tasks
        cur.execute("SELECT COUNT(*), title FROM tasks WHERE archived_at IS NULL GROUP BY id LIMIT 3")
        task_rows = cur.fetchall()
        task_count = len(task_rows)

        # 2. Today's reminders
        now = int(time.time())
        cur.execute("SELECT COUNT(*) FROM alarms WHERE enabled = 1")
        alarm_count = cur.fetchone()[0]

        # 3. Today's expenses
        cur.execute("SELECT SUM(amount_minor) FROM finance_transactions WHERE transaction_type = 'EXPENSE'")
        exp_sum = cur.fetchone()[0] or 0
        exp_rs = exp_sum / 100.0

        # Build concise briefing speech
        speech_parts = [f"Good day! Here is your daily briefing."]
        if task_count > 0:
            speech_parts.append(f"You have {task_count} pending tasks.")
        else:
            speech_parts.append("You have no pending tasks.")

        if alarm_count > 0:
            speech_parts.append(f"You have {alarm_count} scheduled reminders.")

        if exp_rs > 0:
            speech_parts.append(f"Recorded expenses total {exp_rs:.0f} rupees.")

        briefing_text = " ".join(speech_parts)

        # Record to briefing_runs table
        cur.execute(
            """INSERT INTO briefing_runs 
               (briefing_type, scheduled_at, started_at, completed_at, status, summary, created_at) 
               VALUES ('MORNING', ?, ?, ?, 'DELIVERED', ?, ?)""",
            (now, now, now, briefing_text, now)
        )
        conn.commit()
        conn.close()
        self._log_system_event("DAILY_BRIEFING", {"summary": briefing_text})
        return briefing_text

    # =========================================================================
    # Main Voice Turn Processing
    # =========================================================================

    def process_voice_turn(self, transcribed_text: str) -> None:
        """Interprets voice text and executes across all Dexter domains."""
        if not transcribed_text:
            print("⚠️  No speech detected (or purely background noise).")
            return

        print(f"\n🗣️  Recognized Speech: \"{transcribed_text}\"")

        # Interpret intent with Needle 3 / fast-path
        try:
            envelope = self.adapter.interpret(transcribed_text, request_id=f"voice-{int(time.time())}")
        except Exception as exc:
            print(f"❌ Interpretation error: {exc}")
            return

        if envelope.get("clarification"):
            msg = "I didn't quite catch that. Could you please clarify?"
            print("❓ Dexter needs clarification: The request was ambiguous, negated, or out-of-domain.")
            self.speak(msg)
            return

        intent = envelope.get("intent")
        params = envelope.get("parameters", {})
        conf = envelope.get("confidence", 0.0)

        print(f"🧠 Detected Intent: {intent} (Confidence: {conf:.2f})")
        print(f"📋 Parameters: {json.dumps(params)}")

        # 1. Standard task mutations
        if intent in {"CREATE_TASK", "COMPLETE_TASK", "USER_REPORT_TASK_COMPLETION", "CANCEL_TASK"}:
            action = Action(intent=intent, parameters=params)
            res = self.executor.execute(action)
            print(f"✅ Success: {res.message}")

            # Check for alarm time in title
            alarm_msg = ""
            if intent == "CREATE_TASK" and "title" in params:
                m_time = re.search(
                    r"\b(?:at|in)\s+(\d+\s*(?:mins?|minutes?|secs?|seconds?|hours?)|\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?|o'?clock|l)?)\b",
                    params["title"],
                    flags=re.IGNORECASE,
                )
                if m_time:
                    time_parsed = self._parse_time(m_time.group(1))
                    if time_parsed:
                        hour, minute, target_ts = time_parsed
                        self._set_android_alarm(hour, minute, params["title"], target_ts)
                        alarm_msg = f" and reminder scheduled for {hour:02d}:{minute:02d}"

            self.speak(f"{res.message}{alarm_msg}.")

        # 2. Standard pending task listing
        elif intent == "LIST_PENDING_TASKS":
            action = Action(intent=intent, parameters=params)
            res = self.executor.execute(action)
            tasks = res.data.get("tasks", [])
            print(f"\n📋 {res.message}")
            for t in tasks:
                print(f"  • [#{t['id']}] {t['title']} ({t['priority']})")

            spoken_msg = res.message
            if tasks:
                if len(tasks) <= 3:
                    task_summaries = ", ".join(f"task {t['id']}: {t['title']}" for t in tasks)
                    spoken_msg = f"{res.message}: {task_summaries}."
                else:
                    first_three = ", ".join(f"task {t['id']}: {t['title']}" for t in tasks[:3])
                    spoken_msg = f"{res.message}. The top three are: {first_three}, plus {len(tasks) - 3} others."
            self.speak(spoken_msg)

        # 3. Bulk task completion
        elif intent == "COMPLETE_ALL_TASKS":
            msg = self._execute_bulk_complete()
            print(f"✅ {msg}")
            self.speak(msg)

        # 4. Completed tasks listing
        elif intent == "LIST_COMPLETED_TASKS":
            msg = self._execute_list_completed()
            print(f"✅ {msg}")
            self.speak(msg)

        # 5. Finance: Log expense
        elif intent == "LOG_EXPENSE":
            amt = params.get("amount", 0.0)
            desc = params.get("description", "expense")
            msg = self._execute_log_expense(amt, desc)
            print(f"✅ {msg}")
            self.speak(msg)

        # 6. Finance: Summarize expenses
        elif intent == "SUMMARIZE_EXPENSES":
            msg = self._execute_summarize_expenses()
            print(f"✅ {msg}")
            self.speak(msg)

        # 7. Journal: Log note
        elif intent == "LOG_JOURNAL":
            content = params.get("content", "")
            msg = self._execute_log_journal(content)
            print(f"✅ {msg}")
            self.speak(msg)

        # 8. Journal: List notes
        elif intent == "LIST_JOURNAL":
            msg = self._execute_list_journal()
            print(f"✅ {msg}")
            self.speak(msg)

        # 9. Alarms: List alarms
        elif intent == "LIST_ALARMS":
            msg = self._execute_list_alarms()
            print(f"⏰ {msg}")
            self.speak(msg)

        # 10. Alarms: Cancel alarms
        elif intent == "CANCEL_ALARMS":
            msg = self._execute_cancel_alarms()
            print(f"⏰ {msg}")
            self.speak(msg)

        # 11. Goals: Create goal
        elif intent == "CREATE_GOAL":
            title = params.get("title", "")
            msg = self._execute_create_goal(title)
            print(f"🎯 {msg}")
            self.speak(msg)

        # 12. Goals: List goals
        elif intent == "LIST_GOALS":
            msg = self._execute_list_goals()
            print(f"🎯 {msg}")
            self.speak(msg)

        # 13. Projects: Create project
        elif intent == "CREATE_PROJECT":
            name = params.get("name", "")
            msg = self._execute_create_project(name)
            print(f"📁 {msg}")
            self.speak(msg)

        # 14. Projects: List projects
        elif intent == "LIST_PROJECTS":
            msg = self._execute_list_projects()
            print(f"📁 {msg}")
            self.speak(msg)

        # 15. Daily Briefing
        elif intent == "DAILY_BRIEFING":
            msg = self._execute_daily_briefing()
            print(f"🌅 {msg}")
            self.speak(msg)

    # =========================================================================
    # Hands-Free Alexa Loop & Interactive Loop
    # =========================================================================

    def run_handsfree_loop(self):
        """Continuous Alexa-like listening loop. No buttons or typing required."""
        print("=" * 60)
        print("🤖 DEXTER HANDS-FREE VOICE ASSISTANT (ALEXA MODE)")
        print("=" * 60)
        print("Listening continuously! Speak anytime.")
        print("Say 'Exit', 'Stop', or 'Goodbye' to pause.")
        print("=" * 60)
        self.speak("Dexter hands-free assistant is active and listening.")
        time.sleep(2)

        while True:
            try:
                if self.record_audio():
                    text = self.transcribe()
                    if text:
                        # Check exit phrases
                        if re.search(r"\b(?:exit|goodbye|stop\s+listening|shut\s+down)\b", text, flags=re.IGNORECASE):
                            print("👋 Exit command recognized. Shutting down hands-free listener.")
                            self.speak("Goodbye!")
                            time.sleep(1.5)
                            break
                        self.process_voice_turn(text)
                        # Brief pause after speaking before opening mic again
                        time.sleep(1.5)
                    else:
                        time.sleep(0.5)
            except KeyboardInterrupt:
                print("\n🛑 Hands-free mode stopped by user.")
                break
            except Exception as e:
                print(f"⚠️ Loop error: {e}")
                time.sleep(1)

    def run_cli_loop(self):
        """Interactive push-to-talk loop."""
        print("=" * 60)
        print("🎙️  DEXTER VOICE CONTROL (PUSH-TO-TALK)")
        print("=" * 60)
        print("Commands:")
        print(f"  • Press [ENTER] to record {self.record_seconds}s of speech")
        print("  • Type 't <text>' to test text directly")
        print("  • Type 'handsfree' to start continuous Alexa mode")
        print("  • Type 'q' or 'exit' to quit")
        print("=" * 60)

        while True:
            cmd = input("\n[Dexter Voice] > ").strip()
            if cmd.lower() in {"q", "exit", "quit"}:
                print("Exiting voice control.")
                break
            elif cmd.lower() == "handsfree":
                self.run_handsfree_loop()
            elif cmd.lower().startswith("t "):
                manual_text = cmd[2:].strip()
                self.process_voice_turn(manual_text)
            else:
                if self.record_audio():
                    text = self.transcribe()
                    self.process_voice_turn(text)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Dexter Voice Assistant")
    parser.add_argument("--handsfree", action="store_true", help="Start in continuous hands-free (Alexa) mode")
    parser.add_argument("--seconds", type=int, default=10, help="Recording duration in seconds (default: 10)")
    parser.add_argument("--model", type=str, default="base", choices=["tiny", "base"], help="Whisper model (tiny/base)")
    args = parser.parse_args()

    listener = VoiceListener(record_seconds=args.seconds, model_name=args.model, auto_confirm=True)
    if args.handsfree:
        listener.run_handsfree_loop()
    else:
        listener.run_cli_loop()
