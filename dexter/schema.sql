PRAGMA foreign_keys = ON;

PRAGMA journal_mode = WAL;

INTEGER PRIMARY KEY

CREATE TABLE schema_meta (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    schema_version INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

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

CREATE VIRTUAL TABLE journal_entries_fts
USING fts5(
    title,
    content,
    tags,
    content='journal_entries',
    content_rowid='id'
);

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

CREATE VIRTUAL TABLE knowledge_chunks_fts
USING fts5(
    content,
    heading,
    content='knowledge_chunks',
    content_rowid='id'
);

CREATE TABLE system_events (
    id INTEGER PRIMARY KEY,

    event_type TEXT NOT NULL,
    entity_type TEXT,
    entity_id INTEGER,

    occurred_at INTEGER NOT NULL,

    payload TEXT,

    created_at INTEGER NOT NULL
);

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

PRAGMA foreign_keys = ON;
-- Extracted from C:\Users\darkw\Documents\antigravity\mysterious-kepler\dexter\architecture_docs\dexter_database_finance_v1.md

CREATE TABLE finance_accounts (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,

    account_type TEXT NOT NULL
        CHECK (account_type IN (
            'CASH',
            'BANK',
            'WALLET',
            'INVESTMENT',
            'OTHER'
        )),

    currency TEXT NOT NULL DEFAULT 'INR',

    opening_balance_minor INTEGER NOT NULL DEFAULT 0,

    is_active INTEGER NOT NULL DEFAULT 1
        CHECK (is_active IN (0,1)),

    notes TEXT,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE finance_categories (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,

    category_type TEXT NOT NULL
        CHECK (category_type IN (
            'INCOME',
            'EXPENSE'
        )),

    parent_id INTEGER,

    is_active INTEGER NOT NULL DEFAULT 1
        CHECK (is_active IN (0,1)),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (parent_id)
        REFERENCES finance_categories(id)
        ON DELETE RESTRICT,

    UNIQUE(name, category_type, parent_id)
);

CREATE TABLE finance_transactions (
    id INTEGER PRIMARY KEY,

    transaction_type TEXT NOT NULL
        CHECK (transaction_type IN (
            'INCOME',
            'EXPENSE',
            'TRANSFER',
            'ADJUSTMENT'
        )),

    account_id INTEGER NOT NULL,

    transfer_account_id INTEGER,

    category_id INTEGER,

    amount_minor INTEGER NOT NULL
        CHECK (amount_minor > 0),

    occurred_at INTEGER NOT NULL,

    description TEXT,

    reference TEXT,

    source_type TEXT NOT NULL
        CHECK (source_type IN (
            'USER_REPORTED',
            'IMPORTED',
            'DERIVED'
        )),

    status TEXT NOT NULL DEFAULT 'POSTED'
        CHECK (status IN (
            'POSTED',
            'VOID'
        )),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (account_id)
        REFERENCES finance_accounts(id)
        ON DELETE RESTRICT,

    FOREIGN KEY (transfer_account_id)
        REFERENCES finance_accounts(id)
        ON DELETE RESTRICT,

    FOREIGN KEY (category_id)
        REFERENCES finance_categories(id)
        ON DELETE RESTRICT
);

CREATE TABLE finance_recurring_transactions (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,

    transaction_type TEXT NOT NULL
        CHECK (transaction_type IN (
            'INCOME',
            'EXPENSE'
        )),

    account_id INTEGER NOT NULL,

    category_id INTEGER,

    amount_minor INTEGER NOT NULL
        CHECK (amount_minor > 0),

    frequency_type TEXT NOT NULL
        CHECK (frequency_type IN (
            'DAILY',
            'WEEKLY',
            'MONTHLY',
            'YEARLY'
        )),

    next_due_at INTEGER NOT NULL,

    is_active INTEGER NOT NULL DEFAULT 1
        CHECK (is_active IN (0,1)),

    notes TEXT,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (account_id)
        REFERENCES finance_accounts(id)
        ON DELETE RESTRICT,

    FOREIGN KEY (category_id)
        REFERENCES finance_categories(id)
        ON DELETE RESTRICT
);

CREATE TABLE finance_budgets (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,

    category_id INTEGER,

    amount_minor INTEGER NOT NULL
        CHECK (amount_minor > 0),

    period_type TEXT NOT NULL
        CHECK (period_type IN (
            'WEEKLY',
            'MONTHLY',
            'YEARLY'
        )),

    start_date INTEGER NOT NULL,

    is_active INTEGER NOT NULL DEFAULT 1
        CHECK (is_active IN (0,1)),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    FOREIGN KEY (category_id)
        REFERENCES finance_categories(id)
        ON DELETE RESTRICT
);

CREATE TABLE finance_savings_goals (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,

    target_amount_minor INTEGER NOT NULL
        CHECK (target_amount_minor > 0),

    target_date INTEGER,

    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN (
            'ACTIVE',
            'COMPLETED',
            'PAUSED',
            'CANCELLED'
        )),

    notes TEXT,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE finance_savings_contributions (
    id INTEGER PRIMARY KEY,

    goal_id INTEGER NOT NULL,

    amount_minor INTEGER NOT NULL
        CHECK (amount_minor > 0),

    contributed_at INTEGER NOT NULL,

    account_id INTEGER,

    note TEXT,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (goal_id)
        REFERENCES finance_savings_goals(id)
        ON DELETE RESTRICT,

    FOREIGN KEY (account_id)
        REFERENCES finance_accounts(id)
        ON DELETE RESTRICT
);

CREATE TABLE finance_investments (
    id INTEGER PRIMARY KEY,

    name TEXT NOT NULL,

    investment_type TEXT NOT NULL
        CHECK (investment_type IN (
            'STOCK',
            'MUTUAL_FUND',
            'ETF',
            'BOND',
            'OTHER'
        )),

    units REAL,

    total_invested_minor INTEGER NOT NULL DEFAULT 0
        CHECK (total_invested_minor >= 0),

    current_value_minor INTEGER,

    last_valued_at INTEGER,

    notes TEXT,

    is_active INTEGER NOT NULL DEFAULT 1
        CHECK (is_active IN (0,1)),

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_finance_transactions_account_time
ON finance_transactions(account_id, occurred_at);

CREATE INDEX idx_finance_transactions_category_time
ON finance_transactions(category_id, occurred_at);

CREATE INDEX idx_finance_transactions_type_time
ON finance_transactions(transaction_type, occurred_at);

CREATE INDEX idx_finance_recurring_due
ON finance_recurring_transactions(next_due_at);

CREATE INDEX idx_finance_budget_category
ON finance_budgets(category_id);

CREATE INDEX idx_finance_savings_contribution_goal
ON finance_savings_contributions(goal_id, contributed_at);

CREATE INDEX idx_finance_investments_active
ON finance_investments(is_active);

-- Extracted from C:\Users\darkw\Documents\antigravity\mysterious-kepler\dexter\architecture_docs\dexter_database_daily_capture_data_gathering_v1.md

CREATE TABLE capture_templates (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

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

CREATE UNIQUE INDEX idx_capture_response_unique
ON capture_responses(capture_event_id, question_id);

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
