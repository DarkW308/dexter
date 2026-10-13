import subprocess
import time
from pathlib import Path

wav = Path.home() / "test_speech.wav"
t0 = time.perf_counter()
res1 = subprocess.run(["espeak", "-w", str(wav), "Dexter voice assistant is fully operational and speaking."], check=True)
t1 = time.perf_counter()
print(f"Synthesized WAV in {t1-t0:.3f}s, size: {wav.stat().st_size} bytes")

res2 = subprocess.run(["play-audio", str(wav)], check=True)
t2 = time.perf_counter()
print(f"Played audio through phone speaker in {t2-t1:.3f}s!")
print("SUCCESS: Native Voice Speech is 100% working!")
