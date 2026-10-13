import re
from datetime import datetime, time

def extract_reminder_title(text: str) -> str | None:
    patterns = (
        r"^\s*(?:please\s+)?(?:set\s+(?:a\s+)?reminder|remind\s+me)\s+(?:for|to|about)?\s*(.+?)\s+at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)\s*$",
        r"^\s*(?:please\s+)?(?:set\s+(?:a\s+)?reminder|remind\s+me)\s+at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)\s+(?:for|to|about)?\s*(.+?)\s*$",
    )
    for idx, p in enumerate(patterns):
        m = re.match(p, text, flags=re.IGNORECASE)
        if m:
            if idx == 1:
                time_str, subject = m.group(1).strip(), m.group(2).strip()
            else:
                subject, time_str = m.group(1).strip(), m.group(2).strip()
            subject = re.sub(r"^(?:a|an|the)\s+", "", subject, flags=re.IGNORECASE).strip()
            if subject and time_str:
                return f"{subject[0].upper() + subject[1:]} at {time_str.upper()}"
    return None

def parse_time_string(time_str: str) -> tuple[int, int] | None:
    """Parses '2 pm', '2:30 pm', '14:00', '9 am' into (hour, minute)."""
    t = time_str.strip().lower()
    m = re.match(r"^(\d{1,2})(?::(\d{2}))?\s*(am|pm)?$", t)
    if not m:
        return None
    hour = int(m.group(1))
    minute = int(m.group(2)) if m.group(2) else 0
    meridiem = m.group(3)
    
    if meridiem == "pm" and hour < 12:
        hour += 12
    elif meridiem == "am" and hour == 12:
        hour = 0
    return hour, minute

if __name__ == "__main__":
    queries = [
        "Remind me for a skillshop lecture at 2 pm",
        "Please remind me to call mom at 5:30 pm",
        "Set a reminder for doctor appointment at 11 am",
        "Remind me at 9 pm about reading a book",
    ]
    for q in queries:
        title = extract_reminder_title(q)
        print(f"Query: '{q}' -> Title: '{title}'")
        if title:
            # check time parse
            m_time = re.search(r"at\s+(\d{1,2}(?::\d{2})?\s*(?:AM|PM)?)", title, flags=re.IGNORECASE)
            if m_time:
                parsed = parse_time_string(m_time.group(1))
                print(f"   Parsed Alarm Time: {parsed[0]:02d}:{parsed[1]:02d}")
