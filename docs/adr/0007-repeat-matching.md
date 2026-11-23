# ADR-0007: What counts as a repeat

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
A repeat with the same key and the same details returns the existing payment; the same key with different details is rejected (FR-005, FR-006). The full card number and CVV are discarded once a payment is final (BR-006, BR-013), so they can't always be compared. Approved by the engineer on 2026-11-23 (choice 5a; the separate refund-key namespace was approved with that plan) for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose to compare only stored, non-sensitive details:
- A payment repeat must match on amount, currency, the card's last four digits and expiry month/year. The full card number and CVV aren't compared, because they are discarded (BR-006, BR-013).
- Applies once refunds are specified: a refund repeat must match on payment ID and amount, and refund keys are kept separate from payment keys.
- Each key is unique per merchant (BR-010).

Chose this over comparing a keyed hash of the full card number and CVV.

## Consequences
No sensitive card data needs to be kept for matching. Two different cards with the same last four digits, expiry, amount and currency under the same key count as a repeat. Revisit if merchants run into false repeats.
