Phase    Requirements
1. Core functionality    Merchants can submit payments, receive authorization or decline results, and retrieve previous transactions. Invalid payments must be rejected.
2. Reliability    Duplicate payment requests must not charge customers twice. The system must correctly handle unavailable banks and uncertain payment outcomes.
3. New features    Merchants can refund successful payments, view transaction history, and generate daily payment summaries.

Acceptance scenarios
- A valid payment is authorized and can be retrieved afterward.
- An invalid payment is rejected without charging the customer.
- Submitting the same payment twice never results in two charges.
- A bank failure does not incorrectly mark a payment as declined.
- A refund cannot exceed the original authorized payment amount.
- Sensitive card details are never exposed to merchants.
Scope: No actual money, bank accounts, or real card information. Only simulated transactions.
