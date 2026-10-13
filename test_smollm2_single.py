import urllib.request
import json
import time

payload = {
    'model': 'smollm2',
    'messages': [
        {'role': 'user', 'content': 'Create a task to buy milk'}
    ],
    'stream': False,
    'options': {'temperature': 0.0, 'num_predict': 20}
}
t0 = time.time()
req = urllib.request.Request('http://127.0.0.1:11434/api/chat', data=json.dumps(payload).encode('utf-8'), headers={'Content-Type': 'application/json'})
try:
    with urllib.request.urlopen(req, timeout=60) as resp:
        res = json.loads(resp.read().decode())
    t1 = time.time()
    print(f'LATENCY: {t1-t0:.2f}s')
    print('RESPONSE:', res.get('message', {}).get('content', ''))
except Exception as e:
    print('ERROR:', e)
