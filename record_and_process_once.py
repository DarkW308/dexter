"""One-shot push-to-talk recording and transcription runner."""

import sys
import time
from pathlib import Path

# Add dexter directory to path
BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

from voice_listener import VoiceListener

def main():
    listener = VoiceListener(record_seconds=8, auto_confirm=True)
    print("=" * 60)
    print("🎤 TRIGGERING ONE-SHOT VOICE TEST ON YOUR PHONE")
    print("=" * 60)
    sys.stdout.flush()
    if listener.record_audio():
        print("⏹️ Recording finished. Transcribing...")
        text = listener.transcribe()
        print(f"\n🗣️ RAW WHISPER TRANSCRIPTION: \"{text}\"")
        listener.process_voice_turn(text)
    else:
        print("❌ Audio recording failed.")

if __name__ == "__main__":
    main()
