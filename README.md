# Project-20

A collection of independent native Android applications, maintained in one repository, with shared platform services where appropriate.

## Platform and conventions

- **Android apps:** Kotlin, Jetpack Compose and Material 3
- **Architecture:** Independent application modules under apps/
- **Connectivity:** Offline-first by default; network access is added only when an app requires it
- **Quality bar:** Each app receives a specification, implementation, tests where practical, and its own README before it is considered complete

## Nori ID

Nori ID is the shared identity and access-control service for Nori applications. The first local foundation is under [nori-id/](nori-id/).

The current version runs on the owner's PC with a local SQLite database and a versioned HTTP API. It includes international phone-number metadata, development-only phone challenge verification, account registration, Nori handles, Argon2id password hashing, an owner control panel, security activity records and app-specific access policies.

**This is a local development foundation, not a public account service.** Real SMS delivery, guardian consent, production hosting, account recovery, and production security operations remain future work. Follow the setup and safety boundaries in [Nori ID README](nori-id/README.md).

## Repository structure

- apps/ — independent Android application modules
- docs/ — architecture, roadmap and development standards
- nori-id/ — local-first identity service and owner console
- .github/workflows/ — automated build and test workflows

## Build Android apps

Open the repository root in a compatible version of Android Studio, allow Gradle sync to complete, then select the application run configuration documented by that app.

See [App 01 documentation](apps/app-01-taskforge/README.md) for TaskForge setup.

## Quality rule

Do not claim a build, test, or device check passed unless it was actually executed. Existing applications must remain intact when adding a new app or shared service.
