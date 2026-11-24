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
| `./gradlew test` | Unit tests (`src/test/java`), without Spring |
| `./gradlew integrationTest` | Integration tests (`src/integrationTest/java`): full application on a SQLite file in a temporary folder |
| `./gradlew build` | Compiles with `-Xlint:all -Werror`, runs both suites and builds the jar |
