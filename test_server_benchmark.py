import urllib.request
import json
import time
import os
import subprocess

BASE_URL = "http://127.0.0.1:8080"

def get_needle_pid():
    try:
        out = subprocess.check_output(["pgrep", "-f", "./needle.*serve"]).decode().strip()
        return out.split()[0]
    except:
        return None

def get_rss_kb(pid):
    if not pid:
        return 0
    try:
        with open(f"/proc/{pid}/status") as f:
            for line in f:
                if line.startswith("VmRSS:"):
                    return int(line.split()[1])
    except:
        return 0

def reset_session():
    req = urllib.request.Request(f"{BASE_URL}/reset", data=b"", headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.read().decode()
    except Exception as e:
        return str(e)

def send_complete(user_input):
    payload = json.dumps({"input": user_input}).encode("utf-8")
    req = urllib.request.Request(f"{BASE_URL}/complete", data=payload, headers={"Content-Type": "application/json"}, method="POST")
    t0 = time.perf_counter()
    with urllib.request.urlopen(req, timeout=120) as resp:
        raw = resp.read().decode("utf-8")
    t1 = time.perf_counter()
    elapsed = t1 - t0
    return elapsed, json.loads(raw)

pid = get_needle_pid()
print(f"=== NEEDLE 3 SERVER-MODE BENCHMARK (PID: {pid}) ===")
print(f"Initial Memory RSS: {get_rss_kb(pid) / 1024:.2f} MB\n")

# 1. Repeatability test: 'Create a task to buy milk' x 3
print("--- TEST 1: REPEATABILITY ('Create a task to buy milk' x 3) ---")
for i in range(1, 4):
    reset_session()
    lat, res = send_complete("Create a task to buy milk")
    rss = get_rss_kb(pid) / 1024
    print(f"Run {i}: Latency = {lat:.3f}s | Server RSS = {rss:.2f} MB")
    print(f"Response: {json.dumps(res, indent=2)}\n")

# 2. Remaining 4 test cases
other_cases = [
    "Mark task 4 complete",
    "I already finished task 4",
    "Do the thing",
    "What's the weather?"
]

print("--- TEST 2: INDIVIDUAL BENCHMARK CASES ---")
for query in other_cases:
    reset_session()
    lat, res = send_complete(query)
    rss = get_rss_kb(pid) / 1024
    print(f"Input: {query!r}")
    print(f"Latency: {lat:.3f}s | Server RSS = {rss:.2f} MB")
    print(f"Response: {json.dumps(res, indent=2)}\n")
    print("-" * 50)
