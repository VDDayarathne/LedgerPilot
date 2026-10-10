# Phase 1: Authentication, Authorization, and Multi-tenancy

## Architecture

The backend is a capability-oriented modular monolith under `com.ledgerpilot.ledgerpilot_backend`: `auth`, `identity`, `tenant`, `authorization`, `audit`, and `common`. Controllers own HTTP translation, application services own transactions, repositories own persistence, and DTOs prevent secrets or persistence internals from entering API responses.

Tenancy uses one shared database and schema. A global user may have memberships in multiple organizations. Each membership owns exactly one fixed role. The access token identifies the user, session, and selected tenant; role permissions are deliberately read from the current membership on every tenant operation so removal, suspension, or permission changes take effect immediately.

### Tenant-isolation convention for future modules

Every expense, invoice, document, journal, and other organization-owned table must have a non-null `tenant_id`, tenant-leading indexes and uniqueness constraints, and tenant-consistent foreign keys where practical. Its service/repository API must require the server-validated active tenant and query by `(tenant_id, id)`—never retrieve by an unscoped identifier and never treat a body/header tenant ID as authorization. Integration tests must create two tenants and prove both read and mutation isolation.

## Session and token security

- Passwords use BCrypt strength 12.
- Access JWTs are short lived, held only in browser memory, signed with HS256, and validate algorithm, signature, issuer, audience, and expiry.
- Refresh tokens are random opaque values stored only as SHA-256 hashes. The browser cookie is `HttpOnly`, has a restricted `/api/v1/auth` path, configurable `Secure`/`SameSite`, and a fixed expiry.
- Refresh rotation uses a pessimistic database lock. Reuse of the immediately previous token revokes the session family; concurrent refresh therefore has a deterministic single winner.
- Cookie-authenticated refresh/logout endpoints require the readable `XSRF-TOKEN` cookie value in `X-CSRF-TOKEN`. Credentialed CORS accepts only configured explicit origins.
- Verification, reset, and invitation tokens are random, hashed, expiring, single-use values. Password reset and password change revoke all refresh sessions.
- Login and recovery responses avoid account enumeration. Sensitive endpoints have an in-memory per-instance fixed-window rate limit; production multi-instance deployment should replace it with a shared limiter.

Use a unique `AUTH_JWT_SECRET` of at least 32 random bytes in every deployed environment. Set `AUTH_COOKIE_SECURE=true` behind HTTPS. If the frontend and API are deployed cross-site, explicitly review `AUTH_COOKIE_SAME_SITE`, HTTPS, CORS, and CSRF behavior together.

For local development, Vite proxies `/api` to `http://localhost:8080`, and the frontend defaults to the relative `/api/v1` base URL. This removes unnecessary local cross-origin traffic. Direct cross-origin requests remain supported for configured explicit origins; wildcard origins are never combined with credentials.

## Role and permission matrix

| Role | Tenant read | Settings | Member list | Invite | Manage roles/status | Transfer ownership | Audit read | Intended later responsibility |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `OWNER` | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Legal owner and ultimate tenant administration |
| `ADMIN` | Yes | Yes | Yes | Yes | Yes | No | Yes | Administration; does not implicitly gain future posting/payment authority |
| `ACCOUNTANT` | Yes | No | Yes | No | No | No | No | Future validation, posting, payments, reconciliation, and close |
| `APPROVER` | Yes | No | Yes | No | No | No | No | Future assigned approvals; never self-approval |
| `EMPLOYEE` | Yes | No | Yes | No | No | No | No | Future own receipts and claims |
| `VIEWER` | Yes | No | Yes | No | No | No | No | Future authorized read-only financial views |

Only owners may assign administrators. `OWNER` cannot be assigned through role payloads; ownership uses the atomic transfer operation, which promotes the target and demotes the prior owner. Owners cannot be suspended, removed, or demoted through general membership endpoints.

## API summary

All endpoints use `/api/v1`. JSON validation failures and application errors return a consistent body containing `status`, `code`, `message`, `path`, and optional `fields`. Invalid authentication is `401`; valid authentication without authority is `403`.

### Authentication

| Method and path | Purpose |
| --- | --- |
| `POST /auth/register` | Register and send verification mail |
| `POST /auth/verify-email` | Consume verification token |
| `POST /auth/login` | Start session and receive access token |
| `POST /auth/refresh` | Rotate refresh token; requires CSRF header |
| `POST /auth/logout` | Revoke current refresh session; requires CSRF header |
| `POST /auth/logout-all` | Revoke all user sessions |
| `GET /auth/me` | Read current identity and selected tenant |
| `POST /auth/forgot-password` | Send generic recovery response |
| `POST /auth/reset-password` | Consume reset token and revoke sessions |
| `POST /auth/change-password` | Change password and revoke sessions |

### Organizations and access

| Method and path | Purpose |
| --- | --- |
| `POST/GET /organizations` | Create organization / list memberships |
| `POST /organizations/{id}/select` | Validate membership and issue tenant context |
| `GET/PUT /organization` | Read/update selected organization |
| `GET /members` | Paginated selected-tenant members |
| `PATCH /members/{id}/role` | Change an allowed membership role |
| `PATCH /members/{id}/status` | Suspend/reactivate/remove membership |
| `POST /members/{id}/transfer-ownership` | Atomic ownership transfer |
| `GET/POST /invitations` | Paginated invitations / create invitation |
| `DELETE /invitations/{id}` | Cancel pending invitation |
| `GET /invitations/preview?token=...` | Public, masked invitation preview |
| `POST /invitations/accept` | Verified matching user accepts once |
| `GET /permissions` | Fixed role mapping |
| `GET /audit-events` | Paginated tenant audit events for owner/admin |

Example login:

```http
POST /api/v1/auth/login
Content-Type: application/json

{"email":"owner@example.com","password":"a-long-private-password"}
```

Use the returned access token as `Authorization: Bearer <token>`. Refresh/logout requests additionally copy the `XSRF-TOKEN` cookie into the `X-CSRF-TOKEN` header; the raw refresh token is never available to JavaScript.

## Email delivery

Production uses Spring's mail sender configured with `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, and `MAIL_FROM`. Development should run with `SPRING_PROFILES_ACTIVE=dev`; its in-memory mailbox exposes test tokens through the dev-only mailbox endpoint and never writes raw tokens to logs.

## Known boundaries

Phase 1 does not include accounting records, platform super-admin, tenant impersonation, custom roles, subscription enforcement, or asynchronous email delivery. The current rate limiter and development mailbox are process-local. Important concurrency and constraint behavior is integration-tested against H2 in MySQL mode; a MySQL/Testcontainers suite remains recommended for CI before production deployment.
