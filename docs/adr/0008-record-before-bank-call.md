# ADR-0008: Record first, then call the bank

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
A duplicate that arrives while the original is still processing must get "in progress" (FR-007). Requests that fail because the bank was unavailable aren't recorded and don't use up their key (BR-011). The spec defines only the statuses authorized, declined, pending and unresolved. Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose to record before calling the bank:
- A payment is inserted with status `processing` before the bank call, so a simultaneous duplicate finds it and gets "in progress".
- If the bank is unavailable, the row is deleted, so the request isn't recorded and the key can be reused (BR-011).
- At startup, leftover `processing` rows become `pending`, because the bank may already have processed them.
- Merchants see `processing` as **pending** in retrieval (and, once specified, in history and summaries), because the spec only defines authorized, declined, pending and unresolved (approved with the earlier plan).
- Applies once refunds are specified: refunds follow the same pattern, and the amount of a refund still being processed is held back from the refundable amount.

Chose this over calling the bank first and inserting afterwards, which can't detect simultaneous duplicates or hold back refund amounts.

## Consequences
Duplicate detection works across simultaneous requests, at the cost of an extra write and a delete on the unavailable path. Revisit if bank calls become asynchronous, where the merchant gets an ID before the bank answers.
