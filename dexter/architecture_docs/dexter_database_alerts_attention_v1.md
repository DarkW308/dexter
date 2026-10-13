# Dexter Database Architecture — Alerts & Attention v1

**Status:** FINALIZED  
**Database:** SQLite  
**Architecture:** Offline-first, local-only, privacy-first  
**Target hardware:** Low-RAM / older Android device  
**AI:** Qwen 2.5 0.5B + Laya

## 1. Purpose

The Alerts & Attention subsystem is Dexter's central attention layer. It decides whether something requires the user's attention, when it should happen, and whether Dexter should use a notification, voice, or alarm.

It does not own task, schedule, finance, wellbeing, relationship, goal, or data-gathering information. Those domains remain the source of truth.

## 2. Core Principle

> An attention event represents **why Dexter wants the user's attention**. An attention delivery represents **how Dexter attempted to get it**.

Example:

```text
TASK
"VVIP task"
    ↓
ATTENTION EVENT
"VVIP task deadline approaching"
    ↓
DELIVERIES
01:00 Notification
02:45 Voice
03:30 Alarm
```

## 3. Architecture

```text
DOMAIN DATABASE
      │
      ├── TASKS
      ├── SCHEDULE
      └── DATA EVENTS
              │
              ▼
       ATTENTION EVENT
              │
       ATTENTION ENGINE
              │
       ┌──────┼──────┐
       ▼      ▼      ▼
 Notification Voice  Alarm
       │      │      │
       └──────┼──────┘
              ▼
     ATTENTION DELIVERY
              │
              ▼
        USER RESPONSE
```

Future domains such as Finance, Wellbeing, Relationships, Goals, Projects, Documents, Journal, and Data Gathering can create attention events without changing the core Attention schema.

## 4. Attention Decision Model

Dexter should not alert merely because something exists.

The Attention Engine evaluates:

```text
Importance
+ Urgency
+ Deadline proximity
+ Estimated work
+ Available time
+ Consequence
+ Current schedule/context
+ Previous deliveries
+ User response
+ User policy
```

Then chooses:

```text
NONE
NOTIFICATION
VOICE
ALARM
```

Ordinary decisions should be deterministic. Qwen/Laya may assist with ambiguous judgement, but basic attention decisions must not depend on an LLM.

## 5. Importance

Allowed values:

```text
LOW
NORMAL
HIGH
CRITICAL
```

Importance answers: **How much does this matter?**

## 6. Urgency

Allowed values:

```text
LOW
NORMAL
HIGH
CRITICAL
```

Urgency answers: **How quickly does action need to happen?**

Importance and urgency remain separate. Urgency can increase as a deadline approaches.

## 7. Deadline Pressure

Dexter should consider:

```text
time remaining
estimated task duration
available schedule time
current progress
importance
consequence
```

Example:

```text
Current time:   01:00
Deadline:       04:00
Remaining:      3 hours
Estimated work: 2 hours

Result: feasible but time-sensitive
```

If estimated work is 5 hours, the deadline is already at risk and attention should escalate earlier.

## 8. Attention Levels

### Level 0 — NONE

No interruption.

### Level 1 — NOTIFICATION

Normal passive reminder.

### Level 2 — VOICE

Dexter actively speaks to the user.

### Level 3 — ALARM

Strong interruption. Reserved for explicit wake-up alarms, explicitly configured critical deadlines, and other policy-approved critical events.

Alarms should be rare.

## 9. Database Tables

Required v1 tables:

```text
attention_events
attention_deliveries
alarms
```

## 10. `attention_events`

```sql
CREATE TABLE attention_events (
    id INTEGER PRIMARY KEY,
    source_type TEXT NOT NULL,
    source_id INTEGER NOT NULL,
    title TEXT NOT NULL,

    importance TEXT NOT NULL DEFAULT 'NORMAL'
        CHECK (importance IN ('LOW','NORMAL','HIGH','CRITICAL')),

    urgency TEXT NOT NULL DEFAULT 'NORMAL'
        CHECK (urgency IN ('LOW','NORMAL','HIGH','CRITICAL')),

    due_at INTEGER,

    state TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (state IN (
            'PENDING','ACKNOWLEDGED','SNOOZED',
            'DISMISSED','EXPIRED','COMPLETED'
        )),

    next_evaluation_at INTEGER,
    expires_at INTEGER,
    acknowledged_at INTEGER,
    completed_at INTEGER,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### Field meanings

- `source_type` — domain that created the attention event.
- `source_id` — source record identifier.
- `title` — short human-readable reason.
- `importance` — how much the event matters.
- `urgency` — how quickly action is required.
- `due_at` — relevant deadline/time.
- `state` — attention lifecycle.
- `next_evaluation_at` — next time the Attention Engine should evaluate it.
- `expires_at` — when it is no longer actionable.
- `acknowledged_at` — when the user acknowledged it.
- `completed_at` — when the objective was completed.

## 11. Source References

Use:

```text
source_type
source_id
```

Examples:

```text
TASK + 421
SCHEDULE_EVENT + 83
DATA_EVENT + 17
FINANCE + 82
RELATIONSHIP + 14
```

SQLite cannot enforce a polymorphic foreign key across multiple source tables. The responsible domain service must therefore validate the source before creating an attention event.

Do not add separate nullable columns such as `task_id`, `schedule_event_id`, `finance_id`, etc.

## 12. No Stored Attention Score

Do not store an `attention_score`.

It is derived from:

```text
importance
urgency
deadline proximity
estimated duration
available time
consequence
delivery history
current context
user policy
```

## 13. `attention_deliveries`

```sql
CREATE TABLE attention_deliveries (
    id INTEGER PRIMARY KEY,
    attention_event_id INTEGER NOT NULL,

    delivery_type TEXT NOT NULL
        CHECK (delivery_type IN ('NOTIFICATION','VOICE','ALARM')),

    scheduled_at INTEGER NOT NULL,
    delivered_at INTEGER,

    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','DELIVERED','FAILED','CANCELLED')),

    acknowledged_at INTEGER,
    created_at INTEGER NOT NULL,

    FOREIGN KEY (attention_event_id)
        REFERENCES attention_events(id)
        ON DELETE CASCADE
);
```

This records every attempt to get the user's attention.

Example:

```text
Attention Event #1001

01:00  NOTIFICATION  DELIVERED
02:45  VOICE         DELIVERED
03:30  ALARM         PENDING
```

## 14. Delivery Types

```text
NOTIFICATION
VOICE
ALARM
```

`VOICE` here means Dexter speaking to the user as an attention mechanism. It does **not** represent the user's voice-control interface.

## 15. Delivery Status

```text
PENDING
DELIVERED
FAILED
CANCELLED
```

A failed voice delivery may allow the Attention Engine to fall back to another appropriate delivery method.

## 16. Acknowledgement

Acknowledgement primarily belongs to the attention event.

```text
PENDING
  ↓
notification delivered
  ↓
voice delivered
  ↓
user says "Okay Dexter"
  ↓
ACKNOWLEDGED
```

Acknowledgement does not mean completion.

```text
"Got it."                  → ACKNOWLEDGED
"Remind me in 30 minutes." → SNOOZED
"Done."                    → COMPLETED
```

## 17. Snooze

Snoozing must not modify the underlying task, deadline, or source event.

Example:

```text
Task deadline: 04:00
02:00 reminder
User: "Snooze for 30 minutes"

Attention:
SNOOZED
next_evaluation_at = 02:30
```

At 02:30:

```text
SNOOZED → PENDING → evaluate again
```

## 18. Attention State Machine

```text
                    ┌─────────────┐
                    │   PENDING   │
                    └──────┬──────┘
                           │
             ┌─────────────┼──────────────┐
             ▼             ▼              ▼
       ACKNOWLEDGED      SNOOZED       DISMISSED
             │             │
             │             ▼
             │          PENDING
             │
             ▼
         COMPLETED

PENDING ───────────────► EXPIRED
```

## 19. Escalation

Typical progression:

```text
NONE
 ↓
NOTIFICATION
 ↓
VOICE
 ↓
ALARM
```

Not every event follows every level.

Example:

```text
LOW       → Notification only
NORMAL    → Notification
HIGH      → Notification → Voice
CRITICAL  → Notification → Voice → Alarm
```

Actual escalation depends on deadline, importance, urgency, context, delivery history, user response, quiet period, and policy.

## 20. VVIP Deadline Example

```text
Current time: 01:00
Task: VVIP
Deadline: 04:00
Estimated work: 2 hours
Status: Not started
```

Potential lifecycle:

```text
01:00  No unnecessary interruption
02:00  Notification
02:45  Voice if still at risk
03:30  Critical escalation if still incomplete
04:00  Deadline reached
```

If estimated work is 5 hours while only 3 hours remain, the deadline is already at risk and attention should escalate earlier.

Dexter should not blindly wait until the final hour.

## 21. Quiet Periods

Schedule owns periods such as:

```text
23:00–06:30
Sleep
```

Attention consumes that context.

Example:

```text
LOW       → queue
NORMAL    → queue
HIGH      → policy evaluation
CRITICAL  → escalation evaluation
ALARM     → only when policy permits
```

Do not create a separate `quiet_hours` table in v1.

## 22. `alarms`

Wake-up alarms are scheduled mechanisms, not ordinary attention events.

```sql
CREATE TABLE alarms (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    alarm_time INTEGER NOT NULL,
    recurrence_rule TEXT,

    enabled INTEGER NOT NULL DEFAULT 1
        CHECK (enabled IN (0,1)),

    alarm_type TEXT NOT NULL DEFAULT 'WAKE'
        CHECK (alarm_type IN ('WAKE','REMINDER','CUSTOM')),

    label TEXT,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Example:

```text
name: Weekday Wake Up
alarm_time: 06:30
recurrence_rule: weekdays
alarm_type: WAKE
```

When it fires:

```text
Alarm → Attention Engine → ALARM delivery → Dexter speaks/rings
```

## 23. Morning Briefing

The morning briefing is generated from current state rather than stored as a duplicate copy.

```text
06:30
 ↓
Morning Briefing Service
 ↓
Schedule
Tasks
Recurring tasks
Deadlines
Pending attention
Missed data gathering
Goals
Yesterday's summary
 ↓
Qwen/Laya
 ↓
Dexter voice briefing
```

Example:

```text
Good morning.

Today:
09:00 — Work
10:30 — VVIP task
14:00 — Meeting
18:00 — Exercise

Important:
• VVIP task deadline — 16:00
• 2 tasks carried forward
• Yesterday's data check-in was missed

Focus:
Finish the VVIP task before lunch.

Motivation:
Short personalized motivational line.
```

Do not store the entire generated briefing in v1.

## 24. Motivation

Motivation is presentation content, not a core Attention database entity.

Possible sources:

- user-approved motivational material;
- short quotes;
- original motivational lines;
- anime-inspired motivational wording;
- goal-specific encouragement.

Avoid storing long copyrighted anime dialogue.

No dedicated motivation table is required in v1.

## 25. Voice Control Boundary

Alerts & Attention's `VOICE` delivery is not the same as voice control.

The overall Dexter software architecture should be:

```text
USER
 │
 │ voice
 ▼
Speech-to-Text
 │
 ▼
Intent / Command Parser
 │
 ▼
Qwen 2.5 0.5B + Laya
 │
 ▼
Action / Tool Router
 │
 ├── Task Service
 ├── Schedule Service
 ├── Attention Service
 ├── Finance Service
 ├── Wellbeing Service
 ├── Relationship Service
 └── Database Services
          │
          ▼
        SQLite
```

Qwen/Laya must not directly execute arbitrary SQL.

## 26. Safe Database Command Boundary

Example:

```text
"Mark the database architecture task as complete."
        ↓
Intent: COMPLETE_TASK
        ↓
Action Router
        ↓
Validate task
        ↓
Task Service
        ↓
SQLite transaction
        ↓
Confirmation
```

The model may produce a structured action such as:

```json
{
  "action": "complete_task",
  "task_id": 421
}
```

The Action Router validates the operation before database mutation.

Potentially destructive actions should require confirmation.

## 27. Read vs Mutation

### Read

```text
"What do I have tomorrow?"
"What tasks are overdue?"
"What's important today?"
"How much did I spend this week?"
```

Flow:

```text
Voice
 ↓
Intent
 ↓
Service query
 ↓
SQLite
 ↓
Optional Qwen/Laya summarization
 ↓
Voice response
```

### Mutation

```text
"Add a task."
"Move my meeting."
"Mark this complete."
"Record that I worked out."
"Add today's expense."
```

Flow:

```text
Voice
 ↓
Intent
 ↓
Validation
 ↓
Domain Service
 ↓
SQLite transaction
 ↓
Confirmation
```

## 28. Performance Requirements

For the old Android device:

- SQLite remains the source of truth.
- Basic attention evaluation works without an LLM.
- Do not continuously poll the database.
- Use `next_evaluation_at` to schedule evaluations.
- Keep indexes focused.
- Store timestamps as INTEGER Unix time.
- Use INTEGER primary keys.
- Avoid large JSON blobs in hot queries.
- Do not materialize years of future attention events.
- Use SQLite transactions for mutations.
- Keep AI context small.
- Do not require network connectivity.

## 29. Privacy Requirements

All attention data remains local.

No attention event, notification history, alarm history, task deadline, schedule information, or voice-derived command needs to leave the device.

Qwen/Laya operate locally.

The database must not depend on cloud services.

## 30. Indexes

```sql
CREATE INDEX idx_attention_events_state_eval
ON attention_events(state, next_evaluation_at);

CREATE INDEX idx_attention_events_source
ON attention_events(source_type, source_id);

CREATE INDEX idx_attention_events_due
ON attention_events(due_at);

CREATE INDEX idx_attention_deliveries_event
ON attention_deliveries(attention_event_id);

CREATE INDEX idx_attention_deliveries_schedule
ON attention_deliveries(status, scheduled_at);

CREATE INDEX idx_alarms_time
ON alarms(enabled, alarm_time);
```

Keep indexes minimal and add more only after profiling.

## 31. Main Attention Query

```sql
SELECT *
FROM attention_events
WHERE state IN ('PENDING', 'SNOOZED')
  AND (
      next_evaluation_at IS NULL
      OR next_evaluation_at <= ?
  )
ORDER BY
    due_at ASC,
    importance DESC;
```

Final prioritization should remain in application logic.

## 32. Explicitly Excluded from v1

```text
attention_scores
escalation_history
notification_queue
voice_queue
acknowledgement table
snooze table
briefing_items
motivation table
urgency_history
quiet_hours
```

These would duplicate state or add unnecessary complexity.

## 33. Test Matrix

### Attention Events
- Create event
- Validate source
- Change importance
- Change urgency
- Set deadline
- Evaluate future event
- Expire event
- Complete event

### Deliveries
- Notification
- Voice
- Alarm
- Delivered
- Failed
- Cancelled
- Acknowledgement

### Escalation
- Normal reminder
- High-priority reminder
- Critical reminder
- Deadline approaching
- Deadline at risk
- Repeated ignored reminder
- Escalation suppression

### Snooze
- Snooze
- Re-evaluate after snooze
- Multiple snoozes
- Snooze near deadline

### Quiet Period
- Suppress normal attention during sleep
- Queue low priority
- Evaluate high priority
- Evaluate critical priority
- Respect alarm policy

### Alarm
- One-time alarm
- Recurring alarm
- Disable alarm
- Trigger alarm
- Acknowledge alarm

### Integration
- Task creates attention event
- Schedule affects attention
- Missed data gathering creates attention
- Morning briefing includes pending attention
- Completing source resolves attention
- Rescheduling source updates attention evaluation

## 34. Implementation Order

```text
1. Create attention_events
2. Create attention_deliveries
3. Create alarms
4. Add constraints
5. Add indexes
6. Implement repositories
7. Implement deterministic Attention Engine
8. Implement deadline-pressure calculation
9. Implement escalation
10. Implement acknowledgement
11. Implement snooze
12. Integrate Schedule context
13. Integrate Task occurrences
14. Integrate Data Gathering
15. Implement notification delivery
16. Implement Dexter voice delivery
17. Implement alarm delivery
18. Implement morning briefing aggregation
19. Integrate Action/Command Router
20. Test on target Android hardware
21. Profile SQLite performance
```

## 35. Final Decisions

| Decision | Status |
|---|---|
| Central Attention layer | FINAL |
| Event vs delivery separation | FINAL |
| Importance | FINAL |
| Urgency | FINAL |
| Deadline-pressure calculation | FINAL |
| NONE / Notification / Voice / Alarm | FINAL |
| `attention_events` | FINAL |
| `attention_deliveries` | FINAL |
| `alarms` | FINAL |
| Acknowledgement lifecycle | FINAL |
| Snooze behavior | FINAL |
| Escalation model | FINAL |
| Schedule-owned quiet periods | FINAL |
| Generated morning briefing | FINAL |
| Motivation as presentation feature | FINAL |
| No stored attention score | FINAL |
| No direct AI → SQLite access | FINAL |
| Voice → Action Router → Domain Service → SQLite | FINAL |
| Offline/local-only operation | FINAL |
| Low-resource design | FINAL |

## 36. Final Status

**ALERTS & ATTENTION v1 — FINALIZED**

The subsystem is intentionally small:

```text
attention_events
        +
attention_deliveries
        +
alarms
        +
deterministic Attention Engine
```

It provides the foundation for deadline reminders, VVIP escalation, missed data-gathering reminders, wake-up alarms, Dexter voice attention, notification escalation, morning briefings, and future Finance/Wellbeing/Relationship/Goal attention.
