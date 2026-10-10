# AI Document Pipeline

## Objective and trust boundary

The pipeline converts receipts and invoices into reviewable candidate data with traceable evidence. OCR and LLM output is untrusted. The backend owns workflow state, deterministic validation, authorization, and financial decisions.

## Stages

1. **Ingest:** authorize upload, stream with a size limit, hash, store privately, create metadata/job, and return `202`.
2. **Safety checks:** verify type/magic bytes, scan for malware, enforce page and decompression limits, and quarantine failures.
3. **Preprocess:** normalize orientation, split pages, improve image quality, and retain page coordinates.
4. **OCR:** extract text, tokens, tables, bounding boxes, language, and provider confidence.
5. **Structured extraction:** map merchant, invoice number, dates, currency, subtotal, taxes, total, payment hints, and line items into a versioned schema.
6. **Normalize:** parse locale-sensitive dates/numbers, normalize currency and tax identifiers, and preserve original values.
7. **Deterministic validation:** check arithmetic, required fields, date ranges, duplicate signals, currency consistency, and policy constraints.
8. **Confidence/risk assessment:** combine field confidence, OCR quality, validation outcomes, provider disagreement, and historical correction signals.
9. **Human review:** display source evidence and require correction/confirmation for uncertain or high-risk fields.
10. **Finalize:** persist a reviewed immutable result version and allow downstream expense submission.

## Result contract

Each result contains `schemaVersion`, document/job IDs, provider and model versions, extraction timestamp, page metadata, candidate fields, normalized values, confidence, evidence locators, validation issues, and overall disposition. Raw provider output is stored only when retention and sensitivity controls permit it.

Evidence must let a reviewer find the page and bounding region or text span that supports a field. User edits preserve the extracted value, corrected value, actor, timestamp, and reason when required.

## Confidence and review

Confidence is calibrated operational data, not a claim of truth. Thresholds are configuration with recorded versions:

- high confidence plus all deterministic checks: prefill, still user-reviewable;
- medium confidence: highlight fields and require confirmation;
- low confidence, contradictions, duplicate risk, unusual totals, or missing evidence: mandatory review;
- unsafe/unreadable/unsupported documents: reject processing with a stable reason code.

No threshold can auto-approve an expense, post a journal, or authorize payment.

## Async behavior and failure handling

- Jobs use states `QUEUED`, `RUNNING`, `REVIEW_REQUIRED`, `COMPLETED`, `FAILED_RETRYABLE`, `FAILED_TERMINAL`, and `CANCELLED`.
- Idempotency is based on job/result version, not filename alone.
- Use bounded retries with backoff for transient failures; malformed or unsafe content fails terminally.
- Leases/timeouts recover abandoned work. Late results cannot overwrite a newer accepted version.
- Provider outages degrade to queued/manual entry rather than bypassing controls.

## Model governance and evaluation

- Version prompts, schemas, normalization, providers, and models.
- Maintain a consented, de-identified evaluation set representing layouts, languages, currencies, image quality, and edge cases.
- Measure per-field precision/recall or exact match, arithmetic validity, review rate, correction rate, latency, cost, and failure rate.
- Evaluate and approve changes before rollout; use staged rollout and rollback controls.
- Never train on tenant documents or corrections without explicit legal basis and tenant consent.

## Security

Minimize provider data, use approved regions/retention settings, redact where possible, prevent prompt content from invoking tools, and prohibit arbitrary network/file access. Logs contain identifiers and metrics, not document text or images.

