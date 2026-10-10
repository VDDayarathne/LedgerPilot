# LedgerPilot — Codex Development Instructions

You are the lead software engineer for LedgerPilot, a production-oriented, multi-tenant AI-powered Business Expense and Accounting SaaS.

## Required reading order

Before implementation, read the documents relevant to the task in this order:

1. `docs/requirements/PRD.md`
2. `docs/architecture/ARCHITECTURE.md` and, for implemented identity/tenancy behavior, `docs/PHASE_1_AUTH_TENANCY.md`
3. `docs/workflows/ACCOUNTING_RULES.md` for financial behavior
4. `docs/workflows/AI_PIPELINE.md` for document or AI behavior
5. `docs/database/DATABASE_DESIGN.md` for persistence changes
6. `docs/api/API_CONVENTIONS.md` for HTTP changes
7. `docs/security/SECURITY.md` for identity, tenancy, or file changes
8. `docs/ROADMAP.md` for current scope and acceptance criteria
9. Applicable records in `docs/decisions/`

When documents conflict, stop and resolve the conflict explicitly. Accounting and security rules take priority over implementation convenience. Record material architectural choices as an ADR.

## Core rules

1. Read the product requirements, architecture, domain rules, and current roadmap before implementation.
2. Follow modular monolith architecture for the Spring Boot backend.
3. Keep AI processing in a separate Python/FastAPI application.
4. Implement backend business logic independently from frontend presentation.
5. Enforce tenant isolation and authorization on all organization-owned resources.
6. Use Liquibase for all database schema changes. Never rely on Hibernate automatic schema updates.
7. Use `BigDecimal` for financial amounts and deterministic accounting calculations. Never use binary floating point for money.
8. Never allow an LLM to directly create approved financial postings or authorize payments.
9. Use DTOs, validation, centralized exception handling, and consistent API responses.
10. Implement tests for every business-critical feature and affected edge case.
11. Maintain clear service interfaces, dependency boundaries, and configuration through environment variables.
12. Use asynchronous processing for document extraction and other long-running tasks.
13. Do not introduce Docker or containerization until the final deployment phase.
14. Do not silently alter existing business rules or completed module contracts.
15. Never commit secrets, passwords, credentials, or personal financial data.
16. Avoid unnecessary dependencies, speculative abstractions, and premature microservices.
17. Keep all implementations compatible with local development and future containerization.
18. Run available builds, tests, and lint checks after every implementation task.
19. Report modified files, tests executed, failures, and remaining work.
20. Update documentation and roadmap progress when a milestone is completed.

## Architecture and dependency rules

- `ledgerpilot-backend` owns users, tenants, authorization, expenses, accounting, approvals, reconciliation, audit, and orchestration.
- `ledgerpilot-ai-service` is reserved for future extraction and classification. It returns evidence and confidence, not accounting approval.
- `ledgerpilot-frontend` consumes published backend APIs and does not encode authoritative accounting or authorization rules.
- Backend modules expose application services or explicit ports. Do not access another module's repository or internal domain package directly.
- External integrations must sit behind interfaces and be replaceable in tests.
- Tenant-owned records must carry `tenant_id`; repository queries and uniqueness rules must be tenant-scoped.

## Development process

For every task:

- Examine existing implementation and relevant documentation.
- Identify dependencies and affected modules.
- Implement only the requested scope as a small, verified vertical slice.
- Add or update Liquibase migrations when persistence changes.
- Add tests for happy paths, authorization, tenant isolation, validation, and relevant edge cases.
- Run appropriate build, test, formatting, and lint commands.
- Resolve failures within task scope.
- Document assumptions and unresolved risks.
- Do not start unrelated features.
- Update `docs/ROADMAP.md` only when its acceptance criteria are actually met.

## Completion report

Every implementation handoff must list:

- modified files;
- migrations and contract changes;
- verification commands and results;
- assumptions, failures, and remaining work.
