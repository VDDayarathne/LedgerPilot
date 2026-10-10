# Implementation Progress

## Current status

Phase 1 authentication, authorization, and shared-schema multi-tenancy is implemented end to end. A user can register, verify email, sign in, restore/refresh/logout sessions, create and select organizations, invite another verified user, accept once with the matching email, switch organizations, and administer membership access subject to fixed roles. The responsive React UI supports these flows, profile/password settings, permission-aware navigation, and session-expired/access-denied states.

- **Project foundation — completed:** Spring Boot 4.1.1/Java 21 backend, React 19/TypeScript/Vite frontend, MySQL/JPA/Liquibase persistence.
- **Phase 1 identity and sessions — completed:** registration, verification, login, current user, rotating refresh, replay-family revocation, logout/all sessions, recovery/reset, password change, and profile update.
- **Phase 1 tenancy and authorization — completed:** organization lifecycle, server-validated tenant selection, memberships, fixed permissions, invitations, membership status/role changes, ownership transfer, and tenant audit events.
- **Phase 1 frontend — completed:** all requested identity and organization administration pages plus coordinated single-flight refresh and protected routes.
- **Accounting features — planned:** expenses, documents, ledger, billing, OCR, AI, and other later-phase modules remain intentionally unimplemented.

**Next implementation step:** run the integration suite against MySQL/Testcontainers in CI, then begin the approved account-setup/ledger foundation without weakening the tenant-isolation convention documented in `docs/PHASE_1_AUTH_TENANCY.md`.

## Feature record

### Phase 1: authentication, authorization, and multi-tenancy — completed

**Business purpose:** gives global users secure identities and isolates each organization's administration before any financial data is introduced.

**Backend implementation**

- Capability modules: `auth`, `identity`, `tenant`, `authorization`, `audit`, and narrowly shared `common` code.
- BCrypt password hashing; hashed, expiring, single-use verification/reset/invitation tokens.
- Short-lived HS256 JWT access tokens with issuer/audience/expiry/algorithm validation; access tokens remain in frontend memory.
- Opaque rotating refresh tokens in configurable `HttpOnly` cookies; only SHA-256 hashes persist. Pessimistic locking coordinates refresh, and previous-token replay revokes the session family.
- Double-submit CSRF protection for cookie-authenticated refresh/logout, explicit credentialed CORS origins, and per-instance rate limits for sensitive public endpoints.
- Global users, shared-schema tenants, membership-scoped roles, active membership/tenant checks on every tenant service operation, and explicit validated tenant switching.
- Organization creation and owner membership are atomic. General role payloads cannot assign `OWNER`; atomic ownership transfer promotes the target and demotes the current owner. Owners cannot be altered through general membership operations.
- Invitation tenant/email/role/expiry/status binding, matching verified identity, token locking, replay rejection, duplicate prevention, and cancellation.
- Paginated members, invitations, and audit events. Audit records contain actor, tenant where relevant, target, action, request ID/IP context, and time—never credentials or raw tokens.
- Versioned REST endpoints under `/api/v1` with validated DTOs and consistent application error bodies.

**Frontend implementation**

- Registration/verification, login, forgot/reset password, organization creation/selection, dashboard, profile/password, organization settings, members/invitations/roles, invitation acceptance, access denied, and expired-session experiences.
- Axios client with access token in memory, credentialed cookies, CSRF header, one coordinated refresh promise, one bounded request retry, and refresh-loop prevention.
- Session restoration after reload, state clearing on logout, selected-tenant token replacement, protected routes, and role-aware administrative navigation.
- Responsive custom styling replacing the Vite starter interface; loading, validation, success, error, and empty states are represented.

**Persistence and migrations**

- Liquibase changeset: `ledgerpilot-backend/src/main/resources/db/changelog/changes/001-phase-one.yaml`.
- Tables: `users`, `tenants`, `memberships`, `invitations`, `refresh_sessions`, `one_time_tokens`, and `audit_events`.
- Foreign keys, tenant-leading indexes, email/slug/token uniqueness, tenant-user membership uniqueness, timestamps, and optimistic versions are present.
- Hibernate remains `ddl-auto=validate`; production schema mutation is migration-only.

**Architecture decisions**

- Shared database/shared schema with mandatory tenant scoping for tenant-owned records. Future repositories must use `(tenant_id, id)` access; browser-provided tenant IDs are not authority.
- Permissions come from the current database membership rather than long-lived JWT role claims, so suspension/removal/role changes affect subsequent tenant operations immediately.
- Fixed roles are sufficient for Phase 1 and prevent premature custom-policy complexity. The role matrix and future accounting responsibilities are in `docs/PHASE_1_AUTH_TENANCY.md`.
- A development-only in-memory mailbox exposes test tokens without production logging. Production uses Spring mail configuration.
- H2 MySQL mode provides local constraint/transaction integration coverage; real MySQL/Testcontainers remains a CI hardening step.

**Configuration requirements**

| Variable | Purpose | Local default/example |
| --- | --- | --- |
| `SERVER_PORT` | Backend HTTP port | `8080` |
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/ledgerpilot` |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials | Local-only defaults exist; override outside development |
| `FRONTEND_ORIGIN` | Exact credentialed CORS origin | `http://localhost:5173` |
| `FRONTEND_BASE_URL` | Email-link web origin | `http://localhost:5173` |
| `AUTH_JWT_SECRET` | HMAC signing secret, minimum 32 random bytes | Development fallback only; mandatory override in deployment |
| `AUTH_JWT_ISSUER`, `AUTH_JWT_AUDIENCE` | JWT trust boundaries | `ledgerpilot`, `ledgerpilot-web` |
| `ACCESS_TOKEN_MINUTES`, `REFRESH_TOKEN_DAYS` | Session lifetimes | `10`, `30` |
| `AUTH_COOKIE_SECURE`, `AUTH_COOKIE_SAME_SITE` | Refresh/CSRF cookie policy | `false`, `Lax` locally; review for deployment topology |
| `MAIL_FROM`, `SPRING_MAIL_*` | Production mail transport | No production credentials checked in |
| `VITE_API_URL` | Frontend API base | `http://localhost:8080/api/v1` |

Examples live in each application's `.env.example`. Use `SPRING_PROFILES_ACTIVE=dev` for the in-memory mailbox.

**Known limitations / external requirements**

- Production requires MySQL, HTTPS, strong external JWT secret, explicit deployed frontend origin, secure cookie settings, and working SMTP configuration.
- The rate limiter and dev mailbox are process-local; use shared infrastructure for multi-instance production rate limiting and a durable mail/outbox strategy.
- Integration tests currently use H2 in MySQL compatibility mode, not a live MySQL engine.
- Frontend does not yet expose audit-event browsing or an ownership-transfer button; both backend APIs are implemented and documented.
- No accounting, document, subscription, OCR, AI, PayHere, platform super-admin, or tenant-impersonation behavior exists.

## Verification record

| Date | Check | Result |
| --- | --- | --- |
| 2026-10-10 | `gradlew.bat test` with Java 21 | Passed: 4 tests covering Spring context, Liquibase/Hibernate validation, browser CORS preflight, registration/verification/login, refresh rotation and family revocation, password-reset expiry/single-use, organization selection, invitation acceptance/replay, role escalation denial, final-owner protection, membership suspension, and permission enforcement. |
| 2026-10-10 | `npm.cmd run lint` | Passed with no reported warnings or errors. |
| 2026-10-10 | `npm.cmd run build` | Passed: TypeScript and Vite production build, 83 modules transformed. |
| 2026-10-10 | Browser/API connectivity regression | Passed: configured-origin preflight returns origin and credentials headers; Vite `/api` proxy reaches backend validation endpoint. |

## Change log

### 2026-10-10 — Phase 1 completed

- Added the Phase 1 Liquibase schema and modular backend implementation.
- Added secure browser session/token handling, identity recovery, tenant selection, fixed authorization, organization/membership/invitation workflows, ownership safeguards, and audit events.
- Replaced the Vite starter with the Phase 1 responsive application and protected API client.
- Added API/architecture/security/permission documentation, environment examples, local email testing instructions, and integration coverage.
- Corrected explicit CORS source wiring, added a browser-preflight regression test, and routed local frontend API calls through the Vite same-origin proxy. Development health now ignores SMTP because the dev mailbox replaces SMTP delivery.

### 2026-10-10 — Project record initialized

- Audited and documented the original backend/frontend foundations.
