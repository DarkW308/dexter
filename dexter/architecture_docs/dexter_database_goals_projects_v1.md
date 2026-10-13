# Dexter Database Architecture — Goals & Projects v1

**Status:** FINALIZED  
**Database:** SQLite  
**Architecture:** Offline-first, local-only, privacy-first  
**Target hardware:** Low-RAM / older Android device  
**AI:** Qwen 2.5 0.5B + Laya  
**Model:** Hybrid v1, with a migration path to an advanced model later

---

## 1. Purpose

The Goals & Projects subsystem gives Dexter context about **why work is being done** and how individual tasks contribute to larger objectives.

The hierarchy is:

```text
GOAL
  ↓
PROJECT
  ↓
TASK
  ↓
SCHEDULE
  ↓
ATTENTION
```

The subsystem must remain lightweight enough for an old Android device while allowing future expansion.

---

# 2. Core Concepts

## Goal

A desired outcome or direction.

Examples:

```text
Build Dexter
Improve financial stability
Improve fitness
Maintain relationships
Learn Python
```

A goal may be long-term and does not necessarily have a meaningful percentage completion.

## Project

A bounded body of work that contributes to one or more goals.

Example:

```text
Goal:
Build Dexter

Project:
Database Architecture
```

## Task

A concrete actionable unit of work.

Example:

```text
Finalize Finance Architecture
```

Tasks are owned by the Task subsystem, not duplicated inside Projects.

---

# 3. Final Architecture

```text
                     GOAL
                       │
                 goal_projects
                  /                            ▼             ▼
             PROJECT        PROJECT
                │
                ▼
              TASK
                │
                ▼
             SCHEDULE
                │
                ▼
            ATTENTION
```

A project may contribute to multiple goals.

A task normally belongs to one project or no project.

---

# 4. v1 Tables

Required tables:

```text
goals
projects
goal_projects
```

Existing table modification:

```text
tasks
  + project_id
```

No additional project-management tables are required in v1.

---

# 5. Goals Table

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

## Field meanings

### `id`

SQLite integer primary key.

### `name`

Short goal name.

### `description`

Optional detailed description.

### `goal_type`

Organizational category.

It is not intended to contain domain-specific business logic.

### `priority`

```text
LOW
NORMAL
HIGH
CRITICAL
```

Represents how important the goal is.

### `status`

```text
ACTIVE
PAUSED
COMPLETED
CANCELLED
ARCHIVED
```

### `start_at`

Optional goal start timestamp.

### `target_at`

Optional target/deadline timestamp.

### `completed_at`

Timestamp at which the goal was completed.

### `created_at`

Creation timestamp.

### `updated_at`

Last modification timestamp.

---

# 6. Projects Table

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

## Project states

### PLANNED

Project exists but work has not started.

### ACTIVE

Currently being worked on.

### ON_HOLD

Temporarily paused.

### BLOCKED

Cannot proceed because of an external dependency, missing information, hardware/software limitation, etc.

### COMPLETED

Project finished.

### CANCELLED

Project deliberately abandoned.

### ARCHIVED

Historical project retained but no longer active.

---

# 7. Manual Progress

`manual_progress` is optional.

```text
NULL
```

means:

> Dexter should calculate progress from tasks.

Example:

```text
manual_progress = 80
```

means:

> The user explicitly considers the project approximately 80% complete.

This produces the hybrid progress model.

---

# 8. Goal Projects Relationship

Projects may contribute to multiple goals.

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

## Example

```text
Goal A ──┐
         ├── Project X
Goal B ──┘
```

This supports a project contributing to more than one objective without requiring a graph database.

---

# 9. Task Integration

The existing Task table should contain:

```text
project_id
```

A task may therefore be:

```text
project_id = 421
```

or:

```text
project_id = NULL
```

when it is not associated with a project.

Recommended relationship:

```sql
FOREIGN KEY (project_id)
    REFERENCES projects(id)
    ON DELETE SET NULL
```

## Important deletion behavior

Deleting or removing a project must **not delete its tasks**.

Instead:

```text
PROJECT removed
       ↓
TASK remains
       ↓
project_id = NULL
```

This protects historical information.

---

# 10. No `project_tasks` Table

Do not create a separate project/task junction table in v1.

The relationship is intentionally:

```text
PROJECT
   │
   ├── TASK
   ├── TASK
   └── TASK
```

A task normally belongs to at most one project.

This keeps SQLite queries simple and fast.

---

# 11. No Direct Goal → Task Relationship in v1

Prefer:

```text
GOAL
 ↓
PROJECT
 ↓
TASK
```

rather than:

```text
GOAL
 ↓
TASK
```

This prevents unnecessary many-to-many relationships.

If real usage later demonstrates a need for direct Goal → Task relationships, that can be added through a future migration.

---

# 12. Project Progress

For v1, calculated progress is:

```text
completed eligible tasks
------------------------ × 100
total eligible tasks
```

Example:

```text
10 eligible tasks
7 completed

Calculated progress = 70%
```

For v1, eligible tasks should exclude cancelled tasks.

Progress calculation belongs to the Project Service, not permanently stored database state.

---

# 13. Effective Project Progress

Application logic:

```text
IF manual_progress IS NOT NULL
    effective_progress = manual_progress
ELSE
    effective_progress = calculated_task_progress
```

This avoids storing duplicate calculated data.

---

# 14. Goal Progress

Do **not** add a generic `progress` column to `goals`.

Different goals require different measurements.

Examples:

```text
Goal:
Save ₹100,000
        ↓
Finance measurement

Goal:
Exercise regularly
        ↓
Wellbeing measurement

Goal:
Build Dexter
        ↓
Project/task progress
```

Therefore:

```text
GOAL
 ├── PROJECT progress
 ├── FINANCE measurement
 ├── WELLBEING measurement
 └── OTHER DOMAIN measurement
```

The Goal Service may later synthesize these measurements.

---

# 15. Goal Priority

Goal and project priority:

```text
LOW
NORMAL
HIGH
CRITICAL
```

Priority provides context.

It must not automatically override task urgency.

Example:

```text
Goal priority:
HIGH

Task urgency:
LOW
```

does not mean Dexter should immediately interrupt the user.

The Attention Engine evaluates the complete context.

---

# 16. Goal Types

v1:

```text
OUTCOME
HABIT
FINANCIAL
LEARNING
HEALTH
PERSONAL
PROFESSIONAL
OTHER
```

These are organizational labels.

Finance and Wellbeing retain ownership of actual domain measurements.

---

# 17. Indexes

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

The primary key:

```text
(goal_id, project_id)
```

already provides efficient lookup from the goal side.

Do not add unnecessary indexes before profiling.

---

# 18. Example Dexter Structure

```text
GOAL
Build Dexter
Priority: HIGH
Status: ACTIVE
        │
        ├──────────────┐
        ▼              ▼
PROJECT             PROJECT
Database            Voice Control
Architecture
        │
        ├── Task ✓
        ├── Task ✓
        ├── Task ✓
        └── Task →
```

A task can then be connected to:

```text
Task
 ↓
Project
 ↓
Goal
```

This gives Dexter meaningful context.

---

# 19. Attention Integration

Example:

```text
Goal:
Build Dexter
Priority: HIGH

Project:
Database Architecture
Status: ACTIVE

Task:
Finalize Finance Architecture
Deadline:
Tomorrow 10:00
```

The Attention Engine can consider:

```text
Task urgency
+
Task deadline
+
Project priority
+
Goal priority
```

Goal priority remains contextual rather than automatically triggering an alarm.

---

# 20. Schedule Integration

Goals & Projects do not own calendar scheduling.

Use:

```text
Goal
 ↓
Project
 ↓
Task
 ↓
Schedule occurrence
```

Example:

```text
Task:
Finalize Finance Architecture

Scheduled:
Tomorrow 08:00–09:30

Deadline:
Tomorrow 10:00
```

Schedule owns the time slot.

Attention owns whether Dexter should interrupt the user.

---

# 21. Example Query Context

When Dexter needs to answer:

> "What should I focus on today?"

The application should query only relevant data:

```text
Active high-priority goals
        ↓
Active projects
        ↓
Today's tasks
        ↓
Deadlines
        ↓
Attention state
```

Then provide a compact context to Qwen/Laya.

The model does not need the entire database.

---

# 22. Privacy

Goals and projects are private personal data.

Requirements:

- local SQLite only;
- no cloud dependency;
- no network requirement;
- no remote analytics;
- no automatic external synchronization;
- Qwen/Laya operate locally;
- model receives only the minimum context needed.

---

# 23. Performance Requirements

Target device is old and resource constrained.

Therefore:

- use SQLite integer primary keys;
- use INTEGER Unix timestamps;
- keep tables normalized;
- avoid unnecessary JSON blobs;
- avoid storing calculated progress;
- avoid unnecessary indexes;
- keep common queries simple;
- calculate progress on demand;
- cache only when profiling demonstrates a need;
- do not build a graph database;
- do not load all goals/projects into the LLM context.

---

# 24. Explicitly Excluded from v1

Do not create:

```text
milestones
project_dependencies
task_dependencies
goal_metrics
goal_measurements
progress_history
goal_task_links
project_task_links
goal_graph
```

These are reserved for future versions if actual usage justifies them.

---

# 25. Future Advanced Model

Model B is intentionally designed for migration.

Possible future tables:

```text
milestones
project_dependencies
task_dependencies
goal_metrics
goal_measurements
progress_history
```

Possible future capabilities:

```text
Goal hierarchy
Milestones
Project dependencies
Task dependencies
Historical progress
Measurable KPIs
Advanced planning
Predictive completion estimates
Resource planning
```

Existing v1 tables should remain the foundation:

```text
goals
projects
goal_projects
tasks
```

Do not redesign the foundation merely to anticipate these features.

---

# 26. Test Matrix

## Goals

- Create goal
- Edit goal
- Pause goal
- Resume goal
- Complete goal
- Cancel goal
- Archive goal
- Set target date
- Change priority
- Validate goal type

## Projects

- Create project
- Activate project
- Put project on hold
- Mark project blocked
- Complete project
- Cancel project
- Archive project
- Set target date
- Change priority
- Set manual progress
- Clear manual progress

## Relationships

- Attach project to goal
- Attach project to multiple goals
- Mark primary relationship
- Mark secondary relationship
- Remove relationship
- Delete goal
- Delete project

## Tasks

- Assign task to project
- Remove task from project
- Delete project without deleting tasks
- Calculate project progress
- Ignore cancelled tasks in progress calculation

## Integration

- Goal → Project → Task context
- Task → Schedule
- Task → Attention
- Goal priority available to Attention Engine
- Project deadline available to Attention Engine
- Goal/project context available to morning briefing

---

# 27. Implementation Order

```text
1. Create goals table
2. Create projects table
3. Create goal_projects table
4. Add project_id to tasks
5. Add foreign-key constraints
6. Add indexes
7. Implement Goal repository
8. Implement Project repository
9. Implement Goal-Project relationship repository
10. Implement project progress calculation
11. Implement lifecycle rules
12. Integrate with Task service
13. Integrate with Schedule
14. Integrate with Attention
15. Integrate with morning briefing
16. Test on target Android hardware
17. Profile SQLite queries
```

---

# 28. Final Decisions

| Decision | Status |
|---|---|
| Hybrid Model B | FINAL |
| Separate Goals and Projects | FINAL |
| Goal → Project many-to-many | FINAL |
| Project → Task one-to-many | FINAL |
| No project_tasks table | FINAL |
| No direct Goal → Task table | FINAL |
| Hybrid project progress | FINAL |
| No generic goal progress column | FINAL |
| Domain-specific goal measurements | FINAL |
| Goal priority | FINAL |
| Project priority | FINAL |
| Project BLOCKED state | FINAL |
| Preserve tasks when project is removed | FINAL |
| Schedule owns scheduling | FINAL |
| Attention owns attention | FINAL |
| Local SQLite only | FINAL |
| No graph database | FINAL |
| Advanced model deferred | FINAL |

---

# 29. Final Status

**GOALS & PROJECTS v1 — FINALIZED**

The finalized foundation is:

```text
goals
  │
  │ many-to-many
  ▼
goal_projects
  │
  ▼
projects
  │
  │ one-to-many
  ▼
tasks
```

With the broader Dexter architecture:

```text
GOAL
 ↓
PROJECT
 ↓
TASK
 ↓
SCHEDULE
 ↓
ATTENTION
```

The design is intentionally small enough for the current low-resource Android hardware while preserving a clean migration path toward a more advanced planning system when hardware capabilities improve.
