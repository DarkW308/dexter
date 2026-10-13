import urllib.request
import json
import time

def query(prompt):
    # Reset conversation state so each query is evaluated independently
    reset_req = urllib.request.Request("http://127.0.0.1:8080/reset", data=b"{}", headers={"Content-Type": "application/json"})
    urllib.request.urlopen(reset_req)
    
    t0 = time.time()
    req = urllib.request.Request(
        "http://127.0.0.1:8080/complete",
        data=json.dumps({"input": prompt}).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
    t1 = time.time()
    res["_measured_latency_s"] = round(t1 - t0, 3)
    return res

print("--- 1. SUCCESSFUL TASK CREATION ---")
print(json.dumps(query("Create a task to buy milk"), indent=2))

print("\n--- 2. AMBIGUOUS REQUEST ---")
print(json.dumps(query("Do the thing"), indent=2))

print("\n--- 3. EXPLICIT ID COMPLETION ---")
print(json.dumps(query("Mark task 4 complete"), indent=2))

print("\n--- 4. OUT OF DOMAIN ---")
print(json.dumps(query("What's the weather?"), indent=2))
