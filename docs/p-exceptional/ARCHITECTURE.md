# P-Exceptional Architecture Proposal

## Client
Use Kotlin Multiplatform / Compose Multiplatform for shared domain/UI where stable, with platform adapters for notifications, background work, secure storage, share/export, and calendar APIs. Keep the Android client aligned with Project 20's existing native Kotlin conventions. Treat the web target as beta/independently validated.

## Layers
1. Presentation: navigation, screens, UI state, accessibility semantics.
2. Domain: task/recurrence/planning/challenge/certificate/quote rules; pure Kotlin.
3. Data: repositories, local database, sync queue, migrations.
4. Platform adapters: notifications, background execution, secure credential storage, sharing.
5. Backend API: identity, authorization, sync, challenge publishing, certificate verification, account lifecycle, audit.
6. Admin console: separate entry point with role enforcement and audit.

## Data and sync
- Local database is the immediate source of truth for UI reads and offline edits.
- Server stores account-linked sync state and shared challenge content.
- Stable UUID, owner ID, timestamps, schema version, and sync state for records.
- Use tombstones for deletion and idempotency keys for retry.
- Preserve concurrent note/checklist edits; don't blindly overwrite all conflicts.
- Sync worker uses connectivity constraints, retry/backoff, and clear status.

## Backend candidates
Evaluate self-hosted Supabase (Postgres/Auth/Storage/Functions) versus standalone Kotlin/Ktor + PostgreSQL. Select after reviewing maintenance, backup/restore, migrations, authorization tests, and free/self-hosted requirements. No paid hosted tier is required for core use.

## Notification flow
Persist reminder locally, schedule platform notification, handle permission/OS limits, reschedule on reboot/timezone/app update, respect quiet hours. Push is supplemental; don't make server push the only reminder path.

## Cross-platform validation
Android, iOS, Windows, macOS, Linux are target clients. iOS build/test requires macOS and Xcode. Web/PWA support level must be explicitly marked and independently tested.
