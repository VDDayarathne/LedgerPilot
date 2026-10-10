# Architecture

## System shape

LedgerPilot is a logical monorepo with two implemented applications and one reserved service boundary:

```text
ledgerpilot-frontend -> ledgerpilot-backend -> MySQL / future document storage
                         |
                         +-> durable job/outbox -> FastAPI AI service
```

The backend is the system of record and a modular monolith. The AI service is separately deployable because its runtime, scaling, and failure characteristics differ. It is not a trusted financial decision-maker.

## Backend modules

- `auth`: authentication, sessions, one-time tokens, and mail delivery.
- `identity`: user accounts and profile data.
- `tenant`: organizations, memberships, invitations, and tenant selection.
- `authorization`: roles, permissions, policy checks.
- `documents`: upload metadata, storage references, malware/status checks.
- `extraction`: jobs, extraction results, evidence, review state.
- `expenses`: expense lifecycle, categories, attachments.
- `approvals`: policies and append-only decisions.
- `accounting`: chart of accounts, journals, postings, periods.
- `reconciliation`: bank imports, matching, confirmations.
- `integrations`: provider adapters and webhooks.
- `audit`: immutable business and security event history.
- `common`: narrow technical primitives only; never a dumping ground for domain logic.

The first six modules (`auth`, `identity`, `tenant`, `authorization`, `audit`, and `common`) are implemented. The remaining modules are planned boundaries, not existing functionality.

Each module owns its domain and persistence. Cross-module calls go through application services or ports. Cycles are forbidden. The intended future dependency flow is identity/tenant -> documents/expenses -> approvals -> accounting -> reconciliation, with audit observing committed outcomes through explicit events.

## Application responsibilities

### Backend

Owns authorization, validation, workflow transitions, deterministic accounting, transaction boundaries, idempotency, audit emission, and external orchestration. MySQL is its source of truth. Liquibase exclusively controls schema evolution.

### AI service

Accepts authenticated internal jobs, invokes OCR/model adapters, normalizes candidate fields, calculates confidence, and returns a versioned result with evidence. It may recommend categories or mappings but cannot approve expenses, post journals, or authorize payments.

### Web

Presents workflows, performs usability validation, and calls backend APIs. Backend rules remain authoritative. Generated API clients should derive from the published OpenAPI contract once available.

## Processing model

- Short commands use synchronous REST.
- Upload creates document metadata and a durable job, then returns `202 Accepted`.
- An outbox-backed worker dispatches jobs at least once.
- Workers must be idempotent; retries use bounded exponential backoff and terminal failure states.
- AI results are persisted as new versions, never silently overwritten.
- Events are emitted after successful state changes and include tenant and correlation identifiers.

An in-process scheduler plus database outbox is sufficient initially. Introduce a dedicated broker only with measured throughput or reliability need and an ADR.

## Data and storage

- MySQL stores relational application state.
- Documents use private object storage in production and a local filesystem adapter in development.
- Store object keys, hashes, media metadata, and scan status in MySQL—not public URLs or document bytes.
- Redis, search engines, and caches are deferred until evidence supports them.

## Reliability and observability

- Propagate `X-Correlation-Id` through web, backend, jobs, and AI calls.
- Use structured logs and metrics for request latency, queue age, extraction outcomes, retries, and posting failures.
- Health endpoints distinguish liveness from dependency readiness.
- Timeouts, retry ceilings, and circuit breaking apply to external providers.
- Audit records are not application debug logs.

## Decisions and constraints

- No Docker assets until the deployment phase.
- No distributed transactions. Use local transactions, outbox delivery, idempotency keys, and compensating actions.
- No direct AI-service database access; use versioned internal contracts.
- UTC instants are stored for events. Tenant locale, timezone, and fiscal date rules control presentation and accounting dates.
- New infrastructure, service splits, or module-boundary changes require an ADR in `docs/decisions/`.
