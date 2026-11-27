# Owner and Admin Console

## Roles
- Owner: system configuration, account roles, challenge publishing, system health
- Challenge editor: draft/edit; publish only if granted
- Support: account status and diagnostics, not task content by default
- Analyst: aggregate metrics
- User: own account and data

## Default metrics
- Aggregate daily/weekly active users
- Feature adoption counts
- Sync success/failure, queue age, conflict rate
- Reminder permission and delivery diagnostics (coarse)
- Crash/ANR and app-version distribution
- Challenge completion aggregates

## Sensitive activity
Per-account usage duration and timelines are not default dashboard features. If added for a disclosed operational purpose, require narrow access, audit, retention limit, and a user-facing explanation. Never rank personal worth or academic performance from app usage.

## Challenge workflow
Draft -> preview -> validation -> schedule/publish -> notification preference -> version history -> archive.

## Audit
Record admin ID, action, target, timestamp, reason, before/after metadata, and correlation ID. Avoid logging unnecessary personal content. Restrict and protect audit logs.
