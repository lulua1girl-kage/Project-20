# Project-20

A collection of 20 independent native Android applications, maintained in one repository.

## Platform and conventions

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** Independent application modules under `apps/`
- **Connectivity:** Offline-first by default; network access is added only when an app requires it
- **Quality bar:** Each app receives a specification, implementation, tests where practical, and its own README before it is considered complete

## Repository structure

```
Project-20/
├── apps/
│   └── app-01-taskforge/
├── docs/
│   ├── architecture.md
│   ├── app-roadmap.md
│   └── standards.md
├── build.gradle.kts
└── settings.gradle.kts
```

## Applications

| ID | Application | Purpose | Status |
|---|---|---|---|
| 01 | TaskForge | Offline task manager | In development |
| 02–20 | To be specified | Independent applications | Planned |

## Build

Open the repository root in a compatible version of Android Studio, allow Gradle sync to complete, then select the `app-01-taskforge` run configuration.

See [App 01 documentation](apps/app-01-taskforge/README.md) for its scope and implementation details.

## Project status

The initial repository structure and App 01 source are being established. A successful build or device test will only be reported after it has actually been run.
