# Dexter Database Architecture — Time & Schedule Subsystem v1

**Status:** Finalized  
**Database:** SQLite  
**Architecture:** Offline-first, local-only, privacy-first  
**Target hardware:** Low-RAM / older Android device  
**AI boundary:** Qwen 2.5 0.5B + Laya are decision/reasoning models; they are not the source of truth.

## 1. Purpose

The Time & Schedule subsystem manages recurring timetable patterns, fixed commitments, temporary schedule changes, actual planned time blocks, and derived daily availability.

Design goals:
- Lightweight and fast
- Fully offline and private
- Deterministic scheduling where possible
- Suitable for old Android hardware
- Small structured AI context
- No unnecessary duplicated data

## 2. Core Architecture

Persistent concepts:

```text
timetable_rules
schedule_events
schedule_exceptions
schedule_blocks
```

Availability is derived:

```text
TIMETABLE RULES
       +
SCHEDULE EXCEPTIONS
       +
FIXED EVENTS
       +
SCHEDULE BLOCKS
       ↓
ACTUAL AVAILABILITY
       ↓
SCHEDULER
       ↓
SCHEDULE BLOCKS
```

## 3. Concepts

### Timetable Rule
A recurring pattern describing normal time structure. It is a template, not a day's actual schedule.

### Schedule Event
A fixed commitment occupying a particular time. Examples: meetings, appointments, classes, travel. It does not require `event_type`.

### Schedule Block
A concrete planned allocation of time. It may reference a `task_occurrence_id`, but does not have to.

### Schedule Exception
A temporary change to a timetable rule. It preserves the permanent timetable.

## 4. Constraint Levels

### HARD
Dexter must not automatically violate it.

Examples: sleep, required work shift, flight, fixed appointment.

### PREFERRED
Dexter normally respects it but may propose moving it.

Examples: exercise, study, deep work, personal time.

### FLEXIBLE
Dexter may move it during optimization.

Examples: reading, low-priority administration, optional activities.

Principle:

> Dexter may optimize within the user's rules, but it cannot silently rewrite the user's rules.

Explicit user overrides are represented as temporary exceptions; the permanent timetable remains unchanged.

## 5. `timetable_rules`

Suggested v1 schema:

```sql
CREATE TABLE timetable_rules (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    recurrence_rule TEXT NOT NULL,
    start_time INTEGER NOT NULL,
    end_time INTEGER NOT NULL,
    constraint_level TEXT NOT NULL DEFAULT 'PREFERRED'
        CHECK (constraint_level IN ('HARD','PREFERRED','FLEXIBLE')),
    active_from INTEGER,
    active_until INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    CHECK (
        active_until IS NULL
        OR active_from IS NULL
        OR active_until >= active_from
    )
);
```

`start_time` and `end_time` are seconds from midnight.

## 6. Overnight Rules

`end_time < start_time` means the rule crosses midnight.

Example:

```text
23:00 → 06:30
```

The scheduling service interprets this as:

```text
Day N:     23:00 → 24:00
Day N + 1: 00:00 → 06:30
```

## 7. `schedule_events`

```sql
CREATE TABLE schedule_events (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    start_at INTEGER NOT NULL,
    end_at INTEGER NOT NULL,
    constraint_level TEXT NOT NULL DEFAULT 'HARD'
        CHECK (constraint_level IN ('HARD','PREFERRED','FLEXIBLE')),
    recurrence_rule TEXT,
    location TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    CHECK (end_at > start_at)
);
```

One-time events have `recurrence_rule = NULL`.

## 8. `schedule_exceptions`

```sql
CREATE TABLE schedule_exceptions (
    id INTEGER PRIMARY KEY,
    timetable_rule_id INTEGER,
    start_at INTEGER NOT NULL,
    end_at INTEGER NOT NULL,
    action TEXT NOT NULL
        CHECK (action IN ('CANCEL','REPLACE','MODIFY')),
    replacement_start_at INTEGER,
    replacement_end_at INTEGER,
    constraint_level TEXT
        CHECK (
            constraint_level IS NULL
            OR constraint_level IN ('HARD','PREFERRED','FLEXIBLE')
        ),
    reason TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (timetable_rule_id)
        REFERENCES timetable_rules(id)
        ON DELETE RESTRICT,
    CHECK (end_at > start_at)
);
```

Examples:
- `CANCEL` a normal work period for a holiday.
- `MODIFY` exercise from 18:00–19:00 to 19:00–20:00 for one date.
- `REPLACE` a normal timetable period with another period.

## 9. `schedule_blocks`

```sql
CREATE TABLE schedule_blocks (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    start_at INTEGER NOT NULL,
    end_at INTEGER NOT NULL,
    constraint_level TEXT NOT NULL DEFAULT 'FLEXIBLE'
        CHECK (constraint_level IN ('HARD','PREFERRED','FLEXIBLE')),
    task_occurrence_id INTEGER,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM'
        CHECK (created_by IN ('USER','SYSTEM','ASSISTANT')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (task_occurrence_id)
        REFERENCES task_occurrences(id)
        ON DELETE RESTRICT,
    CHECK (end_at > start_at)
);
```

A schedule block without a task is valid for exercise, meals, family time, reading, travel, breaks, and personal time.

## 10. Task ↔ Schedule Separation

```text
TASK OCCURRENCE
    = what needs to be done

SCHEDULE BLOCK
    = when Dexter plans to do it
```

Scheduling a task does not modify its deadline, priority, recurrence, or goal.

## 11. Hybrid Availability

Do not permanently store every daily available slot.

Derive actual availability from:

```text
Timetable rules
+
Schedule exceptions
+
Schedule events
+
Schedule blocks
↓
Actual daily availability
```

This reduces storage and prevents redundant state.

## 12. Conflict Rules

### No default overlapping schedule blocks

For v1, active schedule blocks must not overlap unless an explicitly supported future mechanism permits it.

### HARD
Cannot be silently displaced.

### PREFERRED
May be proposed for movement.

### FLEXIBLE
May be automatically rearranged.

### User override
An explicit user instruction can override a normal timetable rule, but should be represented as a temporary exception.

## 13. Scheduling Algorithm

Recommended flow:

```text
1. Find candidate free periods.
2. Remove HARD conflicts.
3. Consider PREFERRED periods.
4. Consider FLEXIBLE periods.
5. Consider task deadline.
6. Consider duration.
7. Consider priority.
8. Generate viable candidates.
9. Select or recommend the least disruptive option.
10. Validate.
11. Commit using a SQLite transaction.
```

The objective is least disruption, not merely lowest numeric priority.

## 14. AI / Database Boundary

Qwen/Laya should not directly mutate SQLite.

```text
SQLite
   ↓
Schedule Service
   ↓
Current facts + constraints
   ↓
Candidate schedules
   ↓
Qwen/Laya when judgment is needed
   ↓
Validated decision
   ↓
Schedule Service
   ↓
SQLite transaction
```

Basic time arithmetic and conflict detection remain deterministic application logic.

## 15. Indexes

```sql
CREATE INDEX idx_timetable_rules_active
ON timetable_rules(active_from, active_until);

CREATE INDEX idx_schedule_events_time
ON schedule_events(start_at, end_at);

CREATE INDEX idx_schedule_blocks_time
ON schedule_blocks(start_at, end_at);

CREATE INDEX idx_schedule_blocks_task
ON schedule_blocks(task_occurrence_id);

CREATE INDEX idx_schedule_exceptions_time
ON schedule_exceptions(start_at, end_at);

CREATE INDEX idx_schedule_exceptions_rule
ON schedule_exceptions(timetable_rule_id);
```

Keep indexes minimal and expand only after profiling.

## 16. Integrity Rules

1. Every schedule block must have `end_at > start_at`.
2. A task-linked block must reference an existing task occurrence.
3. Moving a block does not change a task deadline.
4. Changing a timetable rule does not rewrite historical blocks.
5. Exceptions affect only their specified period.
6. HARD rules cannot be automatically displaced.
7. Availability is derived rather than permanently stored.
8. Default active schedule blocks must not overlap.
9. Explicit user overrides must not modify the permanent timetable.
10. Historical schedules remain available for auditing and analysis.

## 17. Test Matrix

### Timetable
- Create recurring rule
- Update/deactivate rule
- HARD/PREFERRED/FLEXIBLE behavior
- Overnight rule

### Events
- Create/update/cancel event
- One-time and recurring event
- Detect conflicts

### Exceptions
- Cancel timetable occurrence
- Modify timetable occurrence
- Temporary override
- Preserve original timetable

### Blocks
- Create/move/delete block
- Link to task occurrence
- Create block without task
- Reject invalid range
- Reject unintended overlap

### Integration
- Schedule task occurrence
- Reschedule without changing deadline
- Complete task and preserve history
- Protect HARD rules
- Propose PREFERRED movement
- Rearrange FLEXIBLE blocks
- Accept explicit user override

## 18. Example

```text
TIMETABLE
23:00–06:30  Sleep      HARD
09:00–17:00  Work       HARD
18:00–19:00  Exercise   PREFERRED
21:00–23:00  Personal   FLEXIBLE

FIXED EVENT
14:00–15:00  Team meeting

SCHEDULE BLOCKS
10:00–11:30  Database architecture
16:00–16:30  Administrative work
19:00–20:30  Dinner/personal

TASK OCCURRENCE
Database architecture
    ↓
schedule block 10:00–11:30
```

## 19. Performance and Privacy

For the old Android device:

- Use SQLite integer primary keys.
- Store timestamps as INTEGER Unix time.
- Store recurring rules compactly.
- Do not materialize years of future schedules.
- Generate future data in a rolling window where needed.
- Derive daily availability on demand.
- Keep indexes focused.
- Avoid large JSON blobs in hot scheduling queries.
- Keep AI context small.
- Use transactions for schedule mutations.
- Require no network access.

No schedule data needs to leave the device.

## 20. Relationship to Other Domains

```text
TASKS
  ↓
Task occurrences → Schedule blocks

GOALS / PROJECTS
  ↓
Planning context

ALERTS / ATTENTION
  ↓
Scheduled-time and deadline triggers

WELLBEING
  ↓
Sleep/exercise/routine constraints

RELATIONSHIPS
  ↓
Birthdays/events/follow-ups

FINANCE
  ↓
Financial planning blocks

DAILY CAPTURE
  ↓
Actual schedule adherence

JOURNAL
  ↓
Reflection and productivity history
```

Integrations should be implemented through domain services rather than tightly coupling every table.

## 21. Implementation Order

```text
1. SQLite tables
2. Foreign keys and constraints
3. Indexes
4. Timetable repository
5. Schedule-event repository
6. Exception repository
7. Schedule-block repository
8. Availability calculation service
9. Conflict detection
10. Scheduling service
11. Task-occurrence integration
12. User override handling
13. Unit tests
14. Integration tests
15. Performance tests on target hardware
```

Do not implement Qwen/Laya scheduling logic until deterministic scheduling is working and tested.

## 22. Final v1 Decisions

| Decision | Status |
|---|---|
| Hybrid availability | FINAL |
| Timetable rules | FINAL |
| Fixed schedule events | FINAL |
| Schedule exceptions | FINAL |
| Schedule blocks | FINAL |
| No redundant `event_type` | FINAL |
| HARD/PREFERRED/FLEXIBLE | FINAL |
| Overnight timetable support | FINAL |
| Availability derived | FINAL |
| Task ↔ schedule separation | FINAL |
| No default overlapping blocks | FINAL |
| HARD cannot be silently overridden | FINAL |
| PREFERRED can be proposed for movement | FINAL |
| FLEXIBLE can be automatically rearranged | FINAL |
| Explicit user override supported | FINAL |
| Historical schedules preserved | FINAL |
| AI cannot directly mutate database | FINAL |

## 23. Status

**TIME & SCHEDULE v1 — FINALIZED**

Next subsystem:

**ALERTS & ATTENTION**

This will define notification, Dexter voice intervention, alarm escalation, urgency, importance, deadline pressure, quiet periods, acknowledgement, repetition, and user-controlled interruption policies.
