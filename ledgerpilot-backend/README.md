# LedgerPilot backend

Spring Boot 4.1.1 / Java 21 modular-monolith API for LedgerPilot.

## Run locally

Create a MySQL database named `ledgerpilot`, copy `.env.example` to the Git-ignored `.env`, fill in its local values, and start the backend. The backend loads `.env` automatically when it is started from this directory.

Use the development profile for the in-memory test mailbox (Gmail is not used in this profile):

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
.\gradlew.bat bootRun
```

The API is available at `http://localhost:8080/api/v1`. Development email can be inspected at `GET /api/v1/dev/mailbox?email=user@example.com`; this endpoint does not exist outside `dev` and `test` profiles.

Run tests with a Java 21 JDK:

```powershell
.\gradlew.bat test
```

See `../docs/PHASE_1_AUTH_TENANCY.md` for architecture, permissions, tenant-isolation rules, API routes, and production configuration.
