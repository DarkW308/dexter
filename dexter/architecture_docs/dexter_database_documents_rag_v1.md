# Dexter Database — Documents / Knowledge / RAG v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**AI:** Qwen 2.5 0.5B + Laya  
**Storage ceiling:** 20 GB for the complete Dexter database/data layer

---

## 1. Purpose

Documents / Knowledge / RAG provides Dexter with access to personal and work knowledge without requiring the small local model to keep large amounts of information in its context.

The goal is:

> **Maximum useful intelligence per MB and per CPU cycle.**

The system should make Dexter highly effective over the user's own information while remaining fast and lightweight on old hardware.

It is not a general-purpose cloud RAG platform.

---

# 2. Core Architecture

```text
                         DEXTER DATA
                              │
             ┌────────────────┼────────────────┐
             ▼                ▼                ▼
          SQLite          Documents         Knowledge
       source of truth    original files      files
             │                │                │
             └────────────────┼────────────────┘
                              ▼
                    Retrieval / Search Layer
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
              Structured query      Document search
                    │                   │
                    └─────────┬─────────┘
                              ▼
                       Re-ranking Layer
                              │
                              ▼
                       Context Builder
                              │
                              ▼
                    Compact Markdown Context
                              │
                              ▼
                         Qwen 2.5 0.5B
                              │
                              ▼
                            Laya
```

---

# 3. Source-of-Truth Policy

### SQLite

SQLite is the authoritative source for structured Dexter data:

```text
Tasks
Schedule
Goals
Projects
Alerts & Attention
Wellbeing
Finance
Relationships
Journal metadata
Logging / Audit
Document metadata
RAG metadata
```

Structured information must not be duplicated unnecessarily into permanent Markdown or JSON copies.

---

# 4. Format Policy

## SQLite

Primary persistent storage for structured data.

Use for:

- records;
- relationships;
- dates;
- status;
- numeric values;
- identifiers;
- indexes;
- retrieval metadata.

## Markdown

Preferred **LLM-facing context format**.

Use for:

- compact context;
- retrieved knowledge;
- document summaries;
- relationship context;
- project context;
- structured briefing context.

Markdown is preferred because it is human-readable and easy for a small language model to interpret.

## JSON

Preferred machine-interchange representation.

Use for:

- internal component communication;
- serialization where structured data is required;
- temporary context objects;
- import/export where appropriate.

JSON should not be dumped wholesale into Qwen context.

## Original Documents

Preserve original documents when required:

```text
PDF
DOCX
TXT
MD
CSV
etc.
```

Do not destroy originals merely because text extraction has been performed.

---

# 5. Knowledge Classification

Every document/knowledge item should have a lightweight classification.

```text
CRITICAL
PERSONAL
WORK
PROJECT
REFERENCE
ARCHIVE
```

### Meaning

**CRITICAL**
- Frequently important information.
- Highest retrieval priority.

**PERSONAL**
- Personal reference and life information.

**WORK**
- Professional information.

**PROJECT**
- Information belonging to an active project.

**REFERENCE**
- Useful but lower-priority material.

**ARCHIVE**
- Rarely accessed information.

Classification is primarily for retrieval priority and lifecycle management.

---

# 6. Documents

## SQLite

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

### Rules

- `storage_path` points to the original/local file.
- `content_hash` is used for duplicate detection.
- File size is tracked to enforce the storage budget.
- Failed indexing must not destroy the original file.
- Archived documents remain recoverable.
- Deleted documents must have their search/index data removed.

---

# 7. Document Chunks

Large documents should not be placed directly into Qwen context.

They are divided into smaller searchable sections.

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

### Chunking principles

Prefer semantic boundaries:

```text
Document
 ├── Section
 │    ├── subsection
 │    └── subsection
 └── Section
```

Avoid arbitrary splitting when headings or paragraphs provide better boundaries.

Chunks should be small enough for retrieval but large enough to preserve meaning.

---

# 8. Tags

Tags provide cheap retrieval filtering.

```sql
CREATE TABLE knowledge_tags (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL UNIQUE,

    created_at INTEGER NOT NULL
);
```

Relationship table:

```sql
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

Tags should remain lightweight.

Do not create large automated tag vocabularies without evidence that they improve retrieval.

---

# 9. Search Strategy

Dexter uses a staged retrieval strategy.

```text
User request
     ↓
Intent / domain detection
     ↓
Structured database query
     ↓
Metadata / title / tag search
     ↓
Keyword search
     ↓
Semantic search when needed
     ↓
Re-ranking
     ↓
Context selection
     ↓
Markdown context
     ↓
Qwen
```

### Principle

**Use the cheapest retrieval method that can answer the request.**

Do not perform semantic search for every query.

---

# 10. Structured Retrieval First

If a request can be answered from existing structured data, RAG should not be used.

Examples:

```text
What do I have today?
→ Schedule + Tasks

How much did I spend?
→ Finance

Who needs a follow-up?
→ Relationships

What is my current project status?
→ Projects + Tasks
```

This saves:

- CPU;
- memory;
- storage;
- context length;
- model reasoning effort.

---

# 11. Metadata Retrieval

Before semantic search, search:

```text
Title
Filename
Knowledge class
Project
Tags
Headings
Dates
```

Example:

> "Find the Dexter database storage decision."

Search:

```text
title
tags
project
heading
keyword
```

before using semantic retrieval.

---

# 12. Keyword Search

Use lightweight local keyword/full-text search where practical.

Recommended SQLite approach:

```text
SQLite FTS5
```

for document/chunk text search.

Conceptual index:

```sql
CREATE VIRTUAL TABLE knowledge_chunks_fts
USING fts5(
    content,
    heading,
    content='knowledge_chunks',
    content_rowid='id'
);
```

FTS5 is preferred over maintaining a large custom search engine.

---

# 13. Semantic Retrieval

Semantic/vector retrieval is optional and should be used only when cheaper retrieval is insufficient.

Reasons:

- embeddings consume storage;
- embedding generation consumes CPU;
- vector search consumes memory/processing;
- old hardware must remain responsive.

Therefore:

```text
Exact/structured
       ↓
FTS/keyword
       ↓
Semantic
```

not:

```text
Every query
    ↓
Embedding
    ↓
Vector search
```

---

# 14. Embedding Policy

If semantic retrieval is enabled:

- embeddings must be generated locally;
- no cloud embedding API;
- embeddings should be created only for active/relevant documents;
- archived material may omit embeddings;
- embeddings should be replaceable if the embedding model changes;
- embedding storage must count toward the 20 GB budget.

Do not store multiple embedding versions unless there is a demonstrated need.

---

# 15. Re-ranking

Retrieved candidates should be ranked using inexpensive signals.

Recommended conceptual score:

```text
score =
    keyword relevance
  + title/heading relevance
  + tag relevance
  + knowledge priority
  + project relevance
  + recency where appropriate
  + semantic similarity where available
```

Do not use an additional large model solely for re-ranking on the target device.

---

# 16. Knowledge Priority

Default retrieval priority:

```text
CRITICAL
    ↓
PROJECT / PERSONAL / WORK
    ↓
REFERENCE
    ↓
ARCHIVE
```

This is a ranking preference, not an absolute exclusion rule.

A relevant archive document may still outrank an irrelevant critical document.

---

# 17. Context Builder

The Context Builder is one of the most important components.

Its job is to convert raw retrieved data into a small, coherent context for Qwen.

```text
Retrieved records
      ↓
Remove duplicates
      ↓
Rank
      ↓
Trim irrelevant text
      ↓
Preserve source
      ↓
Assemble Markdown
      ↓
Qwen
```

Example:

```markdown
# Dexter Database Storage Policy

## Relevant facts

- Total Dexter data ceiling: 20 GB.
- Target device: Vivo V11.
- Database: SQLite.
- Priority: fast and lightweight.
- Structured data stays in SQLite.
- Markdown is preferred for LLM-facing context.
- JSON is used for machine interchange.
- Semantic retrieval is used only when cheaper retrieval is insufficient.

## Source

dexter_database_documents_rag_v1.md
```

This is preferable to giving Qwen raw database rows or entire documents.

---

# 18. Context Budget

Qwen must receive only the information necessary for the current request.

Rules:

```text
No entire database dump.
No entire document dump unless explicitly requested.
No unrelated domains.
No duplicate facts.
No unnecessary metadata.
```

The Context Builder should enforce a hard context budget.

The exact token limit should be configurable after measuring the Qwen model's real performance on the target device.

---

# 19. Source Tracking

Every retrieved knowledge item must retain its source.

Minimum source information:

```text
document_id
chunk_id
document title
heading
```

Dexter should be able to answer:

> "Where did you get that?"

with a useful source reference.

---

# 20. Confidence / No-Answer Behavior

If retrieval finds weak or conflicting evidence, Dexter must not fabricate an answer.

Preferred behavior:

```text
Strong evidence
→ answer

Partial evidence
→ answer with uncertainty

No reliable evidence
→ say information was not found
```

Example:

> "I found two notes about this, but neither contains the final decision. I don't have enough information to say which version was approved."

This is especially important because Qwen 0.5B has limited reasoning capacity.

---

# 21. Duplicate Detection

Use:

```text
content_hash
```

to detect identical files/content.

If the same document is imported twice:

```text
Existing hash
     ↓
Duplicate detected
     ↓
Avoid duplicate indexing
```

Do not create duplicate chunks unnecessarily.

---

# 22. Document Lifecycle

```text
IMPORT
  ↓
VALIDATE
  ↓
HASH
  ↓
EXTRACT
  ↓
CHUNK
  ↓
INDEX
  ↓
ACTIVE
  ↓
ARCHIVE
  ↓
DELETE
```

Failure at any stage must preserve the original document.

---

# 23. Storage Budget

Hard ceiling:

> **20 GB for the complete Dexter database/data layer.**

Recommended planning budget:

| Component | Target |
|---|---:|
| SQLite/core data | 1–2 GB |
| Original documents | 8–10 GB |
| Search/index data | 1–2 GB |
| RAG metadata/cache | <1 GB |
| Logs/temp/workspace | 1–2 GB |
| Reserved headroom | 4–6 GB |
| **Total** | **≤20 GB** |

These are planning targets, not guaranteed allocations.

The system must preserve free headroom.

---

# 24. Storage Enforcement

The application should track:

```text
database size
document size
index size
cache size
temporary files
total Dexter data size
```

When approaching the configured storage threshold:

```text
Warning
    ↓
Stop non-essential indexing
    ↓
Suggest archive/cleanup
    ↓
Prevent uncontrolled growth
```

Never allow RAG caches or indexes to silently consume the remaining storage.

---

# 25. Cache Rules

Caches must be:

- bounded;
- disposable;
- rebuildable;
- excluded from source-of-truth data.

If storage becomes constrained:

```text
Delete cache
    ↓
Delete derived indexes if necessary
    ↓
Rebuild later
```

Do not delete authoritative user data to solve a cache problem.

---

# 26. Archive Strategy

Archived documents:

- remain available;
- have lower retrieval priority;
- may have embeddings removed;
- may retain FTS indexing depending on size;
- can be re-indexed when needed.

This allows old knowledge to remain accessible without keeping expensive retrieval artifacts permanently.

---

# 27. Document Compression

Do not blindly compress every file.

Prefer:

- preserve already-compressed formats;
- avoid duplicate extracted copies;
- store extracted text only when it provides clear retrieval value;
- remove temporary extraction files;
- avoid multiple redundant representations.

Markdown is useful for LLM-facing context, but the system should not permanently store a Markdown duplicate of every document unless there is a concrete reason.

---

# 28. Relationship to Existing Domains

Documents may reference:

```text
Projects
Goals
Tasks
People
Journal
Wellbeing
Finance
Schedule
```

Use foreign keys or lightweight references where appropriate.

Example:

```text
Document
  ↓
Project
  ↓
Tasks
```

A project document should not duplicate project/task data.

---

# 29. Knowledge vs Structured Memory

This distinction is mandatory.

### Structured memory

Use SQLite:

```text
Task deadline
Budget amount
Birthday
Goal status
Project status
Sleep record
Transaction
```

### Unstructured knowledge

Use documents/RAG:

```text
Meeting notes
Research paper
Project specification
Personal reference note
Technical documentation
Long-form work document
```

Do not turn every structured record into a document.

Do not force every document fact into relational columns.

---

# 30. Personal Knowledge

Personal knowledge may be stored as Markdown where appropriate.

Example:

```markdown
# Dexter Preferences

## Development

- Prefer simple architecture.
- Target old hardware.
- Keep the system offline-first.

## Communication

- Prefer concise explanations.
```

Such knowledge can be indexed and retrieved like other knowledge.

Sensitive information should remain protected.

---

# 31. LLM Context Policy

The model should receive:

```text
User request
+
Relevant structured facts
+
Relevant retrieved knowledge
+
Necessary source information
```

Not:

```text
Entire SQLite database
+
Entire document collection
+
Entire relationship history
```

This is one of the primary mechanisms for making a small model effective.

---

# 32. Flagship-Like Experience Strategy

The system cannot make a 0.5B model intrinsically equivalent to a flagship LLM.

However, Dexter can improve task-specific performance through:

```text
Better retrieval
+
Structured memory
+
Relevant context
+
Context compression
+
Source tracking
+
Domain-aware prompts
+
Deterministic database queries
+
Laya execution
```

The goal is:

> **Flagship-like usefulness over Dexter's personal data, not flagship-level general intelligence.**

---

# 33. Performance Rules

Target:

```text
Vivo V11
Android 8
Low RAM
Limited storage
Offline operation
```

Rules:

1. Prefer SQLite queries over loading large files.
2. Prefer FTS5 over heavyweight search infrastructure.
3. Avoid vector search unless useful.
4. Do not embed every document by default.
5. Do not load full documents into memory.
6. Stream large documents when necessary.
7. Keep indexes rebuildable.
8. Bound caches.
9. Keep context small.
10. Avoid background indexing while Dexter is handling a user request.
11. Schedule expensive indexing during low-activity periods.
12. Measure actual latency on the target device before adding complexity.

---

# 34. Background Indexing

Document indexing should be asynchronous.

```text
User adds document
        ↓
Store original immediately
        ↓
Queue indexing
        ↓
Extract
        ↓
Chunk
        ↓
FTS index
        ↓
Optional semantic index
```

Dexter should remain responsive.

If the device is under resource pressure:

```text
Pause indexing
Resume later
```

---

# 35. Retrieval Failure

If indexing is unavailable:

```text
Try structured search
        ↓
Try metadata
        ↓
Try FTS
        ↓
If unavailable:
report limitation
```

Dexter should not fail completely because semantic indexing is unavailable.

---

# 36. Security and Privacy

Rules:

- local-only storage;
- no cloud RAG;
- no remote embeddings;
- no remote document processing;
- no document telemetry;
- no document content in generic logs;
- minimum context to Qwen;
- sensitive documents can be excluded from AI retrieval;
- document deletion must remove derived indexes;
- original documents remain under user control.

---

# 37. What v1 Does NOT Include

```text
Cloud vector database
Cloud embeddings
Large vector infrastructure
Multiple embedding models
Large re-ranking model
Automatic internet research
Automatic web ingestion
Continuous background crawling
Automatic social-media ingestion
Automatic email ingestion
Multi-user knowledge sharing
Large knowledge graph
GPU-dependent retrieval
Complex distributed RAG
```

These are intentionally excluded.

---

# 38. Recommended Implementation Order

```text
Phase 1
SQLite document metadata

Phase 2
Original-file storage

Phase 3
Text extraction

Phase 4
Semantic chunking

Phase 5
SQLite FTS5

Phase 6
Context Builder

Phase 7
Source tracking

Phase 8
Storage monitoring

Phase 9
Optional lightweight semantic retrieval

Phase 10
Performance testing on Vivo V11
```

Do not start with embeddings.

Build the cheap retrieval path first.

---

# 39. Critical RAG Rules

```text
RULE 01
SQLite is the authoritative source for structured Dexter data.

RULE 02
Markdown is the preferred LLM-facing context format.

RULE 03
JSON is the preferred machine-interchange format.

RULE 04
Original documents should be preserved where required.

RULE 05
Do not duplicate structured database data unnecessarily.

RULE 06
Use structured database queries before RAG whenever possible.

RULE 07
Use metadata/title/tag retrieval before expensive semantic retrieval.

RULE 08
Use FTS5 as the primary lightweight text-search mechanism.

RULE 09
Semantic retrieval is optional and conditional.

RULE 10
Do not embed every document by default.

RULE 11
All embeddings must be generated locally.

RULE 12
Embeddings count against the 20 GB storage ceiling.

RULE 13
Archived documents may have expensive retrieval artifacts removed.

RULE 14
RAG caches must be bounded and rebuildable.

RULE 15
No cache may consume storage needed for authoritative user data.

RULE 16
Retrieved context must be deduplicated.

RULE 17
Qwen must receive only relevant context.

RULE 18
Entire databases must never be dumped into model context.

RULE 19
Entire documents should not be sent to Qwen unless explicitly necessary.

RULE 20
Every retrieved knowledge item must preserve its source reference.

RULE 21
Weak retrieval evidence must not be presented as certain fact.

RULE 22
When reliable information cannot be found, Dexter must say so.

RULE 23
Document indexing must not block normal Dexter interaction.

RULE 24
Expensive indexing should pause under resource pressure.

RULE 25
The system must remain useful if semantic retrieval is unavailable.

RULE 26
The total Dexter database/data layer must remain under the 20 GB ceiling.

RULE 27
Storage headroom must be preserved.

RULE 28
Document duplication must be minimized.

RULE 29
Structured memory and unstructured knowledge must remain conceptually separate.

RULE 30
The architecture must optimize intelligence-per-resource rather than maximum stored data.

RULE 31
The RAG system must remain fully offline.

RULE 32
No remote AI service may receive document content.

RULE 33
Context generation should favor concise Markdown over raw database structures.

RULE 34
RAG complexity must be justified by measurable improvement.

RULE 35
Future hardware upgrades may enable stronger retrieval without requiring a database redesign.
```

---

# 40. Final Architecture

```text
                         USER REQUEST
                              │
                              ▼
                     Intent / Domain Router
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
       Structured          Metadata            RAG
         Query              Search            Search
            │                 │                 │
            │                 ▼                 ▼
            │              FTS5             Semantic*
            │                 │                 │
            └─────────────────┼─────────────────┘
                              ▼
                         Re-ranking
                              │
                              ▼
                       Context Builder
                              │
                              ▼
                   Compact Markdown Context
                              │
                              ▼
                       Qwen 2.5 0.5B
                              │
                              ▼
                             Laya
                              │
                              ▼
                    Answer / Action / Voice

* Semantic retrieval is optional and conditional.
```

---

# 41. Final Status

**DOCUMENTS / KNOWLEDGE / RAG v1 — FINALIZED**

The architecture is designed specifically for Dexter's constraints:

- 20 GB total storage ceiling;
- old Vivo V11;
- Android 8;
- limited RAM;
- offline operation;
- Qwen 2.5 0.5B;
- Laya execution layer;
- SQLite source of truth.

The system prioritizes **structured retrieval first, lightweight text search second, semantic retrieval only when justified, and compact Markdown context for the model**.

This provides a practical path toward a highly capable personal assistant without attempting to reproduce the infrastructure of a cloud-scale RAG system.
