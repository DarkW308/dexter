import json
import time
import urllib.request

TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "create_task",
            "description": "Create a new pending task in the user's task list.",
            "parameters": {
                "type": "object",
                "properties": {
                    "title": {"type": "string", "description": "The title or description of the task to create"}
                },
                "required": ["title"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "complete_task",
            "description": "Mark an existing task as completed by its integer ID.",
            "parameters": {
                "type": "object",
                "properties": {
                    "task_id": {"type": "integer", "description": "The unique integer ID of the task to complete"}
                },
                "required": ["task_id"]
            }
        }
    }
]

TEST_CASES = [
    ("Case 1: Creation", "Create a task to buy milk"),
    ("Case 2: Explicit ID", "Mark task 4 complete"),
    ("Case 3: Reporting", "I already finished task 4"),
    ("Case 4: Ambiguity", "Do the thing"),
    ("Case 5: Out of Domain", "What's the weather?")
]

def test_chat_completions(name, user_prompt):
    payload = {
        "messages": [
            {"role": "user", "content": user_prompt}
        ],
        "tools": TOOLS,
        "tool_choice": "auto",
        "temperature": 0.0,
        "max_tokens": 64
    }
    
    req = urllib.request.Request(
        "http://127.0.0.1:8081/v1/chat/completions",
        data=json.dumps(payload).encode('utf-8'),
        headers={"Content-Type": "application/json"}
    )
    
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            data = json.loads(resp.read().decode('utf-8'))
        t1 = time.perf_counter()
        latency = t1 - t0
        
        choice = data.get("choices", [{}])[0]
        msg = choice.get("message", {})
        tool_calls = msg.get("tool_calls", [])
        content = msg.get("content", "")
        usage = data.get("usage", {})
        
        prompt_tokens = usage.get("prompt_tokens", 0)
        completion_tokens = usage.get("completion_tokens", 0)
        tps = completion_tokens / latency if latency > 0 else 0
        
        print(f"=== {name} ===")
        print(f"Query: '{user_prompt}'")
        print(f"Latency: {latency:.2f}s | Prompt tokens: {prompt_tokens} | Generated tokens: {completion_tokens} | Approx TPS: {tps:.1f}")
        print(f"Tool Calls: {json.dumps(tool_calls, indent=2)}")
        print(f"Content: {repr(content)}")
        print()
        return {
            "name": name,
            "query": user_prompt,
            "latency": latency,
            "prompt_tokens": prompt_tokens,
            "completion_tokens": completion_tokens,
            "tps": tps,
            "tool_calls": tool_calls,
            "content": content
        }
    except Exception as e:
        print(f"=== {name} FAILED ===")
        print(f"Error: {e}")
        return {"name": name, "query": user_prompt, "error": str(e)}

print("Starting FunctionGemma 270M Benchmark...")
results = []
for name, query in TEST_CASES:
    res = test_chat_completions(name, query)
    results.append(res)

with open("functiongemma_results.json", "w") as f:
    json.dump(results, f, indent=2)
print("Benchmark completed. Results saved to functiongemma_results.json")
