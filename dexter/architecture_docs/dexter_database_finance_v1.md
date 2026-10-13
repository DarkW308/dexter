# Dexter Database — Finance v1

**Status:** FINALIZED  
**Architecture:** Hybrid Model B  
**Database:** SQLite  
**Deployment:** Fully offline / local-only  
**Target hardware:** Dedicated Vivo V11, Android 8  
**AI:** Qwen 2.5 0.5B + Laya  
**Primary external-data mechanism:** Finance Data Capture

---

## 1. Purpose

Finance stores personal financial information needed for Dexter to help with:

- spending;
- income;
- budgets;
- savings;
- financial goals;
- basic investment tracking;
- recurring financial obligations;
- financial summaries.

Finance v1 is intentionally simple.

It is **not accounting software, banking software, tax software, or a financial-market platform**.

---

# 2. Core Architecture

```text
                         FINANCE
                            │
       ┌────────────┬───────┼────────┬────────────┐
       ▼            ▼       ▼        ▼            ▼
    Accounts   Categories Transactions Budgets  Savings
                                │
                                ▼
                           Investments
                                │
                                ▼
                       Financial Goals
                                │
                                ▼
                       Finance Capture
                                │
                                ▼
                         Dexter Context
```

The **transaction record is the source of truth** for normal money movement.

Totals should normally be calculated from transactions rather than manually stored.

---

# 3. Accounts

An account represents where money is held.

Examples:

```text
Cash
Main Bank
Savings Bank
Wallet
Investment Account
```

## SQLite

```sql
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
```

### Rules

- Store monetary amounts as integer **minor units**.
- For INR, ₹100.50 is stored as `10050` paise.
- Never use floating-point numbers for money.
- Account balances are calculated from the opening balance plus transaction effects.
- An account should normally be archived rather than deleted once transactions exist.

---

# 4. Categories

Categories classify income and expenses.

Examples:

```text
FOOD
TRANSPORT
BILLS
EDUCATION
ENTERTAINMENT
SHOPPING
HEALTH
SALARY
FREELANCE
OTHER
```

## SQLite

```sql
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
```

### Rules

- Categories are user-editable.
- Parent categories are optional.
- Do not create hundreds of default categories.
- Transfers do not require income/expense categories.

---

# 5. Transactions

Transactions represent actual money movement.

Supported types:

```text
INCOME
EXPENSE
TRANSFER
ADJUSTMENT
```

## SQLite

```sql
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
```

---

# 6. Transaction Rules

## Income

```text
Account balance ↑
Category = income category
```

Example:

```text
Salary +₹50,000
```

## Expense

```text
Account balance ↓
Category = expense category
```

Example:

```text
Food -₹300
```

## Transfer

```text
Source account ↓
Destination account ↑
```

Example:

```text
Bank → Cash ₹5,000
```

A transfer is **not income and not expense**.

## Adjustment

Used for correcting a known balance discrepancy.

Adjustments must be explicit and should not silently rewrite historical transactions.

---

# 7. Financial Integrity

The application must validate:

```text
TRANSFER:
account_id != transfer_account_id

INCOME:
category must be INCOME

EXPENSE:
category must be EXPENSE

TRANSFER:
category should be NULL

amount_minor > 0
```

Invalid transactions must be rejected before insertion.

---

# 8. Corrections

Historical financial records should not simply be overwritten.

Preferred process:

```text
Incorrect transaction
        ↓
VOID old transaction
        ↓
Create corrected transaction
```

This preserves an understandable history.

User-facing UI may present this as:

> Correct transaction

rather than exposing accounting terminology.

---

# 9. Recurring Transactions

Recurring transactions are needed for things such as:

```text
Rent
Subscription
Salary
Loan payment
Internet bill
Insurance
```

Use a lightweight recurring definition.

```sql
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
```

### Important rule

A recurring definition is **not itself a transaction**.

When it becomes due:

```text
Recurring definition
        ↓
Due
        ↓
Attention / reminder
        ↓
User confirms
        ↓
Actual transaction created
```

Dexter must not automatically claim money was spent unless automatic posting is explicitly enabled in a future version.

---

# 10. Budgets

Budgets define spending limits.

```sql
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
```

### Budget calculation

```text
Budget remaining =
Budget amount
-
qualifying expenses during budget period
```

Transfers are excluded.

Income is excluded.

Voided transactions are excluded.

---

# 11. Budget Alerts

Example thresholds:

```text
70% → informational
85% → warning
100% → exceeded
```

These are defaults and should be configurable.

Attention policy:

```text
Budget approaching limit
        ↓
Attention decision
        ↓
Notification / Dexter message
```

Do not generate repeated alerts for the same threshold unless the user configures them.

---

# 12. Savings Goals

Savings goals track money reserved toward an objective.

Examples:

```text
Emergency Fund
New Laptop
Travel
Education
```

```sql
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
```

## Savings contributions

```sql
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
```

### Rule

Savings contribution is a **goal allocation**, not automatically an expense.

The actual money movement remains represented by transactions where appropriate.

---

# 13. Investment Tracking

Investment tracking is deliberately simple in v1.

Track:

```text
Investment name
Units
Purchase price
Current value
Last updated
```

No live market connection.

```sql
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
```

### Important

Investment values are manually updated in v1.

Dexter must never pretend that an old manually entered market value is current.

---

# 14. Finance Data Capture

Because Dexter is offline and the Vivo V11 is not the user's primary device, finance needs a dedicated capture workflow.

Recommended capture:

```text
MORNING
├── Important financial deadlines
└── Optional balance update

DURING DAY
└── Voice transaction capture

EVENING
├── Missing transactions
├── Money received
├── Money spent
└── Optional balance confirmation

PERIODIC
├── Savings update
└── Investment value update
```

Example:

> "I spent 350 rupees on food."

Dexter should convert this into a candidate transaction:

```text
Type: Expense
Amount: ₹350
Category: Food
Account: ?
Date: today
```

If the account is ambiguous, Dexter asks for the missing information instead of guessing.

---

# 15. Finance Capture Safety

Financial data must use a confirmation rule for ambiguous or consequential actions.

Example:

```text
User:
"I paid 5,000."

Dexter:
"₹5,000 expense. Which account?"

User:
"Main bank."

Dexter:
"Record ₹5,000 from Main Bank as an expense?"

User:
"Yes."

→ Transaction created
```

For simple low-risk data entry, the interaction may be shortened if the required fields are unambiguous.

---

# 16. Financial Queries Dexter Should Support

Examples:

```text
How much did I spend today?

How much did I spend this month?

What category costs me the most?

How much money did I receive this month?

Am I over my food budget?

How much is left in my budget?

How much have I saved for my laptop?

How far am I from my emergency fund target?

What recurring payments are coming?

How much have I invested?

What is my manually recorded investment value?
```

---

# 17. Cross-Domain Connections

Finance may connect to:

```text
Goals
Tasks
Schedule
Alerts & Attention
Daily Capture
Wellbeing
Relationships
Journal
Logging
Briefings
```

Examples:

```text
Finance → Goal
"Save ₹50,000 for laptop"

Finance → Attention
"Budget is nearing limit"

Finance → Schedule
"Rent due tomorrow"

Finance → Wellbeing
"Food spending increased"

Finance → Daily Capture
"Record today's spending"

Finance → Briefing
"₹2,400 spent this week"
```

Cross-domain connections should reference existing records rather than duplicating financial information.

---

# 18. Financial Privacy

Finance is highly sensitive.

Rules:

- SQLite remains local.
- No banking API in v1.
- No cloud synchronization.
- No remote AI.
- No telemetry.
- Financial values must not be copied into generic logs.
- Qwen/Laya receive only the minimum financial context required.
- Raw transaction history should not be placed wholesale into model context.
- Financial exports must be explicit user actions.
- Investment values are treated as sensitive data.
- Deleted/voided financial records must follow the final retention policy.

---

# 19. Performance Rules

Target: old Vivo V11 / Android 8.

Use:

```text
INTEGER primary keys
INTEGER timestamps
INTEGER money amounts in minor units
simple TEXT enums
normal relational tables
targeted indexes
```

Avoid:

```text
large JSON financial objects
full transaction history in memory
unnecessary background calculations
large analytics tables
network dependencies
```

---

# 20. Indexes

```sql
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
```

Indexes should be reviewed using real query patterns on the target device.

---

# 21. What Finance v1 Does NOT Include

```text
Bank API integration
Automatic bank synchronization
Automatic transaction import
Tax accounting
Double-entry accounting system
Invoices
Payroll
Credit scoring
Loan amortization engine
Live stock prices
Live mutual-fund prices
Automatic portfolio rebalancing
Cryptocurrency infrastructure
Financial advice engine
Tax advice
Cloud financial storage
```

These can be future extensions.

---

# 22. Critical Financial Rules

```text
RULE 01
Transactions are the primary source of truth for normal money movement.

RULE 02
Money must use integer minor units, never floating-point storage.

RULE 03
A transfer is not income and not an expense.

RULE 04
A savings contribution is not automatically an expense.

RULE 05
Investment value is manually updated in v1.

RULE 06
Dexter must not claim manually recorded investment values are live.

RULE 07
Historical financial records should not be silently overwritten.

RULE 08
Corrections should normally void the incorrect record and create a corrected record.

RULE 09
Recurring transaction definitions are not actual transactions.

RULE 10
Recurring transactions require confirmation before posting unless a future explicit automation policy allows otherwise.

RULE 11
Unknown account/category information must not be guessed when financially consequential.

RULE 12
Ambiguous financial commands require clarification or confirmation.

RULE 13
Budget calculations exclude transfers.

RULE 14
Budget calculations exclude void transactions.

RULE 15
Budget alerts must not repeatedly spam the user.

RULE 16
Financial data remains local and offline.

RULE 17
No financial information is sent to a remote AI service.

RULE 18
Qwen/Laya receive minimum necessary financial context.

RULE 19
Sensitive financial information must not enter generic application logs.

RULE 20
Account history must remain understandable and recoverable.

RULE 21
Accounts with historical transactions should be archived rather than casually deleted.

RULE 22
Categories should remain simple and user-editable.

RULE 23
Do not build accounting complexity that v1 does not require.

RULE 24
Do not add banking integration until the core offline architecture is stable.

RULE 25
Unknown data remains unknown; Dexter must never invent a transaction.

RULE 26
User confirmation is required whenever an ambiguous command could create a financially meaningful record.

RULE 27
Finance Data Capture is the primary mechanism for information Dexter cannot obtain automatically.

RULE 28
Finance data should be summarized before being supplied to Qwen/Laya.

RULE 29
Raw transactions remain the source of truth; summaries are derived.

RULE 30
All future finance extensions must preserve the offline-first privacy model.
```

---

# 23. Final Finance Architecture

```text
                         FINANCE
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
             ACCOUNTS              CATEGORIES
                │                       │
                └───────────┬───────────┘
                            ▼
                       TRANSACTIONS
                            │
             ┌──────────────┼──────────────┐
             ▼              ▼              ▼
          BUDGETS       SAVINGS       INVESTMENTS
             │              │              │
             └──────────────┼──────────────┘
                            ▼
                       FINANCE DATA
                         CAPTURE
                            │
                            ▼
                       DERIVED DATA
                            │
                            ▼
                      GOALS / ATTENTION
                            │
                            ▼
                       BRIEFINGS
                            │
                            ▼
                      DEXTER CONTEXT
                            │
                       ┌────┴────┐
                       ▼         ▼
                     LAYA      QWEN
```

---

# 24. Final Status

**FINANCE v1 — FINALIZED**

The design is intentionally lightweight enough for implementation by a normal software engineer on the target hardware while providing the core financial capabilities Dexter needs:

- accounts;
- transactions;
- categories;
- transfers;
- recurring payments;
- budgets;
- savings;
- simple investment tracking;
- finance data capture;
- financial attention;
- privacy;
- audit-friendly correction;
- cross-domain integration.

The architecture is deliberately extensible so more advanced finance capabilities can be added later without replacing the core transaction model.
