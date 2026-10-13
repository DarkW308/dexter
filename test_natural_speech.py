import json
from needle_adapter import NeedleAdapter

adapter = NeedleAdapter()

queries = [
    "I just spent 500 bucks on groceries, record that",
    "Could you add pick up Alice from school to my list",
    "Save a note: discussion with Alex about project timeline",
    "What are my total expenses",
    "Turn off my alarms",
    "Let us start a new project called Redesign Website",
    "My new goal is to run a half marathon",
    "What is the weather outside?"
]

print("=" * 75)
print("TESTING NATURAL LANGUAGE AND COLLOQUIAL SPEECH RESOLUTION")
print("=" * 75)

for q in queries:
    res = adapter.interpret(q)
    print(f"INPUT:  '{q}'")
    print(f"RESULT: Intent={res.get('intent')} | Clarification={res.get('clarification')} | Params={res.get('parameters')} | Conf={res.get('confidence', 0.0):.2f}")
    print("-" * 75)
