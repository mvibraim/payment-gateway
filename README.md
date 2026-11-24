# Payment gateway

A Java 25 / Spring Boot JSON HTTP API that authorizes payments against a simulated bank, storing data in a SQLite file. See `docs/specs/payment-authorization.md` and `docs/plans/payment-authorization/plan.md`.

## Run

```
./gradlew bootRun
```

or build and run the jar:

```
./gradlew build
java -jar build/libs/payment-gateway-0.0.1-SNAPSHOT.jar
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

The database file path is `gateway.db.path` (default `data/payment-gateway.db`); its folder is created at startup. Override it, for example: `java -jar build/libs/payment-gateway-0.0.1-SNAPSHOT.jar --gateway.db.path=/tmp/gateway.db`.

## Tests

| Command | What it runs |
|---|---|
| `./gradlew test` | Unit tests (`src/test/java/com/example/gateway/unit`), without Spring |
| `./gradlew integrationTest` | Integration tests (`src/test/java/com/example/gateway/integration`): full application on a SQLite file in a temporary folder |
| `./gradlew build` | Compiles with `-Xlint:all -Werror`, runs both suites and builds the jar |

## API

### Submit a payment: `POST /payments`

Headers: `X-Merchant-Id` (required; a missing or blank value returns `400 {"error": "missing_merchant_id"}`) and `Content-Type: application/json`.

```json
{"cardNumber": "4242424242424240", "expiryMonth": 12, "expiryYear": 2030, "cvv": "123", "amount": 5000, "currency": "USD"}
```

`amount` is a whole number in the currency's smallest unit (5000 = USD 50.00). The payment is sent to the simulated bank, recorded, and returned with `201`:

```json
{"id": "58b7b4ef-3303-4dc2-94be-6a8873c481c3", "status": "authorized", "amount": 5000, "currency": "USD",
 "card": {"last4": "4240", "expiryMonth": 12, "expiryYear": 2030}}
```

`status` is `authorized` or `declined`. The full card number and CVV are never stored or returned. Validation and request keys are not enforced yet.

### Retrieve a payment: `GET /payments/{id}`

Header: `X-Merchant-Id` (required, as above). Returns `200` with the payment JSON shown above: its current `status`, amount, currency and the card's last four digits and expiry. The full card number and CVV are never returned.

`404 {"error": "not_found"}` is returned for an unknown ID, a malformed ID, or a payment that belongs to another merchant.

### Simulated bank

The bank runs inside the gateway and picks its answer from the card number's last digit:

| Last digit | Outcome |
|---|---|
| 1 | Declined |
| any other | Authorized |
