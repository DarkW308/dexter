# Dexter Database Architecture — Daily Capture / Data Gathering v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target:** Low-RAM, older Android hardware  
**AI:** Qwen 2.5 0.5B + Laya

## 1. Purpose

Daily Capture / Data Gathering collects information Dexter cannot reliably obtain automatically, such as food, exercise, sleep, spending, income, relationship interactions, offline activities, mood, events, and task delays.

Daily Capture is an **orchestration layer**, not the permanent owner of domain data.

## 2. Core Architectural Principle

```text
Schedule   → WHEN
Capture    → WHAT
Attention  → HOW
Domain     → WHAT IT MEANS
Logging    → WHAT HAPPENED
```

## 3. Final Architecture

```text
                    SCHEDULE
                       │
                       │ WHEN?
                       ▼
                CAPTURE EVENT
                       │
                       │ WHAT?
                       ▼
              CAPTURE QUESTIONS
                       │
                       ▼
                      USER
                       │
                       │ ANSWER
                       ▼
             CAPTURE RESPONSES
                       │
                 VALIDATE + ROUTE
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
       FINANCE      WELLBEING   RELATIONSHIPS
```

Attention handles notification, voice, and alarm delivery.

## 4. Final v1 Tables

```text
capture_templates
capture_questions
capture_events
capture_responses
```

Do not create a universal `daily_data` table.

## 5. `capture_templates`

```sql
CREATE TABLE capture_templates (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Rules:
- Templates may be paused or archived.
- Archiving must not delete historical events.
- Template changes must not rewrite historical capture meaning.

## 6. `capture_questions`

```sql
CREATE TABLE capture_questions (
    id INTEGER PRIMARY KEY,
    template_id INTEGER NOT NULL,
    question TEXT NOT NULL,
    answer_type TEXT NOT NULL
        CHECK (answer_type IN (
            'BOOLEAN', 'NUMBER', 'TEXT', 'CHOICE',
            'MULTI_CHOICE', 'DURATION', 'TIME', 'DATE', 'CURRENCY'
        )),
    domain TEXT NOT NULL
        CHECK (domain IN (
            'GENERAL', 'WELLBEING', 'FINANCE',
            'RELATIONSHIPS', 'JOURNAL', 'TASKS', 'OTHER'
        )),
    required INTEGER NOT NULL DEFAULT 0
        CHECK (required IN (0, 1)),
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (template_id)
        REFERENCES capture_templates(id)
        ON DELETE CASCADE
);
```

`required=1` means the question must be answered for normal completion.

`domain` identifies the service responsible for the meaning of the answer.

## 7. `capture_events`

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
            'PENDING', 'IN_PROGRESS', 'COMPLETED',
            'SKIPPED', 'MISSED', 'EXPIRED', 'CANCELLED'
        )),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (template_id)
        REFERENCES capture_templates(id)
        ON DELETE RESTRICT
);
```

A capture event is an actual occurrence, not a reusable definition.

Schedule determines `scheduled_at`; the application determines the capture window and `expires_at`.

## 8. `capture_responses`

```sql
CREATE TABLE capture_responses (
    id INTEGER PRIMARY KEY,
    capture_event_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    answer TEXT,
    answered_at INTEGER,
    processing_status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (processing_status IN (
            'PENDING', 'PROCESSED', 'FAILED', 'SKIPPED'
        )),
    processed_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (capture_event_id)
        REFERENCES capture_events(id)
        ON DELETE CASCADE,
    FOREIGN KEY (question_id)
        REFERENCES capture_questions(id)
        ON DELETE RESTRICT
);
```

One question normally receives one response per event.

```sql
CREATE UNIQUE INDEX idx_capture_response_unique
ON capture_responses(capture_event_id, question_id);
```

## 9. Indexes

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

Do not add indexes without a justified query/use case.

## 10. Capture Lifecycle

Normal:

```text
PENDING → IN_PROGRESS → COMPLETED
```

Alternatives:

```text
PENDING → SKIPPED
PENDING → MISSED
PENDING → EXPIRED
PENDING → CANCELLED

IN_PROGRESS → SKIPPED
IN_PROGRESS → EXPIRED
IN_PROGRESS → COMPLETED
```

Invalid transitions must be rejected by application logic.

## 11. Completion Rule

An event becomes `COMPLETED` when every required question has a valid response.

Optional questions never block completion.

## 12. Partial Completion

If required questions remain unanswered, the event stays `IN_PROGRESS` until answered, explicitly skipped, expired, or missed.

## 13. Capture Window

A capture does not become missed immediately at its scheduled time.

Example:

```text
20:30 scheduled
20:30–21:30 capture window
21:30 → MISSED
```

The window is application configuration, not hard-coded schema behavior.

## 14. Recurrence

Daily Capture must **not** implement a second recurrence engine.

Use:

```text
Capture Template
       ↓
Schedule
       ↓
Scheduled occurrence
       ↓
Capture Event
```

Schedule remains authoritative for recurrence, dates, times, and timezone handling.

## 15. Future Event Generation

Do not create hundreds of future events for recurring captures.

Generate only a small near-term window when required.

## 16. Attention Integration

Daily Capture does not directly send notifications.

```text
Capture Event
     ↓
Attention Service
     ↓
Notification / Voice / Alarm
```

No infinite reminder loop is permitted.

## 17. Template Change / Historical Integrity

Historical events must remain historically correct.

If a template gains a new question, old events must not suddenly acquire that question.

The implementation must snapshot the applicable question set at event creation or use immutable template versions.

## 18. Domain Routing

Example:

```text
"Did you exercise?"       → WELLBEING
"How much did you spend?" → FINANCE
"Did you contact Sam?"    → RELATIONSHIPS
```

Routing:

```text
Capture Response
      ↓
Validate
      ↓
Identify Domain
      ↓
Domain Service
      ↓
Domain transaction
```

Daily Capture must not directly manipulate Finance, Wellbeing, or Relationship tables.

## 19. Response Processing

Initial:

```text
PENDING
```

Successful:

```text
PROCESSED
```

Failed:

```text
FAILED
```

A failed response must remain recoverable and must never silently disappear.

## 20. Transaction Rule

Where a response becomes authoritative domain data, use a SQLite transaction.

```text
BEGIN TRANSACTION
Write domain record
Update capture response
COMMIT
```

On failure:

```text
ROLLBACK
```

This prevents a response from being marked processed when the domain record was not actually written.

## 21. Answer Storage

For v1:

```text
answer TEXT
```

is sufficient.

`answer_type` controls validation and interpretation.

Avoid multiple answer columns unless later profiling demonstrates a real need.

## 22. No Universal JSON Answer

Do not store an entire check-in as one large JSON object.

Use one response per question:

```text
Capture Event
 ├── Question 1 → Answer
 ├── Question 2 → Answer
 ├── Question 3 → Answer
 └── Question 4 → Answer
```

## 23. Sensitive Data

Capture may contain financial, health, relationship, and personal information.

Never copy full sensitive answers into generic logs.

Safe example:

```text
capture_response_processed
response_id=123
domain=FINANCE
status=SUCCESS
```

Do not log the actual financial or health content unless an approved domain-specific policy requires it.

## 24. Privacy

Daily Capture is:

```text
LOCAL
OFFLINE
PRIVATE
```

Requirements:
- no cloud dependency;
- no remote processing;
- no telemetry;
- no automatic network synchronization;
- local SQLite only;
- local AI only;
- Qwen/Laya receives only minimum relevant context.

## 25. Retention

Do not hard-code aggressive deletion in Daily Capture v1.

Retention must eventually be coordinated across:

```text
Tasks
Schedule
Attention
Finance
Wellbeing
Relationships
Documents
Journal
Logging
Daily Capture
```

Until the cross-domain policy is finalized, preserve historical capture records unless explicitly deleted by an approved policy.

## 26. Morning Briefing

Morning briefing may query the previous day's capture status:

```text
✓ Evening check-in completed
✓ Finance data captured
✓ Exercise recorded
⚠ Sleep data missing
```

The briefing should summarize relevant information, not dump raw responses.

## 27. End-of-Day Summary

End-of-day processing may combine:

```text
Tasks
Schedule
Attention
Daily Capture
Finance
Wellbeing
Relationships
Journal
```

Daily Capture supplies collection status and relevant processed facts; the briefing subsystem owns final synthesis.

## 28. Performance Requirements

For old Android hardware:

1. Use INTEGER primary keys.
2. Use INTEGER Unix timestamps.
3. Keep tables normalized.
4. Keep answers compact.
5. Avoid unnecessary JSON.
6. Avoid unnecessary indexes.
7. Avoid large future event sets.
8. Process responses incrementally.
9. Use transactions for domain processing.
10. Query only relevant events where possible.
11. Never load the complete capture history into Qwen.
12. Profile before adding caches or denormalization.

## 29. Error Handling

Invalid answer:

```text
Capture Response
     ↓
Validation failure
     ↓
Remain recoverable
     ↓
Ask user to correct
```

Domain processing failure:

```text
processing_status = FAILED
```

The response remains available for retry/diagnosis.

## 30. User Corrections

The user must be able to correct captured information.

The application should:
1. identify the relevant response;
2. validate the correction;
3. update or supersede the response;
4. reconcile the domain record;
5. preserve necessary audit information.

The exact reconciliation rules belong to the relevant domain.

## 31. Voice / Database Safety

Voice commands must never execute arbitrary SQL.

Correct:

```text
Voice
 ↓
Intent parser
 ↓
Application service
 ↓
Validation
 ↓
Repository
 ↓
SQLite
```

Never:

```text
Voice
 ↓
LLM-generated SQL
 ↓
SQLite
```

## 32. AI Context Rules

Qwen/Laya must not receive all capture history.

Only retrieve relevant context for the current operation.

Examples:

```text
"What did I miss yesterday?"
→ Yesterday's incomplete capture events

"Morning briefing"
→ Relevant completed/missed captures

"How is my spending?"
→ Finance data, not the entire capture database
```

## 33. Security Boundary

AI-generated values are intent/value candidates, not trusted database writes.

Validate:

```text
type
required fields
range
domain rules
```

before SQLite writes.

## 34. Test Matrix

### Templates
- create;
- edit;
- pause;
- resume;
- archive;
- preserve history.

### Questions
- add;
- edit;
- reorder;
- required/optional;
- validate answer type;
- validate domain.

### Events
- create;
- start;
- complete;
- skip;
- miss;
- expire;
- cancel;
- validate state transitions.

### Responses
- save;
- validate;
- process;
- retry failed processing;
- prevent duplicates;
- correct response.

### Integration
- Schedule creates occurrence;
- Attention receives event;
- notification works;
- voice reminder works;
- missed event reaches Attention;
- Finance receives finance response;
- Wellbeing receives wellbeing response;
- Relationships receives relationship response;
- Morning Briefing sees status;
- End-of-Day Summary sees relevant data.

### Privacy
- no network required;
- sensitive values absent from generic logs;
- AI receives only relevant context;
- processing remains local.

## 35. Implementation Order

```text
1. Create capture_templates
2. Create capture_questions
3. Create capture_events
4. Create capture_responses
5. Add indexes
6. Implement repositories
7. Implement lifecycle rules
8. Integrate Schedule
9. Integrate Attention
10. Implement response validation
11. Implement domain routing
12. Implement transactional domain processing
13. Implement missed/expired behavior
14. Implement correction flow
15. Integrate Morning Briefing
16. Integrate End-of-Day Summary
17. Test on target Android hardware
18. Profile SQLite performance
```

## 36. Explicitly Excluded from v1

Do not implement yet:

```text
Advanced branching questionnaires
Conditional question trees
Question dependency graphs
Complex workflow engine
Cloud synchronization
Remote AI processing
Large JSON form storage
Automatic external data collection
Independent recurrence engine
Large future-event generation
Generic universal personal-data table
```

## 37. Final Architectural Rules

```text
RULE 01
Schedule owns WHEN.

RULE 02
Daily Capture owns WHAT information is collected.

RULE 03
Attention owns HOW Dexter gets the user's attention.

RULE 04
Domain services own WHAT captured information means.

RULE 05
Daily Capture must not become a universal data store.

RULE 06
Daily Capture must not implement a second recurrence engine.

RULE 07
Historical capture events must remain understandable after template changes.

RULE 08
Required questions determine completion.

RULE 09
Optional questions must not block completion.

RULE 10
Missed events must not cause infinite reminders.

RULE 11
Failed response processing must never silently lose the captured response.

RULE 12
Sensitive response values must not be copied into generic logs.

RULE 13
Domain writes should use transactions where consistency requires them.

RULE 14
Do not generate large numbers of future recurring events.

RULE 15
Do not use large JSON blobs for normal question responses.

RULE 16
Do not directly write domain tables from the Capture UI.

RULE 17
Only minimum relevant context should be supplied to Qwen/Laya.

RULE 18
Capture data remains local and offline.

RULE 19
Retention/deletion policy is finalized at the cross-domain architecture stage.

RULE 20
Do not add advanced workflow features until actual usage justifies them.

RULE 21
AI-generated values must be validated before database writes.

RULE 22
Voice commands must never execute arbitrary SQL.

RULE 23
Template changes must not retroactively mutate historical capture meaning.

RULE 24
A capture response must remain recoverable if downstream processing fails.

## 38. Final Status

**DAILY CAPTURE / DATA GATHERING v1 — FINALIZED**

Final structure:

```text
capture_templates
        │
        ▼
capture_questions
        │
        ▼
capture_events
        │
        ▼
capture_responses
        │
        ▼
domain services
```

This subsystem is intentionally lightweight, offline-first, privacy-preserving, and extensible toward Finance, Wellbeing, Relationships, Journal, Briefings, and the final cross-domain architecture.
