# Project-20 Architecture

## Monorepo model

Each Android application is an independent Gradle application module under apps/. Applications must not depend on another app module. This keeps installation, testing and release boundaries clear.

## Shared platform services

Nori ID is a separate service, not another Android application module. It owns account identity, authentication, application access policy and security event records. Android, Windows and future Nori clients communicate with it through a versioned HTTP API. Client applications must never read or write the identity database directly.

The first Nori ID implementation is local-first and stores records in SQLite on the owner's PC. Its API and persistence boundary are designed to allow a later move to a hosted PostgreSQL-backed service without changing the identity model used by clients. A local instance is not remotely available to users on other networks and must not be exposed publicly.

## Identity and access boundaries

- Nori ID owns the canonical account ID and unique Nori handle.
- Phone numbers are normalized to E.164 using current international numbering metadata.
- Number-format validation is not proof of phone ownership; production ownership verification requires an SMS or equivalent trusted challenge.
- Passwords are stored using Argon2id hashes.
- App-specific policy is evaluated centrally: enabled state, open/invite/closed registration mode, minimum age and guardian-approval requirement.
- Guardian consent is a separate workflow and must not be represented as complete until guardian identity, relationship and authorization have been verified.
- Administrative activity is limited to security and account-management events. It is not permission to inspect private user content.
- Owner and administrator privileges must be protected separately from ordinary user sessions.

## Data portability and operations

- Keep the database behind the service API.
- Use explicit schema migrations before production.
- Keep local database backups private and test restoration.
- A hosted deployment requires HTTPS, managed secrets, real SMS delivery, rate limiting, monitoring, account recovery, privacy/legal review and incident procedures.
- Do not use development OTP mode outside local development.
- Do not collect real personal data in the local prototype.

## Application baseline

- Kotlin for Android application logic
- Jetpack Compose and Material 3
- Local persistence for offline-first application data
- Clear separation between UI, state and data operations
- A unique application ID for every app

## Build strategy

Build and verify one application at a time. A new application must not break an existing application. Shared service code stays outside app-specific modules and has its own tests and CI workflow.
