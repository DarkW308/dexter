"""Dexter Native Reminder Daemon (Option A).

Continuously monitors dexter.db for due alarms/reminders.
When target timestamp is reached:
1. Emits high-priority Android heads-up notification with sound & vibration via termux-notification.
2. Speaks reminder aloud through the phone's speaker via termux-tts-speak.
3. Automatically marks the alarm as executed (enabled=0) in SQLite.
"""

from __future__ import annotations

import os
import sys
import time
import sqlite3
import subprocess
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
DB_PATH = BASE_DIR / "data" / "dexter.db"


def fire_reminder(alarm_id: int, label: str) -> None:
    print(f"🔔 TRIGGERING REMINDER #{alarm_id}: '{label}'")
    sys.stdout.flush()

    # 1. High-priority Android notification with sound & vibration
    try:
        subprocess.run([
            "termux-notification",
            "-t", "Dexter Reminder",
            "-c", label,
            "--sound",
            "--priority", "max",
            "--vibrate", "500,500,500,500",
            "--id", str(alarm_id)
        ], check=False)
    except Exception as exc:
        print(f"Notification error: {exc}")

    # 2. Speak out loud through phone speaker via native OpenSL playback
    try:
        spoken_msg = f"Dexter reminder: {label}"
        wav_path = Path.home() / f"reminder_{alarm_id}.wav"
        subprocess.run(["espeak", "-w", str(wav_path), spoken_msg], check=False, timeout=3)
        if wav_path.exists():
            subprocess.run(["play-audio", str(wav_path)], check=False, timeout=8)
    except Exception as exc:
        print(f"TTS error: {exc}")


def run_loop(poll_interval: int = 3) -> None:
    print(f"🚀 Dexter Reminder Daemon active.")
    print(f"📂 Monitoring database: {DB_PATH} every {poll_interval}s...")
    sys.stdout.flush()

    while True:
        try:
            if not DB_PATH.exists():
                time.sleep(poll_interval)
                continue

            now_ts = int(time.time())
            conn = sqlite3.connect(str(DB_PATH), timeout=10.0)
            cursor = conn.cursor()

            # Find all due enabled reminders
            cursor.execute(
                "SELECT id, label, alarm_time FROM alarms WHERE enabled = 1 AND alarm_time <= ?",
                (now_ts,)
            )
            due = cursor.fetchall()

            for alarm_id, label, alarm_time in due:
                # Mark as disabled (executed) first to prevent duplicate firing
                cursor.execute(
                    "UPDATE alarms SET enabled = 0, updated_at = ? WHERE id = ?",
                    (now_ts, alarm_id)
                )
                conn.commit()
                fire_reminder(alarm_id, label or "Scheduled reminder")

            conn.close()

        except Exception as exc:
            print(f"Daemon error: {exc}")
            sys.stdout.flush()

        time.sleep(poll_interval)


if __name__ == "__main__":
    run_loop()
