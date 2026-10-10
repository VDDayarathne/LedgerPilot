# Accounting Rules

## Principles

- Double-entry accounting is mandatory: each posted journal has equal debits and credits in its posting currency.
- Debit increases assets and expenses and decreases liabilities, equity, and income. Credit has the opposite effect.
- Calculations use decimal arithmetic, explicit scale, and a configured currency rounding mode. Binary floating point is forbidden.
- Posted entries are immutable. Corrections use linked reversals and replacement entries.
- AI suggestions never become posted entries without deterministic validation and authorized human action.

## Expense posting

A typical approved expense produces a draft journal:

- debit expense/asset accounts for net line amounts;
- debit recoverable input-tax accounts when policy permits;
- credit a payable, employee reimbursement, cash, or card clearing account for the total.

The exact mapping comes from tenant configuration, tax rules, payment method, and accounting date. Missing or ambiguous mappings block posting and require accountant review.

## Amount and currency rules

- Store source currency and source amounts. When base currency differs, store the applied rate, rate date/source, and converted amounts.
- Round at documented boundaries using the currency's configured minor units; do not repeatedly round intermediate calculations.
- A deterministic rounding line may be used only within a configured tolerance and must use a designated account.
- Validate `subtotal + tax + adjustments = total` within the currency tolerance before submission or explicitly capture the discrepancy.
- Negative documents are credit notes/refunds with explicit source type, not ordinary expenses with accidental signs.

## Journal lifecycle

`DRAFT -> READY_FOR_REVIEW -> POSTED` or `DRAFT/READY_FOR_REVIEW -> REJECTED`. Only an authorized accountant can post. Posting occurs in one database transaction that:

1. validates source approval and uniqueness;
2. verifies the period is open and accounts are active;
3. recalculates and validates line balance;
4. assigns an organization-scoped immutable journal number;
5. marks the entry posted and records actor/time;
6. writes audit and outbox records.

Retries must not create duplicate journals for one source and posting purpose.

## Adjustments and reversals

- Never edit or delete posted lines.
- A reversal swaps debits and credits, references the original, states a reason, and uses an allowed accounting date.
- A correction normally consists of a full reversal plus a new entry. Partial adjustments require an explicit supported workflow.
- Closed periods cannot receive ordinary postings. Reopening is separately authorized and audited; otherwise post an adjustment in an open period with reference to the affected period.

## Approval and segregation of duties

- Submission freezes the reviewed financial snapshot used for approval; material edits invalidate prior approval and require resubmission.
- Approval decisions are append-only and evaluated against the policy version captured at submission.
- Self-approval is denied by default. Exceptions require explicit tenant policy and audit visibility.
- Posting permission is distinct from expense approval permission.

## Reconciliation

- Imported bank transactions are immutable source records; normalization creates derived fields.
- Candidate matching may use amount, date window, reference, merchant, and account, but remains a suggestion.
- Confirmation records actor, time, matched amounts, and links. One-to-many and many-to-one matches must balance exactly within configured currency tolerance.
- Unmatching preserves history and cannot alter the underlying posted journal.
- A transaction is reconciled only when its full amount is accounted for; partial matches remain visibly partial.

## Required invariants and tests

- total debits equal total credits;
- no line has both a debit and credit or neither;
- no posting to inactive accounts or closed periods;
- source, accounts, policy, and journal all belong to one organization;
- repeated commands are idempotent;
- approval invalidation and reversal behavior are deterministic;
- boundary rounding, zero/negative amounts, foreign currency, duplicate source, concurrency, and cross-tenant cases are tested.

