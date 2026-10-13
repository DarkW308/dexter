import urllib.request
import json
import time

payload = {
    'model': 'hf.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF:Q4_K_M',
    'messages': [
        {'role': 'system', 'content': 'You are a function calling model. Respond only with JSON: {\"name\": \"create_task\", \"arguments\": {\"title\": \"...\"}} or {\"name\": \"complete_task\", \"arguments\": {\"task_id\": N}} or {} if no tool.'},
        {'role': 'user', 'content': 'Create a task to buy milk'}
    ],
    'stream': False,
    'options': {'temperature': 0.0, 'num_predict': 50}
}
t0 = time.time()
req = urllib.request.Request('http://127.0.0.1:11434/api/chat', data=json.dumps(payload).encode('utf-8'), headers={'Content-Type': 'application/json'})
with urllib.request.urlopen(req) as resp:
    res = json.loads(resp.read().decode())
t1 = time.time()
print(f'LATENCY: {t1-t0:.2f}s')
print(json.dumps(res, indent=2))
