# App 01 — TaskForge

TaskForge is a small offline-first task manager for Android.

## Features in this first iteration

- Add a task
- Mark a task complete or active
- Delete a task
- View total and completed counts
- Persist tasks locally using Android SharedPreferences

## Technical choices

- Kotlin
- Jetpack Compose
- Material 3
- No network permission
- No account or remote service

## Run

Open the Project-20 root folder in Android Studio, sync Gradle, and run the `app-01-taskforge` module on an Android emulator or device.

## Current limitations

- Tasks are stored on the device and are not synchronized.
- There are no due dates, reminders, categories, or cloud backup in this version.
- Build and device verification are pending.

The automated debug APK build runs from the repository's GitHub Actions workflow.
