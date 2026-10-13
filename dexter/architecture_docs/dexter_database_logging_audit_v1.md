# Dexter Database — Logging / Audit v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**Storage ceiling:** 20 GB total Dexter data layer

---

## 1. Purpose

Logging / Audit is Dexter's **technical control room**.

It records enough information to:

- understand what Dexter is doing;
- diagnose failures;
- track important data changes;
- explain important automated actions;
- monitor system health;
- maintain accountability.

It must remain lightweight and must never become a major storage consumer.

---

# 2. Logging vs Audit

## System Logging

Records technical/system events.

Examples:

```text
Application started
Database opened
Model loaded
Model unloaded
Voice recognition failed
RAG indexing started
RAG indexing failed
Storage warning
Backup completed
```

## Audit Logging

Records important user/data actions.

Examples:

```text
Task created
Task completed
Task deleted
Deadline changed
Goal modified
Finance transaction added
Relationship updated
Document deleted
Reminder dismissed
Settings changed
```

### Simple distinction

```text
Journal
→ What the user thinks / experiences.

System Log
→ What Dexter/the software did.

Audit
→ What important data or configuration changed.
```

---

# 3. Action / Explanation Trail

Important Dexter actions should be explainable.

Example:

```text
ACTION
Created reminder

REASON
VVIP task deadline approaching

SOURCE
Task #182

TRIGGER
Attention rule: VVIP deadline

ACTOR
DEXTER

TIME
01:04 AM
```

This allows the user to ask:

> "Why did Dexter do this?"

and receive a traceable explanation.

---

# 4. Actors

Every important audit action should identify its actor.

Allowed actors:

```text
USER
DEXTER
SYSTEM
AUTOMATION
```

Examples:

```text
USER
Changed task deadline.

DEXTER
Created reminder because a deadline rule was triggered.

SYSTEM
Recovered an interrupted database transaction.

AUTOMATION
Created scheduled morning briefing.
```

---

# 5. System Log Schema

```sql
CREATE TABLE system_logs (
    id INTEGER PRIMARY KEY,

    level TEXT NOT NULL
        CHECK (level IN (
            'DEBUG',
            'INFO',
            'WARNING',
            'ERROR',
            'CRITICAL'
        )),

    component TEXT NOT NULL,

    event_type TEXT NOT NULL,

    message TEXT,

    details_json TEXT,

    created_at INTEGER NOT NULL
);
```

### Examples of components

```text
DATABASE
VOICE
LLM
LAYA
RAG
SCHEDULER
ALERTS
STORAGE
BACKUP
UI
SYSTEM
```

---

# 6. Audit Schema

```sql
CREATE TABLE audit_logs (
    id INTEGER PRIMARY KEY,

    actor_type TEXT NOT NULL
        CHECK (actor_type IN (
            'USER',
            'DEXTER',
            'SYSTEM',
            'AUTOMATION'
        )),

    action_type TEXT NOT NULL,

    entity_type TEXT NOT NULL,

    entity_id INTEGER,

    before_json TEXT,

    after_json TEXT,

    reason TEXT,

    source_type TEXT,

    source_id INTEGER,

    created_at INTEGER NOT NULL
);
```

### Examples

```text
actor: USER
action: UPDATE
entity: TASK
entity_id: 182

before:
deadline = 18:00

after:
deadline = 20:00

reason:
User voice command
```

---

# 7. Action Explanation Schema

For important Dexter actions:

```sql
CREATE TABLE action_traces (
    id INTEGER PRIMARY KEY,

    action_type TEXT NOT NULL,

    actor_type TEXT NOT NULL,

    entity_type TEXT,

    entity_id INTEGER,

    trigger_type TEXT,

    trigger_id INTEGER,

    reason TEXT,

    source_type TEXT,

    source_id INTEGER,

    result TEXT,

    created_at INTEGER NOT NULL
);
```

This is intentionally separate from generic technical logging.

---

# 8. System Health

Logging/Audit should monitor lightweight health indicators.

Examples:

```text
Database size
Available storage
Database integrity
Last successful backup
Last failed operation
RAG indexing status
Pending indexing queue
Model load status
Last application startup
Last crash/error
```

Health information should be summarized rather than recorded every second.

---

# 9. Storage Monitoring

Because Dexter has a 20 GB total data ceiling, the logging subsystem must track storage.

Conceptually:

```text
Total Dexter data
        │
        ├── SQLite
        ├── Documents
        ├── RAG indexes
        ├── Logs
        ├── Cache
        └── Temporary data
```

The system should warn before the storage ceiling is reached.

Example levels:

```text
NORMAL
WARNING
CRITICAL
```

Exact thresholds remain configurable.

---

# 10. Log Retention

Logs must have retention rules.

Recommended starting policy:

```text
DEBUG
→ very short retention

INFO
→ short retention

WARNING
→ longer retention

ERROR
→ longer retention

CRITICAL
→ long retention until explicitly cleaned

AUDIT
→ long retention according to final privacy/retention policy

ACTION TRACE
→ retain important traces longer than ordinary technical logs
```

Exact retention durations should be configurable after real-world usage.

---

# 11. Storage Cleanup

Cleanup should occur in this order:

```text
Temporary files
        ↓
Caches
        ↓
Old DEBUG logs
        ↓
Old INFO logs
        ↓
Old low-value technical logs
```

Do **not** automatically delete:

```text
User data
Audit records
Important action traces
Journal entries
Financial records
Relationship history
Documents
```

without applying their own explicit retention/deletion policy.

---

# 12. Logging Must Not Become a Storage Problem

Rules:

- Never log continuously at high frequency without need.
- Avoid logging large document contents.
- Avoid logging full model prompts by default.
- Avoid logging full model outputs by default.
- Avoid storing duplicate data in logs.
- Keep `details_json` concise.
- Use IDs/references instead of copying entire records.

Bad:

```text
LOG:
Full 20-page document contents...
```

Good:

```text
LOG:
Document #84 indexed successfully.
chunks=127
duration_ms=1820
```

---

# 13. Privacy

Logging can accidentally expose sensitive information.

Therefore:

### Never place sensitive content into generic logs by default.

Examples:

```text
Passwords
Authentication tokens
Private document contents
Full financial account information
Private relationship details
Journal contents
Voice recordings
Full private conversations
```

Logs should contain references/IDs instead.

---

# 14. Voice Command Audit

When a voice command changes important data, the audit trail should preserve enough information to understand the action.

Example:

```text
Actor:
USER

Source:
VOICE

Action:
UPDATE_TASK

Entity:
TASK #182

Result:
SUCCESS
```

The full voice recording does not need to be permanently stored.

If transcription is retained, it should follow the appropriate privacy policy.

---

# 15. Dexter Action Trace

For automated actions:

```text
Trigger
   ↓
Decision
   ↓
Action
   ↓
Result
```

Example:

```text
Trigger:
Task deadline within 3 hours

Decision:
VVIP priority requires attention

Action:
Create alert

Result:
Alert scheduled successfully
```

This is especially important for:

- alerts;
- alarms;
- reminders;
- automated briefings;
- scheduled tasks;
- data gathering prompts;
- future automation.

---

# 16. Failed Actions

Failed actions must be distinguishable from successful actions.

Example:

```text
ACTION
Create reminder

RESULT
FAILED

REASON
Scheduler unavailable

RECOVERY
Queued for retry
```

Dexter should not claim an action succeeded when the system could not verify success.

---

# 17. Database Integrity

The logging subsystem should support detection of:

```text
Database corruption
Failed transactions
Foreign-key failures
Migration failures
Unexpected shutdown recovery
Incomplete writes
```

SQLite integrity checks should be performed at appropriate maintenance points, not continuously.

---

# 18. Crash Recovery

After an unexpected shutdown:

```text
Application starts
        ↓
Check database state
        ↓
Recover pending state if possible
        ↓
Write recovery log
        ↓
Continue
```

Example:

```text
SYSTEM RECOVERY

Previous state:
Interrupted during RAG indexing

Recovery:
Rolled back incomplete index update

Result:
Database consistent
```

---

# 19. Audit Integrity

Audit records should be treated as important historical records.

Rules:

- Do not casually modify historical audit entries.
- Corrections should create a new audit event.
- Important changes should retain before/after values where practical.
- Audit records should reference the affected entity.
- Audit records should identify the actor.

Example:

```text
Audit #501
USER updated TASK #182

Then:

Audit #502
USER reverted TASK #182
```

This is preferable to silently changing Audit #501.

---

# 20. Logging and Journal Separation

Never confuse:

```text
Journal
```

with:

```text
System Log
```

Journal:

> "I felt overwhelmed today."

System log:

> "Wellbeing record #81 created."

Audit:

> "User changed wellbeing record #81."

These serve completely different purposes.

---

# 21. Logging and RAG

Logs should generally **not** be part of normal RAG retrieval.

Do not answer:

> "What happened with my Dexter project?"

by searching technical logs first.

Logs are primarily for:

- diagnostics;
- system health;
- accountability;
- troubleshooting.

They may be consulted by Dexter's diagnostic functions when explicitly relevant.

---

# 22. Logging and Briefings

Normal morning/evening briefings should not expose technical logs.

Instead, only surface meaningful system problems.

Example:

```text
Normal:
No mention.

Problem:
"Yesterday Dexter failed to index 3 documents.
They will be retried today."
```

Do not say:

> "FTS transaction #1847 returned SQLITE_BUSY."

unless the user asks for technical diagnostics.

---

# 23. User-Facing Diagnostics

Dexter should provide a simple explanation layer.

User:

> "Is everything okay with Dexter?"

Possible response:

```text
Yes. Database is healthy,
storage is within limits,
and there are no unresolved critical errors.
```

If something is wrong:

```text
There is one issue:
3 documents failed to index yesterday.
Your original files are safe.
Dexter will retry them.
```

---

# 24. Developer / Technical Diagnostics

A deeper diagnostic mode can expose:

```text
Database health
Storage
Model status
RAG status
Scheduler
Alert engine
Recent errors
Recent failed actions
```

This should be separate from normal user interaction.

---

# 25. Performance Rules

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
2. Keep log records small.
3. Batch non-critical log writes where practical.
4. Avoid synchronous logging on latency-sensitive operations.
5. Do not log every model token.
6. Do not log every UI event.
7. Avoid storing large JSON payloads.
8. Index only fields required for diagnostics.
9. Periodically clean old low-value logs.
10. Keep audit records separate from high-volume technical logs.

---

# 26. Recommended Indexes

```sql
CREATE INDEX idx_system_logs_level_time
ON system_logs(level, created_at);

CREATE INDEX idx_system_logs_component_time
ON system_logs(component, created_at);

CREATE INDEX idx_audit_logs_entity
ON audit_logs(entity_type, entity_id, created_at);

CREATE INDEX idx_audit_logs_actor_time
ON audit_logs(actor_type, created_at);

CREATE INDEX idx_action_traces_entity
ON action_traces(entity_type, entity_id, created_at);

CREATE INDEX idx_action_traces_time
ON action_traces(created_at);
```

---

# 27. What v1 Does NOT Include

```text
Distributed logging
Cloud logging
Remote telemetry
Continuous token-by-token model logging
Full prompt archives
Full response archives
Enterprise SIEM
Remote monitoring
Automatic log upload
Complex observability platform
Large metrics database
```

These are intentionally excluded.

---

# 28. Critical Logging / Audit Rules

```text
RULE 01
Logging is Dexter's technical control room.

RULE 02
Journal records the user's experience; logs record system behavior.

RULE 03
Audit records important data/configuration changes.

RULE 04
Important automated actions should have an explanation trace.

RULE 05
Every important audit action identifies its actor.

RULE 06
Allowed actors are USER, DEXTER, SYSTEM, and AUTOMATION.

RULE 07
Technical logs and audit records remain separate.

RULE 08
Logs must remain lightweight.

RULE 09
The 20 GB storage ceiling applies to logs and audit data.

RULE 10
Technical logs must have retention policies.

RULE 11
Low-value technical logs are the first candidates for cleanup.

RULE 12
User data must not be deleted merely to make room for logs.

RULE 13
Logs must not contain sensitive data by default.

RULE 14
Use IDs/references instead of copying large records into logs.

RULE 15
Do not log full documents by default.

RULE 16
Do not log full model prompts or outputs by default.

RULE 17
Do not log every model token.

RULE 18
Do not log every UI event.

RULE 19
Failed actions must be clearly distinguished from successful actions.

RULE 20
Dexter must never claim an action succeeded without confirmation.

RULE 21
Important audit history should not be silently modified.

RULE 22
Corrections to historical actions should create new audit events.

RULE 23
Database integrity checks should occur during appropriate maintenance points.

RULE 24
Unexpected shutdowns should produce a recovery record.

RULE 25
Normal RAG retrieval should not search technical logs.

RULE 26
Normal briefings should surface only meaningful system problems.

RULE 27
Technical details should be available through diagnostic mode.

RULE 28
Caches and temporary data should be cleaned before authoritative data.

RULE 29
Logging must not materially slow user interactions.

RULE 30
The subsystem must remain suitable for the Vivo V11.

RULE 31
No cloud telemetry is required.

RULE 32
The logging system itself must remain fully offline/local-first.

RULE 33
Action traces should record trigger → decision → action → result for important Dexter automation.

RULE 34
Voice-driven data changes should preserve source information without requiring permanent voice-recording storage.

RULE 35
The logging subsystem exists to make Dexter understandable, debuggable, and trustworthy.
```

---

# 29. Final Architecture

```text
                         DEXTER
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
          SYSTEM LOG     AUDIT      ACTION TRACE
              │            │            │
              │            │            │
              ▼            ▼            ▼
          Technical      Data        Why Dexter
           events       changes      acted
              │            │            │
              └────────────┼────────────┘
                           ▼
                         SQLite
                           │
             ┌─────────────┼─────────────┐
             ▼             ▼             ▼
         Retention      Health       Diagnostics
             │             │             │
             └─────────────┼─────────────┘
                           ▼
                     Dexter Control
                         Room
```

---

# 30. Final Status

**LOGGING / AUDIT v1 — FINALIZED**

The subsystem provides:

- lightweight system logging;
- audit history;
- action/explanation traces;
- actor identification;
- system health monitoring;
- storage monitoring;
- failure tracking;
- crash/recovery records;
- privacy protection;
- retention and cleanup;
- diagnostic mode;
- strict 20 GB storage awareness;
- offline/local-only operation;
- Vivo V11 performance constraints.

It is intentionally a small **technical control room**, not a heavyweight enterprise observability system.
