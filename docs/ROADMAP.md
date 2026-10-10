# Roadmap

Status values: `PLANNED`, `IN PROGRESS`, `DONE`, `BLOCKED`. A phase is `DONE` only when all acceptance criteria and verification are complete.

## Planning foundation (`DONE`)

Deliverables: logical monorepo structure, permanent agent instructions, product boundaries, architecture, database model, API/security/accounting/AI conventions, and phased roadmap.

Acceptance criteria:

- required directories and governing documents exist and cross-reference consistently;
- ownership boundaries between backend, web, and AI service are explicit;
- tenant, financial, AI, migration, testing, and no-container constraints are recorded;
- secrets and local artifacts are ignored and configuration keys are exemplified.

## Application Phase 1 — Identity and tenant foundation (`DONE`)

Dependencies: planning foundation.

Scope: Spring Boot/MySQL/Liquibase baseline, users, sessions, organizations, memberships, invitations, role/permission enforcement, audit foundation, and web authentication shell.

Acceptance evidence is recorded in `docs/IMPLEMENTATION_PROGRESS.md`: migrations and application context validate; authentication, refresh rotation, tenant isolation, authorization, invitations, ownership safeguards, and audit integration tests pass; frontend lint and production build pass.

## Application Phase 2 — Account setup and ledger foundation (`PLANNED`)

Dependencies: Application Phase 1.

Scope: chart of accounts, accounting periods, tax/account mappings, deterministic money primitives, and tenant-scoped account administration.

Acceptance criteria: account and period migrations validate on MySQL; all records are tenant-scoped; money uses decimal arithmetic; closed-period and account-status rules are tested; authorization and cross-tenant tests pass.

## Application Phase 3 — Document intake and AI extraction (`PLANNED`)

Dependencies: Application Phases 1–2.

Scope: private storage abstraction, safe upload, document/job model, outbox worker, FastAPI service, OCR adapter, versioned extraction contract, status UI, and manual review.

Acceptance criteria: supported documents process asynchronously; unsafe/oversized uploads fail safely; retries are idempotent; evidence/confidence are visible; outages allow retry/manual entry; contract and tenant tests pass.

## Application Phase 4 — Expenses and approvals (`PLANNED`)

Dependencies: Application Phases 1–3.

Scope: expense lifecycle, line items, policy validation, versioned approval workflows, categories, duplicate warnings, and review UI.

Acceptance criteria: state transitions and permissions are enforced; material edits invalidate approvals; self-approval policy is enforced; concurrent and cross-tenant tests pass; audit history is complete.

## Application Phase 5 — Accounting and posting (`PLANNED`)

Dependencies: Application Phases 2 and 4.

Scope: chart of accounts, periods, tax mappings, balanced draft generation, accountant review, posting, reversals, and exports.

Acceptance criteria: all journals balance deterministically; duplicate posting is impossible; closed-period and account rules hold; posted records are immutable; reversal, rounding, concurrency, and authorization tests pass.

## Application Phase 6 — Bank import and reconciliation (`PLANNED`)

Dependencies: Application Phase 5.

Scope: bank accounts, CSV import, duplicate detection, candidate matching, partial/multi-item confirmation, unmatching, and reconciliation reporting.

Acceptance criteria: re-import is idempotent; suggestions never auto-confirm; matched amounts balance; history survives unmatching; tenant and malformed-file tests pass.

## Application Phase 7 — Production readiness and deployment (`PLANNED`)

Dependencies: Application Phases 1–6.

Scope: performance/accessibility work, monitoring, alerting, backups, restore drill, retention/deletion, threat model, penetration remediation, CI/CD, and only now container/deployment infrastructure.

Acceptance criteria: service-level targets are defined and observed; backup restore and incident drills pass; security baseline in `SECURITY.md` is satisfied; migrations and rollback procedure are rehearsed; production deployment is documented and approved.

## Change control

Mark individual criteria complete with evidence links in this file or a phase report. Scope changes require updates to the PRD and relevant domain documents; architectural changes require an ADR.
