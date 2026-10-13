# Dexter Database — Journal v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**Purpose:** Lightweight personal diary / reflection system

---

## 1. Purpose

Journal is intentionally simple.

It is a private digital diary where the user can record:

- thoughts;
- feelings;
- experiences;
- reflections;
- memorable moments;
- short daily notes.

It is **not** intended to become a complex knowledge-management system.

---

# 2. Core Principle

> **Journal entries should be easy to create and easy to retrieve.**

The user should be able to say:

> "Dexter, add this to my journal."

or:

> "Remember this from today."

without filling out a complicated form.

---

# 3. SQLite Schema

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

---

# 4. Entry Content

The main journal content is free-form text.

Example:

```text
Today was difficult. I spent most of the day working,
but I finally completed the database architecture.
I felt tired but satisfied.
```

The system should not force a fixed template.

---

# 5. Optional Metadata

Optional fields:

```text
Title
Mood
Tags
Date
```

The user can skip all of them.

If the user says:

> "Today was actually a good day."

Dexter should be able to create a valid journal entry without asking unnecessary questions.

---

# 6. Mood

Mood is optional.

A simple controlled vocabulary may be used:

```text
VERY_BAD
BAD
NEUTRAL
GOOD
VERY_GOOD
```

The user can also leave mood unspecified.

Dexter must not treat mood as a medical diagnosis.

---

# 7. Tags

Tags are lightweight labels such as:

```text
work
family
friends
dexter
ideas
achievement
problem
reflection
```

Tags should remain optional.

Do not create a complex tagging hierarchy for v1.

---

# 8. Journal and Daily Capture

Daily Capture and Journal are different.

### Daily Capture

Temporary structured information Dexter needs to collect.

Example:

```text
Sleep: 6 hours
Exercise: 30 minutes
Expense: ₹500
Task completed: X
```

### Journal

Personal reflection.

Example:

```text
I felt distracted today but managed to finish the important work.
```

Daily Capture may optionally produce a journal entry when the user explicitly wants that.

---

# 9. Journal and Wellbeing

Journal may reference wellbeing information when useful, but should not duplicate structured wellbeing records.

Example:

```text
Wellbeing:
Sleep = 5.5 hours

Journal:
"I was exhausted today and struggled to focus."
```

The structured sleep record remains in Wellbeing.

The reflection remains in Journal.

---

# 10. Journal and Relationships

A journal entry may mention people.

Example:

```text
Had a great conversation with Rahul today.
```

Do not automatically duplicate the entire entry into Relationships.

If the user explicitly says:

> "Remember that Rahul is preparing for an exam."

that information can be stored in the Relationships subsystem.

---

# 11. Journal and Projects

A journal entry may describe project experiences.

Example:

```text
The Dexter database design finally feels coherent.
```

The journal remains the reflection.

Project status remains in Projects.

Do not duplicate project records unnecessarily.

---

# 12. Search

Journal should support lightweight local search.

Recommended:

```text
SQLite FTS5
```

Search by:

```text
title
content
tags
```

Example:

> "What did I write about Dexter last month?"

Dexter can search journal entries by text and date.

---

# 13. Date Retrieval

Journal should support queries such as:

```text
Show today's entry.
Show yesterday's entry.
Show entries from last week.
Find what I wrote about Dexter.
Find entries mentioning Rahul.
Find entries tagged work.
```

Use SQLite/date filtering rather than RAG whenever possible.

---

# 14. RAG Integration

Journal can optionally participate in the Knowledge/RAG system.

However:

> **Journal is not primarily a document repository.**

Normal journal queries should use SQLite/FTS5.

RAG should be used only when the journal is being used as broader personal knowledge.

Example:

```text
"What did I learn about myself from my journal
over the last six months?"
```

This can trigger selected journal retrieval and context summarization.

Do not embed every journal entry by default.

---

# 15. Privacy

Journal is highly private.

Rules:

- local-only;
- no cloud synchronization required;
- no remote AI processing;
- journal content must not enter generic system logs;
- journal content should only be supplied to Qwen when relevant;
- deletion must remove the journal record;
- backups, if implemented later, must preserve privacy controls.

---

# 16. Voice Entry

Voice should be a first-class input method.

Examples:

> "Dexter, journal this: today I finally finished the database architecture."

Dexter should:

1. transcribe;
2. clean obvious speech artifacts;
3. preserve the user's meaning;
4. save the entry.

Dexter should not rewrite the user's thoughts into a different meaning without permission.

---

# 17. Automatic Journal Creation

Dexter should **not automatically create journal entries from every conversation**.

Automatic creation can create noise and consume storage.

Preferred rule:

```text
Explicit user request
        ↓
Create journal entry
```

Optional future capability:

```text
User enables automatic daily reflection
        ↓
Dexter creates a short daily summary
```

This is not required for v1.

---

# 18. Morning / EOD Integration

Journal can integrate with the existing briefing system.

### End of day

Optional prompt:

> "Anything you want to put in your journal today?"

### Morning

Dexter may reference a previous journal entry when relevant.

Example:

> "Yesterday you wrote that you were worried about the project. Today the main project task is scheduled for 10 AM."

This should only happen when relevant.

---

# 19. Journal Retention

Journal entries should normally be retained indefinitely unless the user deletes or archives them.

Because entries are plain text and metadata is small, journal storage should remain inexpensive.

---

# 20. Archive

Archiving removes an entry from normal active views without destroying it.

Archived entries remain searchable if the user explicitly searches archived content.

---

# 21. Performance

Rules:

1. Keep entries as plain text.
2. Avoid storing unnecessary derived data.
3. Use SQLite.
4. Use FTS5 for search.
5. Do not embed every entry.
6. Do not load the entire journal into memory.
7. Retrieve only the relevant date range or search results.
8. Keep the implementation lightweight.

---

# 22. Journal Rules

```text
RULE 01
Journal is a simple personal diary.

RULE 02
Journal entries are primarily free-form text.

RULE 03
Title, mood, and tags are optional.

RULE 04
The user must be able to create an entry with minimal interaction.

RULE 05
Voice entry is a first-class input method.

RULE 06
Dexter must preserve the user's intended meaning.

RULE 07
Dexter must not automatically create journal entries from every conversation.

RULE 08
Daily Capture and Journal remain separate concepts.

RULE 09
Structured wellbeing data remains in Wellbeing.

RULE 10
Structured relationship data remains in Relationships.

RULE 11
Structured project data remains in Projects.

RULE 12
Journal must not become a duplicate database for other domains.

RULE 13
SQLite is the source of truth.

RULE 14
FTS5 is preferred for normal journal search.

RULE 15
RAG is optional and only used for broader synthesis.

RULE 16
Journal entries should not be embedded by default.

RULE 17
Journal content is private and local-only.

RULE 18
Journal content must not enter generic logs.

RULE 19
Automatic summaries require explicit user preference.

RULE 20
Archived entries remain recoverable.

RULE 21
Deleted entries must be removed according to the final database deletion policy.

RULE 22
The journal must remain lightweight enough for the Vivo V11.
```

---

# 23. Final Architecture

```text
                    JOURNAL
                       │
                 ┌─────┴─────┐
                 ▼           ▼
             Voice Input   Text Input
                 │           │
                 └─────┬─────┘
                       ▼
                Journal Entry
                       │
              ┌────────┼────────┐
              ▼        ▼        ▼
           Content    Mood     Tags
              │
              ▼
            SQLite
              │
              ▼
             FTS5
              │
        ┌─────┴─────┐
        ▼           ▼
   Direct Search   Optional RAG
        │           │
        └─────┬─────┘
              ▼
            Qwen
```

---

# 24. Final Status

**JOURNAL v1 — FINALIZED**

Journal remains intentionally small and lightweight:

- private diary;
- free-form entries;
- voice-first capture;
- optional mood/tags;
- SQLite storage;
- FTS5 search;
- optional RAG for long-term synthesis;
- no unnecessary duplication;
- no automatic diary spam;
- offline/local-first;
- suitable for the Vivo V11.

The subsystem is complete and ready to integrate with the remaining Dexter architecture.
