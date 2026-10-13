# Dexter — Voice, Conversational AI & Reliability Architecture Draft v1

**Status:** Draft / Engineering Baseline  
**Purpose:** Define how Dexter should behave as a voice-first personal assistant, how Qwen and Laya should be used, and how the engineering process should minimize bugs, unsafe actions, regressions, and unnecessary debugging.

---

## 1. Executive Definition

Dexter is a **voice-first personal operating system**, not merely a chatbot.

The intended experience is similar in usability to a smart speaker/assistant, but Dexter is centered on the user's own tasks, schedule, goals, projects, wellbeing, relationships, documents, journal, alerts, briefings, and other structured data.

### Core principle

> **Voice is the interface. Dexter Core is the control layer. Domain services are the capabilities. SQLite/RAG are memory. Models assist reasoning and language; they do not own the system.**

The LLM must never have unrestricted authority over the database or operating system.

---

# 2. Target User Experience

The user should be able to say:

> "Hey Dexter."

Dexter wakes.

> "What are my pending tasks?"

Dexter:

> "You have six pending tasks. Two are high priority. Your VVIP task is due in three hours."

The user can continue naturally:

> "What about tomorrow?"

Dexter understands that the question refers to the current task/schedule context.

Example:

```text
User → "Create a task to finish the database documentation."

Dexter → "When should it be completed?"

User → "Friday."

Dexter → "What priority?"

User → "High."

Dexter → "Done. I've scheduled it for Friday as high priority."
```

The objective is **natural conversation backed by deterministic execution**.

---

# 3. High-Level Architecture

```text
                         USER
                          │
                          ▼
                    Wake Word
                   "Hey Dexter"
                          │
                          ▼
                  Speech-to-Text
                          │
                          ▼
                 Dexter Core Runtime
                          │
          ┌───────────────┼────────────────┐
          │               │                │
          ▼               ▼                ▼
     Context Engine   Intent/Decision   Model Router
          │               │                │
          └───────────────┼────────────────┘
                          │
                          ▼
                   Action Planner
                          │
                          ▼
                  Permission Layer
                          │
                          ▼
                  Domain Services
                          │
        ┌─────────┬───────┼────────┬─────────┐
        ▼         ▼       ▼        ▼         ▼
      Tasks   Schedule  Goals   Wellbeing  Relations
        │         │       │        │         │
        └─────────┴───────┼────────┴─────────┘
                          │
                          ▼
                    SQLite + FTS/RAG
                          │
                          ▼
                 Response Generation
                          │
                          ▼
                    Text-to-Speech
                          │
                          ▼
                     DEXTER VOICE
```

---

# 4. Wake-Word Architecture

Dexter should not send every ambient sound to the main language model.

Preferred flow:

```text
STANDBY
  ↓
Local wake-word detector
  ↓
Wake word detected
  ↓
Capture user utterance
  ↓
STT
  ↓
Understand
  ↓
Execute / answer
  ↓
TTS
  ↓
Return to standby
```

The wake-word detector should be lightweight and local.

Example wake word:

> "Hey Dexter"

The exact wake word should remain configurable.

---

# 5. Conversational Mode

Dexter should support both:

### Single-command mode

```text
"Hey Dexter, what's my schedule today?"
```

Answer → return to standby.

### Conversation mode

```text
"Hey Dexter, start conversation mode."

"What's my schedule tomorrow?"

"What about Friday?"

"Which one is more important?"

"Remind me about that."
```

The context manager resolves references such as:

- it
- that
- this
- tomorrow
- the meeting
- the previous task
- the important one

Conversation context should be temporary unless the user explicitly asks Dexter to remember something.

---

# 6. Qwen's Role

Qwen should be treated as Dexter's **general language and reasoning engine**, not as the system controller.

Qwen is suitable for:

- natural-language understanding
- conversation
- reasoning
- planning
- summarization
- extracting structured information
- interpreting ambiguous requests
- generating natural responses
- document understanding
- RAG synthesis
- tool/function selection
- coding and engineering assistance during Dexter development

Qwen Code is specifically designed as an AI coding agent and its documentation describes task decomposition, code generation, tool use, self-correction, and debugging workflows. citeturn0search2turn0search7

Qwen's ecosystem also supports configurable model providers and reasoning controls, which allows Dexter's model routing to evolve rather than hard-coding one model forever. citeturn0search5

### Qwen must NOT

Qwen must not:

- execute arbitrary SQL
- directly modify SQLite
- directly delete user data
- directly execute shell commands in production
- bypass permissions
- invent database IDs
- silently change system configuration
- decide that confirmation is unnecessary for destructive actions

Instead:

```text
Qwen
  ↓
Structured intent/action
  ↓
Schema validation
  ↓
Permission policy
  ↓
Domain service
  ↓
Database
```

---

# 7. Qwen Model Routing

Do not use the largest model for every request.

Use a model-routing strategy.

### Fast/simple requests

Examples:

> "What time is my next event?"

> "How many pending tasks?"

> "Mark that task complete."

Use deterministic services or a small/fast model where possible.

### Normal conversational requests

Examples:

> "What should I focus on today?"

> "Summarize my week."

Use an appropriate general Qwen model.

### Complex reasoning

Examples:

> "Review my projects and tell me why I'm falling behind."

> "Analyze this document and compare it with my current architecture."

Use a stronger reasoning-capable model.

### Engineering

Use Qwen coding models/tools for development workflows rather than making the runtime assistant responsible for writing production code.

---

# 8. Laya's Role

Laya should be used as a **fast System-1 decision/guardrail component**, not as Dexter's conversational LLM.

The current Laya project describes a non-autoregressive decision model that accepts a state plus typed questions and returns typed answers/probabilities without generating prose. It supports `choice`, `score`, and `noul` decision types. citeturn1search10turn1search0

Reported checkpoints include:

- `laya` — ModernBERT-large, approximately 421M parameters, English
- `laya-multilingual` — approximately 322M parameters, 100+ languages
- `laya-typed-decisions` — approximately 421M parameters

The multilingual checkpoint is intended for multilingual workloads, while the English checkpoint is positioned for English text, guardrails, and triage. citeturn1search10

### Laya should handle decisions such as

```text
Is this request:
  - informational
  - task command
  - reminder
  - destructive action
  - sensitive action
  - ambiguous
```

Or:

```text
Which domain?
  - Tasks
  - Schedule
  - Goals
  - Relationships
  - Journal
  - Finance
  - Wellbeing
  - Documents
```

Or:

```text
Does this action require confirmation?
  - YES
  - NO
```

Or:

```text
How urgent is this?
  - LOW
  - NORMAL
  - HIGH
  - CRITICAL
```

This is valuable because a typed decision system avoids asking a generative LLM to produce arbitrary prose that then has to be parsed.

---

# 9. Qwen + Laya Combined Architecture

Recommended:

```text
                    USER VOICE
                        │
                        ▼
                       STT
                        │
                        ▼
                 ┌──────────────┐
                 │ Dexter Core  │
                 └──────┬───────┘
                        │
              ┌─────────┴─────────┐
              ▼                   ▼
            LAYA                 QWEN
       Fast decision          Language/reasoning
              │                   │
              │                   │
              ▼                   ▼
       classify/score       understand/explain
              │                   │
              └─────────┬─────────┘
                        ▼
                  ACTION OBJECT
                        │
                        ▼
                VALIDATION LAYER
                        │
                        ▼
                 DOMAIN SERVICE
                        │
                        ▼
                    SQLITE
                        │
                        ▼
                  RESULT OBJECT
                        │
                        ▼
                      QWEN
                 response wording
                        │
                        ▼
                       TTS
```

### Critical rule

**Laya decides; Qwen reasons and communicates; deterministic code executes.**

Neither model should become the database authority.

---

# 10. Where Laya Is Especially Useful

Use Laya where Dexter needs fast, bounded decisions.

Examples:

### Intent classification

```text
"What's due tomorrow?"
→ schedule/task query
```

### Confirmation classification

```text
"Delete this task."
→ destructive action
→ confirmation required
```

### Urgency

```text
"That deadline is in 20 minutes!"
→ critical/high urgency
```

### Relevance

When many memories/documents are available:

```text
Is this memory relevant to the current request?
YES / NO / SCORE
```

### Notification filtering

Laya can help determine whether a candidate event is relevant enough to surface, while the deterministic attention policy remains authoritative.

---

# 11. What Must Remain Deterministic

The following must be implemented as normal software, not left to model judgment:

- time calculation
- date arithmetic
- recurring schedules
- alarm scheduling
- database transactions
- task IDs
- foreign keys
- deletion rules
- retention rules
- permissions
- confirmation requirements
- storage limits
- backups
- migrations
- audit logging
- notification delivery state
- event deduplication
- retry policies
- authentication
- system shutdown/recovery

Models may **interpret** these operations.

Models must not **define their own rules** for them.

---

# 12. Structured Action Contract

Qwen should produce a constrained action object.

Example:

```json
{
  "intent": "create_task",
  "parameters": {
    "title": "Finish database documentation",
    "due_date": "2026-10-09",
    "priority": "high"
  }
}
```

Dexter validates this against a schema.

Only then:

```text
ActionValidator
       ↓
PermissionPolicy
       ↓
TaskService.create()
       ↓
SQLite transaction
       ↓
AuditLog
```

Never:

```text
Qwen → raw SQL
```

---

# 13. Result Contract

Domain services should return structured results.

Example:

```json
{
  "success": true,
  "entity": "task",
  "id": 184,
  "operation": "created"
}
```

Qwen can then turn this into:

> "Done. I created the high-priority database documentation task for Friday."

This keeps the model's spoken response synchronized with what actually happened.

---

# 14. Error Handling

Every action must have explicit outcomes.

```text
SUCCESS
VALIDATION_ERROR
NOT_FOUND
PERMISSION_DENIED
CONFIRMATION_REQUIRED
CONFLICT
TIMEOUT
DEPENDENCY_UNAVAILABLE
DATABASE_ERROR
UNKNOWN_ERROR
```

Dexter should never pretend an operation succeeded.

If SQLite fails:

Bad:

> "Done."

Correct:

> "I couldn't save that because the database operation failed. Nothing was changed."

---

# 15. Confirmation Policy

### No confirmation

Safe read-only operations:

> "What's my schedule?"

> "Show pending tasks."

### Usually no confirmation

Low-risk reversible actions:

> "Create a reminder."

> "Move this task to tomorrow."

### Confirmation required

Potentially destructive operations:

> "Delete this task."

> "Remove this person."

> "Delete my journal."

### Strong confirmation

Mass/destructive operations:

> "Delete all my data."

Dexter should explicitly state the scope before executing.

---

# 16. "No Bug" Engineering Principle

Absolute zero bugs cannot honestly be guaranteed.

The correct engineering objective is:

> **Make bugs difficult to create, easy to detect, contained when they occur, and reproducible when debugging is necessary.**

Do not rely on the LLM "being careful."

Build the system so that incorrect model output cannot easily become incorrect system state.

---

# 17. Rules for Minimal Debugging

## Rule 1 — Strong contracts

Every module must have a defined input/output schema.

```text
STT → Transcript
Intent → IntentObject
Planner → ActionObject
Service → ResultObject
TTS → Audio
```

No ambiguous interfaces.

---

## Rule 2 — Validate at every boundary

Validate:

```text
voice → text
text → intent
intent → action
action → service
service → database
database → result
result → response
```

Never trust upstream output.

---

## Rule 3 — Type everything possible

Prefer typed structures/enums over free-form strings.

Bad:

```text
priority = "whatever"
```

Good:

```text
Priority.HIGH
Priority.NORMAL
Priority.LOW
```

---

## Rule 4 — One owner per responsibility

Example:

```text
TaskService owns task mutations.
ScheduleService owns schedules.
AttentionService owns alerts.
```

Do not allow five different components to modify tasks independently.

---

## Rule 5 — No hidden side effects

A read function must not secretly modify data.

A classifier must not modify data.

A response generator must not modify data.

Mutations must occur through explicit domain commands.

---

# 18. Transaction Safety

Every database mutation must use a transaction.

Example:

```text
BEGIN
  validate
  modify entity
  write audit record
COMMIT
```

If anything fails:

```text
ROLLBACK
```

This prevents partially completed operations.

---

# 19. Idempotency

Important commands should be safe against accidental duplicate execution.

Example:

If the user says:

> "Set a reminder for 6 PM."

and the network or process retries the operation, Dexter should not accidentally create five identical reminders.

Use idempotency keys or deterministic command identifiers where appropriate.

---

# 20. Event and Notification Deduplication

Alerts are particularly sensitive.

Never allow:

```text
same event
→ notification
→ retry
→ duplicate notification
→ duplicate notification
```

Use a delivery identity such as:

```text
attention_event_id
+
channel
+
scheduled_delivery
```

and enforce uniqueness at the database level where appropriate.

---

# 21. Testing Strategy

Dexter should be tested in layers.

### Unit tests

Test:

- date calculations
- task creation
- task completion
- scheduling
- priority
- permissions
- database queries
- retention
- parsing

### Integration tests

Test:

```text
Voice
 → STT
 → intent
 → service
 → SQLite
 → response
```

### Scenario tests

Create real user scenarios:

```text
"Hey Dexter, what do I have tomorrow?"

"Create a high priority task."

"Remind me two hours before."

"Delete that."

"Cancel."

"Actually restore it."
```

### Failure tests

Test:

- corrupted input
- missing database
- locked database
- invalid date
- duplicate request
- model timeout
- STT failure
- TTS failure
- network unavailable
- low storage
- unexpected model output

---

# 22. Golden Test Suite

Create a permanent collection of representative Dexter conversations.

Example:

```text
golden/
  tasks/
  schedule/
  alerts/
  relationships/
  wellbeing/
  journal/
  rag/
  voice/
  destructive_actions/
  ambiguous_commands/
```

Each test contains:

```text
Input
Expected intent
Expected action
Expected database effect
Expected spoken response class
```

Every major change must run this suite.

If an existing behavior changes unexpectedly, the build should fail.

---

# 23. Model Regression Tests

Models can change behavior after upgrades.

Therefore:

```text
Model version
+
prompt version
+
tool schema version
```

must be recorded.

Before changing a model:

```text
Run golden suite
       ↓
Compare results
       ↓
Check safety cases
       ↓
Check latency
       ↓
Check memory
       ↓
Approve upgrade
```

Never upgrade a production model simply because it is newer.

---

# 24. Qwen Development Rules

When Qwen is used to develop Dexter:

1. Read the relevant architecture document before editing code.
2. Never invent tables or APIs that are not in the specification.
3. Never modify finalized architecture silently.
4. Make the smallest change required.
5. Inspect existing code before changing it.
6. Reuse existing utilities.
7. Write tests with new functionality.
8. Run tests after changes.
9. Fix the root cause, not the symptom.
10. Never hide failing tests.
11. Never remove validation merely to make a test pass.
12. Never change production behavior without a test.
13. Record migrations explicitly.
14. Keep commits/changes small and reversible.
15. Stop and report when the specification is contradictory instead of guessing.

---

# 25. Qwen Rules Files

Qwen Code supports Markdown rules under `.qwen/rules/`, including baseline rules and path-specific conditional rules. Conditional rules can apply only when matching files are touched. citeturn0search0

Dexter should therefore maintain rules such as:

```text
.qwen/
  QWEN.md
  rules/
    architecture.md
    database.md
    voice.md
    security.md
    testing.md
    rag.md
    migrations.md
```

### Baseline rule

`architecture.md`

Contains rules that apply to all Dexter development.

### Conditional rules

Examples:

```text
database.md
paths:
  - "src/db/**"
  - "migrations/**"
```

```text
voice.md
paths:
  - "src/voice/**"
```

```text
rag.md
paths:
  - "src/rag/**"
  - "documents/**"
```

This avoids putting every rule into every model request while still enforcing relevant constraints. Qwen's documentation specifically distinguishes always-loaded context/rules from path-gated rules. citeturn0search0

---

# 26. Skills vs Rules

Do not put everything into one giant rules file.

Use:

### Rules

For constraints:

> "Never access SQLite directly from the LLM."

### Skills

For procedures:

> "How to add a new Dexter domain."

Qwen's documentation distinguishes skills as procedures that the model can invoke, while rules are constraints that apply to relevant code. citeturn0search9turn0search0

---

# 27. Repository Architecture

Recommended:

```text
dexter/
│
├── docs/
│   ├── architecture/
│   ├── database/
│   ├── domains/
│   └── decisions/
│
├── .qwen/
│   ├── QWEN.md
│   ├── rules/
│   └── skills/
│
├── src/
│   ├── core/
│   ├── voice/
│   ├── models/
│   ├── memory/
│   ├── rag/
│   ├── services/
│   ├── attention/
│   ├── database/
│   └── security/
│
├── tests/
│   ├── unit/
│   ├── integration/
│   ├── scenarios/
│   ├── regression/
│   └── golden/
│
├── migrations/
│
└── scripts/
```

---

# 28. Logging and Audit

Every meaningful mutation should produce an audit event.

Example:

```text
User voice request
      ↓
Intent detected
      ↓
Action approved
      ↓
Task created
      ↓
Audit event
```

Log enough information to reproduce failures without storing unnecessary sensitive data.

Never put secrets, passwords, tokens, or unnecessary private content into logs.

---

# 29. Observability

Dexter should expose internal diagnostic information such as:

```text
request_id
conversation_id
model
model_version
intent
action
latency
database_operation
result
error_code
```

This allows a bug report like:

> "The reminder didn't fire."

to become:

```text
request_id: 84A2...
attention_event: 192
scheduled: 02:30
delivery: failed
reason: device_sleep
```

instead of:

> "Something went wrong."

---

# 30. Recovery

Dexter should survive:

- application crash
- device reboot
- power loss
- model crash
- database interruption
- network loss

SQLite should be the source of truth for persistent state.

After restart:

```text
load state
 ↓
validate database
 ↓
recover pending jobs
 ↓
reconcile scheduled events
 ↓
resume
```

Never depend on RAM alone for important scheduled events.

---

# 31. Storage Discipline

The system has a constrained storage target.

Therefore:

- don't store duplicate model outputs
- don't store raw audio indefinitely
- don't store unnecessary conversation history
- compress/archive documents where appropriate
- keep structured data in SQLite
- keep large documents outside SQLite when appropriate
- index only what is needed
- use retention policies
- monitor free storage
- reserve space for SQLite maintenance and backups

Model files should be treated as part of the storage budget.

Laya's published checkpoints are hundreds of MB rather than multi-GB class models; the multilingual checkpoint is reported around 322M parameters, while the English checkpoint is around 421M. citeturn1search10turn1search7

---

# 32. Latency Budget

Voice interaction must feel responsive.

Target architecture:

```text
Wake detection
    ↓
STT
    ↓
Fast classification
    ↓
Deterministic operation
    ↓
TTS
```

Do not invoke a large reasoning model for:

> "What's the time of my next event?"

If the answer is already in structured data.

Use the cheapest reliable path.

---

# 33. Privacy

Default:

```text
Local first
```

Sensitive personal information should remain local whenever possible.

If cloud inference is used:

- make the route explicit
- minimize data sent
- avoid unnecessary context
- never send secrets
- record the configured privacy policy
- allow cloud features to be disabled

---

# 34. Voice Privacy

The microphone subsystem should have clear states:

```text
OFF
STANDBY / WAKE DETECTION
ACTIVE LISTENING
PROCESSING
RESPONDING
```

The user should be able to know when Dexter is actively listening.

Raw microphone audio should not be retained by default.

---

# 35. Ambiguity Policy

If Dexter is uncertain, it should ask.

Bad:

> "Delete the meeting."

when multiple meetings exist.

Good:

> "I found two meetings matching that. Do you mean the 3 PM project meeting or the 5 PM review?"

Do not guess when guessing could change data.

---

# 36. Hallucination Policy

For structured data:

> **Database truth wins.**

If Dexter says:

> "You have three pending tasks."

that number should come from the task service.

Not from the LLM's memory.

For documents:

> **Retrieved source content wins.**

If evidence is missing:

> "I couldn't find that in your documents."

Do not invent an answer.

---

# 37. Model Failure Containment

If Qwen fails:

```text
Qwen unavailable
     ↓
Deterministic commands remain available
     ↓
Basic Dexter functions continue
```

If Laya fails:

```text
Laya unavailable
     ↓
Use deterministic fallback rules
     ↓
Continue safely
```

If both fail:

```text
Safe fallback mode
     ↓
Basic predefined commands only
```

Dexter should degrade gracefully rather than becoming completely unusable.

---

# 38. Voice Command Fallback

Some commands should have deterministic patterns.

For example:

```text
"show pending tasks"
"what is my schedule today"
"start morning briefing"
"start data gathering"
"stop"
"cancel"
"repeat"
```

These can bypass an LLM entirely.

This reduces latency, cost, bugs, and hallucination.

---

# 39. Self-Debugging Policy

Dexter should not autonomously rewrite production code because it encounters an error.

Instead:

```text
Detect
  ↓
Classify
  ↓
Log
  ↓
Contain
  ↓
Attempt safe recovery
  ↓
Escalate
```

During development, Qwen Code can be used as the engineering/debugging assistant.

Qwen Code explicitly supports agentic coding workflows including reading/writing files, executing scripts, and self-correction/debugging. citeturn0search2

Production Dexter and development Qwen must remain separate concerns.

---

# 40. Definition of Done

A Dexter feature is not complete merely because it works once.

A feature is complete when:

```text
Specification
     ✓
Implementation
     ✓
Unit tests
     ✓
Integration tests
     ✓
Failure tests
     ✓
Golden scenario
     ✓
Logging
     ✓
Permission check
     ✓
Recovery behavior
     ✓
Documentation
     ✓
Regression suite
     ✓
```

Only then is it considered finalized.

---

# 41. Recommended Development Order

Do not build the whole assistant simultaneously.

### Phase 1 — Core

```text
Dexter Core
Event bus
Configuration
Lifecycle
Error handling
```

### Phase 2 — Database

Already architected.

Implement:

```text
SQLite
Migrations
Repositories
Transactions
Audit
```

### Phase 3 — Domain services

Connect:

```text
Tasks
Schedule
Goals
Projects
Attention
```

Then the remaining domains.

### Phase 4 — Model layer

Implement:

```text
Qwen adapter
Laya adapter
Model router
Structured output validation
```

### Phase 5 — Voice

```text
Wake word
STT
Conversation manager
TTS
```

### Phase 6 — Action system

```text
Intent
Action
Validation
Permission
Execution
Result
```

### Phase 7 — RAG

```text
Document ingestion
Chunking
Indexing
Retrieval
Reranking/decision filtering
Qwen synthesis
```

### Phase 8 — Briefings

```text
Morning briefing
Data gathering
End-of-day summary
```

### Phase 9 — Reliability

```text
Crash recovery
Backup
Health checks
Storage monitoring
Regression tests
```

---

# 42. Final Dexter Rule Set

These rules should be treated as architectural invariants:

1. **The database is the source of truth for structured state.**
2. **Models never directly execute arbitrary SQL.**
3. **Models never bypass permission checks.**
4. **Every mutation goes through a domain service.**
5. **Every important mutation is auditable.**
6. **Destructive actions require confirmation.**
7. **Ambiguous high-impact commands require clarification.**
8. **Read-only commands should prefer deterministic execution.**
9. **Use Laya for fast bounded decisions.**
10. **Use Qwen for language, reasoning, planning, and synthesis.**
11. **Do not use a large model when deterministic code is sufficient.**
12. **Validate every model-generated action.**
13. **Never claim success without a successful service result.**
14. **Every critical feature gets automated tests.**
15. **Every bug gets a regression test after fixing.**
16. **Never silently modify finalized architecture.**
17. **Never hide failing tests.**
18. **Prefer small reversible changes.**
19. **Persistent state must survive process/device restart.**
20. **Privacy and security are system properties, not model instructions.**
21. **Dexter must degrade gracefully when models or network services fail.**
22. **Voice is an interface, not the authority.**
23. **Qwen and Laya are replaceable components.**
24. **The Dexter Core remains authoritative.**
25. **Zero bugs cannot be promised; systematic prevention and containment can.**

---

# 43. Final Architecture Principle

The final mental model is:

```text
                 ┌───────────────────┐
                 │       USER        │
                 └─────────┬─────────┘
                           │
                         VOICE
                           │
                           ▼
                 ┌───────────────────┐
                 │   DEXTER CORE     │
                 │                   │
                 │ Context           │
                 │ Routing           │
                 │ Policy            │
                 │ State             │
                 └───────┬───────────┘
                         │
             ┌───────────┴───────────┐
             │                       │
             ▼                       ▼
          ┌───────┐               ┌───────┐
          │ LAYA  │               │ QWEN  │
          │System │               │System │
          │  1    │               │  2    │
          └───┬───┘               └───┬───┘
              │                       │
              └───────────┬───────────┘
                          ▼
                   ACTION CONTRACT
                          │
                          ▼
                  VALIDATION/POLICY
                          │
                          ▼
                    DOMAIN SERVICE
                          │
                          ▼
                  SQLITE / RAG / DATA
                          │
                          ▼
                       RESULT
                          │
                          ▼
                         QWEN
                    natural response
                          │
                          ▼
                         TTS
                          │
                          ▼
                     USER HEARS
```

**This is the architecture I recommend freezing as the baseline for Dexter Core v1.**

The Qwen/Laya division is deliberately conservative: **Qwen provides flexible intelligence; Laya provides fast bounded decisions; deterministic software provides authority.** That separation is the main mechanism for keeping Dexter lightweight, predictable, secure, and substantially easier to debug.
