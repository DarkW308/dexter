# Dexter Database — Relationships v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**AI:** Qwen 2.5 0.5B + Laya  
**Primary external-data mechanism:** Daily Capture / Data Gathering

---

## 1. Purpose

Relationships is a lightweight **relationship memory and maintenance system**, not a traditional CRM.

Its purpose is to help Dexter remember people, understand the context of important relationships, remember important dates, and help the user maintain connections that may otherwise be neglected because of work, distraction, or compulsive use of other activities.

It supports both:

- personal relationships;
- professional relationships.

The system must help without becoming intrusive or turning every person into a task.

---

# 2. Core Problem

The intended use case is:

```text
Work becomes intense
        ↓
Attention becomes focused on work
        ↓
People and events outside work are forgotten
        ↓
Time passes without meaningful contact
        ↓
Social media / other distractions may consume free time
        ↓
Relationships receive less intentional attention
```

Dexter should help by providing **timely, contextual reminders and useful relationship memory**.

It should not repeatedly pressure the user.

---

# 3. Core Architecture

```text
                         RELATIONSHIPS
                               │
        ┌──────────────┬───────┼───────────┬──────────────┐
        ▼              ▼       ▼           ▼              ▼
      People       Categories  Profile   Interactions   Events
        │                                  │              │
        └──────────────────┬───────────────┴──────────────┘
                           ▼
                     Follow-ups
                           │
                           ▼
                  Attention / Schedule
                           │
                           ▼
                  Morning / EOD Briefing
                           │
                           ▼
                     Dexter Context
                           │
                     ┌─────┴─────┐
                     ▼           ▼
                   Laya         Qwen
```

---

# 4. People

A person is the primary relationship record.

Examples:

```text
Rahul
Mom
Amit
College Friend
Manager
Mentor
```

## SQLite

```sql
CREATE TABLE relationship_people (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,

    display_name TEXT,

    relationship_category_id INTEGER,

    importance INTEGER NOT NULL DEFAULT 3
        CHECK (importance BETWEEN 1 AND 5),

    description TEXT,

    contact_preference TEXT,

    is_active INTEGER NOT NULL DEFAULT 1
        CHECK (is_active IN (0,1)),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (relationship_category_id)
        REFERENCES relationship_categories(id)
        ON DELETE RESTRICT
);
```

### Rules

- `name` is required.
- `description` is optional.
- A person may be archived rather than deleted once historical interactions exist.
- A person may belong to one primary category in v1.
- Future versions may support multiple relationship labels if needed.

---

# 5. Relationship Categories

Default categories:

```text
FAMILY
FRIEND
BEST_FRIEND
COLLEAGUE
IMPORTANT_PERSON
MENTOR
ACQUAINTANCE
OTHER
```

Users can create custom categories.

## SQLite

```sql
CREATE TABLE relationship_categories (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL UNIQUE,

    description TEXT,

    is_system INTEGER NOT NULL DEFAULT 0
        CHECK (is_system IN (0,1)),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### Important

Category and importance are different.

Example:

```text
Category: Colleague
Importance: 5
```

A colleague can therefore be extremely important without being placed in a different category.

---

# 6. Relationship Profile / Description

The profile provides natural-language context that helps Qwen/Laya understand the relationship.

Example:

```text
Rahul is one of my closest friends.
He likes anime and gaming.
He is currently preparing for an exam.
He prefers evening calls.
His birthday is in August.
I should check on his exam preparation occasionally.
```

The description should support natural voice updates.

Example:

> "Remember that Rahul is preparing for his exam."

Dexter can update the relationship description or structured memory as appropriate.

---

# 7. Structured vs Free-Form Information

Use structured fields for information that Dexter needs to query reliably.

### Structured

```text
Name
Category
Importance
Contact preference
Birthday
Important dates
Last meaningful interaction
Desired contact frequency
Next follow-up
```

### Free-form

```text
Personality
Interests
Current life context
Important things to remember
Conversation context
Personal notes
```

Do not force every relationship detail into rigid database columns.

---

# 8. Desired Connection Frequency

Dexter needs a lightweight way to determine when a relationship may need attention.

```sql
CREATE TABLE relationship_connection_preferences (
    id INTEGER PRIMARY KEY,

    person_id INTEGER NOT NULL,

    frequency_type TEXT NOT NULL
        CHECK (frequency_type IN (
            'NONE',
            'DAILY',
            'WEEKLY',
            'BIWEEKLY',
            'MONTHLY',
            'QUARTERLY',
            'CUSTOM'
        )),

    frequency_days INTEGER,

    preferred_time INTEGER,

    enabled INTEGER NOT NULL DEFAULT 1
        CHECK (enabled IN (0,1)),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (person_id)
        REFERENCES relationship_people(id)
        ON DELETE CASCADE
);
```

### Rules

- `NONE` means Dexter should not automatically suggest routine contact.
- Frequency is a preference, not a deadline.
- Dexter must not treat a missed connection target as a failure.
- Important life events can override normal frequency when appropriate.

---

# 9. Interactions

Interactions represent meaningful contact.

Examples:

```text
Phone call
In-person meeting
Chat
Message
Email
Work conversation
Shared event
```

## SQLite

```sql
CREATE TABLE relationship_interactions (
    id INTEGER PRIMARY KEY,

    person_id INTEGER NOT NULL,

    interaction_type TEXT NOT NULL
        CHECK (interaction_type IN (
            'CALL',
            'IN_PERSON',
            'CHAT',
            'MESSAGE',
            'EMAIL',
            'WORK',
            'EVENT',
            'OTHER'
        )),

    occurred_at INTEGER NOT NULL,

    duration_seconds INTEGER,

    summary TEXT,

    quality INTEGER
        CHECK (quality IS NULL OR quality BETWEEN 1 AND 5),

    source_type TEXT NOT NULL
        CHECK (source_type IN (
            'USER_REPORTED',
            'IMPORTED',
            'DERIVED'
        )),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (person_id)
        REFERENCES relationship_people(id)
        ON DELETE RESTRICT
);
```

### Important rule

An interaction record does not mean Dexter automatically knows what happened.

If the user says:

> "I talked to Rahul for an hour."

Dexter can record it as user-reported.

Dexter must not invent interaction history.

---

# 10. Meaningful Interaction

Not every digital contact needs to count equally.

The user may say:

> "I just replied okay."

That does not necessarily represent a meaningful connection.

Dexter should therefore distinguish between:

```text
CONTACT
```

and:

```text
MEANINGFUL INTERACTION
```

For v1, this can be represented using the interaction summary and optional quality score rather than adding unnecessary complexity.

---

# 11. Important Dates and Events

Relationship-related dates include:

```text
Birthday
Anniversary
Exam
Interview
Important meeting
Graduation
Medical appointment
Travel
Personal event
Promise / follow-up
```

These should connect to the existing Schedule/Event architecture rather than creating a second calendar system.

Relationship records should reference existing schedule/event records where possible.

Conceptually:

```text
Person
  │
  └── Relationship Event
          │
          ▼
       Schedule
          │
          ▼
      Attention
```

---

# 12. Relationship Follow-ups

Follow-ups represent something the user wants to remember.

Examples:

```text
Check how Rahul's exam went
Ask Amit about his interview
Send birthday message
Call Mom this weekend
Follow up with mentor
```

Use the existing Task/Schedule architecture where the follow-up becomes an actionable task.

Relationship should store the context/reference, while Tasks remain the source of truth for actionable work.

---

# 13. Relationship Notes

Short notes may store information that helps future interaction.

Examples:

```text
Likes anime.
Preparing for an exam.
Recently changed jobs.
Prefers calls over texting.
Planning a trip.
Interested in photography.
```

These should be concise.

Dexter should avoid storing unnecessary sensitive information.

---

# 14. Relationship Memory

Relationship memory should prioritize information that improves future interaction.

Useful information:

```text
Interests
Preferences
Important life events
Current projects
Important dates
Conversation context
Promises
Follow-up topics
```

Avoid unnecessary collection of:

```text
Private credentials
Financial account information
Passwords
Sensitive secrets
Unnecessary personal data
```

---

# 15. Relationship Attention

Dexter can generate attention from:

```text
Important date approaching
Birthday today
Follow-up due
Long time since meaningful interaction
User-defined connection frequency
Important event
Previously promised action
```

Example:

```text
Last meaningful interaction:
21 days ago

Desired frequency:
14 days

Result:
Relationship may need attention
```

This should be a **soft recommendation**, not an automatic task failure.

---

# 16. Attention Example

Bad:

> "You have failed to contact Rahul for 7 days."

Preferred:

> "You haven't properly caught up with Rahul in about three weeks. You mentioned he's preparing for his exam. Want me to remind you this evening to check in?"

This preserves context without creating guilt.

---

# 17. Relationship Priority

Importance scale:

```text
1 = Low
2 = Minor
3 = Normal
4 = Important
5 = Very important
```

Importance can influence:

- reminder priority;
- briefing visibility;
- event attention;
- follow-up ranking.

It must not automatically determine emotional value.

---

# 18. Work / Colleague Relationships

Relationships support professional connections without becoming a full CRM.

Example:

```text
Amit
Category: Colleague
Importance: 4

Description:
Works with me on Project X.
Recently interviewed for another role.
Interested in football.
```

Dexter can then use relevant context when reminding the user:

> "You wanted to check how Amit's interview went."

---

# 19. Personal Relationships

Example:

```text
Rahul
Category: Best Friend
Importance: 5

Description:
Closest friend.
Likes anime and gaming.
Preparing for an exam.
Usually free in the evening.
```

Dexter can use this context for appropriate reminders and briefings.

---

# 20. Relationship Data Capture

Because Dexter is running on a dedicated device, it cannot automatically know what happened on the user's primary phone.

Therefore relationship information is captured through:

```text
Voice
Manual entry
Daily Capture
Scheduled prompts
Explicit Dexter updates
```

Example:

> "I met Rahul today."

Dexter can ask:

```text
How was the interaction?

Optional:
Anything important to remember?
```

The user does not have to fill a form every time.

---

# 21. Morning Capture

Morning capture may ask only for important relationship items when necessary.

Example:

```text
Today:
- Rahul's birthday
- Call Mom this evening
- Follow up with Amit
```

It should not ask:

> "Tell me about every person in your life."

Relationship capture must remain lightweight.

---

# 22. End-of-Day Relationship Capture

Optional end-of-day prompt:

> "Anything important from today's conversations that you'd like me to remember?"

Possible response:

> "I met Amit. He starts his new job next month."

Dexter can store the relevant relationship note or interaction.

---

# 23. Cross-Domain Connections

Relationships connect to:

```text
Tasks
Schedule
Goals
Projects
Alerts & Attention
Daily Capture
Journal
Documents
Briefings
```

Examples:

```text
Relationship → Task
"Send birthday message"

Relationship → Schedule
"Birthday on October 12"

Relationship → Project
"Work with Amit on Project X"

Relationship → Journal
"Reflection about friendship"

Relationship → Briefing
"Rahul's birthday is today"

Relationship → Attention
"Follow up with mentor"
```

---

# 24. Social Media / Addiction Context

Dexter should not assume that relationship neglect is caused by social media.

However, if Wellbeing records show:

```text
High distracting behavior
+
Relationship follow-ups repeatedly missed
```

Dexter may identify this as a possible pattern.

It must phrase this cautiously:

> "I've noticed that several relationship follow-ups were missed during periods when your distracting activity was higher. Would you like me to help create a small evening connection routine?"

Not:

> "Social media is ruining your relationships."

Patterns are observations, not proven causes.

---

# 25. Relationship Attention Limits

To prevent notification fatigue:

```text
No infinite reminders
No repeated nagging
No guilt-based messaging
No automatic messaging to people
No contacting people without user approval
No assumption that silence means a problem
```

Dexter suggests; the user decides.

---

# 26. Privacy

Relationships are highly sensitive.

Rules:

- local storage only;
- no cloud dependency;
- no remote AI;
- no automatic social-media scraping;
- no automatic contact scraping in v1;
- no automatic messages;
- no sharing relationship data;
- minimum necessary context to Qwen/Laya;
- sensitive relationship information excluded from generic logs.

---

# 27. Performance Rules

Target device:

```text
Vivo V11
Android 8
Dedicated Dexter device
```

Rules:

1. Use INTEGER primary keys.
2. Use INTEGER timestamps.
3. Keep relationship tables normalized.
4. Keep descriptions reasonably small.
5. Avoid storing giant conversation histories.
6. Do not load all relationships into Qwen context.
7. Query only relevant people.
8. Use indexes for common lookups.
9. Keep attention calculations lightweight.
10. Profile before adding caching.

---

# 28. Indexes

```sql
CREATE INDEX idx_relationship_people_category
ON relationship_people(relationship_category_id);

CREATE INDEX idx_relationship_people_importance
ON relationship_people(importance);

CREATE INDEX idx_relationship_interactions_person_time
ON relationship_interactions(person_id, occurred_at);

CREATE INDEX idx_relationship_interactions_time
ON relationship_interactions(occurred_at);

CREATE INDEX idx_relationship_connection_person
ON relationship_connection_preferences(person_id);

CREATE INDEX idx_relationship_connection_enabled
ON relationship_connection_preferences(enabled);
```

---

# 29. What Relationships v1 Does NOT Include

```text
Automatic social-media monitoring
Automatic contact synchronization
Automatic messaging
Automatic calling
Social-media scraping
Full CRM pipeline
Sales CRM functionality
Relationship sentiment surveillance
Facial recognition
Location tracking
Private-message scraping
Cloud relationship database
AI-generated messages sent without approval
Complex social graph analytics
```

These may be future capabilities only if they can be implemented safely and privately.

---

# 30. Critical Relationship Rules

```text
RULE 01
Relationships is a relationship memory and maintenance system, not a traditional CRM.

RULE 02
People are the primary relationship records.

RULE 03
Categories describe relationship type; importance describes priority.

RULE 04
A person may be archived rather than deleted when historical records exist.

RULE 05
Free-form descriptions are allowed for contextual information.

RULE 06
Structured fields are used for information that must be queried reliably.

RULE 07
Interaction history must represent actual user-reported/imported events.

RULE 08
Dexter must never invent an interaction.

RULE 09
Relationship events should reuse the existing Schedule/Event architecture.

RULE 10
Actionable follow-ups should reuse the existing Task architecture.

RULE 11
Connection frequency is a preference, not a strict deadline.

RULE 12
Missing a connection target is not automatically treated as failure.

RULE 13
Importance may influence attention priority but does not determine emotional value.

RULE 14
Dexter should prefer contextual reminders over generic reminders.

RULE 15
Relationship reminders must not become guilt-based.

RULE 16
Dexter must not repeatedly nag the user.

RULE 17
Dexter must never contact another person without explicit user approval.

RULE 18
Dexter must not automatically scrape social media in v1.

RULE 19
Dexter must not automatically read private messages in v1.

RULE 20
Relationship data remains local and offline.

RULE 21
Sensitive relationship information must not enter generic logs.

RULE 22
Qwen/Laya receive only relevant relationship context.

RULE 23
Dexter should not assume relationship problems from lack of interaction.

RULE 24
Correlations between distraction and relationship neglect must be presented as observations, not facts about causation.

RULE 25
Daily Capture is the primary mechanism for information Dexter cannot observe automatically.

RULE 26
Morning capture should prioritize important relationship events and follow-ups.

RULE 27
End-of-day capture may ask for important relationship information worth remembering.

RULE 28
The system must support user correction of relationship records.

RULE 29
The system must support archiving people without destroying useful history.

RULE 30
The design must remain lightweight enough for the target Vivo V11.
```

---

# 31. Final Relationship Architecture

```text
                         RELATIONSHIPS
                               │
                         ┌─────┴─────┐
                         ▼           ▼
                       PEOPLE     CATEGORIES
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
          PROFILE    INTERACTIONS  EVENTS
              │          │          │
              └──────────┼──────────┘
                         ▼
                    FOLLOW-UPS
                         │
                  ┌──────┴──────┐
                  ▼             ▼
                TASKS        SCHEDULE
                  │             │
                  └──────┬──────┘
                         ▼
                     ATTENTION
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
       MORNING BRIEFING          EOD SUMMARY
             │                       │
             └───────────┬───────────┘
                         ▼
                    DEXTER CONTEXT
                         │
                    ┌────┴────┐
                    ▼         ▼
                  LAYA       QWEN
```

---

# 32. Final Status

**RELATIONSHIPS v1 — FINALIZED**

The subsystem provides:

- categorized people;
- family/friend/colleague/best-friend/important-person support;
- natural-language relationship descriptions;
- importance levels;
- desired connection frequency;
- interaction history;
- important events;
- follow-ups;
- contextual reminders;
- Daily Capture integration;
- Schedule/Task integration;
- Morning/EOD briefing integration;
- privacy protection;
- lightweight SQLite implementation;
- support for identifying possible distraction/relationship patterns without diagnosing or judging the user.

The architecture is intentionally simple enough for implementation by a normal software engineer while providing Qwen/Laya with enough structured and contextual information to make Dexter useful for maintaining personal and professional relationships.
