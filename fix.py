with open('C:\\Users\\darkw\\Documents\\antigravity\\mysterious-kepler\\ollama_chat_phone.py', 'r') as f:
    code = f.read()

import re
code = re.sub(r'\"format\":\s*format_value,', '', code)
code = re.sub(r'payload\s*=\s*\{([^}]+)\}', r'payload = {\1}\n\n        if format_value is not None:\n            payload["format"] = format_value', code)

with open('C:\\Users\\darkw\\Documents\\antigravity\\mysterious-kepler\\ollama_chat_phone.py', 'w') as f:
    f.write(code)
