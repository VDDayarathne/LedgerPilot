# API Conventions

## Basics

- Public endpoints use JSON over HTTPS under `/api/v1`.
- Resource names are plural nouns; paths do not contain verbs unless representing an action that is not CRUD, such as `POST /expenses/{id}/submit`.
- JSON properties use `camelCase`; timestamps use RFC 3339 UTC; dates use ISO `YYYY-MM-DD`; currencies use ISO 4217 codes.
- Money is serialized as a decimal string to preserve precision, for example `"123.45"`.
- DTOs define contracts. Persistence entities are never request or response bodies.
- OpenAPI is the authoritative machine-readable contract once endpoints exist.

## Tenant context and authorization

The implemented API selects an active tenant through `/api/v1/organizations/{id}/select`, then resolves tenant context from the validated access token and current database membership. Existing tenant endpoints remain stable. Future APIs may use explicit tenant paths only through a documented, versioned contract change. A client-supplied tenant identifier is never proof of access.

Use `404 Not Found` when exposing resource existence would leak another tenant's data. Use `403 Forbidden` for known resources where revealing existence is safe.

## Requests and validation

- Reject unknown fields for security-sensitive and financial commands unless compatibility requires otherwise.
- Apply syntactic DTO validation at the boundary and domain validation in application/domain services.
- Trim and normalize identifiers deliberately; do not silently modify financial descriptions or evidence.
- Mutating endpoints that may be retried accept `Idempotency-Key`; scope stored keys to organization, actor, endpoint, and request hash.
- Optimistic updates use a version field or `If-Match`; stale changes return `409 Conflict`.

## Responses

- `200 OK`: successful read/action with a body.
- `201 Created`: resource created, with `Location`.
- `202 Accepted`: asynchronous job accepted, with job/status resource.
- `204 No Content`: successful mutation without a response body.
- `400`: malformed input; `401`: unauthenticated; `403`: forbidden; `404`: absent/concealed; `409`: state or idempotency conflict; `422`: well-formed request violating domain validation; `429`: rate limited.

Single-resource responses use the resource directly. Collections use:

```json
{
  "items": [],
  "page": { "nextCursor": null, "limit": 50 }
}
```

Prefer stable cursor pagination ordered by `(createdAt, id)`. Default limit is 50 and maximum 200. Filters are explicit query parameters; sort fields are allow-listed.

## Errors

Use `application/problem+json` based on RFC 9457:

```json
{
  "type": "https://api.ledgerpilot.example/problems/validation-error",
  "title": "Validation failed",
  "status": 422,
  "code": "EXPENSE_TOTAL_MISMATCH",
  "detail": "The total does not equal subtotal plus tax.",
  "instance": "/api/v1/organizations/.../expenses/...",
  "correlationId": "...",
  "errors": [{ "field": "total", "code": "mismatch" }]
}
```

Error codes are stable and documented. Details must not expose stack traces, SQL, secrets, cross-tenant identifiers, or provider payloads.

## Compatibility and operations

- Breaking changes require a new major API version or a documented migration period.
- Additive optional fields are normally compatible; clients must tolerate them in responses.
- Correlation IDs are accepted/generated and returned as `X-Correlation-Id`.
- Rate-limit responses include `Retry-After` where possible.
- Webhooks are signed, timestamped, replay-protected, idempotent, and processed asynchronously.
