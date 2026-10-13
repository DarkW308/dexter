import subprocess
import time
import json
import os

os.chdir(os.path.expanduser('~/needle3-test'))

inputs = [
    "Create a task to buy milk",
    "Mark task 4 complete",
    "I already finished task 4",
    "Do the thing",
    "What's the weather?"
]

print("=== NEEDLE 3 ISOLATED BENCHMARK (5 CASES) ===")

for inp in inputs:
    print("\n" + "="*50)
    print(f"INPUT: {inp!r}")
    t0 = time.time()
    res = subprocess.run([
        "./needle",
        "--model", "needle3.cact",
        "--tools", "test_tools.json",
        "--tool-index", "test_tools.idx",
        "--prompt", inp,
        "--max", "100",
        "--threads", "4"
    ], capture_output=True, text=True)
    t1 = time.time()
    
    elapsed = t1 - t0
    print(f"LATENCY: {elapsed:.2f}s")
    print(f"RAW STDOUT: {res.stdout.strip()}")
    if res.stderr.strip():
        print(f"RAW STDERR: {res.stderr.strip()}")
