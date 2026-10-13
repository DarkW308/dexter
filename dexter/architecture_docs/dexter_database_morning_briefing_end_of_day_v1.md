# Dexter Database — Morning Briefing / End-of-Day Summary v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**Storage ceiling:** 20 GB total Dexter data layer

---

## 1. Purpose

Dexter has two daily synthesis points:

```text
🌅 MORNING BRIEFING
→ Start the day

🌙 END-OF-DAY SUMMARY
→ Close the day
```

They are deliberately different.

### Morning Briefing answers:

> **"What do I need to know and do today?"**

### End-of-Day Summary answers:

> **"What happened today, what did I miss, and what should I carry into tomorrow?"**

The morning briefing prepares the user.

The end-of-day summary closes the day and prepares the next one.

---

# 2. Relationship to Other Subsystems

```text
Tasks
Time & Schedule
Goals
Projects
Daily Capture
Wellbeing
Finance
Relationships
Documents / Knowledge
Journal
        │
        ▼
   Structured data
        │
        ▼
Briefing / Summary Engine
        │
        ▼
      Laya
        │
        ▼
      Qwen
        │
        ▼
Morning Briefing / End-of-Day Summary
        │
        ▼
Notification / Voice / In-App
        │
        ▼
Logging / Audit
```

The briefing system is a **synthesis layer**, not a new source of truth.

---

# 3. Core Architecture Rule

The database remains authoritative.

The briefing engine:

```text
READS
→ existing data

SELECTS
→ relevant information

RANKS
→ importance

COMPRESSES
→ useful context

GENERATES
→ natural language

DELIVERS
→ briefing/summary
```

It must never silently modify source records merely because it generated a briefing.

---

# 4. Morning Briefing

## Purpose

The Morning Briefing is the user's **start-of-day command view**.

It should be short, actionable, and forward-looking.

Recommended order:

```text
1. Greeting
2. Short motivational line
3. Today's main focus
4. Critical / VVIP items
5. Schedule
6. Important tasks
7. Goals / projects
8. Required data gathering
9. Important relationships
10. Relevant wellbeing
11. Other important information
12. One clear next action
```

Not every section needs to appear every day.

If a section has nothing useful, omit it.

---

# 5. Morning Briefing Example

```text
Good morning.

Today's focus:
Finish the Dexter database architecture.

Critical:
• VVIP task due at 10:00 AM.

Schedule:
• 08:00 — Database work
• 10:00 — VVIP deadline

Data gathering:
• Morning wellbeing check is due.

Relationships:
• One important follow-up is due today.

Focus:
Finish the VVIP task before starting lower-priority work.

Let's get moving.
```

---

# 6. Motivational Line

The user requested a short motivational/anime-style feeling in the morning.

Rules:

- short;
- positive;
- relevant;
- not repetitive;
- not distracting;
- preferably original wording;
- avoid long copyrighted quotations.

Example style:

> "Start small. Move forward. The next step is enough."

The motivational line is optional.

If no suitable line is available, Dexter can simply give the briefing without one.

---

# 7. Morning Priority Rules

Priority should be approximately:

```text
VVIP / critical deadline
        ↓
High-priority task
        ↓
Scheduled important event
        ↓
Goal / project focus
        ↓
Required data gathering
        ↓
Important relationship follow-up
        ↓
Wellbeing
        ↓
Normal tasks
        ↓
Low-priority information
```

This is a ranking guideline, not a rigid universal ordering.

Attention rules remain authoritative for interruption priority.

---

# 8. Morning Briefing Data Window

Default:

```text
TODAY
+
near-term deadlines
+
relevant unresolved items from yesterday
```

Avoid loading the entire historical database.

Use only information needed for today's decisions.

---

# 9. Missed Events in Morning Briefing

Important missed events from the previous day may appear.

Example:

```text
Yesterday:
You missed the daily data-gathering event.

Today:
The morning capture is available now.
```

Do not shame the user.

Do not repeatedly repeat old misses.

Only surface a missed event when it is still relevant.

---

# 10. Morning Wellbeing

Only include wellbeing information that is useful for today's plan.

Examples:

```text
Sleep-related schedule constraint
Planned exercise
Morning check-in
Habit that needs attention
```

Do not dump raw measurements.

---

# 11. Morning Relationships

Include only relevant relationship items:

```text
Follow-up due today
Birthday/event
Important meeting
Pending response
Recently overdue follow-up
```

Do not show the entire relationship database.

---

# 12. Morning Finance

Finance is optional and contextual.

Examples:

```text
Important bill due today
Budget warning
Scheduled financial task
```

Do not include unnecessary financial detail in the daily briefing.

---

# 13. Morning Journal

Journal information should generally be excluded unless it has direct relevance.

Examples where it may be useful:

```text
User explicitly asked Dexter to remember something
Important reflection relevant to today's goal
Pending personal reflection
```

Do not turn the briefing into a diary recap.

---

# 14. Morning RAG Usage

RAG should not be the default source.

Primary source:

```text
SQLite structured data
```

Use RAG only when the briefing needs relevant unstructured information.

Example:

```text
Today's project requires a document reference
        ↓
Retrieve only the relevant document chunk
        ↓
Add compact context
```

Never perform a full document-library retrieval every morning.

---

# 15. End-of-Day Summary

## Purpose

The End-of-Day Summary is the user's **daily closing report**.

It should answer:

```text
What did I accomplish?
What did I miss?
What changed?
What matters tomorrow?
```

Recommended order:

```text
1. Short opening
2. Completed work
3. Important unfinished work
4. Missed events
5. Goal/project progress
6. Data-gathering status
7. Relevant wellbeing
8. Relationship follow-ups
9. Important financial items
10. Tomorrow's critical items
11. Short reflection
```

Not every section needs to appear every day.

---

# 16. End-of-Day Example

```text
Good work today.

Completed:
• Finished the logging/audit design.
• Completed two project tasks.

Unfinished:
• Database testing remains.

Missed:
• Evening data gathering was missed.

Progress:
Dexter database architecture is now at the Notifications stage.

Tomorrow:
• Finish briefing architecture.
• Test the VVIP alert flow.

Reflection:
You moved the project forward even though the day was not perfect.
```

---

# 17. Completion Detection

Completed work should come primarily from authoritative structured data:

```text
Task status
Project status
Goal progress
Capture status
Scheduled events
```

Do not infer completion solely from a journal entry or LLM-generated text.

---

# 18. Unfinished Work

Show unfinished work only when useful.

Prioritize:

```text
Overdue
VVIP
High priority
Due tomorrow
Blocking another task
Explicitly carried forward
```

Do not list every unfinished low-priority task.

---

# 19. Missed Events

End-of-Day should summarize meaningful misses.

Example:

```text
Missed:
• Evening data gathering
• One scheduled follow-up
```

Do not repeatedly display the same historical miss after it has been acknowledged or resolved.

---

# 20. Goal / Project Progress

Use structured information:

```text
Goal progress
Project progress
Completed milestones
Blocked tasks
Upcoming milestones
```

The summary should focus on meaningful movement rather than percentages for everything.

---

# 21. Wellbeing Summary

Keep it concise.

Possible content:

```text
Exercise completed
Habit completed/missed
Relevant screen-time trend
Sleep-related note
Self-reported wellbeing
```

Avoid turning the End-of-Day Summary into a health report.

---

# 22. Relationship Summary

Include only meaningful relationship events:

```text
Follow-up completed
Follow-up missed
Important interaction
Upcoming event
Response still pending
```

Do not expose the complete contact history.

---

# 23. Finance Summary

Include only actionable or meaningful items:

```text
Important spending event
Budget warning
Bill/payment due
Financial task completed
```

Avoid unnecessary numbers in the daily summary.

---

# 24. Journal Integration

The End-of-Day Summary may optionally offer:

```text
"Would you like to record a short reflection?"
```

It should not automatically create a journal entry.

If the user speaks a reflection:

```text
Voice
  ↓
Journal entry
```

The journal remains user-controlled.

---

# 25. Tomorrow Preparation

This is one of the most important functions.

At the end of the day Dexter should identify:

```text
Tomorrow's VVIP items
Tomorrow's deadlines
Important schedule events
Carry-forward tasks
Required data gathering
Important relationship follow-ups
```

It should prepare the context for tomorrow morning.

It must not automatically reschedule tasks unless an explicit rule or user instruction allows it.

---

# 26. Morning ↔ End-of-Day Loop

```text
             🌅 MORNING
                  │
                  ▼
          Plan / Understand
                  │
                  ▼
             DAY ACTIVITY
                  │
          ┌───────┼────────┐
          ▼       ▼        ▼
        Tasks   Capture  Events
          │       │        │
          └───────┼────────┘
                  ▼
             🌙 NIGHT
                  │
                  ▼
          Review / Summarize
                  │
                  ▼
        Prepare Tomorrow
                  │
                  ▼
             🌅 MORNING
```

This creates a natural daily cycle without creating duplicate data.

---

# 27. Briefing Generation Pipeline

```text
Scheduled briefing time
        ↓
Check whether briefing is enabled
        ↓
Collect relevant structured data
        ↓
Apply priority rules
        ↓
Remove duplicates
        ↓
Remove stale information
        ↓
Build compact context
        ↓
Laya prepares context/prompt
        ↓
Qwen generates wording
        ↓
Validate output
        ↓
Deliver
        ↓
Log result
```

---

# 28. Laya Responsibility

Laya is the local orchestration layer.

It should:

- gather the required data;
- enforce briefing rules;
- build the compact context;
- call Qwen;
- validate the response;
- handle fallback;
- send the result to the delivery layer.

---

# 29. Qwen Responsibility

Qwen should primarily:

- summarize;
- phrase;
- organize;
- provide natural-language transitions;
- generate the motivational line;
- produce a concise briefing.

Qwen should not:

- invent tasks;
- invent deadlines;
- invent completed work;
- modify database records;
- independently decide that an alarm should fire;
- become the source of truth.

---

# 30. Output Validation

Before delivery, Dexter should check:

```text
Does every mentioned task exist?
Does every deadline match the database?
Are completed tasks actually completed?
Are dates/times correct?
Did Qwen invent information?
Is the briefing too long?
Does it contain irrelevant sensitive information?
```

If validation fails:

```text
Retry with smaller/clearer context
```

If still unsuccessful:

```text
Use deterministic structured fallback
```

---

# 31. Deterministic Fallback

Dexter must still work without Qwen.

Example:

```text
GOOD MORNING

Today:
• 4 tasks
• 1 VVIP deadline
• 1 scheduled data-gathering event

Critical:
10:00 AM — VVIP task

Schedule:
08:00 — Database work
10:00 — Deadline
```

No hallucination is possible because the fallback uses structured records directly.

---

# 32. Briefing Length

Default target:

```text
Morning:
~30–90 seconds spoken

End-of-Day:
~30–120 seconds spoken
```

Longer output should occur only when there is genuinely more important information.

The briefing is not intended to become a daily lecture.

---

# 33. Voice Behavior

Morning:

```text
Wake/notification
    ↓
User starts briefing
    ↓
Dexter speaks
```

Optional future mode:

```text
Scheduled alarm
    ↓
Wake user
    ↓
Automatic morning briefing
```

End-of-Day:

```text
Scheduled notification
    ↓
User opens/starts briefing
    ↓
Dexter speaks
```

Automatic voice at night should remain user-configurable.

---

# 34. Delivery Relationship

The briefing engine does not directly manage Android alarms.

Instead:

```text
Briefing Scheduler
        ↓
Attention
        ↓
Notification / Voice
```

This preserves the existing architecture.

---

# 35. Missed Morning Briefing

If the user does not open the morning briefing:

```text
Scheduled
    ↓
Missed
    ↓
Do not repeatedly nag
```

At the next relevant interaction:

```text
"You missed your morning briefing. Want a quick version?"
```

The user can then request it.

---

# 36. Missed End-of-Day Summary

If the user misses the night summary:

```text
Do not repeatedly notify.
```

The next morning can optionally say:

```text
"You missed yesterday's end-of-day summary.
I can give you a quick recap before today's briefing."
```

This should be optional and low priority.

---

# 37. Duplicate Prevention

A briefing should not repeat the same information unnecessarily.

Example:

```text
VVIP task already announced by a critical alert
```

Morning briefing may still mention it because it is important, but should not reproduce the entire alert message.

Use:

```text
"VVIP deadline at 10:00 AM."
```

instead of repeating a long alert.

---

# 38. Privacy

Briefings must follow domain privacy rules.

Rules:

- local-only;
- no cloud generation;
- minimum relevant context;
- no unnecessary sensitive data;
- no full documents;
- no full journal;
- no full financial history;
- no full relationship database;
- no technical logs unless relevant to system health.

---

# 39. Storage

Do not store every generated prompt and intermediate context.

Store only:

```text
Briefing run metadata
Final delivered summary where useful
Status
Timestamp
```

Old briefing history may be cleaned according to the final retention policy.

---

# 40. Database Tables

The existing delivery layer contains:

```sql
briefing_runs
```

Recommended fields:

```sql
CREATE TABLE briefing_runs (
    id INTEGER PRIMARY KEY,

    briefing_type TEXT NOT NULL
        CHECK (briefing_type IN (
            'MORNING',
            'END_OF_DAY'
        )),

    scheduled_at INTEGER NOT NULL,

    started_at INTEGER,

    completed_at INTEGER,

    status TEXT NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN (
            'SCHEDULED',
            'GENERATED',
            'DELIVERED',
            'FAILED',
            'SKIPPED'
        )),

    summary TEXT,

    created_at INTEGER NOT NULL
);
```

The briefing record references source data rather than duplicating the entire domain database.

---

# 41. Briefing Configuration

User settings should control:

```text
Morning briefing enabled
Morning briefing time
End-of-day summary enabled
End-of-day summary time
Voice enabled
Motivational line enabled
Briefing length preference
Included domains
```

Defaults should be sensible but user-configurable.

---

# 42. Suggested Defaults

Because Dexter is a dedicated device:

```text
Morning briefing:
07:00–09:00 user-configurable

End-of-day summary:
20:00–23:00 user-configurable

Voice:
Enabled only if user chooses it

Motivational line:
Enabled

Briefing length:
Short

VVIP items:
Always included
```

These are defaults, not hard-coded times.

---

# 43. No Addiction / Engagement Optimization

Dexter must not optimize briefings for engagement.

Do not:

```text
create suspense
manufacture urgency
send unnecessary motivational messages
extend conversations unnecessarily
maximize app usage
```

The objective is:

```text
Useful information
→ clear decision
→ action
→ leave user alone
```

This is particularly important because the Wellbeing architecture already treats unhealthy digital behavior as something Dexter should help reduce rather than encourage.

---

# 44. Test Cases

### Morning

```text
No tasks
→ short briefing

Normal tasks
→ concise task list

VVIP task
→ prominently included

Missed capture yesterday
→ relevant notice

Important relationship follow-up
→ included

No relevant wellbeing data
→ omit wellbeing section

Qwen unavailable
→ deterministic fallback
```

### End-of-Day

```text
Everything completed
→ positive concise summary

Several unfinished tasks
→ prioritize only meaningful carry-forward items

Missed capture
→ mention once

Goal progress
→ summarize

No activity
→ simple summary

Qwen unavailable
→ deterministic fallback
```

### Failure

```text
Database unavailable
→ do not fabricate briefing

Qwen timeout
→ retry/fallback

Delivery failure
→ log failure

Device restarted
→ restore scheduled briefing

Briefing already delivered
→ do not duplicate
```

---

# 45. Critical Rules

```text
RULE 01
Morning Briefing means START OF DAY.

RULE 02
End-of-Day Summary means CLOSE OF DAY.

RULE 03
They are different experiences and must not be treated as the same briefing.

RULE 04
Morning answers: "What do I need to know and do today?"

RULE 05
End-of-Day answers: "What happened today and what should I carry into tomorrow?"

RULE 06
The database remains the source of truth.

RULE 07
Briefings are synthesis, not a new source of truth.

RULE 08
Do not create duplicate task/schedule/goal records from briefing generation.

RULE 09
Do not infer task completion solely from LLM output.

RULE 10
Do not invent deadlines, events, people, or accomplishments.

RULE 11
Use structured data first.

RULE 12
Use RAG only when unstructured information is actually relevant.

RULE 13
Do not search the entire document library for every briefing.

RULE 14
Do not include every database record.

RULE 15
Omit empty or irrelevant sections.

RULE 16
VVIP and critical deadlines receive strong visibility.

RULE 17
Morning briefings are forward-looking.

RULE 18
End-of-Day summaries are backward-looking plus tomorrow preparation.

RULE 19
Missed events should be surfaced without shaming or endless repetition.

RULE 20
Briefings must not create notification loops.

RULE 21
The motivational line is optional and short.

RULE 22
Avoid long copyrighted quotations.

RULE 23
The user controls briefing time and delivery preferences.

RULE 24
Voice delivery is optional.

RULE 25
Automatic voice delivery must remain configurable.

RULE 26
A missed briefing must not generate endless reminders.

RULE 27
Briefing generation must remain useful when Qwen is unavailable.

RULE 28
Deterministic fallback must use authoritative structured data.

RULE 29
Critical timing must not depend on Qwen.

RULE 30
Laya orchestrates briefing generation; Qwen generates language.

RULE 31
Qwen must not directly modify the database.

RULE 32
Briefing output should be validated before delivery.

RULE 33
Do not store large prompts or intermediate contexts unnecessarily.

RULE 34
Briefing history must remain storage-efficient.

RULE 35
Sensitive domain data should be included only when relevant.

RULE 36
Technical logs should not normally appear in user briefings.

RULE 37
Briefings should normally remain under two minutes.

RULE 38
The user should leave the briefing with a clear next action.

RULE 39
The End-of-Day Summary should prepare tomorrow without automatically changing the user's schedule.

RULE 40
Morning Briefing and End-of-Day Summary must integrate with Attention, Delivery, and Logging rather than creating parallel systems.

RULE 41
The subsystem must remain lightweight enough for the Vivo V11.

RULE 42
The subsystem must remain compatible with the 20 GB storage constraint.

RULE 43
No cloud service is required.

RULE 44
Dexter should optimize for usefulness, not engagement.

RULE 45
The briefing exists to help the user start and close the day—not to keep the user inside Dexter.
```

---

# 46. Final Architecture

```text
                    ┌─────────────────────┐
                    │    DEXTER DATA      │
                    │                     │
                    │ Tasks               │
                    │ Schedule            │
                    │ Goals               │
                    │ Projects            │
                    │ Capture             │
                    │ Wellbeing           │
                    │ Finance             │
                    │ Relationships       │
                    │ Journal             │
                    │ Documents           │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ BRIEFING ENGINE     │
                    │                     │
                    │ Select              │
                    │ Rank                │
                    │ Deduplicate         │
                    │ Compress             │
                    └──────────┬──────────┘
                               │
                         Laya / Context
                               │
                               ▼
                             Qwen
                               │
                         Validation
                               │
                 ┌─────────────┴─────────────┐
                 ▼                           ▼
          🌅 MORNING                   🌙 END-OF-DAY
          "Start day"                  "Close day"
                 │                           │
                 └─────────────┬─────────────┘
                               ▼
                     Notification / Voice
                               │
                               ▼
                              USER
                               │
                               ▼
                         Logging / Audit
```

---

# 47. Final Status

**MORNING BRIEFING / END-OF-DAY SUMMARY v1 — FINALIZED**

Dexter now has a complete daily cycle:

```text
🌅 START
Morning Briefing
        ↓
Plan / Focus
        ↓
DAY
Tasks / Schedule / Capture / Life
        ↓
🌙 CLOSE
End-of-Day Summary
        ↓
Review / Learn / Prepare
        ↓
Tomorrow
        ↓
🌅 START
```

The architecture deliberately separates:

```text
Morning Briefing
= preparation

End-of-Day Summary
= reflection + preparation for tomorrow

Notification
= delivery

Alarm
= time-critical delivery

Attention
= interruption decision

Schedule
= timing

Qwen
= language generation

Laya
= orchestration

SQLite
= source of truth

Logging/Audit
= accountability
```

This completes the daily briefing subsystem without creating another parallel scheduler, attention engine, or data store.
