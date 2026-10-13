# Dexter Database — Wellbeing v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**AI:** Qwen 2.5 0.5B + Laya  
**Primary external-data mechanism:** Daily Capture / Data Gathering

## 1. Purpose

Wellbeing stores lightweight, structured information that helps Dexter understand physical wellbeing, mental state, habits, compulsive behaviors, recovery patterns, and their relationship to productivity.

It is **not a medical system** and must not diagnose diseases, addiction disorders, or other clinical conditions.

## 2. Architecture Boundary

```text
Schedule       → WHEN
Daily Capture  → WHAT information to collect
Wellbeing      → WHAT the information means
Goals          → WHAT target/outcome is desired
Attention      → HOW Dexter gets attention
Logging        → WHAT happened in the system
Qwen/Laya      → reasoning and interaction
```

Daily Capture is the primary way Dexter obtains information the dedicated Vivo V11 cannot observe.

## 3. Provenance

Applicable records distinguish:

```text
USER_REPORTED
DEVICE
IMPORTED
DERIVED
```

- **USER_REPORTED:** explicitly provided by the user.
- **DEVICE:** actually obtained from a supported device/system source.
- **IMPORTED:** explicitly imported through an approved local mechanism.
- **DERIVED:** calculated from existing records.

### Core rule

```text
OBSERVED ≠ REPORTED ≠ IMPORTED ≠ DERIVED
```

Dexter must never present user-reported or derived information as though it directly observed it.

## 4. Tables

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

Goals/targets remain part of the Goals architecture rather than being duplicated inside every Wellbeing domain.

## 5. Sleep

Primary capture timing: **morning**.

Data:
- start/end;
- nap flag;
- quality 1–5;
- interruptions;
- provenance;
- notes.

```sql
CREATE TABLE wellbeing_sleep (
    id INTEGER PRIMARY KEY,
    started_at INTEGER NOT NULL,
    ended_at INTEGER NOT NULL,
    is_nap INTEGER NOT NULL DEFAULT 0 CHECK (is_nap IN (0,1)),
    quality INTEGER CHECK (quality IS NULL OR quality BETWEEN 1 AND 5),
    interruptions INTEGER NOT NULL DEFAULT 0 CHECK (interruptions >= 0),
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    notes TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Rules:
- Sleep is a time interval, not merely a calendar date.
- Duration is derived from timestamps when possible.
- Missing information remains unknown.
- Naps are separate sessions.
- Overlaps should be detected.
- No clinical sleep analysis in v1.

## 6. Exercise / Activity

Primary capture timing: **evening/end-of-day**, with voice entry available anytime.

Categories:

```text
STRENGTH
CARDIO
WALKING
RUNNING
CYCLING
SPORT
YOGA
MOBILITY
OTHER
```

```sql
CREATE TABLE wellbeing_activity (
    id INTEGER PRIMARY KEY,
    activity_type TEXT NOT NULL,
    activity_name TEXT,
    started_at INTEGER,
    ended_at INTEGER,
    duration_seconds INTEGER,
    intensity INTEGER CHECK (intensity IS NULL OR intensity BETWEEN 1 AND 5),
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    notes TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Rules:
- One record per activity session.
- Multiple sessions per day are allowed.
- If timestamps exist, duration is derived.
- No fake calorie estimates.
- No GPS, heart-rate, VO2 max, wearable sync, or advanced fitness tracking in v1.

## 7. Diet / Meals

Meal types:

```text
BREAKFAST
LUNCH
DINNER
SNACK
OTHER
```

```sql
CREATE TABLE wellbeing_meals (
    id INTEGER PRIMARY KEY,
    meal_type TEXT NOT NULL,
    eaten_at INTEGER NOT NULL,
    description TEXT,
    diet_adherence INTEGER CHECK (
        diet_adherence IS NULL OR diet_adherence IN (0,1)
    ),
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    notes TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Rules:
- Natural-language meal descriptions are sufficient.
- Calories/macros are not required.
- No nutrition database in v1.
- Diet targets belong to Goals.
- Adherence is an observation, not a medical judgement.

## 8. Hydration

```sql
CREATE TABLE wellbeing_hydration (
    id INTEGER PRIMARY KEY,
    recorded_at INTEGER NOT NULL,
    amount_ml INTEGER NOT NULL CHECK (amount_ml > 0),
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    notes TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Daily totals are derived using `SUM(amount_ml)`.

Dexter should support natural input such as:
- “I drank 500 ml.”
- “I've had about 2 litres today.”

## 9. Mood

Scale:

```text
1 = Very bad
2 = Bad
3 = Neutral
4 = Good
5 = Very good
```

```sql
CREATE TABLE wellbeing_mood (
    id INTEGER PRIMARY KEY,
    recorded_at INTEGER NOT NULL,
    mood INTEGER NOT NULL CHECK (mood BETWEEN 1 AND 5),
    note TEXT,
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Mood is structured data; Journal is free-form reflection.

## 10. Energy

Scale:

```text
1 = Exhausted
2 = Low
3 = Normal
4 = High
5 = Very high
```

```sql
CREATE TABLE wellbeing_energy (
    id INTEGER PRIMARY KEY,
    recorded_at INTEGER NOT NULL,
    energy INTEGER NOT NULL CHECK (energy BETWEEN 1 AND 5),
    note TEXT,
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Mood and energy are independent signals.

## 11. Screen / Device Usage

The Vivo V11 is dedicated to Dexter and is not the user's primary phone. Its screen time must not automatically represent overall personal screen time.

```sql
CREATE TABLE wellbeing_device_usage (
    id INTEGER PRIMARY KEY,
    usage_date INTEGER NOT NULL,
    screen_time_seconds INTEGER,
    distracting_time_seconds INTEGER,
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    notes TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Critical rule:

```text
Dexter-device usage ≠ overall personal device usage
```

## 12. Habits

Habits represent repeated desired behaviors.

```sql
CREATE TABLE wellbeing_habits (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    frequency_type TEXT NOT NULL CHECK (
        frequency_type IN ('DAILY','WEEKLY','CUSTOM')
    ),
    target_count INTEGER NOT NULL DEFAULT 1 CHECK (target_count > 0),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (
        status IN ('ACTIVE','PAUSED','ARCHIVED')
    ),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

```sql
CREATE TABLE wellbeing_habit_logs (
    id INTEGER PRIMARY KEY,
    habit_id INTEGER NOT NULL,
    recorded_at INTEGER NOT NULL,
    completed INTEGER NOT NULL CHECK (completed IN (0,1)),
    note TEXT,
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (habit_id) REFERENCES wellbeing_habits(id)
        ON DELETE RESTRICT
);
```

A habit must not duplicate an underlying activity record.

## 13. Compulsive Behavior / Addiction Support

Dexter should support **user-defined compulsive behaviors** because these can affect time, money, relationships, wellbeing, and productivity.

Examples:

```text
Excessive social media
Gaming
Compulsive browsing
Unplanned shopping
Excessive entertainment
Other user-defined behavior
```

Use neutral terminology such as `COMPULSIVE_BEHAVIOR`.

This is **not an addiction diagnosis system**.

```sql
CREATE TABLE wellbeing_compulsive_behaviors (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    direction TEXT NOT NULL CHECK (
        direction IN ('REDUCE','AVOID','CONTROL','OBSERVE')
    ),
    severity_self_rating INTEGER CHECK (
        severity_self_rating IS NULL OR severity_self_rating BETWEEN 1 AND 5
    ),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (
        status IN ('ACTIVE','PAUSED','ARCHIVED')
    ),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

`severity_self_rating` is only the user's own rating, not a clinical score.

### Compulsive behavior logs

```sql
CREATE TABLE wellbeing_compulsive_behavior_logs (
    id INTEGER PRIMARY KEY,
    behavior_id INTEGER NOT NULL,
    occurred_at INTEGER NOT NULL,
    duration_seconds INTEGER,
    intensity INTEGER CHECK (
        intensity IS NULL OR intensity BETWEEN 1 AND 5
    ),
    trigger TEXT,
    avoided INTEGER CHECK (
        avoided IS NULL OR avoided IN (0,1)
    ),
    notes TEXT,
    source_type TEXT NOT NULL CHECK (source_type IN (
        'USER_REPORTED','DEVICE','IMPORTED','DERIVED'
    )),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (behavior_id) REFERENCES wellbeing_compulsive_behaviors(id)
        ON DELETE RESTRICT
);
```

Useful derived metrics:
- occurrences/week;
- duration/week;
- longest duration;
- days without occurrence;
- trigger frequency;
- user-reported control rate.

Dexter may identify patterns but must not diagnose addiction.

## 14. Attention Rules for Compulsive Behaviors

Attention may provide:
- planned intervention;
- scheduled check-in;
- goal reminder;
- reflection prompt.

It must not create:
- infinite reminders;
- guilt loops;
- harassment;
- constant behavioral surveillance.

Example:

```text
Goal: Reduce gaming after midnight
Schedule: 23:30 check-in
Attention: One reminder
Ignored: No infinite escalation
```

## 15. Wellbeing Data Gathering

Recommended default:

```text
MORNING
├── Sleep
├── Morning energy
└── Morning mood

DURING DAY
├── Quick voice capture
├── Meals
├── Hydration
└── Exercise

EVENING
├── Exercise
├── Meals
├── Hydration
├── Mood
├── Energy
├── Screen/device usage
├── Habits
└── Optional compulsive-behavior check

END OF DAY
└── Missing-data capture / final confirmation
```

The system should not repeatedly interrupt the user.

## 16. Goals and Targets

Targets belong to Goals.

Examples:

```text
Sleep ≥ 8 hours
Exercise ≥ 4 sessions/week
Water ≥ 2 L/day
Diet adherence ≥ 80%
Screen distraction ≤ 2 hours/day
Gaming ≤ 3 sessions/week
No compulsive browsing after midnight
```

Wellbeing supplies evidence; Goals owns the target.

## 17. Derived Metrics

Potential metrics:

### Sleep
- average duration;
- consistency;
- target adherence.

### Exercise
- sessions/week;
- minutes/week;
- days since last exercise.

### Diet
- adherence percentage;
- meals logged.

### Hydration
- daily total;
- weekly average;
- target adherence.

### Mood/Energy
- averages;
- trends.

### Device usage
- screen time;
- distracting time;
- trend.

### Habits
- completion rate;
- streak;
- missed days.

### Compulsive behaviors
- occurrences/week;
- duration/week;
- trend;
- trigger frequency;
- days without occurrence.

Metrics are normally calculated from raw records rather than duplicated permanently.

## 18. Cross-Domain Reasoning

Wellbeing can provide evidence for Dexter's reasoning:

```text
Sleep ↓
Energy ↓
Task delays ↑
```

or:

```text
Compulsive browsing ↑
Screen distraction ↑
Task delays ↑
```

or:

```text
Exercise ↑
Energy ↑
Task completion ↑
```

These are patterns, not automatically causal relationships.

Dexter must distinguish:

```text
Observed pattern
```

from:

```text
Proven cause
```

## 19. Morning Briefing

Example:

```text
Good morning.

Sleep: 6h 20m — below your target.
Energy: 2/5.
Mood: 4/5.

Yesterday:
Exercise: 45 min.
Water: 1.7 L.
Diet adherence: 80%.
Screen distraction: 2h 10m.

Today:
Your energy is low, so Dexter recommends protecting your highest-priority
task and keeping nonessential work lighter.
```

## 20. End-of-Day Summary

The End-of-Day subsystem may combine:

```text
Tasks
Schedule
Goals
Wellbeing
Finance
Relationships
Journal
```

Wellbeing provides structured facts; the briefing subsystem owns final synthesis.

## 21. Privacy

Requirements:

- local SQLite only;
- no cloud dependency;
- no remote AI;
- no telemetry;
- no automatic network synchronization;
- sensitive values must not enter generic logs;
- Qwen/Laya receive minimum relevant context;
- raw history should not be loaded unnecessarily into model context.

Sensitive compulsive-behavior data receives the same or stronger privacy treatment as other wellbeing data.

## 22. Performance Rules

1. Use INTEGER primary keys.
2. Use INTEGER Unix timestamps.
3. Keep tables normalized.
4. Avoid giant JSON blobs.
5. Avoid unnecessary indexes.
6. Avoid redundant daily/weekly totals.
7. Use SQLite aggregation when practical.
8. Do not load complete history into Qwen/Laya.
9. Use transactions for multi-table domain updates.
10. Profile on the actual Vivo V11 before adding caches or denormalization.

## 23. Initial Indexes

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

Do not add indexes without a query/performance justification.

## 24. Explicitly Excluded from v1

```text
Medical diagnosis
Clinical addiction diagnosis
Clinical mental-health scoring
Medical records
Calorie estimation
Macro tracking
Food nutrition database
GPS fitness tracking
Heart-rate analytics
VO2 max
Sleep-stage analysis
Wearable synchronization
Complex workout programming
Automatic exercise detection
Automatic addiction detection
Continuous behavioral surveillance
Cloud synchronization
Remote AI processing
```

## 25. Final Wellbeing Rules

```text
RULE 01
Wellbeing stores observations, not medical diagnoses.

RULE 02
Daily Capture collects external information; Wellbeing owns resulting domain records.

RULE 03
The Vivo V11 must not be assumed to represent overall device usage.

RULE 04
Applicable records must preserve provenance.

RULE 05
USER_REPORTED, DEVICE, IMPORTED, and DERIVED must not be conflated.

RULE 06
Raw observations are authoritative; derived metrics are calculated from them.

RULE 07
Do not duplicate derived metrics unnecessarily.

RULE 08
Goals own wellbeing targets; Wellbeing supplies evidence.

RULE 09
Sleep duration is derived from timestamps when available.

RULE 10
Exercise sessions are stored independently; totals are derived.

RULE 11
Diet logging does not require calories or nutrition databases in v1.

RULE 12
Hydration totals are derived from hydration records.

RULE 13
Mood and energy are independent signals.

RULE 14
Habits are separate from habit completion logs.

RULE 15
Habits must not duplicate underlying activity records.

RULE 16
Dexter must not repeatedly interrupt the user for wellbeing data.

RULE 17
Morning capture prioritizes sleep and morning state.

RULE 18
Evening capture prioritizes completed-day wellbeing information.

RULE 19
Unknown information remains unknown; Dexter must never invent missing data.

RULE 20
Correlations must not automatically be presented as causation.

RULE 21
Sensitive wellbeing data must not be copied into generic logs.

RULE 22
Qwen/Laya should receive minimum relevant wellbeing context.

RULE 23
All wellbeing data remains local and offline.

RULE 24
No network dependency may exist for core wellbeing functionality.

RULE 25
SQLite complexity must be justified by actual requirements.

RULE 26
Performance must be validated on target hardware before caching or denormalization.

RULE 27
Raw wellbeing records remain the source of truth.

RULE 28
Future device/import integrations must follow the provenance model.

RULE 29
Compulsive behavior tracking is voluntary and user-controlled.

RULE 30
Dexter must not diagnose addiction or other clinical disorders.

RULE 31
Compulsive behavior records describe user-observed behavior, not medical conditions.

RULE 32
Dexter may identify behavioral patterns but must distinguish patterns from diagnoses.

RULE 33
Attention related to compulsive behaviors must not create infinite reminders, guilt loops, or harassment.

RULE 34
User-defined reduction/avoidance goals belong to the Goals architecture.

RULE 35
Sensitive compulsive-behavior data receives strong local privacy treatment.

RULE 36
The system must support correcting or deleting user-entered wellbeing records according to the final cross-domain data-management policy.
```

## 26. Final Architecture

```text
                         WELLBEING
                             │
       ┌──────────┬──────────┼──────────┬──────────┐
       ▼          ▼          ▼          ▼          ▼
     Sleep     Activity     Meals    Hydration   Mood
                                                     │
                                                     ▼
                                                  Energy
                                                     │
                 ┌───────────────────────────────────┘
                 ▼
          Device Usage
                 │
                 ▼
              Habits
                 │
                 ▼
       Compulsive Behaviors
                 │
                 ▼
          Derived Metrics
                 │
                 ▼
          Goals / Progress
                 │
                 ▼
          Dexter Context
                 │
          ┌──────┴──────┐
          ▼             ▼
        Laya          Qwen
       Decision      Interaction
```

## 27. Final Status

**WELLBEING v1 — FINALIZED**

The subsystem is lightweight, offline-first, privacy-preserving, provenance-aware, compatible with the dedicated Vivo V11, primarily driven by Daily Capture, and extensible without requiring a complete schema rewrite.
