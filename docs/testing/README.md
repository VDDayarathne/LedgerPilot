# Testing Strategy

Use unit tests for domain rules, integration tests for persistence/security/module boundaries, contract tests between backend and AI service, and end-to-end tests for critical user journeys.

Every tenant-owned feature requires positive access, unauthenticated, unauthorized-role, cross-tenant direct-ID, and cross-tenant indirect-reference coverage. Financial features also require balance, rounding, currency, idempotency, closed-period, reversal, and concurrency cases. AI tests use sanitized fixtures and assert schemas, evidence, confidence routing, retries, and the prohibition on autonomous approvals/postings.

CI will run formatting, linting, unit/integration tests, migration validation, dependency/secret/static checks, and relevant frontend accessibility checks once applications are bootstrapped.

