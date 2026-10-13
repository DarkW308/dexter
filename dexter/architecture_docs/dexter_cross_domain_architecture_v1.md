# Dexter Database — Cross-Domain Architecture v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**AI:** Qwen 2.5 0.5B + Laya  
**Storage ceiling:** 20 GB total Dexter data layer

---

## 1. Purpose

Cross-Domain Architecture is the layer that makes all finalized Dexter subsystems behave as **one coherent system**.

Its job is to define:

- ownership;
- dependencies;
- data flow;
- source-of-truth boundaries;
- cross-domain references;
- transactions;
- retention;
- deletion;
- privacy;
- AI boundaries;
- derived-data rules;
- storage responsibilities;
- failure behavior.

It must prevent the system from becoming a collection of independent mini-apps.

---

# 2. Final Domain Map

```text
                         DEXTER
                           │
              ┌────────────┴────────────┐
              │     APPLICATION LAYER   │
              │                         │
              │ Intent / Action Router  │
              │ Domain Services         │
              │ Context Builder         │
              │ Briefing Engine         │
              └────────────┬────────────┘
                           │
                           ▼
                    SQLITE SOURCE
                      OF TRUTH
                           │
      ┌────────────┬───────┼────────┬──────────────┐
      ▼            ▼       ▼        ▼              ▼
    TASKS       SCHEDULE  GOALS   DOMAINS       KNOWLEDGE
      │            │       │        │              │
      ▼            ▼       ▼        ▼              ▼
 OCCURRENCES    BLOCKS  PROJECTS  Finance       Documents
      │                         Wellbeing        / RAG
      │                         Relationships
      ▼                         Journal
  ATTENTION                     Capture
      │
      ▼
 DELIVERY
      │
      ▼
    USER

Logging/Audit observes important system/data actions.
```

---

# 3. Ownership Principle

Every important concept has exactly one authoritative owner.

| Concept | Owner |
|---|---|
| Task definition | Tasks |
| Recurrence of tasks | Task Series |
| Actionable task instance | Task Occurrence |
| Calendar/time structure | Schedule |
| Planned time allocation | Schedule Block |
| Goal | Goals |
| Project | Projects |
| Financial movement | Finance Transactions |
| Wellbeing observation | Wellbeing |
| Person | Relationships |
| Relationship interaction | Relationships |
| Diary reflection | Journal |
| Data-gathering event | Daily Capture |
| Attention reason | Attention |
| Attention delivery attempt | Attention / Delivery |
| Alarm | Attention |
| Document metadata | Documents/RAG |
| Document text chunks | Documents/RAG |
| Search index | RAG/FTS |
| Technical event | System Logs |
| Important data change | Audit |
| Why Dexter acted | Action Trace |
| Morning synthesis | Briefing Engine |
| End-of-Day synthesis | Briefing Engine |

No subsystem may silently create a competing source of truth.

---

# 4. Core Architectural Chain

The most important system relationship is:

```text
GOAL
  ↓
PROJECT
  ↓
TASK
  ↓
TASK OCCURRENCE
  ↓
SCHEDULE
  ↓
ATTENTION
  ↓
DELIVERY
  ↓
USER
  ↓
LOGGING / AUDIT
```

Not every record follows the entire chain.

For example:

```text
PERSON
  ↓
RELATIONSHIP EVENT
  ↓
SCHEDULE / TASK
  ↓
ATTENTION
```

or:

```text
DOCUMENT
  ↓
RAG
  ↓
CONTEXT
  ↓
QWEN
```

---

# 5. The Five Ownership Questions

Every feature must answer:

```text
WHEN?
→ Schedule

WHAT information?
→ Domain / Capture

WHAT does it mean?
→ Domain Service

SHOULD Dexter interrupt?
→ Attention

HOW does Dexter communicate?
→ Delivery
```

And:

```text
WHAT ACTUALLY HAPPENED?
→ Logging / Audit
```

This prevents duplicated logic.

---

# 6. Tasks / Goals / Projects

Final relationship:

```text
GOAL
  │
  │ many-to-many
  ▼
GOAL_PROJECTS
  │
  ▼
PROJECT
  │
  │ one-to-many
  ▼
TASK
  │
  ▼
TASK OCCURRENCE
```

Rules:

- A project may support multiple goals.
- A task normally belongs to zero or one project.
- A goal does not directly own tasks in v1.
- No `project_tasks` junction table.
- No `goal_tasks` table.
- Project deletion must not delete tasks.
- Task deletion must not silently delete historical occurrence meaning.

The finalized Goals & Projects design explicitly keeps this lightweight structure and excludes advanced dependency/measurement tables from v1. fileciteturn8file4L723-L740 fileciteturn8file1L257-L315

---

# 7. Important Conflict Resolution — Task Dependencies

The original Task design contains a planned `task_dependencies` table, but its document is explicitly a **Draft / Design Baseline** and says that dependency handling was still a next-finalization step. fileciteturn7file0L12-L18 fileciteturn12file0L492-L504

The finalized Goals & Projects architecture explicitly excludes `task_dependencies` from v1. fileciteturn8file1L257-L273

### Final decision

**NO `task_dependencies` table in Dexter v1.**

Future advanced hardware/model versions may add it through migration.

This is deliberate rather than accidental.

---

# 8. Tasks / Schedule Separation

```text
TASK OCCURRENCE
= what needs to be done

SCHEDULE BLOCK
= when Dexter plans to do it
```

Scheduling a task must not silently change:

- deadline;
- priority;
- recurrence;
- project;
- goal.

The Schedule subsystem explicitly owns time and preserves this separation. fileciteturn7file1L1229-L1239

---

# 9. Schedule / Attention Separation

```text
SCHEDULE
"What time?"

ATTENTION
"Does this deserve interruption?"

DELIVERY
"How do I reach the user?"
```

Schedule therefore must not contain notification logic.

Attention must not create a second calendar.

Delivery must not invent urgency.

---

# 10. Attention / Delivery Consolidation

The finalized Alerts & Attention architecture already owns:

```text
attention_events
attention_deliveries
alarms
```

and defines an attention event as the reason Dexter wants attention, while a delivery represents how Dexter attempted to get it. fileciteturn12file4L913-L956

### Important correction

The later Notifications/Alarms/Briefings specification proposed another `attention_deliveries` shape using `attention_id`.

That would create a duplicate/conflicting definition.

### Final decision

Use the original finalized Attention schema:

```text
attention_deliveries.attention_event_id
        ↓
attention_events.id
```

There is **one** attention-delivery table.

The Notifications/Alarms/Briefings subsystem is a delivery/synthesis layer, not a second attention database.

---

# 11. Daily Capture

Daily Capture is an orchestration layer.

It owns:

```text
capture_templates
capture_questions
capture_events
capture_responses
```

It does not permanently own:

```text
sleep
expenses
relationships
mood
exercise
etc.
```

Instead:

```text
Capture Response
      ↓
Validation
      ↓
Domain Service
      ↓
Domain transaction
```

This is explicitly required by the finalized Daily Capture design. fileciteturn8file8L1661-L1685

---

# 12. Capture Transaction Boundary

When a capture response becomes authoritative domain data:

```text
BEGIN
  ↓
Write domain record
  ↓
Mark response processed
  ↓
Write required audit/event
  ↓
COMMIT
```

If anything fails:

```text
ROLLBACK
```

The capture response remains recoverable.

This prevents:

```text
"Processed"
```

from being recorded when the domain write actually failed. fileciteturn8file8L1709-L1726

---

# 13. Wellbeing

Wellbeing owns structured wellbeing observations.

Examples:

```text
Sleep
Activity
Meals
Hydration
Mood
Energy
Device usage
Habits
Compulsive behaviors
```

Goals owns targets.

Therefore:

```text
GOAL
"Sleep 8 hours"

        ↑

WELLBEING
"Sleep = 6h 20m"
```

The target must not be duplicated inside every wellbeing record.

Wellbeing explicitly defines raw observations as authoritative and derived metrics as calculations. fileciteturn7file2L2249-L2257

---

# 14. Provenance

Applicable observational domains use:

```text
USER_REPORTED
DEVICE
IMPORTED
DERIVED
```

These must never be silently conflated.

For example:

```text
USER_REPORTED
"I slept 6 hours."

≠

DEVICE
"Device recorded 6 hours."

≠

DERIVED
"Average sleep this week = 6.4 hours."
```

This is especially important because the dedicated Vivo V11 is not the user's primary device. fileciteturn7file2L1566-L1588

---

# 15. Finance

Finance owns:

```text
accounts
categories
transactions
recurring transaction definitions
budgets
savings goals
savings contributions
investments
```

The transaction record is authoritative for normal money movement.

Balances and totals should be derived from authoritative records rather than duplicated wherever practical. fileciteturn7file3L2427-L2451

Money is stored as integer minor units.

Example:

```text
₹100.50
→ 10050 paise
```

Never use floating-point values for monetary amounts.

---

# 16. Relationships

Relationships owns:

```text
people
categories
relationship profile/context
interactions
connection preferences
```

It does not own:

```text
tasks
calendar
alarms
```

If a relationship follow-up becomes actionable:

```text
Relationship context
      ↓
Task
      ↓
Schedule
      ↓
Attention
```

The Relationships architecture explicitly requires actionable follow-ups to reuse Tasks rather than duplicating them. fileciteturn9file0L440-L456

---

# 17. Journal

Journal owns personal reflection.

It does not become a duplicate of:

```text
Wellbeing
Relationships
Projects
Tasks
```

Example:

```text
Wellbeing:
sleep = 5.5 hours

Journal:
"I was exhausted today."
```

Both may coexist because they answer different questions.

Normal journal search uses SQLite/FTS5; RAG is optional for broader personal reflection. fileciteturn9file2L2180-L2207 fileciteturn9file2L2269-L2333

---

# 18. Documents / RAG

Documents/RAG owns:

```text
knowledge_documents
knowledge_chunks
knowledge_tags
knowledge_document_tags
FTS index
optional semantic index
```

It does not replace structured SQLite data.

Use:

```text
SQLite
→ facts

Documents/RAG
→ unstructured knowledge
```

The finalized RAG architecture explicitly requires structured retrieval before RAG and FTS5 before optional semantic retrieval. fileciteturn12file2L703-L810

---

# 19. RAG Retrieval Hierarchy

```text
1. Structured SQLite query
        ↓
2. Metadata/title/tag search
        ↓
3. FTS5
        ↓
4. Optional semantic retrieval
        ↓
5. Re-ranking
        ↓
6. Context Builder
        ↓
7. Compact Markdown
        ↓
8. Qwen
```

Do not embed every query.

Do not embed every document.

Do not load the whole database into Qwen.

---

# 20. Markdown vs JSON vs SQLite

Final policy:

```text
SQLite
→ authoritative structured storage

Markdown
→ preferred LLM-facing context

JSON
→ machine interchange / temporary structured payloads

Original files
→ preserve when required
```

This directly preserves the finalized RAG design. fileciteturn9file1L1086-L1169

---

# 21. Briefings

Morning and End-of-Day synthesis are generated from source domains.

They are not authoritative records.

```text
Source data
    ↓
Briefing engine
    ↓
Context
    ↓
Qwen
    ↓
Validated presentation
```

If a briefing is lost, the source data remains intact.

The briefing can always be regenerated.

---

# 22. Derived Data

Derived data includes:

```text
Project progress
Attention score/priority calculations
Availability
Budget remaining
Wellbeing averages
Relationship attention recommendations
RAG ranking
Briefing context
Daily summaries
```

Where practical:

> **Calculate rather than permanently store.**

If cached, the cache must be rebuildable.

---

# 23. Daily State

The Task architecture proposed a derived `daily_state` concept.

### Final decision

A daily-state cache is **optional**, not authoritative.

Do not create it in the first schema migration.

First implement direct indexed queries.

Add a rebuildable `daily_state` cache only if profiling on the Vivo V11 proves that direct queries are insufficient.

This avoids premature denormalization.

---

# 24. System Events

The Task architecture defines an append-oriented `system_events` concept for important lifecycle events. fileciteturn7file0L640-L714

### Final decision

Use `system_events` as the **compact chronological event stream** for important domain/application events.

It is not a replacement for:

```text
system_logs
audit_logs
action_traces
```

They serve different purposes.

```text
system_events
→ meaningful domain/application events

system_logs
→ technical diagnostics

audit_logs
→ important data/configuration changes

action_traces
→ why Dexter took an automated action
```

---

# 25. Transactions

Use SQLite transactions whenever multiple authoritative records must remain consistent.

Examples:

### Complete task

```text
task_occurrence update
+
system event
+
affected attention state
+
audit event
→ one transaction
```

### Finance correction

```text
void old transaction
+
create corrected transaction
+
audit
→ one transaction
```

### Capture processing

```text
domain write
+
capture response status
+
audit/event
→ one transaction
```

---

# 26. AI Boundary

The final architecture is:

```text
USER
 ↓
Speech-to-Text
 ↓
Intent / Command Parser
 ↓
Qwen + Laya where needed
 ↓
Action Router
 ↓
Domain Service
 ↓
Validation
 ↓
SQLite Transaction
```

Never:

```text
USER
 ↓
LLM-generated SQL
 ↓
SQLite
```

The finalized subsystems repeatedly enforce this boundary. fileciteturn12file4L1463-L1497

---

# 27. Read vs Write

### Read

```text
Voice
 ↓
Intent
 ↓
Service query
 ↓
SQLite
 ↓
Optional Qwen summary
 ↓
Voice
```

### Write

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
Audit
 ↓
Confirmation
```

Potentially destructive actions require explicit confirmation.

---

# 28. No Cross-Domain Direct SQL

A domain service must not reach inside another domain's tables simply because they are convenient.

Bad:

```text
Relationship Service
→ directly modifies task tables
```

Good:

```text
Relationship Service
→ requests Task Service to create/update a task
```

This keeps ownership enforceable.

---

# 29. Cross-Domain Query Layer

For complex user questions:

```text
User request
      ↓
Intent/domain classification
      ↓
Relevant domain services
      ↓
Small structured result
      ↓
Context Builder
      ↓
Qwen
```

Example:

> "Why have I been getting less work done?"

Possible structured context:

```text
Tasks:
completion ↓

Wellbeing:
sleep ↓
energy ↓

Compulsive behavior:
distraction ↑

Schedule:
deep-work time ↓

Relationships:
no direct causal claim
```

Qwen explains the pattern without being allowed to invent causality.

---

# 30. Privacy Boundary

Default:

```text
NO CLOUD
NO TELEMETRY
NO REMOTE AI
NO AUTOMATIC SYNC
NO EXTERNAL DATABASE
```

Sensitive data remains local.

The minimum relevant context should be supplied to Qwen.

---

# 31. Privacy Classes

Recommended conceptual classes:

```text
NORMAL
PRIVATE
SENSITIVE
HIGHLY_SENSITIVE
```

Examples:

```text
NORMAL
→ ordinary task title

PRIVATE
→ goals/projects

SENSITIVE
→ finance, relationships, wellbeing

HIGHLY_SENSITIVE
→ journal details, compulsive-behavior details,
  authentication-related information if any
```

These classes guide logging, AI context, deletion, and export.

They do not require duplicating a classification column into every table in v1 unless an actual enforcement feature needs it.

---

# 32. Logging Rules Across Domains

Generic logs may contain:

```text
entity type
entity id
operation
status
timestamp
error code
```

They should not contain:

```text
full journal text
full financial records
private relationship notes
sensitive wellbeing details
document contents
voice recordings
passwords
tokens
```

The Logging/Audit subsystem explicitly requires this boundary. fileciteturn9file3L3016-L3037

---

# 33. Deletion Policy

Deletion is different from lifecycle state.

Normal lifecycle:

```text
ACTIVE
→ COMPLETED
→ ARCHIVED
```

Permanent deletion is a separate privacy operation.

The final architecture must support:

```text
User requests deletion
      ↓
Domain-specific deletion policy
      ↓
Dependent derived/indexed data removed
      ↓
Required audit treatment
      ↓
Confirmation
```

Deletion must not leave searchable orphaned document chunks, FTS records, cached embeddings, or private derived copies.

---

# 34. Audit vs User Deletion

Audit history is important, but privacy wins over technical convenience.

Therefore:

- audit records should preserve accountability where allowed;
- personal content should not be duplicated into audit records unnecessarily;
- if a user permanently deletes private data, audit records containing that private content must be deleted or redacted according to the final deletion policy;
- never use "immutable audit" as an excuse to retain private content forever.

---

# 35. Retention

### Authoritative personal data

Retain until:

```text
user deletes
OR
approved domain retention policy removes it
```

### Technical logs

Short/configurable retention.

### Audit

Longer retention, subject to privacy deletion.

### Action traces

Long enough to explain important automation; compact.

### Caches

Disposable.

### RAG indexes

Rebuildable.

### Briefing outputs

Optional/compact.

---

# 36. Storage Priority

When storage becomes constrained:

```text
1. Temporary files
2. Caches
3. Old technical logs
4. Rebuildable RAG artifacts
5. Optional derived data
6. Archived retrieval artifacts
```

Do not delete authoritative user data merely to make room for caches or logs.

The RAG architecture already establishes that storage headroom must be preserved and caches must be rebuildable. fileciteturn12file2L703-L809

---

# 37. 20 GB Storage Policy

The 20 GB ceiling applies to the **entire Dexter data layer**:

```text
SQLite
+
original documents
+
FTS/indexes
+
optional embeddings
+
logs
+
caches
+
temporary workspace
```

The system should monitor total usage.

At warning threshold:

```text
Stop non-essential growth
```

At critical threshold:

```text
Stop optional indexing
Clean rebuildable artifacts
Require user-directed cleanup
```

Never silently delete authoritative user data.

---

# 38. Derived Data Rule

A derived table must have a documented rebuild path.

Examples:

```text
FTS
→ rebuild from source text

Project progress
→ recalculate from tasks

Budget remaining
→ calculate from transactions

Morning briefing
→ regenerate from source records

Attention priority
→ reevaluate from source/context
```

If a derived store cannot be rebuilt, it must be treated as authoritative data and designed accordingly.

---

# 39. Failure Philosophy

Dexter must prefer:

```text
Known incomplete
```

over:

```text
False success
```

Examples:

```text
Capture processing failed
→ FAILED + recoverable

Alarm scheduling failed
→ FAILED + user-visible if important

RAG indexing failed
→ original document preserved

Qwen unavailable
→ deterministic fallback

Database write failed
→ transaction rollback
```

Never silently discard information.

---

# 40. Offline-First Guarantee

Core functionality must work without network access:

```text
Tasks
Schedule
Goals
Projects
Attention
Alarms
Finance
Wellbeing
Relationships
Journal
Daily Capture
SQLite search
FTS5
Briefings
Local Qwen/Laya
Logging
```

Optional future online features must not become hidden dependencies.

---

# 41. RAG Is Not the Database

The architecture deliberately keeps:

```text
SQLite
→ truth

RAG
→ retrieval
```

If RAG is corrupted:

```text
Rebuild RAG
```

Do not reconstruct authoritative data from RAG.

---

# 42. No Universal Personal Data Table

Do not create:

```text
personal_data
user_data
daily_data
everything
memory
```

as a giant permanent table.

The finalized Daily Capture design explicitly rejects a universal `daily_data` store. fileciteturn5file0L12-L21

Use proper domain ownership.

---

# 43. No Graph Database

Dexter v1 does not need a graph database.

Relational relationships are sufficient:

```text
goals ↔ projects
projects → tasks
tasks → occurrences
people → interactions
documents → chunks
```

This keeps the system lightweight and SQLite-friendly.

---

# 44. No Large JSON Data Model

JSON is permitted for:

```text
small event details
machine interchange
temporary AI context
```

Do not use JSON as the default replacement for relational columns.

This preserves queryability, validation, and storage efficiency.

---

# 45. FTS5 Strategy

FTS5 is the primary local full-text mechanism for:

```text
documents
journal
potential future free-form knowledge
```

For document RAG, external-content FTS5 is preferred so source text is not unnecessarily duplicated inside the FTS index.

SQLite's official FTS5 documentation confirms that external-content tables can avoid storing a second copy of the content, but require synchronization between the source table and the FTS index. citeturn0search0

Therefore:

```text
Source table
   ↕
FTS5 index
```

must be maintained transactionally/triggers or rebuilt when required.

---

# 46. Database Configuration

Required:

```sql
PRAGMA foreign_keys = ON;
```

Recommended initial production candidate:

```text
WAL journal mode
```

because it is well suited to normal concurrent read/write application behavior.

SQLite documents WAL as a supported journal mode and provides automatic checkpointing; the target device must still be tested before final deployment. citeturn0search2

Durability is more important than a small benchmark improvement.

Do not enable arbitrary performance pragmas without measurement.

---

# 47. Backup

Backups must not be simple blind file copies while the database is actively changing.

Use SQLite's Online Backup API or an equivalent safe snapshot mechanism.

SQLite documents the Online Backup API specifically for making consistent snapshots of a live database without holding the source read lock for the entire operation. citeturn0search1

Backup policy:

```text
Source SQLite
      ↓
Consistent snapshot
      ↓
Encrypted/local backup
      ↓
Verify
```

Backup files count toward storage unless stored outside the 20 GB Dexter data allocation.

---

# 48. Database Integrity

At maintenance/startup points, Dexter may perform:

```text
foreign-key checks
integrity checks
migration verification
FTS consistency checks where needed
```

Do not run expensive full integrity work continuously.

---

# 49. Migration Strategy

Never redesign the whole database for a new feature.

Use versioned migrations:

```text
v001
v002
v003
...
```

Each migration should:

- be deterministic;
- be testable;
- preserve existing data;
- be transactionally applied where SQLite permits;
- have a clear rollback/recovery strategy;
- update schema version only after success.

---

# 50. Cross-Domain Dependency Rules

```text
Tasks
→ may reference Projects

Task Occurrences
→ may be referenced by Schedule

Schedule
→ provides context to Attention

Domain records
→ may create Attention events

Attention
→ creates Delivery attempts

Delivery
→ records outcome

Logging/Audit
→ records important system/data events

Briefing
→ reads domains

RAG
→ reads/indexes knowledge

Qwen/Laya
→ consumes controlled context
```

Avoid reverse ownership.

---

# 51. Circular Dependency Prevention

Never allow:

```text
A → B → C → A
```

at the data ownership level.

Example:

```text
Goals
→ Projects
→ Tasks
→ Schedule
```

is valid.

But:

```text
Schedule owns Goals
Goals own Schedule
```

is not.

Cross-domain services may communicate, but ownership remains one-directional.

---

# 52. Recommended Service Layers

```text
Database Layer
    ↓
Repository Layer
    ↓
Domain Service Layer
    ↓
Cross-Domain Coordination
    ↓
Attention / Briefing / Context
    ↓
AI / Voice / UI
```

Repositories should not contain business decisions.

Domain services enforce domain rules.

Cross-domain coordination combines domains.

---

# 53. Action Router

Voice commands should pass through one controlled router.

Example:

```text
"Add ₹500 food expense"

Speech
 ↓
Intent
 ↓
Finance Action
 ↓
Validate
 ↓
Finance Service
 ↓
SQLite transaction
 ↓
Audit
 ↓
Response
```

Another:

```text
"Move my database work to 3 PM"

Speech
 ↓
Intent
 ↓
Task/Schedule Action
 ↓
Validate conflict
 ↓
Schedule Service
 ↓
SQLite transaction
 ↓
Attention reevaluation
 ↓
Audit
```

---

# 54. Attention Re-evaluation Rule

Whenever an important source condition changes:

```text
Task completed
Task deadline changed
Task rescheduled
Schedule changed
Capture missed
Finance threshold changed
Relationship event resolved
```

Dexter should reevaluate relevant Attention state.

Do not leave stale alerts active.

---

# 55. Briefing Recalculation Rule

Briefings are generated from current source state.

Therefore:

```text
Task completed at 08:30
```

should appear as completed if the 09:00 briefing is generated.

Do not rely on yesterday's cached wording.

---

# 56. Cross-Domain Consistency Example

Scenario:

```text
VVIP task
Deadline 04:00
Current 01:00
```

Flow:

```text
Task Occurrence
      ↓
Attention evaluates
      ↓
Attention Event
      ↓
Notification / Voice / Alarm
      ↓
User completes task
      ↓
Task transaction
      +
Attention completion
      +
Audit/Event
      ↓
No stale alarm
      ↓
Morning briefing sees COMPLETED
```

This is the complete consistency chain.

---

# 57. Cross-Domain Test Strategy

Test individual domains first.

Then test:

```text
Task → Schedule
Task → Attention
Task → Briefing

Capture → Finance
Capture → Wellbeing
Capture → Relationships

Relationships → Task
Relationships → Schedule
Relationships → Attention

Wellbeing → Goals
Finance → Goals

Documents → Project
Journal → Briefing

Attention → Delivery
Delivery → Logging

All domains → Morning Briefing
All domains → End-of-Day Summary
```

---

# 58. Critical Invariants

These must become automated tests.

```text
1. Every task occurrence references a valid task.

2. A recurring occurrence references the correct task series.

3. Historical occurrences are not rewritten by future recurrence changes.

4. Schedule blocks cannot silently violate HARD constraints.

5. A task schedule does not change its deadline.

6. An attention acknowledgement does not automatically mean task completion.

7. Completed source conditions cancel or resolve stale attention.

8. Capture processing cannot become PROCESSED without a successful domain write.

9. Finance transactions never use floating-point money.

10. Project deletion does not delete its tasks.

11. Journal content never enters generic logs.

12. Sensitive wellbeing/relationship/finance data is not copied into generic logs.

13. RAG never becomes the source of truth for structured facts.

14. AI cannot bypass application validation.

15. Voice cannot execute arbitrary SQL.

16. Derived data can be rebuilt.

17. Storage monitoring includes all Dexter data.

18. Core functionality works offline.

19. No domain creates a duplicate scheduler.

20. No domain creates a duplicate attention engine.
```

---

# 59. Final Cross-Domain Dependency Graph

```text
                         GOALS
                           │
                           ▼
                        PROJECTS
                           │
                           ▼
                         TASKS
                           │
                           ▼
                   TASK OCCURRENCES
                           │
                           ├──────────────┐
                           ▼              ▼
                       SCHEDULE        ATTENTION
                           │              │
                           │              ▼
                           │           DELIVERY
                           │              │
                           └──────┬───────┘
                                  ▼
                                 USER
                                  │
                                  ▼
                            LOGGING/AUDIT


DAILY CAPTURE
      │
      ├──→ FINANCE
      ├──→ WELLBEING
      └──→ RELATIONSHIPS


RELATIONSHIPS ──→ TASK / SCHEDULE / ATTENTION

WELLBEING ──→ GOALS / BRIEFING
FINANCE ──→ GOALS / ATTENTION / BRIEFING
JOURNAL ──→ BRIEFING / OPTIONAL RAG
DOCUMENTS ──→ RAG / PROJECT CONTEXT

ALL RELEVANT DOMAINS
      │
      ▼
BRIEFING ENGINE
      │
      ├──→ MORNING
      └──→ END-OF-DAY

DOCUMENTS / JOURNAL / KNOWLEDGE
      │
      ▼
RAG
      │
      ▼
CONTEXT BUILDER
      │
      ▼
QWEN / LAYA
```

---

# 60. Final Cross-Domain Rules

```text
RULE 01
Every authoritative concept has exactly one owner.

RULE 02
No domain may silently create a competing source of truth.

RULE 03
Schedule owns WHEN.

RULE 04
Daily Capture owns WHAT information is collected.

RULE 05
Domain services own WHAT collected information means.

RULE 06
Attention owns WHETHER Dexter should interrupt.

RULE 07
Delivery owns HOW Dexter communicates the interruption.

RULE 08
Briefing owns daily synthesis.

RULE 09
Logging/Audit owns system history and accountability.

RULE 10
Tasks own actionable work.

RULE 11
Task occurrences own occurrence-specific state.

RULE 12
Task series own recurrence definitions.

RULE 13
Projects provide bounded work context.

RULE 14
Goals provide higher-level direction.

RULE 15
Goals do not directly own tasks in v1.

RULE 16
No task_dependencies table is included in v1.

RULE 17
Schedule does not own tasks.

RULE 18
Attention does not own domain data.

RULE 19
Daily Capture is not a universal personal-data store.

RULE 20
No second recurrence engine may be created.

RULE 21
No second attention engine may be created.

RULE 22
There is only one attention_deliveries table.

RULE 23
Attention deliveries reference attention_events by attention_event_id.

RULE 24
Briefing records must not become authoritative source data.

RULE 25
Derived data must have a rebuild path.

RULE 26
RAG must never become the source of truth for structured data.

RULE 27
Structured retrieval must precede RAG when sufficient.

RULE 28
FTS5 must precede optional semantic retrieval.

RULE 29
Qwen/Laya never receive unrestricted database access.

RULE 30
AI output is an intent/candidate, not a trusted database mutation.

RULE 31
Application validation precedes database writes.

RULE 32
Important multi-record changes use transactions.

RULE 33
Important state changes and their corresponding history records should be atomic.

RULE 34
Sensitive information must not enter generic logs.

RULE 35
User privacy deletion must remain possible.

RULE 36
Caches and derived indexes may be removed to protect storage.

RULE 37
Authoritative user data must not be deleted merely to solve cache pressure.

RULE 38
The complete Dexter data layer must remain within the 20 GB ceiling.

RULE 39
No cloud dependency exists in core v1 functionality.

RULE 40
All core AI processing remains local.

RULE 41
No network connection is required for normal database operation.

RULE 42
Integer primary keys are preferred.

RULE 43
Unix timestamps are stored as INTEGER values.

RULE 44
Money uses integer minor units.

RULE 45
Do not use large JSON blobs for ordinary relational data.

RULE 46
Do not add indexes without a justified query/use case.

RULE 47
Do not materialize years of recurring events.

RULE 48
Do not load entire domains into Qwen context.

RULE 49
Do not duplicate full records into logs, RAG, briefings, or caches.

RULE 50
Historical records must remain historically correct.

RULE 51
Archiving is not the same as deletion.

RULE 52
A source condition changing must trigger relevant attention reevaluation.

RULE 53
Stale notifications must not survive completed source conditions.

RULE 54
Failure must remain visible and recoverable.

RULE 55
Dexter prefers known uncertainty over fabricated certainty.

RULE 56
The system must remain useful if Qwen is unavailable.

RULE 57
Critical alarms must not depend on Qwen.

RULE 58
The database must remain usable after crash/restart/power interruption.

RULE 59
The schema must be migration-driven.

RULE 60
The final SQLite architecture is the implementation contract for all v1 subsystems.
```

---

# 61. Final Status

**CROSS-DOMAIN ARCHITECTURE v1 — FINALIZED**

This specification reconciles the previously finalized subsystem documents and resolves the important ownership conflicts before the final SQLite schema is frozen.

The most important reconciliations are:

```text
No task_dependencies in v1
No duplicate attention_deliveries table
No second scheduler
No universal daily_data table
No graph database
No RAG-as-source-of-truth
No AI-direct-to-SQL
No duplicate domain records
```

The architecture is ready for the final database schema specification.
