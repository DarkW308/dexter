from needle_adapter import NeedleAdapter

def test():
    adapter = NeedleAdapter()
    test_queries = [
        "What do I have left to do today?",
        "Can you add buying milk to my list?",
        "I already took care of task 2",
        "Could you cancel task 5 for me?",
        "Please don't finish task 2",
        "Do the thing",
        "What is the weather outside?",
    ]

    print("=" * 65)
    print("NATURAL LANGUAGE COMPREHENSION TEST")
    print("=" * 65)
    for q in test_queries:
        res = adapter.interpret(q)
        intent = res.get("intent", "None")
        params = res.get("parameters", {})
        clarify = res.get("clarification", False)
        print(f"🗣️  Input: \"{q}\"")
        if clarify:
            print(f"   ↳ ❓ Handled as Clarification / Rejection (Safe)")
        else:
            print(f"   ↳ 🧠 Intent: {intent} | Params: {params}")
        print("-" * 65)

if __name__ == "__main__":
    test()
