# Security

## Security model

Treat all client, file, provider, and model input as untrusted. Authentication establishes identity; authorization separately establishes permission in the active organization. Deny by default.

## Authentication and sessions

- Use an established framework and strong password hashing (Argon2id or bcrypt with reviewed parameters).
- Require verified email before privileged organization actions.
- Access tokens are short-lived and audience/issuer validated. Refresh tokens are rotated, stored only as hashes server-side, and revoked on reuse.
- For browser delivery, prefer Secure, HttpOnly, SameSite cookies with CSRF protection; do not store long-lived tokens in browser local storage.
- Rate-limit login, recovery, invitation, and token endpoints without enabling user enumeration.
- MFA is required before production for organization owners and privileged accounting roles.

## Authorization and tenant isolation

- Every organization-owned service method receives explicit organization and actor context.
- Verify membership status and permission before loading or mutating data.
- Scope repository operations by `tenant_id`; unscoped find-by-ID methods are forbidden for tenant resources.
- Background jobs carry organization context and revalidate resource ownership.
- Cross-tenant isolation tests are mandatory for every tenant-owned resource and indirect reference.
- Support access requires explicit, time-bound, audited impersonation; it is not granted by operator status alone.

## Files

- Allow-list media types and extensions; verify magic bytes and reject polyglots where feasible.
- Enforce configured size/page/count limits before processing.
- Generate storage keys; never use a submitted filename as a path.
- Keep objects private and encrypted. Use short-lived signed access URLs after authorization.
- Quarantine uploads until malware scanning succeeds; sandbox parsers and disable active document content.
- Record SHA-256, uploader, scan state, extraction versions, access, and deletion events.

## Service and integration security

- Backend-to-AI calls use authenticated service identity, TLS, narrow scopes, timeouts, and replay protection.
- Validate signed provider webhooks before enqueueing work.
- Secrets come from environment variables locally and an approved secret manager in production.
- Never log tokens, credentials, document contents, full bank identifiers, or sensitive provider responses.
- AI prompts and outputs are data, not instructions. Defend against prompt injection and restrict tool/network access.

## Financial controls

- Segregate submission, approval, posting, reconciliation, and administration permissions.
- Enforce approval limits and separation-of-duties rules server-side.
- Require step-up authentication for material security changes and, when introduced, payment actions.
- An LLM can propose but cannot approve, post, change authorization, or initiate payment.
- Audit privileged reads and every financial-state mutation with actor, organization, before/after summary, time, and correlation ID.

## Privacy and operations

- Minimize collected personal data and define retention by record class and jurisdiction before production.
- Encrypt data in transit and at rest; document key rotation and backup restoration.
- Sanitize telemetry and restrict production data access by least privilege.
- Dependency, secret, static-analysis, and vulnerability checks run in CI.
- Maintain incident response, breach notification, disaster recovery, and account deletion procedures before launch.

## Security acceptance baseline

Production is blocked until threat modeling, tenant escape tests, backup restoration, secret rotation, upload abuse tests, access reviews, and incident-response exercises are complete.
