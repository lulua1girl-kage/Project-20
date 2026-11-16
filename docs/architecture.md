# Project-20 Architecture

## Monorepo model

Each application is an independent Gradle application module under `apps/`. Applications must not depend on another app module. This keeps installation, testing, and release boundaries clear.

## Shared code

Shared code is optional, not mandatory. A component belongs in `shared/` only when at least two applications have a stable, genuinely common requirement. App-specific behavior stays inside its application module.

## Application baseline

- Kotlin for application logic
- Jetpack Compose for UI
- Material 3 components
- Local persistence for offline-first applications
- Clear separation between UI, state, and data operations
- A unique application ID for every app

## Build strategy

Build and verify one application at a time. A new application must not break an existing application. Root Gradle configuration should remain minimal and should not become a dumping ground for app-specific dependencies.
