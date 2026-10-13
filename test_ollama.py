import urllib.request, json
req = urllib.request.Request(
    'http://127.0.0.1:11434/api/chat',
    data=json.dumps({'model': 'qwen3.5:0.8b', 'messages': [{'role': 'user', 'content': 'hello'}], 'stream': False}).encode('utf-8'),
    headers={'Content-Type': 'application/json'}
)
print(urllib.request.urlopen(req).read().decode())
