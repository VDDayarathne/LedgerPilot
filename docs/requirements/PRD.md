# Product Requirements

## Product vision

LedgerPilot helps small and medium businesses capture expenses, turn receipts and invoices into reviewable structured data, approve spending, create controlled accounting postings, and reconcile bank activity. Automation reduces data entry; accountable humans retain control over approvals, postings, and payments.

## Users

- **Organization owner/admin:** configures the organization, members, policies, accounts, and integrations.
- **Employee/submitter:** uploads documents and submits expenses.
- **Reviewer/manager:** verifies extracted data and approves or rejects expenses within assigned authority.
- **Accountant:** manages the chart of accounts, tax treatment, postings, adjustments, and reconciliation.
- **Auditor/read-only user:** inspects records and immutable audit history without mutation rights.
- **System operator:** monitors platform health without receiving implicit access to tenant financial data.

One user may belong to multiple organizations with different roles. The active organization must be explicit in every tenant-scoped interaction.

## Core use cases

1. Create an organization and invite members.
2. Upload a receipt or invoice and process it asynchronously.
3. Review extracted merchant, date, currency, totals, tax, line items, and supporting evidence.
4. Create, edit, categorize, and submit an expense.
5. Apply approval policies and capture approval history.
6. Generate a draft balanced journal entry from an approved expense.
7. Let an authorized accountant review and post or reject that entry.
8. Import bank transactions and reconcile them against posted activity.
9. Correct mistakes through traceable adjustments or reversals.
10. Search, export, and audit organization activity.

## Functional requirements

### Identity and tenancy

- Authenticate users securely and manage sessions.
- Enforce role- and permission-based access within an organization.
- Prevent access across organizations, including by guessed identifiers.
- Audit security-sensitive and financial actions.

### Document capture and AI assistance

- Accept supported image and PDF uploads with size and content checks.
- Track asynchronous processing status and retries.
- Store extracted fields with confidence and source evidence.
- Route uncertain, inconsistent, duplicate, or policy-sensitive results to human review.
- Preserve the original document and extraction version for auditability.

### Expenses and approvals

- Support draft, submitted, approved, rejected, and cancelled lifecycle states.
- Validate totals, currency, required evidence, and policy rules before submission.
- Keep approval decisions append-only and attributable.
- Prevent a submitter from bypassing approval authority.

### Accounting and reconciliation

- Maintain a tenant-specific chart of accounts and fiscal settings.
- Produce balanced draft journal entries using deterministic rules.
- Require explicit authorized posting; posted entries are immutable.
- Import, match, confirm, and unmatch bank transactions without destroying history.

## Non-functional requirements

- Financial calculations must be deterministic, decimal-based, currency-aware, and reproducible.
- Critical mutations must be transactional, idempotent where retried, and auditable.
- Long-running work must not block HTTP requests.
- APIs must use stable versioning, structured errors, pagination, and request correlation IDs.
- Logs must exclude credentials, raw document bodies, payment data, and unnecessary personal data.
- The system must support backup, restore, retention, and deletion policies before production launch.
- Accessibility target is WCAG 2.2 AA for primary workflows.

## MVP boundaries

In scope: web application, organization membership, receipt/invoice upload, assisted extraction, expense review and approval, chart of accounts, draft and posted journals, CSV bank import, reconciliation, audit trail, and basic exports.

Out of scope for MVP: payroll, inventory, full accounts receivable, autonomous payments, autonomous posting, tax filing, native mobile apps, multi-entity consolidation, real-time bank payment initiation, and replacing a complete ERP.

## Success measures

- No tenant-isolation failures in automated authorization tests.
- Every posted journal is balanced and linked to its source and approver.
- At least 95% of supported documents complete processing without operator intervention; low-confidence fields may still require user review.
- Median user correction count and time-to-review trend downward without reducing control quality.
- All privileged and financial state changes are represented in the audit trail.

## Product constraints

- AI output is advisory and untrusted until validated.
- Authorization is enforced server-side, never inferred from UI visibility.
- The original financial evidence and material user corrections remain traceable.
- Payments are not initiated in the MVP.
