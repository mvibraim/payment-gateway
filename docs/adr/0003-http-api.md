# ADR-0003: JSON HTTP API with merchant and request-key headers

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
Merchants submit and retrieve payments (FR-001 to FR-003), see only their own payments (BR-004), and send a request key with each payment (FR-005, FR-013). Merchant sign-up and credentials are out of scope (A-005). Every outcome in the spec needs a response a client can tell apart: new, repeated, pending, invalid, key reused, in progress, bank unavailable and not found.

## Decision
Chose a JSON HTTP API with the merchant and request key in headers over putting them in the request body, because they identify the request rather than describe the payment.

- `POST /payments` with body `{"cardNumber", "expiryMonth", "expiryYear", "cvv", "amount", "currency"}`. `amount` is a whole number in the currency's smallest unit.
- `GET /payments/{id}`, where the ID is a UUID.
- `X-Merchant-Id` is required on every request; if it's missing or blank the response is 400. `Idempotency-Key` is required on `POST /payments` from S05 on and is validated with the body fields (FR-013).
- A payment is returned as `{"id", "status", "amount", "currency", "card": {"last4", "expiryMonth", "expiryYear"}}`. No response ever contains the full card number or the CVV.
- Statuses: `authorized`, `declined`, `pending`, `unresolved`, and `in_progress` while the first bank call is under way (the spec's "in progress", FR-007).
- Status codes: 201 new authorized or declined payment · 202 new pending payment · 200 retrieval or repeat (any status) · 400 missing merchant header · 422 `{"errors": [{"field", "message"}]}` listing every validation failure · 409 `{"error": "key_reused"}` or `{"error": "in_progress"}` · 503 `{"error": "bank_unavailable"}` · 404 `{"error": "not_found"}` for unknown, malformed or another merchant's payment IDs.
- Every error body comes from one `@RestControllerAdvice`.

## Consequences
Clients can tell every outcome apart by status code alone, and the merchant header stands in for authentication until credentials are specified. Any merchant can claim any merchant ID, which is acceptable only because credentials are out of scope. Revisit when merchant credentials are specified, or if a client needs a status the API doesn't expose.
