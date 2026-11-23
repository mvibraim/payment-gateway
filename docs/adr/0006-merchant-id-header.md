# ADR-0006: Merchant identity from a trusted header

Status: Accepted · Date: 2026-11-23 · Plan: `docs/plans/payment-authorization/plan.md`

## Context
Merchants may access only their own payments (BR-004), and request keys are per merchant (BR-010), so every request needs a merchant identity. Merchant sign-up and credential management are out of scope. Approved by the engineer on 2026-11-23 for the earlier payment-gateway plan and adopted unchanged for payment-authorization.

## Decision
Chose an `X-Merchant-Id` header, required on every endpoint and trusted without authentication, over fixed API keys in configuration and over a command-line interface (engineer's choice). A missing or blank header gets 400 and is never recorded or sent to the bank.

## Consequences
Simple to call and test, but any caller can act as any merchant; this is accepted for a simulation. Revisit if real merchants or credentials come into scope.
