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

## Downloading the TaskForge APK

Every push that changes App 01 or its build configuration starts the **Build TaskForge APK** GitHub Actions workflow. When the run succeeds, open the repository's **Actions** tab, select the latest **Build TaskForge APK** run, and download the **TaskForge-debug-apk** artifact. Extract the ZIP to get the installable debug APK. The artifact is retained for 14 days.

The debug APK is suitable for testing. It is not a signed production release.

## Project status

The initial repository structure and App 01 source are being established. A successful build or device test will only be reported after it has actually been run.
