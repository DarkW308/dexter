import voice_listener

l = voice_listener.VoiceListener(auto_confirm=True)
text = "Remind me for a Google Skillshop lecture at 2 pm"
print(f"Testing input: '{text}'")
l.process_voice_turn(text)
