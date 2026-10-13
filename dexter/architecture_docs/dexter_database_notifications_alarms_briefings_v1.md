# Dexter Database — Notifications / Alarms / Briefings v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**AI:** Qwen 2.5 0.5B + Laya  
**Storage ceiling:** 20 GB total Dexter data layer

---

## 1. Purpose

Notifications / Alarms / Briefings is the **delivery layer** of Dexter.

It does not decide what is important by itself.

The architecture is:

```text
Domain / Schedule
        ↓
Attention Engine
        ↓
Notification / Alarm / Voice / Briefing
        ↓
User
```

The subsystem exists to make sure that important information reaches the user at the appropriate time and through an appropriate channel.

---

# 2. Existing Architecture It Extends

The previously finalized architecture already establishes:

```text
Schedule   → WHEN
Capture    → WHAT
Attention  → HOW
Domain     → WHAT IT MEANS
Logging    → WHAT HAPPENED
```

Daily Capture already sends events to Attention, which then handles notification, voice, and alarm delivery.

Therefore v1 must **not create a second attention engine**.

Notifications / Alarms / Briefings is the delivery and synthesis layer sitting after Attention.

---

# 3. Core Principle

> **Dexter should interrupt the user only when the value of the interruption justifies the interruption.**

A notification must have a reason.

Examples:

```text
VVIP deadline approaching
        → strong interruption

User-requested wake-up alarm
        → strong interruption

Routine low-priority task
        → normal notification

Morning briefing
        → scheduled briefing

Unimportant information
        → defer / digest / do not interrupt
```

This follows a well-established notification design principle: interruptions should be purposeful, understandable, recoverable, and controllable rather than simply maximizing the number of alerts.

---

# 4. Responsibility Boundaries

## Schedule

Owns:

- when something is scheduled;
- recurrence;
- date/time;
- timezone;
- scheduled occurrence.

## Attention

Owns:

- whether something deserves attention;
- priority;
- urgency;
- escalation decision;
- interruption decision.

## Notification / Alarm / Briefing

Owns:

- delivery channel;
- delivery timing once Attention has decided;
- notification lifecycle;
- alarm lifecycle;
- voice delivery;
- briefing generation;
- acknowledgement/snooze handling.

## Logging / Audit

Owns:

- what was delivered;
- what failed;
- what the user acknowledged;
- why an automated action occurred.

---

# 5. Delivery Channels

v1 supports:

```text
NOTIFICATION
VOICE
ALARM
BRIEFING
IN_APP
```

A single attention item may use more than one channel when justified.

Example:

```text
VVIP deadline
    ↓
Notification
    ↓
Voice
    ↓
Alarm if user explicitly requested wake-up
```

Do not automatically escalate every notification into every channel.

---

# 6. Notification Types

Recommended v1 types:

```text
REMINDER
DEADLINE
MISSED_EVENT
FOLLOW_UP
SYSTEM_WARNING
SYSTEM_ERROR
BRIEFING
INFORMATION
```

These describe the purpose of the delivery, not the importance.

Importance is handled separately.

---

# 7. Priority

Priority levels:

```text
LOW
NORMAL
HIGH
VVIP
```

### LOW

Examples:

```text
Optional reminder
Low-value information
Non-urgent follow-up
```

### NORMAL

Examples:

```text
Routine task reminder
Normal daily capture
Standard scheduled event
```

### HIGH

Examples:

```text
Important deadline
Important relationship follow-up
Important missed capture
```

### VVIP

Reserved for events where missing the event can have serious consequences for the user's objective.

Examples:

```text
Critical deadline
User-designated VVIP task
User-requested wake-up
Critical system failure
```

VVIP must be rare.

If everything is VVIP, VVIP has no meaning.

---

# 8. Attention Score

Attention should consider multiple factors.

Conceptually:

```text
Attention =
    priority
  + time urgency
  + deadline proximity
  + user preference
  + consequence of missing
  + context
  + repetition history
```

Do not expose the mathematical score to the user unless useful.

The score is an internal decision aid.

---

# 9. Deadline Escalation

For a critical task:

```text
Deadline:
04:00 AM

Current:
01:00 AM
```

Dexter may determine:

```text
3 hours remaining
        ↓
VVIP
        ↓
strong reminder
```

If the user has explicitly requested a wake-up:

```text
01:00 AM
    ↓
Schedule alarm for requested time
    ↓
Wake user
    ↓
Voice / notification
```

Dexter must not wake the user merely because a normal task is approaching unless the user has authorized that behavior.

---

# 10. Escalation Policy

Escalation should be controlled.

Example:

```text
Level 0
No interruption

Level 1
Normal notification

Level 2
Persistent/high-priority notification

Level 3
Voice reminder

Level 4
Alarm / wake-up
```

The Attention Engine chooses the highest justified level.

Do not automatically progress through all levels without a policy reason.

---

# 11. Snooze

User can snooze eligible reminders.

Common options:

```text
10 minutes
30 minutes
1 hour
Later today
Custom time
```

The actual list remains configurable.

Snooze should create a new scheduled delivery state rather than repeatedly firing the same notification.

---

# 12. Acknowledgement

Important deliveries should support:

```text
ACKNOWLEDGED
SNOOZED
DISMISSED
ACTIONED
MISSED
EXPIRED
FAILED
```

Example:

```text
Reminder
   ↓
Delivered
   ↓
User opens
   ↓
ACTIONED
```

or:

```text
Reminder
   ↓
Delivered
   ↓
Snoozed
   ↓
Delivered later
```

---

# 13. Deduplication

Dexter must avoid duplicate interruptions.

Example:

```text
Task deadline approaching
+
Daily briefing mentions same task
+
Project reminder mentions same task
```

These should not automatically become three separate interruptions.

The delivery layer should recognize related attention items and consolidate them where appropriate.

---

# 14. Notification Grouping

Related low/normal-priority items may be grouped.

Example:

```text
Dexter — 4 things for today

• 2 tasks
• 1 data capture
• 1 follow-up
```

This is preferable to four separate notifications when urgency is low.

VVIP events should remain individually visible.

---

# 15. Quiet Hours

Quiet hours are user preferences, not absolute suppression rules.

Example:

```text
Quiet hours:
11:00 PM → 7:00 AM
```

During quiet hours:

```text
LOW
→ defer

NORMAL
→ normally defer

HIGH
→ configurable

VVIP
→ may break quiet hours
```

A user-requested alarm may also break quiet hours.

The system must clearly distinguish:

```text
"Do not disturb"
```

from:

```text
"Do not deliver anything."
```

Critical events may still require delivery.

---

# 16. Anti-Spam Rules

Dexter must prevent notification fatigue.

Rules:

- no infinite reminder loops;
- no repeated identical notifications without state change;
- no excessive escalation;
- group related low-priority events;
- respect user snooze;
- respect acknowledgement;
- expire stale notifications;
- suppress notifications that are no longer relevant.

Example:

```text
Task completed
    ↓
Cancel its pending reminders
```

---

# 17. Stale Notification Rule

A notification must not remain actionable after its underlying state has changed.

Example:

```text
08:00
Task reminder scheduled

08:10
Task completed

08:11
Old reminder fires
```

This must be prevented where practical.

Before delivery, Dexter should validate that the underlying event is still relevant.

---

# 18. Missed Event Handling

For Daily Capture:

```text
Scheduled
   ↓
Capture window
   ↓
Completed
```

or:

```text
Scheduled
   ↓
Capture window expires
   ↓
MISSED
   ↓
Attention
   ↓
Missed-event notification
```

Daily Capture already defines that missed events must not create infinite reminders.

Notifications should therefore deliver the state change, not repeatedly nag the user.

---

# 19. Wake-Up Alarm

Wake-up alarms are a special class.

Use when:

- the user explicitly requests an alarm;
- an approved VVIP policy requires it;
- the user has configured Dexter to wake them for specific conditions.

Example:

> "Wake me at 4:30 AM and remind me about the VVIP deadline."

Dexter should create:

```text
Alarm
    time = 04:30
    reason = VVIP deadline
    delivery = alarm + voice
```

This is fundamentally different from a normal notification.

---

# 20. Android Alarm Strategy

For the target Vivo V11 running Android 8:

- use Android `AlarmManager` for time-critical alarms;
- use wake-up alarm types when the device must wake;
- use exact alarms only when exact timing is actually required;
- use inexact scheduling for non-critical reminders where possible;
- use `setAlarmClock()` for true user-facing alarm-clock behavior;
- use `setExactAndAllowWhileIdle()` only where precise delivery during idle is justified.

Android's current documentation explicitly recommends exact alarms only for genuinely user-facing, precise-time use cases, and notes their battery cost. `setAlarmClock()` is specifically designed for alarm-clock behavior and is treated as highly visible/critical by the OS. citeturn0search0turn0search8

Android 8 itself predates the Android 12+ exact-alarm permission model, but the implementation should still be designed so that future devices can handle exact-alarm permissions correctly. citeturn0search0turn0search3

---

# 21. Doze / Idle Behavior

Normal background work must not be relied upon for precise wake-up behavior.

For true time-critical alarms, use the appropriate AlarmManager mechanism.

Android documents that ordinary alarms can be deferred during Doze, while `setAlarmClock()` continues to fire normally and `setExactAndAllowWhileIdle()` can be used when an exact alarm must fire during idle. citeturn0search14

Because the Vivo V11 is dedicated to Dexter, battery policy should be optimized for reliability without using exact alarms unnecessarily.

---

# 22. Notification Channels

On Android 8+, notifications require notification channels.

Dexter should create a small fixed set of channels.

Recommended:

```text
Dexter — Normal
Dexter — Important
Dexter — Critical
Dexter — System
```

Do not create a channel for every task.

Channel importance should reflect broad delivery behavior; individual event priority remains Dexter's responsibility.

---

# 23. Future Android Compatibility

The architecture should remain compatible with newer Android versions.

On Android 12+:

- exact alarm permission handling must be implemented where applicable;
- the app must check whether exact alarms are permitted;
- the user should be guided to the appropriate system setting when necessary.

On Android 13+:

- notification permission behavior must be handled where applicable.

The current Android documentation states that `SCHEDULE_EXACT_ALARM` is user-controlled on modern Android and can be revoked, so Dexter must not assume it is permanently available. citeturn0search6turn0search9

---

# 24. Voice Delivery

Voice is an optional delivery channel.

Example:

```text
"Attention. Your VVIP task is due in three hours."
```

Voice should be used when:

- the user requested voice reminders;
- the event is sufficiently important;
- a normal notification is unlikely to be enough;
- the device is in an appropriate state.

Avoid speaking every normal notification.

---

# 25. Voice + Alarm

For a wake-up:

```text
Alarm
   ↓
Audio
   ↓
Dexter voice message
```

Example:

> "Good morning. Your VVIP deadline is in thirty minutes. Your first task is to finish the final data validation."

This is more useful than simply playing a generic alarm.

---

# 26. Briefings

Briefings are **scheduled summaries**, not ordinary notifications.

Two primary v1 briefings:

```text
MORNING
END_OF_DAY
```

Optional future:

```text
WEEKLY
PROJECT
FINANCE
WELLBEING
```

Do not implement all of these in v1.

---

# 27. Morning Briefing

Morning briefing should synthesize only relevant information.

Suggested order:

```text
1. Greeting
2. Motivational line
3. Today's critical items
4. Schedule
5. High-priority tasks
6. Important deadlines
7. Missed data gathering
8. Relevant wellbeing information
9. Important relationship follow-ups
10. Project/goal focus
11. One clear recommended focus
```

The briefing should be short.

---

# 28. Motivational Line

Dexter may provide a short motivational line in the morning.

The user previously requested an anime-style motivational feel.

v1 rule:

```text
Short
Relevant
Positive
Not repetitive
```

Do not turn the briefing into a long speech.

For copyrighted fictional dialogue, prefer short original lines inspired by the user's preferred tone rather than reproducing long quotations.

---

# 29. Morning Briefing Example

```text
Good morning.

Today's main objective:
Finish the Dexter database architecture.

Critical:
• VVIP task deadline at 10:00 AM.
• Finance data capture is missing.

Schedule:
• 08:00–09:30 Database work
• 10:00 Deadline

Attention:
• One missed capture from yesterday.

Relationships:
• Rahul follow-up is due today.

Focus:
Finish the VVIP task before starting lower-priority work.

Let's get moving.
```

---

# 30. End-of-Day Briefing

End-of-day should summarize:

```text
1. What was completed
2. What was missed
3. Important deadlines tomorrow
4. Data gathering status
5. Project progress
6. Wellbeing highlights
7. Relationship follow-ups
8. Journal prompt/opportunity
9. One short reflection
```

It should not dump raw database records.

---

# 31. Briefing Data Selection

The briefing service should query relevant domains:

```text
Tasks
Schedule
Attention
Goals
Projects
Daily Capture
Wellbeing
Finance
Relationships
Journal
```

The service then creates a compact context for Qwen.

Daily Capture already specifies that morning briefing can include previous-day capture status and that End-of-Day processing may combine Tasks, Schedule, Attention, Daily Capture, Finance, Wellbeing, Relationships, and Journal. citeturn3file2L367-L395

---

# 32. Briefing Should Not Become RAG

Briefings should primarily use structured database queries.

Use:

```text
SQLite queries
+
small relevant context
+
Qwen synthesis
```

Use RAG only when a briefing needs relevant unstructured knowledge.

Do not search the entire document library every morning.

---

# 33. Briefing Failure

If Qwen is unavailable:

```text
Structured fallback briefing
```

Example:

```text
Good morning.

You have:
• 3 tasks today
• 1 critical deadline
• 1 missed data capture

Critical deadline:
10:00 AM — Finish database validation.
```

Dexter must remain useful without the LLM.

---

# 34. Notification / Alarm Database

Recommended lightweight tables:

```sql
CREATE TABLE attention_deliveries (
    id INTEGER PRIMARY KEY,

    attention_id INTEGER NOT NULL,

    delivery_type TEXT NOT NULL
        CHECK (delivery_type IN (
            'NOTIFICATION',
            'VOICE',
            'ALARM',
            'BRIEFING',
            'IN_APP'
        )),

    priority TEXT NOT NULL
        CHECK (priority IN (
            'LOW',
            'NORMAL',
            'HIGH',
            'VVIP'
        )),

    scheduled_at INTEGER NOT NULL,

    delivered_at INTEGER,

    acknowledged_at INTEGER,

    snoozed_until INTEGER,

    status TEXT NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN (
            'SCHEDULED',
            'DELIVERED',
            'ACKNOWLEDGED',
            'SNOOZED',
            'DISMISSED',
            'ACTIONED',
            'MISSED',
            'EXPIRED',
            'FAILED',
            'CANCELLED'
        )),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

The `attention_id` refers to the existing Attention item rather than duplicating its reason, source, and domain data.

---

# 35. Briefing Records

Briefing history should be lightweight.

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

Do not permanently store every intermediate prompt/context used to generate a briefing.

---

# 36. Briefing Context

For each briefing:

```text
Query relevant domains
        ↓
Collect facts
        ↓
Remove duplicates
        ↓
Rank
        ↓
Compress
        ↓
Generate compact context
        ↓
Qwen
        ↓
Briefing
```

The full database must never be passed to Qwen.

---

# 37. Delivery Validation

Before sending an important delivery:

```text
Is source still valid?
Is event still active?
Has user already acted?
Has the task been completed?
Has deadline changed?
Has the reminder been cancelled?
Is quiet-hours policy applicable?
Is escalation still justified?
```

If no longer relevant:

```text
Cancel delivery
```

---

# 38. Notification Lifecycle

```text
CREATED
   ↓
SCHEDULED
   ↓
DELIVERED
   ↓
 ┌───────────────┬─────────────┬─────────────┐
 ▼               ▼             ▼             ▼
ACKNOWLEDGED   SNOOZED      ACTIONED      DISMISSED
                                   │
                                   ▼
                                COMPLETE
```

Alternative:

```text
SCHEDULED
   ↓
EXPIRED / MISSED / CANCELLED / FAILED
```

---

# 39. Alarm Lifecycle

```text
REQUESTED
   ↓
VALIDATED
   ↓
SCHEDULED
   ↓
TRIGGERED
   ↓
ACKNOWLEDGED / DISMISSED / ACTIONED
```

If scheduling fails:

```text
FAILED
   ↓
Log + notify user if appropriate
```

Dexter must never silently assume that a wake-up alarm exists when the OS rejected or lost the schedule.

---

# 40. Important User Safety Rule

Dexter must not repeatedly wake the user because of ordinary task anxiety.

Wake-up behavior must be:

```text
explicitly requested
OR
explicitly configured
OR
covered by a clearly defined VVIP policy
```

Normal deadlines should produce appropriate reminders, not uncontrolled sleep interruption.

---

# 41. No Infinite Escalation

Example:

```text
Notification
   ↓
Snooze
   ↓
Notification
   ↓
Snooze
   ↓
Notification
```

must have limits.

Use:

```text
maximum escalation count
maximum reminder count
expiry time
user acknowledgement
```

After the limit:

```text
MISSED / EXPIRED
```

The event remains visible in the appropriate task/attention history.

---

# 42. Storage Policy

Notifications can generate high event volume.

Therefore:

- delivery history should be compact;
- old low-value delivery records can be archived/removed according to cross-domain retention policy;
- do not store large notification payloads;
- do not store repeated generated voice audio unless explicitly required;
- briefing history should store the useful final result, not all generation artifacts.

---

# 43. Logging Integration

Every important delivery should generate a compact audit/system record.

Example:

```text
attention_delivery
attention_id=182
delivery=ALARM
result=DELIVERED
```

For important automated actions:

```text
trigger
→ decision
→ delivery
→ result
```

Sensitive content should not be copied into generic logs.

---

# 44. Privacy

Rules:

- local-only;
- no cloud notification service required for core operation;
- no remote briefing generation;
- no remote voice processing;
- no notification telemetry;
- no full private content in logs;
- only minimum relevant context reaches Qwen.

---

# 45. Performance Rules

Target:

```text
Vivo V11
Android 8
Low RAM
Limited storage
Offline
```

Rules:

1. Use SQLite.
2. Use Android AlarmManager for time-critical alarms.
3. Avoid exact alarms for ordinary reminders.
4. Keep notification records small.
5. Keep only a small number of scheduled alarms active when possible.
6. Generate briefings once per scheduled run.
7. Do not continuously run the LLM waiting for events.
8. Let the OS wake Dexter for scheduled alarms.
9. Query only relevant domain data.
10. Do not load the whole database into memory.
11. Do not store generated audio unnecessarily.
12. Use deterministic fallback behavior when Qwen is unavailable.

---

# 46. Reliability Strategy

Dexter should treat time-critical delivery differently from normal AI work.

```text
TIME-CRITICAL
→ OS alarm
→ deterministic trigger
→ minimal processing
→ delivery

NON-CRITICAL
→ notification scheduling
→ optional background processing

BRIEFING
→ scheduled run
→ gather data
→ generate
→ deliver
```

Do not make a critical wake-up dependent on Qwen being loaded.

---

# 47. AI Is Not the Alarm Clock

This is a critical architecture rule.

Wrong:

```text
Alarm time
   ↓
Start Qwen
   ↓
Ask Qwen what to do
   ↓
Maybe wake user
```

Correct:

```text
Alarm time
   ↓
OS alarm fires
   ↓
Deterministic Dexter handler
   ↓
Wake / notify user
   ↓
Optional Qwen-generated message
```

If Qwen fails, the alarm still works.

---

# 48. AI Is Not the Source of Truth

Qwen may:

- summarize;
- phrase;
- prioritize within application-provided context;
- generate briefing language.

Qwen must not be trusted to decide raw database writes or independently invent deadlines.

Application services and validation remain authoritative.

---

# 49. Test Matrix

### Notifications

```text
Create
Schedule
Deliver
Acknowledge
Dismiss
Snooze
Expire
Cancel
Deduplicate
Group
```

### Alarms

```text
Create
Schedule
Wake device
Trigger
Voice delivery
Acknowledge
Cancel
Reschedule
Failure recovery
```

### Attention

```text
Priority
Urgency
Escalation
Quiet hours
VVIP
Stale event
Completed task
Missed capture
```

### Briefings

```text
Morning
End-of-Day
Missing data
Critical deadline
Project context
Relationship follow-up
Wellbeing context
LLM unavailable
No relevant data
```

### Android

```text
Screen off
Device idle
Doze
Application not open
Application restarted
Device rebooted
Alarm survives reboot where supported/required
Notification channel disabled
Exact-alarm capability unavailable on newer Android
```

---

# 50. Implementation Order

```text
1. Finalize Attention interface
2. Create attention_deliveries
3. Implement notification service
4. Implement notification channels
5. Implement alarm service
6. Implement alarm persistence/recovery
7. Implement acknowledgement/snooze
8. Implement deduplication
9. Implement escalation limits
10. Integrate Daily Capture missed events
11. Integrate Tasks/deadlines
12. Integrate Goals/Projects context
13. Implement Morning Briefing
14. Implement End-of-Day Briefing
15. Add deterministic fallback briefing
16. Integrate Logging/Audit
17. Test Android 8 behavior
18. Test newer Android compatibility
19. Profile battery/storage/latency
20. Test on the actual Vivo V11
```

---

# 51. Explicitly Excluded from v1

```text
Cloud push notification dependency
Cloud-generated briefings
Continuous AI monitoring
Always-on LLM process
Complex notification recommendation model
Predictive notification timing
Large notification history
Multiple independent reminder engines
Smartwatch integration
Multi-device synchronization
Advanced weekly/monthly briefing system
Automatic wake-up without user authorization
Unlimited escalation
```

---

# 52. Critical Rules

```text
RULE 01
Schedule owns WHEN.

RULE 02
Attention owns WHETHER Dexter should interrupt.

RULE 03
Notification/Alarm/Briefing owns HOW the attention is delivered or synthesized.

RULE 04
Do not create a second attention engine.

RULE 05
Every important notification must have a reason.

RULE 06
VVIP must remain rare and meaningful.

RULE 07
Normal tasks must not automatically wake the user.

RULE 08
Wake-up alarms require explicit user authorization or a clearly configured VVIP policy.

RULE 09
Critical alarms must not depend on Qwen.

RULE 10
Qwen may generate wording but must not be the source of truth for scheduling.

RULE 11
Before delivery, validate that the underlying event is still relevant.

RULE 12
Completed/cancelled tasks must cancel stale deliveries.

RULE 13
Do not repeatedly notify the same unchanged event without a policy reason.

RULE 14
Escalation must have limits.

RULE 15
Snooze must not create infinite reminder loops.

RULE 16
Quiet hours should suppress ordinary interruptions but allow configured critical events.

RULE 17
Low-priority related items should be grouped where practical.

RULE 18
VVIP events should remain individually visible.

RULE 19
Missed Daily Capture events should generate a useful attention event, not endless reminders.

RULE 20
Notifications must remain useful even if Qwen is unavailable.

RULE 21
Briefings must summarize relevant information rather than dump database records.

RULE 22
Morning and End-of-Day briefings should use structured data first.

RULE 23
RAG should be used in briefings only when unstructured knowledge is genuinely relevant.

RULE 24
Generated voice audio should not be permanently stored by default.

RULE 25
Notification history must remain compact.

RULE 26
Sensitive data must not be copied into generic notification logs.

RULE 27
Time-critical delivery should use deterministic OS scheduling.

RULE 28
Exact alarms should only be used when precise delivery is justified.

RULE 29
Android idle/power behavior must be considered in alarm design.

RULE 30
The system must support failure/recovery when the application restarts.

RULE 31
The system must not silently assume an alarm was scheduled successfully.

RULE 32
The user must be able to acknowledge, dismiss, or snooze eligible reminders.

RULE 33
The user must be able to control notification/voice/alarm preferences.

RULE 34
Notification burden must remain controllable.

RULE 35
The complete subsystem must remain suitable for the Vivo V11 and 20 GB storage constraint.

RULE 36
No cloud service is required for core notifications, alarms, or briefings.

RULE 37
Every important automated delivery should be traceable through Logging/Audit.

RULE 38
The notification layer must not duplicate domain data.

RULE 39
Briefings are summaries, not another permanent source of truth.

RULE 40
Reliability is more important than AI sophistication for alarms.
```

---

# 53. Final Architecture

```text
                         DOMAIN DATA
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
                 SCHEDULE            EVENTS
                    │                   │
                    └─────────┬─────────┘
                              ▼
                       ATTENTION ENGINE
                              │
                   "Should Dexter interrupt?"
                              │
                              ▼
                    DELIVERY CONTROLLER
                              │
          ┌───────────┬───────┼───────────┬──────────┐
          ▼           ▼       ▼           ▼          ▼
    Notification    Voice   Alarm      In-App    Briefing
          │           │       │           │          │
          └───────────┴───────┴───────────┴──────────┘
                              │
                              ▼
                             USER
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
          Acknowledge / Snooze          Action
                │                           │
                └─────────────┬─────────────┘
                              ▼
                        Logging / Audit
```

---

# 54. Final Status

**NOTIFICATIONS / ALARMS / BRIEFINGS v1 — FINALIZED**

The subsystem is deliberately built around the previously finalized Attention architecture.

It provides:

- normal notifications;
- priority-based delivery;
- VVIP handling;
- deadline escalation;
- missed-event alerts;
- voice reminders;
- user-controlled wake-up alarms;
- snooze/acknowledgement;
- deduplication;
- notification grouping;
- quiet hours;
- bounded escalation;
- stale-event suppression;
- morning briefing;
- end-of-day briefing;
- deterministic fallback behavior;
- Android 8 alarm strategy;
- newer Android compatibility planning;
- offline/local-first operation;
- low-resource performance;
- 20 GB storage awareness.

The most important architectural decision is:

> **The OS wakes Dexter. Dexter does not depend on the AI to wake the user.**

Qwen/Laya enhances the message and briefing; deterministic application logic remains responsible for reliability.
