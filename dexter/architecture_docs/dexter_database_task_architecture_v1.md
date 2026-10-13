# Dexter Database Architecture — Task Subsystem v1

**Status:** Draft / Design Baseline  
**Database:** SQLite  
**Deployment:** Fully local / offline  
**Primary constraint:** Old, low-RAM Android device  
**Scope:** Database architecture only — not Dexter voice orchestration, Qwen/Laya inference, Android hardware control, or UI.

---

## 1. Purpose

This document defines the database architecture for Dexter's task-management subsystem.

The database must be:

- fully local and offline
- private by design
- lightweight and fast on old hardware
- durable against crashes and power loss
- easy for a coding agent to implement
- extensible to schedules, alerts, finance, wellbeing, relationships, documents, journal, and daily briefings
- structured so AI components never become the source of truth

The task subsystem is the foundation for later scheduling, attention, productivity, and daily-summary features.

---

# 2. Core Architectural Principle

Use **Model B**:

> Every actionable piece of work has exactly one `task_occurrence`.

The model is:

```text
TASK
  = logical definition of work

TASK SERIES
  = recurrence definition

TASK OCCURRENCE
  = one actionable instance of work
```

Relationship:

```text
                         TASK
                          |
              +-----------+-----------+
              |                       |
          one-time                 recurring
              |                       |
              |                  TASK SERIES
              |                       |
              +-----------+-----------+
                          |
                          v
                  TASK OCCURRENCE
                          |
             +------------+-------------+
             |            |             |
             v            v             v
         SCHEDULE       ALERT        HISTORY
```

---

# 3. Why Model B

## Advantages

- One abstraction for actionable work.
- Scheduling always references an occurrence.
- Alerts always reference the specific occurrence.
- Completion and skipping use one state model.
- Recurring and one-time tasks can be queried together.
- Productivity analytics become much simpler.
- Rescheduling one occurrence does not alter the recurrence rule.
- Recurring exceptions are easy to represent.
- Historical occurrences remain independently measurable.

## Trade-offs

- One-time tasks require both a task and an occurrence record.
- More rows are created.
- Queries may require a join between task and occurrence.
- Schema is slightly more complex initially.

These costs are acceptable for SQLite. Even hundreds of thousands of rows are reasonable for this workload; excessive LLM inference, disk I/O, and poor queries are much more likely to affect the old device.

---

# 4. Source of Truth vs Derived Data

## Source-of-truth data

These records must survive application restarts and should not be casually regenerated:

- tasks
- task series
- task occurrences
- task dependencies
- schedules
- alerts
- observations
- domain records
- system events

## Derived/cache data

These can be rebuilt:

- daily state
- daily briefings
- daily summaries
- attention scores
- schedule conflict calculations
- other analytics/cache values

Principle:

```text
SOURCE DATA
     |
     v
DERIVATION
     |
     v
CACHE
```

If a derived table is corrupted, rebuild it from source data.

---

# 5. Core Tables

## 5.1 `tasks`

Represents the stable logical identity of a task.

Suggested fields:

```text
id
title
description
priority
estimated_duration_seconds
created_at
updated_at
archived_at
```

### Important rule

Do **not** store occurrence-specific state here:

- status
- scheduled time
- due time
- completion time

Those belong to `task_occurrences`.

---

## 5.2 `task_series`

Represents recurrence.

Suggested fields:

```text
id
task_id
recurrence_rule
timezone
start_at
end_at
status
created_at
updated_at
```

Series status:

```text
ACTIVE
PAUSED
ENDED
CANCELLED
```

A task does not need a series.

A one-time task has:

```text
series_id = NULL
```

on its occurrence.

---

## 5.3 `task_occurrences`

Represents one actionable instance.

Suggested fields:

```text
id
task_id
series_id
status
priority_override
duration_override_seconds
scheduled_start
scheduled_end
due_at
completed_at
skipped_at
created_at
updated_at
```

Status:

```text
PENDING
IN_PROGRESS
COMPLETED
BLOCKED
SKIPPED
CANCELLED
```

### Important rule

`OVERDUE` is not a stored status.

It is derived:

```text
status NOT IN (COMPLETED, CANCELLED, SKIPPED)
AND due_at < current_time
```

This prevents stale state.

---

# 6. Task Relationships

## Task → Series

One task may have zero or one active recurrence definition in the basic v1 model.

```text
tasks.id
    |
    v
task_series.task_id
```

If future requirements require multiple historical recurrence versions, introduce series versions rather than rewriting historical occurrences.

## Task → Occurrences

One task can have many occurrences.

```text
tasks
  |
  +-- occurrence
  +-- occurrence
  +-- occurrence
```

## Series → Occurrences

A recurring series produces many occurrences.

Historical occurrences must remain unchanged when future recurrence rules change.

---

# 7. One-Time Task Lifecycle

Example:

```text
CREATE
  |
  v
PENDING
  |
  +--> IN_PROGRESS --> COMPLETED
  |
  +--> BLOCKED --> PENDING
  |
  +--> SKIPPED
  |
  +--> CANCELLED
```

A task can become overdue without changing its stored status.

---

# 8. Recurring Task Lifecycle

The series lifecycle:

```text
ACTIVE
  |
  +--> PAUSED
  |       |
  |       +--> ACTIVE
  |
  +--> ENDED
  |
  +--> CANCELLED
```

Occurrence lifecycle remains independent:

```text
PENDING
  |
  +--> IN_PROGRESS --> COMPLETED
  |
  +--> SKIPPED
  |
  +--> CANCELLED
  |
  +--> BLOCKED --> PENDING
```

Pausing a series must not destroy historical occurrences.

---

# 9. Recurrence Generation

Do not generate years of future occurrences.

Use a rolling generation window, for example:

```text
today -> +30 or +60 days
```

The exact horizon should be configurable.

Benefits:

- less storage
- less startup work
- fewer rows
- simpler maintenance
- suitable for old hardware

If the user asks for a date outside the generated window, generate the necessary occurrences on demand.

---

# 10. Recurrence Rule Changes

Example:

```text
Old:
Every Monday at 07:00

New:
Every Monday at 08:00
```

Do not rewrite historical occurrences.

Conceptually:

```text
Historical occurrences
        |
        | unchanged
        v
Future occurrence generation
        |
        | uses new rule
        v
New occurrences
```

Historical facts must remain historically correct.

---

# 11. Rescheduling One Occurrence

Example:

```text
Series:
Every Sunday at 18:00

One occurrence:
Sunday Oct 4 -> Monday Oct 5
```

Only the occurrence changes.

The series remains:

```text
Every Sunday at 18:00
```

Future occurrences remain Sunday.

This is a core reason for separating task definitions, series, and occurrences.

---

# 12. Duration Model

Store the default estimate on `tasks`:

```text
estimated_duration_seconds
```

Allow an occurrence-specific override:

```text
duration_override_seconds
```

Effective duration:

```text
occurrence override
    OR
task default
```

This will support future scheduling and workload calculations.

Example:

```text
Task:
Database implementation

Default:
90 minutes

Today's occurrence:
120 minutes
```

No change to the default task is necessary.

---

# 13. Task Dependencies

Planned table:

```text
task_dependencies
----------------------------
task_id
depends_on_task_id
dependency_type
created_at
```

Dependencies should initially exist at the logical task level.

Example:

```text
Design database
      |
      v
Implement database
      |
      v
Test database
```

Do not duplicate dependency information on every occurrence.

Occurrence-specific dependency semantics can be added later if genuinely required.

---

# 14. Schedule Integration

A schedule occurrence should reference:

```text
task_occurrence_id
```

not merely `task_id`.

Example:

```text
Task:
Weekly finance review

Occurrence:
Oct 4

Schedule:
Oct 4, 18:00–19:00
```

This allows one occurrence to be moved without modifying the recurring definition.

Also remember:

> Not everything in a schedule is a task.

Schedules may represent:

- tasks
- meetings
- appointments
- blocked time
- available time
- flexible work blocks

Therefore schedule data should remain a separate domain.

---

# 15. Alert Integration

Alerts should reference the specific actionable occurrence where applicable.

Example:

```text
Task occurrence:
Submit report
Due:
17:00

Alert:
Due in 1 hour
```

Alert answers:

> Why should Dexter get the user's attention?

Attention events answer:

> How did Dexter surface it?

These are separate concepts.

---

# 16. Attention Model

Initial attention levels:

```text
NONE
PASSIVE
NOTIFICATION
DEXTER
ALARM
```

Possible delivery types:

```text
DASHBOARD
NOTIFICATION
VOICE
ALARM
```

An alert can produce multiple attention events:

```text
ALERT
 |
 +-- notification
 +-- Dexter voice
 +-- alarm
```

Acknowledgement does not necessarily resolve an alert.

Example:

```text
User hears:
"Your task is due in 30 minutes."

User:
"Okay."

=> attention acknowledged

Task still incomplete
=> alert may remain active
```

Only satisfaction of the underlying condition should resolve the alert.

---

# 17. Event/Audit Architecture

Use a central append-oriented:

```text
system_events
```

Suggested fields:

```text
id
event_type
entity_type
entity_id
occurred_at
payload
created_at
```

Examples:

```text
TASK_CREATED
TASK_UPDATED
TASK_OCCURRENCE_CREATED
TASK_STARTED
TASK_COMPLETED
TASK_SKIPPED
TASK_POSTPONED
TASK_CANCELLED
TASK_SERIES_PAUSED
TASK_SERIES_RESUMED
```

The event should contain only event-specific information.

Do not copy the entire task into every event.

---

# 18. Transactional Integrity

Important state changes and their event records should be written in the same SQLite transaction.

Example:

```sql
BEGIN;

UPDATE task_occurrences
SET
    status = 'COMPLETED',
    completed_at = ?,
    updated_at = ?
WHERE id = ?;

INSERT INTO system_events (...);

COMMIT;
```

If the device loses power before commit:

```text
neither change is committed
```

If commit succeeds:

```text
both changes exist
```

This prevents inconsistent history.

---

# 19. SQLite Configuration

Every database connection must enable foreign keys:

```sql
PRAGMA foreign_keys = ON;
```

Other SQLite settings should be selected after measuring the actual device.

Do not blindly enable every performance pragma.

Potential later considerations:

- WAL mode
- synchronous mode
- busy timeout
- cache size
- journal configuration

Durability should take priority over small performance gains.

---

# 20. ID Strategy

Use:

```sql
INTEGER PRIMARY KEY
```

for internal SQLite identifiers.

Advantages:

- compact
- fast
- excellent SQLite locality
- smaller indexes
- simpler queries

Do not use UUID strings everywhere unless an actual cross-device synchronization/export requirement appears.

---

# 21. Timestamp Strategy

Use UTC Unix timestamps internally.

Recommended initial representation:

```text
INTEGER seconds since Unix epoch
```

Recurring rules additionally retain a timezone:

```text
timezone = Asia/Kolkata
```

Why?

A timestamp represents an instant.

A recurrence represents a local calendar rule.

These are different concepts.

---

# 22. Deletion Policy

Normal task completion is not deletion.

Use lifecycle states:

```text
COMPLETED
SKIPPED
CANCELLED
ARCHIVED
```

rather than deleting records simply because they are no longer active.

However, privacy must remain a first-class requirement.

The eventual database architecture must support deliberate permanent deletion of user data.

"Append-only history" must not mean "the user can never erase private information."

Deletion policy should be specified separately for:

- tasks
- journal
- finance
- relationships
- documents
- observations
- event history

---

# 23. Index Strategy

Start with only indexes justified by real queries.

Recommended initial indexes:

```sql
CREATE INDEX idx_tasks_active
ON tasks(archived_at);

CREATE INDEX idx_task_series_task
ON task_series(task_id);

CREATE INDEX idx_task_series_status
ON task_series(status);

CREATE INDEX idx_task_occurrences_task
ON task_occurrences(task_id);

CREATE INDEX idx_task_occurrences_series
ON task_occurrences(series_id);

CREATE INDEX idx_task_occurrences_status_due
ON task_occurrences(status, due_at);

CREATE INDEX idx_task_occurrences_schedule
ON task_occurrences(scheduled_start);

CREATE INDEX idx_task_occurrences_created
ON task_occurrences(created_at);
```

Do not create indexes on every column.

Indexes consume storage and increase write cost.

---

# 24. Query Patterns the Schema Must Support

The task subsystem should efficiently answer:

### Today's tasks

```text
What do I need to do today?
```

### Upcoming

```text
What is coming up in the next 24 hours?
```

### Overdue

```text
What have I failed to finish?
```

### Recurring

```text
What recurring tasks are active?
```

### Completion

```text
What did I complete today?
```

### Adherence

```text
How often am I completing this recurring task?
```

### Rescheduling

```text
What can I move without modifying the recurrence?
```

### Workload

```text
How much estimated work is scheduled today?
```

### Attention

```text
Which task occurrences currently require attention?
```

---

# 25. Performance Principles for Old Hardware

The device is old, so the architecture should favor:

- simple indexed queries
- small records
- integer IDs
- bounded recurrence generation
- limited background work
- batch writes where safe
- transactions for related writes
- no unnecessary polling
- no full-database scans for routine operations
- no permanent duplication of large text
- derived caches that can be rebuilt
- lazy generation of future occurrences

Avoid:

- storing embeddings in the core task tables
- duplicating task descriptions into occurrences
- creating years of recurring records
- continuously recomputing analytics
- excessive indexes
- storing huge JSON payloads for ordinary relational data

---

# 26. Privacy Principles

Dexter is intended to be fully offline.

Database architecture should therefore assume:

```text
NO CLOUD
NO REMOTE DATABASE
NO TELEMETRY
NO EXTERNAL SYNC
```

unless explicitly added later.

The database should not contain:

- API keys
- cloud credentials
- unnecessary device identifiers
- remote telemetry
- external tracking identifiers

AI models should access database information through controlled application services rather than receiving unrestricted direct database access.

---

# 27. AI Boundary

Qwen/Laya are not the database source of truth.

Architecture:

```text
Dexter application
        |
        v
Domain/data services
        |
        v
SQLite
```

AI:

```text
Qwen/Laya
    |
    v
structured commands / decisions
    |
    v
application validation
    |
    v
database transaction
```

Never allow an LLM to arbitrarily execute raw destructive SQL.

For example:

```text
User:
"Complete my database task."

Qwen/Laya:
structured intent

        ↓

Task service:
validate task occurrence

        ↓

SQLite transaction:
update + event
```

The AI proposes.

The application validates.

SQLite persists.

---

# 28. Data Capture Architecture

Later, Dexter will need to collect information that the device cannot automatically know:

- spending
- money received
- savings
- exercise
- diet
- screen time
- relationship interactions
- mood
- other daily facts

Use:

```text
capture_sessions
       |
       v
observations
       |
       v
domain services
```

Example:

```text
Evening capture
    |
    +-- workout = 45 min
    +-- expense = ₹350
    +-- friend contacted = true
```

The observation is an input fact.

A domain table becomes the authoritative record after validation.

Do not use a giant untyped observation table as the permanent replacement for every domain.

---

# 29. Daily State

Daily state is derived.

Conceptually:

```text
tasks
schedules
alerts
finance
wellbeing
relationships
goals
observations
        |
        v
   daily_state
```

Daily state should be rebuildable.

It can contain compact summaries such as:

```text
tasks_total
tasks_completed
tasks_overdue
tasks_skipped
scheduled_minutes
completed_scheduled_minutes
active_alerts
capture_status
domain summaries
generated_at
```

---

# 30. Morning Briefing

The morning briefing is a generated artifact.

It should consider:

- today's schedule
- recurring task occurrences
- important tasks
- deadlines
- overdue work
- active alerts
- relevant goals
- other domain information when those domains exist

The generated briefing can be stored in:

```text
daily_briefings
```

but the briefing itself is not the source of truth.

---

# 31. End-of-Day Summary

The evening summary should be generated from actual stored facts.

It should eventually compare:

```text
planned
vs
completed
vs
skipped
vs
overdue
```

and incorporate information gathered through the daily capture session.

This makes the evening capture session particularly important because Dexter cannot automatically know everything happening on another device.

---

# 32. Reliability Model

The database should survive:

- app crash
- process termination
- device restart
- unexpected power loss
- partial task operations

Important state transitions should be atomic.

Example:

```text
Complete task
      |
      +-- update occurrence
      |
      +-- write event
      |
      +-- update dependent alert
      |
      +-- commit
```

When several records must remain consistent, use one transaction.

---

# 33. What This Design Does NOT Cover Yet

Do not implement these in the task subsystem prematurely:

```text
Finance
Wellbeing
Relationships
Journal
Documents
Goals
RAG
Embeddings
Voice processing
Qwen
Laya
Android automation
Notifications
Alarm implementation
```

The task schema should provide stable references that these future domains can use.

---

# 34. Recommended Database Build Order

Implement the database in this order:

```text
01. tasks
02. task_series
03. task_occurrences
04. task_dependencies
05. system_events
06. schedules
07. alerts
08. attention events
09. capture sessions
10. observations
11. finance
12. wellbeing
13. relationships
14. goals
15. journal
16. documents
17. daily state
18. briefings
19. summaries
```

Do not implement the entire database in one migration.

Keep migrations small and reversible where practical.

---

# 35. Key Design Invariants

These should become tests.

### Invariant 1

Every occurrence references an existing task.

### Invariant 2

If an occurrence has a series, that series belongs to the same task.

### Invariant 3

Historical occurrences are not rewritten because future recurrence rules change.

### Invariant 4

Overdue is derived, not stored as a permanent task status.

### Invariant 5

One occurrence can have its own schedule without changing the recurrence rule.

### Invariant 6

Acknowledging an alert does not necessarily resolve its underlying condition.

### Invariant 7

Important state changes and corresponding event records are committed atomically.

### Invariant 8

Derived data can be deleted and regenerated.

### Invariant 9

AI output never bypasses application validation.

### Invariant 10

The database remains usable without network access.

---

# 36. Proposed Core DDL

The initial task tables should follow this structure:

```sql
CREATE TABLE tasks (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,

    priority TEXT NOT NULL DEFAULT 'NORMAL'
        CHECK (priority IN (
            'CRITICAL',
            'HIGH',
            'NORMAL',
            'LOW',
            'SOMEDAY'
        )),

    estimated_duration_seconds INTEGER
        CHECK (
            estimated_duration_seconds IS NULL
            OR estimated_duration_seconds > 0
        ),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    archived_at INTEGER
);

CREATE TABLE task_series (
    id INTEGER PRIMARY KEY,

    task_id INTEGER NOT NULL,

    recurrence_rule TEXT NOT NULL,

    timezone TEXT NOT NULL DEFAULT 'Asia/Kolkata',

    start_at INTEGER NOT NULL,
    end_at INTEGER,

    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN (
            'ACTIVE',
            'PAUSED',
            'ENDED',
            'CANCELLED'
        )),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE RESTRICT,

    CHECK (
        end_at IS NULL OR end_at > start_at
    )
);

CREATE UNIQUE INDEX idx_task_series_task_id_id
ON task_series(task_id, id);

CREATE TABLE task_occurrences (
    id INTEGER PRIMARY KEY,

    task_id INTEGER NOT NULL,
    series_id INTEGER,

    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
            'PENDING',
            'IN_PROGRESS',
            'COMPLETED',
            'BLOCKED',
            'SKIPPED',
            'CANCELLED'
        )),

    priority_override TEXT
        CHECK (
            priority_override IS NULL
            OR priority_override IN (
                'CRITICAL',
                'HIGH',
                'NORMAL',
                'LOW',
                'SOMEDAY'
            )
        ),

    duration_override_seconds INTEGER
        CHECK (
            duration_override_seconds IS NULL
            OR duration_override_seconds > 0
        ),

    scheduled_start INTEGER,
    scheduled_end INTEGER,

    due_at INTEGER,

    completed_at INTEGER,
    skipped_at INTEGER,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE RESTRICT,

    FOREIGN KEY (task_id, series_id)
        REFERENCES task_series(task_id, id)
        ON DELETE RESTRICT,

    CHECK (
        scheduled_end IS NULL
        OR scheduled_start IS NOT NULL
    ),

    CHECK (
        scheduled_end IS NULL
        OR scheduled_end > scheduled_start
    ),

    CHECK (
        completed_at IS NULL
        OR status = 'COMPLETED'
    ),

    CHECK (
        skipped_at IS NULL
        OR status = 'SKIPPED'
    )
);
```

---

# 37. Final Architecture Summary

The task subsystem should be understood as:

```text
                    +----------------+
                    |      TASK      |
                    | logical work   |
                    +-------+--------+
                            |
                 +----------+----------+
                 |                     |
           one-time                recurring
                 |                     |
                 |              +------+------+
                 |              | TASK SERIES |
                 |              +------+------+
                 |                     |
                 +----------+----------+
                            |
                            v
                   +------------------+
                   | TASK OCCURRENCE  |
                   | actionable work  |
                   +--------+---------+
                            |
             +--------------+---------------+
             |              |               |
             v              v               v
         SCHEDULE         ALERT          EVENTS
             |              |
             |              v
             |         ATTENTION
             |
             v
       DAILY STATE
             |
       +-----+------+
       |            |
       v            v
   MORNING       EVENING
   BRIEFING      SUMMARY
```

The most important architectural decision is:

> **Tasks describe work; occurrences describe actionable instances; series describe recurrence.**

Everything else should build around that distinction.

---

## Next implementation step

Before moving to Schedule, the next database specification should finalize:

1. `task_dependencies`
2. exact allowed state transitions
3. recurrence exception handling
4. task deletion/archive semantics
5. event schema
6. transaction boundaries
7. unit/integration test cases

Only after those are frozen should the task subsystem be considered complete.
