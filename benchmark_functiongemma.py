import urllib.request
import json
import time
import subprocess
import os

BASE_URL = "http://127.0.0.1:11434"
MODEL = "functiongemma"

TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "create_task",
            "description": "Create a new task item on the todo list",
            "parameters": {
                "type": "object",
                "properties": {
                    "title": {"type": "string", "description": "Title or description of the task"}
                },
                "required": ["title"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "complete_task",
            "description": "Mark an existing task as completed",
            "parameters": {
                "type": "object",
                "properties": {
                    "task_id": {"type": "integer", "description": "Numeric ID of the task"}
                },
                "required": ["task_id"]
            }
        }
    }
]

def get_ollama_pid():
    try:
        out = subprocess.check_output(["pgrep", "-f", "ollama serve"]).decode().strip()
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

def send_chat(user_input):
    payload = {
        "model": MODEL,
        "messages": [
            {
                "role": "user",
                "content": f"You are a function calling engine. Tools: create_task(title), complete_task(task_id). If user request matches a tool, output valid JSON: {{\"name\": \"create_task\", \"arguments\": {{\"title\": \"...\"}}}} or {{\"name\": \"complete_task\", \"arguments\": {{\"task_id\": N}}}}. Otherwise output empty JSON: {{}}.\n\nUser: {user_input}"
            }
        ],
        "stream": False,
        "options": {
            "temperature": 0.0,
            "num_predict": 50
        }
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(f"{BASE_URL}/api/chat", data=data, headers={"Content-Type": "application/json"}, method="POST")
    t0 = time.perf_counter()
    with urllib.request.urlopen(req, timeout=120) as resp:
        raw = resp.read().decode("utf-8")
    t1 = time.perf_counter()
    return (t1 - t0), json.loads(raw)

pid = get_ollama_pid()
print(f"=== CANDIDATE #3 BENCHMARK: FunctionGemma 270M (Ollama PID: {pid}) ===")
print(f"Initial Ollama RAM RSS: {get_rss_kb(pid) / 1024:.2f} MB\n")

# 1. Warm-up & Repeatability
print("--- TEST 1: REPEATABILITY ('Create a task to buy milk' x 3) ---")
for i in range(1, 4):
    lat, res = send_chat("Create a task to buy milk")
    rss = get_rss_kb(pid) / 1024
    content = res.get("message", {}).get("content", "").strip()
    print(f"Run {i}: Latency = {lat:.3f}s | Ollama RSS = {rss:.2f} MB")
    print(f"Content: {content!r}\n")

# 2. Individual benchmark cases
cases = [
    "Mark task 4 complete",
    "I already finished task 4",
    "Do the thing",
    "What's the weather?"
]

print("--- TEST 2: INDIVIDUAL BENCHMARK CASES ---")
for query in cases:
    lat, res = send_chat(query)
    rss = get_rss_kb(pid) / 1024
    content = res.get("message", {}).get("content", "").strip()
    print(f"Input: {query!r}")
    print(f"Latency: {lat:.3f}s | Ollama RSS = {rss:.2f} MB")
    print(f"Content: {content!r}")
    print("-" * 50)
