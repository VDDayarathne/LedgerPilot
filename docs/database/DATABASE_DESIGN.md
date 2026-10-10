# Database Design

## Conventions

- MySQL is the primary database; Liquibase owns every schema change.
- Tables and columns use `snake_case`; primary keys are UUIDs.
- Tenant-owned tables contain non-null `tenant_id` and tenant-leading indexes.
- Store instants as UTC `TIMESTAMP(6)`; use `DATE` for accounting dates.
- Monetary values use `DECIMAL(19,4)` plus a three-letter ISO currency code. Higher provider precision is normalized explicitly.
- Optimistically mutable aggregates contain a `version` column.
- Financial and audit history is retained through status changes, reversals, or superseding versions—not destructive edits.

## Core entities

### Identity and tenancy

- `user_account`: identity, normalized email, status, timestamps.
- `organization`: name, base currency, timezone, fiscal settings, status.
- `membership`: tenant, user, role, status; unique `(tenant_id, user_id)`.
- `invitation`: organization, normalized email, role, token hash, expiry, status.
- `refresh_session`: user, token hash, expiry, revocation metadata.

### Documents and extraction

- `document`: organization, uploader, object key, original name, media type, byte size, SHA-256, scan status, processing status.
- `extraction_job`: document, attempt, state, provider, idempotency key, timestamps, error code.
- `extraction_result`: job, schema version, model/provider version, overall confidence, raw response reference, created timestamp.
- `extracted_field`: result, field path, normalized value, confidence, evidence locator, review state.

A hash supports duplicate detection but does not automatically prove two documents represent the same expense.

### Expenses and approvals

- `expense`: organization, submitter, document, merchant, expense date, currency, subtotal, tax, total, category/account hint, state, version.
- `expense_line`: expense, description, quantity, unit amount, net amount, tax amount, account hint.
- `approval_policy`: organization, name, version, conditions, active dates.
- `approval_step`: policy, ordinal, required role/approver rule.
- `approval_decision`: expense, step, actor, decision, comment, decided timestamp. Decisions are append-only.

### Accounting

- `account`: tenant, code, name, type, currency constraint, active flag; unique `(tenant_id, code)`.
- `accounting_period`: organization, start/end dates, state; periods do not overlap.
- `journal_entry`: organization, source type/id, accounting date, currency, status, description, posted_by/at, reversal link.
- `journal_line`: journal entry, account, debit, credit, description, dimensions.

Database checks require debit and credit to be non-negative and exactly one to be positive per line. Application logic and transactional posting require at least two lines and equal total debits and credits after currency rounding.

### Reconciliation and audit

- `bank_account`: organization, display name, masked identifier, currency, ledger account.
- `bank_import`: organization, bank account, file hash, state, imported_by/at.
- `bank_transaction`: import, stable external key, value date, amount, currency, description; uniqueness is scoped to bank account and provider key.
- `reconciliation_match`: bank transaction, journal line/entry, matched amount, status, actor, timestamps.
- `audit_event`: organization when applicable, actor, action, resource type/id, correlation id, timestamp, sanitized metadata.
- `outbox_event`: aggregate reference, event type, payload, created/published timestamps, attempts.

## Relationships and invariants

- Organization deletion is a controlled retention workflow; tenant financial rows do not cascade-delete.
- A document and every expense derived from it belong to the same organization.
- An approval policy snapshot/version used by an expense cannot be retroactively changed.
- A journal source is unique for its posting purpose, preventing duplicate posting.
- A posted journal and its lines are immutable; correction creates a reversal and replacement.
- Reconciled amounts cannot exceed the absolute bank transaction or eligible journal amount.
- Foreign keys between tenant-owned entities must also be verified as same-tenant in application logic; use composite constraints where practical.

## Migration rules

- Changesets are append-only, uniquely identified, and safe for an existing populated database.
- Add non-null columns through staged backfill when needed.
- Destructive changes require a documented compatibility window and backup/restore plan.
- Production profiles set Hibernate schema validation only; automatic DDL is forbidden.
