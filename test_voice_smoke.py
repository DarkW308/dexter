from voice_listener import VoiceListener

listener = VoiceListener()
print("--- TEST 1: PENDING TASKS ---")
listener.process_voice_turn("Show my unfinished tasks")

print("\n--- TEST 2: AMBIGUITY ---")
listener.process_voice_turn("Do the thing")

print("\n--- TEST 3: OUT OF DOMAIN ---")
listener.process_voice_turn("What is the weather?")
