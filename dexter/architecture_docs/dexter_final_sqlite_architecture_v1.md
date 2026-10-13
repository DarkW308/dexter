# Dexter Database — Final SQLite Architecture v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database Engine:** SQLite  
**Deployment:** Fully offline / local-only  
**Target Hardware:** Dedicated Vivo V11, Android 8  
**AI:** Qwen 2.5 0.5B + Laya  
**Storage Ceiling:** 20 GB total Dexter data layer

---

# 1. Purpose

This document is the final database contract for Dexter v1.

It consolidates the finalized subsystem specifications into **one SQLite architecture**.

It defines:

- database configuration;
- authoritative tables;
- foreign keys;
- indexes;
- full-text search;
- migration rules;
- transaction rules;
- retention/deletion boundaries;
- storage management;
- integrity checks;
- backup;
- cross-domain invariants.

This is the document a software engineer should use to implement the final Dexter database.

---

# 2. Final Design Philosophy

```text
SQLite = source of truth

Application services = business rules

Laya = orchestration

Qwen = language/reasoning assistance

FTS5 = lightweight full-text retrieval

RAG = optional retrieval layer

Logs = technical history

Audit = important data-change history

Briefings = derived presentation
```

Nothing outside SQLite is allowed to become the authoritative copy of structured Dexter data.

---

# 3. Database File Layout

Recommended:

```text
dexter/
├── dexter.db
├── documents/
├── backups/
└── temporary/
```

The exact Android filesystem location is an implementation detail.

The SQLite database remains a single logical database.

Original documents remain outside SQLite where appropriate, with metadata/path stored in SQLite.

---

# 4. Database Configuration

Every connection:

```sql
PRAGMA foreign_keys = ON;
```

Foreign-key enforcement is mandatory.

Recommended production candidate:

```sql
PRAGMA journal_mode = WAL;
```

The actual device must be tested before final deployment.

SQLite documents WAL as a supported journal mode with automatic checkpoint behavior. citeturn0search2

Durability takes priority over marginal benchmark gains.

Do not blindly enable arbitrary performance pragmas.

---

# 5. Identifier Policy

Use:

```sql
INTEGER PRIMARY KEY
```

for internal IDs.

Do not use UUID strings everywhere.

Benefits:

- compact;
- fast;
- small indexes;
- good SQLite locality;
- simple joins.

---

# 6. Timestamp Policy

Use:

```text
INTEGER
```

representing Unix epoch seconds in UTC.

Recurring rules also retain an explicit timezone where local-calendar interpretation is required.

Example:

```text
timezone = Asia/Kolkata
```

An instant and a recurring local-calendar rule are different concepts and must not be conflated.

---

# 7. Schema Version

Create:

```sql
CREATE TABLE schema_meta (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    schema_version INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Exactly one row is maintained.

Migration code updates `schema_version` only after the migration succeeds.

---

# 8. Final Table Inventory

## Tasks

```text
tasks
task_series
task_occurrences
```

## Goals / Projects

```text
goals
projects
goal_projects
```

## Schedule

```text
timetable_rules
schedule_events
schedule_exceptions
schedule_blocks
```

## Attention

```text
attention_events
attention_deliveries
alarms
```

## Daily Capture

```text
capture_templates
capture_questions
capture_events
capture_responses
```

## Wellbeing

```text
wellbeing_sleep
wellbeing_activity
wellbeing_meals
wellbeing_hydration
wellbeing_mood
wellbeing_energy
wellbeing_device_usage
wellbeing_habits
wellbeing_habit_logs
wellbeing_compulsive_behaviors
wellbeing_compulsive_behavior_logs
```

## Finance

```text
finance_accounts
finance_categories
finance_transactions
finance_recurring_transactions
finance_budgets
finance_savings_goals
finance_savings_contributions
finance_investments
```

## Relationships

```text
relationship_categories
relationship_people
relationship_connection_preferences
relationship_interactions
```

Relationship events reuse Schedule instead of creating a second event/calendar table.

## Journal

```text
journal_entries
```

## Documents / RAG

```text
knowledge_documents
knowledge_chunks
knowledge_tags
knowledge_document_tags
knowledge_chunks_fts
```

Optional future:

```text
semantic/vector index
```

## Logging / Audit

```text
system_events
system_logs
audit_logs
action_traces
```

## Briefings

```text
briefing_runs
```

Briefings are derived artifacts and do not become a second source of truth.

---

# 9. Tables Explicitly NOT Included

The following are intentionally excluded from v1:

```text
daily_data
user_memory_universal
task_dependencies
project_tasks
goal_tasks
milestones
goal_metrics
goal_measurements
progress_history
project_dependencies
graph tables
attention_scores
quiet_hours
notification_queue
voice_queue
acknowledgement table
snooze table
briefing_items
motivation table
large embedding table by default
```

They can be added through future migrations when real usage justifies them.

---

# 10. TASKS

## 10.1 `tasks`

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

    project_id INTEGER,

    estimated_duration_seconds INTEGER
        CHECK (
            estimated_duration_seconds IS NULL
            OR estimated_duration_seconds > 0
        ),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    archived_at INTEGER,

    FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE SET NULL
);
```

`project_id` is nullable.

Project deletion must not delete the task.

---

# 11. `task_series`

```sql
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
```

---

# 12. `task_occurrences`

```sql
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

The occurrence must validate `series_id` against the same task in application logic or with the composite relationship defined during implementation.

---

# 13. Task Indexes

```sql
CREATE INDEX idx_tasks_archived
ON tasks(archived_at);

CREATE INDEX idx_tasks_project
ON tasks(project_id);

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

Do not store `OVERDUE`.

It is derived from current time and occurrence state.

---

# 14. GOALS

```sql
CREATE TABLE goals (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,
    description TEXT,

    goal_type TEXT NOT NULL DEFAULT 'OUTCOME'
        CHECK (goal_type IN (
            'OUTCOME',
            'HABIT',
            'FINANCIAL',
            'LEARNING',
            'HEALTH',
            'PERSONAL',
            'PROFESSIONAL',
            'OTHER'
        )),

    priority TEXT NOT NULL DEFAULT 'NORMAL'
        CHECK (priority IN (
            'LOW',
            'NORMAL',
            'HIGH',
            'CRITICAL'
        )),

    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN (
            'ACTIVE',
            'PAUSED',
            'COMPLETED',
            'CANCELLED',
            'ARCHIVED'
        )),

    start_at INTEGER,
    target_at INTEGER,
    completed_at INTEGER,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

No generic goal progress column.

---

# 15. PROJECTS

```sql
CREATE TABLE projects (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,
    description TEXT,

    status TEXT NOT NULL DEFAULT 'PLANNED'
        CHECK (status IN (
            'PLANNED',
            'ACTIVE',
            'ON_HOLD',
            'BLOCKED',
            'COMPLETED',
            'CANCELLED',
            'ARCHIVED'
        )),

    priority TEXT NOT NULL DEFAULT 'NORMAL'
        CHECK (priority IN (
            'LOW',
            'NORMAL',
            'HIGH',
            'CRITICAL'
        )),

    start_at INTEGER,
    target_at INTEGER,
    completed_at INTEGER,

    manual_progress INTEGER
        CHECK (
            manual_progress IS NULL
            OR manual_progress BETWEEN 0 AND 100
        ),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

---

# 16. GOAL ↔ PROJECT

```sql
CREATE TABLE goal_projects (
    goal_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,

    relationship_type TEXT NOT NULL DEFAULT 'PRIMARY'
        CHECK (relationship_type IN (
            'PRIMARY',
            'SECONDARY'
        )),

    created_at INTEGER NOT NULL,

    PRIMARY KEY (goal_id, project_id),

    FOREIGN KEY (goal_id)
        REFERENCES goals(id)
        ON DELETE CASCADE,

    FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE CASCADE
);
```

A project may contribute to multiple goals.

---

# 17. Goals / Projects Indexes

```sql
CREATE INDEX idx_goals_status
ON goals(status);

CREATE INDEX idx_goals_priority
ON goals(priority);

CREATE INDEX idx_goals_target
ON goals(target_at);

CREATE INDEX idx_projects_status
ON projects(status);

CREATE INDEX idx_projects_priority
ON projects(priority);

CREATE INDEX idx_projects_target
ON projects(target_at);

CREATE INDEX idx_goal_projects_project
ON goal_projects(project_id);
```

---

# 18. SCHEDULE

## `timetable_rules`

```sql
CREATE TABLE timetable_rules (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,
    recurrence_rule TEXT NOT NULL,

    start_time INTEGER NOT NULL,
    end_time INTEGER NOT NULL,

    constraint_level TEXT NOT NULL DEFAULT 'PREFERRED'
        CHECK (constraint_level IN (
            'HARD',
            'PREFERRED',
            'FLEXIBLE'
        )),

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

Overnight rules are supported by interpreting `end_time < start_time` as crossing midnight.

---

# 19. `schedule_events`

```sql
CREATE TABLE schedule_events (
    id INTEGER PRIMARY KEY,

    title TEXT NOT NULL,
    description TEXT,

    start_at INTEGER NOT NULL,
    end_at INTEGER NOT NULL,

    constraint_level TEXT NOT NULL DEFAULT 'HARD'
        CHECK (constraint_level IN (
            'HARD',
            'PREFERRED',
            'FLEXIBLE'
        )),

    recurrence_rule TEXT,

    location TEXT,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    CHECK (end_at > start_at)
);
```

No `event_type` column is required in v1.

---

# 20. `schedule_exceptions`

```sql
CREATE TABLE schedule_exceptions (
    id INTEGER PRIMARY KEY,

    timetable_rule_id INTEGER,

    start_at INTEGER NOT NULL,
    end_at INTEGER NOT NULL,

    action TEXT NOT NULL
        CHECK (action IN (
            'CANCEL',
            'REPLACE',
            'MODIFY'
        )),

    replacement_start_at INTEGER,
    replacement_end_at INTEGER,

    constraint_level TEXT
        CHECK (
            constraint_level IS NULL
            OR constraint_level IN (
                'HARD',
                'PREFERRED',
                'FLEXIBLE'
            )
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

---

# 21. `schedule_blocks`

```sql
CREATE TABLE schedule_blocks (
    id INTEGER PRIMARY KEY,

    title TEXT NOT NULL,

    start_at INTEGER NOT NULL,
    end_at INTEGER NOT NULL,

    constraint_level TEXT NOT NULL DEFAULT 'FLEXIBLE'
        CHECK (constraint_level IN (
            'HARD',
            'PREFERRED',
            'FLEXIBLE'
        )),

    task_occurrence_id INTEGER,

    created_by TEXT NOT NULL DEFAULT 'SYSTEM'
        CHECK (created_by IN (
            'USER',
            'SYSTEM',
            'ASSISTANT'
        )),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (task_occurrence_id)
        REFERENCES task_occurrences(id)
        ON DELETE RESTRICT,

    CHECK (end_at > start_at)
);
```

Availability is derived rather than stored as a giant daily table.

---

# 22. Schedule Indexes

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

---

# 23. ATTENTION

## `attention_events`

```sql
CREATE TABLE attention_events (
    id INTEGER PRIMARY KEY,

    source_type TEXT NOT NULL,
    source_id INTEGER NOT NULL,

    title TEXT NOT NULL,

    importance TEXT NOT NULL DEFAULT 'NORMAL'
        CHECK (importance IN (
            'LOW',
            'NORMAL',
            'HIGH',
            'CRITICAL'
        )),

    urgency TEXT NOT NULL DEFAULT 'NORMAL'
        CHECK (urgency IN (
            'LOW',
            'NORMAL',
            'HIGH',
            'CRITICAL'
        )),

    due_at INTEGER,

    state TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (state IN (
            'PENDING',
            'ACKNOWLEDGED',
            'SNOOZED',
            'DISMISSED',
            'EXPIRED',
            'COMPLETED'
        )),

    next_evaluation_at INTEGER,
    expires_at INTEGER,
    acknowledged_at INTEGER,
    completed_at INTEGER,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

`source_type/source_id` is intentionally polymorphic.

SQLite cannot enforce a foreign key to multiple possible source tables.

The responsible domain service must validate the source.

---

# 24. `attention_deliveries`

```sql
CREATE TABLE attention_deliveries (
    id INTEGER PRIMARY KEY,

    attention_event_id INTEGER NOT NULL,

    delivery_type TEXT NOT NULL
        CHECK (delivery_type IN (
            'NOTIFICATION',
            'VOICE',
            'ALARM'
        )),

    scheduled_at INTEGER NOT NULL,
    delivered_at INTEGER,

    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
            'PENDING',
            'DELIVERED',
            'FAILED',
            'CANCELLED'
        )),

    acknowledged_at INTEGER,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (attention_event_id)
        REFERENCES attention_events(id)
        ON DELETE CASCADE
);
```

This is the **single authoritative delivery table**.

---

# 25. `alarms`

```sql
CREATE TABLE alarms (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,

    alarm_time INTEGER NOT NULL,
    recurrence_rule TEXT,

    enabled INTEGER NOT NULL DEFAULT 1
        CHECK (enabled IN (0,1)),

    alarm_type TEXT NOT NULL DEFAULT 'WAKE'
        CHECK (alarm_type IN (
            'WAKE',
            'REMINDER',
            'CUSTOM'
        )),

    label TEXT,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Critical alarm scheduling must not depend on Qwen.

---

# 26. Attention Indexes

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

No stored attention score.

---

# 27. DAILY CAPTURE

## `capture_templates`

```sql
CREATE TABLE capture_templates (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,
    description TEXT,

    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN (
            'ACTIVE',
            'PAUSED',
            'ARCHIVED'
        )),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

---

# 28. `capture_questions`

```sql
CREATE TABLE capture_questions (
    id INTEGER PRIMARY KEY,

    template_id INTEGER NOT NULL,

    question TEXT NOT NULL,

    answer_type TEXT NOT NULL
        CHECK (answer_type IN (
            'BOOLEAN',
            'NUMBER',
            'TEXT',
            'CHOICE',
            'MULTI_CHOICE',
            'DURATION',
            'TIME',
            'DATE',
            'CURRENCY'
        )),

    domain TEXT NOT NULL
        CHECK (domain IN (
            'GENERAL',
            'WELLBEING',
            'FINANCE',
            'RELATIONSHIPS',
            'JOURNAL',
            'TASKS',
            'OTHER'
        )),

    required INTEGER NOT NULL DEFAULT 0
        CHECK (required IN (0,1)),

    display_order INTEGER NOT NULL DEFAULT 0,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (template_id)
        REFERENCES capture_templates(id)
        ON DELETE CASCADE
);
```

---

# 29. `capture_events`

```sql
CREATE TABLE capture_events (
    id INTEGER PRIMARY KEY,

    template_id INTEGER NOT NULL,

    scheduled_at INTEGER NOT NULL,
    started_at INTEGER,
    completed_at INTEGER,
    expires_at INTEGER,

    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
            'PENDING',
            'IN_PROGRESS',
            'COMPLETED',
            'SKIPPED',
            'MISSED',
            'EXPIRED',
            'CANCELLED'
        )),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (template_id)
        REFERENCES capture_templates(id)
        ON DELETE RESTRICT
);
```

---

# 30. `capture_responses`

```sql
CREATE TABLE capture_responses (
    id INTEGER PRIMARY KEY,

    capture_event_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,

    answer TEXT,

    answered_at INTEGER,

    processing_status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (processing_status IN (
            'PENDING',
            'PROCESSED',
            'FAILED',
            'SKIPPED'
        )),

    processed_at INTEGER,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (capture_event_id)
        REFERENCES capture_events(id)
        ON DELETE CASCADE,

    FOREIGN KEY (question_id)
        REFERENCES capture_questions(id)
        ON DELETE RESTRICT,

    UNIQUE(capture_event_id, question_id)
);
```

---

# 31. Capture Indexes

```sql
CREATE INDEX idx_capture_questions_template
ON capture_questions(template_id);

CREATE INDEX idx_capture_events_status
ON capture_events(status);

CREATE INDEX idx_capture_events_scheduled
ON capture_events(scheduled_at);

CREATE INDEX idx_capture_events_template
ON capture_events(template_id);

CREATE INDEX idx_capture_responses_event
ON capture_responses(capture_event_id);

CREATE INDEX idx_capture_responses_processing
ON capture_responses(processing_status);
```

Historical question meaning must be preserved.

Implementation should snapshot the applicable question set at event creation or use immutable template versions.

---

# 32. WELLBEING

The finalized Wellbeing tables are:

```text
wellbeing_sleep
wellbeing_activity
wellbeing_meals
wellbeing_hydration
wellbeing_mood
wellbeing_energy
wellbeing_device_usage
wellbeing_habits
wellbeing_habit_logs
wellbeing_compulsive_behaviors
wellbeing_compulsive_behavior_logs
```

They remain separate rather than being compressed into one universal wellbeing table.

The finalized architecture uses provenance:

```text
USER_REPORTED
DEVICE
IMPORTED
DERIVED
```

and explicitly keeps compulsive-behavior tracking non-clinical. fileciteturn7file2L1566-L1606 fileciteturn7file2L1868-L1942

---

# 33. Wellbeing Indexes

```sql
CREATE INDEX idx_wellbeing_sleep_started
ON wellbeing_sleep(started_at);

CREATE INDEX idx_wellbeing_activity_started
ON wellbeing_activity(started_at);

CREATE INDEX idx_wellbeing_meals_eaten
ON wellbeing_meals(eaten_at);

CREATE INDEX idx_wellbeing_hydration_recorded
ON wellbeing_hydration(recorded_at);

CREATE INDEX idx_wellbeing_mood_recorded
ON wellbeing_mood(recorded_at);

CREATE INDEX idx_wellbeing_energy_recorded
ON wellbeing_energy(recorded_at);

CREATE INDEX idx_wellbeing_device_usage_date
ON wellbeing_device_usage(usage_date);

CREATE INDEX idx_wellbeing_habit_logs_recorded
ON wellbeing_habit_logs(recorded_at);

CREATE INDEX idx_wellbeing_habit_logs_habit
ON wellbeing_habit_logs(habit_id);

CREATE INDEX idx_wellbeing_compulsive_logs_recorded
ON wellbeing_compulsive_behavior_logs(occurred_at);

CREATE INDEX idx_wellbeing_compulsive_logs_behavior
ON wellbeing_compulsive_behavior_logs(behavior_id);
```

---

# 34. FINANCE

Final Finance tables:

```text
finance_accounts
finance_categories
finance_transactions
finance_recurring_transactions
finance_budgets
finance_savings_goals
finance_savings_contributions
finance_investments
```

The complete Finance v1 design is intentionally simple and offline-first. fileciteturn12file1L539-L557

---

# 35. Finance Storage Rule

Money:

```text
INTEGER minor units
```

Example:

```text
₹350.00
→ 35000 paise
```

Never store money as floating-point.

Transfers are not expenses or income.

---

# 36. Finance Referential Rules

```text
finance_transactions.account_id
→ finance_accounts.id

finance_transactions.transfer_account_id
→ finance_accounts.id

finance_transactions.category_id
→ finance_categories.id

finance_recurring_transactions.account_id
→ finance_accounts.id

finance_recurring_transactions.category_id
→ finance_categories.id

finance_budgets.category_id
→ finance_categories.id

finance_savings_contributions.goal_id
→ finance_savings_goals.id

finance_savings_contributions.account_id
→ finance_accounts.id
```

Use `ON DELETE RESTRICT` for authoritative financial history.

---

# 37. RELATIONSHIPS

Final tables:

```text
relationship_categories
relationship_people
relationship_connection_preferences
relationship_interactions
```

No relationship-specific calendar table.

Important dates and actionable follow-ups reuse Schedule and Tasks.

The finalized Relationships design explicitly uses this boundary. fileciteturn9file0L403-L456

---

# 38. Relationship Indexes

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

# 39. JOURNAL

```sql
CREATE TABLE journal_entries (
    id INTEGER PRIMARY KEY,

    entry_date INTEGER NOT NULL,

    title TEXT,
    content TEXT NOT NULL,

    mood TEXT,
    tags TEXT,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    is_archived INTEGER NOT NULL DEFAULT 0
        CHECK (is_archived IN (0,1))
);
```

Journal remains free-form and lightweight. fileciteturn9file2L2076-L2098

---

# 40. Journal FTS5

Use a rebuildable FTS5 index.

Recommended:

```sql
CREATE VIRTUAL TABLE journal_entries_fts
USING fts5(
    title,
    content,
    tags,
    content='journal_entries',
    content_rowid='id'
);
```

The FTS index is derived.

It must never become the authoritative journal store.

---

# 41. DOCUMENTS

## `knowledge_documents`

```sql
CREATE TABLE knowledge_documents (
    id INTEGER PRIMARY KEY,

    title TEXT NOT NULL,
    document_type TEXT NOT NULL,

    storage_path TEXT NOT NULL,

    knowledge_class TEXT NOT NULL
        CHECK (knowledge_class IN (
            'CRITICAL',
            'PERSONAL',
            'WORK',
            'PROJECT',
            'REFERENCE',
            'ARCHIVE'
        )),

    mime_type TEXT,

    file_size_bytes INTEGER NOT NULL DEFAULT 0,

    content_hash TEXT,

    project_id INTEGER,

    created_at INTEGER NOT NULL,
    modified_at INTEGER NOT NULL,
    indexed_at INTEGER,

    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN (
            'ACTIVE',
            'ARCHIVED',
            'DELETED',
            'FAILED'
        )),

    summary TEXT,

    FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE SET NULL
);
```

---

# 42. `knowledge_chunks`

```sql
CREATE TABLE knowledge_chunks (
    id INTEGER PRIMARY KEY,

    document_id INTEGER NOT NULL,

    chunk_index INTEGER NOT NULL,

    heading TEXT,

    content TEXT NOT NULL,

    token_estimate INTEGER,

    content_hash TEXT,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (document_id)
        REFERENCES knowledge_documents(id)
        ON DELETE CASCADE,

    UNIQUE(document_id, chunk_index)
);
```

---

# 43. Knowledge Tags

```sql
CREATE TABLE knowledge_tags (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL UNIQUE,

    created_at INTEGER NOT NULL
);

CREATE TABLE knowledge_document_tags (
    document_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,

    PRIMARY KEY(document_id, tag_id),

    FOREIGN KEY (document_id)
        REFERENCES knowledge_documents(id)
        ON DELETE CASCADE,

    FOREIGN KEY (tag_id)
        REFERENCES knowledge_tags(id)
        ON DELETE CASCADE
);
```

---

# 44. Document FTS5

Use external content:

```sql
CREATE VIRTUAL TABLE knowledge_chunks_fts
USING fts5(
    content,
    heading,
    content='knowledge_chunks',
    content_rowid='id'
);
```

SQLite's FTS5 documentation confirms that external-content tables can avoid a second stored copy of the content, but the index must be kept synchronized with the source table. citeturn0search0

Use triggers or an equivalent transactional index-maintenance mechanism.

---

# 45. FTS Consistency

When a chunk changes:

```text
source change
   +
FTS update
```

must occur atomically where practical.

If inconsistency is detected:

```text
FTS rebuild
```

from authoritative source tables.

Never repair the source table from FTS.

---

# 46. LOGGING / AUDIT

## `system_events`

```sql
CREATE TABLE system_events (
    id INTEGER PRIMARY KEY,

    event_type TEXT NOT NULL,
    entity_type TEXT,
    entity_id INTEGER,

    occurred_at INTEGER NOT NULL,

    payload TEXT,

    created_at INTEGER NOT NULL
);
```

`payload` must remain compact.

Do not copy full entities into events.

---

# 47. `system_logs`

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

---

# 48. `audit_logs`

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

Audit payloads must be minimized.

Do not place private content into before/after JSON unless necessary.

---

# 49. `action_traces`

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

This records:

```text
Trigger
→ Decision
→ Action
→ Result
```

for important Dexter automation.

---

# 50. Logging Indexes

```sql
CREATE INDEX idx_system_events_entity_time
ON system_events(entity_type, entity_id, occurred_at);

CREATE INDEX idx_system_events_time
ON system_events(occurred_at);

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

# 51. BRIEFING RUNS

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

The briefing is a generated artifact.

Source domains remain authoritative.

---

# 52. Briefing Does Not Duplicate Data

Do not store:

```text
all tasks
all schedule records
all finance records
all wellbeing records
```

inside `briefing_runs`.

Store only:

```text
status
timing
compact final result where useful
```

The briefing can be regenerated from source data.

---

# 53. Foreign-Key Dependency Order

Creation order should respect dependencies:

```text
1. schema_meta

2. independent domain tables
   goals
   projects
   relationship_categories
   finance_accounts
   finance_categories
   capture_templates
   timetable_rules

3. task tables
   tasks
   task_series
   task_occurrences

4. goal_projects

5. schedule events/exceptions/blocks

6. attention

7. capture questions/events/responses

8. wellbeing dependent tables

9. finance dependent tables

10. relationship dependent tables

11. journal

12. knowledge documents/chunks/tags

13. system events/logs/audit/traces

14. briefing_runs

15. FTS5 indexes/triggers
```

Actual migration files may group these differently as long as dependencies are respected.

---

# 54. Foreign-Key Policy

Mandatory:

```sql
PRAGMA foreign_keys = ON;
```

Use:

```text
CASCADE
```

only when the child has no independent historical meaning.

Use:

```text
RESTRICT
```

for authoritative historical records.

Use:

```text
SET NULL
```

when removing a parent should preserve the child record.

Examples:

```text
Project → Task
SET NULL

Document → Chunk
CASCADE

Finance Account → Transaction
RESTRICT

Relationship Person → Interaction
RESTRICT
```

---

# 55. Transaction Policy

Every mutation that crosses ownership boundaries must be transactional.

Examples:

```text
Task completion
+ event
+ attention resolution
+ audit
```

```text
Capture processing
+ domain write
+ response status
+ event/audit
```

```text
Document deletion
+ metadata state
+ chunk deletion
+ FTS deletion
+ audit
```

---

# 56. AI Mutation Boundary

Never allow:

```text
Qwen → SQL
```

Instead:

```text
Qwen
 ↓
Structured intent
 ↓
Action Router
 ↓
Domain Service
 ↓
Validation
 ↓
Repository
 ↓
SQLite
```

Destructive operations require explicit confirmation.

---

# 57. Read Boundary

Read operations:

```text
Qwen/Laya
→ controlled service query
→ compact structured result
```

Never:

```text
Qwen
→ SELECT *
→ entire database
```

---

# 58. RAG Boundary

RAG is:

```text
retrieval
+
context construction
```

not:

```text
database
```

The authoritative document metadata and chunks remain in SQLite.

Original files remain under controlled local storage.

---

# 59. Storage Accounting

The application must track:

```text
SQLite database
SQLite WAL/shm temporary state where relevant
Documents
FTS indexes
Optional semantic indexes
Caches
Temporary extraction files
Backups
```

The storage monitor must calculate the complete Dexter allocation.

---

# 60. 20 GB Enforcement

Recommended conceptual thresholds:

```text
NORMAL
< configured warning threshold

WARNING
→ stop non-essential growth

CRITICAL
→ stop optional indexing
→ clean caches
→ request user-directed cleanup
```

Do not hard-code exact percentages until actual storage behavior on the Vivo V11 is measured.

---

# 61. RAG Storage Strategy

Priority:

```text
Original document
        ↓
SQLite metadata
        ↓
Chunks
        ↓
FTS
        ↓
Optional embeddings
```

If storage pressure occurs:

```text
Delete caches
        ↓
Remove optional embeddings
        ↓
Reduce/archive retrieval artifacts
        ↓
Preserve originals + authoritative SQLite
```

---

# 62. Backup

Use SQLite's Online Backup API or an equivalent safe snapshot strategy rather than copying a changing database blindly.

SQLite documents the Online Backup API as a mechanism for producing a consistent snapshot of a live database. citeturn0search1

Backup policy:

```text
Database
 ↓
Consistent snapshot
 ↓
Verify
 ↓
Store locally
```

Backup storage counts toward the configured storage allocation if kept inside Dexter's storage boundary.

---

# 63. Crash Recovery

On startup:

```text
Open database
 ↓
Foreign keys enabled
 ↓
Check schema version
 ↓
Complete/recover migrations if necessary
 ↓
Check pending recoverable operations
 ↓
Record recovery event
 ↓
Continue
```

SQLite transactions provide atomicity for committed changes.

---

# 64. Integrity Checks

Maintenance should include:

```text
PRAGMA foreign_key_check;
PRAGMA integrity_check;
```

Frequency should be configurable and appropriate for device performance.

Do not execute full integrity checks continuously.

---

# 65. WAL Checkpointing

If WAL mode is used:

```text
normal writes
 ↓
WAL grows
 ↓
checkpoint
 ↓
main database
```

SQLite documents automatic checkpointing behavior and provides explicit checkpoint controls. citeturn0search2

Dexter should monitor unusual WAL growth as part of system health.

---

# 66. Migration Rules

Every schema change gets a version.

Example:

```text
v001_initial
v002_goals_projects
v003_schedule
v004_attention
...
```

Rules:

- never edit an already-applied migration;
- create a new migration;
- verify schema after migration;
- preserve data;
- log migration success/failure;
- test migration on a copy before production use.

---

# 67. Testing Levels

## Unit

```text
validation
state transitions
calculations
parsers
```

## Repository

```text
CRUD
constraints
foreign keys
indexes
```

## Transaction

```text
commit
rollback
crash-like interruption
```

## Integration

```text
Task → Schedule → Attention → Delivery

Capture → Domain

Domain → Briefing

Document → Chunk → FTS

Voice → Action Router → Service → SQLite
```

## Device

```text
Vivo V11
Android 8
screen off
device idle
restart
low storage
low RAM
```

---

# 68. Critical Database Invariants

```text
1. Foreign keys are enabled.

2. Every task occurrence references a valid task.

3. Recurring occurrences belong to the correct task series.

4. Historical occurrences are immutable with respect to future recurrence changes.

5. Overdue is derived.

6. Project deletion does not delete tasks.

7. Goal/project relationships are unique.

8. Schedule blocks reference valid task occurrences.

9. Schedule blocks have valid time ranges.

10. HARD schedule constraints cannot be silently displaced.

11. Attention sources are validated by their owning service.

12. Attention delivery references a valid attention event.

13. Completing a source resolves relevant attention.

14. Capture responses cannot be processed without successful domain writes.

15. One capture event/question has at most one response in v1.

16. Finance amounts are integer minor units.

17. Transfers are not income/expense.

18. Financial history is not silently overwritten.

19. Relationship interactions are never invented.

20. Journal content is not copied to generic logs.

21. RAG indexes are derived from authoritative content.

22. FTS inconsistency is repairable from source data.

23. AI cannot directly mutate SQLite.

24. Important mutations are transactional.

25. Derived data can be rebuilt.

26. Core functionality works offline.

27. Storage growth is bounded.

28. User deletion remains possible.

29. No duplicate scheduler exists.

30. No duplicate attention engine exists.
```

---

# 69. Final System Architecture

```text
                              DEXTER
                                │
                  ┌─────────────┴─────────────┐
                  │      APPLICATION          │
                  │                           │
                  │ Intent / Action Router    │
                  │ Domain Services           │
                  │ Cross-Domain Coordinator  │
                  │ Context Builder           │
                  │ Briefing Engine           │
                  └─────────────┬─────────────┘
                                │
                                ▼
                         SQLITE SOURCE
                           OF TRUTH
                                │
     ┌────────┬────────┬────────┼────────┬─────────┬────────┐
     ▼        ▼        ▼        ▼        ▼         ▼        ▼
   TASKS   SCHEDULE  GOALS   PROJECTS  FINANCE  WELLBEING RELATIONSHIPS
     │        │        │        │        │         │        │
     └────────┴────────┴────────┴────────┴─────────┴────────┘
                                │
                                ▼
                         DAILY CAPTURE
                                │
                ┌───────────────┼───────────────┐
                ▼               ▼               ▼
             JOURNAL        DOCUMENTS         ATTENTION
                               / RAG               │
                                  │                 ▼
                                  │              DELIVERY
                                  │                 │
                                  └────────┬────────┘
                                           ▼
                                          USER
                                           │
                                           ▼
                                     LOGGING/AUDIT

All structured data
        │
        ▼
Context Builder
        │
        ▼
Compact Markdown
        │
        ▼
Qwen 2.5 0.5B
        │
        ▼
Laya / Application
```

---

# 70. Final Implementation Order

```text
PHASE 1 — FOUNDATION
1. schema_meta
2. SQLite connection layer
3. foreign keys
4. transaction helper
5. migration engine
6. integrity/health checks

PHASE 2 — CORE WORK
7. tasks
8. task_series
9. task_occurrences
10. goals
11. projects
12. goal_projects

PHASE 3 — TIME
13. timetable_rules
14. schedule_events
15. schedule_exceptions
16. schedule_blocks

PHASE 4 — ATTENTION
17. attention_events
18. attention_deliveries
19. alarms

PHASE 5 — CAPTURE
20. capture_templates
21. capture_questions
22. capture_events
23. capture_responses

PHASE 6 — PERSONAL DOMAINS
24. wellbeing
25. finance
26. relationships
27. journal

PHASE 7 — KNOWLEDGE
28. knowledge_documents
29. knowledge_chunks
30. knowledge_tags
31. knowledge_document_tags
32. FTS5
33. indexing worker

PHASE 8 — ACCOUNTABILITY
34. system_events
35. system_logs
36. audit_logs
37. action_traces

PHASE 9 — SYNTHESIS
38. briefing_runs
39. morning briefing
40. end-of-day summary

PHASE 10 — HARDENING
41. migrations
42. backup
43. integrity checks
44. storage monitor
45. crash recovery
46. performance profiling
47. Vivo V11 testing
48. full cross-domain integration tests
```

---

# 71. What Must Not Be Implemented Yet

Do not add merely because they may be useful later:

```text
Graph database
Cloud sync
Cloud vector DB
Automatic social scraping
Automatic message sending
Medical diagnosis
Clinical addiction detection
Complex task dependency graph
Large semantic index
Multiple embedding models
Large re-ranking model
Distributed services
Microservice architecture
Multi-device synchronization
Large analytics warehouse
```

The architecture is intentionally designed so these can be added later without destroying the core.

---

# 72. Final Decision on SQLite vs Markdown vs JSON

```text
                    PERSISTENCE
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
           SQLite              Original files
         structured              documents
              │
              ▼
       retrieval/context
              │
              ▼
          Markdown
       LLM-facing context
              │
              ▼
            Qwen

JSON:
machine interchange / temporary structured payloads
```

Therefore:

> **Do not replace SQLite with Markdown or JSON.**

Use SQLite for truth.

Use Markdown to make the small model understand the right information efficiently.

---

# 73. Final Status

**FINAL SQLITE ARCHITECTURE v1 — FINALIZED**

The database architecture is now the consolidated implementation contract for Dexter v1.

It preserves the finalized subsystem decisions while resolving the two important cross-document conflicts:

```text
1. No task_dependencies in v1.
2. One authoritative attention_deliveries table.
```

It also establishes:

```text
Single source of truth
Clear ownership
Foreign-key discipline
Transactional writes
Offline operation
FTS5 retrieval
Optional semantic retrieval
20 GB storage enforcement
Privacy/deletion boundaries
Backup strategy
Crash recovery
Migration strategy
AI safety boundary
Cross-domain invariants
Vivo V11 performance constraints
```

The architecture is deliberately small, normalized, deterministic where reliability matters, and extensible toward a future advanced model/hardware generation.
